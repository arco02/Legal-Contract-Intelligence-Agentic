package com.legalrag.rag.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngestionRequest {

    private String pdfBase64;
    private String documentId;
    private String contractType;
}