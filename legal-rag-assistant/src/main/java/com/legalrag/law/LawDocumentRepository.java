package com.legalrag.law;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LawDocumentRepository extends JpaRepository<LawDocument, UUID> {

    Optional<LawDocument> findByTitle(String title);

    boolean existsByTitle(String title);

    long countByLawType(String lawType);
}