package com.moduplaylist.core.playlist.entity;

import com.moduplaylist.core.common.BaseEntity;
import com.moduplaylist.core.content.entity.Genre;
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
@Table(name = "playlist_genres")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistGenre extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "playlist_id", nullable = false)
  private Playlist playlist;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "genre_id", nullable = false)
  private Genre genre;

  private PlaylistGenre(Playlist playlist, Genre genre) {
    this.playlist = playlist;
    this.genre = genre;
    validate();
  }

  public static PlaylistGenre create(Playlist playlist, Genre genre) {
    return new PlaylistGenre(playlist, genre);
  }

  private void validate() {
    if (playlist == null) {
      throw new IllegalArgumentException("플레이리스트는 필수입니다.");
    }

    if (genre == null) {
      throw new IllegalArgumentException("장르는 필수입니다.");
    }
  }
}