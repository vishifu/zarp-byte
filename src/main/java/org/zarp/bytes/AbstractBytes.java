package org.zarp.bytes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zarp.bytes.exception.DecoratedBufferOverflowException;
import org.zarp.core.api.Jvm;
import org.zarp.core.conditions.Ints;
import org.zarp.core.conditions.Longs;
import org.zarp.core.io.AbstractReferenceCount;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;

/**
 * Provides an abstract implementation of {@link ZBytes} with read/write pointers management.
 * All pointer's positions are relative to {@link #start()}.
 *
 * @param <U> underlying memory type
 */
public abstract class AbstractBytes<U>
        extends AbstractReferenceCount
        implements ZBytes<U>, HasUncheckedRandomInput {

    private static final AtomicLong G_ID = new AtomicLong(1);
    private static final boolean BOUND_UNCHECKED = Jvm.getBoolean("zbytes.bounds_check.disable", false);
    protected static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final UncheckedRandomInputHolder UNCHECKED_RANDOM_IN = new UncheckedRandomInputHolder();

    /* optionally, for debugging */
    private final String name;

    /* underlying store */
    protected ZByteStore<U> zstore;
    /* whether the underlying store is open */
    protected boolean isPresent;
    /* offset for next byte to read */
    protected long readPosition;
    /* offset for next byte to write */
    protected long writePosition;
    /* highest index can be written */
    protected long writeLimit;
    /* lenient mode suppressed on reads */
    private boolean lenient;

    protected AbstractBytes(ZByteStore<U> zstore,
                            long writePos,
                            long writeLimit) {
        this("bytes-" + G_ID.getAndIncrement(), zstore, writePos, writeLimit);
    }

    protected AbstractBytes(String name,
                            ZByteStore<U> zstore,
                            long writePos,
                            long writeLimit) {
        super(zstore.isNative());

        this.name = name;
        this.writeLimit = writeLimit;
        this.writePosition = writePos;
        this.readPosition = zstore.readPosition();
        setByteStore(zstore);
        zstore.reserve(this);
    }

    @Override
    public UncheckedRandomInput acquireUncheckedInput() {
        return this.UNCHECKED_RANDOM_IN;
    }

    @Override
    public long translate(long offset) {
        return this.zstore.translate(offset);
    }

    @Override
    public U underlyingObject() {
        return this.zstore.underlyingObject();
    }

    @Override
    public long start() {
        return this.zstore.start();
    }

    @Override
    public void writePosition(long pos) throws DecoratedBufferOverflowException {
        if (this.writePosition == pos) {
            return;
        }
        assertTheadSafe();
        if (pos < start()) {
            throw new DecoratedBufferOverflowException(format("pos (%d) < start (%d)", pos, start()));
        }
        if (pos > writeLimit()) {
            throw new DecoratedBufferOverflowException(format("pos (%d) > limit (%d)", pos, writeLimit()));
        }
        this.writeLimit = pos;
    }

    @Override
    public void readPosition(long pos) throws DecoratedBufferOverflowException {
        if (this.readPosition == pos) {
            return;
        }
        assertTheadSafe();
        if (pos < start()) {
            throw new DecoratedBufferOverflowException(format("pos (%d) < start (%d)", pos, start()));
        }
        if (pos > readLimit()) {
            throw new DecoratedBufferOverflowException(format("pos (%d) > limit (%d)", pos, readLimit()));
        }
        this.readPosition = pos;
    }

    @Override
    public long writePosition() {
        return this.writePosition;
    }

    @Override
    public long readPosition() {
        return this.readPosition;
    }

    @Override
    public long writeLimit() {
        return this.writeLimit;
    }

    @Override
    public long size() {
        return this.zstore.size();
    }

    @Override
    public long capacity() {
        return this.zstore.capacity();
    }

    @Override
    public boolean isHeap() {
        return this.zstore.isHeap();
    }

    @Override
    public boolean isNative() {
        return this.zstore.isNative();
    }

    @Override
    public boolean isElastic() {
        return false;
    }

    @Override
    public boolean canReadDirect(long n) {
        long remaining = writePosition() - readPosition();
        return this.zstore.isNative() && remaining >= n;
    }

    @Override
    public boolean canWriteDirect(long n) {
        long left = Math.min(writeLimit(), size());
        return this.zstore.isNative() && left >= n;
    }

    @Override
    public void readAdvance(long n) {
        this.readPosition += n;
    }

    @Override
    public void writeAdvance(long n) {
        this.writePosition += n;
    }

    @Override
    public ZByteStore<U> byteStore() {
        return this.zstore;
    }

    protected void setByteStore(ZByteStore<U> store) {
        this.zstore = store;
    }

    protected int thresholdCopySize() {
        return 64 << 10;
    }

    public String getName() {
        return this.name;
    }

    public boolean isPresent() {
        return this.isPresent;
    }

    public boolean isLenient() {
        return this.lenient;
    }

    @Override
    public void lenient(boolean mode) {
        this.lenient = mode;
    }

    private DecoratedBufferOverflowException newBOELow(long offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("negative offset " + offset);
        }
        return new DecoratedBufferOverflowException(format("offset (%d) < start (%d)", offset, start()));
    }

    private DecoratedBufferOverflowException newBOERange(long offset, long advances, long limit) {
        return new DecoratedBufferOverflowException(format("offset (%d) + adv (%d) overflow range [%d,%d)",
                offset, advances, start(), limit));
    }

    private DecoratedBufferOverflowException positionTooHigh(long pos, long hi) {
        return new DecoratedBufferOverflowException(String.format("position (%d) > limit (%d)", pos, hi));
    }

    private void assertTheadSafe() {
        assert DISABLE_SINGLE_THREADED_CHECK || threadSafeCheck(true);
    }

    protected void writeCheckOffset(long offset, long adv) {
        if (BOUND_UNCHECKED) {
            return;
        }
        if (offset < start()) {
            throw newBOELow(offset);
        }
        if ((offset + adv) > writeLimit()) {
            throw newBOERange(offset, adv, writeLimit());
        }
    }

    protected long writeAdvanceOffset(long advances) {
        long old = writePosition();
        assertTheadSafe();
        writeCheckOffset(old, advances);
        uncheckedWritePosition(old + advances);
        return old;
    }

    protected void uncheckedWritePosition(long pos) {
        this.writePosition = pos;
    }

    protected void readCheckOffset(long offset, long adv, boolean given) {
        if (BOUND_UNCHECKED) {
            return;
        }
        assertTheadSafe();
        if (offset < start()) {
            throw newBOELow(offset);
        }
        long limit = given ? writeLimit() : readLimit();
        if ((offset + adv) > limit) {
            throw newBOERange(offset, adv, limit);
        }
    }

    protected long readAdvanceOffset(long advances) {
        long old = readPosition();
        assertTheadSafe();
        readCheckOffset(old, advances, false);
        uncheckedReadPosition(old + advances);
        return old;
    }

    protected void uncheckedReadPosition(long pos) {
        this.readPosition = pos;
    }

    @Override
    public void uncheckedReadSkipOne() {
        ++this.readPosition;
    }

    @Override
    public void uncheckedReadBackSkipOne() {
        --this.readPosition;
    }

    @Override
    public void writeLimit(long limit) throws DecoratedBufferOverflowException {
        if (this.writeLimit == limit) {
            return;
        }
        assertTheadSafe();
        if (limit < start()) {
            throw newBOELow(limit);
        }
        if (limit > capacity()) {
            throw positionTooHigh(limit, capacity());
        }
        this.writeLimit = limit;
    }

    @Override
    public void readLimit(long limit) throws DecoratedBufferOverflowException {
        if (writePosition() == limit) {
            return;
        }
        assertTheadSafe();
        if (limit > writeLimit()) {
            throw positionTooHigh(limit, writeLimit());
        }
        uncheckedWritePosition(limit);
    }

    @Override
    public void writeByte(long offset, byte i8)
            throws IllegalStateException, DecoratedBufferOverflowException {
        writeCheckOffset(offset, Byte.BYTES);
        this.zstore.writeByte(offset, i8);
    }

    @Override
    public void writeShort(long offset, short i16)
            throws IllegalStateException, DecoratedBufferOverflowException {
        writeCheckOffset(offset, Short.BYTES);
        this.zstore.writeShort(offset, i16);
    }

    @Override
    public void writeInt(long offset, int i32)
            throws IllegalStateException, DecoratedBufferOverflowException {
        writeCheckOffset(offset, Integer.BYTES);
        this.zstore.writeInt(offset, i32);
    }

    @Override
    public void writeLong(long offset, long i64)
            throws IllegalStateException, DecoratedBufferOverflowException {
        writeCheckOffset(offset, Long.BYTES);
        this.zstore.writeLong(offset, i64);
    }

    @Override
    public void writeFloat(long offset, float f32)
            throws IllegalStateException, DecoratedBufferOverflowException {
        writeCheckOffset(offset, Float.BYTES);
        this.zstore.writeFloat(offset, f32);
    }

    @Override
    public void writeDouble(long offset, double f64)
            throws IllegalStateException, DecoratedBufferOverflowException {
        writeCheckOffset(offset, Double.BYTES);
        this.zstore.writeDouble(offset, f64);
    }

    @Override
    public void writeIntOrdered(long offset, int i32)
            throws IllegalStateException, DecoratedBufferOverflowException {
        writeCheckOffset(offset, Integer.BYTES);
        this.zstore.writeIntOrdered(offset, i32);
    }

    @Override
    public void writeLongOrdered(long offset, long i64)
            throws IllegalStateException, DecoratedBufferOverflowException {
        writeCheckOffset(offset, Long.BYTES);
        this.zstore.writeLongOrdered(offset, i64);
    }

    @Override
    public void writeIntVolatile(long offset, int i32)
            throws IllegalStateException, DecoratedBufferOverflowException {
        writeCheckOffset(offset, Integer.BYTES);
        this.zstore.writeIntVolatile(offset, i32);
    }

    @Override
    public void writeLongVolatile(long offset, long i64)
            throws IllegalStateException, DecoratedBufferOverflowException {
        writeCheckOffset(offset, Long.BYTES);
        this.zstore.writeLongVolatile(offset, i64);
    }

    @Override
    public void write(long offset, byte[] src, int srcBegin, int len)
            throws IllegalStateException, DecoratedBufferOverflowException {
        Objects.requireNonNull(src);
        Longs.requireNonNegative(offset);
        Ints.requireNonNegative(srcBegin);
        Ints.requireNonNegative(len);
        if ((srcBegin + len) > src.length) {
            throw new IllegalArgumentException(format("required write (%d + %d) out of src length %d", srcBegin, len, src.length));
        }
        if (len > writeRemaining()) {
            throw new DecoratedBufferOverflowException(offset + len, offset, writeRemaining());
        }
        ensureCapacity(offset + len);
        while (len > 0) {
            int copy = Math.min(len, thresholdCopySize());
            writeCheckOffset(offset, copy);
            this.zstore.write(offset, src, srcBegin, copy);
            offset += copy;
            srcBegin += copy;
            len -= copy;
        }
    }

    @Override
    public void write(long offset, ByteBuffer src, int srcBegin, int len)
            throws IllegalStateException, DecoratedBufferOverflowException {
        Objects.requireNonNull(src);
        Ints.requireNonNegative(srcBegin);
        Ints.requireNonNegative(len);
        Longs.requireNonNegative(offset);
        if ((srcBegin + len) > src.capacity()) {
            throw new IllegalArgumentException(format("require (%d + %d) out of src capacity %d",
                    srcBegin, len, src.capacity()));
        }
        if (len > writeRemaining()) {
            throw new DecoratedBufferOverflowException(offset + len, offset, writeRemaining());
        }
        ensureCapacity(offset + len);
        writeCheckOffset(offset, len);
        this.zstore.write(offset, src, srcBegin, len);
    }

    @Override
    public boolean compareAndSwap(long offset, int expected, int value)
            throws IllegalStateException, DecoratedBufferOverflowException {
        writeCheckOffset(offset, Integer.BYTES);
        return this.zstore.compareAndSwap(offset, expected, value);
    }

    @Override
    public boolean compareAndSwap(long offset, long expected, long value)
            throws IllegalStateException, DecoratedBufferOverflowException {
        writeCheckOffset(offset, Long.BYTES);
        return this.zstore.compareAndSwap(offset, expected, value);
    }

    @Override
    public void testAndSet(long offset, int expected, int value)
            throws IllegalStateException, DecoratedBufferOverflowException {
        writeCheckOffset(offset, Integer.BYTES);
        this.zstore.testAndSet(offset, expected, value);
    }

    @Override
    public void testAndSet(long offset, long expected, long value)
            throws IllegalStateException, DecoratedBufferOverflowException {
        writeCheckOffset(offset, Long.BYTES);
        this.zstore.testAndSet(offset, expected, value);
    }

    @Override
    public void writeByte(byte i8) throws IllegalStateException {
        long offset = writeAdvanceOffset(Byte.BYTES);
        this.zstore.writeByte(offset, i8);
    }

    @Override
    public void writeShort(short i16) throws IllegalStateException {
        long offset = writeAdvanceOffset(Short.BYTES);
        this.zstore.writeShort(offset, i16);
    }

    @Override
    public void writeInt(int i32) throws IllegalStateException {
        long offset = writeAdvanceOffset(Integer.BYTES);
        this.zstore.writeInt(offset, i32);
    }

    @Override
    public void writeLong(long i64) throws IllegalStateException {
        long offset = writeAdvanceOffset(Long.BYTES);
        this.zstore.writeLong(offset, i64);
    }

    @Override
    public void writeFloat(float f32) throws IllegalStateException {
        long offset = writeAdvanceOffset(Float.BYTES);
        this.zstore.writeFloat(offset, f32);
    }

    @Override
    public void writeDouble(double f64) throws IllegalStateException {
        long offset = writeAdvanceOffset(Double.BYTES);
        this.zstore.writeDouble(offset, f64);
    }

    @Override
    public void writeIntOrdered(int i32) throws IllegalStateException {
        long offset = writeAdvanceOffset(Integer.BYTES);
        this.zstore.writeIntOrdered(offset, i32);
    }

    @Override
    public void writeLongOrdered(long i64) throws IllegalStateException {
        long offset = writeAdvanceOffset(Long.BYTES);
        this.zstore.writeLongOrdered(offset, i64);
    }

    @Override
    public int write(byte[] src, int srcBegin, int len) throws IllegalStateException {
        Objects.requireNonNull(src);
        Ints.requireNonNegative(srcBegin);
        Ints.requireNonNegative(len);
        if ((srcBegin + len) > src.length) {
            throw new IllegalArgumentException(format("required write (%d + %d) out of src length %d",
                    srcBegin, len, src.length));
        }
        if (len > writeRemaining()) {
            throw new DecoratedBufferOverflowException(writePosition() + len, writePosition(), writeRemaining());
        }
        ensureCapacity(writePosition() + len);
        while (len > 0) {
            int copy = Math.min(len, thresholdCopySize());
            long offset = writeAdvanceOffset(copy);
            this.zstore.write(offset, src, srcBegin, copy);
            srcBegin += copy;
            len -= copy;
        }
        return len;
    }

    @Override
    public int write(ByteBuffer src, int srcBegin, int len) throws IllegalStateException {
        Objects.requireNonNull(src);
        Ints.requireNonNegative(srcBegin);
        Ints.requireNonNegative(len);
        if ((srcBegin + len) > src.capacity()) {
            throw new IllegalArgumentException(format("require (%d + %d) out of src capacity %d",
                    srcBegin, len, src.capacity()));
        }
        if (len > writeRemaining()) {
            throw new DecoratedBufferOverflowException(writePosition() + len, writePosition(), writeRemaining());
        }
        ensureCapacity(writePosition() + len);
        this.zstore.write(writePosition(), src, srcBegin, len);
        uncheckedWritePosition(writePosition() + len);
        return len;
    }

    @Override
    public byte readByte(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        readCheckOffset(offset, Byte.BYTES, true);
        return this.zstore.readByte(offset);
    }

    @Override
    public short readShort(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        readCheckOffset(offset, Short.BYTES, true);
        return this.zstore.readShort(offset);
    }

    @Override
    public int readInt(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        readCheckOffset(offset, Integer.BYTES, true);
        return this.zstore.readInt(offset);
    }

    @Override
    public long readLong(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        readCheckOffset(offset, Long.BYTES, true);
        return this.zstore.readLong(offset);
    }

    @Override
    public float readFloat(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        readCheckOffset(offset, Float.BYTES, true);
        return this.zstore.readFloat(offset);
    }

    @Override
    public double readDouble(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        readCheckOffset(offset, Double.BYTES, true);
        return this.zstore.readDouble(offset);
    }

    @Override
    public byte readByteVolatile(long offset) throws IllegalStateException, DecoratedBufferOverflowException {
        readCheckOffset(offset, Byte.BYTES, true);
        return this.zstore.readByteVolatile(offset);
    }

    @Override
    public short readShortVolatile(long offset) throws IllegalStateException, DecoratedBufferOverflowException {
        readCheckOffset(offset, Short.BYTES, true);
        return this.zstore.readShortVolatile(offset);
    }

    @Override
    public int readIntVolatile(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        readCheckOffset(offset, Integer.BYTES, true);
        return this.zstore.readIntVolatile(offset);
    }

    @Override
    public long readLongVolatile(long offset)
            throws IllegalStateException, DecoratedBufferOverflowException {
        readCheckOffset(offset, Long.BYTES, true);
        return this.zstore.readLongVolatile(offset);
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
        int left = (int) Math.min(len, readLimit() - offset);
        if (left <= 0) {
            return -1;
        }
        return this.zstore.read(offset, dst, dstBegin, left);
    }

    @Override
    public int read(long offset, ByteBuffer dst, int dstBegin, int len)
            throws IllegalStateException, DecoratedBufferOverflowException {
        Objects.requireNonNull(dst);
        Longs.requireNonNegative(offset);
        Ints.requireNonNegative(dstBegin);
        Ints.requireNonNegative(len);
        if ((dstBegin + len) > dst.capacity()) {
            throw new IllegalArgumentException(format("%d + %d is out of dst capacity %d", dstBegin, len, dst.capacity()));
        }
        int left = (int) Math.min(len, readLimit() - offset);
        if (left <= 0) {
            return -1;
        }
        return this.zstore.read(offset, dst, dstBegin, left);
    }

    @Override
    public byte readByte() throws IllegalStateException {
        long offset = readAdvanceOffset(Byte.BYTES);
        try {
            return this.zstore.readByte(offset);
        } catch (DecoratedBufferOverflowException ex) {
            if (isLenient()) return 0;
            throw ex;
        }
    }

    @Override
    public short readShort() throws IllegalStateException {
        long offset = readAdvanceOffset(Short.BYTES);
        try {
            return this.zstore.readShort(offset);
        } catch (DecoratedBufferOverflowException ex) {
            if (isLenient()) return 0;
            throw ex;
        }
    }

    @Override
    public int readInt() throws IllegalStateException {
        long offset = readAdvanceOffset(Integer.BYTES);
        try {
            return this.zstore.readInt(offset);
        } catch (DecoratedBufferOverflowException ex) {
            if (isLenient()) return 0;
            throw ex;
        }
    }

    @Override
    public long readLong() throws IllegalStateException {
        long offset = readAdvanceOffset(Long.BYTES);
        try {
            return this.zstore.readLong(offset);
        } catch (DecoratedBufferOverflowException ex) {
            if (isLenient()) return 0;
            throw ex;
        }
    }

    @Override
    public float readFloat() throws IllegalStateException {
        long offset = readAdvanceOffset(Float.BYTES);
        try {
            return this.zstore.readFloat(offset);
        } catch (DecoratedBufferOverflowException ex) {
            if (isLenient()) return 0;
            throw ex;
        }
    }

    @Override
    public double readDouble() throws IllegalStateException {
        long offset = readAdvanceOffset(Double.BYTES);
        try {
            return this.zstore.readDouble(offset);
        } catch (DecoratedBufferOverflowException ex) {
            if (isLenient()) return 0;
            throw ex;
        }
    }

    @Override
    public byte readByteVolatile() throws IllegalStateException {
        long offset = readAdvanceOffset(Byte.BYTES);
        try {
            return this.zstore.readByteVolatile(offset);
        } catch (DecoratedBufferOverflowException ex) {
            if (isLenient()) return 0;
            throw ex;
        }
    }

    @Override
    public short readShortVolatile() throws IllegalStateException {
        long offset = readAdvanceOffset(Short.BYTES);
        try {
            return this.zstore.readShortVolatile(offset);
        } catch (DecoratedBufferOverflowException ex) {
            if (isLenient()) return 0;
            throw ex;
        }
    }

    @Override
    public int readIntVolatile() throws IllegalStateException {
        long offset = readAdvanceOffset(Integer.BYTES);
        try {
            return this.zstore.readIntVolatile(offset);
        } catch (DecoratedBufferOverflowException ex) {
            if (isLenient()) return 0;
            throw ex;
        }
    }

    @Override
    public long readLongVolatile() throws IllegalStateException {
        long offset = readAdvanceOffset(Long.BYTES);
        try {
            return this.zstore.readLongVolatile(offset);
        } catch (DecoratedBufferOverflowException ex) {
            if (isLenient()) return 0;
            throw ex;
        }
    }

    @Override
    public int read(byte[] dst, int dstBegin, int len) throws IllegalStateException {
        Objects.requireNonNull(dst);
        Ints.requireNonNegative(dstBegin);
        Ints.requireNonNegative(len);
        int left = (int) Math.min(len, readRemaining());
        if (left <= 0) {
            return -1;
        }
        while (left > 0) {
            int copy = Math.min(left, thresholdCopySize());
            long offset = readAdvanceOffset(copy);
            this.zstore.read(offset, dst, dstBegin, copy);
            dstBegin += copy;
            left -= copy;
        }
        return left;
    }

    @Override
    public int read(ByteBuffer dst, int dstBegin, int len) throws IllegalStateException {
        Objects.requireNonNull(dst);
        Ints.requireNonNegative(dstBegin);
        Ints.requireNonNegative(len);
        int left = (int) Math.min(len, readRemaining());
        if (left <= 0) {
            return -1;
        }
        while (left > 0) {
            int copy = Math.min(left, thresholdCopySize());
            long offset = readAdvanceOffset(copy);
            this.zstore.read(offset, dst, dstBegin, copy);
            dstBegin += copy;
            left -= copy;
        }
        return left;
    }

    @Override
    public void nativeWrite(long address, long len)
            throws IllegalStateException {
        long offset = writeAdvanceOffset(len);
        this.zstore.nativeWrite(address, offset, len);
    }

    @Override
    public void nativeWrite(long address, long offset, long len)
            throws IllegalStateException {
        ensureCapacity(offset + len);
        this.zstore.nativeWrite(address, offset, len);
    }

    @Override
    public void nativeRead(long address, long len)
            throws IllegalStateException {
        long offset = readAdvanceOffset(len);
        this.zstore.nativeRead(address, offset, len);
    }

    @Override
    public void nativeRead(long offset, long address, long len)
            throws IllegalStateException {
        ensureCapacity(offset + len);
        this.zstore.nativeRead(offset, address, len);
    }

    @Override
    public void move(long from, long to, long len)
            throws IllegalStateException, DecoratedBufferOverflowException {
        if (from < 0 || to < 0) {
            throw new IllegalArgumentException(format("from (%d) or to (%d) is negative", from, to));
        }
        if (len <= 0) {
            return;
        }
        assertTheadSafe();
        long start = start();
        ensureCapacity(to + len);
        this.zstore.move(from - start, to - start, len);
    }

    @Override
    public long addressForRead(long offset) throws DecoratedBufferOverflowException {
        readCheckOffset(offset, 0, true);
        return this.zstore.addressForRead(offset);
    }

    @Override
    public long addressForWrite(long offset) throws DecoratedBufferOverflowException {
        writeCheckOffset(offset, 0);
        return this.zstore.addressForWrite(offset);
    }

    @Override
    public void clear() throws IllegalStateException {
        long start  = start();
        long cap = capacity();
        if (readPosition() == start && writePosition() == start && writeLimit() == cap) {
            return;
        }
        assertTheadSafe();
        uncheckedReadPosition(start);
        uncheckedWritePosition(start);
        writeLimit(cap);
    }

    @Override
    protected void doFree0() throws IllegalStateException {
        try {
            this.zstore.release(this);
        } catch (IllegalStateException ex) {
            log.warn("error while freeing", ex);
        }
    }

    private final class UncheckedRandomInputHolder implements UncheckedRandomInput {

        private UncheckedRandomInputHolder() {
        }

        @Override
        public byte readByte(long offset) {
            return memory().readByte(underlyingObject(), byteStore().translate(offset));
        }

        @Override
        public short readShort(long offset) {
            return memory().readShort(underlyingObject(), byteStore().translate(offset));
        }

        @Override
        public int readInt(long offset) {
            return memory().readInt(underlyingObject(), byteStore().translate(offset));
        }

        @Override
        public long readLong(long offset) {
            return memory().readLong(underlyingObject(), byteStore().translate(offset));
        }
    }

    static final class ReportUnoptimized {
        static {
            Jvm.reportUnoptimized();
        }

        private ReportUnoptimized() {
        }

        static void reportOnce() {
        }
    }

}
