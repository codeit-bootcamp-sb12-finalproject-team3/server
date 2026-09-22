package com.moduplaylist.batch.job.contentembedding;

import java.util.ArrayList;
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

        List<String> lines = new ArrayList<>();
        lines.add("제목: " + normalize(source.getTitle()));
        lines.add("콘텐츠 유형: " + normalize(source.getType()));
        addCollectionLine(lines, "장르", source.getGenres());
        addCollectionLine(lines, "태그", source.getTags());
        addTextLine(lines, "설명", source.getDescription());
        return String.join("\n", lines);
    }

    private void addCollectionLine(
            List<String> lines,
            String label,
            Collection<String> values
    ) {
        if (values == null || values.isEmpty()) {
            return;
        }

        String normalizedValues = values.stream()
                .filter(Objects::nonNull)
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(", "));
        if (!normalizedValues.isEmpty()) {
            lines.add(label + ": " + normalizedValues);
        }
    }

    private void addTextLine(List<String> lines, String label, String value) {
        if (value != null && !value.isBlank()) {
            lines.add(label + ": " + normalize(value));
        }
    }

    private String normalize(String value) {
        return value.strip().replaceAll("\\s+", " ");
    }
}
