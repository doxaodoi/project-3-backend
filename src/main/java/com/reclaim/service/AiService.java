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
     * Parse a natural-language search query into structured filter parameters.
     * e.g. "black phone lost near JQB last week" → {type: "LOST", category: "Electronics", color: "black", location: "JQB (Dept. of CS)", ...}
     */
    public java.util.Map<String, String> parseSearch(String query) {
        if (!isAvailable()) return null;

        try {
            String today = java.time.LocalDate.now().toString();
            String prompt = String.format("""
                You are a search parser for a campus lost & found system at the University of Ghana, Legon.
                Today's date is %s.

                Parse this user search into structured filters. Respond with ONLY a JSON object (no markdown, no backticks).
                Only include fields you can confidently extract. Omit fields you're unsure about.

                {
                  "type": "LOST or FOUND (if they say lost/found/missing/misplaced → LOST, if they say found/spotted/handed in → FOUND, else omit)",
                  "category": "exactly one of: Electronics, Keys & Access, Bags & Wallets, Clothing, Books & Notes, ID & Documents, Water Bottles, Jewelry, Sports Equipment, Other",
                  "location": "exactly one of: Balme Library, Bush Canteen, JQB (Dept. of CS), Great Hall, Main Gate, Legon Hall, Akuafo Hall, Pentagon Hall, N Block, Security Office, Science Block, Athletic Oval",
                  "color": "primary color mentioned",
                  "q": "remaining keywords for text search (NOT the color/category/location — just item-specific words like brand, model, distinguishing features)",
                  "dateFrom": "YYYY-MM-DD (interpret 'today', 'yesterday', 'last week', 'Monday', etc. relative to today)",
                  "dateTo": "YYYY-MM-DD"
                }

                User's search: "%s"
                """, today, query.replace("\"", "'"));

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
                String text = root.path("content").get(0).path("text").asText();
                JsonNode result = mapper.readTree(text);

                var filters = new java.util.LinkedHashMap<String, String>();
                result.fields().forEachRemaining(entry -> {
                    if (!entry.getValue().isNull() && !entry.getValue().asText().isBlank()) {
                        filters.put(entry.getKey(), entry.getValue().asText());
                    }
                });
                return filters;
            } else {
                log.warn("Claude API parse-search returned status {}", response.statusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("AI parse-search failed", e);
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
