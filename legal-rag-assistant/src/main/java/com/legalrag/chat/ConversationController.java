package com.legalrag.chat;

import com.legalrag.user.User;
import com.legalrag.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Slf4j
public class ConversationController {

    private final ConversationRepository conversationRepository;
    private final MessageRepository      messageRepository;
    private final UserRepository         userRepository;


    // ─── List all conversations for user ──────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listConversations(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));

        List<Map<String, Object>> response = conversationRepository
            .findByUserIdOrderByUpdatedAtDesc(user.getId())
            .stream()
            .map(conv -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id",        conv.getId().toString());
                item.put("title",     conv.getTitle());
                item.put("createdAt", conv.getCreatedAt().toString());
                item.put("updatedAt", conv.getUpdatedAt().toString());
                return item;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }


    // ─── Get messages for a conversation ──────────────────────────────────────

    @GetMapping("/{id}/messages")
    public ResponseEntity<?> getMessages(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));

        Conversation conversation = conversationRepository
            .findByIdAndUserId(id, user.getId())
            .orElse(null);

        if (conversation == null) {
            return ResponseEntity.status(403)
                .body(Map.of("error", "Conversation not found or access denied"));
        }

        List<Map<String, Object>> messages = messageRepository
            .findByConversationIdOrderByCreatedAtAsc(id)
            .stream()
            .map(msg -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id",          msg.getId().toString());
                item.put("role",        msg.getRole());
                item.put("content",     msg.getContent());
                item.put("sources",     msg.getSources());
                item.put("mode",        msg.getMode());
                item.put("isWebSearch", msg.getIsWebSearch());
                item.put("isPartial",   msg.getIsPartial());
                item.put("createdAt",   msg.getCreatedAt().toString());
                return item;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(messages);
    }


    // ─── Delete a conversation ────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteConversation(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));

        Conversation conversation = conversationRepository
            .findByIdAndUserId(id, user.getId())
            .orElse(null);

        if (conversation == null) {
            return ResponseEntity.status(403)
                .body(Map.of("error", "Conversation not found or access denied"));
        }

        conversationRepository.delete(conversation);
        log.info("Deleted conversation={} for user={}", id, user.getId());

        return ResponseEntity.ok(Map.of("message", "Conversation deleted"));
    }
}