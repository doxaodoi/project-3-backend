package com.reclaim.controller;

import com.reclaim.entity.Category;
import com.reclaim.entity.Location;
import com.reclaim.repository.CategoryRepository;
import com.reclaim.repository.LocationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Public reference-data endpoints (categories and campus locations). */
@RestController
@RequestMapping("/api")
public class ReferenceController {

    private final CategoryRepository categoryRepo;
    private final LocationRepository locationRepo;

    public ReferenceController(CategoryRepository categoryRepo, LocationRepository locationRepo) {
        this.categoryRepo = categoryRepo;
        this.locationRepo = locationRepo;
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> categories() {
        return ResponseEntity.ok(categoryRepo.findAll());
    }

    @GetMapping("/locations")
    public ResponseEntity<List<Location>> locations() {
        return ResponseEntity.ok(locationRepo.findAll());
    }
}
