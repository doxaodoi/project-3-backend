package com.reclaim.controller;

import com.reclaim.dto.request.MessageRequest;
import com.reclaim.dto.response.ConversationResponse;
import com.reclaim.dto.response.MessageResponse;
import com.reclaim.entity.User;
import com.reclaim.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService convService;

    public ConversationController(ConversationService convService) {
        this.convService = convService;
    }

    @GetMapping
    public ResponseEntity<List<ConversationResponse>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(convService.getMyConversations(user));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<MessageResponse>> messages(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(convService.getMessages(id, user));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageResponse> send(
            @PathVariable Long id,
            @Valid @RequestBody MessageRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(convService.sendMessage(id, req, user));
    }

    @PatchMapping("/messages/{id}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        convService.markRead(id, user);
        return ResponseEntity.noContent().build();
    }
}
