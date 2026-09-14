package com.moduplaylist.core.playlist.entity;

import com.moduplaylist.core.common.BaseEntity;
import com.moduplaylist.core.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "playlists")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Playlist extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @Column(name = "title", nullable = false, length = 100)
  private String title;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(
      name = "weekly_popularity_score",
      nullable = false,
      precision = 10,
      scale = 2
  )
  private BigDecimal weeklyPopularityScore = BigDecimal.ZERO;

  private Playlist(User owner, String title, String description) {
    this.owner = owner;
    this.title = title;
    this.description = description;
    this.weeklyPopularityScore = BigDecimal.ZERO;
    validate();
  }

  public static Playlist create(User owner, String title, String description) {
    return new Playlist(owner, title, description);
  }

  private void validate() {
    if (owner == null) {
      throw new IllegalArgumentException("플레이리스트 소유자는 필수입니다.");
    }

    if (title == null || title.isBlank() || title.length() > 100) {
      throw new IllegalArgumentException("플레이리스트 제목은 필수이며 100자 이하여야 합니다.");
    }
  }
}
