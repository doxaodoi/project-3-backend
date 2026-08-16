package com.reclaim.service;

import com.reclaim.dto.response.MatchResponse;
import com.reclaim.entity.Item;
import com.reclaim.entity.Match;
import com.reclaim.entity.Notification;
import com.reclaim.exception.ApiException;
import com.reclaim.repository.ItemRepository;
import com.reclaim.repository.MatchRepository;
import com.reclaim.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MatchService {

    private final MatchRepository matchRepo;
    private final ItemRepository itemRepo;
    private final NotificationRepository notifRepo;

    public MatchService(MatchRepository matchRepo, ItemRepository itemRepo,
                        NotificationRepository notifRepo) {
        this.matchRepo = matchRepo;
        this.itemRepo = itemRepo;
        this.notifRepo = notifRepo;
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> getMatchesForItem(Long itemId) {
        return matchRepo.findByItemId(itemId).stream()
            .map(MatchResponse::from)
            .toList();
    }

    /**
     * Compute matches for a newly created item against all open opposite-type items.
     * Uses a weighted score based on category, geo-distance, text similarity, and color/brand.
     */
    @Transactional
    public void computeMatches(Item newItem) {
        String oppositeType = "LOST".equals(newItem.getType()) ? "FOUND" : "LOST";
        List<Item> candidates = itemRepo.findOpenByType(oppositeType);

        for (Item candidate : candidates) {
            double score = computeScore(newItem, candidate);
            if (score >= 0.25) { // minimum 25% match threshold
                Item lostItem = "LOST".equals(newItem.getType()) ? newItem : candidate;
                Item foundItem = "FOUND".equals(newItem.getType()) ? newItem : candidate;
                int pct = (int) Math.round(score * 100);
                String why = buildExplanation(newItem, candidate);

                Match match = Match.builder()
                    .lostItem(lostItem)
                    .foundItem(foundItem)
                    .score(score)
                    .aiExplanation(why)
                    .build();
                matchRepo.save(match);

                // Notify the candidate's reporter — name the counterpart (the new item)
                notifRepo.save(Notification.builder()
                    .user(candidate.getReporter())
                    .type("MATCH")
                    .title("New " + pct + "% match")
                    .body("Your " + candidate.getType().toLowerCase() + " \"" + candidate.getTitle()
                          + "\" matches a " + newItem.getType().toLowerCase() + " item: \""
                          + newItem.getTitle() + "\"")
                    .link("/items/" + candidate.getId() + "/matches")
                    .build());

                // Notify the new item's reporter — name the counterpart (the candidate)
                notifRepo.save(Notification.builder()
                    .user(newItem.getReporter())
                    .type("MATCH")
                    .title("New " + pct + "% match")
                    .body("Your " + newItem.getType().toLowerCase() + " \"" + newItem.getTitle()
                          + "\" matches a " + candidate.getType().toLowerCase() + " item: \""
                          + candidate.getTitle() + "\"")
                    .link("/items/" + newItem.getId() + "/matches")
                    .build());

                // Update status to MATCHED if still OPEN
                if ("OPEN".equals(newItem.getStatus())) {
                    newItem.setStatus("MATCHED");
                    itemRepo.save(newItem);
                }
                if ("OPEN".equals(candidate.getStatus())) {
                    candidate.setStatus("MATCHED");
                    itemRepo.save(candidate);
                }
            }
        }
    }

    /**
     * Weighted scoring: category 30%, geo-distance 25%, text similarity 30%, color/brand 15%.
     */
    private double computeScore(Item a, Item b) {
        double score = 0;

        // Category match (30%)
        if (a.getCategory() != null && b.getCategory() != null
                && a.getCategory().getId().equals(b.getCategory().getId())) {
            score += 0.30;
        }

        // Geo-distance (25%) — closer = higher score
        if (a.getLatitude() != null && b.getLatitude() != null) {
            double distKm = haversine(a.getLatitude(), a.getLongitude(),
                                      b.getLatitude(), b.getLongitude());
            if (distKm < 0.1) score += 0.25;       // within 100m
            else if (distKm < 0.5) score += 0.18;   // within 500m
            else if (distKm < 1.0) score += 0.10;   // within 1km
        } else if (a.getLocation() != null && b.getLocation() != null
                && a.getLocation().getId().equals(b.getLocation().getId())) {
            score += 0.20; // same named location
        }

        // Text similarity (30%) — simple word overlap
        score += 0.30 * textSimilarity(
            (a.getTitle() + " " + orEmpty(a.getDescription())).toLowerCase(),
            (b.getTitle() + " " + orEmpty(b.getDescription())).toLowerCase()
        );

        // Color + brand match (15%)
        if (a.getColor() != null && a.getColor().equalsIgnoreCase(b.getColor())) {
            score += 0.08;
        }
        if (a.getBrand() != null && a.getBrand().equalsIgnoreCase(b.getBrand())) {
            score += 0.07;
        }

        return Math.min(score, 1.0);
    }

    /**
     * Plain-language reason a match was suggested, derived from the same
     * components as the score. Populates each match so users see *why*, even
     * without an external AI service configured.
     */
    private String buildExplanation(Item a, Item b) {
        java.util.List<String> reasons = new java.util.ArrayList<>();

        if (a.getCategory() != null && b.getCategory() != null
                && a.getCategory().getId().equals(b.getCategory().getId())) {
            reasons.add("same category");
        }

        if (a.getLatitude() != null && b.getLatitude() != null) {
            double d = haversine(a.getLatitude(), a.getLongitude(),
                                 b.getLatitude(), b.getLongitude());
            if (d < 0.1) reasons.add("reported within 100m of each other");
            else if (d < 0.5) reasons.add("reported within 500m of each other");
            else if (d < 1.0) reasons.add("reported within 1km of each other");
        } else if (a.getLocation() != null && b.getLocation() != null
                && a.getLocation().getId().equals(b.getLocation().getId())) {
            reasons.add("same location");
        }

        double sim = textSimilarity(
            (a.getTitle() + " " + orEmpty(a.getDescription())).toLowerCase(),
            (b.getTitle() + " " + orEmpty(b.getDescription())).toLowerCase());
        if (sim >= 0.2) reasons.add("similar description");

        if (a.getColor() != null && a.getColor().equalsIgnoreCase(b.getColor())) {
            reasons.add("matching colour");
        }
        if (a.getBrand() != null && a.getBrand().equalsIgnoreCase(b.getBrand())) {
            reasons.add("matching brand");
        }

        if (reasons.isEmpty()) return "Some overlapping details between the two reports.";
        String joined = reasons.size() == 1
            ? reasons.get(0)
            : String.join(", ", reasons.subList(0, reasons.size() - 1))
              + " and " + reasons.get(reasons.size() - 1);
        return Character.toUpperCase(joined.charAt(0)) + joined.substring(1) + ".";
    }

    /** Haversine distance in km. */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** Jaccard similarity on word sets. */
    private double textSimilarity(String a, String b) {
        Set<String> wordsA = new java.util.HashSet<>(java.util.Arrays.asList(a.split("\\W+")));
        Set<String> wordsB = new java.util.HashSet<>(java.util.Arrays.asList(b.split("\\W+")));
        if (wordsA.isEmpty() || wordsB.isEmpty()) return 0;

        long intersection = wordsA.stream().filter(wordsB::contains).count();
        long union = wordsA.size() + wordsB.size() - intersection;
        return union == 0 ? 0 : (double) intersection / union;
    }

    private String orEmpty(String s) { return s != null ? s : ""; }

    @Transactional
    public MatchResponse confirm(Long matchId) {
        Match match = matchRepo.findById(matchId)
            .orElseThrow(() -> ApiException.notFound("Match"));
        match.setStatus("CONFIRMED");
        return MatchResponse.from(matchRepo.save(match));
    }

    @Transactional
    public MatchResponse dismiss(Long matchId) {
        Match match = matchRepo.findById(matchId)
            .orElseThrow(() -> ApiException.notFound("Match"));
        match.setStatus("DISMISSED");
        return MatchResponse.from(matchRepo.save(match));
    }
}
