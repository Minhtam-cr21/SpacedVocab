package com.spacedvocab.repository;

import com.spacedvocab.entity.LearningPath;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {

    List<LearningPath> findAllByOrderByIdAsc();

    Optional<LearningPath> findByName(String name);
}
