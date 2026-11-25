package com.spacedvocab.repository;

import com.spacedvocab.entity.CefrLevel;
import com.spacedvocab.entity.PathStage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PathStageRepository extends JpaRepository<PathStage, Long> {

    List<PathStage> findByPathIdOrderByPositionAsc(Long pathId);

    Optional<PathStage> findByPathIdAndCefrLevel(Long pathId, CefrLevel cefrLevel);
}
