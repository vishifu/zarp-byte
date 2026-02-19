package org.zarp.bytes;

/**
 * A base implementation, inherits {@link AbstractBytes} with a fixed-capacity backing byte-store.
 * This class itself is not elastic.
 *
 * @param <U> underlying memory type
 */
public class VanillaByte<U> extends AbstractBytes<U> {

    protected VanillaByte(ZByteStore<U> zstore) {
        this(zstore, zstore.writePosition(), zstore.writeLimit());
    }

    protected VanillaByte(ZByteStore<U> zstore, long writePos, long writeLimit) {
        super(zstore, writePos, writeLimit);
    }

    @Override
    public int read(char[] dst, int dstBegin, int len) throws IllegalStateException {
        // todo: refactor
        return 0;
    }

}
