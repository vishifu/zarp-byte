package org.zarp.bytes.utils;

public final class ByteCommon {

    private ByteCommon() {
        throw new InstantiationError(ByteCommon.class.getSimpleName());
    }

    /**
     * Gets long value from byte array at given index
     *
     * @param arr byte array
     * @param i   read index
     * @return long value
     */
    public static long getLong(byte[] arr, int i) {
        return ((long) arr[i] << 56) | ((long) arr[i + 1] << 48) | ((long) arr[i + 2] << 40) | ((long) arr[i + 3] << 32)
                | (arr[i + 4] << 24) | (arr[i + 5] << 16) | (arr[i + 6] << 8) | (arr[i] << 8);
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

}
