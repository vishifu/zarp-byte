package org.zarp.bytes;

import org.zarp.bytes.exception.DecoratedBufferOverflowException;
import org.zarp.bytes.utils.ByteCommon;
import org.zarp.core.annotations.NonNegative;

import java.nio.ByteBuffer;

/**
 * Represents an input that provides methods for reading data from various types.
 * It allows to read data from an input source in a non-sequence manner (random-access),
 * which the data can be access via {@code offset} (relative to {@link #start()}).
 * <p>
 * Supports reading of primitive data type like {@code boolean, byte, int, long, etc}.
 * This also have capability of direct reading memory or reading after loading memory barrier.
 * <p>
 * Methods of this can throw exception to {@link DecoratedBufferOverflowException} for invalid offset or
 * {@link IllegalStateException} for accessing closed resource.
 * <p>
 * Note: implementation of this usually not care about thread-safe, if multiple threads interact
 * with this, it must be synchronized externally.
 */
public interface RandomInput extends RandomAccess {

    /**
     * Reads a byte value (8-bit) from given offset of this input.
     *
     * @param offset logical position within this input
     * @return byte value
     */
    byte readByte(@NonNegative long offset)
            throws IllegalStateException, DecoratedBufferOverflowException;

    /**
     * Reads an unsigned byte value (8-bit) from given offset of this input.
     *
     * @param offset logical position within this input
     * @return int value (represents for unsigned 8-bit value)
     */
    default int readUByte(@NonNegative long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        return readByte(offset) & 0xff;
    }

    /**
     * Reads a boolean value from given offset of this input.
     *
     * @param offset logical position within this input
     * @return boolean value
     */
    default boolean readBool(@NonNegative long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        return ByteCommon.byteToBool(readByte(offset));
    }

    /**
     * Reads a short value (16-bit) from given offset of this input.
     *
     * @param offset logical position within this input
     * @return short value
     */
    short readShort(@NonNegative long offset)
            throws IllegalStateException, DecoratedBufferOverflowException;

    /**
     * Reads an unsigned short value (16-bit) from given offset of this input.
     *
     * @param offset logical position within this input
     * @return int value (represents for unsigned 16-bit value)
     */
    default int readUShort(@NonNegative long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        return readShort(offset) & 0xffff;
    }

    /**
     * Reads an int 24-bit value from given offset of this input.
     *
     * @param offset logical position within this input
     * @return 24-bit integer value
     */
    default int readInt24(@NonNegative long offset) {
        return IS_LITTLE_ENDIAN
                ? (readShort(offset) & 0xffff) | ((readByte(offset + 2) & 0xff) << 16)
                : ((readShort(offset) & 0xffff) << 8) | (readByte(offset + 2) & 0xff);
    }

    /**
     * Reads an unsigned int 24-bit value from given offset of this input.
     *
     * @param offset logical position within this input
     * @return unsigned 24-bit integer value
     */
    default int readUInt24(@NonNegative long offset) {
        return readInt24(offset) & 0xffffff;
    }

    /**
     * Reads an int value (32-bit) from given offset of this input.
     *
     * @param offset logical position within this input
     * @return int value
     */
    int readInt(@NonNegative long offset)
            throws IllegalStateException, DecoratedBufferOverflowException;

    /**
     * Reads an unsigned int value (32-bit) from given offset of this input.
     *
     * @param offset logical position within this input
     * @return long value (represents for unsigned 32-bit value)
     */
    default long readUInt(@NonNegative long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        return readInt(offset) & 0xffffffffL;
    }

    /**
     * Reads a long value (64-bit) from given offset of this input.
     *
     * @param offset logical position within this input
     * @return long value
     */
    long readLong(@NonNegative long offset)
            throws IllegalStateException, DecoratedBufferOverflowException;

    /**
     * Reads a floating value (32-bit) from given offset of this input.
     *
     * @param offset logical position within this input
     * @return 32-bit floating value
     */
    float readFloat(@NonNegative long offset)
            throws IllegalStateException, DecoratedBufferOverflowException;

    /**
     * Reads a double value (64-bit) from given offset of this input.
     *
     * @param offset logical position within this input
     * @return 64-bit floating value
     */
    double readDouble(@NonNegative long offset)
            throws IllegalStateException, DecoratedBufferOverflowException;

    /**
     * Reads a long at given offset, if where are less than 8 bytes then it is possible to read
     * and pads high bytes with zeros.
     *
     * @param offset logical position within this output
     * @return long value, maybe pads with zeros
     */
    default long readLongIncomplete(@NonNegative long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        int left = (int) (readLimit() - offset);
        long v = 0;
        if (left >= 8) {
            return readLong(offset);
        }
        if (left == 4) {
            return readInt(offset);
        }
        for (int i = 0; i < left; i++) {
            v |= (long) readUByte(offset + i) << (i * 8);
        }
        return v;
    }

    /**
     * Reads a volatile byte value (8-bit) from given offset of this input.
     *
     * @param offset logical position within this input
     * @return volatile byte value
     */
    byte readByteVolatile(@NonNegative long offset)
            throws IllegalStateException, DecoratedBufferOverflowException;

    /**
     * Reads a volatile short value (16-bit) from given offset of this input.
     *
     * @param offset logical position within this input
     * @return volatile short value
     */
    short readShortVolatile(@NonNegative long offset)
            throws IllegalStateException, DecoratedBufferOverflowException;

    /**
     * Reads a volatile int value (32-bit) from given offset of this input.
     *
     * @param offset logical position within this input
     * @return volatile integer value
     */
    int readIntVolatile(@NonNegative long offset)
            throws IllegalStateException, DecoratedBufferOverflowException;

    /**
     * Reads a volatile long value (64-bit) from given offset of this input.
     *
     * @param offset logical position within this input
     * @return volatile long value
     */
    long readLongVolatile(@NonNegative long offset)
            throws IllegalStateException, DecoratedBufferOverflowException;

    /**
     * Reads a volatile float value (32-bit) from given offset of this input.
     *
     * @param offset logical position within this input
     * @return volatile 32-bit floating value
     */
    default float readFloatVolatile(@NonNegative long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        return Float.intBitsToFloat(readIntVolatile(offset));
    }

    /**
     * Reads a volatile double value (64-bit) from given offset of this input.
     *
     * @param offset logical position within this input
     * @return volatile 64-bit floating value
     */
    default double readDoubleVolatile(@NonNegative long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        return Double.longBitsToDouble(readLongVolatile(offset));
    }

    /**
     * Reads this input into another byte array, the data is read from {@code offset}.
     * By default, the implementation at {@link #read(long, byte[], int, int)}
     *
     * @param offset logical position within this input
     * @param dst    destination byte array
     * @return actual number of bytes that was read
     */
    default int read(@NonNegative long offset, byte[] dst)
            throws IllegalStateException, DecoratedBufferOverflowException {
        return read(offset, dst, 0, dst.length);
    }

    /**
     * Reads this input into another byte array, the data is read from {@code offset}.
     * By default, this leverage the implementation of {@link #read(long, byte[], int, int)}
     *
     * @param offset   logical position within this input
     * @param dst      destination byte array
     * @param dstBegin destination start position
     * @return actual number of bytes that was read
     */
    default int read(@NonNegative long offset, byte[] dst, int dstBegin)
            throws IllegalStateException, DecoratedBufferOverflowException {
        return read(offset, dst, dstBegin, (dst.length - dstBegin));
    }

    /**
     * Reads this input into another byte array, the data is read from {@code offset}
     * until fulfill the destination of reach {@link #readLimit()} of this.
     *
     * @param offset   logical position within this input
     * @param dst      destination byte array
     * @param dstBegin destination start position
     * @param len      number of bytes to read
     * @return actual number of bytes that was read
     */
    int read(@NonNegative long offset, byte[] dst, int dstBegin, int len)
            throws IllegalStateException, DecoratedBufferOverflowException;

    /**
     * Reads this input into another ByteBuffer, the data is read from {@code offset}.
     * By default, this leverage implementation of {@link #read(long, ByteBuffer, int, int)}.
     *
     * @param offset logical position within this input
     * @param dst    destination ByteBuffer
     * @return actual number of bytes that was can be read
     */
    default int read(@NonNegative long offset, ByteBuffer dst)
            throws IllegalStateException, DecoratedBufferOverflowException {
        return read(offset, dst, dst.position(), dst.remaining());
    }

    /**
     * Reads this input into another ByteBuffer, the data is read from {@code offset}.
     *
     * @param offset   logical position within this input
     * @param dst      destination ByteBuffer
     * @param dstBegin destination start position
     * @param len      number of bytes to read
     * @return actual number of bytes that was can be read
     */
    int read(@NonNegative long offset, ByteBuffer dst, int dstBegin, int len)
            throws IllegalStateException, DecoratedBufferOverflowException;

    /**
     * Reads data into native memory from this input at given offset
     *
     * @param offset  logical position within this input
     * @param address native memory address
     * @param len     number of bytes to read
     */
    void nativeRead(@NonNegative long offset, long address, long len)
            throws IllegalStateException;

    /**
     * Finds the first occurrence position of the give byte value in this input, starting to find from {@code offset}
     *
     * @param offset   logical position within this input
     * @param stopByte the byte to search
     * @return position of first occurrence for the stop byte, or -1 if not found
     */
    default long find(@NonNegative long offset, byte stopByte) throws IllegalStateException {
        ensureNotReleased();
        long hi = readRemaining();
        for (long i = offset; i < hi; i++) {
            if (readByte(i) == stopByte) {
                return i;
            }
        }
        return -1; // not found
    }

    /**
     * Computes a hash code from a number of bytes of this input swiftly.
     *
     * @param offset logical position of this input to start reading
     * @param len    number of bytes to read
     * @return a hash code represents for a range of bytes of this input
     */
    default int fmix(@NonNegative final long offset, @NonNegative final long len) {
        long h = 0;
        int i = 0;
        if (len >= 4) {
            h = readInt(offset + i);
            i += 4;
        }
        for (; i < len - 3; i += 4) {
            h *= 0x6d0f27bd;
            h += readInt(offset + i);
        }
        for (; i < len; i++) {
            h *= 0x6d0f27bd;
            h += readByte(offset + i);
        }
        h *= 0x855dd4dbL;
        return (int) (h ^ (h >> 32));
    }

}
