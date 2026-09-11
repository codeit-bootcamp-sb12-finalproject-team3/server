package com.moduplaylist.core.playlist.entity;

import com.moduplaylist.core.common.BaseEntity;
import com.moduplaylist.core.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "playlist_subscriptions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistSubscription extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "playlist_id", nullable = false)
  private Playlist playlist;

  public PlaylistSubscription(User user, Playlist playlist) {
    this.user = user;
    this.playlist = playlist;
    validate();
  }

  public static PlaylistSubscription create(User user, Playlist playlist) {
    return new PlaylistSubscription(user, playlist);
  }

  private void validate() {
    if (user == null) {
      throw new IllegalArgumentException("User cannot be null");
    }
    if (playlist == null) {
      throw new IllegalArgumentException("Playlist cannot be null");
    }
  }
}
