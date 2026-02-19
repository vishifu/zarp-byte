package org.zarp.bytes.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zarp.bytes.ZByteStore;
import org.zarp.bytes.exception.DecoratedBufferOverflowException;
import org.zarp.core.api.Jvm;
import org.zarp.core.api.ZPlatform;
import org.zarp.core.cleaner.BufferFreeServiceLocator;
import org.zarp.core.cleaner.spi.ByteBufferFreeService;
import org.zarp.core.common.SimpleCleaner;
import org.zarp.core.conditions.Ints;
import org.zarp.core.conditions.Longs;
import org.zarp.core.utils.DirectBufferUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

import static java.lang.String.format;

/**
 * An {@link ZByteStore} backed by off-heap memory (native memory). Instances are reference counted and must
 * release underlying memory.
 * The store maybe elastic or fixed-sie depending on how it was created.
 */
public class NativeByteStore<U> extends AbstractByteStore<U> implements ZByteStore<U> {

    private static final NoDeallocator NO_DEALLOCATOR = new NoDeallocator();
    private static final ByteBufferFreeService FREE_SERVICE = BufferFreeServiceLocator.instantiate();
    private static final long MEMORY_MAPPED_SIZE = 128 << 10;
    private static final Logger log = LoggerFactory.getLogger(NativeByteStore.class);

    /* finalizer used to warn about unreleased native memory when tracing resource */
    private final Finalizer finalizer;

    /* free native memory when ref count reach zero */
    protected SimpleCleaner cleaner;

    /* base address of allocated memory */
    private long address;

    /* maximum capacity of this store */
    private long capacity;

    /* actual allocated capacity of this store */
    private long size;

    /* backing underlying object, typically a ByteBuffer */
    private U underlyingObject;

    private NativeByteStore() {
        this.finalizer = null;
    }

    protected NativeByteStore(long address, long size) {
        this(address, size, null, false);
    }

    protected NativeByteStore(long address, long size, SimpleCleaner deallocator, boolean elastic) {
        this(address, size, deallocator, elastic, false);
    }

    protected NativeByteStore(long address, long size, SimpleCleaner deallocator, boolean elastic, boolean monitored) {
        super(monitored);
        setAddress(address);
        this.size = size;
        this.capacity = elastic ? DEFAULT_MAX_CAPACITY : size;
        this.cleaner = deallocator == null ? NO_DEALLOCATOR : deallocator;
        this.finalizer = deallocator == null || !Jvm.isResourceTracing() ? null : new Finalizer();
        this.underlyingObject = null;
    }

    protected NativeByteStore(ByteBuffer buffer) {
        this(buffer, false);
    }

    protected NativeByteStore(ByteBuffer buffer, boolean elastic) {
        this(buffer, elastic, MAX_HEAP_CAPACITY);
    }

    protected NativeByteStore(ByteBuffer buffer, boolean elastic, long capacity) {
        this();
        initiate(buffer);
        this.capacity = elastic ? capacity : Math.min(buffer.capacity(), capacity);
    }

    public static NativeByteStore<Void> wrap(byte[] buffer) {
        NativeByteStore<Void> store = fixedCapacity(buffer.length);
        store.write(0, buffer);
        return store;
    }

    public static NativeByteStore<ByteBuffer> wrap(ByteBuffer buffer) {
        return new NativeByteStore<>(buffer);
    }

    public static NativeByteStore<ByteBuffer> wrap(ByteBuffer buffer, boolean elastic) {
        return new NativeByteStore<>(buffer, elastic);
    }

    public static NativeByteStore<ByteBuffer> elasticBuffer(int size) {
        return elasticBuffer(size, DEFAULT_MAX_CAPACITY);
    }

    public static NativeByteStore<ByteBuffer> elasticBuffer(int size, long capacity) {
        if (capacity > DEFAULT_MAX_CAPACITY) {
            log.warn("capacity over default value, capped to {}", DEFAULT_MAX_CAPACITY);
            capacity = DEFAULT_MAX_CAPACITY;
        }
        return new NativeByteStore<>(ByteBuffer.allocateDirect(size), true, capacity);
    }

    public static NativeByteStore<ByteBuffer> follow(ByteBuffer buffer) {
        NativeByteStore<ByteBuffer> store = new NativeByteStore<>();
        store.initiate(buffer);
        store.capacity = store.size;
        store.cleaner = NO_DEALLOCATOR;
        return store;
    }

    public static NativeByteStore<Void> elastic(int initSize) {
        return elastic(initSize, DEFAULT_MAX_CAPACITY);
    }

    public static NativeByteStore<Void> elastic(int initSize, long capacity) {
        if (capacity > DEFAULT_MAX_CAPACITY) {
            log.warn("capacity {} exceeds {}, capping to {}", capacity, DEFAULT_MAX_CAPACITY, DEFAULT_MAX_CAPACITY);
            capacity = DEFAULT_MAX_CAPACITY;
        }
        return new NativeByteStore<>(ByteBuffer.allocateDirect(initSize), true, capacity);
    }

    public static NativeByteStore<Void> lazyFixedCapacity(long capacity) {
        return of(capacity, false, false);
    }

    public static NativeByteStore<Void> fixedCapacity(long capacity) {
        return of(capacity, true, false);
    }

    public static NativeByteStore<Void> fixedCapacity(long capacity, boolean zeroFill) {
        return of(capacity, zeroFill, false);
    }

    public static NativeByteStore<Void> of(long capacity, boolean zeroFill, boolean elastic) {
        if (capacity < 0) {
            return new NativeByteStore<>(NoByteStore.NO_PAGE, 0, null, elastic);
        }
        long alloc = ZPlatform.memory().allocate(capacity);
        if (zeroFill || capacity < MEMORY_MAPPED_SIZE) {
            ZPlatform.memory().setMemory(alloc, capacity, (byte) 0);
            ZPlatform.memory().storeFence();
        }

        Deallocator deallocator = new Deallocator(alloc, capacity);
        return new NativeByteStore<>(alloc, capacity, deallocator, elastic);
    }

    @SuppressWarnings("unchecked")
    private void initiate(ByteBuffer buffer) {
        buffer.order(ByteOrder.nativeOrder());
        this.underlyingObject = (U) buffer;
        this.size = buffer.capacity();
        setAddress(DirectBufferUtil.addressOf(buffer));
    }

    public void setAddress(long address) {
        if ((address & 0x3fff) == 0) {
            throw new AssertionError("Invalid address " + Long.toHexString(address));
        }
        this.address = address;
    }

    @Override
    public long translate(long offset) {
        return this.address + offset;
    }

    @Override
    public U underlyingObject() {
        return this.underlyingObject;
    }

    @Override
    public long capacity() {
        return this.capacity;
    }

    @Override
    public long size() {
        return this.size;
    }

    @Override
    public void zeroOut(long begin, long end)
            throws IllegalStateException, DecoratedBufferOverflowException {
        if (end <= begin) {
            return;
        }
        begin = Math.max(begin, start());
        end = Math.min(end, capacity());
        long addr = translate(begin);
        long len = end - begin;
        while ((addr & 0x7) != 0 && len > 0) {
            memory().writeByte(addr, (byte) 0);
            addr++;
            len--;
        }
        long i = 0;
        for (; i < len - 7; i += 8) {
            if (memory().readLong(addr + i) != 0) {
                memory().writeLong(addr + i, 0);
            }
        }
        for (; i < len; i++) {
            memory().writeByte(addr + i, (byte) 0);
        }
    }

    @Override
    public void move(long from, long to, long len)
            throws IllegalStateException, DecoratedBufferOverflowException {
        ensureNotReleased();
        copyNativeMemory(translate(from), translate(to), len);
    }

    @Override
    public byte readByte(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        ensureNotReleased();
        ensureOffsetInBound(offset);
        return memory().readByte(translate(offset));
    }

    @Override
    public short readShort(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        ensureNotReleased();
        ensureOffsetInBound(offset);
        return memory().readShort(translate(offset));
    }

    @Override
    public int readInt(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        ensureNotReleased();
        ensureOffsetInBound(offset);
        return memory().readInt(translate(offset));
    }

    @Override
    public long readLong(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        ensureNotReleased();
        ensureOffsetInBound(offset);
        return memory().readLong(translate(offset));
    }

    @Override
    public float readFloat(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        ensureNotReleased();
        ensureOffsetInBound(offset);
        return memory().readFloat(translate(offset));
    }

    @Override
    public double readDouble(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        ensureNotReleased();
        ensureOffsetInBound(offset);
        return memory().readDouble(translate(offset));
    }

    @Override
    public int read(long offset, byte[] dst, int dstBegin, int len)
            throws IllegalStateException, DecoratedBufferOverflowException {
        Objects.requireNonNull(dst);
        Longs.requireNonNegative(offset);
        Ints.requireNonNegative(dstBegin);
        Ints.requireNonNegative(len);
        if ((dstBegin + len) > dst.length) {
            throw new IllegalArgumentException(format("%d + %d is out of dst length %d", dstBegin, len, dst.length));
        }
        int left = Math.toIntExact(Math.min(len, readLimit() - offset));
        memory().readBytes(translate(offset), dst, dstBegin, left);
        return left;
    }

    @Override
    public int read(long offset, ByteBuffer dst, int dstBegin, int len) {
        Objects.requireNonNull(dst);
        Longs.requireNonNegative(offset);
        Ints.requireNonNegative(dstBegin);
        Ints.requireNonNegative(len);
        if ((dstBegin + len) > dst.capacity()) {
            throw new IllegalArgumentException(format("%d + %d is out of dst capacity %d", dstBegin, len, dst.capacity()));
        }
        int left = Math.toIntExact(Math.min(len, readLimit() - offset));
        if (dst.isDirect()) {
            memory().copyMemory(translate(offset), DirectBufferUtil.addressOf(dst) + dstBegin, left);
        } else {
            memory().readBytes(translate(offset), dst.array(), dstBegin, left);
        }
        return left;
    }

    @Override
    public byte readByteVolatile(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        ensureNotReleased();
        ensureOffsetInBound(offset);
        return memory().readVolatileByte(translate(offset));
    }

    @Override
    public short readShortVolatile(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        ensureNotReleased();
        ensureOffsetInBound(offset);
        return memory().readVolatileShort(translate(offset));
    }

    @Override
    public int readIntVolatile(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        ensureNotReleased();
        ensureOffsetInBound(offset);
        return memory().readVolatileInt(translate(offset));
    }

    @Override
    public long readLongVolatile(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        ensureNotReleased();
        ensureOffsetInBound(offset);
        return memory().readVolatileLong(translate(offset));
    }

    @Override
    public void nativeRead(long offset, long address, long len)
            throws IllegalStateException {
        ensureNotReleased();
        ensureOffsetInBound(offset);
        copyNativeMemory(translate(offset), address, len);
    }

    @Override
    public void writeByte(long offset, byte i8)
            throws IllegalStateException, DecoratedBufferOverflowException {
        ensureNotReleased();
        ensureOffsetInBound(offset);
        memory().writeByte(translate(offset), i8);
    }

    @Override
    public void writeShort(long offset, short i16)
            throws IllegalStateException, DecoratedBufferOverflowException {
        ensureNotReleased();
        ensureOffsetInBound(offset);
        memory().writeShort(translate(offset), i16);
    }

    @Override
    public void writeInt(long offset, int i32)
            throws IllegalStateException, DecoratedBufferOverflowException {
        ensureNotReleased();
        ensureOffsetInBound(offset);
        memory().writeInt(translate(offset), i32);
    }

    @Override
    public void writeLong(long offset, long i64)
            throws IllegalStateException, DecoratedBufferOverflowException {
        ensureNotReleased();
        ensureOffsetInBound(offset);
        memory().writeLong(translate(offset), i64);
    }

    @Override
    public void writeFloat(long offset, float f32)
            throws IllegalStateException, DecoratedBufferOverflowException {
        ensureNotReleased();
        ensureOffsetInBound(offset);
        memory().writeFloat(translate(offset), f32);
    }

    @Override
    public void writeDouble(long offset, double f64)
            throws IllegalStateException, DecoratedBufferOverflowException {
        ensureNotReleased();
        ensureOffsetInBound(offset);
        memory().writeDouble(translate(offset), f64);
    }

    @Override
    public void write(long offset, byte[] src, int srcBegin, int len)
            throws IllegalStateException, DecoratedBufferOverflowException {
        Objects.requireNonNull(src);
        Longs.requireNonNegative(offset);
        Ints.requireNonNegative(srcBegin);
        Ints.requireNonNegative(len);
        if ((srcBegin + len) > src.length) {
            throw new IllegalArgumentException(format("%d + %d out of src length %d", srcBegin, len, src.length));
        }
        memory().copyMemory(src, srcBegin, translate(offset), len);
    }

    @Override
    public void write(long offset, ByteBuffer src, int srcBegin, int len)
            throws IllegalStateException, DecoratedBufferOverflowException {
        Objects.requireNonNull(src);
        Longs.requireNonNegative(offset);
        Ints.requireNonNegative(srcBegin);
        Ints.requireNonNegative(len);
        if ((srcBegin + len) > src.capacity()) {
            throw new IllegalArgumentException(format("%d + %d out of src capacity %d", srcBegin, len, src.capacity()));
        }
        if (src.isDirect()) {
            memory().copyMemory(DirectBufferUtil.addressOf(src) + srcBegin, translate(offset), len);
        } else {
            memory().copyMemory(src.array(), src.arrayOffset() + srcBegin, translate(offset), len);
        }
    }

    @Override
    public void writeIntOrdered(long offset, int i32)
            throws IllegalStateException, DecoratedBufferOverflowException {
        ensureNotReleased();
        ensureOffsetInBound(offset);
        memory().writeOrderedInt(translate(offset), i32);
    }

    @Override
    public void writeLongOrdered(long offset, long i64)
            throws IllegalStateException, DecoratedBufferOverflowException {
        ensureNotReleased();
        ensureOffsetInBound(offset);
        memory().writeOrderedLong(translate(offset), i64);
    }

    @Override
    public void writeIntVolatile(long offset, int i32)
            throws IllegalStateException, DecoratedBufferOverflowException {
        ensureNotReleased();
        ensureOffsetInBound(offset);
        memory().writeVolatileInt(translate(offset), i32);
    }

    @Override
    public void writeLongVolatile(long offset, long i64)
            throws IllegalStateException, DecoratedBufferOverflowException {
        ensureNotReleased();
        ensureOffsetInBound(offset);
        memory().writeVolatileLong(translate(offset), i64);
    }

    @Override
    public boolean compareAndSwap(long offset, int expected, int value)
            throws IllegalStateException, DecoratedBufferOverflowException {
        ensureNotReleased();
        ensureOffsetInBound(offset);
        return memory().compareAndSwapInt(translate(offset), expected, value);
    }

    @Override
    public boolean compareAndSwap(long offset, long expected, long value)
            throws IllegalStateException, DecoratedBufferOverflowException {
        ensureNotReleased();
        ensureOffsetInBound(offset);
        return memory().compareAndSwapLong(translate(offset), expected, value);
    }

    @Override
    public void testAndSet(long offset, int expected, int value)
            throws IllegalStateException, DecoratedBufferOverflowException {
        ensureOffsetInBound(offset);
        memory().testAndSetInt(translate(offset), expected, value);
    }

    @Override
    public void testAndSet(long offset, long expected, long value)
            throws IllegalStateException, DecoratedBufferOverflowException {
        ensureNotReleased();
        ensureOffsetInBound(offset);
        memory().testAndSetLong(translate(offset), expected, value);
    }

    @Override
    public void nativeWrite(long address, long offset, long len)
            throws IllegalStateException {
        copyNativeMemory(address, translate(offset), len);
    }

    @Override
    public long addressForRead(long offset)
            throws DecoratedBufferOverflowException {
        ensureOffsetInBound(offset);
        return translate(offset);
    }

    @Override
    public long addressForWrite(long offset)
            throws DecoratedBufferOverflowException {
        ensureOffsetInBound(offset);
        return translate(offset);
    }

    @Override
    public boolean isHeap() {
        return false;
    }

    @Override
    public boolean isNative() {
        return true;
    }

    @Override
    public boolean canReadDirect(long n) {
        return this.size >= n;
    }

    @Override
    protected void daemonFree() {
        super.daemonFree();
    }

    @Override
    protected void doFree0() throws IllegalStateException {
        if (this.cleaner != null) {
            this.cleaner.cleanUp();
        } else if (this.underlyingObject instanceof ByteBuffer) {
            final ByteBuffer buffer = (ByteBuffer) this.underlyingObject;
            if (buffer.isDirect()) {
                FREE_SERVICE.free(buffer);
            }
        }
    }

    protected void copyNativeMemory(long fromAddr, long toAddr, long len) {
        if (fromAddr < 0 || toAddr < 0) {
            throw new IllegalArgumentException(format("from (%d) or to (%d) is negative", fromAddr, toAddr));
        }
        if (len < 0) {
            throw new IllegalArgumentException(format("len (%d) < 0 ", len));
        }
        memory().copyMemory(fromAddr, toAddr, len);
    }

    private void ensureOffsetInBound(long offset) {
        if (offset < start() || offset > size()) {
            throw new DecoratedBufferOverflowException(format("offset (%d) is out of bound [%d,%d)", offset, start(), size()));
        }
    }

    private final class Finalizer {
        @SuppressWarnings("deprecation")
        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            warnAndReleaseIfNotRelease();
        }
    }

    private static final class Deallocator extends SimpleCleaner {
        public Deallocator(long address, long size) {
            super(() -> ZPlatform.memory().freeMemory(address, size));
        }
    }

    private static final class NoDeallocator extends SimpleCleaner {
        public NoDeallocator() {
            super(null);
        }

        @Override
        public void cleanUp() {
            // nop
        }
    }

}
