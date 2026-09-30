package com.moduplaylist.batch.job.playlistrecommendation;

import com.moduplaylist.core.recommendation.repository.UserPlaylistGenrePreferenceRepository;
import com.moduplaylist.core.recommendation.repository.UserPlaylistTagPreferenceRepository;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaylistRecommendationTargetService {

    private final UserPlaylistTagPreferenceRepository tagPreferenceRepository;
    private final UserPlaylistGenrePreferenceRepository genrePreferenceRepository;

    public List<UUID> findTargetUserIds() {
        TreeSet<UUID> userIds = new TreeSet<>();
        userIds.addAll(tagPreferenceRepository.findDistinctUserIdsWithPositiveScore());
        userIds.addAll(genrePreferenceRepository.findDistinctUserIdsWithPositiveScore());
        return List.copyOf(userIds);
    }
}
