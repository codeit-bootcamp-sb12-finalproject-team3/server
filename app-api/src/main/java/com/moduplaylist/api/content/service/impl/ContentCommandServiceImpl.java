package com.moduplaylist.api.content.service.impl;

import com.moduplaylist.api.content.dto.ContentCastRequest;
import com.moduplaylist.api.content.dto.ContentCreateRequest;
import com.moduplaylist.api.content.dto.ContentCreateResponse;
import com.moduplaylist.api.content.dto.ContentResponse;
import com.moduplaylist.api.content.dto.ContentUpdateRequest;
import com.moduplaylist.api.content.service.ContentCommandService;
import com.moduplaylist.api.content.service.ContentQueryService;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentCast;
import com.moduplaylist.core.content.entity.ContentGenre;
import com.moduplaylist.core.content.entity.ContentTag;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.entity.Genre;
import com.moduplaylist.core.content.entity.SportEvent;
import com.moduplaylist.core.content.entity.SportType;
import com.moduplaylist.core.content.entity.Tag;
import com.moduplaylist.core.content.entity.TagSource;
import com.moduplaylist.core.content.exception.ContentDeletionBlockedException;
import com.moduplaylist.core.content.exception.ContentNotFoundException;
import com.moduplaylist.core.content.exception.ContentSeasonAlreadyExistsException;
import com.moduplaylist.core.content.exception.ContentStorageUnavailableException;
import com.moduplaylist.core.content.exception.GenreNotFoundException;
import com.moduplaylist.core.content.exception.InvalidContentSearchException;
import com.moduplaylist.core.content.repository.ContentCastRepository;
import com.moduplaylist.core.content.repository.ContentDependencyQueryRepository;
import com.moduplaylist.core.content.repository.ContentGenreRepository;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.content.repository.ContentTagRepository;
import com.moduplaylist.core.content.repository.GenreRepository;
import com.moduplaylist.core.content.repository.SportEventRepository;
import com.moduplaylist.core.content.repository.SportTypeRepository;
import com.moduplaylist.core.content.repository.TagRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ContentCommandServiceImpl implements ContentCommandService {

	private final ContentRepository contentRepository;
	private final GenreRepository genreRepository;
	private final ContentGenreRepository contentGenreRepository;
	private final TagRepository tagRepository;
	private final ContentTagRepository contentTagRepository;
	private final ContentCastRepository contentCastRepository;
	private final SportTypeRepository sportTypeRepository;
	private final SportEventRepository sportEventRepository;
	private final ContentDependencyQueryRepository dependencyQueryRepository;
	private final ContentQueryService contentQueryService;

	@Override
	@Transactional
	public ContentCreateResponse create(ContentCreateRequest request, MultipartFile thumbnail) {
		requireImageStorageAbsent(thumbnail);
		validateCreateRequest(request);
		Content content = createContent(request);
		contentRepository.saveAndFlush(content);

		if (content.getType() == ContentType.MOVIE || content.getType() == ContentType.TV_SEASON) {
			replaceRelations(content, request.getGenreIds(), request.getManualTags(), request.getCasts());
		}
		if (content.getType() == ContentType.SPORT) {
			createSportEvent(content, request);
		}

		return ContentCreateResponse.builder()
			.seriesId(content.getType() == ContentType.TV_SERIES ? content.getId() : null)
			.contentIds(content.getType() == ContentType.TV_SERIES ? List.of() : List.of(content.getId()))
			.createdAt(content.getCreatedAt())
			.build();
	}

	private void validateCreateRequest(ContentCreateRequest request) {
		if (request.getType() == ContentType.TV_SERIES) {
			return;
		}
		if (request.getDescription() == null || request.getDescription().isBlank()) {
			throw new InvalidContentSearchException();
		}
		if (request.getType() == ContentType.MOVIE || request.getType() == ContentType.TV_SEASON) {
			if (request.getGenreIds() == null || request.getGenreIds().isEmpty()) {
				throw new InvalidContentSearchException();
			}
		}
		if (request.getType() == ContentType.SPORT
			&& (request.getSportType() == null || request.getHomeTeam() == null
			|| request.getAwayTeam() == null)) {
			throw new InvalidContentSearchException();
		}
	}

	@Override
	@Transactional
	public ContentResponse update(
		UUID contentId,
		ContentUpdateRequest request,
		MultipartFile thumbnail
	) {
		requireImageStorageAbsent(thumbnail);
		Content content = contentRepository.findByIdForUpdate(contentId)
			.orElseThrow(() -> new ContentNotFoundException(contentId));

		String title = value(request.getTitle(), content.getTitle());
		String description = value(request.getDescription(), content.getDescription());
		content.updateCommonDetails(
			title,
			description,
			value(request.getReleaseDate(), content.getReleaseDate()),
			value(request.getMetadata(), content.getMetadata())
		);
		if (content.getType() == ContentType.MOVIE && request.getRuntime().isPresent()) {
			content.updateMovieDetails(request.getRuntime().orElse(null));
		}
		if (content.getType() == ContentType.TV_SEASON) {
			Content parent = content.getParentContent();
			if (request.getParentContentId().isPresent()) {
				UUID parentId = request.getParentContentId().orElse(null);
				parent = requireSeries(parentId);
			}
			Integer seasonNumber = value(request.getSeasonNumber(), content.getSeasonNumber());
			if (contentRepository.existsByParentContent_IdAndSeasonNumberAndIdNot(
				parent.getId(), seasonNumber, contentId)) {
				throw new ContentSeasonAlreadyExistsException(parent.getId(), seasonNumber);
			}
			content.updateTvSeasonDetails(
				parent,
				seasonNumber,
				value(request.getEpisodeCount(), content.getEpisodeCount()),
				null
			);
		}

		if (request.getGenreIds().isPresent() || request.getManualTags().isPresent()
			|| request.getCasts().isPresent()) {
			if (content.getType() != ContentType.MOVIE && content.getType() != ContentType.TV_SEASON) {
				throw new InvalidContentSearchException();
			}
			if (request.getGenreIds().isPresent()) {
				replaceGenres(content, request.getGenreIds().orElse(List.of()));
			}
			if (request.getManualTags().isPresent()) {
				replaceTags(content, request.getManualTags().orElse(List.of()));
			}
			if (request.getCasts().isPresent()) {
				replaceCasts(content, request.getCasts().orElse(List.of()));
			}
		}
		contentRepository.flush();
		return contentQueryService.findByIdForCommand(contentId);
	}

	@Override
	@Transactional
	public void delete(UUID contentId) {
		Content content = contentRepository.findByIdForUpdate(contentId)
			.orElseThrow(() -> new ContentNotFoundException(contentId));
		if (content.getType() == ContentType.TV_SERIES) {
			throw new InvalidContentSearchException();
		}
		if (content.isHidden()) {
			return;
		}
		List<UUID> idsToCheck = new ArrayList<>();
		idsToCheck.add(contentId);
		Content parent = content.getParentContent();
		boolean hideParent = false;
		if (parent != null) {
			List<Content> seasons = dependencyQueryRepository.findChildSeasonsForUpdate(parent.getId());
			hideParent = seasons.stream().noneMatch(season ->
				!season.getId().equals(contentId) && !season.isHidden());
			if (hideParent) {
				idsToCheck.add(parent.getId());
			}
		}
		if (dependencyQueryRepository.existsWatchPartyByContentIdIn(idsToCheck)) {
			throw new ContentDeletionBlockedException(contentId);
		}
		content.hide();
		if (hideParent) {
			parent.hide();
		}
	}

	private Content createContent(ContentCreateRequest request) {
		Content parent = null;
		if (request.getType() == ContentType.TV_SEASON) {
			parent = requireSeries(request.getParentContentId());
			if (contentRepository.existsByParentContent_IdAndSeasonNumber(
				parent.getId(), request.getSeasonNumber())) {
				throw new ContentSeasonAlreadyExistsException(parent.getId(), request.getSeasonNumber());
			}
			parent.show();
		}
		return Content.builder()
			.parentContent(parent)
			.title(request.getTitle())
			.type(request.getType())
			.seasonNumber(request.getSeasonNumber())
			.episodeCount(request.getEpisodeCount())
			.description(request.getDescription())
			.releaseDate(request.getType() == ContentType.SPORT ? null : request.getReleaseDate())
			.runtime(request.getType() == ContentType.MOVIE ? request.getRuntime() : null)
			.metadata(request.getType() == ContentType.SPORT ? null : request.getMetadata())
			.build();
	}

	private void createSportEvent(Content content, ContentCreateRequest request) {
		SportType sportType = sportTypeRepository.findByCode(request.getSportType())
			.orElseThrow(InvalidContentSearchException::new);
		sportEventRepository.save(SportEvent.create(
			content, sportType, null, request.getLeague(), request.getSeason(), request.getRound(),
			request.getHomeTeam(), request.getAwayTeam(), request.getVenue(), request.getCountry(),
			request.getScheduledAt(), request.getHomeScore(), request.getAwayScore(), null,
			SportEvent.NormalizedStatus.UNKNOWN, null
		));
	}

	private Content requireSeries(UUID parentId) {
		if (parentId == null) {
			throw new InvalidContentSearchException();
		}
		Content parent = contentRepository.findById(parentId)
			.orElseThrow(() -> new ContentNotFoundException(parentId));
		if (parent.getType() != ContentType.TV_SERIES) {
			throw new InvalidContentSearchException();
		}
		return parent;
	}

	private void replaceRelations(
		Content content,
		List<UUID> genreIds,
		List<String> tags,
		List<ContentCastRequest> casts
	) {
		replaceGenres(content, genreIds == null ? List.of() : genreIds);
		replaceTags(content, tags == null ? List.of() : tags);
		replaceCasts(content, casts == null ? List.of() : casts);
	}

	private void replaceGenres(Content content, List<UUID> genreIds) {
		Set<UUID> uniqueIds = new HashSet<>(genreIds);
		if (uniqueIds.isEmpty() || uniqueIds.size() != genreIds.size()) {
			throw new InvalidContentSearchException();
		}
		List<Genre> genres = genreRepository.findAllById(uniqueIds);
		if (genres.size() != uniqueIds.size()) {
			UUID missing = uniqueIds.stream().filter(id -> genres.stream()
				.noneMatch(genre -> genre.getId().equals(id))).findFirst().orElseThrow();
			throw new GenreNotFoundException(missing);
		}
		contentGenreRepository.deleteAllByContentId(content.getId());
		contentGenreRepository.saveAll(genres.stream()
			.map(genre -> ContentGenre.create(content, genre)).toList());
	}

	private void replaceTags(Content content, List<String> tagNames) {
		List<String> normalized = tagNames.stream().map(String::strip).distinct().toList();
		if (normalized.size() != tagNames.size() || normalized.size() > 3) {
			throw new InvalidContentSearchException();
		}
		contentTagRepository.deleteAllByContentId(content.getId());
		List<ContentTag> relations = normalized.stream().map(name -> {
			Tag tag = tagRepository.findByName(name).orElseGet(() -> tagRepository.save(Tag.create(name)));
			return ContentTag.create(content, tag, TagSource.MANUAL);
		}).toList();
		contentTagRepository.saveAll(relations);
	}

	private void replaceCasts(Content content, List<ContentCastRequest> casts) {
		contentCastRepository.deleteAllByContentId(content.getId());
		List<ContentCast> relations = new ArrayList<>();
		for (int index = 0; index < casts.size(); index++) {
			ContentCastRequest cast = casts.get(index);
			relations.add(ContentCast.create(content, cast.getName(), index,
				cast.getRoleName(), cast.getProfileImageUrl()));
		}
		contentCastRepository.saveAll(relations);
	}

	private void requireImageStorageAbsent(MultipartFile thumbnail) {
		if (thumbnail != null && !thumbnail.isEmpty()) {
			throw new ContentStorageUnavailableException(
				new IllegalStateException("콘텐츠 이미지 저장 서비스가 구성되지 않았습니다."));
		}
	}

	private static <T> T value(JsonNullable<T> wrapper, T current) {
		return wrapper.isPresent() ? wrapper.orElse(null) : current;
	}
}
