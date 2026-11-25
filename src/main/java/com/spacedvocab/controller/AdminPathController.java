package com.spacedvocab.controller;

import com.spacedvocab.dto.AddLessonRequest;
import com.spacedvocab.dto.MoveLessonRequest;
import com.spacedvocab.dto.PathLessonResponse;
import com.spacedvocab.dto.RenameStageRequest;
import com.spacedvocab.service.PathAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** D26: path management. {@code /api/admin/**} is ADMIN-only (SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminPathController {

    private final PathAdminService pathAdminService;

    public AdminPathController(PathAdminService pathAdminService) {
        this.pathAdminService = pathAdminService;
    }

    @PutMapping("/stages/{stageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void renameStage(@PathVariable Long stageId, @Valid @RequestBody RenameStageRequest request) {
        pathAdminService.renameStage(stageId, request);
    }

    @PostMapping("/stages/{stageId}/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    public PathLessonResponse addLesson(@PathVariable Long stageId, @Valid @RequestBody AddLessonRequest request) {
        return pathAdminService.addLesson(stageId, request);
    }

    @PutMapping("/lessons/{lessonId}")
    public PathLessonResponse moveLesson(@PathVariable Long lessonId, @Valid @RequestBody MoveLessonRequest request) {
        return pathAdminService.moveLesson(lessonId, request.position());
    }

    @DeleteMapping("/lessons/{lessonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeLesson(@PathVariable Long lessonId) {
        pathAdminService.removeLesson(lessonId);
    }
}
