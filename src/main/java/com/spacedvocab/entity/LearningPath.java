package com.spacedvocab.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A learning path such as "Lộ trình CEFR" (business-spec §6). Created by migration as reference data. */
@Entity
@Table(name = "learning_paths")
public class LearningPath extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    protected LearningPath() {
        // for JPA
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
