package com.spacedvocab.repository;

import com.spacedvocab.entity.Card;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    Optional<Card> findByIdAndDeckIdAndDeletedAtIsNull(Long id, Long deckId);

    long countByDeckIdAndDeletedAtIsNull(Long deckId);

    /** R2: id of the (not deleted) card with this word in the deck, compared case-insensitively. */
    @Query("""
            select c.id from Card c
            where c.deck.id = :deckId and lower(c.content.word) = lower(:word) and c.deletedAt is null
            """)
    Optional<Long> findActiveIdByDeckAndWord(@Param("deckId") Long deckId, @Param("word") String word);

    /**
     * D10 search: {@code pattern} is a lower-case LIKE pattern (escaped with {@code \}) matched against word and meaning.
     * Sorted alphabetically by word.
     */
    @Query(value = """
            select c from Card c
            where c.deck.id = :deckId and c.deletedAt is null
              and (lower(c.content.word) like :pattern escape '\\' or lower(c.content.meaning) like :pattern escape '\\')
            order by lower(c.content.word), c.id
            """,
            countQuery = """
            select count(c) from Card c
            where c.deck.id = :deckId and c.deletedAt is null
              and (lower(c.content.word) like :pattern escape '\\' or lower(c.content.meaning) like :pattern escape '\\')
            """)
    Page<Card> search(@Param("deckId") Long deckId, @Param("pattern") String pattern, Pageable pageable);

    /** D13 review: the card with its deck and owner, for the R6 check. */
    @EntityGraph(attributePaths = {"deck", "deck.owner"})
    Optional<Card> findByIdAndDeletedAtIsNull(Long id);

    /**
     * Cards in the learner's library (own decks; subscribed decks in M4) that they have never reviewed,
     * in the order they were added. {@code deckId} is optional.
     */
    @Query("""
            select c from Card c join fetch c.deck d
            where d.owner.id = :userId and d.deletedAt is null and c.deletedAt is null
              and (:deckId is null or d.id = :deckId)
              and not exists (select p.id from CardProgress p where p.card = c and p.user.id = :userId)
            order by c.id
            """)
    List<Card> findNewForLearner(@Param("userId") Long userId, @Param("deckId") Long deckId, Limit limit);

    @Query("""
            select count(c) from Card c join c.deck d
            where d.owner.id = :userId and d.deletedAt is null and c.deletedAt is null
              and (:deckId is null or d.id = :deckId)
              and not exists (select p.id from CardProgress p where p.card = c and p.user.id = :userId)
            """)
    long countNewForLearner(@Param("userId") Long userId, @Param("deckId") Long deckId);
}
