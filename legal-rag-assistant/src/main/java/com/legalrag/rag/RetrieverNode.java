package com.legalrag.rag;

import com.legalrag.document.ContractChunkRepository;
import com.legalrag.document.DocumentRepository;
import com.legalrag.law.LawChunkRepository;
import com.legalrag.law.LawDocumentRepository;
import com.legalrag.rag.dto.RetrievedChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class RetrieverNode {

    private final ContractChunkRepository  contractChunkRepository;
    private final DocumentRepository       documentRepository;
    private final LawChunkRepository       lawChunkRepository;
    private final LawDocumentRepository    lawDocumentRepository;

    private static final int CONTRACT_LIMIT = 5;
    private static final int LAW_LIMIT      = 3;


    // ─── Main retrieval entry point ────────────────────────────────────────────

    /**
     * documentId == null or "ALL" → query all user chunks across selected contract types
     * documentId == UUID string   → query only chunks from that specific document
     */
    public List<RetrievedChunk> retrieve(String queryEmbedding,
                                          String userId,
                                          List<String> contractTypes,
                                          boolean searchLaw,
                                          String documentId) {

        List<RetrievedChunk> results = new ArrayList<>();

        // 1. Contract chunks — scoped or unscoped
        boolean isScoped = documentId != null && !documentId.isBlank()
                           && !documentId.equalsIgnoreCase("ALL");

        if (isScoped) {
            List<RetrievedChunk> contractChunks = retrieveContractChunksByDocument(
                queryEmbedding, userId, documentId, CONTRACT_LIMIT
            );
            results.addAll(contractChunks);
            log.debug("Retrieved {} contract chunks (document-scoped: {})",
                contractChunks.size(), documentId);

        } else if (contractTypes != null && !contractTypes.isEmpty()) {
            List<RetrievedChunk> contractChunks = retrieveContractChunks(
                queryEmbedding, userId, contractTypes, CONTRACT_LIMIT
            );
            results.addAll(contractChunks);
            log.debug("Retrieved {} contract chunks (all-documents)", contractChunks.size());
        }

        // 2. Law chunks — never scoped to a single document
        if (searchLaw) {
            List<RetrievedChunk> lawChunks = retrieveLawChunks(
                queryEmbedding, LAW_LIMIT
            );
            results.addAll(lawChunks);
            log.debug("Retrieved {} law chunks", lawChunks.size());
        }

        // Sort combined results by score descending
        results.sort(Comparator.comparingDouble(RetrievedChunk::getScore).reversed());

        log.info("Total retrieved chunks: {} (scoped={}, contractTypes={}, searchLaw={})",
            results.size(), isScoped, contractTypes, searchLaw);

        return results;
    }

    /**
     * Backward-compatible overload — called by any existing code that does not
     * yet pass documentId. Delegates to the full method with documentId=null.
     */
    public List<RetrievedChunk> retrieve(String queryEmbedding,
                                          String userId,
                                          List<String> contractTypes,
                                          boolean searchLaw) {
        return retrieve(queryEmbedding, userId, contractTypes, searchLaw, null);
    }


    // ─── Contract chunk retrieval — all documents ──────────────────────────────

    private List<RetrievedChunk> retrieveContractChunks(String queryEmbedding,
                                                          String userId,
                                                          List<String> contractTypes,
                                                          int limit) {
        String[] typesArray = contractTypes.toArray(new String[0]);

        List<Object[]> rows = contractChunkRepository.findSimilarChunks(
            queryEmbedding, userId, typesArray, limit
        );

        return mapContractRows(rows);
    }


    // ─── Contract chunk retrieval — single document ────────────────────────────

    private List<RetrievedChunk> retrieveContractChunksByDocument(String queryEmbedding,
                                                                    String userId,
                                                                    String documentId,
                                                                    int limit) {
        List<Object[]> rows = contractChunkRepository.findSimilarChunksByDocument(
            queryEmbedding, userId, documentId, limit
        );

        return mapContractRows(rows);
    }


    // ─── Row mapper — shared by both contract retrieval paths ─────────────────

    private List<RetrievedChunk> mapContractRows(List<Object[]> rows) {
        List<RetrievedChunk> chunks = new ArrayList<>();
        for (Object[] row : rows) {
            try {
                // Row order from native query:
                // 0=id, 1=document_id, 2=user_id, 3=contract_type,
                // 4=page_number, 5=chunk_index, 6=content, 7=score
                String documentId   = row[1].toString();
                String contractType = row[3].toString();
                int    pageNumber   = row[4] != null ? ((Number) row[4]).intValue() : 0;
                int    chunkIndex   = row[5] != null ? ((Number) row[5]).intValue() : 0;
                String content      = row[6].toString();
                double score        = ((Number) row[7]).doubleValue();

                String documentTitle = documentRepository.findById(
                    UUID.fromString(documentId)
                ).map(d -> d.getTitle()).orElse("Unknown Document");

                chunks.add(RetrievedChunk.builder()
                    .content(content)
                    .score(score)
                    .pageNumber(pageNumber)
                    .chunkIndex(chunkIndex)
                    .documentId(documentId)
                    .documentTitle(documentTitle)
                    .contractType(contractType)
                    .sourceType(RetrievedChunk.SourceType.CONTRACT)
                    .build());

            } catch (Exception e) {
                log.warn("Failed to map contract chunk row: {}", e.getMessage());
            }
        }
        return chunks;
    }


    // ─── Law chunk retrieval ───────────────────────────────────────────────────

    private List<RetrievedChunk> retrieveLawChunks(String queryEmbedding, int limit) {

        List<Object[]> rows = lawChunkRepository.findSimilarLawChunks(
            queryEmbedding, limit
        );

        List<RetrievedChunk> chunks = new ArrayList<>();
        for (Object[] row : rows) {
            try {
                // Row order from native query:
                // 0=id, 1=law_document_id, 2=law_type, 3=section_reference,
                // 4=page_number, 5=chunk_index, 6=content, 7=score
                String lawDocumentId = row[1].toString();
                String lawType       = row[2].toString();
                String sectionRef    = row[3] != null ? row[3].toString() : "";
                int    pageNumber    = row[4] != null ? ((Number) row[4]).intValue() : 0;
                int    chunkIndex    = row[5] != null ? ((Number) row[5]).intValue() : 0;
                String content       = row[6].toString();
                double score         = ((Number) row[7]).doubleValue();

                String lawTitle = lawDocumentRepository.findById(
                    UUID.fromString(lawDocumentId)
                ).map(d -> d.getTitle()).orElse("Unknown Law Document");

                chunks.add(RetrievedChunk.builder()
                    .content(content)
                    .score(score)
                    .pageNumber(pageNumber)
                    .chunkIndex(chunkIndex)
                    .lawDocumentId(lawDocumentId)
                    .lawTitle(lawTitle)
                    .lawType(lawType)
                    .sectionReference(sectionRef)
                    .sourceType(RetrievedChunk.SourceType.LAW)
                    .build());

            } catch (Exception e) {
                log.warn("Failed to map law chunk row: {}", e.getMessage());
            }
        }
        return chunks;
    }


    // ─── Embedding format helper ───────────────────────────────────────────────

    public static String formatEmbedding(List<Double> embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.size(); i++) {
            sb.append(embedding.get(i));
            if (i < embedding.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}