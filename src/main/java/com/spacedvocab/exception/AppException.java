package com.spacedvocab.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class of all expected business errors. Each subclass fixes the HTTP status;
 * {@link GlobalExceptionHandler} turns it into {@code { code, message, field? }}.
 */
public abstract class AppException extends RuntimeException {

    private final ErrorCode code;
    private final String field;

    protected AppException(ErrorCode code, String message, String field) {
        super(message != null ? message : code.defaultMessage());
        this.code = code;
        this.field = field;
    }

    public abstract HttpStatus getStatus();

    public ErrorCode getCode() {
        return code;
    }

    /** Name of the request field the error belongs to, or {@code null} for a general error. */
    public String getField() {
        return field;
    }
}
