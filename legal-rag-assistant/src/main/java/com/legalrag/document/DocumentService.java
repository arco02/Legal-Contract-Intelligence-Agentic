package com.legalrag.document;

import com.legalrag.rag.PythonIngestionClient;
import com.legalrag.user.User;
import com.legalrag.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository       documentRepository;
    private final ContractChunkRepository  contractChunkRepository;
    private final UserRepository           userRepository;
    private final PythonIngestionClient    pythonIngestionClient;


    // ─── Upload: save metadata, kick off async ingestion ──────────────────────

    public Document uploadDocument(MultipartFile file,
                                   String contractType,
                                   String title,
                                   String userEmail) throws IOException {

        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        // 1. Save document record with PROCESSING status
        Document document = Document.builder()
            .user(user)
            .title(title)
            .contractType(contractType.toUpperCase())
            .originalFilename(file.getOriginalFilename())
            .ingestionStatus("PROCESSING")
            .build();

        document = documentRepository.save(document);
        log.info("Document saved with id={}, starting async ingestion", document.getId());

        // 2. Kick off async ingestion (does not block the HTTP response)
        final UUID documentId = document.getId();
        final byte[] pdfBytes  = file.getBytes();
        ingestAsync(documentId, pdfBytes, contractType.toUpperCase(), user);

        return document;
    }


    // ─── Async ingestion: call Python, save chunks ────────────────────────────

    @Async
    public void ingestAsync(UUID documentId,
                            byte[] pdfBytes,
                            String contractType,
                            User user) {
        try {
            String pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);

            List<Map<String, Object>> chunks = pythonIngestionClient.ingestPdf(
                pdfBase64,
                documentId.toString(),
                contractType
            );

            if (chunks.isEmpty()) {
                markFailed(documentId, "Python service returned 0 chunks");
                return;
            }

            // Fetch the managed document entity inside this thread
            Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

            List<ContractChunk> chunkEntities = new ArrayList<>();
            for (Map<String, Object> chunk : chunks) {

                String content   = (String)  chunk.get("content");
                List<Double> emb = (List<Double>) chunk.get("embedding");
                Integer pageNum  = (Integer) chunk.get("page_number");
                Integer chunkIdx = (Integer) chunk.get("chunk_index");

                // Format embedding as "[0.1,0.2,...]" for pgvector
                String embeddingStr = emb.toString()
                    .replace(" ", "");

                ContractChunk entity = ContractChunk.builder()
                    .document(document)
                    .user(user)
                    .contractType(contractType)
                    .pageNumber(pageNum)
                    .chunkIndex(chunkIdx)
                    .content(content)
                    .embedding(embeddingStr)
                    .build();

                chunkEntities.add(entity);
            }

            contractChunkRepository.saveAll(chunkEntities);

            // Update document: COMPLETED + total pages
            int maxPage = chunks.stream()
                .mapToInt(c -> (Integer) c.getOrDefault("page_number", 0))
                .max()
                .orElse(0);

            document.setTotalPages(maxPage);
            document.setIngestionStatus("COMPLETED");
            documentRepository.save(document);

            log.info("Ingestion COMPLETED for document={}, chunks={}", documentId, chunkEntities.size());

        } catch (Exception e) {
            log.error("Ingestion FAILED for document={}: {}", documentId, e.getMessage(), e);
            markFailed(documentId, e.getMessage());
        }
    }


    // ─── List user's documents ─────────────────────────────────────────────────

    public List<Document> listDocuments(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
        return documentRepository.findByUserIdOrderByUploadedAtDesc(user.getId());
    }


    // ─── Delete document + its chunks ─────────────────────────────────────────

    public void deleteDocument(UUID documentId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        if (!document.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied: document belongs to another user");
        }

        contractChunkRepository.deleteByDocumentId(documentId);
        documentRepository.delete(document);
        log.info("Deleted document={} and all its chunks", documentId);
    }


    // ─── Helper ───────────────────────────────────────────────────────────────

    private void markFailed(UUID documentId, String reason) {
        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setIngestionStatus("FAILED");
            documentRepository.save(doc);
            log.warn("Document {} marked FAILED: {}", documentId, reason);
        });
    }
}