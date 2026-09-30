package com.moduplaylist.infrastructure.recommendation.embedding;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.moduplaylist.infrastructure.recommendation.embedding.dto.UserGenrePreferenceValue;
import com.moduplaylist.infrastructure.recommendation.embedding.dto.UserTagPreferenceValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
public class UserProfileEmbeddingTextBuilder {

    private static final int CORE_GENRE_LIMIT = 3;
    private static final int INTEREST_GENRE_LIMIT = 5;
    private static final int CORE_TAG_LIMIT = 5;
    private static final int INTEREST_TAG_LIMIT = 10;

    private static final Comparator<NamedPreference> PREFERENCE_ORDER = Comparator
            .comparingDouble(NamedPreference::getScore)
            .reversed()
            .thenComparing(NamedPreference::getScoreUpdatedAt, Comparator.reverseOrder())
            .thenComparing(NamedPreference::getName);

    public String build(
            List<UserGenrePreferenceValue> genrePreferences,
            List<UserTagPreferenceValue> tagPreferences
    ) {
        Objects.requireNonNull(genrePreferences, "genrePreferences must not be null");
        Objects.requireNonNull(tagPreferences, "tagPreferences must not be null");

        List<NamedPreference> genres = genrePreferences.stream()
                .peek(this::validate)
                .map(preference -> new NamedPreference(
                        preference.getGenreName(),
                        preference.getScore(),
                        preference.getScoreUpdatedAt()
                ))
                .toList();
        List<NamedPreference> tags = tagPreferences.stream()
                .peek(this::validate)
                .map(preference -> new NamedPreference(
                        preference.getTagName(),
                        preference.getScore(),
                        preference.getScoreUpdatedAt()
                ))
                .toList();

        List<String> lines = new ArrayList<>();
        addPositivePreferenceLines(
                lines,
                "장르",
                genres,
                CORE_GENRE_LIMIT,
                INTEREST_GENRE_LIMIT
        );
        addPositivePreferenceLines(
                lines,
                "태그",
                tags,
                CORE_TAG_LIMIT,
                INTEREST_TAG_LIMIT
        );
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("사용자 프로필 임베딩에 사용할 양수 선호 정보가 없습니다.");
        }
        return String.join("\n", lines);
    }

    private void addPositivePreferenceLines(
            List<String> lines,
            String subject,
            List<NamedPreference> preferences,
            int coreLimit,
            int interestLimit
    ) {
        List<NamedPreference> positivePreferences = preferences.stream()
                .filter(preference -> preference.getScore() > 0.0)
                .sorted(PREFERENCE_ORDER)
                .limit((long) coreLimit + interestLimit)
                .toList();

        addLine(lines, "핵심 선호 " + subject, positivePreferences, 0, coreLimit);
        addLine(
                lines,
                "관심 " + subject,
                positivePreferences,
                coreLimit,
                coreLimit + interestLimit
        );
    }

    private void addLine(
            List<String> lines,
            String label,
            List<NamedPreference> preferences,
            int fromIndex,
            int toIndex
    ) {
        if (fromIndex >= preferences.size()) {
            return;
        }

        int endIndex = Math.min(toIndex, preferences.size());
        String names = preferences.subList(fromIndex, endIndex).stream()
                .map(NamedPreference::getName)
                .collect(Collectors.joining(", "));
        lines.add(label + ": " + names);
    }

    private void validate(UserGenrePreferenceValue preference) {
        if (preference == null
                || preference.getGenreId() == null
                || preference.getGenreName() == null
                || preference.getGenreName().isBlank()
                || !Double.isFinite(preference.getScore())
                || preference.getScoreUpdatedAt() == null) {
            throw new IllegalArgumentException("유효하지 않은 사용자 장르 선호 정보입니다.");
        }
    }

    private void validate(UserTagPreferenceValue preference) {
        if (preference == null
                || preference.getTagId() == null
                || preference.getTagName() == null
                || preference.getTagName().isBlank()
                || !Double.isFinite(preference.getScore())
                || preference.getScoreUpdatedAt() == null) {
            throw new IllegalArgumentException("유효하지 않은 사용자 태그 선호 정보입니다.");
        }
    }

    @Getter
    @AllArgsConstructor
    private static class NamedPreference {

        private String name;
        private double score;
        private Instant scoreUpdatedAt;
    }
}
