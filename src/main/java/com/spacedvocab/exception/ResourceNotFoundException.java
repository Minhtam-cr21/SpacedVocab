package com.spacedvocab.exception;

import org.springframework.http.HttpStatus;

/** 404. Also used for resources the caller may not see (e.g. another user's PRIVATE deck). */
public class ResourceNotFoundException extends AppException {

    public ResourceNotFoundException() {
        super(ErrorCode.NOT_FOUND, null, null);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
