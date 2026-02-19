package org.zarp.bytes;

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
