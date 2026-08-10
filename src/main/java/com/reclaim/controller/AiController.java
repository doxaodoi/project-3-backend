package com.reclaim.controller;

import com.reclaim.dto.response.AiDescribeResponse;
import com.reclaim.exception.ApiException;
import com.reclaim.service.AiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /** Smart-Describe: upload a photo → get structured item fields. */
    @PostMapping("/describe")
    public ResponseEntity<?> describe(@RequestParam("photo") MultipartFile photo) {
        if (!aiService.isAvailable()) {
            return ResponseEntity.ok(Map.of(
                "error", "AI service not configured",
                "available", false
            ));
        }

        try {
            String base64 = Base64.getEncoder().encodeToString(photo.getBytes());
            String mediaType = photo.getContentType() != null ? photo.getContentType() : "image/jpeg";

            AiDescribeResponse result = aiService.describeItem(base64, mediaType);
            if (result != null) {
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.ok(Map.of(
                "error", "AI analysis failed — please describe the item manually",
                "available", true
            ));
        } catch (Exception e) {
            throw ApiException.badRequest("Failed to process photo");
        }
    }

    /** Check if AI features are available. */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> status() {
        return ResponseEntity.ok(Map.of("available", aiService.isAvailable()));
    }
}
