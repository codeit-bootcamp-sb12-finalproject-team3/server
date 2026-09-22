package com.moduplaylist.core.playlist.repository;

import com.moduplaylist.core.playlist.entity.Playlist;
import com.moduplaylist.core.playlist.repository.PlaylistSearch.Direction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class PlaylistQueryRepositoryImpl implements PlaylistQueryRepository {

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public SearchResult search(PlaylistSearch search) {
    long totalCount = count(search);

    List<Playlist> playlists = findPlaylists(search);
    boolean hasNext = playlists.size() > search.getLimit();

    if (hasNext) {
      playlists = new ArrayList<>(
          playlists.subList(0, search.getLimit())
      );
    }

    if (playlists.isEmpty()) {
      return new SearchResult(
          List.of(),
          totalCount,
          false
      );
    }

    List<UUID> playlistIds = playlists.stream()
        .map(Playlist::getId)
        .toList();

    Map<UUID, Long> subscriberCounts = findSubscriberCounts(playlistIds);

    Map<UUID, Long> contentCounts = findContentCounts(playlistIds);

    List<Item> items = playlists.stream()
        .map(playlist -> new Item(
            playlist,
            subscriberCounts.getOrDefault(playlist.getId(), 0L),
            contentCounts.getOrDefault(playlist.getId(), 0L)
        ))
        .toList();

    return new SearchResult(
        items,
        totalCount,
        hasNext
    );
  }

  @Override
  public List<Item> findAllByIds(List<UUID> playlistIds) {
    if (playlistIds.isEmpty()) {
      return List.of();
    }

    List<Playlist> playlists = entityManager.createQuery("""
        SELECT p
        FROM Playlist p
        JOIN FETCH p.owner
        WHERE p.id IN :playlistIds
        """, Playlist.class)
        .setParameter("playlistIds", playlistIds)
        .getResultList();
    if (playlists.isEmpty()) {
      return List.of();
    }

    List<UUID> existingIds = playlists.stream().map(Playlist::getId).toList();
    Map<UUID, Long> subscriberCounts = findSubscriberCounts(existingIds);
    Map<UUID, Long> contentCounts = findContentCounts(existingIds);
    return playlists.stream()
        .map(playlist -> new Item(
            playlist,
            subscriberCounts.getOrDefault(playlist.getId(), 0L),
            contentCounts.getOrDefault(playlist.getId(), 0L)
        ))
        .toList();
  }

  private List<Playlist> findPlaylists(PlaylistSearch search) {
    StringBuilder jpql = new StringBuilder("""
        SELECT p
        FROM Playlist p
        JOIN FETCH p.owner
        WHERE 1 = 1
        """);

    Map<String, Object> parameters = new HashMap<>();

    appendFilters(jpql, parameters, search);
    appendCursor(jpql, parameters, search);
    appendOrderBy(jpql, search);

    TypedQuery<Playlist> query =
        entityManager.createQuery(jpql.toString(), Playlist.class);

    parameters.forEach(query::setParameter);

    query.setMaxResults(search.getLimit() + 1);

    return query.getResultList();
  }

  private long count(PlaylistSearch search) {
    StringBuilder jpql = new StringBuilder("""
        SELECT COUNT(p.id)
        FROM Playlist p
        WHERE 1 = 1
        """);

    Map<String, Object> parameters = new HashMap<>();

    appendFilters(jpql, parameters, search);

    TypedQuery<Long> query =
        entityManager.createQuery(jpql.toString(), Long.class);

    parameters.forEach(query::setParameter);

    return query.getSingleResult();
  }

  private void appendFilters(
      StringBuilder jpql,
      Map<String, Object> parameters,
      PlaylistSearch search
  ) {
    if (search.getOwnerIdEqual() != null) {
      jpql.append("""
        
        AND p.owner.id = :ownerId
        """);

      parameters.put(
          "ownerId",
          search.getOwnerIdEqual()
      );
    }

    if (search.getSubscriberIdEqual() != null) {
      jpql.append("""
        
        AND EXISTS (
          SELECT 1
          FROM PlaylistSubscription ps
          WHERE ps.playlist = p
            AND ps.user.id = :subscriberId
        )
        """);

      parameters.put(
          "subscriberId",
          search.getSubscriberIdEqual()
      );
    }

    if (search.getContentIdEqual() != null) {
      jpql.append("""
      
      AND EXISTS (
        SELECT 1
        FROM PlaylistContent pc
        WHERE pc.playlist = p
          AND pc.content.id = :contentId
          AND pc.content.hidden = false
      )
      """);

      parameters.put(
          "contentId",
          search.getContentIdEqual()
      );
    }
  }

  private void appendCursor(
      StringBuilder jpql,
      Map<String, Object> parameters,
      PlaylistSearch search
  ) {
    if (search.getCursorId() == null) {
      return;
    }

    String comparison =
        search.getDirection() == Direction.ASCENDING ? ">" : "<";

    String sortField = getSortField(search.getSort());

    String cursorParameter = switch (search.getSort()) {
      case CREATED_AT -> "cursorCreatedAt";
      case WEEKLY_POPULARITY_SCORE -> "cursorWeeklyPopularityScore";
    };

    Object cursorValue = switch (search.getSort()) {
      case CREATED_AT -> search.getCursorCreatedAt();
      case WEEKLY_POPULARITY_SCORE -> search.getCursorWeeklyPopularityScore();
    };

    jpql.append("""
        AND (
          %s %s :%s
            OR (
              %s = :%s
              AND p.id %s :cursorId
            )
          )
        """.formatted(
        sortField,
        comparison,
        cursorParameter,
        sortField,
        cursorParameter,
        comparison
    ));

    parameters.put(cursorParameter, cursorValue);
    parameters.put("cursorId", search.getCursorId());
  }

  private void appendOrderBy(
      StringBuilder jpql,
      PlaylistSearch search
  ) {
    String sortField = getSortField(search.getSort());

    String direction =
        search.getDirection() == Direction.ASCENDING ? "ASC" : "DESC";

    jpql.append(
        " ORDER BY "
            + sortField + " " + direction
            + ", p.id " + direction
    );
  }

  private String getSortField(PlaylistSearch.Sort sort) {
    return switch (sort) {
      case CREATED_AT -> "p.createdAt";
      case WEEKLY_POPULARITY_SCORE -> "p.weeklyPopularityScore";
    };
  }

  private Map<UUID, Long> findSubscriberCounts(List<UUID> playlistIds) {
    return findCounts("""
        SELECT ps.playlist.id, COUNT(ps.id)
        FROM PlaylistSubscription ps
        WHERE ps.playlist.id IN :playlistIds
        GROUP BY ps.playlist.id
        """,
        playlistIds
    );
  }

  private Map<UUID, Long> findContentCounts(List<UUID> playlistIds) {
    return findCounts("""
        SELECT pc.playlist.id, COUNT(pc.id)
        FROM PlaylistContent pc
        WHERE pc.playlist.id IN :playlistIds
          AND pc.content.hidden = false
        GROUP BY pc.playlist.id
        """,
        playlistIds
    );
  }

  private Map<UUID, Long> findCounts(String jpql, List<UUID> playlistIds) {
    List<Object[]> rows = entityManager
        .createQuery(jpql, Object[].class)
        .setParameter("playlistIds", playlistIds)
        .getResultList();

    Map<UUID, Long> result = new HashMap<>();

    for (Object[] row : rows) {
      result.put(
          (UUID) row[0],
          (Long) row[1]
      );
    }

    return result;
  }
}
