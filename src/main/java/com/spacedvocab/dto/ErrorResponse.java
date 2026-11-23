package com.spacedvocab.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Body of every error response: {@code { code, message, field?, errors? }}.
 * {@code errors} lists every invalid field for {@code VALIDATION_FAILED}; {@code field} is the first one.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String code, String message, String field, List<FieldError> errors) {

    public static ErrorResponse of(String code, String message, String field) {
        return new ErrorResponse(code, message, field, null);
    }

    public record FieldError(String field, String message) {
    }
}
