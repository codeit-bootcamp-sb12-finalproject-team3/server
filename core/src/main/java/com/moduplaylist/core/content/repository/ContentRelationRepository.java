package com.moduplaylist.core.content.repository;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.EntityManager;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ContentRelationRepository {
    private static final int RELATED_PLAYLIST_LIMIT = 20;

    private final EntityManager em;

    public enum Source {
        AI,
        MANUAL,
        EXTERNAL
    }

    @Getter
    @RequiredArgsConstructor
    public static class Genre {
        private final UUID id;
        private final String name;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Tag {
        private final UUID id;
        private final String name;
        private final Source source;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Cast {
        private final String name;
        private final String roleName;
        private final String profileImageUrl;
        private final int displayOrder;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Ott {
        private final UUID id;
        private final String name;
        private final String logoUrl;
        private final String watchUrl;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Playlist {
        private final UUID id;
        private final String title;
        private final long subscriberCount;
    }

    @Getter
    @RequiredArgsConstructor
    public static class WatchParty {
        private final UUID id;
        private final String title;
        private final Instant scheduledAt;
        private final String status;
        private final long reminderCount;
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> findRows(String sql, UUID contentId) {
        return em.createNativeQuery(sql).setParameter("id", bytes(contentId)).getResultList();
    }

    public List<Genre> genres(UUID id) {
        return findRows("SELECT g.id,g.name FROM genres g JOIN content_genres cg ON cg.genre_id=g.id WHERE cg.content_id=:id ORDER BY g.name,g.id", id)
                .stream().map(r -> new Genre(uuid(r[0]), (String) r[1])).toList();
    }
    public List<Tag> tags(UUID id) {
        return findRows("SELECT t.id,t.name,ct.source FROM tags t JOIN content_tags ct ON ct.tag_id=t.id WHERE ct.content_id=:id ORDER BY t.name,t.id", id)
                .stream().map(this::toTag).toList();
    }
    public List<Cast> casts(UUID id) {
        return findRows("SELECT name,role_name,profile_image_url,display_order FROM content_casts WHERE content_id=:id ORDER BY display_order", id)
                .stream().map(r -> new Cast((String) r[0], (String) r[1], (String) r[2], ((Number) r[3]).intValue())).toList();
    }
    public List<Ott> otts(UUID id) {
        return findRows("SELECT o.id,o.name,o.logo_url,co.watch_url FROM ott_platforms o JOIN content_ott co ON co.ott_id=o.id WHERE co.content_id=:id ORDER BY o.name,o.id", id)
                .stream().map(r -> new Ott(uuid(r[0]), (String) r[1], (String) r[2], (String) r[3])).toList();
    }

    @SuppressWarnings("unchecked")
    public List<Playlist> findTopPlaylists(UUID id) {
        List<Object[]> values = em.createNativeQuery("""
                SELECT p.id,p.title,(SELECT COUNT(*) FROM playlist_subscriptions ps WHERE ps.playlist_id=p.id) AS subscribers
                FROM playlists p JOIN playlist_contents pc ON pc.playlist_id=p.id
                WHERE pc.content_id=:id ORDER BY subscribers DESC,p.created_at ASC,p.id ASC
                """).setParameter("id", bytes(id)).setMaxResults(RELATED_PLAYLIST_LIMIT).getResultList();
        return values.stream().map(this::toPlaylist).toList();
    }

    @SuppressWarnings("unchecked")
    public List<WatchParty> findWatchParties(UUID id, Instant now, Instant cutoff) {
        List<Object[]> values = em.createNativeQuery("""
                SELECT w.id,w.title,w.scheduled_at,w.status,
                    (SELECT COUNT(*) FROM watch_party_reminders wr WHERE wr.watch_party_id=w.id) AS reminders
                FROM watch_parties w WHERE w.content_id=:id AND w.status <> 'ENDED' AND w.ended_at IS NULL
                    AND w.scheduled_at > :cutoff
                ORDER BY CASE WHEN w.scheduled_at <= :now THEN 0 ELSE 1 END,
                    CASE WHEN w.scheduled_at <= :now THEN w.scheduled_at END DESC,
                    CASE WHEN w.scheduled_at > :now THEN w.scheduled_at END ASC,
                    reminders DESC,w.id ASC
                """).setParameter("id", bytes(id)).setParameter("cutoff", Timestamp.from(cutoff))
                .setParameter("now", Timestamp.from(now)).getResultList();
        return values.stream().map(this::toWatchParty).toList();
    }

    /** 호출 Service에서 대상 Content를 잠가 동일 콘텐츠의 수정을 직렬화한다. */
    public void replaceManualTags(UUID contentId, List<String> names) {
        deleteTagsBySource(contentId, Source.MANUAL);
        for (String name : names) {
            upsertTag(name);
            upsertContentTag(contentId, name, Source.MANUAL);
        }
    }

    public boolean hasWatchParty(UUID id) {
        Number exists = (Number) em.createNativeQuery("""
                SELECT EXISTS (SELECT 1 FROM watch_parties WHERE content_id=:id)
                """)
                .setParameter("id", bytes(id)).getSingleResult();
        return exists.intValue() == 1;
    }

    private Tag toTag(Object[] row) {
        return new Tag(uuid(row[0]), (String) row[1], Source.valueOf((String) row[2]));
    }

    private Playlist toPlaylist(Object[] row) {
        return new Playlist(uuid(row[0]), (String) row[1], ((Number) row[2]).longValue());
    }

    private WatchParty toWatchParty(Object[] row) {
        return new WatchParty(uuid(row[0]), (String) row[1], ((Timestamp) row[2]).toInstant(),
                (String) row[3], ((Number) row[4]).longValue());
    }

    private void deleteTagsBySource(UUID contentId, Source source) {
        em.createNativeQuery("DELETE FROM content_tags WHERE content_id=:id AND source=:source")
                .setParameter("id", bytes(contentId)).setParameter("source", source.name()).executeUpdate();
    }

    private void upsertTag(String name) {
        em.createNativeQuery("INSERT INTO tags(id,name) VALUES (:id,:name) ON DUPLICATE KEY UPDATE name=name")
                .setParameter("id", bytes(UuidCreator.getTimeOrderedEpoch()))
                .setParameter("name", name).executeUpdate();
    }

    private void upsertContentTag(UUID contentId, String name, Source source) {
        em.createNativeQuery("""
                INSERT INTO content_tags(id,content_id,tag_id,source)
                SELECT :id,:contentId,t.id,:source FROM tags t WHERE t.name=:name
                ON DUPLICATE KEY UPDATE source=CASE
                    WHEN source='AI' THEN 'AI'
                    WHEN source='MANUAL' AND :source='EXTERNAL' THEN 'MANUAL'
                    ELSE :source
                END
                """).setParameter("id", bytes(UuidCreator.getTimeOrderedEpoch()))
                .setParameter("contentId", bytes(contentId)).setParameter("name", name)
                .setParameter("source", source.name()).executeUpdate();
    }

    private byte[] bytes(UUID id) {
        return ByteBuffer.allocate(16).putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits()).array();
    }

    private UUID uuid(Object value) {
        if (value instanceof UUID id) return id;
        if (!(value instanceof byte[] bytes) || bytes.length != 16) {
            throw new IllegalArgumentException("BINARY(16) UUID 값이 아닙니다.");
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }

}
