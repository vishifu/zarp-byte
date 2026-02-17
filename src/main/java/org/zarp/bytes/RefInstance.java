package org.zarp.bytes;

import org.zarp.core.io.ReferenceCountable;

public interface RefInstance extends ReferenceCountable {

    /**
     * Checks to ensure that this instance is not released yet.
     */
    default void ensureNotReleased() throws IllegalStateException {
        if (refCount() <= 0) {
            // This is a trick, rather than throws an Exception, we call releaseLast() which will attempt
            // to release last owner and throws error with more information
            releaseLast();
        }
    }

}
