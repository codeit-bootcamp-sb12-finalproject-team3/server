package com.moduplaylist.infrastructure.opensearch.content;

import java.util.UUID;

public record ContentSimilarityCandidate(UUID contentId, double score) {
}
