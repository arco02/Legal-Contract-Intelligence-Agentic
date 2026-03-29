package com.legalrag.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    // Last N messages for conversation context window
    @Query(value = """
        SELECT * FROM messages
        WHERE conversation_id = CAST(:conversationId AS uuid)
        ORDER BY created_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Message> findLastNMessages(
        @Param("conversationId") String conversationId,
        @Param("limit") int limit
    );

    long countByConversationId(UUID conversationId);
}