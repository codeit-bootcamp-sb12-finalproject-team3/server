package com.moduplaylist.infrastructure.opensearch.playlist;

import java.util.UUID;

public record PlaylistSimilarityCandidate(UUID playlistId, double score) {
}
