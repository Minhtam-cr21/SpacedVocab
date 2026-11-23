package com.spacedvocab.exception;

import org.springframework.http.HttpStatus;

/** 409. A unique business key already exists (R1 email, R2 deck name / card word). */
public class DuplicateResourceException extends AppException {

    public DuplicateResourceException(ErrorCode code, String field) {
        super(code, null, field);
    }

    public DuplicateResourceException(ErrorCode code, String message, String field) {
        super(code, message, field);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
