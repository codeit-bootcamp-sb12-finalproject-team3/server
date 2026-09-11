package com.moduplaylist.core.content.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "genres",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_genres_name", columnNames = "name"),
                @UniqueConstraint(
                        name = "uq_genres_external",
                        columnNames = {"external_source", "external_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Genre {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "external_source", nullable = false, length = 30)
    private String externalSource;

    @Column(name = "external_id", nullable = false)
    private Integer externalId;

    private Genre(String name, String externalSource, Integer externalId) {
        this.name = name;
        this.externalSource = externalSource;
        this.externalId = externalId;
        validate();
    }

    public static Genre create(String name, String externalSource, Integer externalId) {
        return new Genre(name, externalSource, externalId);
    }

    @PrePersist
    private void initId() {
        if (id == null) {
            id = UuidCreator.getTimeOrderedEpoch();
        }
    }

    private void validate() {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("장르 이름은 필수입니다.");
        }
        if (externalSource == null || externalSource.isBlank() || externalId == null) {
            throw new IllegalArgumentException("장르의 외부 출처와 외부 ID는 필수입니다.");
        }
    }
}
