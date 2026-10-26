package com.catalog.movie.exception;

/** Request conflicts with the current state, e.g. a duplicate name. Mapped to 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
