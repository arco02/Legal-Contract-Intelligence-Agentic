package com.legalrag.chat.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchRequest {

    private String       query;
    private List<String> contractTypes;   // null → search all types
}