package com.orbis.stream.exceptions;


public class LiveException extends RuntimeException {

    private final Object[] params;

    public LiveException(String message, Object[] params) {
        super(message);
        this.params = params;
    }

    public LiveException(String message) {
        super(message);
        this.params = null;
    }

    public Object[] getParams(){
        return params;
    }
}
