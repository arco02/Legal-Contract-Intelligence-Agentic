package com.legalrag.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the User entity.
 *
 * Spring auto-generates the implementation at startup — no SQL needed
 * for these two queries; Spring derives them from the method names.
 *
 * findByEmail       → used by UserDetailsServiceImpl to load a user
 *                     during JWT authentication and by AuthService to
 *                     check for duplicate registration emails.
 *
 * existsByEmail     → used by AuthService before saving a new user,
 *                     avoids a full entity fetch just to check existence.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}