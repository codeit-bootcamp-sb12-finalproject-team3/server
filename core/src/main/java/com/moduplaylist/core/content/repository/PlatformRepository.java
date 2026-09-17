package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.Platform;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformRepository extends JpaRepository<Platform, UUID> {

	Optional<Platform> findByName(String name);

	Optional<Platform> findByTmdbProviderId(Integer tmdbProviderId);

	List<Platform> findAllByNameIn(Collection<String> names);
}
