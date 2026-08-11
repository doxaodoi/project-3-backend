package com.reclaim.controller;

import com.reclaim.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * Simple file upload endpoint. Saves photos to disk and returns the
 * publicly accessible URL path. Files are served by WebConfig's
 * resource handler at /uploads/**.
 */
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @PostMapping
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw ApiException.badRequest("File is empty");
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw ApiException.badRequest("Only image files are allowed");
        }

        // Generate a unique filename
        String ext = "";
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String filename = UUID.randomUUID() + ext;

        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath();
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            file.transferTo(target.toFile());

            // Return the URL path (relative — frontend prepends the API base)
            String url = "/uploads/" + filename;
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IOException e) {
            throw ApiException.badRequest("Failed to save file");
        }
    }
}
