package com.sdchat.ce.sp.complexity.exception;

/**
 * Exception thrown when loading resources fails.
 */
public class ResourceLoadException extends Exception {

    public ResourceLoadException(String message) {
        super(message);
    }

    public ResourceLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
