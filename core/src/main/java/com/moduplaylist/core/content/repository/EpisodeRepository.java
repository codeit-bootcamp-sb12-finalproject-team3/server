package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.Episode;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpisodeRepository extends JpaRepository<Episode, UUID> {

    List<Episode> findAllBySeason_IdOrderByEpisodeNumberAsc(UUID seasonId);

    Optional<Episode> findByExternalId(Integer externalId);

    List<Episode> findAllByExternalIdIn(Collection<Integer> externalIds);

    boolean existsBySeason_IdAndEpisodeNumber(UUID seasonId, Integer episodeNumber);
}
