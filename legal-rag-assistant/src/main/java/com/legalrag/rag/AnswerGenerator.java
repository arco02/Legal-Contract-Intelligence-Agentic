package com.legalrag.rag;

import com.legalrag.rag.dto.RetrievedChunk;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.StreamingResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AnswerGenerator {

    private final OpenAiChatModel          mainModel;
    private final OpenAiStreamingChatModel streamingModel;

    private static final String SYSTEM_PROMPT = """
        You are a legal document assistant specializing in Indian contract law.
    Answer questions based strictly on the provided contract excerpts and
    Indian law references supplied in the context.

    Rules:
    1. Always cite your sources: for contracts include (Document: {title}, Page: {page});
       for statutes include (Law: {title}, Section: {section}).
    2. If the context does not contain enough information to answer fully,
       say so explicitly — do not fabricate details.
    3. Be precise and concise. Use plain English, not legalese.
    4. When asked about vulnerabilities, risks, or enforceability:
       - Compare each relevant contract clause against the cited Indian law references.
       - Explicitly state if a clause is consistent with, potentially conflicts with,
         or is silent on what Indian law requires.
       - Flag clauses that are vague, one-sided, or commonly disputed in Indian courts.
       - Suggest what a stronger clause would look like, briefly.
    5. When Indian law chunks and contract chunks are both present, always
       cross-reference them — do not answer from one source alone.
    6. IMPORTANT: This response is informational only and does not constitute
       legal advice. Consult a qualified lawyer before acting on any information.
    """;

    public AnswerGenerator(
            @Value("${groq.base-url}")   String baseUrl,
            @Value("${groq.api-key}")    String apiKey,
            @Value("${groq.model.main}") String mainModelName) {

        this.mainModel = OpenAiChatModel.builder()
            .baseUrl(baseUrl).apiKey(apiKey)
            .modelName(mainModelName).temperature(0.1)
            .build();

        this.streamingModel = OpenAiStreamingChatModel.builder()
            .baseUrl(baseUrl).apiKey(apiKey)
            .modelName(mainModelName).temperature(0.1)
            .build();
    }


    // ─── Non-streaming ────────────────────────────────────────────────────────

    public String generate(String question,
                           List<RetrievedChunk> chunks,
                           List<String> history) {

        String userPrompt = buildContractPrompt(question, chunks, history);

        dev.langchain4j.model.chat.response.ChatResponse response = mainModel.chat(
            dev.langchain4j.data.message.SystemMessage.from(SYSTEM_PROMPT),
            dev.langchain4j.data.message.UserMessage.from(userPrompt)
        );
        return response.aiMessage().text();
    }


    // ─── Streaming — contract/law context ─────────────────────────────────────

    public void generateStreaming(String question,
                                   List<RetrievedChunk> chunks,
                                   List<String> history,
                                   boolean isWebSearch,
                                   SseEmitter emitter,
                                   StreamingCallback onComplete) {

        String userPrompt = buildContractPrompt(question, chunks, history);
        streamToEmitter(userPrompt, emitter, onComplete);
    }


    // ─── Streaming — web search fallback ──────────────────────────────────────

    public void generateStreamingFromWeb(String question,
                                          List<String> webSnippets,
                                          List<String> history,
                                          SseEmitter emitter,
                                          StreamingCallback onComplete) {

        String userPrompt = buildWebPrompt(question, webSnippets, history);
        streamToEmitter(userPrompt, emitter, onComplete);
    }


    // ─── Core streaming logic ─────────────────────────────────────────────────

    private void streamToEmitter(String userPrompt, SseEmitter emitter, StreamingCallback onComplete) {

    		StringBuilder fullAnswer = new StringBuilder();

    			// Use .chat() instead of .generate()
    		streamingModel.chat(
    						List.of(
    								dev.langchain4j.data.message.SystemMessage.from(SYSTEM_PROMPT),
    								dev.langchain4j.data.message.UserMessage.from(userPrompt)
    								),
    						// Use the new StreamingChatResponseHandler interface
    		new dev.langchain4j.model.chat.response.StreamingChatResponseHandler() {

    							@Override
    							public void onPartialResponse(String partialResponse) {
    								fullAnswer.append(partialResponse);
    								try {
    									emitter.send(SseEmitter.event()
    											.data("{\"token\":\"" + escapeJson(partialResponse) + "\"}"));
    								} catch (Exception e) {
    										log.warn("SSE send failed: {}", e.getMessage());
    								}
    							}

								@Override
								public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse completeResponse) {
									log.info("Streaming complete (~{} chars)", fullAnswer.length());
									onComplete.accept(fullAnswer.toString());
								}
								
								@Override
								public void onError(Throwable error) {
								log.error("Streaming error: {}", error.getMessage(), error);
								try {
								  emitter.send(SseEmitter.event()
								      .data("{\"error\":\"" + escapeJson(error.getMessage()) + "\"}"));
								  emitter.completeWithError(error);
								} catch (Exception e) {
								  log.warn("Failed to send streaming error event", e);
								}
								}
    						}
    				);
    }


    // ─── Prompt builders ──────────────────────────────────────────────────────

    private String buildContractPrompt(String question,
                                        List<RetrievedChunk> chunks,
                                        List<String> history) {
        StringBuilder contractCtx = new StringBuilder();
        StringBuilder lawCtx      = new StringBuilder();

        for (RetrievedChunk chunk : chunks) {
            if (chunk.getSourceType() == RetrievedChunk.SourceType.CONTRACT) {
                contractCtx.append("--- Contract: ").append(chunk.getDocumentTitle())
                    .append(", Page: ").append(chunk.getPageNumber())
                    .append(", Type: ").append(chunk.getContractType()).append(" ---\n")
                    .append(chunk.getContent()).append("\n\n");
            } else {
                lawCtx.append("--- Law: ").append(chunk.getLawTitle())
                    .append(", ").append(chunk.getSectionReference()).append(" ---\n")
                    .append(chunk.getContent()).append("\n\n");
            }
        }

        return """
            Conversation history:
            %s

            Contract excerpts:
            %s

            Indian law references:
            %s

            Question: %s
            """.formatted(
                history.isEmpty() ? "(none)" : String.join("\n", history),
                contractCtx.isEmpty() ? "(none)" : contractCtx,
                lawCtx.isEmpty()      ? "(none)" : lawCtx,
                question
            );
    }

    private String buildWebPrompt(String question,
                                   List<String> snippets,
                                   List<String> history) {
        StringBuilder webCtx = new StringBuilder();
        for (int i = 0; i < snippets.size(); i++) {
            webCtx.append("Web Result ").append(i + 1).append(":\n")
                  .append(snippets.get(i)).append("\n\n");
        }

        return """
            Conversation history:
            %s

            Web search results (no uploaded contracts matched this query):
            %s

            Question: %s

            Note: This answer is based on web search results, not uploaded contracts.
            """.formatted(
                history.isEmpty() ? "(none)" : String.join("\n", history),
                webCtx,
                question
            );
    }


    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String escapeJson(String text) {
        if (text == null) return "";
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    @FunctionalInterface
    public interface StreamingCallback {
        void accept(String fullAnswer);
    }
}