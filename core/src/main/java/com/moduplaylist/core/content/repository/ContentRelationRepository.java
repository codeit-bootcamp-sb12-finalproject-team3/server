package com.moduplaylist.core.content.repository;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.time.Instant;
import java.sql.Timestamp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import static com.moduplaylist.core.content.repository.ContentQueryRepository.bytes;
import static com.moduplaylist.core.content.repository.ContentQueryRepository.uuid;

@Repository
@RequiredArgsConstructor
public class ContentRelationRepository {
    private final EntityManager em;
    public record Genre(UUID id, String name) {}
    public record Tag(UUID id, String name, String source) {}
    public record Cast(String name, String roleName, String profileImageUrl, int displayOrder) {}
    public record Ott(UUID id, String name, String logoUrl, String watchUrl) {}
    public record Playlist(UUID id, String title, long subscriberCount) {}
    public record WatchParty(UUID id, String title, Instant scheduledAt, String status, long reminderCount) {}

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String sql, UUID contentId) {
        return em.createNativeQuery(sql).setParameter("id", bytes(contentId)).getResultList();
    }

    public List<Genre> genres(UUID id) {
        return rows("SELECT g.id,g.name FROM genres g JOIN content_genres cg ON cg.genre_id=g.id WHERE cg.content_id=:id ORDER BY g.name,g.id", id)
                .stream().map(r -> new Genre(uuid(r[0]), (String) r[1])).toList();
    }
    public List<Tag> tags(UUID id) {
        return rows("SELECT t.id,t.name,ct.source FROM tags t JOIN content_tags ct ON ct.tag_id=t.id WHERE ct.content_id=:id ORDER BY t.name,t.id", id)
                .stream().map(r -> new Tag(uuid(r[0]), (String) r[1], (String) r[2])).toList();
    }
    public List<Cast> casts(UUID id) {
        return rows("SELECT name,role_name,profile_image_url,display_order FROM content_casts WHERE content_id=:id ORDER BY display_order", id)
                .stream().map(r -> new Cast((String) r[0], (String) r[1], (String) r[2], ((Number) r[3]).intValue())).toList();
    }
    public List<Ott> otts(UUID id) {
        return rows("SELECT o.id,o.name,o.logo_url,co.watch_url FROM ott_platforms o JOIN content_ott co ON co.ott_id=o.id WHERE co.content_id=:id ORDER BY o.name,o.id", id)
                .stream().map(r -> new Ott(uuid(r[0]), (String) r[1], (String) r[2], (String) r[3])).toList();
    }

    public List<Playlist> playlists(UUID id) {
        return rows("""
                SELECT p.id,p.title,(SELECT COUNT(*) FROM playlist_subscriptions ps WHERE ps.playlist_id=p.id) AS subscribers
                FROM playlists p JOIN playlist_contents pc ON pc.playlist_id=p.id
                WHERE pc.content_id=:id ORDER BY subscribers DESC,p.created_at ASC,p.id ASC
                """, id).stream().map(r -> new Playlist(uuid(r[0]), (String) r[1], ((Number) r[2]).longValue())).toList();
    }

    @SuppressWarnings("unchecked")
    public List<WatchParty> watchParties(UUID id, Instant now) {
        List<Object[]> values = em.createNativeQuery("""
                SELECT w.id,w.title,w.scheduled_at,w.status,
                    (SELECT COUNT(*) FROM watch_party_reminders wr WHERE wr.watch_party_id=w.id) AS reminders
                FROM watch_parties w WHERE w.content_id=:id AND w.status <> 'ENDED' AND w.ended_at IS NULL
                    AND w.scheduled_at > :cutoff
                ORDER BY CASE WHEN w.scheduled_at <= :now THEN 0 ELSE 1 END,
                    w.scheduled_at ASC,reminders DESC,w.id ASC
                """).setParameter("id", bytes(id)).setParameter("cutoff", Timestamp.from(now.minusSeconds(3600)))
                .setParameter("now", Timestamp.from(now)).getResultList();
        return values.stream().map(r -> new WatchParty(uuid(r[0]), (String) r[1],
                ((Timestamp) r[2]).toInstant(), (String) r[3], ((Number) r[4]).longValue())).toList();
    }

    /** 호출 Service에서 대상 Content를 잠가 동일 콘텐츠의 수정을 직렬화한다. */
    public void replaceManualTags(UUID contentId, List<String> names) {
        em.createNativeQuery("DELETE FROM content_tags WHERE content_id=:id AND source='MANUAL'")
                .setParameter("id", bytes(contentId)).executeUpdate();
        // 같은 태그에 AI/EXTERNAL 출처가 있으면 해당 출처를 유지한다.
        for (String name : new TreeSet<>(names)) {
            em.createNativeQuery("INSERT INTO tags(id,name) VALUES (:id,:name) ON DUPLICATE KEY UPDATE name=name")
                    .setParameter("id", bytes(UuidCreator.getTimeOrderedEpoch())).setParameter("name", name).executeUpdate();
            em.createNativeQuery("""
                    INSERT INTO content_tags(id,content_id,tag_id,source)
                    SELECT :id,:contentId,t.id,'MANUAL' FROM tags t WHERE t.name=:name
                    ON DUPLICATE KEY UPDATE source=source
                    """).setParameter("id", bytes(UuidCreator.getTimeOrderedEpoch()))
                    .setParameter("contentId", bytes(contentId)).setParameter("name", name).executeUpdate();
        }
    }

    public boolean hasWatchParty(UUID id) {
        Number count = (Number) em.createNativeQuery("SELECT COUNT(*) FROM watch_parties WHERE content_id=:id")
                .setParameter("id", bytes(id)).getSingleResult();
        return count.longValue() > 0;
    }
}
