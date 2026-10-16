package com.chubb.claims.exception;

/** Signals a violated business rule, e.g. an invalid claim status transition. Mapped to HTTP 409. */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
