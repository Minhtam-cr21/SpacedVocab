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
import java.util.Objects;

/**
 * A deck of cards owned by one user. Deleting is always soft (R8), so cards and learning history survive.
 */
@Entity
@Table(name = "decks")
public class Deck extends BaseEntity implements Ownable {

    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_DESCRIPTION_LENGTH = 500;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    private User owner;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(length = MAX_DESCRIPTION_LENGTH)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeckVisibility visibility;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Deck() {
        // for JPA
    }

    public Deck(User owner, String name, String description, DeckVisibility visibility) {
        this.owner = Objects.requireNonNull(owner, "owner");
        edit(name, description, visibility);
    }

    public void edit(String name, String description, DeckVisibility visibility) {
        this.name = requireName(name);
        this.description = blankToNull(description);
        this.visibility = Objects.requireNonNull(visibility, "visibility");
    }

    public void delete(Instant now) {
        if (deletedAt == null) {
            deletedAt = Objects.requireNonNull(now, "now");
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    @Override
    public boolean isOwnedBy(Long userId) {
        return owner.getId().equals(userId);
    }

    /** The owner sees every deck; others only PUBLIC / OFFICIAL ones. A PRIVATE deck of someone else is a 404. */
    public boolean isVisibleTo(Long userId) {
        return !isDeleted() && (visibility != DeckVisibility.PRIVATE || isOwnedBy(userId));
    }

    /** R3: OFFICIAL decks are edited by admins, every other deck only by its owner. */
    public boolean isEditableBy(Long userId, Role role) {
        if (isDeleted()) {
            return false;
        }
        return visibility == DeckVisibility.OFFICIAL ? role == Role.ADMIN : isOwnedBy(userId);
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return name.trim();
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public User getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public DeckVisibility getVisibility() {
        return visibility;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
