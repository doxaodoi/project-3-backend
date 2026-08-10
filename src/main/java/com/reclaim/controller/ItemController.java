package com.reclaim.controller;

import com.reclaim.dto.request.ItemRequest;
import com.reclaim.dto.response.ItemResponse;
import com.reclaim.entity.User;
import com.reclaim.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    public ResponseEntity<Page<ItemResponse>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {

        Sort sortObj = "asc".equalsIgnoreCase(dir)
            ? Sort.by(sort).ascending()
            : Sort.by(sort).descending();
        Pageable pageable = PageRequest.of(page, size, sortObj);

        return ResponseEntity.ok(itemService.search(type, status, categoryId, locationId, q, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ItemResponse> create(
            @Valid @RequestBody ItemRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(itemService.create(req, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ItemRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(itemService.update(id, req, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        itemService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ItemResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(itemService.updateStatus(id, body.get("status"), user));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ItemResponse>> mine(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(itemService.getMyItems(user));
    }
}
