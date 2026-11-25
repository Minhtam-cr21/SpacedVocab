package com.spacedvocab.repository;

import com.spacedvocab.entity.Deck;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeckRepository extends JpaRepository<Deck, Long> {

    @EntityGraph(attributePaths = "owner")
    Optional<Deck> findByIdAndDeletedAtIsNull(Long id);

    /** R2: id of the owner's (not deleted) deck with this name, compared case-insensitively. */
    @Query("""
            select d.id from Deck d
            where d.owner.id = :ownerId and lower(d.name) = lower(:name) and d.deletedAt is null
            """)
    Optional<Long> findActiveIdByOwnerAndName(@Param("ownerId") Long ownerId, @Param("name") String name);

    @Query("""
            select new com.spacedvocab.repository.DeckWithCardCount(d, count(c))
            from Deck d left join Card c on c.deck = d and c.deletedAt is null
            where d.owner.id = :ownerId and d.deletedAt is null
            group by d
            order by d.updatedAt desc, d.id desc
            """)
    List<DeckWithCardCount> findOwnedWithCardCount(@Param("ownerId") Long ownerId);

    /** D20 / D26: every OFFICIAL deck with its card count and, if it is a lesson, the lesson and stage. */
    @Query("""
            select new com.spacedvocab.repository.OfficialDeckRow(d.id, d.name, count(c), l.id, l.stage.id)
            from Deck d
              left join Card c on c.deck = d and c.deletedAt is null
              left join PathLesson l on l.deck = d
            where d.visibility = com.spacedvocab.entity.DeckVisibility.OFFICIAL and d.deletedAt is null
            group by d.id, d.name, l.id, l.stage.id
            order by lower(d.name), d.id
            """)
    List<OfficialDeckRow> findOfficialWithLesson();
}
