package com.spacedvocab.dto;

import com.spacedvocab.entity.StageTestAttempt;
import java.time.Instant;
import java.util.List;

/** D25: the questions of a stage test, never with their answers (R12). */
public record StageTestResponse(Long attemptId, Long stageId, Instant expiresAt, List<Question> questions) {

    public record Question(int position, String meaning, String partOfSpeech) {
    }

    public static StageTestResponse from(StageTestAttempt attempt) {
        List<Question> questions = attempt.getQuestions().stream()
                .map(q -> new Question(q.getPosition(), q.getCard().getContent().meaning(),
                        q.getCard().getContent().partOfSpeech()))
                .toList();
        return new StageTestResponse(attempt.getId(), attempt.getStage().getId(), attempt.getExpiresAt(), questions);
    }
}
