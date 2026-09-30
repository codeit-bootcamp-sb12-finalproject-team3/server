package com.moduplaylist.batch.job.playlistembedding;

import com.moduplaylist.core.content.entity.Genre;
import com.moduplaylist.core.content.entity.Tag;
import com.moduplaylist.core.playlist.entity.Playlist;
import com.moduplaylist.core.playlist.entity.PlaylistGenre;
import com.moduplaylist.core.playlist.entity.PlaylistTag;
import com.moduplaylist.core.playlist.exception.PlaylistNotFoundException;
import com.moduplaylist.core.playlist.repository.PlaylistGenreRepository;
import com.moduplaylist.core.playlist.repository.PlaylistRepository;
import com.moduplaylist.core.playlist.repository.PlaylistTagRepository;
import com.moduplaylist.infrastructure.embedding.EmbeddingGenerator;
import com.moduplaylist.infrastructure.opensearch.playlist.PlaylistVectorDocument;
import com.moduplaylist.infrastructure.opensearch.playlist.PlaylistVectorRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaylistEmbeddingService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistGenreRepository playlistGenreRepository;
    private final PlaylistTagRepository playlistTagRepository;
    private final PlaylistEmbeddingTextBuilder textBuilder;
    private final EmbeddingGenerator embeddingGenerator;
    private final PlaylistVectorRepository vectorRepository;

    public void embedAndIndex(UUID playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new PlaylistNotFoundException(playlistId));
        Optional<PlaylistVectorDocument> existingDocument = vectorRepository.findById(playlistId);
        List<String> genres = existingDocument
                .map(PlaylistVectorDocument::getGenres)
                .map(List::copyOf)
                .orElseGet(() -> findCurrentGenres(playlistId));
        List<String> tags = existingDocument
                .map(PlaylistVectorDocument::getTags)
                .map(List::copyOf)
                .orElseGet(() -> findCurrentTags(playlistId));

        String embeddingText = textBuilder.build(
                playlist.getTitle(), playlist.getDescription(), genres, tags
        );
        float[] embedding = embeddingGenerator.embed(embeddingText);
        PlaylistVectorDocument document = new PlaylistVectorDocument(
                playlistId,
                playlist.getOwner().getId(),
                playlist.getTitle(),
                playlist.getDescription(),
                genres,
                tags,
                embedding,
                embeddingGenerator.modelName(),
                playlist.getUpdatedAt(),
                Instant.now()
        );
        vectorRepository.upsert(document);
    }

    private List<String> findCurrentGenres(UUID playlistId) {
        return playlistGenreRepository.findAllWithGenreByPlaylistId(playlistId)
                .stream()
                .map(PlaylistGenre::getGenre)
                .map(Genre::getName)
                .distinct()
                .sorted()
                .toList();
    }

    private List<String> findCurrentTags(UUID playlistId) {
        return playlistTagRepository.findAllWithTagByPlaylistId(playlistId)
                .stream()
                .map(PlaylistTag::getTag)
                .map(Tag::getName)
                .distinct()
                .sorted()
                .toList();
    }

    public void deleteFromIndex(UUID playlistId) {
        vectorRepository.deleteById(playlistId);
    }
}
