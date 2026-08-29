package com.qpss.exception;

public class DocumentRenderingException extends RuntimeException {
    public DocumentRenderingException(String message) {
        super(message);
    }
    public DocumentRenderingException(String message, Throwable cause) {
        super(message, cause);
    }
}
