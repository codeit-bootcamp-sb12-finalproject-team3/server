package com.moduplaylist.core.notification.repository;

import com.moduplaylist.core.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByReceiverIdOrderByCreatedAtDescIdDesc(UUID receiverId, Pageable pageable);

    @Query("""
        SELECT n
        FROM Notification n
        WHERE n.receiver.id = :receiverId
          AND (
              n.createdAt < :cursorCreatedAt
              OR (
                  n.createdAt = :cursorCreatedAt
                  AND n.id < :cursorId
              )
          )
        ORDER BY n.createdAt DESC, n.id DESC
        """)
    List<Notification> findNextPage(
            UUID receiverId,
            Instant cursorCreatedAt,
            UUID cursorId,
            Pageable pageable
    );
}
