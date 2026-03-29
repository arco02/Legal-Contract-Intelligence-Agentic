package com.legalrag.law;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "law_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LawDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "law_type", nullable = false, length = 50)
    private String lawType;        // STATUTE | CASE_LAW | REGULATION

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column
    private Integer year;

    @CreationTimestamp
    @Column(name = "loaded_at", updatable = false)
    private Instant loadedAt;
}