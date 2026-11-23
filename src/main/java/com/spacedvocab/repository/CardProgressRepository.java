package com.spacedvocab.repository;

import com.spacedvocab.entity.CardProgress;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * "In the learner's library" (R6) currently means a deck they own; M4 adds subscribed decks to these queries.
 * Cards and decks that are soft-deleted are never due. {@code deckId} is optional (null = whole library).
 */
public interface CardProgressRepository extends JpaRepository<CardProgress, Long> {

    /** Locks the row so two reviews of the same card are applied one after the other. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from CardProgress p where p.user.id = :userId and p.card.id = :cardId")
    Optional<CardProgress> findForUpdate(@Param("userId") Long userId, @Param("cardId") Long cardId);

    /** Cards due before {@code before} (the end of the learner's day), most overdue first. */
    @Query("""
            select p from CardProgress p join fetch p.card c join fetch c.deck d
            where p.user.id = :userId and p.nextReviewAt < :before
              and c.deletedAt is null and d.deletedAt is null and d.owner.id = :userId
              and (:deckId is null or d.id = :deckId)
            order by p.nextReviewAt, p.id
            """)
    List<CardProgress> findDue(@Param("userId") Long userId, @Param("deckId") Long deckId,
                               @Param("before") Instant before, Limit limit);

    @Query("""
            select count(p) from CardProgress p join p.card c join c.deck d
            where p.user.id = :userId and p.nextReviewAt < :before
              and c.deletedAt is null and d.deletedAt is null and d.owner.id = :userId
              and (:deckId is null or d.id = :deckId)
            """)
    long countDue(@Param("userId") Long userId, @Param("deckId") Long deckId, @Param("before") Instant before);

    /** D7 "next card is due at …": the earliest scheduled review from {@code from} on. */
    @Query("""
            select min(p.nextReviewAt) from CardProgress p join p.card c join c.deck d
            where p.user.id = :userId and p.nextReviewAt >= :from
              and c.deletedAt is null and d.deletedAt is null and d.owner.id = :userId
              and (:deckId is null or d.id = :deckId)
            """)
    Optional<Instant> findNextReviewAt(@Param("userId") Long userId, @Param("deckId") Long deckId,
                                       @Param("from") Instant from);

    /** D10: how many of the deck's cards this learner has in each state (cards without a row are NEW). */
    @Query("""
            select new com.spacedvocab.repository.StateCount(p.state, count(p))
            from CardProgress p join p.card c
            where p.user.id = :userId and c.deck.id = :deckId and c.deletedAt is null
            group by p.state
            """)
    List<StateCount> countByState(@Param("userId") Long userId, @Param("deckId") Long deckId);
}
