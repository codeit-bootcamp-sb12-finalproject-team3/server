package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContentRepository extends JpaRepository<Content, UUID> {
}
