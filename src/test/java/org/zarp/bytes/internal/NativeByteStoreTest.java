package org.zarp.bytes.internal;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class NativeByteStoreTest {

    @Test
    void fixed_readWrite() {
        NativeByteStore<Void> store = NativeByteStore.of(1024, true, false);

        assertFalse(store.isHeap());
        assertTrue(store.isNative());

        store.writeByte(0, (byte)'a');
        store.writeShort(1, (short) 0xab);
        store.writeInt(4, 0xaff);
        store.writeLong(8, 0xffabcd);
        store.writeFloat(16, 0xabcd);
        store.writeDouble(20, 0xffabcd);

        assertEquals('a', store.readByte(0));
        assertEquals(0xab, store.readShort(1));
        assertEquals(0xaff, store.readInt(4));
        assertEquals(0xffabcd, store.readLong(8));
        assertEquals(0xabcd, store.readFloat(16));
        assertEquals(0xffabcd, store.readDouble(20));

        byte[] arr = new byte[100];
        new Random().nextBytes(arr);
        store.write(100, arr);
        byte[] ret = new byte[100];
        store.read(100, ret);
        assertArrayEquals(arr, ret);

        ByteBuffer buf1 = ByteBuffer.wrap(arr);
        store.write(200, buf1);
        store.read(200, ret);
        assertArrayEquals(arr, ret);

        ByteBuffer buf2 = ByteBuffer.allocateDirect(100);
        buf2.put(0, arr);
        store.write(300, buf2);
        store.read(300, ret);
        assertArrayEquals(arr, ret);

        store.move(300, 400, 50);
        ret = new byte[50];
        store.read(400, ret);
        assertArrayEquals(Arrays.copyOfRange(arr, 0, 50), ret);
    }

}
