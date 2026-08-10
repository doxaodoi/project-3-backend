package com.reclaim.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reclaim.dto.response.AiDescribeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Thin wrapper around the Anthropic Claude API for:
 * 1) Smart-Describe: image → structured item fields
 * 2) Match Explainer: two items → human-readable explanation
 *
 * Graceful fallback: if the API key is absent or the call fails,
 * returns null so the caller can handle manual entry.
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final String API_URL = "https://api.anthropic.com/v1/messages";

    @Value("${anthropic.api-key}")
    private String apiKey;

    @Value("${anthropic.model}")
    private String model;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Analyze a photo and return structured item fields.
     * @param imageBase64 base64-encoded image data
     * @param mediaType e.g. "image/jpeg"
     */
    public AiDescribeResponse describeItem(String imageBase64, String mediaType) {
        if (!isAvailable()) return null;

        try {
            String requestBody = mapper.writeValueAsString(new java.util.LinkedHashMap<>() {{
                put("model", model);
                put("max_tokens", 1024);
                put("messages", List.of(new java.util.LinkedHashMap<>() {{
                    put("role", "user");
                    put("content", List.of(
                        new java.util.LinkedHashMap<>() {{
                            put("type", "image");
                            put("source", new java.util.LinkedHashMap<>() {{
                                put("type", "base64");
                                put("media_type", mediaType);
                                put("data", imageBase64);
                            }});
                        }},
                        new java.util.LinkedHashMap<>() {{
                            put("type", "text");
                            put("text", """
                                You are helping catalog a found item for a campus lost & found system.
                                Analyze this image and respond with ONLY a JSON object (no markdown, no backticks):
                                {
                                  "title": "short descriptive title (max 60 chars)",
                                  "description": "detailed 1-2 sentence description",
                                  "category": "one of: Electronics, Keys & Access, Bags & Wallets, Clothing, Books & Notes, ID & Documents, Water Bottles, Jewelry, Sports Equipment, Other",
                                  "color": "primary color",
                                  "brand": "brand if visible, else null",
                                  "tags": ["tag1", "tag2", "tag3"]
                                }
                                """);
                        }}
                    ));
                }}));
            }});

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                String text = root.path("content").get(0).path("text").asText();

                // Parse the JSON from Claude's response
                JsonNode result = mapper.readTree(text);
                List<String> tags = new ArrayList<>();
                if (result.has("tags")) {
                    result.path("tags").forEach(t -> tags.add(t.asText()));
                }

                return AiDescribeResponse.builder()
                    .title(result.path("title").asText(null))
                    .description(result.path("description").asText(null))
                    .category(result.path("category").asText(null))
                    .color(result.path("color").asText(null))
                    .brand(result.path("brand").isNull() ? null : result.path("brand").asText(null))
                    .tags(tags)
                    .build();
            } else {
                log.warn("Claude API returned status {}: {}", response.statusCode(), response.body());
                return null;
            }
        } catch (Exception e) {
            log.error("AI describe failed", e);
            return null;
        }
    }

    /**
     * Generate a human-readable explanation of why two items might be a match.
     */
    public String explainMatch(String lostTitle, String lostDesc, String lostCategory,
                                String foundTitle, String foundDesc, String foundCategory,
                                double score) {
        if (!isAvailable()) return null;

        try {
            String prompt = String.format("""
                You are a smart matching assistant for a campus lost & found system.
                Explain in 2-3 conversational sentences why these two items might be the same thing.
                Be specific about what they have in common.

                LOST ITEM:
                - Title: %s
                - Description: %s
                - Category: %s

                FOUND ITEM:
                - Title: %s
                - Description: %s
                - Category: %s

                Match score: %.0f%%

                Respond with ONLY the explanation, no preamble.
                """, lostTitle, lostDesc, lostCategory, foundTitle, foundDesc, foundCategory, score * 100);

            String requestBody = mapper.writeValueAsString(new java.util.LinkedHashMap<>() {{
                put("model", model);
                put("max_tokens", 256);
                put("messages", List.of(new java.util.LinkedHashMap<>() {{
                    put("role", "user");
                    put("content", prompt);
                }}));
            }});

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                return root.path("content").get(0).path("text").asText();
            } else {
                log.warn("Claude API returned status {}", response.statusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("AI explain failed", e);
            return null;
        }
    }
}
