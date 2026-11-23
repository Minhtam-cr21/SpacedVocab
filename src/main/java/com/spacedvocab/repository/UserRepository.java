package com.spacedvocab.repository;

import com.spacedvocab.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /** Expects an already normalized (lower-cased) email, see {@link User#normalizeEmail(String)}. */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
