package com.spacedvocab.service;

import com.spacedvocab.config.security.AuthenticatedUser;
import com.spacedvocab.dto.LessonResponse;
import com.spacedvocab.dto.LessonStartResponse;
import com.spacedvocab.dto.PathResponse;
import com.spacedvocab.dto.PathSummaryResponse;
import com.spacedvocab.dto.StageResponse;
import com.spacedvocab.dto.StageSummaryResponse;
import com.spacedvocab.dto.StageTestResponse;
import com.spacedvocab.dto.StageTestResultResponse;
import com.spacedvocab.dto.StageTestSubmitRequest;
import com.spacedvocab.entity.Card;
import com.spacedvocab.entity.Deck;
import com.spacedvocab.entity.DeckSubscription;
import com.spacedvocab.entity.LearningPath;
import com.spacedvocab.entity.PathLesson;
import com.spacedvocab.entity.PathStage;
import com.spacedvocab.entity.StageTestAttempt;
import com.spacedvocab.entity.StageUnlock;
import com.spacedvocab.entity.UnlockMethod;
import com.spacedvocab.exception.BusinessRuleViolationException;
import com.spacedvocab.exception.DuplicateResourceException;
import com.spacedvocab.exception.ErrorCode;
import com.spacedvocab.exception.ResourceNotFoundException;
import com.spacedvocab.repository.DeckSubscriptionRepository;
import com.spacedvocab.repository.LearningPathRepository;
import com.spacedvocab.repository.LessonProgress;
import com.spacedvocab.repository.PathLessonRepository;
import com.spacedvocab.repository.PathStageRepository;
import com.spacedvocab.repository.StageTestAttemptRepository;
import com.spacedvocab.repository.StageUnlockRepository;
import com.spacedvocab.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The CEFR learning path for learners (business-spec §6): UC-L12 view, UC-L13 start a lesson,
 * UC-L14 stage test. Rules R10–R12.
 * <p>
 * Stages opened by completing every lesson (R11) are recorded whenever the path is read, so
 * the review flow (M3) does not need to know about paths.
 */
@Service
public class PathService {

    private static final Logger log = LoggerFactory.getLogger(PathService.class);

    private final LearningPathRepository pathRepository;
    private final PathStageRepository stageRepository;
    private final PathLessonRepository lessonRepository;
    private final StageUnlockRepository unlockRepository;
    private final StageTestAttemptRepository attemptRepository;
    private final DeckSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public PathService(LearningPathRepository pathRepository, PathStageRepository stageRepository,
                       PathLessonRepository lessonRepository, StageUnlockRepository unlockRepository,
                       StageTestAttemptRepository attemptRepository,
                       DeckSubscriptionRepository subscriptionRepository, UserRepository userRepository,
                       Clock clock) {
        this.pathRepository = pathRepository;
        this.stageRepository = stageRepository;
        this.lessonRepository = lessonRepository;
        this.unlockRepository = unlockRepository;
        this.attemptRepository = attemptRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PathSummaryResponse> list() {
        return pathRepository.findAllByOrderByIdAsc().stream()
                .map(path -> new PathSummaryResponse(path.getId(), path.getName()))
                .toList();
    }

    /** D23. Not read-only: stages whose previous stage was just completed are unlocked here (R11). */
    @Transactional
    public PathResponse get(Long pathId, AuthenticatedUser me) {
        LearningPath path = pathRepository.findById(pathId).orElseThrow(ResourceNotFoundException::new);
        PathProgress progress = loadProgress(pathId, me.id());
        List<StageSummaryResponse> stages = progress.stages.stream().map(progress::summary).toList();
        return new PathResponse(path.getId(), path.getName(), path.getDescription(), stages);
    }

    /** D24. */
    @Transactional
    public StageResponse getStage(Long stageId, AuthenticatedUser me) {
        PathStage stage = stageRepository.findById(stageId).orElseThrow(ResourceNotFoundException::new);
        PathProgress progress = loadProgress(stage.getPath().getId(), me.id());
        List<LessonResponse> lessons = progress.lessonsOf(stage).stream().map(LessonResponse::from).toList();
        return new StageResponse(progress.summary(stage), lessons);
    }

    /** UC-L13: subscribes the lesson's deck so its cards enter the review queue (R4, R6). */
    @Transactional
    public LessonStartResponse startLesson(Long lessonId, AuthenticatedUser me) {
        PathLesson lesson = lessonRepository.findActive(lessonId).orElseThrow(ResourceNotFoundException::new);
        PathStage stage = lesson.getStage();
        PathProgress progress = loadProgress(stage.getPath().getId(), me.id());
        if (!progress.isUnlocked(stage)) {
            throw new BusinessRuleViolationException(ErrorCode.STAGE_LOCKED); // R11
        }
        Deck deck = lesson.getDeck();
        if (!deck.isOwnedBy(me.id()) && !subscriptionRepository.existsByUserIdAndDeckId(me.id(), deck.getId())) {
            try {
                subscriptionRepository.saveAndFlush(
                        new DeckSubscription(userRepository.getReferenceById(me.id()), deck));
            } catch (DataIntegrityViolationException ex) {
                log.debug("User {} already subscribed to deck {} (concurrent start)", me.id(), deck.getId());
            }
        }
        return new LessonStartResponse(deck.getId());
    }

    /** UC-L14 / R12. Returns the attempt in progress if there is one, so reloading D25 keeps the same test. */
    @Transactional
    public StartedTest startTest(Long stageId, AuthenticatedUser me) {
        PathStage stage = stageRepository.findById(stageId).orElseThrow(ResourceNotFoundException::new);
        PathProgress progress = loadProgress(stage.getPath().getId(), me.id());
        if (!progress.isUnlocked(stage)) {
            throw new BusinessRuleViolationException(ErrorCode.STAGE_LOCKED);
        }
        if (!progress.nextStageIsLocked(stage)) {
            throw new BusinessRuleViolationException(ErrorCode.STAGE_TEST_NOT_AVAILABLE);
        }

        Instant now = clock.instant();
        Optional<StageTestAttempt> latest = attemptRepository
                .findFirstByUserIdAndStageIdOrderByStartedAtDesc(me.id(), stageId);
        if (latest.isPresent()) {
            StageTestAttempt attempt = latest.get();
            if (attempt.isInProgress(now)) {
                return new StartedTest(StageTestResponse.from(attempt), false);
            }
            if (attempt.isFailed(now) && now.isBefore(attempt.retryAt())) {
                throw new BusinessRuleViolationException(ErrorCode.STAGE_TEST_COOLDOWN);
            }
        }

        List<Card> cards = pickQuestions(stageId);
        StageTestAttempt attempt = attemptRepository.save(
                new StageTestAttempt(userRepository.getReferenceById(me.id()), stage, cards, now));
        log.info("User {} started the stage test of stage {} (attempt {})", me.id(), stageId, attempt.getId());
        return new StartedTest(StageTestResponse.from(attempt), true);
    }

    /** R12: grades the attempt; passing opens the next stage. */
    @Transactional
    public StageTestResultResponse submitTest(Long attemptId, StageTestSubmitRequest request, AuthenticatedUser me) {
        StageTestAttempt attempt = attemptRepository.findForUpdate(attemptId, me.id())
                .orElseThrow(ResourceNotFoundException::new);
        if (attempt.isSubmitted()) {
            throw new DuplicateResourceException(ErrorCode.STAGE_TEST_ALREADY_SUBMITTED, null);
        }
        Map<Integer, String> answers = new HashMap<>();
        request.answers().forEach(a -> answers.put(a.position(), a.answer()));
        attempt.submit(answers, clock.instant());

        boolean nextStageUnlocked = false;
        if (attempt.isPassed()) {
            Optional<PathStage> next = nextStage(attempt.getStage());
            next.ifPresent(stage -> unlockRepository.unlock(me.id(), stage.getId(), UnlockMethod.TEST.name()));
            nextStageUnlocked = next.isPresent();
        }
        log.info("User {} submitted stage test attempt {}: {}/{} passed={}", me.id(), attemptId,
                attempt.getCorrectCount(), StageTestAttempt.QUESTION_COUNT, attempt.isPassed());
        return StageTestResultResponse.from(attempt, nextStageUnlocked);
    }

    /** {@code created} is false when an attempt in progress was returned instead of a new one. */
    public record StartedTest(StageTestResponse test, boolean created) {
    }

    /** 20 cards with distinct words, picked at random from every lesson of the stage (R12). */
    private List<Card> pickQuestions(Long stageId) {
        Map<String, Card> byWord = new LinkedHashMap<>();
        for (Card card : lessonRepository.findCardsOfStage(stageId)) {
            byWord.putIfAbsent(card.getContent().normalizedWord(), card);
        }
        if (byWord.size() < StageTestAttempt.QUESTION_COUNT) {
            throw new BusinessRuleViolationException(ErrorCode.STAGE_TEST_TOO_FEW_CARDS);
        }
        List<Card> pool = new ArrayList<>(byWord.values());
        Collections.shuffle(pool);
        return List.copyOf(pool.subList(0, StageTestAttempt.QUESTION_COUNT));
    }

    private Optional<PathStage> nextStage(PathStage stage) {
        return stageRepository.findByPathIdOrderByPositionAsc(stage.getPath().getId()).stream()
                .filter(s -> s.getPosition() > stage.getPosition())
                .findFirst();
    }

    /** Loads the learner's progress on the path and records stages opened by completed lessons (R11). */
    private PathProgress loadProgress(Long pathId, Long userId) {
        List<PathStage> stages = stageRepository.findByPathIdOrderByPositionAsc(pathId);
        Map<Long, List<LessonProgress>> lessons = new HashMap<>();
        for (LessonProgress lesson : lessonRepository.findProgressByPath(pathId, userId)) {
            lessons.computeIfAbsent(lesson.stageId(), id -> new ArrayList<>()).add(lesson);
        }
        Map<Long, UnlockMethod> unlocks = new HashMap<>();
        for (StageUnlock unlock : unlockRepository.findByUserAndPath(userId, pathId)) {
            unlocks.put(unlock.getStage().getId(), unlock.getMethod());
        }

        PathProgress progress = new PathProgress(stages, lessons, unlocks);
        for (int i = 0; i + 1 < stages.size(); i++) {
            PathStage stage = stages.get(i);
            PathStage next = stages.get(i + 1);
            if (progress.isUnlocked(stage) && progress.isCompleted(stage) && !progress.isUnlocked(next)) {
                unlockRepository.unlock(userId, next.getId(), UnlockMethod.LESSONS.name());
                unlocks.put(next.getId(), UnlockMethod.LESSONS);
                log.info("User {} completed stage {} and unlocked stage {}", userId, stage.getId(), next.getId());
            }
        }
        progress.findRetryAt(userId);
        return progress;
    }

    /** The learner's view of one path. Stages are ordered by position. */
    private final class PathProgress {

        private final List<PathStage> stages;
        private final Map<Long, List<LessonProgress>> lessonsByStage;
        private final Map<Long, UnlockMethod> unlocks;
        private Long retryStageId;
        private Instant retryAt;

        PathProgress(List<PathStage> stages, Map<Long, List<LessonProgress>> lessonsByStage,
                     Map<Long, UnlockMethod> unlocks) {
            this.stages = stages;
            this.lessonsByStage = lessonsByStage;
            this.unlocks = unlocks;
        }

        List<LessonProgress> lessonsOf(PathStage stage) {
            return lessonsByStage.getOrDefault(stage.getId(), List.of());
        }

        /** R11: the first stage is always open. */
        boolean isUnlocked(PathStage stage) {
            return (!stages.isEmpty() && stages.get(0).getId().equals(stage.getId()))
                    || unlocks.containsKey(stage.getId());
        }

        /** R10: 100% of the lessons are completed; a stage without lessons is never completed. */
        boolean isCompleted(PathStage stage) {
            List<LessonProgress> lessons = lessonsOf(stage);
            return !lessons.isEmpty() && lessons.stream().allMatch(LessonProgress::completed);
        }

        /** R12: the test belongs to the highest open stage, i.e. an open stage followed by a locked one. */
        boolean nextStageIsLocked(PathStage stage) {
            int index = indexOf(stage);
            return index + 1 < stages.size() && !isUnlocked(stages.get(index + 1));
        }

        /** Cooldown of the only stage that can have a stage test right now. */
        void findRetryAt(Long userId) {
            Instant now = clock.instant();
            stages.stream()
                    .filter(stage -> isUnlocked(stage) && nextStageIsLocked(stage))
                    .findFirst()
                    .flatMap(stage -> attemptRepository.findFirstByUserIdAndStageIdOrderByStartedAtDesc(userId, stage.getId()))
                    .filter(attempt -> attempt.isFailed(now) && now.isBefore(attempt.retryAt()))
                    .ifPresent(attempt -> {
                        retryStageId = attempt.getStage().getId();
                        retryAt = attempt.retryAt();
                    });
        }

        StageSummaryResponse summary(PathStage stage) {
            List<LessonProgress> lessons = lessonsOf(stage);
            int completed = (int) lessons.stream().filter(LessonProgress::completed).count();
            long cardCount = lessons.stream().mapToLong(LessonProgress::cardCount).sum();
            boolean unlocked = isUnlocked(stage);
            Instant stageRetryAt = stage.getId().equals(retryStageId) ? retryAt : null;
            boolean testAvailable = unlocked && nextStageIsLocked(stage) && stageRetryAt == null
                    && cardCount >= StageTestAttempt.QUESTION_COUNT;
            return new StageSummaryResponse(stage.getId(), stage.getCefrLevel(), stage.getTitle(),
                    stage.getPosition(), lessons.size(), completed, unlocked, unlocks.get(stage.getId()),
                    testAvailable, stageRetryAt);
        }

        private int indexOf(PathStage stage) {
            for (int i = 0; i < stages.size(); i++) {
                if (stages.get(i).getId().equals(stage.getId())) {
                    return i;
                }
            }
            throw new IllegalArgumentException("stage " + stage.getId() + " is not on this path");
        }
    }
}
