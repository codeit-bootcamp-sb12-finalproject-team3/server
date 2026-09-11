package com.moduplaylist.core.playlist.entity;

import com.moduplaylist.core.common.BaseEntity;
import com.moduplaylist.core.content.entity.Content;
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
@Table( name = "playlist_contents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistContent extends BaseEntity {

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "playlist_id", nullable = false)
  private Playlist playlist;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "content_id", nullable = false)
  private Content content;

  private PlaylistContent(Playlist playlist, Content content) {
    this.playlist = playlist;
    this.content = content;
    validate();
  }

  public static PlaylistContent create(Playlist playlist, Content content) {
    return new PlaylistContent(playlist, content);
  }

  private void validate() {
    if (playlist == null) {
      throw new IllegalArgumentException("플레이리스트는 필수입니다.");
    }


    if (content == null) {
      throw new IllegalArgumentException("콘텐츠는 필수입니다.");
    }
  }

}
