package com.legalrag.law;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface LawChunkRepository extends JpaRepository<LawChunk, UUID> {

    // PGVector cosine similarity search — no user filter (law corpus is shared)
    @Query(value = """
        SELECT id, law_document_id, law_type, section_reference,
               page_number, chunk_index, content,
               1 - (embedding <=> CAST(:embedding AS vector)) AS score
        FROM law_chunks
        ORDER BY score DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarLawChunks(
        @Param("embedding") String embedding,
        @Param("limit") int limit
    );

    // Filtered by law type (e.g. only STATUTE or only CASE_LAW)
    @Query(value = """
        SELECT id, law_document_id, law_type, section_reference,
               page_number, chunk_index, content,
               1 - (embedding <=> CAST(:embedding AS vector)) AS score
        FROM law_chunks
        WHERE law_type = :lawType
        ORDER BY score DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarLawChunksByType(
        @Param("embedding") String embedding,
        @Param("lawType") String lawType,
        @Param("limit") int limit
    );

    // Check if corpus is already loaded (used by LawCorpusLoader)
    long countByLawDocumentId(UUID lawDocumentId);

    boolean existsByLawDocumentId(UUID lawDocumentId);

    @Modifying
    @Transactional
    @Query("DELETE FROM LawChunk c WHERE c.lawDocument.id = :lawDocumentId")
    void deleteByLawDocumentId(@Param("lawDocumentId") UUID lawDocumentId);
}