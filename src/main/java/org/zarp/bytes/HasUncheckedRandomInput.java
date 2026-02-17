package org.zarp.bytes;

@FunctionalInterface
public interface HasUncheckedRandomInput {

    /**
     * @return a view for reading data randomly without validations
     */
    UncheckedRandomInput acquireUncheckedInput();

}
