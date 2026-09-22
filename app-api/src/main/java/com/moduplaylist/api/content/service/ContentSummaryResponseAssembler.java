package com.moduplaylist.api.content.service;

import com.moduplaylist.api.content.dto.ContentSummaryResponse;
import com.moduplaylist.api.content.dto.ContentSummaryType;
import com.moduplaylist.api.content.dto.GenreResponse;
import com.moduplaylist.api.content.dto.TagResponse;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentTag;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.entity.Genre;
import com.moduplaylist.core.content.entity.SportEvent;
import com.moduplaylist.core.content.repository.ContentGenreRepository;
import com.moduplaylist.core.content.repository.ContentLikeRepository;
import com.moduplaylist.core.content.repository.ContentTagRepository;
import com.moduplaylist.core.content.repository.SportEventRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentSummaryResponseAssembler {

	private final ContentGenreRepository contentGenreRepository;
	private final ContentTagRepository contentTagRepository;
	private final ContentLikeRepository contentLikeRepository;
	private final SportEventRepository sportEventRepository;

	public List<ContentSummaryResponse> toResponses(List<Content> contents, UUID userId) {
		if (contents.isEmpty()) {
			return List.of();
		}

		List<UUID> contentIds = contents.stream().map(Content::getId).toList();
		Map<UUID, List<GenreResponse>> genresByContentId = contentGenreRepository
			.findAllWithGenreByContentIdIn(contentIds).stream()
			.collect(Collectors.groupingBy(
				contentGenre -> contentGenre.getContent().getId(),
				Collectors.mapping(
					contentGenre -> toGenreResponse(contentGenre.getGenre()),
					Collectors.toList()
				)
			));
		Map<UUID, List<TagResponse>> tagsByContentId = contentTagRepository
			.findAllWithTagByContentIdIn(contentIds).stream()
			.collect(Collectors.groupingBy(
				contentTag -> contentTag.getContent().getId(),
				Collectors.mapping(this::toTagResponse, Collectors.toList())
			));
		Map<UUID, SportEvent> sportEventByContentId = sportEventRepository
			.findAllWithSportTypeByContentIdIn(contentIds).stream()
			.collect(Collectors.toMap(SportEvent::getContentId, Function.identity()));
		Set<UUID> likedContentIds = new HashSet<>(
			contentLikeRepository.findContentIdsByUserIdAndContentIdIn(userId, contentIds)
		);

		return contents.stream()
			.map(content -> toResponse(
				content,
				genresByContentId.getOrDefault(content.getId(), List.of()),
				tagsByContentId.getOrDefault(content.getId(), List.of()),
				sportEventByContentId.get(content.getId()),
				likedContentIds.contains(content.getId())
			))
			.toList();
	}

	private ContentSummaryResponse toResponse(
		Content content,
		List<GenreResponse> genres,
		List<TagResponse> tags,
		SportEvent sportEvent,
		boolean likedByMe
	) {
		return ContentSummaryResponse.builder()
			.id(content.getId())
			.parentContentId(content.getParentContent() == null
				? null
				: content.getParentContent().getId())
			.title(content.getTitle())
			.description(content.getDescription())
			.type(ContentSummaryType.from(content.getType()))
			.seasonNumber(content.getSeasonNumber())
			.episodeCount(content.getEpisodeCount())
			.sportType(sportEvent == null ? null : sportEvent.getSportType().getCode())
			.league(sportEvent == null ? null : sportEvent.getLeagueName())
			.homeTeam(sportEvent == null ? null : sportEvent.getHomeTeamName())
			.awayTeam(sportEvent == null ? null : sportEvent.getAwayTeamName())
			.thumbnailUrl(content.getThumbnailUrl())
			.releaseDate(content.getReleaseDate())
			.runtime(content.getType() == ContentType.MOVIE ? content.getRuntime() : null)
			.averageRating(content.getAverageRating())
			.reviewCount(content.getReviewCount())
			.likeCount(content.getLikeCount())
			.likedByMe(likedByMe)
			.genres(genres)
			.tags(tags)
			.build();
	}

	private GenreResponse toGenreResponse(Genre genre) {
		return GenreResponse.builder()
			.id(genre.getId())
			.name(genre.getName())
			.build();
	}

	private TagResponse toTagResponse(ContentTag contentTag) {
		return TagResponse.builder()
			.id(contentTag.getTag().getId())
			.name(contentTag.getTag().getName())
			.build();
	}
}
