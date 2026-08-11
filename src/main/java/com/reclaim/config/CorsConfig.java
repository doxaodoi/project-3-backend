package com.reclaim.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides the CorsConfigurationSource bean used by Spring Security's
 * built-in CORS support. This ensures preflight OPTIONS requests are
 * handled WITHIN the security filter chain — before authentication runs —
 * so authenticated endpoints (like /api/ai/describe, /api/admin/**)
 * don't reject the browser's preflight.
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * Always-allowed origins so things work even if the CORS_ORIGINS env var
     * is missing or stale on Render.
     */
    private static final List<String> BUILT_IN_ORIGINS = List.of(
        "http://localhost:3000",
        "https://reclaim-br0f.onrender.com"
    );

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Merge env-var origins with built-in list (de-duplicated)
        Set<String> origins = new LinkedHashSet<>(BUILT_IN_ORIGINS);
        if (allowedOrigins != null && !allowedOrigins.isBlank()) {
            origins.addAll(Arrays.asList(allowedOrigins.split(",")));
        }

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(new ArrayList<>(origins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
