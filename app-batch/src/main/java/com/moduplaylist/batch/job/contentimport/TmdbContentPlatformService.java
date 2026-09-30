package com.moduplaylist.batch.job.contentimport;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentPlatform;
import com.moduplaylist.core.content.entity.Platform;
import com.moduplaylist.core.content.repository.ContentPlatformRepository;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.content.repository.PlatformRepository;
import com.moduplaylist.infrastructure.tmdb.TmdbKrWatchProviderExtractor;
import com.moduplaylist.infrastructure.tmdb.TmdbKrWatchProviderExtractor.Result;
import com.moduplaylist.infrastructure.tmdb.TmdbProperties;
import com.moduplaylist.infrastructure.tmdb.TmdbWatchProviderResponse;
import com.moduplaylist.infrastructure.tmdb.TmdbWatchProviderResponse.Provider;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TmdbContentPlatformService {

    private static final String REGION_CODE = "KR";

    private final PlatformRepository platformRepository;
    private final ContentPlatformRepository contentPlatformRepository;
    private final ContentRepository contentRepository;
    private final TmdbKrWatchProviderExtractor watchProviderExtractor;
    private final TmdbProperties tmdbProperties;

    @Transactional
    public void synchronizeProviders(UUID contentId, TmdbWatchProviderResponse response) {
        Content content = contentRepository.findById(contentId).orElse(null);
        if (content == null || content.isHidden()) return;

        Result result = watchProviderExtractor.extract(response).orElse(null);
        List<Provider> providers = result == null ? List.of() : result.providers();

        Map<Integer, Platform> platformsByTmdbId = platformRepository
            .findAllByTmdbProviderIdIn(providers.stream().map(Provider::providerId).toList())
            .stream()
            .collect(Collectors.toMap(Platform::getTmdbProviderId, Function.identity()));

        List<Platform> newPlatforms = providers.stream()
            .filter(provider -> !platformsByTmdbId.containsKey(provider.providerId()))
            .map(provider -> Platform.create(
                provider.providerId(),
                provider.providerName(),
                tmdbProperties.imageUrl(provider.logoPath())
            ))
            .toList();
        platformRepository.saveAll(newPlatforms).forEach(
            platform -> platformsByTmdbId.put(platform.getTmdbProviderId(), platform)
        );

        boolean changed = false;
        for (Provider provider : providers) {
            Platform platform = platformsByTmdbId.get(provider.providerId());
            String previousName = platform.getName();
            String previousLogoUrl = platform.getLogoUrl();
            platform.updateDetails(
                provider.providerName(),
                tmdbProperties.imageUrl(provider.logoPath())
            );
            changed |= !Objects.equals(previousName, platform.getName())
                || !Objects.equals(previousLogoUrl, platform.getLogoUrl());
        }

        List<ContentPlatform> existingRelations = contentPlatformRepository
            .findAllWithPlatformByContentIdAndRegionCode(content.getId(), REGION_CODE);
        Map<UUID, ContentPlatform> relationsByPlatformId = existingRelations.stream()
            .collect(Collectors.toMap(
                relation -> relation.getPlatform().getId(),
                Function.identity()
            ));
        Set<UUID> providerPlatformIds = platformsByTmdbId.values().stream()
            .map(Platform::getId)
            .collect(Collectors.toSet());

        List<ContentPlatform> removedRelations = existingRelations.stream()
            .filter(relation -> relation.getSource() == ContentPlatform.PlatformSource.TMDB)
            .filter(relation -> !providerPlatformIds.contains(relation.getPlatform().getId()))
            .toList();
        contentPlatformRepository.deleteAll(removedRelations);
        changed |= !removedRelations.isEmpty();

        if (result == null) {
            if (changed) content.markUpdated();
            return;
        }

        List<ContentPlatform> newRelations = providers.stream()
            .map(provider -> platformsByTmdbId.get(provider.providerId()))
            .filter(platform -> !relationsByPlatformId.containsKey(platform.getId()))
            .map(platform -> ContentPlatform.create(
                content,
                platform,
                ContentPlatform.PlatformSource.TMDB,
                REGION_CODE,
                result.link()
            ))
            .toList();
        contentPlatformRepository.saveAll(newRelations);
        changed |= !newRelations.isEmpty();

        for (ContentPlatform relation : existingRelations) {
            if (relation.getSource() != ContentPlatform.PlatformSource.TMDB
                || !providerPlatformIds.contains(relation.getPlatform().getId())
                || Objects.equals(relation.getUrl(), result.link())) {
                continue;
            }
            relation.updateTmdbUrl(result.link());
            changed = true;
        }

        if (changed) content.markUpdated();
    }
}
