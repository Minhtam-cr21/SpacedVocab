package com.spacedvocab.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * The learner's current day in their own timezone (R7, R9), as the UTC range {@code [start, end)}.
 */
public record StudyDay(LocalDate date, Instant start, Instant end) {

    public static StudyDay today(Clock clock, ZoneId zone) {
        LocalDate date = LocalDate.ofInstant(clock.instant(), zone);
        return new StudyDay(date, date.atStartOfDay(zone).toInstant(), date.plusDays(1).atStartOfDay(zone).toInstant());
    }
}
