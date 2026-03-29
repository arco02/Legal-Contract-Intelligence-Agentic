package com.legalrag.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class WebSearchService {

    private final RestTemplate restTemplate;

    @Value("${tavily.api-key}")
    private String tavilyApiKey;

    @Value("${tavily.base-url}")
    private String tavilyBaseUrl;

    @Value("${tavily.result-count:3}")
    private int resultCount;

    @Value("${tavily.search-depth:basic}")
    private String searchDepth;

    public WebSearchService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    // ─── Main search entry point ───────────────────────────────────────────────

    public List<String> search(String query) {

        if (tavilyApiKey == null || tavilyApiKey.isBlank()
                || tavilyApiKey.equals("your-tavily-api-key")) {
            log.warn("Tavily API key not configured — returning empty results");
            return List.of("Web search is not configured. Please set TAVILY_API_KEY.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("api_key",      tavilyApiKey);
            body.put("query",        query + " India law legal");
            body.put("search_depth", searchDepth);
            body.put("max_results",  resultCount);
            body.put("include_answer", false);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                tavilyBaseUrl, HttpMethod.POST, request, Map.class
            );

            return extractSnippets(response.getBody());

        } catch (Exception e) {
            log.error("Tavily search failed for query '{}': {}", query, e.getMessage());
            return List.of("Web search temporarily unavailable: " + e.getMessage());
        }
    }


    // ─── Extract snippets from Tavily response ─────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<String> extractSnippets(Map<String, Object> responseBody) {

        if (responseBody == null) return Collections.emptyList();

        List<String> snippets = new ArrayList<>();

        try {
            List<Map<String, Object>> results =
                (List<Map<String, Object>>) responseBody.get("results");

            if (results == null) return snippets;

            for (Map<String, Object> result : results) {
                String title   = (String) result.getOrDefault("title",   "");
                String url     = (String) result.getOrDefault("url",     "");
                String content = (String) result.getOrDefault("content", "");

                snippets.add("Title: %s\nURL: %s\nSummary: %s"
                    .formatted(title, url, content));

                if (snippets.size() >= resultCount) break;
            }

        } catch (Exception e) {
            log.warn("Failed to parse Tavily response: {}", e.getMessage());
        }

        log.info("Tavily search returned {} snippets", snippets.size());
        return snippets;
    }
}