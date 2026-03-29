package com.legalrag.rag.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetrievedChunk {

    // Common fields
    private String  content;
    private double  score;          // cosine similarity 0.0 – 1.0
    private int     pageNumber;
    private int     chunkIndex;

    // Contract chunk fields (null for law chunks)
    private String  documentId;
    private String  documentTitle;
    private String  contractType;   // COMMERCIAL | CORPORATE_IP | OPERATIONAL

    // Law chunk fields (null for contract chunks)
    private String  lawDocumentId;
    private String  lawTitle;
    private String  lawType;        // STATUTE | CASE_LAW | REGULATION
    private String  sectionReference;  // e.g. "Section 73"

    // Source type discriminator
    private SourceType sourceType;  // CONTRACT | LAW

    public enum SourceType {
        CONTRACT, LAW
    }
}