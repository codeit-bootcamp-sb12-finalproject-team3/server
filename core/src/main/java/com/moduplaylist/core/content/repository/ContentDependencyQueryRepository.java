package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.Content;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ContentDependencyQueryRepository {

    private final EntityManager entityManager;

    /**
     * 시리즈 삭제 시 함께 삭제될 하위 시즌을 잠그고 반환한다.
     * 호출자는 같은 트랜잭션에서 루트 콘텐츠를 먼저 잠가야 한다.
     */
    public List<Content> findChildSeasonsForUpdate(UUID parentContentId) {
        return entityManager.createQuery("""
                        select content
                        from Content content
                        where content.parentContent.id = :parentContentId
                        order by content.seasonNumber asc, content.id asc
                        """, Content.class)
                .setParameter("parentContentId", parentContentId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
    }

    /** 삭제 대상 중 하나라도 Watch Party에서 참조 중인지 확인한다. */
    public boolean existsWatchPartyByContentIdIn(Collection<UUID> contentIds) {
        if (contentIds.isEmpty()) {
            return false;
        }

        List<UUID> matchedIds = entityManager.createQuery("""
                        select watchParty.id
                        from WatchParty watchParty
                        where watchParty.contentId in :contentIds
                        """, UUID.class)
                .setParameter("contentIds", contentIds)
                .setMaxResults(1)
                .getResultList();
        return !matchedIds.isEmpty();
    }
}
