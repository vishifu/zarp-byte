package org.zarp.bytes.internal;

import org.zarp.bytes.ZByteStore;
import org.zarp.core.io.ReferenceCountListener;
import org.zarp.core.io.ReferenceOwnable;

public class OnHeapByteStore<U> implements ZByteStore<U> {


    public long dataOffset() {

    }

    public Object actualUnderlyingObject() {

    }

    @Override
    public U underlyingObject() {
        return null;
    }

    @Override
    public void move(long from, long to, long len) throws IllegalStateException, IndexOutOfBoundsException {

    }

    @Override
    public byte readByte(long offset) throws IllegalStateException, IndexOutOfBoundsException {
        return 0;
    }

    @Override
    public short readShort(long offset) throws IllegalStateException, IndexOutOfBoundsException {
        return 0;
    }

    @Override
    public short readInt(long offset) throws IllegalStateException, IndexOutOfBoundsException {
        return 0;
    }

    @Override
    public long readLong(long offset) throws IllegalStateException, IndexOutOfBoundsException {
        return 0;
    }

    @Override
    public float readFloat(long offset) throws IllegalStateException, IndexOutOfBoundsException {
        return 0;
    }

    @Override
    public double readDouble(long offset) throws IllegalStateException, IndexOutOfBoundsException {
        return 0;
    }

    @Override
    public byte readByteVolatile(long offset) throws IllegalStateException, IndexOutOfBoundsException {
        return 0;
    }

    @Override
    public short readShortVolatile(long offset) throws IllegalStateException, IndexOutOfBoundsException {
        return 0;
    }

    @Override
    public int readIntVolatile(long offset) throws IllegalStateException, IndexOutOfBoundsException {
        return 0;
    }

    @Override
    public long readLongVolatile(long offset) throws IllegalStateException, IndexOutOfBoundsException {
        return 0;
    }

    @Override
    public void writeByte(long offset, byte i8) throws IllegalStateException, IndexOutOfBoundsException {

    }

    @Override
    public void writeShort(long offset, short i16) throws IllegalStateException, IndexOutOfBoundsException {

    }

    @Override
    public void writeInt(long offset, int i32) {

    }

    @Override
    public void writeLong(long offset, long i64) throws IllegalStateException, IndexOutOfBoundsException {

    }

    @Override
    public void writeFloat(long offset, float f32) throws IllegalStateException, IndexOutOfBoundsException {

    }

    @Override
    public void writeDouble(long offset, double f64) throws IllegalStateException, IndexOutOfBoundsException {

    }

    @Override
    public void writeIntOrdered(long offset, int i32) throws IllegalStateException, IndexOutOfBoundsException {

    }

    @Override
    public void writeLongOrdered(long offset, long i64) throws IllegalStateException, IndexOutOfBoundsException {

    }

    @Override
    public void writeIntVolatile(long offset, int i32) throws IllegalStateException, IndexOutOfBoundsException {

    }

    @Override
    public void writeLongVolatile(long offset, long i64) throws IllegalStateException, IndexOutOfBoundsException {

    }

    @Override
    public boolean compareAndSwap(long offset, int expected, int value) throws IllegalStateException, IndexOutOfBoundsException {
        return false;
    }

    @Override
    public boolean compareAndSwap(long offset, long expected, long value) throws IllegalStateException, IndexOutOfBoundsException {
        return false;
    }

    @Override
    public void testAndSet(long offset, int expected, int value) throws IllegalStateException, IndexOutOfBoundsException {

    }

    @Override
    public void testAndSet(long offset, long expected, long value) throws IllegalStateException, IndexOutOfBoundsException {

    }

    @Override
    public void nativeWrite(long address, long offset, long len) throws IllegalStateException, IndexOutOfBoundsException {

    }

    @Override
    public long addressForRead(long offset) throws IndexOutOfBoundsException {
        return 0;
    }

    @Override
    public long addressForWrite(long offset) throws IndexOutOfBoundsException {
        return 0;
    }

    @Override
    public boolean isHeap() {
        return false;
    }

    @Override
    public boolean isNative() {
        return false;
    }

    @Override
    public int refCount() throws IllegalStateException {
        return 0;
    }

    @Override
    public void reserve(ReferenceOwnable referenceOwnable) throws IllegalStateException {

    }

    @Override
    public boolean tryReserve(ReferenceOwnable referenceOwnable) {
        return false;
    }

    @Override
    public void release(ReferenceOwnable referenceOwnable) throws IllegalStateException {

    }

    @Override
    public void releaseLast(ReferenceOwnable referenceOwnable) throws IllegalStateException {

    }

    @Override
    public void addListener(ReferenceCountListener referenceCountListener) {

    }

    @Override
    public void removeListener(ReferenceCountListener referenceCountListener) {

    }
}
