package com.legalrag.rag.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouterDecision {

    // Contract types the router decided to search
    // e.g. ["COMMERCIAL", "CORPORATE_IP"]
    private List<String> contractTypes;

    // Whether to also search the Indian law corpus
    private boolean searchLaw;

    // Whether to search ALL contract types (router chose E)
    private boolean searchAllContracts;

    // Raw LLM response for debugging
    private String rawResponse;
}