package com.reclaim.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lightweight public health check. No DB, no auth — just an instant 200 so an
 * external uptime pinger (UptimeRobot, cron-job.org, etc.) can keep the free-tier
 * Render service from spinning down. Hit GET /ping every ~10 minutes.
 */
@RestController
public class PingController {

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "time", System.currentTimeMillis()
        ));
    }
}
