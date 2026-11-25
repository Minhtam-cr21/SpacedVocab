package com.spacedvocab.dto;

import com.spacedvocab.entity.StageTestAttempt;
import java.util.List;

/** D25 result: the score and, now that the test is over, the expected word of every question. */
public record StageTestResultResponse(int correctCount, int total, boolean passed, boolean nextStageUnlocked,
                                      List<Question> questions) {

    public record Question(int position, String meaning, String partOfSpeech, String answer, String correctWord,
                           boolean correct) {
    }

    public static StageTestResultResponse from(StageTestAttempt attempt, boolean nextStageUnlocked) {
        List<Question> questions = attempt.getQuestions().stream()
                .map(q -> new Question(q.getPosition(), q.getCard().getContent().meaning(),
                        q.getCard().getContent().partOfSpeech(), q.getAnswer(), q.getCard().getContent().word(),
                        q.isCorrect()))
                .toList();
        return new StageTestResultResponse(attempt.getCorrectCount(), questions.size(), attempt.isPassed(),
                nextStageUnlocked, questions);
    }
}
