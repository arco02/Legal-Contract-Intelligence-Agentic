package com.legalrag.rag.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RAGResult {

    private String              answer;
    private List<RetrievedChunk> sources;

    @Builder.Default
    private boolean isWebSearch = false;

    @Builder.Default
    private boolean isPartial   = false;

    private String              conversationId;
    private String              messageId;

    // Convenience: was CRAG gate passed?
    @Builder.Default
    private boolean contextSufficient = true;

    // Web search snippets when fallback triggered
    @Builder.Default
    private List<String> webSearchSnippets = new ArrayList<>();
}