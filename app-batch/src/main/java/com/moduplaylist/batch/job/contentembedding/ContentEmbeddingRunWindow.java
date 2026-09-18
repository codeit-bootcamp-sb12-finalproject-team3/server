package com.moduplaylist.batch.job.contentembedding;

import java.time.Instant;

public record ContentEmbeddingRunWindow(Instant after, Instant through, boolean fullScan) {
}
