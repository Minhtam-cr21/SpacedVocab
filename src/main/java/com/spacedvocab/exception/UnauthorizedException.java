package com.spacedvocab.exception;

import org.springframework.http.HttpStatus;

/** 401. Who the caller is could not be established (wrong credentials, expired or revoked token). */
public class UnauthorizedException extends AppException {

    public UnauthorizedException(ErrorCode code) {
        super(code, null, null);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
