package com.gaminggear.exception;

/**
 * Exception thrown when a requested entity cannot be found in the data store.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
