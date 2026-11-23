package com.spacedvocab.exception;

import org.springframework.http.HttpStatus;

/** 403. The caller is known but not allowed to do this (wrong role, locked account). */
public class ForbiddenOperationException extends AppException {

    public ForbiddenOperationException() {
        this(ErrorCode.FORBIDDEN);
    }

    public ForbiddenOperationException(ErrorCode code) {
        super(code, null, null);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
