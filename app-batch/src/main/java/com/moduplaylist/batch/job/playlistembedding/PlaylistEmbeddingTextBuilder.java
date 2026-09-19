package com.moduplaylist.batch.job.playlistembedding;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PlaylistEmbeddingTextBuilder {

    public String build(
            String title,
            String description,
            Collection<String> genres,
            Collection<String> tags
    ) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("플레이리스트 제목은 필수입니다.");
        }

        return """
                플레이리스트 제목: %s
                장르: %s
                태그: %s
                설명: %s""".formatted(
                normalize(title),
                joinValues(genres),
                joinValues(tags),
                description == null || description.isBlank() ? "없음" : normalize(description)
        );
    }

    private String joinValues(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return "없음";
        }
        String names = values.stream()
                .filter(Objects::nonNull)
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
        return names.isEmpty() ? "없음" : names;
    }

    private String normalize(String value) {
        return value.strip().replaceAll("\\s+", " ");
    }
}
