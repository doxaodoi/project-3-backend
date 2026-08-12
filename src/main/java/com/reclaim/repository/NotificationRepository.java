package com.reclaim.repository;

import com.reclaim.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndIsRead(Long userId, boolean isRead);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllRead(@Param("userId") Long userId);

    /** Mark a user's notifications pointing at a given link as read. */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true "
         + "WHERE n.user.id = :userId AND n.link = :link AND n.isRead = false")
    void markReadByLink(@Param("userId") Long userId, @Param("link") String link);
}
