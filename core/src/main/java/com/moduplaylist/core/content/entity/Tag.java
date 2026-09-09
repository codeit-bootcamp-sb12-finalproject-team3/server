package com.moduplaylist.core.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "tags")
public class Tag {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

}
