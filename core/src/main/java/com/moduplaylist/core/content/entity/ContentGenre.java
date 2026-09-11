package com.moduplaylist.core.content.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;

@Getter
@Entity
@Table(
        name = "content_genres",
        indexes = {
                @Index(name = "idx_content_genres_genre", columnList = "genre_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_content_genres",
                        columnNames = {"content_id", "genre_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentGenre extends ContentUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "content_id",
            nullable = false,
            updatable = false
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Content content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "genre_id",
            nullable = false,
            updatable = false
    )
    private Genre genre;

    private ContentGenre(Content content, Genre genre) {
        this.content = Objects.requireNonNull(content, "content는 필수입니다.");
        this.genre = Objects.requireNonNull(genre, "genre는 필수입니다.");
    }

    public static ContentGenre create(Content content, Genre genre) {
        return new ContentGenre(content, genre);
    }
}