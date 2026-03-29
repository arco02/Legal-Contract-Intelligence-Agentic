package com.legalrag.document;

import com.legalrag.document.dto.DocumentListResponse;
import com.legalrag.document.dto.DocumentUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;

    // ─── Upload ───────────────────────────────────────────────────────────────

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> upload(
            @RequestParam("file")         MultipartFile file,
            @RequestParam("contractType") String contractType,
            @RequestParam("title")        String title,
            @AuthenticationPrincipal      UserDetails userDetails) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                DocumentUploadResponse.builder()
                    .message("File must not be empty")
                    .status("FAILED")
                    .build()
            );
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().body(
                DocumentUploadResponse.builder()
                    .message("Only PDF files are accepted")
                    .status("FAILED")
                    .build()
            );
        }

        if (!List.of("COMMERCIAL", "CORPORATE_IP", "OPERATIONAL")
                 .contains(contractType.toUpperCase())) {
            return ResponseEntity.badRequest().body(
                DocumentUploadResponse.builder()
                    .message("contractType must be COMMERCIAL, CORPORATE_IP, or OPERATIONAL")
                    .status("FAILED")
                    .build()
            );
        }

        try {
            Document document = documentService.uploadDocument(
                file, contractType, title, userDetails.getUsername()
            );

            return ResponseEntity.ok(
                DocumentUploadResponse.builder()
                    .documentId(document.getId().toString())
                    .title(document.getTitle())
                    .contractType(document.getContractType())
                    .status(document.getIngestionStatus())
                    .message("Document received. Ingestion in progress.")
                    .build()
            );

        } catch (Exception e) {
            log.error("Upload failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                DocumentUploadResponse.builder()
                    .message("Upload failed: " + e.getMessage())
                    .status("FAILED")
                    .build()
            );
        }
    }


    // ─── List ─────────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<DocumentListResponse>> listDocuments(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<DocumentListResponse> response = documentService
            .listDocuments(userDetails.getUsername())
            .stream()
            .map(doc -> DocumentListResponse.builder()
                .documentId(doc.getId().toString())
                .title(doc.getTitle())
                .contractType(doc.getContractType())
                .originalFilename(doc.getOriginalFilename())
                .totalPages(doc.getTotalPages())
                .ingestionStatus(doc.getIngestionStatus())
                .uploadedAt(doc.getUploadedAt())
                .build()
            )
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }


    // ─── Delete ───────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            documentService.deleteDocument(id, userDetails.getUsername());
            return ResponseEntity.ok(Map.of("message", "Document deleted"));
        } catch (RuntimeException e) {
            log.error("Delete failed for document={}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}