package com.reclaim.repository;

import com.reclaim.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {
    java.util.Optional<Location> findByNameIgnoreCase(String name);
}
