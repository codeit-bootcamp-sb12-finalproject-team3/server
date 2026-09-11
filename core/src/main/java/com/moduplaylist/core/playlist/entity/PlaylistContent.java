package com.moduplaylist.core.playlist.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import com.moduplaylist.core.content.entity.Content;
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
@Table(name = "playlist_contents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PlaylistContent {

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "playlist_id", nullable = false)
  private Playlist playlist;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "content_id", nullable = false)
  private Content content;

  @CreatedDate
  @Column(name = "created_at", updatable = false, nullable = false)
  private Instant createdAt;

  private PlaylistContent(Playlist playlist, Content content) {
    this.playlist = playlist;
    this.content = content;
  }

  public static PlaylistContent create(Playlist playlist, Content content) {
    return new PlaylistContent(playlist, content);
  }

  @PrePersist
  protected void init() {
    if (this.id == null) {
      this.id = UuidCreator.getTimeOrderedEpoch();
    }
  }
}