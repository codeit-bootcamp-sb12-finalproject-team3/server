package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.TagSource;
import jakarta.persistence.EntityManager;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ContentRelationRepository {
    private final EntityManager em;

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

    @SuppressWarnings("unchecked")
    private List<Object[]> findRows(String sql, UUID contentId) {
        return em.createNativeQuery(sql).setParameter("id", bytes(contentId)).getResultList();
    }

    public List<Genre> genres(UUID id) {
        return findRows("SELECT g.id,g.name FROM contents c JOIN content_genres cg ON cg.content_id=c.id JOIN genres g ON g.id=cg.genre_id WHERE c.id=:id AND c.hidden=false ORDER BY g.name,g.id", id)
                .stream().map(r -> new Genre(uuid(r[0]), (String) r[1])).toList();
    }
    public List<Genre> genresIncludingHidden(UUID id) {
        return findRows("SELECT g.id,g.name FROM contents c JOIN content_genres cg ON cg.content_id=c.id JOIN genres g ON g.id=cg.genre_id WHERE c.id=:id ORDER BY g.name,g.id", id)
                .stream().map(r -> new Genre(uuid(r[0]), (String) r[1])).toList();
    }
    public List<Tag> tags(UUID id) {
        return findRows("SELECT t.id,t.name,ct.source FROM contents c JOIN content_tags ct ON ct.content_id=c.id JOIN tags t ON t.id=ct.tag_id WHERE c.id=:id AND c.hidden=false ORDER BY t.name,t.id", id)
                .stream().map(this::toTag).toList();
    }
    public List<Tag> tagsIncludingHidden(UUID id) {
        return findRows("SELECT t.id,t.name,ct.source FROM contents c JOIN content_tags ct ON ct.content_id=c.id JOIN tags t ON t.id=ct.tag_id WHERE c.id=:id ORDER BY t.name,t.id", id)
                .stream().map(this::toTag).toList();
    }
    public List<Cast> casts(UUID id) {
        return findRows("SELECT cc.name,cc.role_name,cc.profile_image_url,cc.display_order FROM contents c JOIN content_casts cc ON cc.content_id=c.id WHERE c.id=:id AND c.hidden=false ORDER BY cc.display_order", id)
                .stream().map(r -> new Cast((String) r[0], (String) r[1], (String) r[2], ((Number) r[3]).intValue())).toList();
    }
    public List<Cast> castsIncludingHidden(UUID id) {
        return findRows("SELECT cc.name,cc.role_name,cc.profile_image_url,cc.display_order FROM contents c JOIN content_casts cc ON cc.content_id=c.id WHERE c.id=:id ORDER BY cc.display_order", id)
                .stream().map(r -> new Cast((String) r[0], (String) r[1], (String) r[2], ((Number) r[3]).intValue())).toList();
    }
    public List<PlatformItem> platforms(UUID id) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("SELECT p.id,p.name,p.logo_url,cp.url FROM contents c JOIN content_platforms cp ON cp.content_id=c.id JOIN platforms p ON p.id=cp.platform_id WHERE c.id=:id AND c.hidden=false AND cp.region_code='KR' ORDER BY p.name,p.id")
                .setParameter("id", bytes(id))
                .getResultList();
        return rows
                .stream().map(r -> new PlatformItem(uuid(r[0]), (String) r[1], (String) r[2], (String) r[3])).toList();
    }
    public List<PlatformItem> platformsIncludingHidden(UUID id) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("SELECT p.id,p.name,p.logo_url,cp.url FROM contents c JOIN content_platforms cp ON cp.content_id=c.id JOIN platforms p ON p.id=cp.platform_id WHERE c.id=:id AND cp.region_code='KR' ORDER BY p.name,p.id")
                .setParameter("id", bytes(id))
                .getResultList();
        return rows
                .stream().map(r -> new PlatformItem(uuid(r[0]), (String) r[1], (String) r[2], (String) r[3])).toList();
    }

    private Tag toTag(Object[] row) {
        return new Tag(uuid(row[0]), (String) row[1], TagSource.valueOf((String) row[2]));
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
