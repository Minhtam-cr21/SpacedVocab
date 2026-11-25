package com.spacedvocab.controller;

import com.spacedvocab.config.security.AuthenticatedUser;
import com.spacedvocab.dto.LessonStartResponse;
import com.spacedvocab.dto.PathResponse;
import com.spacedvocab.dto.PathSummaryResponse;
import com.spacedvocab.dto.StageResponse;
import com.spacedvocab.dto.StageTestResponse;
import com.spacedvocab.dto.StageTestResultResponse;
import com.spacedvocab.dto.StageTestSubmitRequest;
import com.spacedvocab.service.PathService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** D23 path, D24 stage, D25 stage test (business-spec §6). */
@RestController
@RequestMapping("/api")
public class PathController {

    private final PathService pathService;

    public PathController(PathService pathService) {
        this.pathService = pathService;
    }

    @GetMapping("/paths")
    public List<PathSummaryResponse> list() {
        return pathService.list();
    }

    @GetMapping("/paths/{pathId}")
    public PathResponse get(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long pathId) {
        return pathService.get(pathId, me);
    }

    @GetMapping("/stages/{stageId}")
    public StageResponse stage(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long stageId) {
        return pathService.getStage(stageId, me);
    }

    @PostMapping("/lessons/{lessonId}/start")
    public LessonStartResponse startLesson(@AuthenticationPrincipal AuthenticatedUser me,
                                           @PathVariable Long lessonId) {
        return pathService.startLesson(lessonId, me);
    }

    /** 201 for a new attempt, 200 when the attempt in progress is returned. */
    @PostMapping("/stages/{stageId}/test-attempts")
    public ResponseEntity<StageTestResponse> startTest(@AuthenticationPrincipal AuthenticatedUser me,
                                                       @PathVariable Long stageId) {
        PathService.StartedTest started = pathService.startTest(stageId, me);
        return ResponseEntity.status(started.created() ? HttpStatus.CREATED : HttpStatus.OK).body(started.test());
    }

    @PostMapping("/stage-test-attempts/{attemptId}/submit")
    public StageTestResultResponse submitTest(@AuthenticationPrincipal AuthenticatedUser me,
                                              @PathVariable Long attemptId,
                                              @Valid @RequestBody StageTestSubmitRequest request) {
        return pathService.submitTest(attemptId, request, me);
    }
}
