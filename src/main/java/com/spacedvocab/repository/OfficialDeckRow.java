package com.spacedvocab.repository;

/** Query projection: an OFFICIAL deck, its card count and the lesson using it ({@code null} ids if none). */
public record OfficialDeckRow(Long id, String name, long cardCount, Long lessonId, Long stageId) {
}
