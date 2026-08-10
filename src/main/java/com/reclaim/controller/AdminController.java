package com.reclaim.controller;

import com.reclaim.dto.response.ClaimResponse;
import com.reclaim.dto.response.ItemResponse;
import com.reclaim.dto.response.UserResponse;
import com.reclaim.entity.User;
import com.reclaim.exception.ApiException;
import com.reclaim.repository.ClaimRepository;
import com.reclaim.repository.ItemRepository;
import com.reclaim.repository.UserRepository;
import com.reclaim.service.AdminService;
import com.reclaim.service.ItemService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final UserRepository userRepo;
    private final ItemRepository itemRepo;
    private final ClaimRepository claimRepo;
    private final ItemService itemService;

    public AdminController(AdminService adminService, UserRepository userRepo,
                           ItemRepository itemRepo, ClaimRepository claimRepo,
                           ItemService itemService) {
        this.adminService = adminService;
        this.userRepo = userRepo;
        this.itemRepo = itemRepo;
        this.claimRepo = claimRepo;
        this.itemService = itemService;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/items")
    public ResponseEntity<Page<ItemResponse>> items(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
            itemRepo.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(ItemResponse::from)
        );
    }

    @PatchMapping("/items/{id}")
    public ResponseEntity<ItemResponse> moderateItem(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(itemService.updateStatus(id, body.get("status"), admin));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> removeItem(
            @PathVariable Long id,
            @AuthenticationPrincipal User admin) {
        itemService.delete(id, admin);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> users() {
        return ResponseEntity.ok(
            userRepo.findAll().stream().map(UserResponse::from).toList()
        );
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userRepo.existsById(id)) throw ApiException.notFound("User");
        userRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/claims")
    public ResponseEntity<List<ClaimResponse>> pendingClaims() {
        return ResponseEntity.ok(
            claimRepo.findByStatus("PENDING").stream()
                .map(ClaimResponse::from).toList()
        );
    }
}
