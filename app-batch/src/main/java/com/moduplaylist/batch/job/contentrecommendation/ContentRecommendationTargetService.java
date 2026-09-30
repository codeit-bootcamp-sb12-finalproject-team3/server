package com.moduplaylist.batch.job.contentrecommendation;

import com.moduplaylist.core.recommendation.repository.UserContentGenrePreferenceRepository;
import com.moduplaylist.core.recommendation.repository.UserContentTagPreferenceRepository;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentRecommendationTargetService {

    private final UserContentTagPreferenceRepository tagPreferenceRepository;
    private final UserContentGenrePreferenceRepository genrePreferenceRepository;

    public List<UUID> findTargetUserIds() {
        TreeSet<UUID> userIds = new TreeSet<>();
        userIds.addAll(tagPreferenceRepository.findDistinctUserIdsWithPositiveScore());
        userIds.addAll(genrePreferenceRepository.findDistinctUserIdsWithPositiveScore());
        return List.copyOf(userIds);
    }
}
