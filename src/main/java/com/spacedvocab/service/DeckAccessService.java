package com.spacedvocab.service;

import com.spacedvocab.config.security.AuthenticatedUser;
import com.spacedvocab.entity.Deck;
import com.spacedvocab.entity.DeckVisibility;
import com.spacedvocab.exception.ForbiddenOperationException;
import com.spacedvocab.exception.ResourceNotFoundException;
import com.spacedvocab.repository.DeckRepository;
import com.spacedvocab.repository.DeckSubscriptionRepository;
import org.springframework.stereotype.Service;

/**
 * Loads a deck and applies the permission matrix (R3). Runs inside the caller's transaction.
 * <ul>
 *   <li>deleted, missing or someone else's PRIVATE deck → 404 (its existence is not revealed)</li>
 *   <li>visible but not editable (e.g. another user's PUBLIC deck) → 403</li>
 * </ul>
 */
@Service
public class DeckAccessService {

    private final DeckRepository deckRepository;
    private final DeckSubscriptionRepository subscriptionRepository;

    public DeckAccessService(DeckRepository deckRepository, DeckSubscriptionRepository subscriptionRepository) {
        this.deckRepository = deckRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    public Deck requireVisible(Long deckId, AuthenticatedUser me) {
        return deckRepository.findByIdAndDeletedAtIsNull(deckId)
                .filter(deck -> deck.isVisibleTo(me.id()))
                .orElseThrow(ResourceNotFoundException::new);
    }

    public Deck requireEditable(Long deckId, AuthenticatedUser me) {
        Deck deck = requireVisible(deckId, me);
        if (!isEditable(deck, me)) {
            throw new ForbiddenOperationException();
        }
        return deck;
    }

    public boolean isEditable(Deck deck, AuthenticatedUser me) {
        return deck.isEditableBy(me.id(), me.role());
    }

    /**
     * R6: the learner may study this deck: they own it, or subscribed to it while it is PUBLIC / OFFICIAL.
     * Keep in sync with the library queries in {@code CardProgressRepository} / {@code CardRepository}.
     */
    public boolean isInLibrary(Deck deck, AuthenticatedUser me) {
        if (deck.isDeleted()) {
            return false;
        }
        return deck.isOwnedBy(me.id())
                || (deck.getVisibility() != DeckVisibility.PRIVATE
                    && subscriptionRepository.existsByUserIdAndDeckId(me.id(), deck.getId()));
    }
}
