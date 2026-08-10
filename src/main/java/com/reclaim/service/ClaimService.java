package com.reclaim.service;

import com.reclaim.dto.request.ClaimRequest;
import com.reclaim.dto.response.ClaimResponse;
import com.reclaim.entity.*;
import com.reclaim.exception.ApiException;
import com.reclaim.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClaimService {

    private final ClaimRepository claimRepo;
    private final ItemRepository itemRepo;
    private final NotificationRepository notifRepo;
    private final ConversationRepository convRepo;

    public ClaimService(ClaimRepository claimRepo, ItemRepository itemRepo,
                        NotificationRepository notifRepo, ConversationRepository convRepo) {
        this.claimRepo = claimRepo;
        this.itemRepo = itemRepo;
        this.notifRepo = notifRepo;
        this.convRepo = convRepo;
    }

    @Transactional
    public ClaimResponse create(Long itemId, ClaimRequest req, User claimant) {
        Item item = itemRepo.findById(itemId)
            .orElseThrow(() -> ApiException.notFound("Item"));

        if (item.getReporter().getId().equals(claimant.getId())) {
            throw ApiException.badRequest("You cannot claim your own item");
        }

        if (claimRepo.existsByItemIdAndClaimantId(itemId, claimant.getId())) {
            throw ApiException.badRequest("You have already submitted a claim for this item");
        }

        Claim claim = Claim.builder()
            .item(item)
            .claimant(claimant)
            .message(req.getMessage())
            .build();
        claimRepo.save(claim);

        // Notify the item reporter
        notifRepo.save(Notification.builder()
            .user(item.getReporter())
            .type("CLAIM")
            .title("New claim on your item")
            .body(claimant.getFullName() + " claims \"" + item.getTitle() + "\"")
            .link("/items/" + item.getId())
            .build());

        return ClaimResponse.from(claim);
    }

    @Transactional(readOnly = true)
    public List<ClaimResponse> getForItem(Long itemId) {
        return claimRepo.findByItemId(itemId).stream()
            .map(ClaimResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ClaimResponse> getMyClaims(User user) {
        return claimRepo.findByClaimantId(user.getId()).stream()
            .map(ClaimResponse::from).toList();
    }

    @Transactional
    public ClaimResponse updateStatus(Long claimId, String status, User reviewer) {
        Claim claim = claimRepo.findById(claimId)
            .orElseThrow(() -> ApiException.notFound("Claim"));

        Item item = claim.getItem();
        // Only the item reporter or admin can approve/reject
        if (!item.getReporter().getId().equals(reviewer.getId())
                && !"ADMIN".equals(reviewer.getRole())) {
            // Claimant can only withdraw
            if ("WITHDRAWN".equalsIgnoreCase(status)
                    && claim.getClaimant().getId().equals(reviewer.getId())) {
                claim.setStatus("WITHDRAWN");
            } else {
                throw ApiException.forbidden("Not authorized to review this claim");
            }
        } else {
            claim.setStatus(status.toUpperCase());
            claim.setReviewedBy(reviewer);
            claim.setReviewedAt(LocalDateTime.now());
        }

        // If approved, create a conversation between finder and claimant
        if ("APPROVED".equals(claim.getStatus())) {
            Conversation conv = Conversation.builder()
                .item(item)
                .claim(claim)
                .userA(item.getReporter())
                .userB(claim.getClaimant())
                .build();
            convRepo.save(conv);

            // Move item toward resolved
            item.setStatus("RESOLVED");
            itemRepo.save(item);
        }

        // Notify the claimant
        notifRepo.save(Notification.builder()
            .user(claim.getClaimant())
            .type("CLAIM_UPDATE")
            .title("Claim " + claim.getStatus().toLowerCase())
            .body("Your claim on \"" + item.getTitle() + "\" was " + claim.getStatus().toLowerCase())
            .link("/items/" + item.getId())
            .build());

        return ClaimResponse.from(claimRepo.save(claim));
    }
}
