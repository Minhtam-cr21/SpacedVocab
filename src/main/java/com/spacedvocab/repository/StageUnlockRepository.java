package com.spacedvocab.repository;

import com.spacedvocab.entity.StageUnlock;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StageUnlockRepository extends JpaRepository<StageUnlock, Long> {

    @Query("select u from StageUnlock u where u.user.id = :userId and u.stage.path.id = :pathId")
    List<StageUnlock> findByUserAndPath(@Param("userId") Long userId, @Param("pathId") Long pathId);

    /** R11: records the unlock once; a concurrent or repeated unlock of the same stage is a no-op. */
    @Modifying
    @Query(value = """
            insert into stage_unlocks (user_id, stage_id, method, unlocked_at, created_at, updated_at)
            values (:userId, :stageId, :method, now(), now(), now())
            on conflict (user_id, stage_id) do nothing
            """, nativeQuery = true)
    int unlock(@Param("userId") Long userId, @Param("stageId") Long stageId, @Param("method") String method);
}
