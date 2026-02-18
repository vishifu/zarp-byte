package org.zarp.bytes;

import org.zarp.bytes.utils.ByteCommon;
import org.zarp.core.annotations.NonNegative;
import org.zarp.core.conditions.Ints;

import java.nio.ByteBuffer;
import java.util.Objects;

import static java.lang.String.format;

/**
 * Represents a sequential base reading stream or buffer on binary data.
 * Methods typically advance {@link #readPosition()} by the number of bytes consumed.
 * <p>
 * Implementations might offer a lenient mode, which allow read data beyond the limit and padding with default value.
 * <p>
 * Operations usually throws {@link IllegalStateException} that indicating resources have been released/closed or
 * accessed by multiple-thread.
 */
public interface StreamInput extends StreamCommon {

    /**
     * Sets the lenient mode of this stream, if true when reading beyond the limit, padding with zero-value.
     *
     * @param mode lenient mode
     */
    void lenient(boolean mode);

    /**
     * @return true if lenient is active, false otherwise
     */
    boolean isLenient();

    /**
     * Sets the read position of this input.
     *
     * @param pos new position
     */
    void readPosition(@NonNegative long pos) throws IndexOutOfBoundsException;

    /**
     * Sets the limit of this input.
     *
     * @param limit new limit
     */
    void readLimit(@NonNegative long limit) throws IndexOutOfBoundsException;

    /**
     * Sets the read limit to capacity of this input.
     */
    default void readLimitToCapacity() throws IndexOutOfBoundsException {
        readLimit(capacity());
    }

    /**
     * Sets the read position and limit of this input based on specified position and remaining value.
     * This is likely as {@link #readLimit(long)} and {@link #readPosition(long)}
     *
     * @param pos new position
     * @param n   the remaining size, which is used to set limit
     */
    default void readPositionRemaining(@NonNegative long pos, long n) throws IndexOutOfBoundsException {
        readLimit(pos + n);
        readPosition(pos);
    }

    /**
     * Sets the read position and limit to capacity of this input.
     * This is likely as {@link #readLimitToCapacity()} and {@link #readPosition(long)}
     *
     * @param pos new position
     */
    default void readPositionUnlimit(long pos)
            throws IllegalStateException, IndexOutOfBoundsException {
        readLimitToCapacity();
        readPosition(pos);
    }

    /**
     * Advances the read position by {@code n} bytes, if the lenient mode is active, attempts to move beyond
     * the read limit are capped at the limit without error.
     *
     * @param n number of bytes to skip
     */
    void readAdvance(@NonNegative long n);

    /**
     * Obtains the current read position, optional skips padding bytes.
     * Useful when read data with padding for header
     *
     * @param skipPadding skip padding byte
     * @return current read position
     */
    default long readPositionForHeader(boolean skipPadding) {
        long pos = readPosition();
        if (skipPadding) {
            readAdvance(ByteCommon.padOffset(pos));
            return readPosition();
        }
        return pos;
    }

    /**
     * This is likely as {@code readAdvance(1)}, but do not do any checks.
     * Use this when you are surely that it is safe to do.
     */
    void uncheckedReadSkipOne();

    /**
     * This is likely as {@code readAdvance(-1)}, but do not do any checks.
     * Use this when you are surely that it is safe to do.
     */
    void uncheckedReadBackSkipOne();

    /**
     * Reads byte value from input stream.
     *
     * @return byte value
     */
    byte readByte() throws IllegalStateException;

    /**
     * Reads unsigned byte value from input stream.
     *
     * @return unsigned byte value
     */
    default int readUByte() throws IllegalStateException {
        return readByte() & 0xff;
    }

    /**
     * Reads boolean value from input stream.
     *
     * @return boolean value
     */
    default boolean readBool() throws IllegalStateException {
        byte b = readByte();
        return ByteCommon.byteToBool(b);
    }

    /**
     * Reads short value from input stream.
     *
     * @return short value
     */
    short readShort() throws IllegalStateException;

    /**
     * Reads short value from input stream.
     *
     * @return unsigned short value
     */
    default int readUShort() throws IllegalStateException {
        return readShort() & 0xffff;
    }

    /**
     * Reads 24-bit int value from input stream.
     *
     * @return 24-bit int
     */
    default int readInt24() throws IllegalStateException {
        return IS_LITTLE_ENDIAN
                ? (readShort() & 0xffff) | ((readByte() & 0xff) << 16)
                : ((readShort() & 0xffff) << 8) | (readByte() & 0xff);
    }

    /**
     * Reads unsigned 24-bit int value from input stream.
     *
     * @return unsigned 24-bit int
     */
    default int readUInt24() throws IllegalStateException {
        return readInt24() & 0xffff;
    }

    /**
     * Reads int value from input stream.
     *
     * @return int value
     */
    int readInt() throws IllegalStateException;

    /**
     * Reads int value from input stream.
     *
     * @return unsigned int value
     */
    default long readUInt() throws IllegalStateException {
        return readInt() & 0xffff;
    }

    /**
     * Reads long value from input stream.
     *
     * @return long value
     */
    long readLong() throws IllegalStateException;

    /**
     * Reads float value from input stream.
     *
     * @return float value
     */
    float readFloat() throws IllegalStateException;

    /**
     * Reads double value from input stream.
     *
     * @return double value
     */
    double readDouble() throws IllegalStateException;

    /**
     * Reads a long value, if where are less than 8 bytes then it is possible to read
     * and pads high bytes with zeros.
     *
     * @return long value, maybe pads with zeros
     */
    default long readLongIncomplete() throws IllegalStateException {
        long left = readRemaining();
        if (left >= 8) {
            return readLong();
        }
        if (left == 4) {
            return readInt();
        }
        long v = 0;
        for (int i = 0; i < left; i++) {
            v |= (long) readUByte() << (i * 8);
        }
        return v;
    }

    /**
     * Reads a volatile byte value from main memory of this input stream.
     *
     * @return volatile byte value
     */
    byte readByteVolatile() throws IllegalStateException;

    /**
     * Reads a volatile short value from main memory of this input stream.
     *
     * @return volatile short value
     */
    short readShortVolatile() throws IllegalStateException;

    /**
     * Reads a volatile int value from main memory of this input stream.
     *
     * @return volatile int value
     */
    int readIntVolatile() throws IllegalStateException;

    /**
     * Reads a volatile long value from main memory of this input stream.
     *
     * @return volatile long value
     */
    long readLongVolatile() throws IllegalStateException;

    /**
     * Reads a volatile float value from main memory of this input stream.
     *
     * @return volatile float value
     */
    default float readFloatVolatile() throws IllegalStateException {
        return Float.intBitsToFloat(readIntVolatile());
    }

    /**
     * Reads a volatile double value from main memory of this input stream.
     *
     * @return volatile double value
     */
    default double readDoubleVolatile() throws IllegalStateException {
        return Double.longBitsToDouble(readLongVolatile());
    }

    /**
     * Reads this input stream into entirely the given destination array.
     *
     * @param dst destination array
     * @return actual bytes read, or -1 if nothing to read
     */
    default int read(byte[] dst) throws IllegalStateException {
        return read(dst, 0, dst.length);
    }

    /**
     * Reads this input stream into a region of given destination array, starting to write at {@code dstBegin}.
     *
     * @param dst      destination array
     * @param dstBegin start position of destination
     * @param len      number of bytes to read
     * @return actual bytes read, or -1 if nothing to read
     */
    int read(byte[] dst, int dstBegin, int len) throws IllegalStateException;

    /**
     * Reads this input stream into a region of given destination array of chars, starting to write at {@code dstBegin}.
     *
     * @param dst      destination array
     * @param dstBegin start position of destination
     * @param len      number of bytes to read
     * @return actual bytes read, or -1 if nothing to read
     */
    int read(char[] dst, int dstBegin, int len) throws IllegalStateException;

    /**
     * Reads this input stream into a ByteBuffer, this doesn't modify the position of ByteBuffer.
     *
     * @param dst destination ByteBuffer
     * @return actual bytes read
     */
    default int read(ByteBuffer dst) throws IllegalStateException {
        return read(dst, dst.position(), dst.remaining());
    }

    /**
     * Reads this input stream into a region ByteBuffer, this doesn't modify the position of ByteBuffer.
     *
     * @param dst      destination ByteBuffer
     * @param dstBegin start position of ByteBuffer
     * @param len      number of bytes to read
     * @return actual bytes read
     */
    int read(ByteBuffer dst, int dstBegin, int len) throws IllegalStateException;

    /**
     * Reads this input stream into a native memory address.
     *
     * @param address memory address
     * @param len     number of bytes to read
     */
    void nativeRead(long address, long len) throws IllegalStateException;

    /**
     * Reads this input stream into a memory object.
     *
     * @param obj    target memory object
     * @param offset offset of memory object
     * @param len    number of bytes to read
     */
    default void nativeRead(Object obj, long offset, int len) throws IllegalStateException {
        if (isNative()) {
            readAdvance(len);
            long from = addressForRead(readPosition() - len);
            memory().copyMemory(from, obj, offset, len);
            return;
        }

        int i = 0;
        for (; i < len - 7; i += 8) {
            memory().writeLong(obj, offset + i, readLong());
        }
        for (; i < len; i++) {
            memory().writeLong(obj, offset + i, readByte());
        }
    }

}
