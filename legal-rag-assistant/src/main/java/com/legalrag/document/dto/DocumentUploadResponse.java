package com.legalrag.document.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentUploadResponse {

    private String documentId;
    private String title;
    private String contractType;
    private String status;
    private String message;
}