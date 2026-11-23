package com.spacedvocab.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.ZoneId;

public class TimeZoneValidator implements ConstraintValidator<ValidTimeZone, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // region ids only ("Asia/Ho_Chi_Minh", "UTC"); offsets like "+07:00" ignore daylight saving rules
        return value == null || ZoneId.getAvailableZoneIds().contains(value);
    }
}
