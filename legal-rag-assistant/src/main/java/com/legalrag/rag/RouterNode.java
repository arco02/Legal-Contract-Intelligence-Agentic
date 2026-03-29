package com.legalrag.rag;

import com.legalrag.rag.dto.RouterDecision;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class RouterNode {

    private final OpenAiChatModel fastModel;

    private static final String ROUTER_PROMPT = """
        Given this legal question: '%s'

        Which of the following knowledge bases should be searched?
        Pick ALL that apply based on the question intent.

        A) COMMERCIAL contracts
           (NDAs, non-disclosure agreements, confidentiality agreements,
            license agreements, reseller agreements, distributor agreements,
            sales contracts, vendor agreements, purchase orders)
        B) CORPORATE_IP contracts
           (IP assignment agreements, technology transfer agreements,
            alliance agreements, joint ventures — NOT simple NDAs)
        C) OPERATIONAL contracts
           (service agreements, maintenance contracts, hosting agreements,
            consulting agreements, SLAs, employment contracts)
        D) INDIAN_LAW
           (Indian Contract Act, Arbitration Act, Specific Relief Act,
            Supreme Court / High Court judgments — use when question
            asks about enforceability, legality, or statutory rights)
        E) ALL_CONTRACTS
           (search all contract types — use when question is general
            or could apply across all contract categories)

        Rules:
        - NDAs, non-disclosure agreements, and confidentiality agreements → always pick A.
        - IP assignment, technology transfer, joint ventures → pick B.
        - Service agreements, SLAs, maintenance, consulting → pick C.
        - If the question asks whether something is legally valid or enforceable → always include D.
        - If the question is broad or unclear → pick E.
        - Respond with ONLY a JSON array of letters. No explanation.

        Examples:
        "What are the termination clauses in the NDA?" → ["A"]
        "What does the NDA say about confidential information?" → ["A"]
        "Is the non-compete clause enforceable?" → ["A","D"]
        "Who owns the IP developed under the alliance agreement?" → ["B"]
        "What are the SLA penalties in our hosting contract?" → ["C"]
        "Which contracts expire this year?" → ["E"]
        "What does Indian law say about liquidated damages?" → ["D"]
        "Are there any indemnification clauses?" → ["E","D"]
        "Is a 30-day notice period enforceable under Indian law?" → ["A","D"]

        Question: '%s'
        Response (JSON array only):
        """;

    public RouterNode(
            @Value("${groq.base-url}")   String baseUrl,
            @Value("${groq.api-key}")    String apiKey,
            @Value("${groq.model.fast}") String fastModelName) {

        this.fastModel = OpenAiChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(apiKey)
            .modelName(fastModelName)
            .temperature(0.0)
            .maxTokens(50)
            .build();
    }


    // ─── Main routing entry point ──────────────────────────────────────────────

    public RouterDecision route(String question, List<String> conversationHistory) {

        String contextualQuestion = buildContextualQuestion(question, conversationHistory);
        String prompt = ROUTER_PROMPT.formatted(contextualQuestion, contextualQuestion);

        try {
            dev.langchain4j.data.message.UserMessage userMessage =
                dev.langchain4j.data.message.UserMessage.from(prompt);

            dev.langchain4j.model.chat.response.ChatResponse response =
                fastModel.chat(userMessage);

            String raw = response.aiMessage().text().trim();
            log.debug("Router raw response: {}", raw);

            return parseRouterResponse(raw, question);

        } catch (Exception e) {
            log.error("Router LLM call failed: {} — defaulting to ALL_CONTRACTS", e.getMessage());
            return defaultDecision();
        }
    }


    // ─── Parse LLM response ────────────────────────────────────────────────────

    private RouterDecision parseRouterResponse(String raw, String question) {

        String jsonPart = raw;
        int start = raw.indexOf('[');
        int end   = raw.lastIndexOf(']');
        if (start >= 0 && end > start) {
            jsonPart = raw.substring(start, end + 1);
        }

        List<String> contractTypes = new ArrayList<>();
        boolean      searchLaw     = false;
        boolean      searchAll     = false;

        for (char c : jsonPart.toUpperCase().toCharArray()) {
            switch (c) {
                case 'A' -> contractTypes.add("COMMERCIAL");
                case 'B' -> contractTypes.add("CORPORATE_IP");
                case 'C' -> contractTypes.add("OPERATIONAL");
                case 'D' -> searchLaw  = true;
                case 'E' -> searchAll  = true;
            }
        }

        if (searchAll || (contractTypes.isEmpty() && !searchLaw)) {
            contractTypes = List.of("COMMERCIAL", "CORPORATE_IP", "OPERATIONAL");
            searchAll = true;
        }

        RouterDecision decision = RouterDecision.builder()
            .contractTypes(contractTypes)
            .searchLaw(searchLaw)
            .searchAllContracts(searchAll)
            .rawResponse(raw)
            .build();

        log.info("Router decision for '{}': types={}, searchLaw={}, searchAll={}",
            question, contractTypes, searchLaw, searchAll);

        return decision;
    }


    // ─── Build contextual question using recent history ────────────────────────

    private String buildContextualQuestion(String question,
                                            List<String> conversationHistory) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return question;
        }
        int historySize = conversationHistory.size();
        int fromIndex   = Math.max(0, historySize - 2);
        List<String> recentHistory = conversationHistory.subList(fromIndex, historySize);
        return String.join("\n", recentHistory) + "\nCurrent question: " + question;
    }


    // ─── Default fallback decision ─────────────────────────────────────────────

    private RouterDecision defaultDecision() {
        return RouterDecision.builder()
            .contractTypes(List.of("COMMERCIAL", "CORPORATE_IP", "OPERATIONAL"))
            .searchLaw(false)
            .searchAllContracts(true)
            .rawResponse("DEFAULT")
            .build();
    }
}