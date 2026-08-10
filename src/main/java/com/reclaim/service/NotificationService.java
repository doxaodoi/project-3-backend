package com.reclaim.service;

import com.reclaim.dto.response.NotificationResponse;
import com.reclaim.entity.Notification;
import com.reclaim.entity.User;
import com.reclaim.exception.ApiException;
import com.reclaim.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notifRepo;

    public NotificationService(NotificationRepository notifRepo) {
        this.notifRepo = notifRepo;
    }

    public List<NotificationResponse> getMyNotifications(User user) {
        return notifRepo.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
            .map(NotificationResponse::from)
            .toList();
    }

    public long getUnreadCount(User user) {
        return notifRepo.countByUserIdAndIsRead(user.getId(), false);
    }

    @Transactional
    public void markRead(Long notifId, User user) {
        Notification n = notifRepo.findById(notifId)
            .orElseThrow(() -> ApiException.notFound("Notification"));
        if (!n.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("Not your notification");
        }
        n.setIsRead(true);
        notifRepo.save(n);
    }

    @Transactional
    public void markAllRead(User user) {
        notifRepo.markAllRead(user.getId());
    }
}
