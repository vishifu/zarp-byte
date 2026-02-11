package org.zarp.bytes.algo;

import org.zarp.bytes.ZByteStore;

public final class BytesDigest {

    private BytesDigest() {
    }

    /**
     * Computes 32-bit hash of a range of given {@link ZByteStore}.
     *
     * @param bytes bytes to compute
     * @return 32-bit hash
     */
    public static int hash32(ZByteStore<?> bytes) {
        long hash = hash(bytes);
        return (int) (hash ^ (hash >> 32));
    }

    /**
     * Computes 32-bit hash of a range of given {@link ZByteStore}.
     *
     * @param bytes bytes to compute
     * @param len   number of bytes to compute
     * @return 32-bit hash
     */
    public static int hash32(ZByteStore<?> bytes, long len) {
        long hash = hash(bytes, len);
        return (int) (hash ^ (hash >> 32));
    }

    /**
     * Computes a 64-bit hash of given {@link ZByteStore}.
     *
     * @param bytes bytes to compute
     * @return 64-bit hash
     */
    public static long hash(ZByteStore<?> bytes) {
        return bytes.isNative()
                ? OptimisedByteStoreHash.INSTANCE.applyAsLong(bytes)
                : VanillaByteStoreHash.INSTANCE.applyAsLong(bytes);
    }

    /**
     * Computes a 64-bit hash of a range of given {@link ZByteStore}.
     *
     * @param bytes bytes to compute
     * @param len   number of bytes to compute
     * @return 64-bit hash
     */
    public static long hash(ZByteStore<?> bytes, long len) {
        return bytes.isNative()
                ? OptimisedByteStoreHash.INSTANCE.applyAsLong(bytes, len)
                : VanillaByteStoreHash.INSTANCE.applyAsLong(bytes, len);
    }

}
