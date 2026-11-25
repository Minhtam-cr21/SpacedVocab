package com.spacedvocab.repository;

import com.spacedvocab.entity.StageTestAttempt;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StageTestAttemptRepository extends JpaRepository<StageTestAttempt, Long> {

    Optional<StageTestAttempt> findFirstByUserIdAndStageIdOrderByStartedAtDesc(Long userId, Long stageId);

    /** Locks the attempt so it is graded only once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from StageTestAttempt a where a.id = :id and a.user.id = :userId")
    Optional<StageTestAttempt> findForUpdate(@Param("id") Long id, @Param("userId") Long userId);
}
