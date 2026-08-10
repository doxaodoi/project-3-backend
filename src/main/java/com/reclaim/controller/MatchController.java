package com.reclaim.controller;

import com.reclaim.dto.response.MatchResponse;
import com.reclaim.entity.Match;
import com.reclaim.exception.ApiException;
import com.reclaim.repository.MatchRepository;
import com.reclaim.service.AiService;
import com.reclaim.service.MatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MatchController {

    private final MatchService matchService;
    private final MatchRepository matchRepo;
    private final AiService aiService;

    public MatchController(MatchService matchService, MatchRepository matchRepo,
                           AiService aiService) {
        this.matchService = matchService;
        this.matchRepo = matchRepo;
        this.aiService = aiService;
    }

    @GetMapping("/items/{itemId}/matches")
    public ResponseEntity<List<MatchResponse>> getMatches(@PathVariable Long itemId) {
        return ResponseEntity.ok(matchService.getMatchesForItem(itemId));
    }

    @PostMapping("/matches/{id}/confirm")
    public ResponseEntity<MatchResponse> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(matchService.confirm(id));
    }

    @PostMapping("/matches/{id}/dismiss")
    public ResponseEntity<MatchResponse> dismiss(@PathVariable Long id) {
        return ResponseEntity.ok(matchService.dismiss(id));
    }

    /** Smart Match Explainer — generates AI explanation on demand (cached). */
    @GetMapping("/matches/{id}/explanation")
    public ResponseEntity<Map<String, String>> getExplanation(@PathVariable Long id) {
        Match match = matchRepo.findById(id)
            .orElseThrow(() -> ApiException.notFound("Match"));

        // Return cached explanation if available
        if (match.getAiExplanation() != null) {
            return ResponseEntity.ok(Map.of("explanation", match.getAiExplanation()));
        }

        // Generate via AI
        String explanation = aiService.explainMatch(
            match.getLostItem().getTitle(),
            match.getLostItem().getDescription(),
            match.getLostItem().getCategory() != null ? match.getLostItem().getCategory().getName() : "Unknown",
            match.getFoundItem().getTitle(),
            match.getFoundItem().getDescription(),
            match.getFoundItem().getCategory() != null ? match.getFoundItem().getCategory().getName() : "Unknown",
            match.getScore()
        );

        if (explanation != null) {
            match.setAiExplanation(explanation);
            matchRepo.save(match);
            return ResponseEntity.ok(Map.of("explanation", explanation));
        }

        // Fallback
        return ResponseEntity.ok(Map.of("explanation",
            "These items share similar characteristics and were reported in the same area."));
    }
}
