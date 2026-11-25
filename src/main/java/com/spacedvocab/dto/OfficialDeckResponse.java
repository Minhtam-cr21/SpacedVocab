package com.spacedvocab.dto;

import com.spacedvocab.repository.OfficialDeckRow;

/** D26 deck picker: an OFFICIAL deck and the lesson/stage using it ({@code null} when it is free). */
public record OfficialDeckResponse(Long id, String name, long cardCount, Long lessonId, Long stageId) {

    public static OfficialDeckResponse from(OfficialDeckRow row) {
        return new OfficialDeckResponse(row.id(), row.name(), row.cardCount(), row.lessonId(), row.stageId());
    }
}
