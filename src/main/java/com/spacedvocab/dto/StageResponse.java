package com.spacedvocab.dto;

import java.util.List;

/** D24: a stage with its lessons. */
public record StageResponse(StageSummaryResponse stage, List<LessonResponse> lessons) {
}
