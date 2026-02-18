package org.zarp.bytes;

import org.zarp.bytes.utils.ByteCommon;
import org.zarp.core.annotations.NonNegative;
import org.zarp.core.conditions.Ints;
import org.zarp.core.utils.MathUtil;

import java.nio.ByteBuffer;
import java.util.Objects;

import static java.lang.String.format;

/**
 * Represents a sequential base writing stream or buffer on binary data into other data structures.
 * Methods typically advance {@link #readPosition()} by the number of bytes consumed.
 * <p>
 * Operations usually throws {@link IllegalStateException} that indicating resources have been released/closed or
 * accessed by multiple-thread.
 */
public interface StreamOutput extends StreamCommon {

    /**
     * Ensures that the buffer has enough space for requested size.
     * If the buffer is elastic, it will automatically grow to accommodate the requested size.
     * If the buffer is not elastic and there is insufficient space, throw error.
     *
     * @param requested requested size
     */
    void ensureCapacity(long requested) throws IllegalStateException, IndexOutOfBoundsException;

    @Override
    default boolean canWriteDirect(long n) {
        return false;
    }

    /**
     * Sets the current write position of this output.
     *
     * @param pos newly position
     */
    void writePosition(@NonNegative long pos) throws IndexOutOfBoundsException;

    /**
     * Sets the limit index of write position can be of this output.
     *
     * @param limit newly limit
     */
    void writeLimit(@NonNegative long limit) throws IndexOutOfBoundsException;

    /**
     * Advances the write position by {@code n} bytes, the eventual result of {@link #writePosition()}
     * must within {@link #writeLimit()}.
     * Any attempts to move position out of limit will be capped to limit.
     *
     * @param n number of bytes skip
     */
    void writeAdvance(@NonNegative long n);

    /**
     * Sets the write position and the remaining length of writable in this output.
     * This is likely as {@link #writeLimit(long)} and {@link #writePosition(long)}
     *
     * @param pos newly write position
     * @param n   newly write limit
     */
    default void writePositionRemaining(@NonNegative long pos, long n) throws IllegalStateException {
        writeLimit(pos + n);
        writePosition(pos);
    }

    /**
     * Obtains the current write position, optional skips padding bytes.
     * Useful when read data with padding for header
     *
     * @param skipPadding skip padding byte
     * @return current read position
     */
    default long writePositionForHeader(boolean skipPadding) throws IllegalStateException {
        long pos = writePosition();
        if (skipPadding) {
            writeAdvance(ByteCommon.padOffset(pos));
            return writePosition();
        }
        return pos;
    }

    /**
     * Writes a byte value into this output.
     *
     * @param i8 byte value
     */
    void writeByte(byte i8) throws IllegalStateException;

    /**
     * Writes a byte value into this output.
     *
     * @param i8 byte value presents as int
     */
    default void writeByte(int i8) throws IllegalStateException {
        writeByte(MathUtil.toInt8(i8));
    }

    /**
     * Writes an unsigned byte value into this output.
     *
     * @param u8 unsigned byte value
     */
    default void writeUByte(int u8) throws IllegalStateException {
        writeByte(MathUtil.toUint8(u8));
    }

    /**
     * Writes a bool value into this output.
     *
     * @param b bool value
     */
    default void writeBool(boolean b) throws IllegalStateException {
        writeByte(b ? (byte) 1 : (byte) 0);
    }

    /**
     * Writes a short value into this output.
     *
     * @param i16 short value
     */
    void writeShort(short i16) throws IllegalStateException;

    /**
     * Writes an unsigned short value into this output.
     *
     * @param u16 unsigned short value
     */
    default void writeUShort(int u16) throws IllegalStateException {
        writeShort((short) MathUtil.toUint16(u16));
    }

    /**
     * Writes a 24-bit int value into this output.
     *
     * @param i24 24-bit int value
     */
    default void writeInt24(int i24) throws IllegalStateException {
        if (IS_LITTLE_ENDIAN) {
            writeShort((short) i24);
            writeByte((byte) (i24 >> 16));
        } else {
            writeShort((short) (i24 >> 8));
            writeByte((byte) i24);
        }
    }

    /**
     * Writes 24-bit int value into this output.
     *
     * @param u24 unsigned 24-bit value
     */
    default void writeUInt24(int u24) throws IllegalStateException {
        writeInt24(MathUtil.toUint24(u24));
    }

    /**
     * Writes an int value into this output.
     *
     * @param i32 int value
     */
    void writeInt(int i32) throws IllegalStateException;

    /**
     * Writes an unsigned int value into this output.
     *
     * @param u32 unsigned int value
     */
    default void writeUInt(long u32) throws IllegalStateException {
        writeInt((int) MathUtil.toUint32(u32));
    }

    /**
     * Writes a long value into this output.
     *
     * @param i64 long value
     */
    void writeLong(long i64) throws IllegalStateException;

    /**
     * Writes a float value into this output.
     *
     * @param f32 float value
     */
    void writeFloat(float f32) throws IllegalStateException;

    /**
     * Writes a double value into this output.
     *
     * @param f64 double value
     */
    void writeDouble(double f64) throws IllegalStateException;

    /**
     * Writes an integer value with a memory barrier to ensure of stores.
     * Operation must be non-blocking
     *
     * @param i32 integer value
     */
    void writeIntOrdered(int i32) throws IllegalStateException;

    /**
     * Writes a long value with a memory barrier to ensure of stores.
     * Operation must be non-blocking
     *
     * @param i64 long value
     */
    void writeLongOrdered(long i64) throws IllegalStateException;

    /**
     * Writes a float value with a memory barrier to ensure of stores.
     * Operation must be non-blocking
     *
     * @param f32 float value
     */
    default void writeFloatOrdered(float f32) throws IllegalStateException {
        writeIntOrdered(Float.floatToIntBits(f32));
    }

    /**
     * Writes a double value with a memory barrier to ensure of stores.
     * Operation must be non-blocking
     *
     * @param f64 double value
     */
    default void writeDoubleOrdered(double f64) throws IllegalStateException {
        writeLongOrdered(Double.doubleToLongBits(f64));
    }

    /**
     * Writes entirely byte array into this output.
     *
     * @param src destination array
     * @return actual number bytes written
     */
    default int write(byte[] src) throws IllegalStateException {
        return write(src, 0, src.length);
    }

    /**
     * Writes a region of byte array into this output.
     *
     * @param src      destination array
     * @param srcBegin start position of destination
     * @param len      number of bytes to write
     * @return actual number bytes written
     */
    int write(byte[] src, int srcBegin, int len) throws IllegalStateException;

    /**
     * Writes a ByteBuffer into this output. This doesn't modify ByteBuffer position.
     *
     * @param src destination ByteBuffer
     * @return actual number bytes written
     */
    default int write(ByteBuffer src) throws IllegalStateException {
        return write(src, src.position(), src.remaining());
    }

    /**
     * Writes a region of ByteBuffer into this output.
     *
     * @param src      destination ByteBuffer
     * @param srcBegin ByteBuffer write position
     * @param len      number of bytes to write
     * @return actual number bytes written
     */
    int write(ByteBuffer src, int srcBegin, int len) throws IllegalStateException;

    /**
     * Writes data from native memory address into this output.
     *
     * @param address memory address
     * @param len     number of bytes to write
     */
    void nativeWrite(long address, long len) throws IllegalStateException;

    /**
     * Writes data from given object into this output.
     *
     * @param obj    target memory object
     * @param offset offset of memory object
     * @param len    number of bytes to write
     */
    default void nativeWrite(Objects obj, int offset, int len) throws IllegalStateException {
        if (isNative()) {
            writeAdvance(len);
            long dst = addressForWrite(writePosition() - len);
            memory().copyMemory(obj, offset, dst, len);
            return;
        }

        long i = 0;
        for (; i < len - 7; i += 8) {
            writeLong(memory().readLong(obj, offset + i));
        }
        for (; i < len; i++) {
            writeByte(memory().readByte(obj, offset + i));
        }
    }
}
