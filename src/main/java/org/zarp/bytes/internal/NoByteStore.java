package org.zarp.bytes.internal;

import org.zarp.bytes.ZByteStore;
import org.zarp.core.api.ZPlatform;
import org.zarp.core.io.ReferenceCountListener;
import org.zarp.core.io.ReferenceOwnable;

import java.nio.ByteBuffer;

/**
 * An immutable dummy byte-store with zero capacity used as a placeholder.
 */
public final class NoByteStore implements ZByteStore<Void> {

    public static final ZByteStore<?> NO_BYTE_STORE = new NoByteStore();
    public static final long NO_PAGE;

    private static final UnsupportedOperationException UNSUPPORTED = new UnsupportedOperationException("No support!");

    static {
        NO_PAGE = ZPlatform.memory().allocate(ZPlatform.pageSize());
    }

    @SuppressWarnings("unchecked")
    public static <T> ZByteStore<T> noByteStore() {
        return (ZByteStore<T>) NO_BYTE_STORE;
    }

    @Override
    public Void underlyingObject() {
        return null;
    }

    @Override
    public void move(long from, long to, long len)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public byte readByte(long offset)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public short readShort(long offset)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public int readInt(long offset)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public long readLong(long offset)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public float readFloat(long offset)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public double readDouble(long offset)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public byte readByteVolatile(long offset)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public short readShortVolatile(long offset)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public int readIntVolatile(long offset)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public long readLongVolatile(long offset)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public int read(long offset, byte[] dst, int dstBegin, int len)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public int read(long offset, ByteBuffer dst, int dstBegin, int len)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public void nativeRead(long offset, long address, long len)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public void writeByte(long offset, byte i8)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public void writeShort(long offset, short i16)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public void writeInt(long offset, int i32)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public void writeLong(long offset, long i64)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public void writeFloat(long offset, float f32)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public void writeDouble(long offset, double f64)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public void writeIntOrdered(long offset, int i32)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public void writeLongOrdered(long offset, long i64)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public void writeIntVolatile(long offset, int i32)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public void writeLongVolatile(long offset, long i64)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public void write(long offset, byte[] src, int srcBegin, int len) throws IllegalStateException, IndexOutOfBoundsException {

    }

    @Override
    public void write(long offset, ByteBuffer src, int srcBegin, int len) throws IllegalStateException, IndexOutOfBoundsException {

    }

    @Override
    public boolean compareAndSwap(long offset, int expected, int value)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public boolean compareAndSwap(long offset, long expected, long value)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public void testAndSet(long offset, int expected, int value)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public void testAndSet(long offset, long expected, long value)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public void nativeWrite(long address, long offset, long len)
            throws IllegalStateException, IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public long addressForRead(long offset) throws IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public long addressForWrite(long offset) throws IndexOutOfBoundsException {
        throw UNSUPPORTED;
    }

    @Override
    public boolean isHeap() {
        return false;
    }

    @Override
    public boolean isNative() {
        return false;
    }

    @Override
    public int refCount() throws IllegalStateException {
        return 0;
    }

    @Override
    public void reserve(ReferenceOwnable owner) throws IllegalStateException {
        // nop
    }

    @Override
    public boolean tryReserve(ReferenceOwnable owner) {
        return false;
    }

    @Override
    public void release(ReferenceOwnable owner) throws IllegalStateException {
        // nop
    }

    @Override
    public void releaseLast(ReferenceOwnable owner) throws IllegalStateException {
        // nop
    }

    @Override
    public void addListener(ReferenceCountListener listener) {
        // nop
    }

    @Override
    public void removeListener(ReferenceCountListener listener) {
        // nop
    }

}
