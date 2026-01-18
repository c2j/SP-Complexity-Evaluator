package com.sdchat.ce.sp.complexity.exception;

/**
 * Exception thrown when complexity evaluation fails.
 */
public class EvaluationException extends Exception {

    public EvaluationException(String message) {
        super(message);
    }

    public EvaluationException(String message, Throwable cause) {
        super(message, cause);
    }
}
