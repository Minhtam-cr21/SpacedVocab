package com.spacedvocab.dto;

import com.spacedvocab.entity.PathLesson;

/** D26: a lesson as the admin manages it. */
public record PathLessonResponse(Long id, Long stageId, Long deckId, String deckName, int position) {

    public static PathLessonResponse from(PathLesson lesson) {
        return new PathLessonResponse(lesson.getId(), lesson.getStage().getId(), lesson.getDeck().getId(),
                lesson.getDeck().getName(), lesson.getPosition());
    }
}
