package org.zarp.bytes.algo;

import org.zarp.bytes.ZByteStore;
import org.zarp.core.api.ZPlatform;
import org.zarp.core.memory.ZMemory;

import java.nio.BufferOverflowException;
import java.nio.ByteOrder;

import static org.zarp.bytes.algo.ByteStoreHash.agitate;

/**
 * Optimised hashing for {@link ZByteStore}.
 * <p>
 * this enumeration implementation for optimised hashing depending on the data size and byte resides in direct memory,
 * leverage system architecture details like endian for performance.
 */
public enum OptimisedByteStoreHash implements ByteStoreHash<ZByteStore<?>> {
    INSTANCE;

    /**
     * Native memory access
     */
    private static final ZMemory MEMORY = ZPlatform.memory();

    /**
     * System native byte order
     */
    private static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    /**
     * Offset used to select top four bytes of a long depending on system endian.
     */
    private static final int TOP_BYTES = IS_LITTLE_ENDIAN ? 4 : 0;

    @Override
    public long applyAsLong(ZByteStore<?> store) {
        return applyAsLong(store, store.readRemaining());
    }

    @Override
    public long applyAsLong(ZByteStore<?> store, long len) throws IllegalStateException, BufferOverflowException {
        if (len <= 0) {
            return 0;
        }
        if (len < 8) {
            return applyAsLong1To7(store, (int) len);
        }
        if (len == 8) {
            return applyAsLong8(store);
        }
        if (len < 16) {
            return applyAsLong9To16(store, (int) len);
        }
        if (len <= 32) {
            return applyAsLong17To32(store, (int) len);
        }
        if ((len & 31) == 0) {
            return applyAsLong32BytesMultiple(store, (int) len);
        }
        return applyAsLongAny(store, len);
    }

    /**
     * Computes 64-bit hash a long value
     *
     * @param l long value
     * @return 64-bit hash
     */
    public static long hash(long l) {
        return hash0(l, l >> 32);
    }

    /**
     * Computes a 64-bit hash using 2 longs.
     *
     * @param l  lower 64-bit
     * @param hi higher 64-bit
     * @return 64-bit hash
     */
    static long hash0(long l, long hi) {
        return agitate(l * K0 + hi * K1);
    }

    private static long applyAsLong1To7(ZByteStore<?> store, int remaining) {
        final long address = store.addressForRead(store.readPosition());
        return hash(readIncomplete64Bit(address, remaining));
    }

    private static long applyAsLong8(ZByteStore<?> store) {
        final long address = store.addressForRead(store.readPosition());
        return hash(MEMORY.readLong(address));
    }

    private static long applyAsLong9To16(ZByteStore<?> store, int remaining) {
        final long address = store.addressForRead(store.readPosition());
        long l0 = readIncomplete64Bit(address, remaining);
        int l0a = (int) (l0 >> 32);
        long l1 = readIncomplete64Bit(address + 8, remaining - 8);
        int l1a = (int) (l1 >> 32);
        long l2 = 0;
        int l2a = 0;
        long l3 = 0;
        int l3a = 0;

        long h0 = (long) remaining * K0;
        h0 += (l0 + l1a - l2a) * M0;
        long h1 = (l1 + l2a - l3a) * M1;
        long h2 = (l2 + l3a - l0a) * M2;
        long h3 = (l3 + l0a - l1a) * M3;

        return agitate(h0) ^ agitate(h1) ^ agitate(h2) ^ agitate(h3);
    }

    private static long applyAsLong17To32(ZByteStore<?> store, int remaining) {
        final long address = store.addressForRead(store.readPosition());

        long l0 = MEMORY.readLong(address);
        int l0a = (int) (l0 >> 32);
        long l1 = MEMORY.readLong(address + 8);
        int l1a = (int) (l1 >> 32);
        long l2 = readIncomplete64Bit(address + 16, remaining - 16);
        int l2a = (int) (l2 >> 32);
        long l3 = readIncomplete64Bit(address + 24, remaining - 24);
        int l3a = (int) (l3 >> 32);

        long h0 = (long) remaining * K0;
        h0 += (l0 + l1a - l2a) * M0;
        long h1 = (l1 + l2a - l3a) * M1;
        long h2 = (l2 + l3a - l0a) * M2;
        long h3 = (l3 + l0a - l1a) * M3;

        return agitate(h0) ^ agitate(h1) ^ agitate(h2) ^ agitate(h3);
    }

    private static long applyAsLong32BytesMultiple(ZByteStore<?> store, int remaining) {
        final long address = store.addressForRead(store.readPosition());
        long h0 = (long) remaining * K0;
        long h1 = 0;
        long h2 = 0;
        long h3 = 0;

        int i = 0;
        for (; i < remaining - 31; i += 32) {
            if (i > 0) {
                h0 *= K0;
                h1 *= K1;
                h2 *= K2;
                h3 *= K3;
            }

            long offset = address + i;
            long l0 = MEMORY.readLong(offset);
            int l0a = (int) (l0 >> 32);
            long l1 = MEMORY.readLong(offset + 8);
            int l1a = (int) (l1 >> 32);
            long l2 = MEMORY.readLong(offset + 16);
            int l2a = (int) (l2 >> 32);
            long l3 = MEMORY.readLong(offset + 24);
            int l3a = (int) (l3 >> 32);

            h0 += (l0 + l1a - l2a) * M0;
            h1 += (l1 + l2a - l3a) * M1;
            h2 += (l2 + l3a - l0a) * M2;
            h3 += (l3 + l0a - l1a) * M3;
        }

        return agitate(h0) ^ agitate(h1) ^ agitate(h2) ^ agitate(h3);
    }

    private static long applyAsLongAny(ZByteStore<?> store, long remaining) {
        final long address = store.addressForRead(store.readPosition());
        long h0 = (long) remaining * K0;
        long h1 = 0;
        long h2 = 0;
        long h3 = 0;

        long i = 0;
        for (; i < remaining - 31; i += 32) {
            if (i > 0) {
                h0 *= K0;
                h1 *= K1;
                h2 *= K2;
                h3 *= K3;
            }

            long offset = address + i;
            long l0 = MEMORY.readLong(offset);
            int l0a = (int) (l0 >> 32);
            long l1 = MEMORY.readLong(offset + 8);
            int l1a = (int) (l1 >> 32);
            long l2 = MEMORY.readLong(offset + 16);
            int l2a = (int) (l2 >> 32);
            long l3 = MEMORY.readLong(offset + 24);
            int l3a = (int) (l3 >> 32);

            h0 += (l0 + l1a - l2a) * M0;
            h1 += (l1 + l2a - l3a) * M1;
            h2 += (l2 + l3a - l0a) * M2;
            h3 += (l3 + l0a - l1a) * M3;
        }

        int left = Math.toIntExact(remaining - i);
        if (left > 0) {
            if (i > 0) {
                h0 *= K0;
                h1 *= K1;
                h2 *= K2;
                h3 *= K3;
            }
            long offset = address + i;
            if (left <= 16) {
                long l0 = readIncomplete64Bit(offset, left);
                int l0a = (int) (l0 >> 32);
                long l1 = readIncomplete64Bit(offset + 8, left - 8);
                int l1a = (int) (l1 >> 32);
                long l2 = 0;
                int l2a = 0;
                long l3 = 0;
                int l3a = 0;

                h0 += (l0 + l1a - l2a) * M0;
                h1 += (l1 + l2a - l3a) * M1;
                h2 += (l2 + l3a - l0a) * M2;
                h3 += (l3 + l0a - l1a) * M3;
            } else {
                long l0 = MEMORY.readLong(offset);
                int l0a = (int) (l0 >> 32);
                long l1 = MEMORY.readLong(offset + 8);
                int l1a = (int) (l1 >> 32);
                long l2 = readIncomplete64Bit(offset + 16, left - 16);
                int l2a = (int) (l2 >> 32);
                long l3 = readIncomplete64Bit(offset + 24, left - 24);
                int l3a = (int) (l3 >> 32);

                h0 += (l0 + l1a - l2a) * M0;
                h1 += (l1 + l2a - l3a) * M1;
                h2 += (l2 + l3a - l0a) * M2;
                h3 += (l3 + l0a - l1a) * M3;
            }
        }

        return agitate(h0) ^ agitate(h1) ^ agitate(h2) ^ agitate(h3);
    }

    private static long readIncomplete64Bit(long address, int len) {
        switch (len) {
            case 1:
                return MEMORY.readByte(address);
            case 2:
                return MEMORY.readShort(address);
            case 3:
                return IS_LITTLE_ENDIAN
                        ? (MEMORY.readShort(address) & 0xffff) | ((MEMORY.readByte(address + 2) & 0xff) << 16)
                        : ((MEMORY.readShort(address) & 0xffff) << 8) | (MEMORY.readByte(address + 2) & 0xff);
            case 4:
                return MEMORY.readInt(address);
            case 5:
                return IS_LITTLE_ENDIAN
                        ? (MEMORY.readInt(address) & 0xffffffffL) | ((long) (MEMORY.readByte(address + 4) & 0xffff) << 32)
                        : ((MEMORY.readInt(address) & 0xffffffffL) << 8) | (MEMORY.readByte(address + 4) & 0xffff);
            case 6:
                return IS_LITTLE_ENDIAN
                        ? (MEMORY.readInt(address) & 0xffffffffL) | ((long) (MEMORY.readShort(address + 4) & 0xffff) << 32)
                        : ((MEMORY.readInt(address) & 0xffffffffL) << 16) | (MEMORY.readShort(address + 4) & 0xffff);
            case 7:
                return IS_LITTLE_ENDIAN
                        ? (MEMORY.readInt(address) & 0xffffffffL) | ((long) (MEMORY.readShort(address + 4) & 0xffff) << 32) | ((long) (MEMORY.readByte(address + 6) & 0xff) << 48)
                        : ((MEMORY.readInt(address) & 0xffffffffL) << 24) | ((MEMORY.readShort(address + 4) & 0xffff) << 8) | (MEMORY.readByte(address + 6) & 0xff);
            default:
                return len >= 8 ? MEMORY.readLong(address) : 0;
        }
    }

}
