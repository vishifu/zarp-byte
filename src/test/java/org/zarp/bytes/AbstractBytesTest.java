package org.zarp.bytes;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zarp.core.utils.RandUtil;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

abstract class AbstractBytesTest {

    static ZBytes<?> zbytes;
    static ZBytes<?> resizingZBytes;

    @BeforeEach
    void setUp() {
        zbytes.clear();
    }

    @Test
    void writeAndRead_offset() {
        byte b = 0xa;
        short i16 = 0xaa;
        int i32 = 0xabcd;
        long i64 = 0xffabcd;
        float f32 = 0xff;
        double f64 = 0xffffff;

        zbytes.writeByte(1, b);
        zbytes.writeShort(2, i16);
        zbytes.writeInt(4, i32);
        zbytes.writeLong(8, i64);
        zbytes.writeFloat(16, f32);
        zbytes.writeDouble(20, f64);

        assertEquals(b, zbytes.readByte(1));
        assertEquals(i16, zbytes.readShort(2));
        assertEquals(i32, zbytes.readInt(4));
        assertEquals(i64, zbytes.readLong(8));
        assertEquals(f32, zbytes.readFloat(16));
        assertEquals(f64, zbytes.readDouble(20));
    }

    @Test
    void writeAndReadBatch_offset() {
        byte[] rands = RandUtil.randBytes(100);
        zbytes.write(0, rands);
        ByteBuffer randBuf = ByteBuffer.wrap(rands);
        zbytes.write(100, randBuf);

        byte[] ret1 = new byte[100];
        zbytes.read(0, ret1);
        assertArrayEquals(rands, ret1);
        zbytes.read(100, ret1);
        assertArrayEquals(rands, ret1);

        byte[] ret2 = new byte[50];
        zbytes.write(200, rands, 0, 50);
        zbytes.read(200, ret2, 0, 50);
        assertArrayEquals(Arrays.copyOfRange(rands, 0, 50), ret2);
    }

    @Test
    void readAndWrite() {
        byte b = 0xa;
        short i16 = 0xaa;
        int i32 = 0xabcd;
        long i64 = 0xffabcd;
        float f32 = 0xff;
        double f64 = 0xffffff;

        zbytes.writeByte(b);
        zbytes.writeShort(i16);
        zbytes.writeInt(i32);
        zbytes.writeLong(i64);
        zbytes.writeFloat(f32);
        zbytes.writeDouble(f64);

        assertEquals(27, zbytes.writePosition());
        assertEquals(0, zbytes.readPosition());

        assertEquals(b, zbytes.readByte());
        assertEquals(i16, zbytes.readShort());
        assertEquals(i32, zbytes.readInt());
        assertEquals(i64, zbytes.readLong());
        assertEquals(f32, zbytes.readFloat());
        assertEquals(f64, zbytes.readDouble());

        assertEquals(27, zbytes.writePosition());
        assertEquals(27, zbytes.readPosition());
    }

    @Test
    void readAndWriteBatch() {
        // write
        byte[] rands = RandUtil.randBytes(100);
        zbytes.write(rands);
        assertEquals(100, zbytes.writePosition());

        ByteBuffer randBuf = ByteBuffer.wrap(rands);
        zbytes.write(randBuf);
        assertEquals(200, zbytes.writePosition());

        // read
        byte[] ret1 = new byte[100];
        zbytes.read(ret1);
        assertEquals(100, zbytes.readPosition());
        assertArrayEquals(rands, ret1);

        zbytes.read(ret1);
        assertEquals(200, zbytes.readPosition());
        assertArrayEquals(rands, ret1);

        // write and read partial
        byte[] ret2 = new byte[50];
        zbytes.write(rands, 0, 50);
        zbytes.read(ret2, 0, 50);
        assertArrayEquals(Arrays.copyOfRange(rands, 0, 50), ret2);
    }

    @Test
    void resizing() {
        Assumptions.assumeTrue(resizingZBytes.isElastic());

        byte[] rands = RandUtil.randBytes(5000);
        resizingZBytes.write(rands);
        byte[] ret = new byte[5000];
        resizingZBytes.read(ret);
        assertArrayEquals(rands, ret);
    }

}