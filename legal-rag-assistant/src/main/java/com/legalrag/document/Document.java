package com.legalrag.document;

import com.legalrag.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "contract_type", nullable = false, length = 50)
    private String contractType;   // COMMERCIAL | CORPORATE_IP | OPERATIONAL

    @Column(name = "original_filename", length = 500)
    private String originalFilename;

    @Column(name = "total_pages")
    private Integer totalPages;

    @Column(name = "ingestion_status", length = 20)
    @Builder.Default
    private String ingestionStatus = "PENDING";   // PENDING | PROCESSING | COMPLETED | FAILED

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    private Instant uploadedAt;
}