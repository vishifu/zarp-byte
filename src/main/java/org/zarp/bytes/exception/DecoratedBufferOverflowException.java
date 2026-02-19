package org.zarp.bytes.exception;

import java.io.Serial;
import java.nio.BufferOverflowException;

public class DecoratedBufferOverflowException extends BufferOverflowException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String message;

    public DecoratedBufferOverflowException(String message){
        super();
        this.message = message;
    }

    public DecoratedBufferOverflowException(long idx, long low, long hi) {
        this("index " + idx + " is overflow of range [" + low + "," + hi + ")");
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
