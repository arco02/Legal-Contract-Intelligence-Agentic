package com.legalrag.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalrag.chat.*;
import com.legalrag.chat.dto.ChatRequest;
import com.legalrag.chat.dto.SourceDTO;
import com.legalrag.rag.dto.*;
import com.legalrag.user.User;
import com.legalrag.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RAGService {

    private final RouterNode              routerNode;
    private final RetrieverNode           retrieverNode;
    private final CRAGGrader              cragGrader;
    private final AnswerGenerator         answerGenerator;
    private final WebSearchService        webSearchService;
    private final ConversationRepository  conversationRepository;
    private final MessageRepository       messageRepository;
    private final UserRepository          userRepository;
    private final PythonIngestionClient   pythonIngestionClient;
    private final ObjectMapper            objectMapper;

    @Value("${rag.similarity-threshold:0.45}")
    private double scoreGate;
    private static final int CONTEXT_WINDOW = 3;


    // ─── ASK MODE — full agentic flow ─────────────────────────────────────────

    public void ask(ChatRequest request,
                    String userEmail,
                    SseEmitter emitter) {
        try {
            User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

            // 1. Resolve or create conversation
            Conversation conversation = resolveConversation(
                request.getConversationId(), user
            );

            // 2. Save user message
            saveMessage(conversation, "user", request.getQuestion(),
                null, "ASK", false, false);

            // 3. Build conversation history
            List<String> history = buildHistory(conversation.getId(), CONTEXT_WINDOW);

            // 4. STEP 1 — ROUTER NODE
            RouterDecision decision = routerNode.route(request.getQuestion(), history);
            log.info("Router: types={}, searchLaw={}", decision.getContractTypes(),
                decision.isSearchLaw());

            // 5. Embed query
            String queryEmbedding = embedQuery(request.getQuestion());

            // 6. STEP 2 — RETRIEVER NODE
            // Pass documentId from request — null / "ALL" → all user chunks,
            // UUID string → scoped to that document only.
            List<RetrievedChunk> chunks = retrieverNode.retrieve(
                queryEmbedding,
                user.getId().toString(),
                decision.getContractTypes(),
                decision.isSearchLaw(),
                request.getDocumentId()
            );

            // 7. STEP 3 — SCORE GATE
            double bestScore = chunks.stream()
                .mapToDouble(RetrievedChunk::getScore)
                .max().orElse(0.0);
            log.info("Best score: {} (documentId={})", bestScore, request.getDocumentId());

            final StringBuilder fullAnswer  = new StringBuilder();
            final boolean[]     isWebSearch = {false};
            final boolean[]     isPartial   = {false};

            if (bestScore < scoreGate) {
                log.info("Score gate FAILED ({}) — routing to web search", bestScore);
                isWebSearch[0] = true;
                streamWebSearchAnswer(request.getQuestion(), history, emitter, fullAnswer);

            } else {
                // 8. STEP 4 — CRAG GRADER
                boolean sufficient = cragGrader.isSufficient(request.getQuestion(), chunks);

                if (!sufficient) {
                    log.info("CRAG grader: INSUFFICIENT — routing to web search");
                    isWebSearch[0] = true;
                    streamWebSearchAnswer(request.getQuestion(), history, emitter, fullAnswer);

                } else {
                    // 9. STEP 5A — ANSWER GENERATION (streaming)
                    log.info("CRAG grader: SUFFICIENT — generating answer from context");

                    java.util.concurrent.CountDownLatch latch =
                        new java.util.concurrent.CountDownLatch(1);

                    answerGenerator.generateStreaming(
                        request.getQuestion(),
                        chunks,
                        history,
                        false,
                        emitter,
                        (answer) -> {
                            fullAnswer.append(answer);
                            latch.countDown();
                        }
                    );

                    try {
                        latch.await(60, java.util.concurrent.TimeUnit.SECONDS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Streaming latch interrupted");
                    }
                }
            }

            // 10. Build sources
            List<SourceDTO> sources = isWebSearch[0]
                ? Collections.emptyList()
                : buildSources(chunks);
            String sourcesJson = objectMapper.writeValueAsString(sources);

            // 11. Save assistant message
            Message assistantMessage = saveMessage(
                conversation, "assistant", fullAnswer.toString(),
                sourcesJson, "ASK", isWebSearch[0], isPartial[0]
            );

            // 12. Auto-title conversation on first exchange
            if (messageRepository.countByConversationId(conversation.getId()) <= 2) {
                String title = request.getQuestion().length() > 60
                    ? request.getQuestion().substring(0, 60) + "..."
                    : request.getQuestion();
                conversation.setTitle(title);
                conversationRepository.save(conversation);
            }

            // 13. Log for evaluation
            logForEvaluation(request.getQuestion(), chunks, fullAnswer.toString());

            // 14. Send final SSE event
            Map<String, Object> finalEvent = new LinkedHashMap<>();
            finalEvent.put("done",           true);
            finalEvent.put("sources",        sources);
            finalEvent.put("isWebSearch",    isWebSearch[0]);
            finalEvent.put("isPartial",      isPartial[0]);
            finalEvent.put("conversationId", conversation.getId().toString());
            finalEvent.put("messageId",      assistantMessage.getId().toString());

            emitter.send(SseEmitter.event()
                .data(objectMapper.writeValueAsString(finalEvent)));
            emitter.complete();

        } catch (Exception e) {
            log.error("RAGService.ask failed: {}", e.getMessage(), e);
            try {
                emitter.send(SseEmitter.event()
                    .data("{\"error\":\"" + escapeJson(e.getMessage()) + "\"}"));
                emitter.completeWithError(e);
            } catch (Exception ex) {
                log.warn("Failed to send error SSE event", ex);
            }
        }
    }


    // ─── SEARCH MODE ──────────────────────────────────────────────────────────

    public List<RetrievedChunk> search(String query,
                                        List<String> contractTypes,
                                        String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        String queryEmbedding = embedQuery(query);

        List<String> types = (contractTypes != null && !contractTypes.isEmpty())
            ? contractTypes
            : List.of("COMMERCIAL", "CORPORATE_IP", "OPERATIONAL");

        // Search mode always queries all documents — no document scoping here.
        // The dropdown scoping applies to ASK mode only.
        return retrieverNode.retrieve(
            queryEmbedding, user.getId().toString(), types, false, null
        ).stream().limit(10).collect(Collectors.toList());
    }


    // ─── Web search fallback — streams answer via SSE ─────────────────────────

    private void streamWebSearchAnswer(String question,
                                        List<String> history,
                                        SseEmitter emitter,
                                        StringBuilder fullAnswer) throws Exception {
        List<String> snippets = webSearchService.search(question);

        java.util.concurrent.CountDownLatch latch =
            new java.util.concurrent.CountDownLatch(1);

        answerGenerator.generateStreamingFromWeb(
            question,
            snippets,
            history,
            emitter,
            (answer) -> {
                fullAnswer.append(answer);
                latch.countDown();
            }
        );

        try {
            latch.await(60, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }


    // ─── Embed query via Python service ───────────────────────────────────────

    private String embedQuery(String query) {
        List<Double> embedding = pythonIngestionClient.embedQuery(query);
        return RetrieverNode.formatEmbedding(embedding);
    }


    // ─── Conversation helpers ─────────────────────────────────────────────────

    private Conversation resolveConversation(String conversationId, User user) {
        if (conversationId != null && !conversationId.isBlank()) {
            return conversationRepository
                .findByIdAndUserId(UUID.fromString(conversationId), user.getId())
                .orElseGet(() -> createConversation(user));
        }
        return createConversation(user);
    }

    private Conversation createConversation(User user) {
        return conversationRepository.save(
            Conversation.builder().user(user).title("New Conversation").build()
        );
    }

    private List<String> buildHistory(UUID conversationId, int lastN) {
        List<Message> recent = messageRepository.findLastNMessages(
            conversationId.toString(), lastN * 2
        );
        Collections.reverse(recent);
        return recent.stream()
            .map(m -> m.getRole().toUpperCase() + ": " + m.getContent())
            .collect(Collectors.toList());
    }

    private Message saveMessage(Conversation conversation, String role,
                                 String content, String sourcesJson,
                                 String mode, boolean isWebSearch,
                                 boolean isPartial) {
        return messageRepository.save(Message.builder()
            .conversation(conversation)
            .role(role)
            .content(content)
            .sources(sourcesJson)
            .mode(mode)
            .isWebSearch(isWebSearch)
            .isPartial(isPartial)
            .build());
    }


    // ─── Sources builder ──────────────────────────────────────────────────────

    private List<SourceDTO> buildSources(List<RetrievedChunk> chunks) {
        return chunks.stream().map(chunk -> {
            if (chunk.getSourceType() == RetrievedChunk.SourceType.CONTRACT) {
                return SourceDTO.builder()
                    .type("CONTRACT")
                    .title(chunk.getDocumentTitle())
                    .page(chunk.getPageNumber())
                    .contractType(chunk.getContractType())
                    .score(chunk.getScore())
                    .build();
            } else {
                return SourceDTO.builder()
                    .type("LAW")
                    .title(chunk.getLawTitle())
                    .section(chunk.getSectionReference())
                    .lawType(chunk.getLawType())
                    .score(chunk.getScore())
                    .build();
            }
        }).collect(Collectors.toList());
    }


    // ─── Evaluation logger ────────────────────────────────────────────────────

    private void logForEvaluation(String question,
                                   List<RetrievedChunk> chunks,
                                   String answer) {
        try {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("question",         question);
            entry.put("retrieved_chunks", chunks.stream()
                .map(RetrievedChunk::getContent).collect(Collectors.toList()));
            entry.put("llm_answer",       answer);
            entry.put("timestamp",        Instant.now().toString());

            File logFile = new File("evaluation/logged_queries.json");
            logFile.getParentFile().mkdirs();

            List<Map<String, Object>> existing = new ArrayList<>();
            if (logFile.exists() && logFile.length() > 0) {
                existing = objectMapper.readValue(logFile,
                    objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, Map.class));
            }
            existing.add(entry);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(logFile, existing);

        } catch (Exception e) {
            log.warn("Evaluation logging failed: {}", e.getMessage());
        }
    }


    // ─── JSON escape ──────────────────────────────────────────────────────────

    private String escapeJson(String text) {
        if (text == null) return "";
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}