package com.orbis.stream.exceptions;

public class NotFoundCustomException extends RuntimeException {
    private final Object[] params;

    public NotFoundCustomException (String message) {
        super(message);
        this.params = null;
    }

    public NotFoundCustomException(String message, Object[] params) {
        super(message);
        this.params = params;
    }
    public Object[] getParams(){
        return params;
    }
}
