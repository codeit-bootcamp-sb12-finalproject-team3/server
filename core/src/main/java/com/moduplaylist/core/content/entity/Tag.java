package com.moduplaylist.core.content.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
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
        name = "tags",
        uniqueConstraints = @UniqueConstraint(name = "uq_tags_name", columnNames = "name")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Access(AccessType.FIELD)
public class Tag {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    private Tag(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("태그 이름은 필수입니다.");
        }
        this.name = name;
    }

    public static Tag create(String name) {
        return new Tag(name);
    }

    @PrePersist
    private void initId() {
        if (id == null) {
            id = UuidCreator.getTimeOrderedEpoch();
        }
    }
}
