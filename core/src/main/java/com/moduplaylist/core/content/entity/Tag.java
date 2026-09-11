package com.moduplaylist.core.content.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    private Tag(String name) {
        this.name = validateName(name);
    }

    public static Tag create(String name) {
        return new Tag(name);
    }

    @PrePersist
    private void assignId() {
        if (id == null) {
            id = UuidCreator.getTimeOrderedEpoch();
        }
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank() || name.length() > 100) {
            throw new IllegalArgumentException("태그 이름은 필수이며 100자 이하여야 합니다.");
        }
        return name;
    }
}
