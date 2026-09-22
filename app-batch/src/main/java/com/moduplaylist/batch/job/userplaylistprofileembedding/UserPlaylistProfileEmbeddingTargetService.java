package com.moduplaylist.batch.job.userplaylistprofileembedding;

import com.moduplaylist.core.recommendation.entity.UserPlaylistGenrePreference;
import com.moduplaylist.core.recommendation.entity.UserPlaylistTagPreference;
import com.moduplaylist.core.recommendation.repository.UserPlaylistGenrePreferenceRepository;
import com.moduplaylist.core.recommendation.repository.UserPlaylistTagPreferenceRepository;
import com.moduplaylist.infrastructure.embedding.EmbeddingGenerator;
import com.moduplaylist.infrastructure.opensearch.recommendation.UserPlaylistPreferenceVectorDocument;
import com.moduplaylist.infrastructure.opensearch.recommendation.UserPlaylistPreferenceVectorRepository;
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
public class UserPlaylistProfileEmbeddingTargetService {

    private final UserPlaylistTagPreferenceRepository tagPreferenceRepository;
    private final UserPlaylistGenrePreferenceRepository genrePreferenceRepository;
    private final UserPlaylistPreferenceVectorRepository vectorRepository;
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
                                .map(UserPlaylistTagPreference::getScoreUpdatedAt),
                        genrePreferenceRepository.findAllByUser_Id(userId).stream()
                                .map(UserPlaylistGenrePreference::getScoreUpdatedAt)
                )
                .max(Comparator.naturalOrder())
                .orElseThrow(() -> new IllegalStateException(
                        "사용자 플레이리스트 선호 갱신 시각을 찾을 수 없습니다. userId=" + userId
                ));
    }

    private boolean isOutdated(
            Instant preferenceUpdatedAt,
            UserPlaylistPreferenceVectorDocument document
    ) {
        Instant embeddedPreferenceUpdatedAt = document.getPreferenceUpdatedAt();
        return embeddedPreferenceUpdatedAt == null
                || preferenceUpdatedAt.isAfter(embeddedPreferenceUpdatedAt)
                || !Objects.equals(embeddingGenerator.modelName(), document.getEmbeddingModel());
    }
}
