package com.spacedvocab.service;

import com.spacedvocab.dto.AddLessonRequest;
import com.spacedvocab.dto.PathLessonResponse;
import com.spacedvocab.dto.RenameStageRequest;
import com.spacedvocab.entity.Deck;
import com.spacedvocab.entity.DeckVisibility;
import com.spacedvocab.entity.PathLesson;
import com.spacedvocab.entity.PathStage;
import com.spacedvocab.exception.BusinessRuleViolationException;
import com.spacedvocab.exception.DuplicateResourceException;
import com.spacedvocab.exception.ErrorCode;
import com.spacedvocab.exception.ResourceNotFoundException;
import com.spacedvocab.repository.DeckRepository;
import com.spacedvocab.repository.PathLessonRepository;
import com.spacedvocab.repository.PathStageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-A5 (D26): admins rename stages and choose which OFFICIAL decks are lessons (R10).
 * Removing a lesson never locks a stage again (R11).
 */
@Service
public class PathAdminService {

    private static final Logger log = LoggerFactory.getLogger(PathAdminService.class);

    private final PathStageRepository stageRepository;
    private final PathLessonRepository lessonRepository;
    private final DeckRepository deckRepository;

    public PathAdminService(PathStageRepository stageRepository, PathLessonRepository lessonRepository,
                            DeckRepository deckRepository) {
        this.stageRepository = stageRepository;
        this.lessonRepository = lessonRepository;
        this.deckRepository = deckRepository;
    }

    @Transactional
    public void renameStage(Long stageId, RenameStageRequest request) {
        PathStage stage = stageRepository.findById(stageId).orElseThrow(ResourceNotFoundException::new);
        stage.rename(request.title());
    }

    @Transactional
    public PathLessonResponse addLesson(Long stageId, AddLessonRequest request) {
        PathStage stage = stageRepository.findById(stageId).orElseThrow(ResourceNotFoundException::new);
        Deck deck = deckRepository.findByIdAndDeletedAtIsNull(request.deckId())
                .orElseThrow(ResourceNotFoundException::new);
        if (deck.getVisibility() != DeckVisibility.OFFICIAL) {
            throw new BusinessRuleViolationException(ErrorCode.LESSON_DECK_NOT_OFFICIAL, "deckId");
        }
        if (lessonRepository.existsByDeckId(deck.getId())) {
            throw new DuplicateResourceException(ErrorCode.DECK_ALREADY_IN_PATH, "deckId");
        }
        PathLesson lesson = new PathLesson(stage, deck, request.position());
        try {
            lessonRepository.saveAndFlush(lesson);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException(ErrorCode.DECK_ALREADY_IN_PATH, "deckId");
        }
        log.info("Deck {} added as lesson {} of stage {}", deck.getId(), lesson.getId(), stageId);
        return PathLessonResponse.from(lesson);
    }

    @Transactional
    public PathLessonResponse moveLesson(Long lessonId, int position) {
        PathLesson lesson = lessonRepository.findActive(lessonId).orElseThrow(ResourceNotFoundException::new);
        lesson.moveTo(position);
        return PathLessonResponse.from(lesson);
    }

    @Transactional
    public void removeLesson(Long lessonId) {
        PathLesson lesson = lessonRepository.findById(lessonId).orElseThrow(ResourceNotFoundException::new);
        lessonRepository.delete(lesson);
        log.info("Lesson {} removed from stage {}", lessonId, lesson.getStage().getId());
    }
}
