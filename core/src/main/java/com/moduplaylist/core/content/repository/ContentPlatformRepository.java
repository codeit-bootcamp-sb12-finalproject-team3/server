package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.ContentPlatform;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentPlatformRepository extends JpaRepository<ContentPlatform, UUID> {

	@Query("""
		select contentPlatform
		from ContentPlatform contentPlatform
		join fetch contentPlatform.platform platform
		where contentPlatform.content.id = :contentId
		  and contentPlatform.content.hidden = false
		  and contentPlatform.regionCode = :regionCode
		order by platform.name asc, platform.id asc
		""")
	List<ContentPlatform> findAllWithPlatformByContentIdAndRegionCode(
		@Param("contentId") UUID contentId,
		@Param("regionCode") String regionCode);

	Optional<ContentPlatform> findByContent_IdAndPlatform_IdAndRegionCode(
		UUID contentId,
		UUID platformId,
		String regionCode);

	@Modifying
	@Query("""
		delete from ContentPlatform contentPlatform
		where contentPlatform.content.id = :contentId
		  and contentPlatform.platform.id = :platformId
		  and contentPlatform.regionCode = :regionCode
		""")
	int deleteByContentIdAndPlatformIdAndRegionCode(
		@Param("contentId") UUID contentId,
		@Param("platformId") UUID platformId,
		@Param("regionCode") String regionCode);

	@Modifying(flushAutomatically = true)
	@Query("delete from ContentPlatform contentPlatform where contentPlatform.content.id = :contentId")
	int deleteAllByContentId(@Param("contentId") UUID contentId);
}
