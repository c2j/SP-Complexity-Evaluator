package com.sdchat.ce.sp.complexity.exception;

/**
 * Exception thrown when SQL validation fails.
 */
public class SqlValidationException extends Exception {

    public SqlValidationException(String message) {
        super(message);
    }

    public SqlValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
