package com.moduplaylist.api.recommendation.service.impl;

import com.moduplaylist.api.recommendation.service.PlaylistPreferenceUpdateService;
import com.moduplaylist.core.activity.enums.PlaylistActivityType;
import com.moduplaylist.core.playlist.entity.PlaylistGenre;
import com.moduplaylist.core.playlist.entity.PlaylistTag;
import com.moduplaylist.core.playlist.repository.PlaylistGenreRepository;
import com.moduplaylist.core.playlist.repository.PlaylistTagRepository;
import com.moduplaylist.core.recommendation.policy.PlaylistRecommendationScorePolicy;
import com.moduplaylist.core.recommendation.repository.PlaylistPreferenceScoreRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PlaylistPreferenceUpdateServiceImpl implements PlaylistPreferenceUpdateService {

    private final PlaylistGenreRepository playlistGenreRepository;
    private final PlaylistTagRepository playlistTagRepository;
    private final PlaylistPreferenceScoreRepository preferenceScoreRepository;

    @Override
    public void applyActivity(
            UUID userId,
            UUID playlistId,
            PlaylistActivityType activityType
    ) {
        double delta = PlaylistRecommendationScorePolicy.calculate(activityType);

        playlistGenreRepository.findAllWithGenreByPlaylistId(playlistId)
                .stream()
                .map(PlaylistGenre::getGenre)
                .map(genre -> genre.getId())
                .distinct()
                .sorted()
                .forEach(genreId ->
                        preferenceScoreRepository.addGenreScore(userId, genreId, delta));

        playlistTagRepository.findAllWithTagByPlaylistId(playlistId)
                .stream()
                .map(PlaylistTag::getTag)
                .map(tag -> tag.getId())
                .distinct()
                .sorted()
                .forEach(tagId ->
                        preferenceScoreRepository.addTagScore(userId, tagId, delta));
    }
}
