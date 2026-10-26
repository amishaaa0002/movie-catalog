package com.catalog.movie.exception;

/** Request is well-formed but violates a catalog rule, e.g. too many genres. Mapped to 400. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
