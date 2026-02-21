package org.zarp.bytes;

import org.zarp.bytes.exception.DecoratedBufferOverflowException;
import org.zarp.bytes.internal.OnHeapByteStore;

import java.nio.ByteBuffer;

import static org.zarp.core.utils.ZFmt.bytesToKiB;

/**
 * Represents an implementation of {@link ZBytes} which backed by a heap byte array.
 */
public class OnHeapByte extends VanillaByte<byte[]> {

    private final long capacity;
    private final boolean elastic;

    protected OnHeapByte(ZByteStore<byte[]> zstore, boolean elastic) {
        super(zstore);
        this.elastic = elastic;
        this.capacity = elastic ? DEFAULT_MAX_CAPACITY : zstore.capacity();

        writePosition(0L);
        writeLimit(capacity());
    }

    public static OnHeapByte wraps(ByteBuffer buffer) {
        return wraps(buffer, false);
    }

    public static OnHeapByte wraps(ByteBuffer buffer, boolean elastic) {
        if (buffer.isDirect()) {
            throw new IllegalArgumentException("buffer is direct");
        }
        return wraps(buffer.array(), elastic);
    }

    public static OnHeapByte wraps(byte[] array) {
        return wraps(array, false);
    }

    public static OnHeapByte wraps(byte[] array, boolean elastic) {
        OnHeapByteStore<byte[]> store = OnHeapByteStore.wrap(array);
        return new OnHeapByte(store, elastic);
    }

    @Override
    public long capacity() {
        return this.capacity;
    }

    @Override
    public boolean isElastic() {
        return this.elastic;
    }

    @Override
    public void ensureCapacity(long requested)
            throws IllegalStateException, DecoratedBufferOverflowException {
        if (isElastic() && capacity() < requested) {
            resize(requested);
        } else {
            super.ensureCapacity(requested);
        }
    }

    @Override
    protected void writeCheckOffset(long offset, long adv) {
        if (offset >= this.zstore.start()  && (offset + adv) >= this.zstore.start()) {
            long hi = offset + adv;
            if (hi > writeLimit()) {
                throw new DecoratedBufferOverflowException(String.format("write advance %d from %d overflow limit %d",
                        offset, adv, writeLimit()));
            }
            if (hi <= this.zstore.safeLimit()) {
                return;
            }
            if (isElastic()) {
                resize(hi);
            } else {
                throw new DecoratedBufferOverflowException("Overflow, could not grow this bytes");
            }
        }
    }

    private void resize(long requested) throws DecoratedBufferOverflowException {
        if (requested < 0) {
            throw new DecoratedBufferOverflowException(requested + " < 0");
        }
        if (requested > capacity()) {
            throw new DecoratedBufferOverflowException(requested + " > cap (" + capacity() + ')');
        }
        long size = size();
        if (requested < size) {
            return;
        }

        long newSize = Math.max(requested, size * 3 / 2); // grow 1/2
        int actualGrowSize = (int) Math.min(newSize, capacity());

        // native block of 128 KiB or more have an individual memory mapping, expensive.
        if (actualGrowSize >= (128 << 10)) {
            log.warn("resizing buffer; size={}; need={}; new_size={}",
                    bytesToKiB(size),
                    bytesToKiB(actualGrowSize - size),
                    bytesToKiB(actualGrowSize));
        }

        ZByteStore<byte[]> newStore = ZByteStore.wraps(new byte[actualGrowSize]);
        newStore.reserveTransfer(INIT, this);

        ZByteStore<byte[]> temp = this.zstore;
        this.zstore.copyTo(newStore);
        setByteStore(newStore);
        temp.release(this);
    }
}
