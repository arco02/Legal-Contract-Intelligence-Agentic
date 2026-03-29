package com.legalrag.document;

import com.legalrag.user.User;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contract_chunks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "contract_type", nullable = false, length = 50)
    private String contractType;   // COMMERCIAL | CORPORATE_IP | OPERATIONAL

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