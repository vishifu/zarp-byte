package org.zarp.bytes.utils;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class ByteCommonTest {

    @Test
    void byte2Bool() {
        assertTrue(ByteCommon.byteToBool((byte) 1));
        assertTrue(ByteCommon.byteToBool((byte) 'y'));
        assertTrue(ByteCommon.byteToBool((byte) 'Y'));

        assertFalse(ByteCommon.byteToBool((byte) 0));
        assertFalse(ByteCommon.byteToBool((byte) 3));
        assertFalse(ByteCommon.byteToBool((byte) 'N'));
        assertFalse(ByteCommon.byteToBool((byte) 'A'));
    }

    @Test
    void getLong() {
        byte[] arr = new byte[16];
        ByteCommon.putLong(arr, 0, 33_234);
        ByteCommon.putLong(arr, 8, -42_4565);

        long a = ByteCommon.getLong(arr, 0);
        long b = ByteCommon.getLong(arr, 8);
        assertEquals(33_234, a);
        assertEquals(-42_4565, b);
    }

}
