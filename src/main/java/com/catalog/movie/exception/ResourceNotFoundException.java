package com.catalog.movie.exception;

/** Requested resource does not exist. Mapped to 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Long id) {
        this(resource + " not found with id " + id);
    }
}
