package org.zarp.bytes;

import org.zarp.bytes.exception.DecoratedBufferOverflowException;
import org.zarp.core.annotations.NonNegative;
import org.zarp.core.api.Jvm;
import org.zarp.core.internal.UnsafeMemory;
import org.zarp.core.memory.ZMemory;

import java.nio.ByteOrder;

/**
 * Represents a foundation interface for random-access byte of a buffer
 * (note that 'buffer' can indicate a sequence of bytes, they are interchangeable).
 * This defines position, capacity, and pointer manipulation.
 * <p>
 * This interface extends {@link RefInstance} for life-cycle management.
 */
public interface RandomAccess extends RefInstance {

    /* macros maximum supported capacity (roughly 8 exbibytes) for buffer */
    long DEFAULT_MAX_CAPACITY = Long.MAX_VALUE & ~0xf;
    /* macros supported max heap memory (roughly 2 GiB) for buffer */
    int MAX_HEAP_CAPACITY = Integer.MAX_VALUE & ~0xf;

    /* macros skip assertion */
    boolean SKIP_ASSERT = !Jvm.isAssertEnabled();

    /* macros check for little-endian of native byte order */
    boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    /* Gets the memory instance */
    default ZMemory memory() {
        return UnsafeMemory.INSTANCE;
    }

    /**
     * Gets the smallest position allowed in this buffer.
     *
     * @return start offset of this buffer.
     */
    default long start() {
        return 0L;
    }

    /**
     * Gets the maximum supported capacity default is as large as {@link #DEFAULT_MAX_CAPACITY}.
     *
     * @return capacity of this buffer.
     */
    default long capacity() {
        return DEFAULT_MAX_CAPACITY;
    }

    /**
     * For elastic buffer, this maybe less than {@link #capacity()} and can be growth on demand.
     * By default, value will be the same as {@link #capacity()}.
     *
     * @return current allocated size of this buffer.
     */
    default long size() {
        return capacity();
    }

    /**
     * Gets the current {@code read} pointer position.
     * Typically, {@code (start() <= readPosition() <= writePosition())} and {@code (readPosition() <= readLimit())}.
     * By default, read position is same as {@link #start()}
     *
     * @return current read pointer position
     */
    default long readPosition() {
        return start();
    }

    /**
     * Gets the current {@code write} pointer position.
     * Typically, {@code (readPosition() <= writePosition() <= writeLimit())}
     * By default, write position is same as {@link #start()}
     *
     * @return current write pointer position
     */
    default long writePosition() {
        return start();
    }

    /**
     * Gets the bound that a read pointer can be. If this resource is closed, this value is unspecified.
     * By default, the value is same as {@link #size()}.
     *
     * @return the highest position allowed for read pointer
     */
    default long readLimit() {
        return size();
    }

    /**
     * Gets the bound that a write pointer can be. If this resource is closed, this value is unspecified.
     * By default, the value is same as {@link #size()}.
     *
     * @return the highest position allowed for write pointer
     */
    default long writeLimit() {
        return size();
    }

    /**
     * Gets the available amount of bytes that can be safely read from this buffer.
     * If the resource is closed, this value is unspecified.
     * By default, the returned value is same as {@code (readLimit - readPosition)}
     *
     * @return number of bytes that can be safely read
     */
    default long readRemaining() {
        return readLimit() - readPosition();
    }

    /**
     * Gets the available amount of bytes that can be safely written into this buffer.
     * If the resource is closed, this value is unspecified.
     * By default, the returned value is same as {@code (writeLimit - writePosition)}
     *
     * @return number of bytes that can be safely written
     */
    default long writeRemaining() {
        return writeLimit() - writePosition();
    }

    /**
     * Gets the actual remaining number of bytes that can be read from this buffer with resizing.
     * If the resource is closed, this value is unspecified.
     * By default, the returned value is min of {@link #size()} and {@code readLimit - readPosition}
     *
     * @return number of bytes that can be safely read with resizing buffer
     */
    default long readAvailable() {
        return Math.min(size(), readLimit()) - readPosition();
    }

    /**
     * Gets the actual remaining number of bytes that can be written from this buffer with resizing.
     * If the resource is closed, this value is unspecified.
     * By default, the returned value is min of {@link #size()} and {@code writeLimit - writePosition}
     *
     * @return number of bytes that can be safely written with resizing buffer
     */
    default long writeAvailable() {
        return Math.min(size(), writeLimit()) - writePosition();
    }

    /**
     * Retrieves the underlying memory address for reading.
     *
     * @param offset logical position within this buffer relative to {@link #start()}
     * @return underlying address for reading
     * @throws DecoratedBufferOverflowException if offset is out of bounds of this buffer
     */
    long addressForRead(long offset) throws DecoratedBufferOverflowException;

    /**
     * Retrieves the underlying memory address for writing.
     *
     * @param offset logical position within this buffer relative to {@link #start()}
     * @return underlying address for writing
     * @throws DecoratedBufferOverflowException if offset is out of bounds of this buffer
     */
    long addressForWrite(long offset) throws DecoratedBufferOverflowException;

    /**
     * Tests if this input can be read directly from native memory.
     *
     * @return true if directly read from native memory, false otherwise
     */
    default boolean canReadDirect() {
        return canReadDirect(0);
    }

    /**
     * Tests if this input can be read directly from native memory.
     *
     * @param n required number of bytes to read
     * @return true if directly read from native memory, false otherwise
     */
    default boolean canReadDirect(@NonNegative long n) {
        return isNative() && readRemaining() >= n;
    }

    /**
     * Tests if this output can be written directly from native memory.
     *
     * @return true if directly read from native memory, false otherwise
     */
    default boolean canWriteDirect() {
        return canWriteDirect(0L);
    }

    /**
     * Tests if this output can be written directly from native memory.
     *
     * @param n required number of bytes to read
     * @return true if directly read from native memory, false otherwise
     */
    default boolean canWriteDirect(@NonNegative long n) {
        return isNative() && writeAvailable() >= n;
    }

    /**
     * Gets the byte-order of this buffer.
     * By default, the returned value is native-order (according to {@link ByteOrder#nativeOrder()}.
     *
     * @return byte order of this
     */
    default ByteOrder byteOrder() {
        return ByteOrder.nativeOrder();
    }

    /**
     * @return true if this is allocated on heap memory, false otherwise
     */
    boolean isHeap();

    /**
     * @return true if this is allocated on native memory, false otherwise
     */
    boolean isNative();

    /**
     * Creates a new exception report out of bound access.
     */
    default IllegalArgumentException newIllegalBound(int begin, int len, int bound) {
        return new IllegalArgumentException(String.format("Range [%d, %d+%d) is out of bound %d", begin, begin, len, bound));
    }
}
