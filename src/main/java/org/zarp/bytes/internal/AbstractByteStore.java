package org.zarp.bytes.internal;

import org.zarp.bytes.ZByteStore;
import org.zarp.bytes.algo.ByteHashDigest;
import org.zarp.bytes.utils.ByteCommon;
import org.zarp.core.io.AbstractReferenceCount;

/**
 * An abstract class for {@link ZByteStore} implementation. This extends {@link AbstractReferenceCount} to manage
 * reference counting and supplies default behavior for many operations.
 * <p>
 * Concrete class are expected to implement the raw data access and capacity related methods.
 *
 * @param <U> underlying type
 */
public abstract class AbstractByteStore<U> extends AbstractReferenceCount implements ZByteStore<U> {

    protected AbstractByteStore() {
        super();
    }

    protected AbstractByteStore(boolean monitored) {
        super(monitored);
    }

    @Override
    protected boolean canFreeInBackground() {
        return isNative();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AbstractByteStore<?> && ByteCommon.contentEquals(this, (ZByteStore<?>) obj);
    }

    @Override
    public int hashCode() {
        return ByteHashDigest.hash32(this);
    }

}
