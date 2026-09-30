package com.moduplaylist.infrastructure.embedding;

import java.util.Objects;
import org.springframework.ai.embedding.EmbeddingModel;

public class SpringAiEmbeddingGenerator implements EmbeddingGenerator {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingProperties properties;

    public SpringAiEmbeddingGenerator(
            EmbeddingModel embeddingModel,
            EmbeddingProperties properties
    ) {
        this.embeddingModel = Objects.requireNonNull(embeddingModel);
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("임베딩 입력 텍스트는 비어 있을 수 없습니다.");
        }

        float[] embedding = embeddingModel.embed(text);
        if (embedding.length != properties.getDimensions()) {
            throw new IllegalStateException(
                    "임베딩 차원이 일치하지 않습니다. expected=%d, actual=%d"
                            .formatted(properties.getDimensions(), embedding.length)
            );
        }
        return embedding;
    }

    @Override
    public String modelName() {
        return properties.getModel();
    }
}
