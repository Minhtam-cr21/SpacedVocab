package com.spacedvocab.repository;

/**
 * One lesson with the learner's progress on it (R10).
 *
 * @param cardCount    cards of the lesson's deck that are not deleted
 * @param learnedCount those cards the learner has reviewed at least once
 */
public record LessonProgress(Long lessonId, Long stageId, Long deckId, String deckName, int position,
                             long cardCount, long learnedCount) {

    /** R10: every card has been studied at least once. A lesson without cards has nothing left to learn. */
    public boolean completed() {
        return learnedCount >= cardCount;
    }
}
