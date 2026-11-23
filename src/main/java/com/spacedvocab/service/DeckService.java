package com.spacedvocab.service;

import com.spacedvocab.config.security.AuthenticatedUser;
import com.spacedvocab.dto.DeckRequest;
import com.spacedvocab.dto.DeckResponse;
import com.spacedvocab.entity.Deck;
import com.spacedvocab.entity.DeckVisibility;
import com.spacedvocab.entity.Role;
import com.spacedvocab.exception.DuplicateResourceException;
import com.spacedvocab.exception.ErrorCode;
import com.spacedvocab.exception.ForbiddenOperationException;
import com.spacedvocab.repository.CardRepository;
import com.spacedvocab.repository.DeckRepository;
import com.spacedvocab.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/** UC-L4: a learner's own decks (D8, D9, D10). OFFICIAL decks are managed by admins in M4. */
@Service
public class DeckService {

    private static final Logger log = LoggerFactory.getLogger(DeckService.class);

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final DeckAccessService deckAccess;
    private final Clock clock;

    public DeckService(DeckRepository deckRepository, CardRepository cardRepository, UserRepository userRepository,
                       DeckAccessService deckAccess, Clock clock) {
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
        this.deckAccess = deckAccess;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<DeckResponse> listOwned(AuthenticatedUser me) {
        return deckRepository.findOwnedWithCardCount(me.id()).stream()
                .map(row -> DeckResponse.from(row.deck(), row.cardCount(), deckAccess.isEditable(row.deck(), me)))
                .toList();
    }

    @Transactional(readOnly = true)
    public DeckResponse get(Long deckId, AuthenticatedUser me) {
        Deck deck = deckAccess.requireVisible(deckId, me);
        return toResponse(deck, me);
    }

    @Transactional
    public DeckResponse create(AuthenticatedUser me, DeckRequest request) {
        if (me.role() != Role.LEARNER) {
            // admins create OFFICIAL decks through /api/admin/decks (M4)
            throw new ForbiddenOperationException();
        }
        requireLearnerVisibility(request.visibility());
        ensureNameAvailable(me.id(), request.name(), null);
        Deck deck = new Deck(userRepository.getReferenceById(me.id()),
                request.name(), request.description(), request.visibility());
        try {
            deckRepository.saveAndFlush(deck);
        } catch (DataIntegrityViolationException ex) {
            throw nameTaken(request.name());
        }
        log.info("User {} created deck {}", me.id(), deck.getId());
        return DeckResponse.from(deck, 0, true);
    }

    @Transactional
    public DeckResponse update(Long deckId, AuthenticatedUser me, DeckRequest request) {
        Deck deck = deckAccess.requireEditable(deckId, me);
        requireLearnerVisibility(request.visibility());
        ensureNameAvailable(deck.getOwner().getId(), request.name(), deck.getId());
        deck.edit(request.name(), request.description(), request.visibility());
        try {
            deckRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw nameTaken(request.name());
        }
        return toResponse(deck, me);
    }

    /** Always a soft delete (R8): cards and review history stay in the database. */
    @Transactional
    public void delete(Long deckId, AuthenticatedUser me) {
        Deck deck = deckAccess.requireEditable(deckId, me);
        deck.delete(clock.instant());
        log.info("User {} deleted deck {}", me.id(), deckId);
    }

    private DeckResponse toResponse(Deck deck, AuthenticatedUser me) {
        long cardCount = cardRepository.countByDeckIdAndDeletedAtIsNull(deck.getId());
        return DeckResponse.from(deck, cardCount, deckAccess.isEditable(deck, me));
    }

    /** R3: OFFICIAL is reserved for admins. */
    private static void requireLearnerVisibility(DeckVisibility visibility) {
        if (visibility == DeckVisibility.OFFICIAL) {
            throw new ForbiddenOperationException();
        }
    }

    /** R2: unique name per owner, case-insensitive. {@code currentDeckId} is the deck being edited, if any. */
    private void ensureNameAvailable(Long ownerId, String name, Long currentDeckId) {
        deckRepository.findActiveIdByOwnerAndName(ownerId, name)
                .filter(existingId -> !existingId.equals(currentDeckId))
                .ifPresent(existingId -> {
                    throw nameTaken(name);
                });
    }

    private static DuplicateResourceException nameTaken(String name) {
        return new DuplicateResourceException(ErrorCode.DECK_NAME_TAKEN,
                "Bạn đã có deck tên \"%s\". Hãy đặt tên khác.".formatted(name), "name");
    }
}
