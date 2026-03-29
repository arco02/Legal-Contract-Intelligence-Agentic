package com.legalrag.chat.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourceDTO {

    private String type;           // "CONTRACT" | "LAW"
    private String title;
    private Double score;

    // Contract fields
    private Integer page;
    private String  contractType;

    // Law fields
    private String section;
    private String lawType;
}