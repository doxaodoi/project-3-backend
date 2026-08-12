package com.reclaim.repository;

import com.reclaim.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    /** Mark every incoming (not-mine) message in a conversation as read. */
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true "
         + "WHERE m.conversation.id = :convId AND m.sender.id <> :userId AND m.isRead = false")
    void markConversationRead(@Param("convId") Long convId, @Param("userId") Long userId);
}
