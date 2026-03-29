package com.legalrag.law;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "law_chunks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LawChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "law_document_id", nullable = false)
    private LawDocument lawDocument;

    @Column(name = "law_type", nullable = false, length = 50)
    private String lawType;        // STATUTE | CASE_LAW | REGULATION

    @Column(name = "section_reference", length = 200)
    private String sectionReference;   // e.g. "Section 73", "Para 12 of judgment"

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "vector(384)")
    @ColumnTransformer(write = "?::vector")
    private String embedding;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}