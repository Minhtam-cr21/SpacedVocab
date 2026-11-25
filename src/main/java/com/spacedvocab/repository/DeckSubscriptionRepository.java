package com.spacedvocab.repository;

import com.spacedvocab.entity.DeckSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckSubscriptionRepository extends JpaRepository<DeckSubscription, Long> {

    boolean existsByUserIdAndDeckId(Long userId, Long deckId);
}
