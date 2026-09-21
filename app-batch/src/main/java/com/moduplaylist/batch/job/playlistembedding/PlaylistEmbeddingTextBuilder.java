package com.moduplaylist.batch.job.playlistembedding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

        List<String> lines = new ArrayList<>();
        lines.add("플레이리스트 제목: " + normalize(title));
        addCollectionLine(lines, "장르", genres);
        addCollectionLine(lines, "태그", tags);
        addTextLine(lines, "설명", description);
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
        String names = values.stream()
                .filter(Objects::nonNull)
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
        if (!names.isEmpty()) {
            lines.add(label + ": " + names);
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
