package com.spacedvocab.service;

import com.spacedvocab.algorithm.Quality;
import com.spacedvocab.algorithm.SchedulingState;
import com.spacedvocab.algorithm.SpacedRepetitionAlgorithm;
import com.spacedvocab.config.security.AuthenticatedUser;
import com.spacedvocab.dto.DueCardResponse;
import com.spacedvocab.dto.DueCardsResponse;
import com.spacedvocab.dto.ReviewRequest;
import com.spacedvocab.dto.ReviewResponse;
import com.spacedvocab.entity.Card;
import com.spacedvocab.entity.CardProgress;
import com.spacedvocab.entity.CardState;
import com.spacedvocab.entity.User;
import com.spacedvocab.exception.BusinessRuleViolationException;
import com.spacedvocab.exception.DuplicateResourceException;
import com.spacedvocab.exception.ErrorCode;
import com.spacedvocab.exception.ResourceNotFoundException;
import com.spacedvocab.repository.CardProgressRepository;
import com.spacedvocab.repository.CardRepository;
import com.spacedvocab.repository.ReviewLogRepository;
import com.spacedvocab.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** UC-L8 due cards and UC-L9 grading (D7, D13). Rules R5–R8. */
@Service
public class ReviewService {

    /** Qualities of the four rating buttons: Quên, Khó, Nhớ, Dễ (business-spec §5). */
    static final List<Integer> BUTTON_QUALITIES = List.of(1, 3, 4, 5);

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final CardProgressRepository progressRepository;
    private final CardRepository cardRepository;
    private final ReviewLogRepository reviewLogRepository;
    private final UserRepository userRepository;
    private final DeckAccessService deckAccess;
    private final SpacedRepetitionAlgorithm algorithm;
    private final Clock clock;

    public ReviewService(CardProgressRepository progressRepository, CardRepository cardRepository,
                         ReviewLogRepository reviewLogRepository, UserRepository userRepository,
                         DeckAccessService deckAccess, SpacedRepetitionAlgorithm algorithm, Clock clock) {
        this.progressRepository = progressRepository;
        this.cardRepository = cardRepository;
        this.reviewLogRepository = reviewLogRepository;
        this.userRepository = userRepository;
        this.deckAccess = deckAccess;
        this.algorithm = algorithm;
        this.clock = clock;
    }

    /**
     * Due cards first (most overdue first), then new cards up to today's allowance (R7).
     * {@code limit = 0} only returns the counts (D7).
     */
    @Transactional(readOnly = true)
    public DueCardsResponse due(AuthenticatedUser me, Long deckId, int limit) {
        if (deckId != null) {
            deckAccess.requireVisible(deckId, me);
        }
        User user = loadUser(me);
        StudyDay today = StudyDay.today(clock, user.getTimezone());

        long reviewCount = progressRepository.countDue(me.id(), deckId, today.end());
        long newAvailable = cardRepository.countNewForLearner(me.id(), deckId);
        long newCount = Math.min(newAvailable, remainingNewCards(user, today));

        List<DueCardResponse> cards = new ArrayList<>();
        if (limit > 0) {
            progressRepository.findDue(me.id(), deckId, today.end(), Limit.of(limit))
                    .forEach(p -> cards.add(DueCardResponse.of(p.getCard(), p.getState(), previewIntervals(p.schedulingState()))));
            int newSlots = (int) Math.min(limit - cards.size(), newCount);
            if (newSlots > 0) {
                Map<Integer, Integer> newCardPreview = previewIntervals(SchedulingState.INITIAL);
                cardRepository.findNewForLearner(me.id(), deckId, Limit.of(newSlots))
                        .forEach(card -> cards.add(DueCardResponse.of(card, CardState.NEW, newCardPreview)));
            }
        }

        Instant nextDueAt = reviewCount + newCount == 0 ? nextDueAt(me, deckId, today, newAvailable) : null;
        return new DueCardsResponse(reviewCount, newCount, nextDueAt, cards);
    }

    /** UC-L9: grade one card, update its schedule and append a {@code ReviewLog}. */
    @Transactional
    public ReviewResponse review(AuthenticatedUser me, ReviewRequest request) {
        Quality quality = Quality.of(request.quality());
        Card card = cardRepository.findByIdAndDeletedAtIsNull(request.cardId())
                .filter(c -> c.getDeck().isVisibleTo(me.id()))
                .orElseThrow(ResourceNotFoundException::new);
        if (!deckAccess.isInLibrary(card.getDeck(), me)) {
            throw new BusinessRuleViolationException(ErrorCode.CARD_NOT_IN_LIBRARY, "cardId"); // R6
        }

        User user = loadUser(me);
        CardProgress progress = progressRepository.findForUpdate(me.id(), card.getId())
                .orElseGet(() -> startLearning(user, card));
        var reviewLog = progress.applyReview(quality, algorithm, clock);
        try {
            progressRepository.saveAndFlush(progress);
        } catch (DataIntegrityViolationException ex) {
            // the same new card was graded twice at the same moment (unique user_id, card_id)
            throw new DuplicateResourceException(ErrorCode.REVIEW_CONFLICT, "cardId");
        }
        reviewLogRepository.save(reviewLog);
        log.debug("User {} reviewed card {} with quality {} → interval {}",
                me.id(), card.getId(), quality.value(), progress.getIntervalDays());
        return ReviewResponse.of(progress, quality.value());
    }

    /** R7: a card seen for the first time counts towards today's new-card limit. */
    private CardProgress startLearning(User user, Card card) {
        if (remainingNewCards(user, StudyDay.today(clock, user.getTimezone())) <= 0) {
            throw new BusinessRuleViolationException(ErrorCode.DAILY_NEW_LIMIT_REACHED, "cardId");
        }
        return new CardProgress(user, card);
    }

    private long remainingNewCards(User user, StudyDay today) {
        long started = reviewLogRepository.countNewCardsStarted(user.getId(), today.start(), today.end());
        return Math.max(0, user.getDailyNewLimit() - started);
    }

    /** Interval (days) each rating button would give, so the UI can show it before grading (D13). */
    private Map<Integer, Integer> previewIntervals(SchedulingState state) {
        Map<Integer, Integer> preview = new LinkedHashMap<>();
        for (int quality : BUTTON_QUALITIES) {
            preview.put(quality, algorithm.next(state, Quality.of(quality)).intervalDays());
        }
        return preview;
    }

    /** Earliest scheduled review; new cards left for later count as due when tomorrow starts. */
    private Instant nextDueAt(AuthenticatedUser me, Long deckId, StudyDay today, long newAvailable) {
        Instant tomorrow = newAvailable > 0 ? today.end() : null;
        return Stream.concat(
                        progressRepository.findNextReviewAt(me.id(), deckId, today.end()).stream(),
                        Stream.ofNullable(tomorrow))
                .min(Instant::compareTo)
                .orElse(null);
    }

    private User loadUser(AuthenticatedUser me) {
        return userRepository.findById(me.id()).orElseThrow(ResourceNotFoundException::new);
    }
}
