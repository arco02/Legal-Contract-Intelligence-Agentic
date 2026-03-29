package com.legalrag.document.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentListResponse {

    private String  documentId;
    private String  title;
    private String  contractType;
    private String  originalFilename;
    private Integer totalPages;
    private String  ingestionStatus;
    private Instant uploadedAt;
}