package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.SportEvent;
import com.moduplaylist.core.content.entity.SportEvent.NormalizedStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SportEventRepository extends JpaRepository<SportEvent, UUID> {

	@Query("""
		select count(sportEvent) > 0
		from SportEvent sportEvent
		where sportEvent.content.title = :title
		  and sportEvent.homeTeamName = :homeTeam
		  and sportEvent.awayTeamName = :awayTeam
		  and ((:scheduledAt is null and sportEvent.scheduledAt is null)
		    or sportEvent.scheduledAt = :scheduledAt)
		""")
	boolean existsDuplicate(
		@Param("title") String title,
		@Param("homeTeam") String homeTeam,
		@Param("awayTeam") String awayTeam,
		@Param("scheduledAt") java.time.Instant scheduledAt);

	@Query("""
		select count(sportEvent) > 0
		from SportEvent sportEvent
		where sportEvent.contentId <> :contentId
		  and sportEvent.content.title = :title
		  and sportEvent.homeTeamName = :homeTeam
		  and sportEvent.awayTeamName = :awayTeam
		  and ((:scheduledAt is null and sportEvent.scheduledAt is null)
		    or sportEvent.scheduledAt = :scheduledAt)
		""")
	boolean existsDuplicateExcluding(
		@Param("contentId") UUID contentId,
		@Param("title") String title,
		@Param("homeTeam") String homeTeam,
		@Param("awayTeam") String awayTeam,
		@Param("scheduledAt") Instant scheduledAt);

	@Query("""
		select sportEvent
		from SportEvent sportEvent
		join fetch sportEvent.sportType
		where sportEvent.contentId = :contentId
		  and sportEvent.content.hidden = false
		""")
	Optional<SportEvent> findWithSportTypeByContentId(
		@Param("contentId") UUID contentId);

	@Query("""
		select sportEvent
		from SportEvent sportEvent
		join fetch sportEvent.sportType
		where sportEvent.contentId in :contentIds
		  and sportEvent.content.hidden = false
		""")
	List<SportEvent> findAllWithSportTypeByContentIdIn(
		@Param("contentIds") Collection<UUID> contentIds);

	@Query("""
		select sportEvent
		from SportEvent sportEvent
		join fetch sportEvent.content content
		join fetch sportEvent.sportType
		where content.externalSource = :externalSource
		  and sportEvent.normalizedStatus in :statuses
		  and sportEvent.scheduledAt between :from and :to
		order by sportEvent.scheduledAt asc, sportEvent.contentId asc
		""")
	List<SportEvent> findAllBatchUpdateCandidates(
		@Param("externalSource") String externalSource,
		@Param("statuses") Collection<NormalizedStatus> statuses,
		@Param("from") Instant from,
		@Param("to") Instant to);
}
