package org.zarp.bytes.internal;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class OnHeapByteStoreTest {

    private static OnHeapByteStore<byte[]> store;

    @BeforeAll
    static void oneTimeSetUp() {
        store = OnHeapByteStore.wrap(new byte[1024]);
        assertTrue(store.isHeap());
        assertFalse(store.isNative());
        assertEquals(1024, store.capacity());
        assertEquals(0, store.start());
    }

    @AfterAll
    static void oneTimeTearDown() {
        store.doFree0();
    }

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void writeRead() {
        byte b = (byte) 'b';
        store.writeByte(1, b);
        assertEquals(b, store.readByte(1));

        int ub = 255;
        store.writeUByte(1, ub);
        assertEquals(ub, store.readUByte(1));

        short i16 = 16553;
        store.writeShort(2, i16);
        assertEquals(i16, store.readShort(2));

        int i32 = 6553533;
        store.writeInt(4, i32);
        assertEquals(i32, store.readInt(4));

        long i64 = 1L << 45;
        store.writeLong(8, i64);
        assertEquals(i64, store.readLong(8));

        float f32 = 78_453f;
        store.writeFloat(16, f32);
        assertEquals(f32, store.readFloat(16));

        double f64 = 1L << 60;
        store.writeDouble(20, f64);
        assertEquals(f64, store.readDouble(20));

        byte[] arr = new byte[500];
        new Random().nextBytes(arr);
        store.write(30, arr);
        byte[] ret = new byte[500];
        store.read(30, ret);
        assertArrayEquals(arr, ret);

        ByteBuffer bufRet = ByteBuffer.allocate(500);
        store.read(30, bufRet);
        assertArrayEquals(arr, bufRet.array());

        bufRet.rewind();
        store.write(100, bufRet);
        ByteBuffer bufRet2 = ByteBuffer.allocate(500);
        store.read(100, bufRet2);
        assertArrayEquals(arr, bufRet2.array());
    }

    @Test
    void equalsContent() {
        final byte[] arr = new byte[64];
        new Random().nextBytes(arr);
        OnHeapByteStore<byte[]> that = OnHeapByteStore.wrap(arr);
        OnHeapByteStore<byte[]> other = OnHeapByteStore.wrap(arr);
        assertEquals(that, other);

        ByteBuffer buf = ByteBuffer.wrap(arr);
        OnHeapByteStore<ByteBuffer> otherBuffer = OnHeapByteStore.wrap(buf);
        //noinspection AssertBetweenInconvertibleTypes
        assertEquals(that, otherBuffer);
    }

    @Test
    void moveContent() {
        final byte[] arr = new byte[64];
        new Random().nextBytes(arr);
        store.write(0, arr);
        store.move(0, 100, arr.length);

        byte[] ret = new byte[64];
        store.read(100, ret);
        assertArrayEquals(arr, ret);
    }


}