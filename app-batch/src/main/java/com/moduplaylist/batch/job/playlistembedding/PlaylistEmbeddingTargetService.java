package com.moduplaylist.batch.job.playlistembedding;

import com.moduplaylist.core.playlist.entity.Playlist;
import com.moduplaylist.core.playlist.repository.PlaylistRepository;
import com.moduplaylist.infrastructure.embedding.EmbeddingGenerator;
import com.moduplaylist.infrastructure.opensearch.playlist.PlaylistVectorDocument;
import com.moduplaylist.infrastructure.opensearch.playlist.PlaylistVectorRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaylistEmbeddingTargetService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistVectorRepository vectorRepository;
    private final EmbeddingGenerator embeddingGenerator;

    public List<UUID> findTargetPlaylistIds() {
        return playlistRepository.findAll().stream()
                .sorted(Comparator.comparing(Playlist::getId))
                .filter(this::requiresEmbedding)
                .map(Playlist::getId)
                .toList();
    }

    public List<UUID> findDeletedPlaylistIds() {
        HashSet<UUID> existingPlaylistIds = new HashSet<>(playlistRepository.findAllIds());
        return vectorRepository.findAllIds().stream()
                .filter(playlistId -> !existingPlaylistIds.contains(playlistId))
                .sorted()
                .toList();
    }

    private boolean requiresEmbedding(Playlist playlist) {
        return vectorRepository.findById(playlist.getId())
                .map(document -> isOutdated(playlist, document))
                .orElse(true);
    }

    private boolean isOutdated(Playlist playlist, PlaylistVectorDocument document) {
        Instant sourceUpdatedAt = document.getSourceUpdatedAt();
        return sourceUpdatedAt == null
                || playlist.getUpdatedAt().isAfter(sourceUpdatedAt)
                || !Objects.equals(embeddingGenerator.modelName(), document.getEmbeddingModel());
    }
}
