package com.moduplaylist.infrastructure.recommendation.embedding;

import com.moduplaylist.core.recommendation.entity.UserContentGenrePreference;
import com.moduplaylist.core.recommendation.entity.UserContentTagPreference;
import com.moduplaylist.core.recommendation.exception.PreferenceNotFoundException;
import com.moduplaylist.core.recommendation.repository.UserContentGenrePreferenceRepository;
import com.moduplaylist.core.recommendation.repository.UserContentTagPreferenceRepository;
import com.moduplaylist.infrastructure.embedding.EmbeddingGenerator;
import com.moduplaylist.infrastructure.opensearch.recommendation.GenreScoreDocument;
import com.moduplaylist.infrastructure.opensearch.recommendation.TagScoreDocument;
import com.moduplaylist.infrastructure.opensearch.recommendation.UserContentPreferenceVectorDocument;
import com.moduplaylist.infrastructure.opensearch.recommendation.UserContentPreferenceVectorRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.moduplaylist.infrastructure.recommendation.embedding.dto.UserGenrePreferenceValue;
import com.moduplaylist.infrastructure.recommendation.embedding.dto.UserProfileEmbeddingResult;
import com.moduplaylist.infrastructure.recommendation.embedding.dto.UserTagPreferenceValue;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mopl.embedding", name = "enabled", havingValue = "true")
public class UserContentProfileEmbeddingService {

    private final UserContentTagPreferenceRepository tagPreferenceRepository;
    private final UserContentGenrePreferenceRepository genrePreferenceRepository;
    private final UserProfileEmbeddingTextBuilder textBuilder;
    private final EmbeddingGenerator embeddingGenerator;
    private final UserContentPreferenceVectorRepository vectorRepository;

    public UserProfileEmbeddingResult embedAndIndex(UUID userId) {
        List<UserContentTagPreference> tagPreferences =
                tagPreferenceRepository.findAllWithTagByUserId(userId);
        List<UserContentGenrePreference> genrePreferences =
                genrePreferenceRepository.findAllWithGenreByUserId(userId);
        if (tagPreferences.isEmpty() && genrePreferences.isEmpty()) {
            throw new PreferenceNotFoundException(userId);
        }

        List<UserTagPreferenceValue> tagValues = tagPreferences.stream()
                .map(preference -> new UserTagPreferenceValue(
                        preference.getTag().getId(),
                        preference.getTag().getName(),
                        preference.getScore(),
                        preference.getScoreUpdatedAt()
                ))
                .sorted(Comparator
                        .comparingDouble(UserTagPreferenceValue::getScore)
                        .reversed()
                        .thenComparing(
                                UserTagPreferenceValue::getScoreUpdatedAt,
                                Comparator.reverseOrder()
                        )
                        .thenComparing(UserTagPreferenceValue::getTagName))
                .toList();
        List<UserGenrePreferenceValue> genreValues = genrePreferences.stream()
                .map(preference -> new UserGenrePreferenceValue(
                        preference.getGenre().getId(),
                        preference.getGenre().getName(),
                        preference.getScore(),
                        preference.getScoreUpdatedAt()
                ))
                .sorted(Comparator
                        .comparingDouble(UserGenrePreferenceValue::getScore)
                        .reversed()
                        .thenComparing(
                                UserGenrePreferenceValue::getScoreUpdatedAt,
                                Comparator.reverseOrder()
                        )
                        .thenComparing(UserGenrePreferenceValue::getGenreName))
                .toList();
        String embeddingText = textBuilder.build(genreValues, tagValues);
        float[] embedding = embeddingGenerator.embed(embeddingText);
        Instant preferenceUpdatedAt = Stream.concat(
                        tagPreferences.stream()
                                .map(UserContentTagPreference::getScoreUpdatedAt),
                        genrePreferences.stream()
                                .map(UserContentGenrePreference::getScoreUpdatedAt)
                )
                .max(Comparator.naturalOrder())
                .orElseThrow();

        UserContentPreferenceVectorDocument document = new UserContentPreferenceVectorDocument(
                userId,
                genreValues.stream()
                        .map(value -> new GenreScoreDocument(
                                value.getGenreId(),
                                value.getGenreName(),
                                value.getScore()
                        ))
                        .toList(),
                tagValues.stream()
                        .map(value -> new TagScoreDocument(
                                value.getTagId(),
                                value.getTagName(),
                                value.getScore()
                        ))
                        .toList(),
                embedding,
                embeddingGenerator.modelName(),
                preferenceUpdatedAt,
                Instant.now()
        );
        vectorRepository.upsert(document);

        return new UserProfileEmbeddingResult(
                userId,
                embeddingText,
                embedding,
                embedding.length
        );
    }
}
