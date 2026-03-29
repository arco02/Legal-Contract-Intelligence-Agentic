package com.legalrag.chat.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {

    private String question;
    private String conversationId;   // null → create new conversation
    private String documentId;       // null or "ALL" → query all user chunks
                                     // UUID string → query only that document
}