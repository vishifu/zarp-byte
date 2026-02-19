package org.zarp.bytes;

public interface AddressTranslate {

    /**
     * Translates an offset into memory address.
     *
     * @param offset logical position
     * @return access memory address
     */
    long translate(long offset);

}
