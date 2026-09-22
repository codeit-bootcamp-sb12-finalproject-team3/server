package com.moduplaylist.core.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "tags",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_tags_name", columnNames = "name")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag extends ContentUuidEntity {
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    private Tag(String name) {
        this.name = validateName(name);
    }

    public static Tag create(String name) {
        return new Tag(name);
    }

    private static String validateName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("태그 이름은 필수이며 100자 이하여야 합니다.");
        }
        String normalized = name.strip();
        if (normalized.isEmpty() || normalized.length() > 100) {
            throw new IllegalArgumentException("태그 이름은 필수이며 100자 이하여야 합니다.");
        }
        return normalized;
    }
}
