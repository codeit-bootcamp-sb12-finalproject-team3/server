package com.moduplaylist.core.playlist.entity;

import com.moduplaylist.core.common.BaseEntity;
import com.moduplaylist.core.content.entity.Tag;
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
@Table(name = "playlist_tags")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistTag extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "playlist_id", nullable = false)
  private Playlist playlist;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tag_id", nullable = false)
  private Tag tag;

  public PlaylistTag(Playlist playlist, Tag tag) {
    this.playlist = playlist;
    this.tag = tag;
    validate();
  }

  public static PlaylistTag create(Playlist playlist, Tag tag) {
    return new PlaylistTag(playlist, tag);
  }

  private void validate() {
    if (playlist == null) {
      throw new IllegalArgumentException("플레이리스트는 필수입니다.");
    }

    if (tag == null) {
      throw new IllegalArgumentException("태그는 필수입니다.");
    }
  }
}
