package org.zarp.bytes;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assertions.*;

class NativeByteTest extends AbstractBytesTest{

    @BeforeAll
    static void beforeAll() {
        zbytes = ZBytes.elasticBuffer(1024, 1 << 16);
        assertTrue(zbytes.isNative());
        assertEquals(1 << 16, zbytes.capacity());

        resizingZBytes = ZBytes.elasticBuffer(1024, 1 << 16);
    }

    @AfterAll
    static void afterAll() {
        zbytes.releaseLast();
        resizingZBytes.releaseLast();
        ((NativeByte<?>) zbytes).throwsIfNotReleased();
        ((NativeByte<?>) resizingZBytes).throwsIfNotReleased();
    }

}
