package org.zarp.bytes;

import org.zarp.bytes.exception.DecoratedBufferOverflowException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Presents a buffer for raw data with separate 63-bit read/write pointer.
 * This class might wrap a {@link ZByteStore} which reside on heap memory or native memory, all intances will be
 * elastic and  reference counted.
 * <p>
 * This is not thread-safe.
 *
 * @param <U> underlying type
 */
public interface ZBytes<U> extends ZByteStore<U>, ByteIn, ByteOut {

    /**
     * @return true if this is elastic, false otherwise
     */
    boolean isElastic();

    /**
     * Retrieves the backing store that this object wraps.
     * If this does not wrap any store, return null.
     *
     * @return backing byte-store, or null if there is none
     */
    ZByteStore<U> byteStore();

    @Override
    default void ensureCapacity(long requested) throws IllegalStateException, DecoratedBufferOverflowException {
        if (requested > capacity()) {
            throw new DecoratedBufferOverflowException(isElastic() ? "Required resizing" : "Requested size out of capacity");
        }
    }

    /**
     * Writes the input stream content into this instance, continues reading until reach the end of stream.
     * <p>
     * NOTE: this does not close the {@link InputStream}.
     *
     * @param is input
     */
    default void write(InputStream is) throws IllegalStateException, IOException {
        Objects.requireNonNull(is);
        int read;
        for (; ; ) {
            read = is.read();
            if (read == -1) {
                break;
            }
            writeByte(read);
        }
    }

}
