package org.zarp.bytes;

import org.zarp.core.annotations.NonNegative;

/**
 * Provides methods for reading data (byte, short, int, float, double) from an input source.
 * This is a high-performance interface for random data access.
 * Implementations are expected do not care about bounds, safety checks, the caller must validate conditions.
 */
public interface UncheckedRandomInput {

    /**
     * Reads a byte at the given offset.
     *
     * @param offset logical position within input
     * @return byte value
     */
    byte readByte(@NonNegative long offset);

    /**
     * Reads a short at the given offset.
     *
     * @param offset logical position within input
     * @return short value
     */
    short readShort(@NonNegative long offset);

    /**
     * Reads a 32-bit integer at the given offset.
     *
     * @param offset logical position within input
     * @return integer value
     */
    int readInt(@NonNegative long offset);

    /**
     * Reads a 64-bit long at the given offset.
     *
     * @param offset logical position within input
     * @return long value
     */
    long readLong(@NonNegative long offset);

    /**
     * Reads a 32-bit floating at the given offset.
     *
     * @param offset logical position within input
     * @return float value
     */
    default float readFloat(@NonNegative long offset) {
        return Float.intBitsToFloat(readInt(offset));
    }

    /**
     * Reads a 64-bit floating at the given offset.
     *
     * @param offset logical position within input
     * @return double value
     */
    default double readDouble(@NonNegative long offset) {
        return Double.longBitsToDouble(readLong(offset));
    }

}
