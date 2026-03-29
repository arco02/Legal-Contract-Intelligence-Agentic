package com.legalrag.chat;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(nullable = false, length = 10)
    private String role;           // "user" | "assistant"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // Stored as JSONB in Postgres:
    // [{"type":"CONTRACT","title":"NDA.pdf","page":12,"contractType":"COMMERCIAL"},
    //  {"type":"LAW","title":"Indian Contract Act 1872","section":"Section 73"}]
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String sources;

    @Column(length = 10)
    private String mode;           // "ASK" | "SEARCH"

    @Column(name = "is_web_search")
    @Builder.Default
    private Boolean isWebSearch = false;

    @Column(name = "is_partial")
    @Builder.Default
    private Boolean isPartial = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}