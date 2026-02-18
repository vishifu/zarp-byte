package org.zarp.bytes;

/**
 * Represents for layer accessing data sequentially, it manages fundamental buffer properties and support random access
 * semantic.
 */
public interface StreamCommon extends RandomAccess {

    /**
     * Clears all data of this streaming source, generally by seek read and write pointer.
     */
    void clear() throws IllegalStateException;

}
