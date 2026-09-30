package com.moduplaylist.core.recommendation.repository;

import com.moduplaylist.core.recommendation.entity.UserContentTagPreference;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserContentTagPreferenceRepository
        extends JpaRepository<UserContentTagPreference, UUID> {

    Optional<UserContentTagPreference> findByUser_IdAndTag_Id(UUID userId, UUID tagId);

    List<UserContentTagPreference> findAllByUser_Id(UUID userId);

    @Query("""
            select preference
            from UserContentTagPreference preference
            join fetch preference.tag
            where preference.user.id = :userId
              and preference.tag.id in :tagIds
            """)
    List<UserContentTagPreference> findAllWithTagByUserIdAndTagIdIn(
            @Param("userId") UUID userId,
            @Param("tagIds") Collection<UUID> tagIds
    );

    @Query("select distinct preference.user.id from UserContentTagPreference preference")
    List<UUID> findDistinctUserIds();

    @Query("""
            select distinct preference.user.id
            from UserContentTagPreference preference
            where preference.score > 0
            """)
    List<UUID> findDistinctUserIdsWithPositiveScore();

    @Query("""
            select preference
            from UserContentTagPreference preference
            join fetch preference.tag
            where preference.user.id = :userId
            """)
    List<UserContentTagPreference> findAllWithTagByUserId(@Param("userId") UUID userId);
}
