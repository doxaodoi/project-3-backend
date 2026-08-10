package com.reclaim.controller;

import com.reclaim.dto.request.ClaimRequest;
import com.reclaim.dto.response.ClaimResponse;
import com.reclaim.entity.User;
import com.reclaim.service.ClaimService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @PostMapping("/items/{itemId}/claims")
    public ResponseEntity<ClaimResponse> create(
            @PathVariable Long itemId,
            @Valid @RequestBody ClaimRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(claimService.create(itemId, req, user));
    }

    @GetMapping("/items/{itemId}/claims")
    public ResponseEntity<List<ClaimResponse>> getForItem(@PathVariable Long itemId) {
        return ResponseEntity.ok(claimService.getForItem(itemId));
    }

    @GetMapping("/claims/mine")
    public ResponseEntity<List<ClaimResponse>> mine(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(claimService.getMyClaims(user));
    }

    @PatchMapping("/claims/{id}")
    public ResponseEntity<ClaimResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(claimService.updateStatus(id, body.get("status"), user));
    }
}
