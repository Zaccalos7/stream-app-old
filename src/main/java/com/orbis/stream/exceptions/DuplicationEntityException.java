package com.orbis.stream.exceptions;

public class DuplicationEntityException extends RuntimeException {
    private final Object[] params;

    public DuplicationEntityException (String message) {
        super(message);
        this.params = null;
    }

    public DuplicationEntityException(String message, Object[] params) {
        super(message);
        this.params = params;
    }
    public Object[] getParams(){
        return params;
    }
}



