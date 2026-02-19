package org.zarp.bytes.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zarp.bytes.HasUncheckedRandomInput;
import org.zarp.bytes.UncheckedRandomInput;
import org.zarp.bytes.ZByteStore;
import org.zarp.bytes.internal.OnHeapByteStore;
import org.zarp.core.api.Jvm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public final class ByteCommon {

    private static final Logger log = LoggerFactory.getLogger(ByteCommon.class);

    private ByteCommon() {
        throw new InstantiationError(ByteCommon.class.getSimpleName());
    }

    private static final boolean SKIP_ASSERT = !Jvm.isAssertEnabled();
    private static final MethodHandle VECTORIZED_MISMATCH_METHOD;

    static {
        MethodHandle vectorizedMismatchMethod = null;
        try {
            if (Jvm.isJava9Plus() && !Jvm.getBoolean("vectorized.content_equals.disable")) {
                Class<?> arraySupportClass = Class.forName("jdk.internal.util.ArraysSupport");
                Method vectorizedMismatch = Jvm.getMethod(arraySupportClass, "vectorizedMismatch",
                        Object.class,
                        long.class,
                        Object.class,
                        long.class,
                        int.class,
                        int.class);
                vectorizedMismatch.setAccessible(true);
                vectorizedMismatchMethod = MethodHandles.lookup().unreflect(vectorizedMismatch);
            }
        } catch (Exception ex) {
            if (ex.getClass().getSimpleName().equals("java.lang.reflect.InaccessibleObjectException")) {
                log.warn("could not access vectorizedMismatch, the following command line args are required:\n\t{}\n\t{}\n\t{}",
                        "--illegal-access=permit",
                        "--add-exports java.base/jdk.internal.ref=ALL-UNNAMED",
                        "--add-exports java.base/jdk.internal.util=ALL-UNNAMED", ex);
            } else {
                log.error("error initializing", ex);
            }
        } finally {
            VECTORIZED_MISMATCH_METHOD = vectorizedMismatchMethod;
        }
    }

    /**
     * Puts a long value into a byte array
     *
     * @param arr byte array
     * @param i   index of byte array
     * @param v   long value
     */
    public static void putLong(byte[] arr, int i, long v) {
        arr[i++] = (byte) (v >>> 56);
        arr[i++] = (byte) (v >>> 48);
        arr[i++] = (byte) (v >>> 40);
        arr[i++] = (byte) (v >>> 32);
        arr[i++] = (byte) (v >>> 24);
        arr[i++] = (byte) (v >>> 16);
        arr[i++] = (byte) (v >>> 8);
        arr[i] = (byte) v;
    }

    /**
     * Gets long value from byte array at given index
     *
     * @param arr byte array
     * @param i   read index
     * @return long value
     */
    public static long getLong(byte[] arr, int i) {
        return ((long) (arr[i] & 0xff) << 56)
                | ((long) (arr[i + 1] & 0xff) << 48)
                | ((long) (arr[i + 2] & 0xff) << 40)
                | ((long) (arr[i + 3] & 0xff) << 32)
                | ((long) (arr[i + 4] & 0xff) << 24)
                | ((arr[i + 5] & 0xff) << 16)
                | ((arr[i + 6] & 0xff) << 8)
                | (arr[i + 7] & 0xff);
    }

    /**
     * Converts a byte into a boolean value, the byte is considered to represent the bool value false if
     * its value equals to 0, 'N', 'n'. Otherwise, returns true.
     *
     * @param i8 byte value to check
     * @return boolean value represents by byte
     */
    public static boolean byteToBool(byte i8) {
        return i8 == 1 || i8 == 'Y' || i8 == 'y';
    }

    public static boolean contentEquals(ZByteStore<?> a, ZByteStore<?> b) throws IllegalStateException {
        if (a == null) {
            return b == null;
        }
        if (b == null) {
            a.ensureNotReleased();
            return false;
        }
        a.ensureNotReleased();
        b.ensureNotReleased();
        long readRemains = a.readRemaining();
        if (readRemains != b.readRemaining()) {
            return false;
        }

        if (VECTORIZED_MISMATCH_METHOD != null
                && a.readAvailable() == b.readAvailable()
                && a.readAvailable() < Integer.MAX_VALUE
                && a.readAvailable() > 7) {
            try {
                return java11ContentEqualsUsingVectorizedMismatch(a, b);
            } catch (Exception ex) {
                log.warn("error while checks content equality", ex);
            }
        }

        return contentEqualLoop0(a, b);
    }

    @SuppressWarnings("unchecked")
    private static <T extends ZByteStore<?> & HasUncheckedRandomInput> boolean contentEqualLoop0(ZByteStore<?> a, ZByteStore<?> b) {
        long alen = a.readAvailable();
        long blen = b.readAvailable();
        if (a instanceof HasUncheckedRandomInput && b instanceof HasUncheckedRandomInput) {
            if (alen < blen) {
                return contentEqualLoopUnchecked((T) b, (T) a, blen, alen);
            } else {
                return contentEqualLoopUnchecked((T) a, (T) b, alen, blen);
            }
        }
        if (alen < blen) {
            return contentEqualLoop(b, a, blen, alen);
        } else {
            return contentEqualLoop(a, b, alen, blen);
        }
    }

    private static <T extends ZByteStore<?> & HasUncheckedRandomInput> boolean contentEqualLoopUnchecked(T a, T b, long alen, long blen) {
        assert SKIP_ASSERT || alen >= blen;
        UncheckedRandomInput ain = a.acquireUncheckedInput();
        UncheckedRandomInput bin = b.acquireUncheckedInput();
        long apos = a.readPosition();
        long bpos = b.readPosition();
        long i = 0;
        for (; i < blen - 7; i += 8) {
            if (ain.readLong(apos + i) != bin.readLong(bpos + i)) {
                return false;
            }
        }
        for (; i < blen; i++) {
            if (ain.readByte(apos + i) != bin.readByte(bpos + i)) {
                return false;
            }
        }

        // check zeros
        for (; i < alen - 7; i += 8) {
            if (ain.readLong(apos + i) != 0) {
                return false;
            }
        }
        for (; i < alen; i++) {
            if (ain.readByte(apos + i) != 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean contentEqualLoop(ZByteStore<?> a, ZByteStore<?> b, long alen, long blen)
            throws IllegalStateException, DecoratedBufferOverflowException {
        long apos = a.readPosition();
        long bpos = b.readPosition();
        long i = 0;
        for (; i < blen - 7; i += 8) {
            if (a.readLong(apos + i) != b.readLong(bpos + i)) {
                return false;
            }
        }
        for (; i < blen; i++) {
            if (a.readByte(apos + i) != b.readByte(bpos + i)) {
                return false;
            }
        }

        // check zeros
        for (; i < alen - 7; i += 8) {
            if (a.readLong(apos + i) != 0) {
                return false;
            }
        }
        for (; i < alen; i++) {
            if (a.readByte(bpos + i) != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the contents are equal using VECTORIZED_MISMATCH, use AVX instruction.
     *
     * @param left  byte data on left
     * @param right byte data on right
     */
    private static boolean java11ContentEqualsUsingVectorizedMismatch(ZByteStore<?> left, ZByteStore<?> right) {
        try {
            Object leftObj;
            long leftOffset;
            if (left.isNative()) {
                leftObj = null;
                leftOffset = left.addressForRead(left.readPosition());
            } else {
                ZByteStore<?> store = left.byteStore();
                if (!(store instanceof OnHeapByteStore)) {
                    return false;
                }
                OnHeapByteStore<?> heapByteStore = (OnHeapByteStore<?>) store;
                leftObj = heapByteStore.actualUnderlyingObject();
                leftOffset = heapByteStore.dataOffset() + left.readPosition();
            }
            Object rightObj;
            long rightOffset;
            if (right.isNative()) {
                rightObj = null;
                rightOffset = right.addressForRead(right.readPosition());
            } else {
                ZByteStore<?> store = right.byteStore();
                if (!(store instanceof OnHeapByteStore)) {
                    return false;
                }
                OnHeapByteStore<?> heapByteStore = (OnHeapByteStore<?>) store;
                rightObj = heapByteStore.actualUnderlyingObject();
                rightOffset = heapByteStore.dataOffset() + right.readPosition();
            }

            int length = (int) left.readAvailable();
            int invoke = (int) VECTORIZED_MISMATCH_METHOD.invoke(leftObj, leftOffset, rightObj, rightOffset, length, 0);
            if (invoke >= 0) {
                return false;
            }

            int remaining = length - ~invoke;
            for (; remaining < length; remaining++) {
                if (left.readByte(left.readPosition() + remaining) != right.readByte(right.readPosition() + remaining)) {
                    return false;
                }
            }
            return true;
        } catch (Throwable cause) {
            log.warn("error", cause);
            return false;
        }
    }

    public static long padOffset(long from) {
        return (-from) & 0x3f;
    }

}
