package com.moduplaylist.infrastructure.embedding;

public interface EmbeddingGenerator {

    float[] embed(String text);

    String modelName();
}
