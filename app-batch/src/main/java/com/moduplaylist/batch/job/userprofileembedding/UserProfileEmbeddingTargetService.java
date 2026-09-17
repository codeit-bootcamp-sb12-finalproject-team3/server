package com.moduplaylist.batch.job.userprofileembedding;

import com.moduplaylist.core.recommendation.entity.UserContentGenrePreference;
import com.moduplaylist.core.recommendation.entity.UserContentTagPreference;
import com.moduplaylist.core.recommendation.repository.UserContentGenrePreferenceRepository;
import com.moduplaylist.core.recommendation.repository.UserContentTagPreferenceRepository;
import com.moduplaylist.infrastructure.embedding.EmbeddingGenerator;
import com.moduplaylist.infrastructure.opensearch.recommendation.UserPreferenceVectorDocument;
import com.moduplaylist.infrastructure.opensearch.recommendation.UserPreferenceVectorRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileEmbeddingTargetService {

    private final UserContentTagPreferenceRepository tagPreferenceRepository;
    private final UserContentGenrePreferenceRepository genrePreferenceRepository;
    private final UserPreferenceVectorRepository vectorRepository;
    private final EmbeddingGenerator embeddingGenerator;

    public List<UUID> findTargetUserIds() {
        TreeSet<UUID> candidateIds = new TreeSet<>();
        candidateIds.addAll(tagPreferenceRepository.findDistinctUserIdsWithPositiveScore());
        candidateIds.addAll(genrePreferenceRepository.findDistinctUserIdsWithPositiveScore());

        return candidateIds.stream()
                .filter(this::requiresEmbedding)
                .toList();
    }

    public List<UUID> findUserIdsWithoutPositivePreference() {
        TreeSet<UUID> allUserIds = new TreeSet<>();
        allUserIds.addAll(tagPreferenceRepository.findDistinctUserIds());
        allUserIds.addAll(genrePreferenceRepository.findDistinctUserIds());

        HashSet<UUID> positiveUserIds = new HashSet<>();
        positiveUserIds.addAll(tagPreferenceRepository.findDistinctUserIdsWithPositiveScore());
        positiveUserIds.addAll(genrePreferenceRepository.findDistinctUserIdsWithPositiveScore());
        allUserIds.removeAll(positiveUserIds);
        return List.copyOf(allUserIds);
    }

    private boolean requiresEmbedding(UUID userId) {
        Instant preferenceUpdatedAt = findPreferenceUpdatedAt(userId);
        return vectorRepository.findById(userId)
                .map(document -> isOutdated(preferenceUpdatedAt, document))
                .orElse(true);
    }

    private Instant findPreferenceUpdatedAt(UUID userId) {
        return Stream.concat(
                        tagPreferenceRepository.findAllByUser_Id(userId).stream()
                                .map(UserContentTagPreference::getScoreUpdatedAt),
                        genrePreferenceRepository.findAllByUser_Id(userId).stream()
                                .map(UserContentGenrePreference::getScoreUpdatedAt)
                )
                .max(Comparator.naturalOrder())
                .orElseThrow(() -> new IllegalStateException(
                        "사용자 선호 갱신 시각을 찾을 수 없습니다. userId=" + userId
                ));
    }

    private boolean isOutdated(
            Instant preferenceUpdatedAt,
            UserPreferenceVectorDocument document
    ) {
        Instant embeddedPreferenceUpdatedAt = document.getPreferenceUpdatedAt();
        return embeddedPreferenceUpdatedAt == null
                || preferenceUpdatedAt.isAfter(embeddedPreferenceUpdatedAt)
                || !Objects.equals(embeddingGenerator.modelName(), document.getEmbeddingModel());
    }
}
