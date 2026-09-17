package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.TagSource;
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
    private static final int PREVIEW_FETCH_LIMIT = 21;

    private final EntityManager em;

    public enum DisplayStatus {
        LIVE,
        SCHEDULED
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
        private final TagSource source;
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
    public static class PlatformItem {
        private final UUID id;
        private final String name;
        private final String logoUrl;
        private final String url;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Playlist {
        private final UUID id;
        private final String title;
        private final String description;
        private final long subscriberCount;
        private final Instant createdAt;
    }

    @Getter
    @RequiredArgsConstructor
    public static class WatchParty {
        private final UUID id;
        private final String title;
        private final Instant scheduledAt;
        private final DisplayStatus displayStatus;
        private final int participantCount;
        private final int maxParticipants;
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> findRows(String sql, UUID contentId) {
        return em.createNativeQuery(sql).setParameter("id", bytes(contentId)).getResultList();
    }

    public List<Genre> genres(UUID id) {
        return findRows("SELECT g.id,g.name FROM contents c JOIN content_genres cg ON cg.content_id=c.id JOIN genres g ON g.id=cg.genre_id WHERE c.id=:id AND c.hidden=false ORDER BY g.name,g.id", id)
                .stream().map(r -> new Genre(uuid(r[0]), (String) r[1])).toList();
    }
    public List<Tag> tags(UUID id) {
        return findRows("SELECT t.id,t.name,ct.source FROM contents c JOIN content_tags ct ON ct.content_id=c.id JOIN tags t ON t.id=ct.tag_id WHERE c.id=:id AND c.hidden=false ORDER BY t.name,t.id", id)
                .stream().map(this::toTag).toList();
    }
    public List<Cast> casts(UUID id) {
        return findRows("SELECT cc.name,cc.role_name,cc.profile_image_url,cc.display_order FROM contents c JOIN content_casts cc ON cc.content_id=c.id WHERE c.id=:id AND c.hidden=false ORDER BY cc.display_order", id)
                .stream().map(r -> new Cast((String) r[0], (String) r[1], (String) r[2], ((Number) r[3]).intValue())).toList();
    }
    public List<PlatformItem> platforms(UUID id) {
        return findRows("SELECT p.id,p.name,p.logo_url,cp.url FROM contents c JOIN content_platforms cp ON cp.content_id=c.id JOIN platforms p ON p.id=cp.platform_id WHERE c.id=:id AND c.hidden=false AND cp.region_code='KR' ORDER BY p.name,p.id", id)
                .stream().map(r -> new PlatformItem(uuid(r[0]), (String) r[1], (String) r[2], (String) r[3])).toList();
    }

    @SuppressWarnings("unchecked")
    public List<Playlist> findTopPlaylists(UUID id) {
        List<Object[]> values = em.createNativeQuery("""
                SELECT p.id,p.title,p.description,
                    (SELECT COUNT(*) FROM playlist_subscriptions ps WHERE ps.playlist_id=p.id) AS subscribers,
                    p.created_at
                FROM contents c
                JOIN playlist_contents pc ON pc.content_id=c.id
                JOIN playlists p ON p.id=pc.playlist_id
                WHERE c.id=:id AND c.hidden=false
                ORDER BY subscribers DESC,p.created_at DESC,p.id ASC
                """).setParameter("id", bytes(id)).setMaxResults(PREVIEW_FETCH_LIMIT).getResultList();
        return values.stream().map(this::toPlaylist).toList();
    }

    @SuppressWarnings("unchecked")
    public List<WatchParty> findWatchParties(UUID id, Instant cutoff) {
        List<Object[]> values = em.createNativeQuery("""
                SELECT w.id,w.title,w.scheduled_at,
                    w.status AS display_status,
                    (SELECT COUNT(*) FROM watch_party_participants participant
                        WHERE participant.watch_party_id=w.id AND participant.status='JOINED') AS participants,
                    w.max_participants,
                    (SELECT COUNT(*) FROM watch_party_reminders wr WHERE wr.watch_party_id=w.id) AS reminders
                FROM contents c JOIN watch_parties w ON w.content_id=c.id
                WHERE c.id=:id AND c.hidden=false AND w.status <> 'ENDED'
                    AND w.scheduled_at >= :cutoff
                ORDER BY CASE WHEN w.status='LIVE' THEN 0 ELSE 1 END,
                    CASE WHEN w.status='LIVE' THEN w.scheduled_at END DESC,
                    CASE WHEN w.status='SCHEDULED' THEN w.scheduled_at END ASC,
                    reminders DESC,w.id ASC
                """).setParameter("id", bytes(id)).setParameter("cutoff", Timestamp.from(cutoff))
                .setMaxResults(PREVIEW_FETCH_LIMIT)
                .getResultList();
        return values.stream().map(this::toWatchParty).toList();
    }

    private Tag toTag(Object[] row) {
        return new Tag(uuid(row[0]), (String) row[1], TagSource.valueOf((String) row[2]));
    }

    private Playlist toPlaylist(Object[] row) {
        return new Playlist(
                uuid(row[0]),
                (String) row[1],
                (String) row[2],
                ((Number) row[3]).longValue(),
                ((Timestamp) row[4]).toInstant());
    }

    private WatchParty toWatchParty(Object[] row) {
        return new WatchParty(
                uuid(row[0]),
                (String) row[1],
                ((Timestamp) row[2]).toInstant(),
                DisplayStatus.valueOf((String) row[3]),
                ((Number) row[4]).intValue(),
                ((Number) row[5]).intValue());
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
