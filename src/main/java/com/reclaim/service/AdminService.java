package com.reclaim.service;

import com.reclaim.repository.*;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final ItemRepository itemRepo;
    private final ClaimRepository claimRepo;
    private final UserRepository userRepo;

    public AdminService(ItemRepository itemRepo, ClaimRepository claimRepo,
                        UserRepository userRepo) {
        this.itemRepo = itemRepo;
        this.claimRepo = claimRepo;
        this.userRepo = userRepo;
    }

    public Map<String, Object> getStats() {
        long totalItems = itemRepo.count();
        long lostCount = itemRepo.countByType("LOST");
        long foundCount = itemRepo.countByType("FOUND");
        long resolvedCount = itemRepo.countByStatus("RESOLVED");
        long pendingClaims = claimRepo.countByStatus("PENDING");
        long totalUsers = userRepo.count();
        double resolutionRate = totalItems > 0 ? (double) resolvedCount / totalItems * 100 : 0;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalItems", totalItems);
        stats.put("lostCount", lostCount);
        stats.put("foundCount", foundCount);
        stats.put("resolvedCount", resolvedCount);
        stats.put("pendingClaims", pendingClaims);
        stats.put("totalUsers", totalUsers);
        stats.put("resolutionRate", Math.round(resolutionRate * 10) / 10.0);

        // Category breakdown
        List<Object[]> byCategory = itemRepo.countByCategory();
        Map<String, Long> categoryMap = new LinkedHashMap<>();
        for (Object[] row : byCategory) {
            categoryMap.put((String) row[0], (Long) row[1]);
        }
        stats.put("byCategory", categoryMap);

        // Location breakdown (for heatmap) — combined + split by type
        stats.put("byLocation", toLocationMap(itemRepo.countByLocation()));
        stats.put("byLocationLost", toLocationMap(itemRepo.countByLocationAndType("LOST")));
        stats.put("byLocationFound", toLocationMap(itemRepo.countByLocationAndType("FOUND")));

        return stats;
    }

    private Map<String, Long> toLocationMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            map.put((String) row[0], (Long) row[1]);
        }
        return map;
    }
}
