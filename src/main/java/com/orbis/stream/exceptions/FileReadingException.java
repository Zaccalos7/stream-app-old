package com.orbis.stream.exceptions;


public class FileReadingException extends RuntimeException {
    private final Object[] params;

    public FileReadingException (String message) {
        super(message);
        this.params = null;
    }

    public FileReadingException(String message, Object[] params) {
        super(message);
        this.params = params;
    }
    public Object[] getParams(){
        return params;
    }
}

