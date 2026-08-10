package com.reclaim.repository;

import com.reclaim.entity.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    List<Claim> findByItemId(Long itemId);
    List<Claim> findByClaimantId(Long claimantId);
    List<Claim> findByStatus(String status);
    long countByStatus(String status);
}
