package com.spacedvocab.service;

import com.spacedvocab.config.security.AuthenticatedUser;
import com.spacedvocab.entity.Deck;
import com.spacedvocab.exception.ForbiddenOperationException;
import com.spacedvocab.exception.ResourceNotFoundException;
import com.spacedvocab.repository.DeckRepository;
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

    public DeckAccessService(DeckRepository deckRepository) {
        this.deckRepository = deckRepository;
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
}
