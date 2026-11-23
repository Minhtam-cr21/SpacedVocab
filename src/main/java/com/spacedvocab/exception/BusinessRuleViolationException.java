package com.spacedvocab.exception;

import org.springframework.http.HttpStatus;

/** 422. The request is well-formed but breaks a business rule (R1–R9). */
public class BusinessRuleViolationException extends AppException {

    public BusinessRuleViolationException(ErrorCode code) {
        super(code, null, null);
    }

    public BusinessRuleViolationException(ErrorCode code, String field) {
        super(code, null, field);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNPROCESSABLE_CONTENT;
    }
}
