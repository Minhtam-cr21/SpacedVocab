package com.spacedvocab.repository;

import com.spacedvocab.entity.Card;
import com.spacedvocab.entity.PathLesson;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Lessons whose deck was deleted are left out everywhere (R8 soft delete). */
public interface PathLessonRepository extends JpaRepository<PathLesson, Long> {

    @EntityGraph(attributePaths = {"stage", "deck", "deck.owner"})
    @Query("select l from PathLesson l where l.id = :id and l.deck.deletedAt is null")
    Optional<PathLesson> findActive(@Param("id") Long id);

    boolean existsByDeckId(Long deckId);

    /** Every lesson of the path with this learner's progress, in stage then lesson order. */
    @Query("""
            select new com.spacedvocab.repository.LessonProgress(
                l.id, s.id, d.id, d.name, l.position, count(c), count(p))
            from PathLesson l join l.stage s join l.deck d
              left join Card c on c.deck = d and c.deletedAt is null
              left join CardProgress p on p.card = c and p.user.id = :userId
            where s.path.id = :pathId and d.deletedAt is null
            group by l.id, s.id, d.id, d.name, l.position
            order by l.position, l.id
            """)
    List<LessonProgress> findProgressByPath(@Param("pathId") Long pathId, @Param("userId") Long userId);

    /** R12: question pool of a stage test, i.e. the cards of every lesson of the stage. */
    @Query("""
            select c from Card c join c.deck d
            where d.deletedAt is null and c.deletedAt is null
              and d.id in (select l.deck.id from PathLesson l where l.stage.id = :stageId)
            order by c.id
            """)
    List<Card> findCardsOfStage(@Param("stageId") Long stageId);
}
