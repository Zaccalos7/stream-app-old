package com.orbis.stream.exceptions;

public class StreamingException extends RuntimeException {
    private final Object[] params;

    public Object[] getParams(){
        return params;
    }

    public StreamingException(String message, Object[] parmas, Object[] params) {

        super(message);
        this.params = params;
    }

    public StreamingException(String message, Object[] params) {
        super(message);
        this.params = params;
    }
}
