package com.moduplaylist.infrastructure.recommendation.embedding;

import com.moduplaylist.core.recommendation.entity.UserPlaylistGenrePreference;
import com.moduplaylist.core.recommendation.entity.UserPlaylistTagPreference;
import com.moduplaylist.core.recommendation.exception.PreferenceNotFoundException;
import com.moduplaylist.core.recommendation.repository.UserPlaylistGenrePreferenceRepository;
import com.moduplaylist.core.recommendation.repository.UserPlaylistTagPreferenceRepository;
import com.moduplaylist.infrastructure.embedding.EmbeddingGenerator;
import com.moduplaylist.infrastructure.opensearch.recommendation.GenreScoreDocument;
import com.moduplaylist.infrastructure.opensearch.recommendation.TagScoreDocument;
import com.moduplaylist.infrastructure.opensearch.recommendation.UserPlaylistPreferenceVectorRepository;
import com.moduplaylist.infrastructure.opensearch.recommendation.UserPlaylistPreferenceVectorDocument;
import com.moduplaylist.infrastructure.recommendation.embedding.dto.UserGenrePreferenceValue;
import com.moduplaylist.infrastructure.recommendation.embedding.dto.UserProfileEmbeddingResult;
import com.moduplaylist.infrastructure.recommendation.embedding.dto.UserTagPreferenceValue;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mopl.embedding", name = "enabled", havingValue = "true")
public class UserPlaylistProfileEmbeddingService {

    private final UserPlaylistTagPreferenceRepository tagPreferenceRepository;
    private final UserPlaylistGenrePreferenceRepository genrePreferenceRepository;
    private final UserProfileEmbeddingTextBuilder textBuilder;
    private final EmbeddingGenerator embeddingGenerator;
    private final UserPlaylistPreferenceVectorRepository vectorRepository;

    public UserProfileEmbeddingResult embedAndIndex(UUID userId) {
        List<UserPlaylistTagPreference> tagPreferences =
                tagPreferenceRepository.findAllWithTagByUserId(userId);
        List<UserPlaylistGenrePreference> genrePreferences =
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
                .toList();
        List<UserGenrePreferenceValue> genreValues = genrePreferences.stream()
                .map(preference -> new UserGenrePreferenceValue(
                        preference.getGenre().getId(),
                        preference.getGenre().getName(),
                        preference.getScore(),
                        preference.getScoreUpdatedAt()
                ))
                .toList();

        String embeddingText = textBuilder.build(genreValues, tagValues);
        float[] embedding = embeddingGenerator.embed(embeddingText);
        Instant preferenceUpdatedAt = Stream.concat(
                        tagPreferences.stream()
                                .map(UserPlaylistTagPreference::getScoreUpdatedAt),
                        genrePreferences.stream()
                                .map(UserPlaylistGenrePreference::getScoreUpdatedAt)
                )
                .max(Comparator.naturalOrder())
                .orElseThrow();

        UserPlaylistPreferenceVectorDocument document = new UserPlaylistPreferenceVectorDocument(
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
