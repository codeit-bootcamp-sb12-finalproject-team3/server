package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
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
     * 부모 시리즈의 가시성은 자식 시즌의 가시성으로 결정된다.
     *
     * <p>일반 조회는 REPEATABLE READ 스냅샷을 재사용할 수 있으므로, 부모 행을 잠근 뒤에도
     * 동시에 완료된 다른 시즌 변경을 놓칠 수 있다. 마지막 노출 시즌 여부를 판단할 때는
     * locking read로 최신 상태를 읽고 같은 시리즈의 시즌 변경을 직렬화한다.</p>
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

    /** 예약·진행 중인 Watch Party가 해당 시즌 회차를 포함하는지 확인한다. */
    public boolean existsActiveWatchPartyForEpisode(UUID seasonId, Integer episodeNumber) {
        List<UUID> matchedIds = entityManager.createQuery("""
                        select watchParty.id
                        from WatchParty watchParty
                        where watchParty.contentId = :seasonId
                          and watchParty.status in :statuses
                          and (
                              (watchParty.startEpisode is null and watchParty.endEpisode is null)
                              or (
                                  watchParty.startEpisode <= :episodeNumber
                                  and watchParty.endEpisode >= :episodeNumber
                              )
                          )
                        """, UUID.class)
                .setParameter("seasonId", seasonId)
                .setParameter("statuses", List.of(
                        WatchPartyStatus.SCHEDULED,
                        WatchPartyStatus.LIVE))
                .setParameter("episodeNumber", episodeNumber)
                .setMaxResults(1)
                .getResultList();
        return !matchedIds.isEmpty();
    }
}
