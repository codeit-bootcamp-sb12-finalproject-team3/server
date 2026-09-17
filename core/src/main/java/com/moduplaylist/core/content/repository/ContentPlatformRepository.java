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
		order by platform.name asc, platform.id asc
		""")
	List<ContentPlatform> findAllWithPlatformByContentId(
		@Param("contentId") UUID contentId);

	Optional<ContentPlatform> findByContent_IdAndPlatform_IdAndRegionCode(
		UUID contentId,
		UUID platformId,
		String regionCode);

	@Modifying
	@Query("""
		delete from ContentPlatform contentPlatform
		where contentPlatform.content.id = :contentId
		""")
	int deleteAllByContentId(@Param("contentId") UUID contentId);
}
