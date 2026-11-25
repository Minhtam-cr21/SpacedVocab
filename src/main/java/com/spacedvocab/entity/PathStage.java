package com.spacedvocab.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One CEFR stage of a path. The six stages A1–C2 are reference data; admins may only rename them. */
@Entity
@Table(name = "path_stages")
public class PathStage extends BaseEntity {

    public static final int MAX_TITLE_LENGTH = 100;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "path_id", nullable = false, updatable = false)
    private LearningPath path;

    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_level", nullable = false, length = 2, updatable = false)
    private CefrLevel cefrLevel;

    @Column(nullable = false, length = MAX_TITLE_LENGTH)
    private String title;

    @Column(nullable = false, updatable = false)
    private int position;

    protected PathStage() {
        // for JPA
    }

    public void rename(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        this.title = title.trim();
    }

    public LearningPath getPath() {
        return path;
    }

    public CefrLevel getCefrLevel() {
        return cefrLevel;
    }

    public String getTitle() {
        return title;
    }

    public int getPosition() {
        return position;
    }
}
