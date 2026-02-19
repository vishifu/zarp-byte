package org.zarp.bytes.internal;

import org.zarp.bytes.AddressTranslate;
import org.zarp.bytes.ZByteStore;
import org.zarp.bytes.exception.DecoratedBufferOverflowException;
import org.zarp.bytes.utils.ByteFieldInfo;
import org.zarp.core.api.Jvm;
import org.zarp.core.conditions.Ints;
import org.zarp.core.conditions.Longs;
import org.zarp.core.memory.ZMemory;
import org.zarp.core.utils.DirectBufferUtil;
import org.zarp.core.utils.MathUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

import static java.lang.String.format;

/**
 * This represents an implementation of {@link ZByteStore} that allocate bytes on heap memory.
 * Accesses are performed using {@link sun.misc.Unsafe} for performance, and instance are reference counted.
 *
 * @param <U> underlying type
 */
public class OnHeapByteStore<U> extends AbstractByteStore<U> implements ZByteStore<U> {

    /* actual byte array backing of this store when wrapping heap memory */
    private Object realUnderlyingObject;

    /* original object passed to constructor, array or ByteBuffer */
    private final U underlyingObject;

    /* unsafe offset of the first byte of this store */
    private final int dataOffset;

    /* usable capacity of this store */
    private final long capacity;

    @SuppressWarnings("unchecked")
    protected OnHeapByteStore(ByteBuffer buffer) {
        super(false);
        buffer.order(ByteOrder.nativeOrder());
        this.underlyingObject = (U) buffer;
        this.realUnderlyingObject = buffer.array();
        this.dataOffset = buffer.arrayOffset() + ZMemory.ARRAY_BYTE_BASE_OFFSET;
        this.capacity = buffer.capacity();
    }

    protected OnHeapByteStore(byte[] array) {
        super(false);
        this.underlyingObject = (U) array;
        this.realUnderlyingObject = array;
        this.dataOffset = ZMemory.ARRAY_BYTE_BASE_OFFSET;
        this.capacity = array.length;
    }

    @SuppressWarnings("unchecked")
    protected OnHeapByteStore(Object object, int start, long cap) {
        super(false);
        this.underlyingObject = (U) object;
        this.realUnderlyingObject = object;
        this.dataOffset = start;
        this.capacity = cap;
    }

    public static OnHeapByteStore<byte[]> wrap(byte[] array) {
        Objects.requireNonNull(array);
        return new OnHeapByteStore<>(array);
    }

    public static OnHeapByteStore<ByteBuffer> wrap(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        return new OnHeapByteStore<>(buffer);
    }

    public static <T> OnHeapByteStore<T> of(T object, String groupName, int padding) {
        ByteFieldInfo lookup = ByteFieldInfo.lookup(object.getClass());
        int begin = (int) lookup.startOf(groupName);
        int length = (int) lookup.lengthOf(groupName);
        return new OnHeapByteStore<>(object, begin + padding, length - padding);
    }

    public long dataOffset() {
        return this.dataOffset;
    }

    @Override
    public long translate(long offset) {
        return dataOffset() + offset;
    }

    protected void ensureOffsetInBound(long offset) {
        if (offset < start() || offset > capacity()) {
            throw new IllegalArgumentException(format("%d is out of bound [%d, %d)", offset, start(), capacity()));
        }
    }

    public Object actualUnderlyingObject() {
        return this.realUnderlyingObject;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public void throwsIfRelease() throws IllegalStateException {
        if (this.realUnderlyingObject == null) {
            throw new IllegalStateException("Released resource");
        }
        super.throwsIfRelease();
    }

    @Override
    public U underlyingObject() {
        return this.underlyingObject;
    }

    @Override
    public long capacity() {
        return this.capacity;
    }

    @Override
    public void move(long from, long to, long len)
            throws IllegalStateException, DecoratedBufferOverflowException {
        if (from < 0 || to < 0) {
            throw new IllegalArgumentException(format("from %d or to %d index is negative", from, to));
        }
        if (len < 0 || len > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(format("required len %d must be in [0, 2^31 - 1)", len));
        }
        throwsIfRelease();
        try {
            memory().copyMemory(this.realUnderlyingObject, translate(from),
                    this.realUnderlyingObject, translate(to),
                    (int) len);
        } catch (NullPointerException ex) {
            throwsIfRelease();
            throw ex;
        }
    }

    @Override
    public byte readByte(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        return memory().readByte(this.realUnderlyingObject, translate(offset));
    }

    @Override
    public short readShort(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        return memory().readShort(this.realUnderlyingObject, translate(offset));
    }

    @Override
    public int readInt(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        return memory().readInt(this.realUnderlyingObject, translate(offset));
    }

    @Override
    public long readLong(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        return memory().readLong(this.realUnderlyingObject, translate(offset));
    }

    @Override
    public float readFloat(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        return memory().readFloat(this.realUnderlyingObject, translate(offset));
    }

    @Override
    public double readDouble(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        return memory().readDouble(this.realUnderlyingObject, translate(offset));
    }

    @Override
    public int read(long offset, byte[] dst, int dstBegin, int len)
            throws IllegalStateException, DecoratedBufferOverflowException {
        Objects.requireNonNull(dst);
        Ints.requireNonNegative(dstBegin);
        Ints.requireNonNegative(len);
        Longs.requireNonNegative(offset);
        if ((dstBegin + len) > dst.length) {
            throw new IllegalArgumentException(format("require range [%d, %d+%d) is out of dst range", dstBegin, dstBegin, len));
        }
        int left = (int) MathUtil.toUint32(Math.min(len, (readLimit() - offset)));
        transferMemory0(dst, dstBegin, offset, left);
        return left;
    }

    @Override
    public int read(long offset, ByteBuffer dst, int dstBegin, int len) {
        Objects.requireNonNull(dst);
        Ints.requireNonNegative(dstBegin);
        Ints.requireNonNegative(len);
        Longs.requireNonNegative(offset);
        if ((dstBegin + len) > dst.capacity()) {
            throw new IllegalArgumentException(format("require range [%d, %d+%d) is out of dst capacity", dstBegin, dstBegin, len));
        }
        int left = (int) MathUtil.toUint32(Math.min(len, (readLimit() - offset)));
        if (dst.isDirect()) {
            memory().copyMemory(this.realUnderlyingObject, translate(offset), DirectBufferUtil.addressOf(dst), left);
        } else {
            byte[] arr = dst.array();
            transferMemory0(arr, dstBegin, offset, left);
        }
        return left;
    }

    @Override
    public byte readByteVolatile(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        return memory().readVolatileByte(this.realUnderlyingObject, translate(offset));
    }

    @Override
    public short readShortVolatile(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        return memory().readVolatileShort(this.realUnderlyingObject, translate(offset));
    }

    @Override
    public int readIntVolatile(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        return memory().readVolatileInt(this.realUnderlyingObject, translate(offset));
    }

    @Override
    public long readLongVolatile(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        return memory().readVolatileLong(this.realUnderlyingObject, translate(offset));
    }

    @Override
    public void writeByte(long offset, byte i8)
            throws IllegalStateException, DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        memory().writeByte(this.realUnderlyingObject, translate(offset), i8);
    }

    @Override
    public void writeShort(long offset, short i16)
            throws IllegalStateException, DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        memory().writeShort(this.realUnderlyingObject, translate(offset), i16);

    }

    @Override
    public void writeInt(long offset, int i32)
            throws IllegalStateException, DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        memory().writeInt(this.realUnderlyingObject, translate(offset), i32);

    }

    @Override
    public void writeLong(long offset, long i64)
            throws IllegalStateException, DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        memory().writeLong(this.realUnderlyingObject, translate(offset), i64);

    }

    @Override
    public void writeFloat(long offset, float f32)
            throws IllegalStateException, DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        memory().writeFloat(this.realUnderlyingObject, translate(offset), f32);
    }

    @Override
    public void writeDouble(long offset, double f64)
            throws IllegalStateException, DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        memory().writeDouble(this.realUnderlyingObject, translate(offset), f64);
    }

    @Override
    public void write(long offset, byte[] src, int srcBegin, int len)
            throws IllegalStateException, DecoratedBufferOverflowException {
        Objects.requireNonNull(src);
        Ints.requireNonNegative(srcBegin);
        Ints.requireNonNegative(len);
        Longs.requireNonNegative(offset);
        if ((srcBegin + len) > src.length) {
            throw new IllegalArgumentException(format("require range [%d, %d+%d) is out of src length", srcBegin, srcBegin, len));
        }
        copyMemory0(offset, src, srcBegin, len);
    }

    @Override
    public void write(long offset, ByteBuffer src, int srcBegin, int len)
            throws IllegalStateException, DecoratedBufferOverflowException {
        Objects.requireNonNull(src);
        Ints.requireNonNegative(srcBegin);
        Ints.requireNonNegative(len);
        Longs.requireNonNegative(offset);
        assert this.realUnderlyingObject != null || this.dataOffset >= (Jvm.is64Bit() ? 12 : 8);
        if ((srcBegin + len) > src.capacity()) {
            throw new IllegalArgumentException(format("require range [%d, %d+%d) is out of src capacity", srcBegin, srcBegin, len));
        }
        if (src.isDirect()) {
            memory().copyMemory(DirectBufferUtil.addressOf(src) + srcBegin,
                    this.realUnderlyingObject, translate(offset),
                    len);
        } else {
            byte[] arr = src.array();
            copyMemory0(offset, arr, srcBegin, len);
        }
    }

    @Override
    public void writeIntOrdered(long offset, int i32)
            throws IllegalStateException, DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        memory().writeOrderedInt(this.realUnderlyingObject, translate(offset), i32);
    }

    @Override
    public void writeLongOrdered(long offset, long i64)
            throws IllegalStateException, DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        memory().writeOrderedLong(this.realUnderlyingObject, translate(offset), i64);
    }

    @Override
    public void writeIntVolatile(long offset, int i32)
            throws IllegalStateException, DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        memory().writeVolatileInt(this.realUnderlyingObject, translate(offset), i32);
    }

    @Override
    public void writeLongVolatile(long offset, long i64)
            throws IllegalStateException, DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        memory().writeVolatileLong(this.realUnderlyingObject, translate(offset), i64);
    }

    @Override
    public boolean compareAndSwap(long offset, int expected, int value)
            throws IllegalStateException, DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        return memory().compareAndSwapInt(this.realUnderlyingObject, translate(offset), expected, value);
    }

    @Override
    public boolean compareAndSwap(long offset, long expected, long value)
            throws IllegalStateException, DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        return memory().compareAndSwapLong(this.realUnderlyingObject, translate(offset), expected, value);
    }

    @Override
    public void testAndSet(long offset, int expected, int value)
            throws IllegalStateException, DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        memory().testAndSetInt(this.realUnderlyingObject, translate(offset), expected, value);
    }

    @Override
    public void testAndSet(long offset, long expected, long value)
            throws IllegalStateException, DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        memory().testAndSetLong(this.realUnderlyingObject, translate(offset), expected, value);
    }

    @Override
    public void nativeRead(long offset, long address, long len) throws IllegalStateException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public void nativeWrite(long address, long offset, long len)
            throws IllegalStateException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public long addressForRead(long offset) throws DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public long addressForWrite(long offset) throws DecoratedBufferOverflowException {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public boolean isHeap() {
        return true;
    }

    @Override
    public boolean isNative() {
        return false;
    }

    @Override
    protected void doFree0() throws IllegalStateException {
        this.realUnderlyingObject = null;
    }

    private void transferMemory0(byte[] dst, int dstBegin, long offset, int len) {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        if (this.realUnderlyingObject instanceof byte[]) {
            memory().copyMemory(this.realUnderlyingObject, offset + ZMemory.ARRAY_BYTE_BASE_OFFSET,
                    dst, dstBegin + ZMemory.ARRAY_BYTE_BASE_OFFSET,
                    len);
        } else {
            memory().copyMemory(this.realUnderlyingObject, translate(offset),
                    dst, dstBegin,
                    len);
        }
    }

    private void copyMemory0(long offset, byte[] src, int srcBegin, int len) {
        throwsIfRelease();
        ensureOffsetInBound(offset);
        if (this.realUnderlyingObject instanceof byte[]) {
            memory().copyMemory(src, srcBegin + ZMemory.ARRAY_BYTE_BASE_OFFSET,
                    this.realUnderlyingObject, offset + ZMemory.ARRAY_BYTE_BASE_OFFSET,
                    len);
        } else {
            memory().copyMemory(src, srcBegin,
                    this.realUnderlyingObject, translate(offset),
                    len);
        }
    }

}
