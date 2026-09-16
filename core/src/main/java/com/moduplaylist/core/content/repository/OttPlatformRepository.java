package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.OttPlatform;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OttPlatformRepository extends JpaRepository<OttPlatform, UUID> {

    Optional<OttPlatform> findByName(String name);

    List<OttPlatform> findAllByNameIn(Collection<String> names);
}
