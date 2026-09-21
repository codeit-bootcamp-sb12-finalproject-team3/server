package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ContentDependencyQueryRepository {

    private final EntityManager entityManager;

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
