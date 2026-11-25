package com.spacedvocab.dto;

import java.util.List;

/** D23: the path with the learner's progress on every stage. */
public record PathResponse(Long id, String name, String description, List<StageSummaryResponse> stages) {
}
