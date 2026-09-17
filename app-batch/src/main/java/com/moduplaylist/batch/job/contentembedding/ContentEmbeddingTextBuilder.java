package com.moduplaylist.batch.job.contentembedding;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.moduplaylist.batch.job.contentembedding.dto.ContentEmbeddingSource;
import org.springframework.stereotype.Component;

@Component
public class ContentEmbeddingTextBuilder {

    public String build(ContentEmbeddingSource source) {
        if (source == null || source.getTitle() == null || source.getTitle().isBlank()) {
            throw new IllegalArgumentException("콘텐츠 제목은 필수입니다.");
        }
        if (source.getType() == null || source.getType().isBlank()) {
            throw new IllegalArgumentException("콘텐츠 유형은 필수입니다.");
        }

        return """
                제목: %s
                콘텐츠 유형: %s
                장르: %s
                태그: %s
                설명: %s""".formatted(
                normalize(source.getTitle()),
                normalize(source.getType()),
                joinValues(source.getGenres()),
                joinValues(source.getTags()),
                normalizeOrNone(source.getDescription())
        );
    }

    private String joinValues(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return "없음";
        }

        List<String> normalizedValues = values.stream()
                .filter(Objects::nonNull)
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
        if (normalizedValues.isEmpty()) {
            return "없음";
        }
        return normalizedValues.stream().collect(Collectors.joining(", "));
    }

    private String normalizeOrNone(String value) {
        if (value == null || value.isBlank()) {
            return "없음";
        }
        return normalize(value);
    }

    private String normalize(String value) {
        return value.strip().replaceAll("\\s+", " ");
    }
}
