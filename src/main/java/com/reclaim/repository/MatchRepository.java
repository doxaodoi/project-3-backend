package com.reclaim.repository;

import com.reclaim.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {

    @Query("SELECT m FROM Match m WHERE m.lostItem.id = :itemId OR m.foundItem.id = :itemId ORDER BY m.score DESC")
    List<Match> findByItemId(@Param("itemId") Long itemId);

    List<Match> findByLostItemId(Long lostItemId);
    List<Match> findByFoundItemId(Long foundItemId);
}
