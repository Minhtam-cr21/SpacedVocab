package com.spacedvocab.dto;

import com.spacedvocab.repository.LessonProgress;

/** One lesson row of D24. {@code learnedCount} cards have been studied at least once (R10). */
public record LessonResponse(Long id, Long deckId, String name, int position, long cardCount, long learnedCount,
                             boolean completed) {

    public static LessonResponse from(LessonProgress lesson) {
        return new LessonResponse(lesson.lessonId(), lesson.deckId(), lesson.deckName(), lesson.position(),
                lesson.cardCount(), lesson.learnedCount(), lesson.completed());
    }
}
