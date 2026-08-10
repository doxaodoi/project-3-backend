package com.reclaim.repository;

import com.reclaim.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    Page<Item> findByReporterId(Long reporterId, Pageable pageable);

    List<Item> findByReporterId(Long reporterId);

    @Query(value = """
        SELECT i FROM Item i
        LEFT JOIN FETCH i.category
        LEFT JOIN FETCH i.location
        LEFT JOIN FETCH i.reporter
        WHERE (:type IS NULL OR i.type = :type)
          AND (:status IS NULL OR i.status = :status)
          AND (:categoryId IS NULL OR i.category.id = :categoryId)
          AND (:locationId IS NULL OR i.location.id = :locationId)
          AND (:q IS NULL OR (LOWER(CAST(i.title AS string)) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
               OR LOWER(CAST(i.description AS string)) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))))
    """,
    countQuery = """
        SELECT COUNT(i) FROM Item i
        WHERE (:type IS NULL OR i.type = :type)
          AND (:status IS NULL OR i.status = :status)
          AND (:categoryId IS NULL OR i.category.id = :categoryId)
          AND (:locationId IS NULL OR i.location.id = :locationId)
          AND (:q IS NULL OR (LOWER(CAST(i.title AS string)) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
               OR LOWER(CAST(i.description AS string)) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))))
    """)
    Page<Item> search(
        @Param("type") String type,
        @Param("status") String status,
        @Param("categoryId") Long categoryId,
        @Param("locationId") Long locationId,
        @Param("q") String q,
        Pageable pageable
    );

    /** Opposite-type OPEN items for matching. */
    @Query("SELECT i FROM Item i WHERE i.type = :type AND i.status = 'OPEN'")
    List<Item> findOpenByType(@Param("type") String type);

    long countByType(String type);

    long countByStatus(String status);

    @Query("SELECT i.category.name, COUNT(i) FROM Item i GROUP BY i.category.name ORDER BY COUNT(i) DESC")
    List<Object[]> countByCategory();

    @Query("SELECT i.location.name, COUNT(i) FROM Item i WHERE i.location IS NOT NULL GROUP BY i.location.name ORDER BY COUNT(i) DESC")
    List<Object[]> countByLocation();
}
