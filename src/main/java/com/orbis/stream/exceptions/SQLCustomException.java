package com.orbis.stream.exceptions;

public class SQLCustomException extends RuntimeException {
    private final Object[] params;

    public SQLCustomException(String message, Object[] parmas) {
        super(message);
        this.params = parmas;
    }

    public SQLCustomException(String message) {
        super(message);
        this.params = null;
    }

    public Object[] getParams(){
        return params;
    }
}
