package com.moduplaylist.batch.job.contentimport;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentPlatform;
import com.moduplaylist.core.content.entity.Platform;
import com.moduplaylist.core.content.repository.ContentPlatformRepository;
import com.moduplaylist.core.content.repository.PlatformRepository;
import com.moduplaylist.infrastructure.tmdb.TmdbKrWatchProviderExtractor;
import com.moduplaylist.infrastructure.tmdb.TmdbKrWatchProviderExtractor.Result;
import com.moduplaylist.infrastructure.tmdb.TmdbProperties;
import com.moduplaylist.infrastructure.tmdb.TmdbWatchProviderResponse;
import com.moduplaylist.infrastructure.tmdb.TmdbWatchProviderResponse.Provider;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final TmdbKrWatchProviderExtractor watchProviderExtractor;
    private final TmdbProperties tmdbProperties;

    @Transactional
    public void saveInitialProviders(Content content, TmdbWatchProviderResponse response) {
        Result result = watchProviderExtractor.extract(response).orElse(null);
        if (result == null || result.providers().isEmpty()) {
            return;
        }

        List<Provider> providers = result.providers();

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

        Set<java.util.UUID> existingPlatformIds = contentPlatformRepository
            .findAllWithPlatformByContentIdAndRegionCode(content.getId(), REGION_CODE)
            .stream()
            .map(ContentPlatform::getPlatform)
            .map(Platform::getId)
            .collect(Collectors.toSet());

        List<ContentPlatform> relations = providers.stream()
            .map(provider -> platformsByTmdbId.get(provider.providerId()))
            .filter(platform -> !existingPlatformIds.contains(platform.getId()))
            .map(platform -> ContentPlatform.create(
                content,
                platform,
                ContentPlatform.PlatformSource.TMDB,
                REGION_CODE,
                result.link()
            ))
            .toList();
        contentPlatformRepository.saveAll(relations);
    }
}
