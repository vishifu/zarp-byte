package org.zarp.bytes;

import org.zarp.bytes.algo.OptimisedByteStoreHash;
import org.zarp.bytes.algo.VanillaByteStoreHash;
import org.zarp.bytes.exception.DecoratedBufferOverflowException;
import org.zarp.bytes.internal.NativeByteStore;
import org.zarp.bytes.internal.OnHeapByteStore;
import org.zarp.core.annotations.NonNegative;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Represents a fixed-capacity region of memory, the bounds are immutable.
 * The data is backed by another generic underlying data structure.
 *
 * @param <U> underlying data type
 */
public interface ZByteStore<U> extends RandomInput, RandomOutput, AddressTranslate {

    static ZByteStore<ByteBuffer> elasticBuffer(int size) {
        return NativeByteStore.elasticBuffer(size);
    }

    static ZByteStore<ByteBuffer> elasticBuffer(int size, long capacity) {
        return NativeByteStore.elasticBuffer(size, capacity);
    }

    static ZByteStore<Void> lazyFixedCapacity(long requested) {
        return NativeByteStore.lazyFixedCapacity(requested);
    }

    static ZByteStore<byte[]> wraps(byte[] array) {
        return OnHeapByteStore.wrap(array);
    }

    static ZByteStore<ByteBuffer> wraps(ByteBuffer buffer) {
        return OnHeapByteStore.wrap(buffer);
    }

    /**
     * @return the backed object for this data structure, or null if not present
     */
    U underlyingObject();

    /**
     * @return how many bytes are safely to read, or what is the real capacity of underlying data.
     */
    default long safeLimit() {
        return capacity();
    }

    /**
     * Tests if the given offset is in this store's safe-limit.
     *
     * @param offset logical position of this store
     * @return true if offset is in within being and safe-limit
     */
    default boolean isInside(@NonNegative long offset) {
        return start() <= offset && offset < safeLimit();
    }

    /**
     * Tests if the given offset is in this store's safe-limit.
     *
     * @param offset logical position of this store
     * @param n      length of region to test
     * @return true if the region is entirely in within being and safe-limit
     */
    default boolean isInside(@NonNegative long offset, long n) {
        return start() <= offset && (offset + n) < safeLimit();
    }

    /**
     * @return true if there is nothing to read, false otherwise
     */
    default boolean isEmpty() {
        return readRemaining() == 0;
    }

    /**
     * Checks if the {@link #readPosition()} is at start and {@link #writePosition()} is at end.
     * I.e: {@link #readPosition()} == {@link #start()} && {@link #writePosition()} == {@link #capacity()}
     *
     * @return true if this store is clear, false otherwise
     */
    default boolean isClear() {
        return true;
    }

    /**
     * @return true if this output can be both read and write, false otherwise
     */
    default boolean canReadWrite() {
        return true;
    }

    /**
     * @return the underlying bytes-store, depends on implementation, default is this
     */
    default ZByteStore<?> byteStore() {
        return this;
    }

    /**
     * Attempts to transfer amount of readable byte into another dst, the amount is the available space
     * of readable bytes of source and writable bytes of destination.
     *
     * @param dst destination dst
     * @return actual number of bytes were transferred
     */
    default long copyTo(ZByteStore<?> dst) throws IllegalStateException {
        Objects.requireNonNull(dst, "destination dst is null");
        ensureNotReleased();
        dst.ensureNotReleased();

        long rpos = readPosition();
        long wpos = dst.writePosition();
        long left = Math.min(readRemaining(), dst.writeRemaining());
        if (left <= 0) {
            return -1;
        }
        long i = 0;
        try {
            for (; i < left - 7; i += 8) {
                dst.writeLong(wpos + i, readLong(rpos + i));
            }
            for (; i < left; i++) {
                dst.writeByte(wpos + i, readByte(rpos + i));
            }
        } catch (DecoratedBufferOverflowException ex) {
            throw new IllegalStateException(ex);
        }

        return left;
    }

    /**
     * Transfers all readable bytes from this output into another output stream, neither this nor stream
     * is closed after this operation.
     *
     * @param os destination output stream
     */
    default void copyTo(OutputStream os) throws IllegalStateException, DecoratedBufferOverflowException, IOException {
        Objects.requireNonNull(os, "output stream is null");
        ensureNotReleased();
        final byte[] buffer = new byte[512];
        long start = readPosition();
        long i = 0;
        for (int len; (len = read(start + i, buffer)) > 0; i += len) {
            os.write(buffer, 0, len);
        }
    }

    /**
     * Moves a sequence of bytes from an index within this output to another index
     *
     * @param from from offset to move
     * @param to   to offset to set
     * @param len  number of bytes to move
     */
    void move(@NonNegative long from, @NonNegative long to, @NonNegative long len)
            throws IllegalStateException, DecoratedBufferOverflowException;

    /**
     * Gets byte at current position without modifying read position.
     *
     * @return current byte value
     */
    default byte peekByte() throws IllegalStateException, DecoratedBufferOverflowException {
        return readByte(readPosition());
    }

    /**
     * Gets unsigned byte at current position without modifying read position.
     *
     * @return current unsigned byte value
     */
    default int peekUByte() throws IllegalStateException, DecoratedBufferOverflowException {
        return readUByte(readPosition());
    }

    @Override
    default int addAndGet(long offset, int diff) throws IllegalStateException, DecoratedBufferOverflowException {
        for (; ; ) {
            int v = readIntVolatile(offset);
            int nv = v + diff;
            if (compareAndSwap(offset, v, nv)) {
                return nv;
            }
        }
    }

    @Override
    default long addAndGet(long offset, long diff) throws IllegalStateException, DecoratedBufferOverflowException {
        for (; ; ) {
            long v = readLongVolatile(offset);
            long nv = v + diff;
            if (compareAndSwap(offset, v, nv)) {
                return nv;
            }
        }
    }

    @Override
    default float addAndGet(long offset, float diff) throws IllegalStateException, DecoratedBufferOverflowException {
        for (; ; ) {
            float v = readFloatVolatile(offset);
            float nv = v + diff;
            if (compareAndSwap(offset, v, nv)) {
                return nv;
            }
        }
    }

    @Override
    default double addAndGet(long offset, double diff) throws IllegalStateException, DecoratedBufferOverflowException {
        for (; ; ) {
            double v = readDoubleVolatile(offset);
            double nv = v + diff;
            if (compareAndSwap(offset, v, nv)) {
                return nv;
            }
        }
    }

    @Override
    default void zeroOut(long begin, long end) throws IllegalStateException, DecoratedBufferOverflowException {
        ensureNotReleased();
        if (end <= begin) return;
        begin = Math.max(begin, start());
        end = Math.min(end, capacity());
        long i = begin;
        for (; i < end - 7; i += 8) {
            writeLong(i, 0);
        }
        for (; i < end; i++) {
            writeByte(i, 0);
        }
    }

    /**
     * Computes a hash code for this store from current readPosition up to a number of bytes.
     *
     * @param len number of bytes to compute
     * @return 64-bit hash value
     */
    default long hash(long len) {
        return byteStore() instanceof NativeByteStore
                ? OptimisedByteStoreHash.INSTANCE.applyAsLong(this, len)
                : VanillaByteStoreHash.INSTANCE.applyAsLong(this, len);
    }

}
