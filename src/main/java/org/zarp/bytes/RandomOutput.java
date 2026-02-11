package org.zarp.bytes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zarp.bytes.utils.ByteCommon;
import org.zarp.core.annotations.NonNegative;
import org.zarp.core.utils.MathUtil;

import java.nio.ByteBuffer;

/**
 * Represents an output that provides methods for writing data into various type.
 * It allows to write data into another output in a non-sequence manner (random-access),
 * which the data can be access via {@code offset} (relative to {@link #start()}).
 * <p>
 * Supports reading of primitive data type like {@code boolean, byte, int, long, etc}.
 * This also have capability of direct reading memory or reading after loading memory barrier.
 * <p>
 * Methods of this can throw exception to {@link IndexOutOfBoundsException} for invalid offset or
 * {@link IllegalStateException} for accessing closed resource.
 * <p>
 * Note: implementation of this usually not care about thread-safe, if multiple threads interact
 * with this, it must be synchronized externally.
 */
public interface RandomOutput extends RandomAccess {

    Logger log = LoggerFactory.getLogger(RandomOutput.class);

    /**
     * Writes a byte (8-bit) value at given offset of this output.
     *
     * @param offset logical position within this output
     * @param i8     byte value
     */
    void writeByte(@NonNegative long offset, byte i8)
            throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Writes an integer as byte (8-bit) value at given offset of this output.
     *
     * @param offset logical position within this output
     * @param i8     byte value represented as int
     */
    default void writeByte(@NonNegative long offset, int i8)
            throws IllegalStateException, IndexOutOfBoundsException {
        writeByte(offset, MathUtil.toInt8(i8));
    }

    /**
     * Writes an unsigned byte (8-bit) value at given offset of this output.
     *
     * @param offset logical position within this output
     * @param u8     unsigned byte value
     */
    default void writeUByte(@NonNegative long offset, int u8)
            throws IllegalStateException, IndexOutOfBoundsException {
        writeByte(offset, (byte) MathUtil.toUint8(u8));
    }

    /**
     * Writes a boolean value at given offset of this output.
     *
     * @param offset logical position within this output
     * @param b      bool value
     */
    default void writeByte(@NonNegative long offset, boolean b)
            throws IllegalStateException, IndexOutOfBoundsException {
        writeByte(offset, b ? 1 : 0);
    }

    /**
     * Writes a short (16-bit) value at given offset of this output.
     *
     * @param offset logical position within this output
     * @param i16    short value
     */
    void writeShort(@NonNegative long offset, short i16)
            throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Writes an integer as short (16-bit) value at given offset of this output.
     *
     * @param offset logical position within this output
     * @param i16    short value represented as int
     */
    default void writeShort(@NonNegative long offset, int i16)
            throws IllegalStateException, IndexOutOfBoundsException {
        writeShort(offset, (short) MathUtil.toInt16(i16));
    }

    /**
     * Writes an unsigned short (16-bit) value at given offset of this output.
     *
     * @param offset logical position within this output
     * @param u16    unsigned short value
     */
    default void writeUShort(@NonNegative long offset, int u16)
            throws IllegalStateException, IndexOutOfBoundsException {
        writeShort(offset, (short) MathUtil.toUint16(u16));
    }

    /**
     * Writes an integer 24-bit value at given offset of this output.
     *
     * @param offset logical position within this output
     * @param i24    24-bit value
     */
    default void writeInt24(@NonNegative long offset, int i24)
            throws IllegalStateException, IndexOutOfBoundsException {
        if (IS_LITTLE_ENDIAN) {
            writeShort(offset, i24);
            writeByte(offset + 2, (byte) (i24 >> 16));
        } else {
            writeShort(offset, i24 >> 8);
            writeByte(offset + 2, (byte) i24);
        }
    }

    /**
     * Writes an unsigned int 24-bit value at given offset of this output.
     *
     * @param offset logical position within this output
     * @param u24    unsigned 24-bit value
     */
    default void writeUInt24(@NonNegative long offset, int u24)
            throws IllegalStateException, IndexOutOfBoundsException {
        writeInt24(offset, MathUtil.toUint24(u24));
    }

    /**
     * Writes an int (32-bit) value at given offset of this output.
     *
     * @param offset logical position within this output
     * @param i32    int value
     */
    void writeInt(@NonNegative long offset, int i32);

    /**
     * Writes an unsigned int (32-bit) value at given offset of this output.
     *
     * @param offset logical position within this output
     * @param u32    unsigned int value
     */
    default void writeUInt(@NonNegative long offset, long u32)
            throws IllegalStateException, IndexOutOfBoundsException {
        writeInt(offset, (int) MathUtil.toUint32(u32));
    }

    /**
     * Writes a long (64-bit) value at given offset of this output.
     *
     * @param offset logical position within this output
     * @param i64    long value
     */
    void writeLong(@NonNegative long offset, long i64)
            throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Writes a float (32-bit) value at given offset of this output.
     *
     * @param offset logical position within this output
     * @param f32    32-bit floating value
     */
    void writeFloat(@NonNegative long offset, float f32)
            throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Writes a double (64-bit) value at given offset of this output.
     *
     * @param offset logical position within this output
     * @param f64    64-bit floating value
     */
    void writeDouble(@NonNegative long offset, double f64)
            throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Writes a 32-bit integer value at give offset with a memory barrier to ensure of stores.
     * Operation must be non-blocking
     *
     * @param offset logical position within this output
     * @param i32    integer value
     */
    void writeIntOrdered(@NonNegative long offset, int i32)
            throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Writes a 64-bit integer value at give offset with a memory barrier to ensure of stores.
     * Operation must be non-blocking
     *
     * @param offset logical position within this output
     * @param i64    long value
     */
    void writeLongOrdered(@NonNegative long offset, long i64)
            throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Writes a 32-bit floating value at give offset with a memory barrier to ensure of stores.
     * Operation must be non-blocking
     *
     * @param offset logical position within this output
     * @param f32    32-bit floating value
     */
    default void writeFloatOrdered(@NonNegative long offset, float f32)
            throws IllegalStateException, IndexOutOfBoundsException {
        writeInt(offset, Float.floatToIntBits(f32));
    }

    /**
     * Writes a 64-bit floating value at give offset with a memory barrier to ensure of stores.
     * Operation must be non-blocking
     *
     * @param offset logical position within this output
     * @param f64    64-bit floating value
     */
    default void writeDoubleOrdered(@NonNegative long offset, double f64)
            throws IllegalStateException, IndexOutOfBoundsException {
        writeLong(offset, Double.doubleToLongBits(f64));
    }

    /**
     * Writes a volatile 32-bit integer value at specified offset of this source.
     * The write is volatile, giving guarantee it is not cached and visible to all other client.
     *
     * @param offset logical position to write data
     * @param i32    integer value
     */
    void writeIntVolatile(@NonNegative long offset, int i32)
            throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Writes a volatile 64-bit integer value at specified offset of this source.
     * The write is volatile, giving guarantee it is not cached and visible to all other client.
     *
     * @param offset logical position to write data
     * @param i64    long value
     */
    void writeLongVolatile(@NonNegative long offset, long i64)
            throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Writes a volatile 32-bit float value at specified offset of this source.
     * The write is volatile, giving guarantee it is not cached and visible to all other client.
     *
     * @param offset logical position to write data
     * @param f32    32-bit floating value
     */
    default void writeFloatVolatile(@NonNegative long offset, float f32)
            throws IllegalStateException, IndexOutOfBoundsException {
        writeInt(offset, Float.floatToIntBits(f32));
    }

    /**
     * Writes a volatile 64-bit double value at specified offset of this source.
     * The write is volatile, giving guarantee it is not cached and visible to all other client.
     *
     * @param offset logical position to write data
     * @param f64    64-bit floating value
     */
    default void writeDoubleVolatile(@NonNegative long offset, double f64)
            throws IllegalStateException, IndexOutOfBoundsException {
        writeLong(offset, Double.doubleToLongBits(f64));
    }

    /**
     * Writes the entire of a source of byte array into this output.
     *
     * @param offset logical position to write data
     * @param src    source byte array
     */
    default void write(@NonNegative long offset, byte[] src)
            throws IllegalStateException, IndexOutOfBoundsException {
        write(offset, src, 0, src.length);
    }

    /**
     * Writes a source of byte array into this output.
     *
     * @param offset   logical position to write data
     * @param src      source byte array
     * @param srcBegin position starting to copy data from source
     * @param len      number of bytes to write
     */
    default void write(@NonNegative long offset, byte[] src, int srcBegin, int len)
            throws IllegalStateException, IndexOutOfBoundsException {
        if ((srcBegin + len) >= src.length) {
            throw newIllegalBound(srcBegin, len, src.length);
        }

        int i = 0;

        for (; i < len - 7; i++) {
            long v = ByteCommon.getLong(src, srcBegin + i);
            memory().writeLong(offset + i, v);
        }
        for (; i < len; i++) {
            memory().writeByte(offset + i, src[srcBegin + i]);
        }
    }

    /**
     * Writes a source of ByteBuffer into this output.
     *
     * @param offset logical position within this output
     * @param src    source data
     */
    default void write(@NonNegative long offset, ByteBuffer src) {
        int len = src.remaining();
        int i = 0;
        for (; i < len - 7; i += 8) {
            memory().writeLong(offset + i, src.getLong());
        }
        for (; i < len; i++) {
            memory().writeByte(offset + i, src.get());
        }
    }

    /**
     * Writes a source of ByteBuffer into this output, this operation do not modify ByteBuffer position.
     *
     * @param offset   logical position within this output
     * @param src      source data
     * @param srcBegin position starting to copy data from source
     * @param len      number of bytes to write
     */
    default void write(@NonNegative long offset, ByteBuffer src, int srcBegin, int len) {
        if ((srcBegin + len) >= src.capacity()) {
            throw newIllegalBound(srcBegin, len, src.capacity());
        }

        int i = 0;
        for (; i < len - 7; i += 8) {
            memory().writeLong(offset + i, src.getLong(srcBegin + i));
        }
        for (; i < len; i++) {
            memory().writeByte(offset + i, src.get(srcBegin + i));
        }
    }

    /**
     * Performs a compare-and-swap (CAS) operation for 32-bit value at given offset of this output.
     *
     * @param offset   logical offset within this output
     * @param expected expected value of current
     * @param value    newly value to set
     * @return true if newly value is set  at given offset, false otherwise
     */
    boolean compareAndSwap(@NonNegative long offset, int expected, int value)
            throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Performs a compare-and-swap (CAS) operation for 64-bit value at given offset of this output.
     *
     * @param offset   logical offset within this output
     * @param expected expected value of current
     * @param value    newly value to set
     * @return true if newly value is set  at given offset, false otherwise
     */
    boolean compareAndSwap(@NonNegative long offset, long expected, long value)
            throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Performs a compare-and-swap (CAS) operation for 32-bit floating value at given offset of this output.
     *
     * @param offset   logical offset within this output
     * @param expected expected value of current
     * @param value    newly value to set
     * @return true if newly value is set  at given offset, false otherwise
     */
    default boolean compareAndSwap(@NonNegative long offset, float expected, float value)
            throws IllegalStateException, IndexOutOfBoundsException {
        return compareAndSwap(offset, Float.floatToIntBits(expected), Float.floatToIntBits(value));
    }

    /**
     * Performs a compare-and-swap (CAS) operation for 64-bit floating value at given offset of this output.
     *
     * @param offset   logical offset within this output
     * @param expected expected value of current
     * @param value    newly value to set
     * @return true if newly value is set  at given offset, false otherwise
     */
    default boolean compareAndSwap(@NonNegative long offset, double expected, double value)
            throws IllegalStateException, IndexOutOfBoundsException {
        return compareAndSwap(offset, Double.doubleToLongBits(expected), Double.doubleToLongBits(value));
    }

    /**
     * Tests if current value at given offset equals to expected, if so, set the new value for it.
     *
     * @param offset   logical offset within this output
     * @param expected expected value of current
     * @param value    newly value to set
     */
    void testAndSet(@NonNegative long offset, int expected, int value)
            throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Tests if current value at given offset equals to expected, if so, set the new value for it.
     *
     * @param offset   logical offset within this output
     * @param expected expected value of current
     * @param value    newly value to set
     */
    void testAndSet(@NonNegative long offset, long expected, long value)
            throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Atomically adds a 32-bit value to current value at given offset and get the eventual result back.
     *
     * @param offset logical position within this output
     * @param diff   adding value
     * @return sum of original value with adding value
     */
    int addAndGet(@NonNegative long offset, int diff)
            throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Atomically adds a 64-bit value to current value at given offset and get the eventual result back.
     *
     * @param offset logical position within this output
     * @param diff   adding value
     * @return sum of original value with adding value
     */
    long addAndGet(@NonNegative long offset, long diff)
            throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Atomically adds a 32-bit floating value to current value at given offset and get the eventual result back.
     *
     * @param offset logical position within this output
     * @param diff   adding value
     * @return sum of original value with adding value
     */
    float addAndGet(@NonNegative long offset, float diff)
            throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Atomically adds a 64-bit floating value to current value at given offset and get the eventual result back.
     *
     * @param offset logical position within this output
     * @param diff   adding value
     * @return sum of original value with adding value
     */
    double addAndGet(@NonNegative long offset, double diff)
            throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Fills the specified range [begin, end) of this output with zeros.
     *
     * @param begin offset start the range
     * @param end   offset end the range
     */
    void zeroOut(@NonNegative long begin, @NonNegative long end)
            throws IllegalStateException, IndexOutOfBoundsException;

    /**
     * Writes data directly from native memory address into this output at given offset.
     *
     * @param address native memory address
     * @param offset  logical position within this output
     * @param len     number of bytes to write
     */
    void nativeWrite(@NonNegative long address, @NonNegative long offset, @NonNegative long len)
            throws IllegalStateException, IndexOutOfBoundsException;

}
