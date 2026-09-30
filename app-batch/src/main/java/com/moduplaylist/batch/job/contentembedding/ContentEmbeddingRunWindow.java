package com.moduplaylist.batch.job.contentembedding;

import java.time.Instant;

public record ContentEmbeddingRunWindow(Instant through, boolean fullScan) {
}
