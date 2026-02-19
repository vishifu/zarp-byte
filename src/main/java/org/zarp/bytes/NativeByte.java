package org.zarp.bytes;

import org.zarp.bytes.exception.DecoratedBufferOverflowException;
import org.zarp.core.api.ZPlatform;
import org.zarp.core.common.ZStackTrace;
import org.zarp.core.io.AbstractReferenceCount;

import java.nio.Buffer;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Objects;

import static org.zarp.core.utils.ZFmt.bytesToKiB;

/**
 * Represents an implementation of {@link ZBytes} which is backed by a generic object (allocated on native memory).
 * This implementation class is elastic.
 */
public class NativeByte<U> extends VanillaByte<U> {

    private static final long LARGE_MEMORY_BLOCK = 128 << 10;

    /* capacity of this object */
    private long capacity;

    protected NativeByte(ZByteStore<U> zstore) {
        this(zstore, zstore.capacity());
    }

    protected NativeByte(ZByteStore<U> zstore, long capacity) {
        super(zstore);
        this.capacity = capacity;
    }

    @Override
    public long capacity() {
        return this.capacity;
    }

    @Override
    public boolean isElastic() {
        return true;
    }

    @Override
    public void ensureCapacity(long requested)
            throws IllegalStateException, DecoratedBufferOverflowException {
        if (requested < 0)  {
            throw new IllegalArgumentException(requested + " < 0");
        }
        assert DISABLE_SINGLE_THREADED_CHECK || threadSafeCheck(true);
        writeCheckOffset(requested, 0);
    }

    @Override
    protected void setByteStore(ZByteStore<U> store) {
        Objects.requireNonNull(store);
        this.capacity = Math.max(this.capacity, store.capacity());
        super.setByteStore(store);
    }

    @Override
    protected long writeAdvanceOffset(long advances) {
        final long old = writePosition();
        if (writePosition() < this.zstore.start()) {
            throw new BufferOverflowException();
        }
        final long hi = writePosition() + advances;
        if (hi > writeLimit()) {
            throw new DecoratedBufferOverflowException(String.format("write advance %d from %d overflow limit %d",
                    advances, old, writeLimit()));
        } else if (hi > this.zstore.safeLimit()) {
            resize(hi);
        }
        assert DISABLE_SINGLE_THREADED_CHECK || threadSafeCheck(true);
        uncheckedWritePosition(hi);
        return old;
    }

    private void resize(long requested) {
        ensureNotReleased();
        if (requested  < 0) {
            throw new IllegalArgumentException("requested "  + requested +  " < 0");
        }
        if (requested > capacity()) {
            throw new DecoratedBufferOverflowException("requested " + requested + " > capacity " + capacity());
        }
        final long size = size();
        if (requested <= size && !isImmutableEmptyStore()) {
            return;
        }

        long growSize = Math.max(requested + 7, size * 3 / 2 + 32);
        if (isNative() || size > MAX_HEAP_CAPACITY)  {
            growSize = ZPlatform.pageAlign(growSize, ZPlatform.defaultOsPageSize());
        } else {
            growSize &= ~0x7;
        }

        final long newSize = Math.min(growSize, capacity()); // actual size to grow
        final boolean isNioBufferBacked = this.zstore.underlyingObject() instanceof ByteBuffer;
        if (isNioBufferBacked && size > MAX_HEAP_CAPACITY) {
            ZStackTrace ztrace = new ZStackTrace("[perf]");
            log.warn("going to try to replace ZByteStore backed by ByteBuffer by NativeByteStore for growing to {}",
                    bytesToKiB(newSize), ztrace);
        }
        if (newSize >= LARGE_MEMORY_BLOCK && size > 0) {
            log.warn("resizing buffer; size={}; need={}; new_size={}",
                    bytesToKiB(size), bytesToKiB(newSize - size), bytesToKiB(newSize));
        }
        doResize(newSize, isNioBufferBacked);
    }

    @SuppressWarnings("unchecked")
    private void doResize(long requested, boolean isNioBufferBacked) {
        ZByteStore<U> store;
        int pos  = 0;
        try {
            if (isNioBufferBacked && requested < MAX_HEAP_CAPACITY) {
                pos = ((ByteBuffer)this.zstore.underlyingObject()).position();
                store = allocateBackedStore(Math.toIntExact(requested));
            } else {
                store = (ZByteStore<U>) ZByteStore.lazyFixedCapacity(requested);
                if (this.referenceCountable.unmonitored()) {
                    AbstractReferenceCount.unmonitor(store);
                }
            }
            store.reserveTransfer(INIT, this);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            throw ex;
        }

        ensureNotReleased();
        ZByteStore<U> tmp = this.zstore;
        this.zstore.copyTo(store);
        ZByteStore<U> tmp2 = store;
        this.setByteStore(tmp2);
        try {
            tmp.release(this);
        } catch (IllegalStateException ex) {
            log.info(ex.getMessage(), ex);
        }
        if (this.zstore.underlyingObject() instanceof ByteBuffer) {
            Buffer buffer = (ByteBuffer) this.zstore.underlyingObject();
            buffer.position(0);
            buffer.limit(buffer.capacity());
            buffer.position(pos);
        }
    }

    private boolean isImmutableEmptyStore() {
        return this.zstore.capacity() == 0;
    }

    @SuppressWarnings("unchecked")
    private ZByteStore<U> allocateBackedStore(int size) {
        return (ZByteStore<U>) (
                isNative()
                        ? ZByteStore.elasticBuffer(size, capacity())
                        : ZByteStore.wraps(ByteBuffer.allocate(size))
        );
    }
}
