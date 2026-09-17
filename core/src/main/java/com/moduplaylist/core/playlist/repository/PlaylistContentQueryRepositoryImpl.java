package com.moduplaylist.core.playlist.repository;

import com.moduplaylist.core.playlist.entity.PlaylistContent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class PlaylistContentQueryRepositoryImpl implements PlaylistContentQueryRepository {

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public List<PlaylistContent> findPreviewContents(
      List<UUID> playlistIds,
      int previewLimit
  ) {
    if (playlistIds.isEmpty()) {
      return List.of();
    }

    @SuppressWarnings("unchecked") // Native Query 결과를 PlaylistContent로 매핑하기 위한 unchecked 경고 무시
    List<PlaylistContent> previews = entityManager
        .createNativeQuery("""
        SELECT ranked.id,
               ranked.playlist_id,
               ranked.content_id,
               ranked.created_at
        FROM (
            SELECT pc.id,
                   pc.playlist_id,
                   pc.content_id,
                   pc.created_at,
                   ROW_NUMBER() OVER (
                       PARTITION BY pc.playlist_id
                       ORDER BY pc.created_at ASC, pc.id ASC
                   ) AS rn
            FROM playlist_contents pc
            WHERE pc.playlist_id IN (:playlistIds)
        ) ranked
        WHERE ranked.rn <= :previewLimit
        """,
        PlaylistContent.class
        )
        .setParameter("playlistIds", playlistIds)
        .setParameter("previewLimit", previewLimit)
        .getResultList();

    if (previews.isEmpty()) {
      return List.of();
    }

    List<UUID> previewIds = previews.stream()
        .map(PlaylistContent::getId)
        .toList();

    return entityManager.createQuery("""
        SELECT pc
        FROM PlaylistContent pc
        JOIN FETCH pc.playlist
        JOIN FETCH pc.content
        WHERE pc.id IN :previewIds
        ORDER BY pc.playlist.id ASC,
                 pc.createdAt ASC,
                 pc.id ASC
        """,
        PlaylistContent.class
        )
        .setParameter("previewIds", previewIds)
        .getResultList();
  }
}
