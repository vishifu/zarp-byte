package org.zarp.bytes.utils;

import org.junit.jupiter.api.Test;

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

}