package com.legalrag.rag;

import com.legalrag.rag.dto.RetrievedChunk;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CRAGGrader {

    private final OpenAiChatModel fastModel;

    private static final String GRADER_PROMPT = """
        You are a relevance grader for a legal document retrieval system.

        Question: %s

        Retrieved context:
        %s

        Task: Determine if the retrieved context contains sufficient information
        to answer the question accurately and completely.

        Criteria for YES:
        - The context directly addresses the question
        - Key facts, clauses, or legal references needed are present
        - The answer can be grounded in the provided text

        Criteria for NO:
        - The context is on a different topic
        - Critical information is missing
        - The context is too vague or generic to answer the question

        Answer with ONLY the word YES or NO. No explanation.
        """;

    public CRAGGrader(
            @Value("${groq.base-url}")   String baseUrl,
            @Value("${groq.api-key}")    String apiKey,
            @Value("${groq.model.fast}") String fastModelName) {

        this.fastModel = OpenAiChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(apiKey)
            .modelName(fastModelName)
            .temperature(0.0)
            .maxTokens(5)
            .build();
    }


    // ─── Main grading entry point ──────────────────────────────────────────────

    public boolean isSufficient(String question, List<RetrievedChunk> chunks) {

        if (chunks == null || chunks.isEmpty()) {
            log.info("CRAG grader: no chunks — returning insufficient");
            return false;
        }

        // Use top 3 chunks for grading (cheaper, faster)
        String context = chunks.stream()
            .limit(3)
            .map(chunk -> buildChunkSummary(chunk))
            .collect(Collectors.joining("\n\n---\n\n"));

        String prompt = GRADER_PROMPT.formatted(question, context);

        try {
            dev.langchain4j.data.message.UserMessage userMessage =
                dev.langchain4j.data.message.UserMessage.from(prompt);

            dev.langchain4j.model.chat.response.ChatResponse response =
                fastModel.chat(userMessage);

            String raw = response.aiMessage().text().trim().toUpperCase();
            log.debug("CRAG grader raw response: '{}'", raw);

            boolean sufficient = raw.startsWith("YES");

            // Fail open: if grader says NO but a contract chunk has acceptable
            // score, trust the retrieval — grader can be overly strict on
            // large page-sized chunks
            if (!sufficient) {
                boolean hasGoodContractChunk = chunks.stream()
                    .anyMatch(c -> c.getSourceType() == RetrievedChunk.SourceType.CONTRACT
                                && c.getScore() > 0.40);
                if (hasGoodContractChunk) {
                    log.info("CRAG grader said NO but contract chunk score "
                        + "acceptable — overriding to SUFFICIENT");
                    return true;
                }
            }

            log.info("CRAG grader decision: {} for question: '{}'",
                sufficient ? "SUFFICIENT" : "INSUFFICIENT", question);
            return sufficient;

        } catch (Exception e) {
            log.error("CRAG grader LLM call failed: {} — defaulting to sufficient",
                e.getMessage());
            return true;
        }
    }


    // ─── Format chunk for grading prompt ──────────────────────────────────────

    private String buildChunkSummary(RetrievedChunk chunk) {
        if (chunk.getSourceType() == RetrievedChunk.SourceType.CONTRACT) {
            return "[Contract: %s, Page %d, Score: %.3f]\n%s".formatted(
                chunk.getDocumentTitle(),
                chunk.getPageNumber(),
                chunk.getScore(),
                chunk.getContent()
            );
        } else {
            return "[Law: %s, %s, Score: %.3f]\n%s".formatted(
                chunk.getLawTitle(),
                chunk.getSectionReference(),
                chunk.getScore(),
                chunk.getContent()
            );
        }
    }
}