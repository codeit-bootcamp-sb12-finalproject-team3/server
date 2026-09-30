package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.Episode;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpisodeRepository extends JpaRepository<Episode, UUID> {

    List<Episode> findAllBySeason_IdAndSeason_HiddenFalseOrderByEpisodeNumberAsc(
            UUID seasonId);

    Optional<Episode> findByIdAndSeason_IdAndSeason_HiddenFalse(
            UUID episodeId,
            UUID seasonId);

    Optional<Episode> findByExternalSourceAndExternalId(
            String externalSource,
            Integer externalId);

    List<Episode> findAllByExternalSourceAndExternalIdIn(
            String externalSource,
            Collection<Integer> externalIds);

    boolean existsBySeason_IdAndEpisodeNumber(UUID seasonId, Integer episodeNumber);

    boolean existsBySeason_IdAndEpisodeNumberAndIdNot(
            UUID seasonId,
            Integer episodeNumber,
            UUID episodeId);

    long countBySeason_Id(UUID seasonId);
}
