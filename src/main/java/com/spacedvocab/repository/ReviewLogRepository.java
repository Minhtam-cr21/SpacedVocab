package com.spacedvocab.repository;

import com.spacedvocab.entity.CardState;
import com.spacedvocab.entity.ReviewLog;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

/** Append-only (R8): no update or delete methods are exposed. */
public interface ReviewLogRepository extends Repository<ReviewLog, Long> {

    ReviewLog save(ReviewLog log);

    /** R7: reviews in [from, to) of cards that were NEW at the time, i.e. new cards started that day. */
    @Query("""
            select count(l) from ReviewLog l
            where l.userId = :userId and l.stateBefore = :state and l.reviewedAt >= :from and l.reviewedAt < :to
            """)
    long countByStateBefore(@Param("userId") Long userId, @Param("state") CardState state,
                            @Param("from") Instant from, @Param("to") Instant to);

    default long countNewCardsStarted(Long userId, Instant from, Instant to) {
        return countByStateBefore(userId, CardState.NEW, from, to);
    }
}
