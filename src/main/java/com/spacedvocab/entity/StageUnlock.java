package com.spacedvocab.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** R11: a stage a learner has opened. Never deleted, so a stage never locks again. */
@Entity
@Table(name = "stage_unlocks")
public class StageUnlock extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false, updatable = false)
    private PathStage stage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private UnlockMethod method;

    @Column(name = "unlocked_at", nullable = false, updatable = false)
    private Instant unlockedAt;

    protected StageUnlock() {
        // for JPA
    }

    public User getUser() {
        return user;
    }

    public PathStage getStage() {
        return stage;
    }

    public UnlockMethod getMethod() {
        return method;
    }

    public Instant getUnlockedAt() {
        return unlockedAt;
    }
}
