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
