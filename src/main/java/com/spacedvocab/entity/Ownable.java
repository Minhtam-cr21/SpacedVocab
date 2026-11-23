package com.spacedvocab.entity;

/** A resource that belongs to one user. Ownership checks always go through this interface (R3). */
public interface Ownable {

    boolean isOwnedBy(Long userId);
}
