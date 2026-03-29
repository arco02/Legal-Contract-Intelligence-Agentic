package com.legalrag.law;

import com.legalrag.rag.PythonIngestionClient;
import com.legalrag.rag.RetrieverNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class LawCorpusLoader implements ApplicationRunner {

    private final LawDocumentRepository   lawDocumentRepository;
    private final LawChunkRepository      lawChunkRepository;
    private final PythonIngestionClient   pythonIngestionClient;

    // Statutes — loaded from resources/law-corpus/
    private static final List<LawCorpusEntry> STATUTES = List.of(
        new LawCorpusEntry(
            "Indian Contract Act 1872",
            "STATUTE",
            "indian-contract-act-1872.txt",
            "https://indiankanoon.org/doc/1729136/",
            1872
        ),
        new LawCorpusEntry(
            "Arbitration and Conciliation Act 1996",
            "STATUTE",
            "arbitration-act-1996.txt",
            "https://indiankanoon.org/doc/1306164/",
            1996
        ),
        new LawCorpusEntry(
            "Specific Relief Act 1963",
            "STATUTE",
            "specific-relief-act-1963.txt",
            "https://indiankanoon.org/doc/1271396/",
            1963
        ),
        new LawCorpusEntry(
            "Information Technology Act 2000 (Relevant Sections)",
            "STATUTE",
            "it-act-2000-relevant.txt",
            "https://indiankanoon.org/doc/1185033/",
            2000
        )
    );


    // ─── Run once on application startup ──────────────────────────────────────

    @Override
    public void run(ApplicationArguments args) {
        log.info("LawCorpusLoader: checking if law corpus needs loading...");

        long totalChunks = lawChunkRepository.count();
        if (totalChunks > 0) {
            log.info("LawCorpusLoader: corpus already loaded ({} chunks) — skipping", totalChunks);
            return;
        }

        log.info("LawCorpusLoader: law_chunks table is empty — loading corpus...");

        int totalLoaded = 0;

        // Load statutes
        totalLoaded += loadStatutes();

        // Load judgments
        totalLoaded += loadJudgments();

        log.info("LawCorpusLoader: COMPLETE — {} total chunks loaded", totalLoaded);
    }


    // ─── Load statutes ────────────────────────────────────────────────────────

    private int loadStatutes() {
        int count = 0;
        for (LawCorpusEntry entry : STATUTES) {
            try {
                String text = readResource("law-corpus/" + entry.filename());
                if (text == null || text.isBlank()) {
                    log.warn("Statute file empty or missing: {}", entry.filename());
                    continue;
                }
                count += ingestLawDocument(
                    entry.title(), entry.lawType(),
                    entry.sourceUrl(), entry.year(), text
                );
            } catch (Exception e) {
                log.error("Failed to load statute {}: {}", entry.title(), e.getMessage());
            }
        }
        return count;
    }


    // ─── Load judgments from resources/law-corpus/judgments/ ─────────────────

    private int loadJudgments() {
        int count = 0;
        try {
            PathMatchingResourcePatternResolver resolver =
                new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(
                "classpath:law-corpus/judgments/*.txt"
            );

            log.info("LawCorpusLoader: found {} judgment files", resources.length);

            for (Resource resource : resources) {
                try {
                    String filename = resource.getFilename();
                    if (filename == null) continue;

                    String text = new String(
                        resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8
                    );
                    if (text.isBlank()) continue;

                    // Derive title from filename: "judgment-001.txt" → "Judgment 001"
                    String title = deriveJudgmentTitle(text, filename);

                    count += ingestLawDocument(
                        title, "CASE_LAW", null, null, text
                    );
                } catch (Exception e) {
                    log.error("Failed to load judgment {}: {}",
                        resource.getFilename(), e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("No judgment files found or error scanning: {}", e.getMessage());
        }
        return count;
    }


    // ─── Ingest a single law document ─────────────────────────────────────────

    private int ingestLawDocument(String title,
                                   String lawType,
                                   String sourceUrl,
                                   Integer year,
                                   String text) {
        // Skip if already loaded
        if (lawDocumentRepository.existsByTitle(title)) {
            log.info("LawCorpusLoader: '{}' already exists — skipping", title);
            return 0;
        }

        // Save law document record
        LawDocument lawDoc = LawDocument.builder()
            .title(title)
            .lawType(lawType)
            .sourceUrl(sourceUrl)
            .year(year)
            .build();
        lawDoc = lawDocumentRepository.save(lawDoc);

        // Call Python service to chunk + embed
        List<Map<String, Object>> chunks = pythonIngestionClient.ingestLaw(
            text, lawDoc.getId().toString(), lawType
        );

        if (chunks.isEmpty()) {
            log.warn("LawCorpusLoader: Python returned 0 chunks for '{}'", title);
            return 0;
        }

        // Save chunks to law_chunks table
        List<LawChunk> chunkEntities = new ArrayList<>();
        for (Map<String, Object> chunk : chunks) {
            try {
                String   content      = (String)  chunk.get("content");
                @SuppressWarnings("unchecked")
                List<Double> emb      = (List<Double>) chunk.get("embedding");
                String   sectionRef   = (String)  chunk.getOrDefault("section_reference", "");
                Integer  chunkIndex   = (Integer) chunk.get("chunk_index");

                String embeddingStr = RetrieverNode.formatEmbedding(emb);

                chunkEntities.add(LawChunk.builder()
                    .lawDocument(lawDoc)
                    .lawType(lawType)
                    .sectionReference(sectionRef)
                    .pageNumber(0)
                    .chunkIndex(chunkIndex)
                    .content(content)
                    .embedding(embeddingStr)
                    .build());

            } catch (Exception e) {
                log.warn("Failed to map law chunk for '{}': {}", title, e.getMessage());
            }
        }

        lawChunkRepository.saveAll(chunkEntities);
        log.info("LawCorpusLoader: loaded '{}' — {} chunks", title, chunkEntities.size());
        return chunkEntities.size();
    }


    // ─── Read text file from classpath resources ───────────────────────────────

    private String readResource(String path) {
        try {
            PathMatchingResourcePatternResolver resolver =
                new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResource("classpath:" + path);
            if (!resource.exists()) {
                log.warn("Resource not found: {}", path);
                return null;
            }
            return new String(
                resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            log.error("Failed to read resource {}: {}", path, e.getMessage());
            return null;
        }
    }


    // ─── Derive judgment title from first line of text ────────────────────────

    private String deriveJudgmentTitle(String text, String filename) {
        // Try to use first non-empty line as title (judgment files start with case name)
        String[] lines = text.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && trimmed.length() <= 200) {
                return trimmed;
            }
        }
        // Fallback: derive from filename
        return filename
            .replace(".txt", "")
            .replace("-", " ")
            .replace("_", " ");
    }


    // ─── Corpus entry record ──────────────────────────────────────────────────

    private record LawCorpusEntry(
        String title,
        String lawType,
        String filename,
        String sourceUrl,
        Integer year
    ) {}
}