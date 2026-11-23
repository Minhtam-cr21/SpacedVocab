package com.spacedvocab.service;

import com.spacedvocab.config.security.AuthenticatedUser;
import com.spacedvocab.dto.CardRequest;
import com.spacedvocab.dto.CardResponse;
import com.spacedvocab.dto.PageResponse;
import com.spacedvocab.entity.Card;
import com.spacedvocab.entity.CardContent;
import com.spacedvocab.entity.Deck;
import com.spacedvocab.exception.DuplicateResourceException;
import com.spacedvocab.exception.ErrorCode;
import com.spacedvocab.exception.ResourceNotFoundException;
import com.spacedvocab.repository.CardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;

/** UC-L5: cards of a deck (D10 list, D11 form). Reading needs a visible deck, writing an editable one (R3). */
@Service
public class CardService {

    private static final Logger log = LoggerFactory.getLogger(CardService.class);

    private final CardRepository cardRepository;
    private final DeckAccessService deckAccess;
    private final Clock clock;

    public CardService(CardRepository cardRepository, DeckAccessService deckAccess, Clock clock) {
        this.cardRepository = cardRepository;
        this.deckAccess = deckAccess;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<CardResponse> search(Long deckId, AuthenticatedUser me, String query, int page, int size) {
        Deck deck = deckAccess.requireVisible(deckId, me);
        return PageResponse.from(cardRepository
                .search(deck.getId(), likePattern(query), PageRequest.of(page, size))
                .map(CardResponse::from));
    }

    @Transactional(readOnly = true)
    public CardResponse get(Long deckId, Long cardId, AuthenticatedUser me) {
        Deck deck = deckAccess.requireVisible(deckId, me);
        return CardResponse.from(findCard(deck, cardId));
    }

    @Transactional
    public CardResponse create(Long deckId, AuthenticatedUser me, CardRequest request) {
        Deck deck = deckAccess.requireEditable(deckId, me);
        CardContent content = request.toContent();
        ensureWordAvailable(deck, content, null);
        Card card = new Card(deck, content);
        try {
            cardRepository.saveAndFlush(card);
        } catch (DataIntegrityViolationException ex) {
            throw wordTaken(content);
        }
        log.debug("User {} added card {} to deck {}", me.id(), card.getId(), deckId);
        return CardResponse.from(card);
    }

    @Transactional
    public CardResponse update(Long deckId, Long cardId, AuthenticatedUser me, CardRequest request) {
        Deck deck = deckAccess.requireEditable(deckId, me);
        Card card = findCard(deck, cardId);
        CardContent content = request.toContent();
        ensureWordAvailable(deck, content, card.getId());
        card.edit(content);
        try {
            cardRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw wordTaken(content);
        }
        return CardResponse.from(card);
    }

    /** Soft delete, so review history (M3) keeps pointing at the card. */
    @Transactional
    public void delete(Long deckId, Long cardId, AuthenticatedUser me) {
        Deck deck = deckAccess.requireEditable(deckId, me);
        findCard(deck, cardId).delete(clock.instant());
    }

    private Card findCard(Deck deck, Long cardId) {
        return cardRepository.findByIdAndDeckIdAndDeletedAtIsNull(cardId, deck.getId())
                .orElseThrow(ResourceNotFoundException::new);
    }

    /** R2: a word appears once per deck, case-insensitive. {@code currentCardId} is the card being edited, if any. */
    private void ensureWordAvailable(Deck deck, CardContent content, Long currentCardId) {
        cardRepository.findActiveIdByDeckAndWord(deck.getId(), content.word())
                .filter(existingId -> !existingId.equals(currentCardId))
                .ifPresent(existingId -> {
                    throw wordTaken(content);
                });
    }

    private static DuplicateResourceException wordTaken(CardContent content) {
        return new DuplicateResourceException(ErrorCode.CARD_WORD_TAKEN,
                "\"%s\" đã có trong deck này.".formatted(content.word()), "word");
    }

    /** "Run%" → "%run\%%": user input is matched literally, case-insensitively. Blank → match everything. */
    static String likePattern(String query) {
        if (query == null || query.isBlank()) {
            return "%";
        }
        String escaped = query.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
