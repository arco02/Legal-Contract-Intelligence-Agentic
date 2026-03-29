package com.legalrag.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContractChunkRepository extends JpaRepository<ContractChunk, UUID> {

    // PGVector cosine similarity search — filtered by user + contract types
    @Query(value = """
        SELECT id, document_id, user_id, contract_type,
               page_number, chunk_index, content,
               1 - (embedding <=> CAST(:embedding AS vector)) AS score
        FROM contract_chunks
        WHERE user_id = CAST(:userId AS uuid)
          AND contract_type = ANY(CAST(:types AS text[]))
        ORDER BY score DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarChunks(
        @Param("embedding") String embedding,
        @Param("userId") String userId,
        @Param("types") String[] types,
        @Param("limit") int limit
    );

    // PGVector cosine similarity search — scoped to a single document
    // Used when user selects a specific document from the dropdown.
    // contract_type filter is intentionally removed — the document already
    // belongs to exactly one type, so filtering by type adds no value and
    // could incorrectly exclude chunks if the type mapping ever drifts.
    @Query(value = """
        SELECT id, document_id, user_id, contract_type,
               page_number, chunk_index, content,
               1 - (embedding <=> CAST(:embedding AS vector)) AS score
        FROM contract_chunks
        WHERE user_id = CAST(:userId AS uuid)
          AND document_id = CAST(:documentId AS uuid)
        ORDER BY score DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarChunksByDocument(
        @Param("embedding") String embedding,
        @Param("userId") String userId,
        @Param("documentId") String documentId,
        @Param("limit") int limit
    );

    // Delete all chunks for a document (used on document delete)
    @Modifying
    @Transactional
    @Query("DELETE FROM ContractChunk c WHERE c.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") UUID documentId);

    long countByDocumentId(UUID documentId);
}