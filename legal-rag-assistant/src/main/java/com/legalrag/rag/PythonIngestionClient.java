package com.legalrag.rag;

import com.legalrag.rag.dto.IngestionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class PythonIngestionClient {

    private final RestTemplate restTemplate;

    @Value("${python.service.url}")
    private String pythonServiceUrl;


    // ─── Contract PDF Ingestion ────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> ingestPdf(String pdfBase64,
                                                String documentId,
                                                String contractType) {
        String url = pythonServiceUrl + "/ingest";

        Map<String, String> body = Map.of(
            "pdf_base64",   pdfBase64,
            "document_id",  documentId,
            "contract_type", contractType
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> chunks =
                    (List<Map<String, Object>>) response.getBody().get("chunks");
                log.info("Python ingestion returned {} chunks for document {}",
                    chunks != null ? chunks.size() : 0, documentId);
                return chunks != null ? chunks : Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Python ingestion failed for document {}: {}", documentId, e.getMessage());
            throw new RuntimeException("PDF ingestion service unavailable: " + e.getMessage(), e);
        }

        return Collections.emptyList();
    }


    // ─── Indian Law Text Ingestion ─────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> ingestLaw(String text,
                                                String lawDocumentId,
                                                String lawType) {
        String url = pythonServiceUrl + "/ingest-law";

        Map<String, String> body = Map.of(
            "text",            text,
            "law_document_id", lawDocumentId,
            "law_type",        lawType
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> chunks =
                    (List<Map<String, Object>>) response.getBody().get("chunks");
                log.info("Python law ingestion returned {} chunks for law doc {}",
                    chunks != null ? chunks.size() : 0, lawDocumentId);
                return chunks != null ? chunks : Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Python law ingestion failed for {}: {}", lawDocumentId, e.getMessage());
            throw new RuntimeException("Law ingestion service unavailable: " + e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    public List<Double> embedQuery(String query) {
        try {
            Map<String, String> body = Map.of("text", query);
            Map<?, ?> response = restTemplate.postForObject(
                pythonServiceUrl + "/embed", body, Map.class
            );
            if (response == null) throw new RuntimeException("Empty response from /embed");
            return (List<Double>) response.get("embedding");
        } catch (Exception e) {
            log.error("Failed to embed query: {}", e.getMessage());
            throw new RuntimeException("Query embedding failed", e);
        }
    }

    // ─── Health Check ──────────────────────────────────────────────────────────

    public boolean isHealthy() {
        try {
            ResponseEntity<Map> response =
                restTemplate.getForEntity(pythonServiceUrl + "/health", Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Python service health check failed: {}", e.getMessage());
            return false;
        }
    }
}