package com.qpss.exception;

public class QuestionParsingException extends RuntimeException {
    public QuestionParsingException(String message) {
        super(message);
    }

    public QuestionParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
