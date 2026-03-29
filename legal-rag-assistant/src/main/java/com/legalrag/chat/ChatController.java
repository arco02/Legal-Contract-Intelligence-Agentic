package com.legalrag.chat;

import com.legalrag.chat.dto.ChatRequest;
import com.legalrag.chat.dto.SearchRequest;
import com.legalrag.chat.dto.SourceDTO;
import com.legalrag.rag.RAGService;
import com.legalrag.rag.dto.RetrievedChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final RAGService ragService;

    private final ExecutorService executorService =
        Executors.newCachedThreadPool();


    // ─── ASK MODE — SSE streaming ─────────────────────────────────────────────

    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter ask(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        SseEmitter emitter = new SseEmitter(300_000L);   // 5 min timeout

        executorService.submit(() -> {
            try {
                ragService.ask(request, userDetails.getUsername(), emitter);
            } catch (Exception e) {
                log.error("Unhandled error in ask thread: {}", e.getMessage(), e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }


    // ─── SEARCH MODE — synchronous ranked results ─────────────────────────────

    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestBody SearchRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (request.getQuery() == null || request.getQuery().isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Query must not be empty"));
        }

        List<RetrievedChunk> chunks = ragService.search(
            request.getQuery(),
            request.getContractTypes(),
            userDetails.getUsername()
        );

        List<Map<String, Object>> results = chunks.stream()
            .map(chunk -> {
                Map<String, Object> result = new java.util.LinkedHashMap<>();
                result.put("content",      chunk.getContent());
                result.put("score",        chunk.getScore());
                result.put("pageNumber",   chunk.getPageNumber());
                result.put("sourceType",   chunk.getSourceType().name());

                if (chunk.getSourceType() == RetrievedChunk.SourceType.CONTRACT) {
                    result.put("documentTitle", chunk.getDocumentTitle());
                    result.put("contractType",  chunk.getContractType());
                } else {
                    result.put("lawTitle",         chunk.getLawTitle());
                    result.put("sectionReference", chunk.getSectionReference());
                    result.put("lawType",          chunk.getLawType());
                }
                return result;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("results", results));
    }
}