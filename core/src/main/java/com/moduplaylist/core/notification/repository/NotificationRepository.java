package com.moduplaylist.core.notification.repository;

import com.moduplaylist.core.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
              :cursorCreatedAt is null
              or n.createdAt < :cursorCreatedAt
              OR (
                  n.createdAt = :cursorCreatedAt
                  AND n.id < :cursorId
              )
          )
        ORDER BY n.createdAt DESC, n.id DESC
        """)
    Slice<Notification> findNextPage(
            @Param("receiverId") UUID receiverId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
