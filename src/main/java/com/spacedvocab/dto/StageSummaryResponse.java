package com.spacedvocab.dto;

import com.spacedvocab.entity.CefrLevel;
import com.spacedvocab.entity.UnlockMethod;
import java.time.Instant;

/**
 * One stage card of D23.
 *
 * @param unlockMethod how this stage was opened; {@code null} for the first stage or a locked one
 * @param testAvailable the stage test can be started now (R12)
 * @param testRetryAt   when the stage test can be retried after a failure, otherwise {@code null}
 */
public record StageSummaryResponse(
        Long id,
        CefrLevel cefrLevel,
        String title,
        int position,
        int lessonCount,
        int completedLessonCount,
        boolean unlocked,
        UnlockMethod unlockMethod,
        boolean testAvailable,
        Instant testRetryAt) {
}
