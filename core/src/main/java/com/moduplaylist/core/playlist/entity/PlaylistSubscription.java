package com.moduplaylist.core.playlist.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import com.moduplaylist.core.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "playlist_subscriptions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PlaylistSubscription {

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "playlist_id", nullable = false)
  private Playlist playlist;

  @CreatedDate
  @Column(name = "created_at", updatable = false, nullable = false)
  private Instant createdAt;

  private PlaylistSubscription(User user, Playlist playlist) {
    this.user = user;
    this.playlist = playlist;
  }

  public static PlaylistSubscription create(User user, Playlist playlist) {
    return new PlaylistSubscription(user, playlist);
  }

  @PrePersist
  protected void init() {
    if (this.id == null) {
      this.id = UuidCreator.getTimeOrderedEpoch();
    }
  }
}