package org.zarp.bytes.algo;

import org.zarp.bytes.ZByteStore;

import java.nio.BufferOverflowException;
import java.util.function.ToLongFunction;

/**
 * Represents a function that computes a 64-bit hash from {@link ZByteStore}.
 * Implementation provides a fast, deterministic base on little-endian variants of Murmur3 hashing.
 * <p>
 * Implementation should avoid allocating memory and may assume that bytes can be read without extra boundary checks.
 */
public interface ByteStoreHash<B extends ZByteStore<?>> extends ToLongFunction<B> {

    /**
     * Mixing constant used in the hashing algorithm.
     */
    int K0 = 0x6d0f27bd;
    /**
     * Mixing constant used in the hashing algorithm.
     */
    int K1 = 0xc1f3bfc9;
    /**
     * Mixing constant used in the hashing algorithm.
     */
    int K2 = 0x6b192397;
    /**
     * Mixing constant used in the hashing algorithm.
     */
    int K3 = 0x6b915657;
    /**
     * Multiplicative constant used in the hashing algorithm.
     */
    int M0 = 0x5bc80bad;
    /**
     * Multiplicative constant used in the hashing algorithm.
     */
    int M1 = 0xea7585d7;
    /**
     * Multiplicative constant used in the hashing algorithm.
     */
    int M2 = 0x7a646e19;
    /**
     * Multiplicative constant used in the hashing algorithm.
     */
    int M3 = 0x855dd4db;

    /**
     * Computes a 64-bit hash of given {@link ZByteStore} with length.
     *
     * @param bytes bytes to compute
     * @param len   number of bytes to compute
     * @return 64-bit hash
     */
    long applyAsLong(ZByteStore<?> bytes, long len) throws IllegalStateException, BufferOverflowException;

    /**
     * Applies a series of bitwise operations (XOR and rotate) to given long value
     * to improve hash distribution
     *
     * @param l input value
     * @return agitated value
     */
    static long agitate(long l) {
        l ^= Long.rotateLeft(l, 26);
        l ^= Long.rotateRight(l, 17);
        return l;
    }

}
