package com.moduplaylist.api.content.service.impl;

import com.github.f4b6a3.uuid.UuidCreator;
import com.moduplaylist.api.content.dto.ContentCastRequest;
import com.moduplaylist.api.content.dto.ContentCreateRequest;
import com.moduplaylist.api.content.dto.ContentCreateResponse;
import com.moduplaylist.api.content.dto.ContentPlatformCreateRequest;
import com.moduplaylist.api.content.dto.ContentResponse;
import com.moduplaylist.api.content.dto.ContentUpdateRequest;
import com.moduplaylist.api.content.dto.EpisodeCreateRequest;
import com.moduplaylist.api.content.dto.EpisodeResponse;
import com.moduplaylist.api.content.dto.EpisodeUpdateRequest;
import com.moduplaylist.api.content.dto.SeasonCreateRequest;
import com.moduplaylist.api.content.event.ContentLifecycleEvent;
import com.moduplaylist.api.content.service.ContentCommandService;
import com.moduplaylist.api.content.service.ContentQueryService;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentCast;
import com.moduplaylist.core.content.entity.ContentGenre;
import com.moduplaylist.core.content.entity.ContentPlatform;
import com.moduplaylist.core.content.entity.ContentTag;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.entity.Genre;
import com.moduplaylist.core.content.entity.Episode;
import com.moduplaylist.core.content.entity.Platform;
import com.moduplaylist.core.content.entity.SportEvent;
import com.moduplaylist.core.content.entity.SportType;
import com.moduplaylist.core.content.entity.Tag;
import com.moduplaylist.core.content.entity.TagSource;
import com.moduplaylist.core.content.exception.ContentDeletionBlockedException;
import com.moduplaylist.core.content.exception.ContentNotFoundException;
import com.moduplaylist.core.content.exception.ContentSeasonAlreadyExistsException;
import com.moduplaylist.core.content.exception.ContentStorageUnavailableException;
import com.moduplaylist.core.content.exception.GenreNotFoundException;
import com.moduplaylist.core.content.exception.HiddenSeasonAlreadyExistsException;
import com.moduplaylist.core.content.exception.DuplicateContentConfirmationRequiredException;
import com.moduplaylist.core.content.exception.ContentUploadLimitExceededException;
import com.moduplaylist.core.content.exception.InvalidContentImageException;
import com.moduplaylist.core.content.exception.UnsupportedContentImageTypeException;
import com.moduplaylist.core.content.exception.EpisodeAlreadyExistsException;
import com.moduplaylist.core.content.exception.EpisodeNumberChangeBlockedException;
import com.moduplaylist.core.content.exception.InvalidContentSearchException;
import com.moduplaylist.core.content.exception.PlatformNotFoundException;
import com.moduplaylist.core.content.exception.SportTypeNotFoundException;
import com.moduplaylist.core.content.repository.ContentCastRepository;
import com.moduplaylist.core.content.repository.ContentDependencyQueryRepository;
import com.moduplaylist.core.content.repository.ContentGenreRepository;
import com.moduplaylist.core.content.repository.ContentPlatformRepository;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.content.repository.ContentTagRepository;
import com.moduplaylist.core.content.repository.GenreRepository;
import com.moduplaylist.core.content.repository.EpisodeRepository;
import com.moduplaylist.core.content.repository.PlatformRepository;
import com.moduplaylist.core.content.repository.SportEventRepository;
import com.moduplaylist.core.content.repository.SportTypeRepository;
import com.moduplaylist.core.content.repository.TagRepository;
import com.moduplaylist.infrastructure.storage.ContentImageStorage;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContentCommandServiceImpl implements ContentCommandService {
	private static final String ORIGINAL_TITLE_METADATA_KEY = "originalTitle";

	private final ContentRepository contentRepository;
	private final GenreRepository genreRepository;
	private final ContentGenreRepository contentGenreRepository;
	private final PlatformRepository platformRepository;
	private final ContentPlatformRepository contentPlatformRepository;
	private final TagRepository tagRepository;
	private final ContentTagRepository contentTagRepository;
	private final ContentCastRepository contentCastRepository;
	private final SportTypeRepository sportTypeRepository;
	private final SportEventRepository sportEventRepository;
	private final EpisodeRepository episodeRepository;
	private final ContentDependencyQueryRepository dependencyQueryRepository;
	private final ContentQueryService contentQueryService;
	private final ContentImageStorage contentImageStorage;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	@Transactional
	public ContentCreateResponse create(ContentCreateRequest request, Map<String, MultipartFile> images) {
		validateCreateRequest(request);
		validateDuplicateCandidate(request);
		validateImages(request, images);
		List<String> uploadedKeys = new ArrayList<>();
		registerImageRollbackCleanup(uploadedKeys);
		if (request.getType() == ContentType.TV_SERIES) {
			return createTvSeries(request, images, uploadedKeys);
		}
		String thumbnailUrl = uploadImage(images.get("thumbnail"), uploadedKeys);
		ContentCreationResult creationResult = createContent(request, thumbnailUrl);
		Content content = creationResult.content();
		contentRepository.saveAndFlush(content);

		if (content.getType() == ContentType.MOVIE || content.getType() == ContentType.TV_SEASON) {
			replaceRelations(content, request.getGenreIds(), request.getManualTags(), request.getCasts(),
				request.getPlatforms(), creationResult.reusedHiddenSeason());
		}
		if (content.getType() == ContentType.SPORT) {
			createSportEvent(content, request);
		}
		publishContentLifecycleEvent(content.getId(), ContentLifecycleEvent.Type.UPSERTED);

		return ContentCreateResponse.builder()
			.contentIds(List.of(content.getId()))
			.createdAt(content.getCreatedAt())
			.build();
	}

	private void validateCreateRequest(ContentCreateRequest request) {
		switch (request.getType()) {
			case MOVIE -> validateMovieCreateRequest(request);
			case TV_SERIES -> validateTvSeriesCreateRequest(request);
			case TV_SEASON -> validateTvSeasonCreateRequest(request);
			case SPORT -> validateSportCreateRequest(request);
		}
	}

	private void validateDuplicateCandidate(ContentCreateRequest request) {
		if (request.isDuplicateConfirmed() || request.getType() == ContentType.TV_SEASON) {
			return;
		}
		boolean duplicate = switch (request.getType()) {
			case MOVIE -> contentRepository.existsByTypeAndTitleAndReleaseDate(
				ContentType.MOVIE, request.getTitle(), request.getReleaseDate());
			case TV_SERIES -> contentRepository.existsByTypeAndTitle(
				ContentType.TV_SERIES, request.getTitle());
			case SPORT -> sportEventRepository.existsDuplicate(
				request.getTitle(), request.getHomeTeam(), request.getAwayTeam(),
				request.getScheduledAt());
			case TV_SEASON -> false;
		};
		if (duplicate) {
			throw new DuplicateContentConfirmationRequiredException();
		}
	}

	private void validateMovieCreateRequest(ContentCreateRequest request) {
		requireDescription(request);
		requireGenres(request);
		validateTags(request.getManualTags());
		validatePlatforms(request.getPlatforms());
		if (request.getParentContentId() != null || request.getSeasonNumber() != null
			|| request.getEpisodeCount() != null || request.getSeasons() != null
			|| hasSportFields(request)) {
			throw new InvalidContentSearchException();
		}
	}

	private void validateTvSeriesCreateRequest(ContentCreateRequest request) {
		if (request.getSeasons() == null || request.getSeasons().isEmpty()
			|| request.getSeasons().size() > 15) {
			throw new InvalidContentSearchException();
		}
		if (request.getParentContentId() != null || request.getSeasonNumber() != null
			|| request.getEpisodeCount() != null
			|| request.getDescription() != null || request.getReleaseDate() != null
			|| request.getRuntime() != null
			|| request.getGenreIds() != null || request.getManualTags() != null
			|| request.getCasts() != null || request.getPlatforms() != null
			|| hasSportFields(request)) {
			throw new InvalidContentSearchException();
		}
		Set<Integer> seasonNumbers = new HashSet<>();
		Set<String> thumbnailKeys = new HashSet<>();
		for (SeasonCreateRequest season : request.getSeasons()) {
			validateTags(season.getTags());
			validatePlatforms(season.getPlatforms());
			if (!seasonNumbers.add(season.getSeasonNumber())
				|| (season.getThumbnailKey() != null
				&& !thumbnailKeys.add(season.getThumbnailKey()))) {
				throw new InvalidContentSearchException();
			}
		}
	}

	private void validateTvSeasonCreateRequest(ContentCreateRequest request) {
		requireDescription(request);
		requireGenres(request);
		validateTags(request.getManualTags());
		validatePlatforms(request.getPlatforms());
		if (request.getParentContentId() == null || request.getSeasonNumber() == null
			|| request.getRuntime() != null || request.getOriginalTitle() != null
			|| request.getSeasons() != null
			|| hasSportFields(request)) {
			throw new InvalidContentSearchException();
		}
	}

	private void validateSportCreateRequest(ContentCreateRequest request) {
		requireDescription(request);
		if (request.getSportTypeId() == null
			|| request.getHomeTeam() == null || request.getHomeTeam().isBlank()
			|| request.getAwayTeam() == null || request.getAwayTeam().isBlank()
			|| (request.getHomeScore() == null) != (request.getAwayScore() == null)
			|| request.getParentContentId() != null || request.getSeasonNumber() != null
			|| request.getEpisodeCount() != null || request.getSeasons() != null
			|| request.getReleaseDate() != null || request.getRuntime() != null
			|| request.getOriginalTitle() != null || request.getGenreIds() != null
			|| request.getManualTags() != null || request.getCasts() != null
			|| request.getPlatforms() != null) {
			throw new InvalidContentSearchException();
		}
	}

	private void requireDescription(ContentCreateRequest request) {
		if (request.getDescription() == null || request.getDescription().isBlank()) {
			throw new InvalidContentSearchException();
		}
	}

	private void requireGenres(ContentCreateRequest request) {
		if (request.getGenreIds() == null || request.getGenreIds().isEmpty()) {
			throw new InvalidContentSearchException();
		}
	}

	private void validateTags(List<String> tagNames) {
		if (tagNames == null) {
			return;
		}
		long tagCount = tagNames.stream().map(String::strip).distinct().count();
		if (tagCount > 3) {
			throw new InvalidContentSearchException();
		}
	}

	private void validatePlatforms(List<ContentPlatformCreateRequest> platforms) {
		if (platforms == null) {
			return;
		}
		Set<UUID> platformIds = new HashSet<>();
		for (ContentPlatformCreateRequest platform : platforms) {
			if (!platformIds.add(platform.getPlatformId())) {
				throw new InvalidContentSearchException();
			}
		}
	}

	private boolean hasSportFields(ContentCreateRequest request) {
		return request.getSportTypeId() != null || request.getScheduledAt() != null
			|| request.getLeague() != null || request.getSeason() != null
			|| request.getRound() != null || request.getHomeTeam() != null
			|| request.getAwayTeam() != null || request.getVenue() != null
			|| request.getCountry() != null || request.getHomeScore() != null
			|| request.getAwayScore() != null;
	}

	@Override
	@Transactional
	public ContentResponse update(
		UUID contentId,
		ContentUpdateRequest request,
		MultipartFile thumbnail
	) {
		Content content = lockContentForUpdate(contentId, request, true);
		validateUpdateFields(content.getType(), request);
		if (content.getType() == ContentType.TV_SERIES) {
			if (thumbnail != null) {
				throw new InvalidContentSearchException();
			}
			validateUpdateDuplicateCandidate(content, request);
			return updateSeries(content, request);
		}
		if (thumbnail != null && request.isRemoveThumbnail()) {
			throw new InvalidContentSearchException();
		}
		if (thumbnail != null) {
			validateImage(thumbnail);
		}
		validateUpdateDuplicateCandidate(content, request);

		String previousThumbnailUrl = content.getThumbnailUrl();
		String thumbnailUrl = previousThumbnailUrl;
		boolean thumbnailChanged = false;
		if (thumbnail != null) {
			List<String> uploadedKeys = new ArrayList<>();
			registerImageRollbackCleanup(uploadedKeys);
			thumbnailUrl = uploadImage(thumbnail, uploadedKeys);
			thumbnailChanged = true;
		} else if (request.isRemoveThumbnail() && thumbnailUrl != null) {
			thumbnailUrl = null;
			thumbnailChanged = true;
		}
		if (thumbnailChanged && previousThumbnailUrl != null) {
			registerPreviousImageCleanup(previousThumbnailUrl);
		}
		if (content.getType() == ContentType.SPORT) {
			return updateSport(content, request, thumbnailUrl, thumbnailChanged);
		}
		boolean embeddingSourceChanged = isEmbeddingSourceChanged(content, request);
		TvSeasonUpdateContext tvSeasonContext = content.getType() == ContentType.TV_SEASON
			? prepareTvSeasonUpdate(content, request)
			: null;
		Map<String, Object> metadata = content.getMetadata();
		boolean parentSearchSourceChanged = false;
		if (content.getType() == ContentType.TV_SEASON
			&& !request.getParentContentId().isPresent()
			&& !request.isCreateNewSeries()
			&& (request.getSeriesTitle().isPresent()
			|| request.getOriginalTitle().isPresent())) {
			Content parent = contentRepository.findByIdForUpdate(tvSeasonContext.parent().getId())
				.orElseThrow(() -> new ContentNotFoundException(
					tvSeasonContext.parent().getId()));
			String parentTitle = value(request.getSeriesTitle(), parent.getTitle());
			Map<String, Object> parentMetadata = updateOriginalTitle(
				parent.getMetadata(), request.getOriginalTitle());
			parentSearchSourceChanged = !Objects.equals(parentTitle, parent.getTitle())
				|| !Objects.equals(parentMetadata, parent.getMetadata());
			if (parentSearchSourceChanged) {
				parent.updateCommonDetails(parentTitle, null, null, parentMetadata);
			}
		} else {
			metadata = updateOriginalTitle(content.getMetadata(), request.getOriginalTitle());
		}
		boolean searchSourceChanged = embeddingSourceChanged || isCastChanged(content, request)
			|| !Objects.equals(metadata, content.getMetadata());
		Integer seasonNumber = tvSeasonContext == null
			? content.getSeasonNumber() : tvSeasonContext.seasonNumber();
		if (tvSeasonContext != null && (!Objects.equals(seasonNumber, content.getSeasonNumber())
			|| tvSeasonContext.parentChanged())) {
			searchSourceChanged = true;
		}
		String title = value(request.getTitle(), content.getTitle());
		String description = value(request.getDescription(), content.getDescription());
		content.updateCommonDetails(
			title,
			description,
			value(request.getReleaseDate(), content.getReleaseDate()),
			metadata
		);
		if (content.getType() == ContentType.MOVIE && request.getRuntime().isPresent()) {
			content.updateMovieDetails(request.getRuntime().orElse(null));
		}
		if (content.getType() == ContentType.TV_SEASON) {
			Integer episodeCount = value(
				request.getEpisodeCount(), content.getEpisodeCount());
			validateEpisodeCount(content.getId(), episodeCount);
			content.updateTvSeasonDetails(
				tvSeasonContext.parent(),
				seasonNumber,
				episodeCount,
				null
			);
		}
		if (thumbnailChanged) {
			content.replaceThumbnailUrl(thumbnailUrl);
		}

		if (request.getGenreIds().isPresent() || request.getManualTags().isPresent()
			|| request.getCasts().isPresent() || request.getPlatforms().isPresent()) {
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
			if (request.getPlatforms().isPresent()) {
				List<ContentPlatformCreateRequest> platforms = request.getPlatforms().orElse(List.of());
				validatePlatforms(platforms);
				replacePlatforms(content, platforms);
			}
		}
		if (embeddingSourceChanged) {
			content.markEmbeddingSourceUpdated();
		}
		contentRepository.flush();
		ContentResponse response = contentQueryService.findByIdForCommand(contentId);
		if (tvSeasonContext != null && tvSeasonContext.activatedParentId() != null) {
			publishContentLifecycleEvent(
				tvSeasonContext.activatedParentId(), ContentLifecycleEvent.Type.UPSERTED);
		}
		if (tvSeasonContext != null && tvSeasonContext.hiddenParentId() != null) {
			publishContentLifecycleEvent(
				tvSeasonContext.hiddenParentId(), ContentLifecycleEvent.Type.DELETED);
		}
		if (parentSearchSourceChanged) {
			Content parent = tvSeasonContext.parent();
			publishContentLifecycleEvent(parent.getId(), ContentLifecycleEvent.Type.UPSERTED);
			contentRepository.findAllByParentContent_IdAndHiddenFalseOrderBySeasonNumberAsc(parent.getId())
				.forEach(season -> publishContentLifecycleEvent(
					season.getId(), ContentLifecycleEvent.Type.UPSERTED));
		} else if (searchSourceChanged) {
			publishContentLifecycleEvent(contentId, ContentLifecycleEvent.Type.UPSERTED);
		}
		return response;
	}

	@Override
	@Transactional
	public ContentResponse restoreSeason(
		UUID hiddenSeasonId,
		ContentUpdateRequest request,
		MultipartFile thumbnail
	) {
		Content season = lockContentForUpdate(hiddenSeasonId, request, false);
		if (!season.isHidden() || season.getType() != ContentType.TV_SEASON) {
			throw new ContentNotFoundException(hiddenSeasonId);
		}
		Content parent = season.getParentContent();
		boolean parentWasHidden = parent.isHidden();
		season.show();
		parent.show();

		ContentResponse response = update(hiddenSeasonId, request, thumbnail);
		publishContentLifecycleEvent(hiddenSeasonId, ContentLifecycleEvent.Type.UPSERTED);
		if (parentWasHidden && !parent.isHidden()) {
			publishContentLifecycleEvent(parent.getId(), ContentLifecycleEvent.Type.UPSERTED);
		}
		return response;
	}

	private ContentResponse updateSeries(Content content, ContentUpdateRequest request) {
		String title = value(request.getTitle(), content.getTitle());
		Map<String, Object> metadata = updateOriginalTitle(
			content.getMetadata(), request.getOriginalTitle());
		boolean searchSourceChanged = !Objects.equals(title, content.getTitle())
			|| !Objects.equals(metadata, content.getMetadata());
		content.updateCommonDetails(title, null, null, metadata);
		contentRepository.flush();
		if (searchSourceChanged) {
			publishContentLifecycleEvent(content.getId(), ContentLifecycleEvent.Type.UPSERTED);
			contentRepository.findAllByParentContent_IdAndHiddenFalseOrderBySeasonNumberAsc(content.getId())
				.forEach(season -> publishContentLifecycleEvent(
					season.getId(), ContentLifecycleEvent.Type.UPSERTED));
		}
		return ContentResponse.builder()
			.id(content.getId())
			.type(content.getType())
			.title(content.getTitle())
			.originalTitle(content.getOriginalTitle())
			.createdAt(content.getCreatedAt())
			.updatedAt(content.getUpdatedAt())
			.build();
	}

	private void validateUpdateDuplicateCandidate(
		Content content,
		ContentUpdateRequest request
	) {
		if (request.isDuplicateConfirmed()) {
			return;
		}
		boolean duplicate = switch (content.getType()) {
			case MOVIE -> {
				String title = value(request.getTitle(), content.getTitle());
				java.time.LocalDate releaseDate = value(
					request.getReleaseDate(), content.getReleaseDate());
				boolean candidateChanged = !Objects.equals(title, content.getTitle())
					|| !Objects.equals(releaseDate, content.getReleaseDate());
				yield candidateChanged
					&& contentRepository.existsByTypeAndTitleAndReleaseDateAndIdNot(
						ContentType.MOVIE, title, releaseDate, content.getId());
			}
			case TV_SERIES -> {
				String title = value(request.getTitle(), content.getTitle());
				yield !Objects.equals(title, content.getTitle())
					&& contentRepository.existsByTypeAndTitleAndIdNot(
						ContentType.TV_SERIES, title, content.getId());
			}
			case TV_SEASON -> {
				if (request.isCreateNewSeries()
					|| request.getParentContentId().isPresent()
					|| !request.getSeriesTitle().isPresent()) {
					yield false;
				}
				Content parent = content.getParentContent();
				String title = request.getSeriesTitle().orElse(parent.getTitle());
				yield !Objects.equals(title, parent.getTitle())
					&& contentRepository.existsByTypeAndTitleAndIdNot(
						ContentType.TV_SERIES, title, parent.getId());
			}
			case SPORT -> false;
		};
		if (duplicate) {
			throw new DuplicateContentConfirmationRequiredException();
		}
	}

	private TvSeasonUpdateContext prepareTvSeasonUpdate(
		Content season,
		ContentUpdateRequest request
	) {
		Content previousParent = season.getParentContent();
		Integer seasonNumber = value(request.getSeasonNumber(), season.getSeasonNumber());
		Content targetParent = previousParent;
		UUID activatedParentId = null;
		UUID hiddenParentId = null;

		if (request.isCreateNewSeries()) {
			String seriesTitle = request.getSeriesTitle().orElse(null);
			if (!request.isDuplicateConfirmed()
				&& contentRepository.existsByTypeAndTitle(ContentType.TV_SERIES, seriesTitle)) {
				throw new DuplicateContentConfirmationRequiredException();
			}
			targetParent = Content.builder()
				.title(seriesTitle)
				.type(ContentType.TV_SERIES)
				.metadata(originalTitleMetadata(request.getOriginalTitle().orElse(null)))
				.build();
			contentRepository.save(targetParent);
			activatedParentId = targetParent.getId();
		} else if (request.getParentContentId().isPresent()) {
			UUID targetParentId = request.getParentContentId().orElseThrow();
			targetParent = requireSeries(targetParentId);
		}

		validateSeasonSlot(targetParent.getId(), seasonNumber, season.getId());
		boolean parentChanged = !targetParent.getId().equals(previousParent.getId());
		if (parentChanged) {
			if (targetParent.isHidden()) {
				targetParent.show();
				activatedParentId = targetParent.getId();
			}
			boolean hasOtherVisibleSeason = hasOtherVisibleSeasonForUpdate(
				previousParent.getId(), season.getId());
			if (!hasOtherVisibleSeason && !previousParent.isHidden()) {
				previousParent.hide();
				hiddenParentId = previousParent.getId();
			}
		}

		return new TvSeasonUpdateContext(
			targetParent,
			seasonNumber,
			parentChanged,
			activatedParentId,
			hiddenParentId
		);
	}

	private void validateSeasonSlot(UUID seriesId, Integer seasonNumber, UUID currentSeasonId) {
		Content conflictingSeason = contentRepository
			.findByParentContent_IdAndSeasonNumber(seriesId, seasonNumber)
			.orElse(null);
		if (conflictingSeason == null || conflictingSeason.getId().equals(currentSeasonId)) {
			return;
		}
		if (conflictingSeason.isHidden()) {
			throw new HiddenSeasonAlreadyExistsException(
				seriesId, seasonNumber, conflictingSeason.getId());
		}
		throw new ContentSeasonAlreadyExistsException(seriesId, seasonNumber);
	}

	private boolean isCastChanged(Content content, ContentUpdateRequest request) {
		if ((content.getType() != ContentType.MOVIE
			&& content.getType() != ContentType.TV_SEASON)
			|| !request.getCasts().isPresent()) {
			return false;
		}
		List<ContentCastRequest> requested = request.getCasts().orElse(List.of());
		List<ContentCast> current = contentCastRepository
			.findAllByContent_IdOrderByDisplayOrderAsc(content.getId());
		if (requested.size() != current.size()) {
			return true;
		}
		for (int index = 0; index < requested.size(); index++) {
			ContentCastRequest requestedCast = requested.get(index);
			ContentCast currentCast = current.get(index);
			if (!Objects.equals(requestedCast.getName(), currentCast.getName())
				|| !Objects.equals(requestedCast.getRoleName(), currentCast.getRoleName())) {
				return true;
			}
		}
		return false;
	}

	private boolean isEmbeddingSourceChanged(Content content, ContentUpdateRequest request) {
		if (content.getType() != ContentType.MOVIE
			&& content.getType() != ContentType.TV_SEASON) {
			return false;
		}
		if (!Objects.equals(value(request.getTitle(), content.getTitle()), content.getTitle())
			|| !Objects.equals(
				value(request.getDescription(), content.getDescription()), content.getDescription())) {
			return true;
		}
		if (request.getGenreIds().isPresent()) {
			Set<UUID> requestedGenreIds = new HashSet<>(request.getGenreIds().orElse(List.of()));
			Set<UUID> currentGenreIds = contentGenreRepository
				.findAllWithGenreByContentIdIn(List.of(content.getId())).stream()
				.map(relation -> relation.getGenre().getId())
				.collect(java.util.stream.Collectors.toSet());
			if (!requestedGenreIds.equals(currentGenreIds)) {
				return true;
			}
		}
		if (request.getManualTags().isPresent()) {
			Set<String> requestedTagNames = request.getManualTags().orElse(List.of()).stream()
				.map(String::strip)
				.collect(java.util.stream.Collectors.toSet());
			Set<String> currentTagNames = contentTagRepository
				.findAllWithTagByContentIdIn(List.of(content.getId())).stream()
				.map(relation -> relation.getTag().getName())
				.collect(java.util.stream.Collectors.toSet());
			if (!requestedTagNames.equals(currentTagNames)) {
				return true;
			}
		}
		return false;
	}

	private void validateUpdateFields(ContentType type, ContentUpdateRequest request) {
		boolean invalid = switch (type) {
			case MOVIE -> request.getSeriesTitle().isPresent()
				|| request.getParentContentId().isPresent()
				|| request.isCreateNewSeries()
				|| request.getSeasonNumber().isPresent()
				|| request.getEpisodeCount().isPresent()
				|| hasSportUpdateFields(request);
			case TV_SERIES -> request.getSeriesTitle().isPresent()
				|| request.getParentContentId().isPresent()
				|| request.isCreateNewSeries()
				|| request.getDescription().isPresent()
				|| request.getReleaseDate().isPresent()
				|| request.getRuntime().isPresent()
				|| request.getSeasonNumber().isPresent()
				|| request.getEpisodeCount().isPresent()
				|| request.getGenreIds().isPresent()
				|| request.getManualTags().isPresent()
				|| request.getCasts().isPresent()
				|| request.getPlatforms().isPresent()
				|| request.isRemoveThumbnail()
				|| hasSportUpdateFields(request);
			case TV_SEASON -> request.getRuntime().isPresent()
				|| hasSportUpdateFields(request);
			case SPORT -> request.getSeriesTitle().isPresent()
				|| request.getParentContentId().isPresent()
				|| request.isCreateNewSeries()
				|| request.getReleaseDate().isPresent()
				|| request.getRuntime().isPresent()
				|| request.getSeasonNumber().isPresent()
				|| request.getEpisodeCount().isPresent()
				|| request.getOriginalTitle().isPresent()
				|| request.getGenreIds().isPresent()
				|| request.getManualTags().isPresent()
				|| request.getCasts().isPresent()
				|| request.getPlatforms().isPresent();
		};
		if (invalid) {
			throw new InvalidContentSearchException();
		}
		if (type == ContentType.TV_SEASON) {
			if (request.getParentContentId().isPresent() && request.isCreateNewSeries()) {
				throw new InvalidContentSearchException();
			}
			if (request.isCreateNewSeries()) {
				if (!request.getSeriesTitle().isPresent()
					|| request.getSeriesTitle().orElse(null) == null) {
					throw new InvalidContentSearchException();
				}
			} else if (request.getParentContentId().isPresent()
				&& (request.getSeriesTitle().isPresent()
				|| request.getOriginalTitle().isPresent())) {
				throw new InvalidContentSearchException();
			}
		}
	}

	private ContentResponse updateSport(
		Content content,
		ContentUpdateRequest request,
		String thumbnailUrl,
		boolean thumbnailChanged
	) {
		SportEvent sportEvent = sportEventRepository.findById(content.getId())
			.orElseThrow(() -> new IllegalStateException(
				"스포츠 콘텐츠에 경기 정보가 없습니다. contentId=" + content.getId()));

		SportType sportType = sportEvent.getSportType();
		if (request.getSportTypeId().isPresent()) {
			UUID sportTypeId = request.getSportTypeId().orElse(null);
			if (sportTypeId == null) {
				throw new InvalidContentSearchException();
			}
			sportType = sportTypeRepository.findById(sportTypeId)
				.orElseThrow(() -> new SportTypeNotFoundException(sportTypeId));
		}

		String title = value(request.getTitle(), content.getTitle());
		String description = value(request.getDescription(), content.getDescription());
		String homeTeam = value(request.getHomeTeam(), sportEvent.getHomeTeamName());
		String awayTeam = value(request.getAwayTeam(), sportEvent.getAwayTeamName());
		Instant scheduledAt = value(request.getScheduledAt(), sportEvent.getScheduledAt());
		if (title == null || title.isBlank() || description == null || description.isBlank()
			|| homeTeam == null || homeTeam.isBlank() || awayTeam == null || awayTeam.isBlank()) {
			throw new InvalidContentSearchException();
		}

		Integer homeScore = value(request.getHomeScore(), sportEvent.getHomeScore());
		Integer awayScore = value(request.getAwayScore(), sportEvent.getAwayScore());
		if ((homeScore == null) != (awayScore == null)) {
			throw new InvalidContentSearchException();
		}
		boolean duplicateCandidateChanged = !Objects.equals(title, content.getTitle())
			|| !Objects.equals(homeTeam, sportEvent.getHomeTeamName())
			|| !Objects.equals(awayTeam, sportEvent.getAwayTeamName())
			|| !Objects.equals(scheduledAt, sportEvent.getScheduledAt());
		if (!request.isDuplicateConfirmed() && duplicateCandidateChanged
			&& sportEventRepository.existsDuplicateExcluding(
				content.getId(), title, homeTeam, awayTeam, scheduledAt)) {
			throw new DuplicateContentConfirmationRequiredException();
		}

		boolean changed = thumbnailChanged
			|| !Objects.equals(title, content.getTitle())
			|| !Objects.equals(description, content.getDescription())
			|| !Objects.equals(sportType.getId(), sportEvent.getSportType().getId())
			|| !Objects.equals(scheduledAt, sportEvent.getScheduledAt())
			|| !Objects.equals(value(request.getLeague(), sportEvent.getLeagueName()), sportEvent.getLeagueName())
			|| !Objects.equals(value(request.getSeason(), sportEvent.getSeason()), sportEvent.getSeason())
			|| !Objects.equals(value(request.getRound(), sportEvent.getRound()), sportEvent.getRound())
			|| !Objects.equals(homeTeam, sportEvent.getHomeTeamName())
			|| !Objects.equals(awayTeam, sportEvent.getAwayTeamName())
			|| !Objects.equals(value(request.getVenue(), sportEvent.getVenue()), sportEvent.getVenue())
			|| !Objects.equals(value(request.getCountry(), sportEvent.getCountry()), sportEvent.getCountry())
			|| !Objects.equals(homeScore, sportEvent.getHomeScore())
			|| !Objects.equals(awayScore, sportEvent.getAwayScore());
		boolean searchSourceChanged = !Objects.equals(title, content.getTitle())
			|| !Objects.equals(description, content.getDescription())
			|| !Objects.equals(sportType.getId(), sportEvent.getSportType().getId())
			|| !Objects.equals(value(request.getLeague(), sportEvent.getLeagueName()), sportEvent.getLeagueName())
			|| !Objects.equals(value(request.getSeason(), sportEvent.getSeason()), sportEvent.getSeason())
			|| !Objects.equals(homeTeam, sportEvent.getHomeTeamName())
			|| !Objects.equals(awayTeam, sportEvent.getAwayTeamName())
			|| !Objects.equals(value(request.getCountry(), sportEvent.getCountry()), sportEvent.getCountry());

		if (changed) {
			sportEvent.updateDetails(
				sportType,
				value(request.getLeague(), sportEvent.getLeagueName()),
				value(request.getSeason(), sportEvent.getSeason()),
				value(request.getRound(), sportEvent.getRound()),
				homeTeam,
				awayTeam,
				value(request.getVenue(), sportEvent.getVenue()),
				value(request.getCountry(), sportEvent.getCountry()),
				scheduledAt,
				homeScore,
				awayScore
			);
			sportEventRepository.flush();
			contentRepository.updateSportCommonDetails(
				content.getId(), title, description, thumbnailUrl);
			if (searchSourceChanged) {
				publishContentLifecycleEvent(content.getId(), ContentLifecycleEvent.Type.UPSERTED);
			}
		}
		return contentQueryService.findByIdForCommand(content.getId());
	}

	private boolean hasSportUpdateFields(ContentUpdateRequest request) {
		return request.getSportTypeId().isPresent() || request.getScheduledAt().isPresent()
			|| request.getLeague().isPresent() || request.getSeason().isPresent()
			|| request.getRound().isPresent() || request.getHomeTeam().isPresent()
			|| request.getAwayTeam().isPresent() || request.getVenue().isPresent()
			|| request.getCountry().isPresent() || request.getHomeScore().isPresent()
			|| request.getAwayScore().isPresent();
	}

	@Override
	@Transactional
	public EpisodeResponse createEpisode(
		UUID seasonId,
		EpisodeCreateRequest request,
		MultipartFile thumbnail
	) {
		if (thumbnail != null) {
			validateImage(thumbnail);
		}
		List<String> uploadedKeys = new ArrayList<>();
		registerImageRollbackCleanup(uploadedKeys);
		String thumbnailUrl = uploadImage(thumbnail, uploadedKeys);
		Content season = requireVisibleSeasonForUpdate(seasonId);
		if (episodeRepository.existsBySeason_IdAndEpisodeNumber(
			seasonId, request.getEpisodeNumber())) {
			throw new EpisodeAlreadyExistsException(seasonId, request.getEpisodeNumber());
		}
		Episode episode = episodeRepository.saveAndFlush(Episode.builder()
			.season(season)
			.episodeNumber(request.getEpisodeNumber())
			.title(request.getTitle())
			.description(request.getDescription())
			.thumbnailUrl(thumbnailUrl)
			.runtime(request.getRuntime())
			.build());
		return toEpisodeResponse(episode);
	}

	@Override
	@Transactional
	public EpisodeResponse updateEpisode(
		UUID seasonId,
		UUID episodeId,
		EpisodeUpdateRequest request,
		MultipartFile thumbnail
	) {
		requireVisibleSeasonForUpdate(seasonId);
		Episode episode = episodeRepository.findByIdAndSeason_IdAndSeason_HiddenFalse(
			episodeId, seasonId)
			.orElseThrow(() -> new ContentNotFoundException(episodeId));
		if (thumbnail != null && request.isRemoveThumbnail()) {
			throw new InvalidContentSearchException();
		}

		Integer episodeNumber = value(request.getEpisodeNumber(), episode.getEpisodeNumber());
		if (!Objects.equals(episodeNumber, episode.getEpisodeNumber())) {
			if (episodeRepository.existsBySeason_IdAndEpisodeNumberAndIdNot(
				seasonId, episodeNumber, episodeId)) {
				throw new EpisodeAlreadyExistsException(seasonId, episodeNumber);
			}
			if (dependencyQueryRepository.existsActiveWatchPartyForEpisode(
					seasonId, episode.getEpisodeNumber())
				|| dependencyQueryRepository.existsActiveWatchPartyForEpisode(
					seasonId, episodeNumber)) {
				throw new EpisodeNumberChangeBlockedException(
					seasonId,
					episodeId,
					episode.getEpisodeNumber(),
					episodeNumber
				);
			}
		}

		String previousThumbnailUrl = episode.getThumbnailUrl();
		String thumbnailUrl = previousThumbnailUrl;
		boolean thumbnailChanged = false;
		if (thumbnail != null) {
			validateImage(thumbnail);
			List<String> uploadedKeys = new ArrayList<>();
			registerImageRollbackCleanup(uploadedKeys);
			thumbnailUrl = uploadImage(thumbnail, uploadedKeys);
			thumbnailChanged = true;
		} else if (request.isRemoveThumbnail() && thumbnailUrl != null) {
			thumbnailUrl = null;
			thumbnailChanged = true;
		}
		if (thumbnailChanged && previousThumbnailUrl != null) {
			registerPreviousImageCleanup(previousThumbnailUrl);
		}

		episode.updateDetails(
			episodeNumber,
			value(request.getTitle(), episode.getTitle()),
			value(request.getDescription(), episode.getDescription()),
			thumbnailUrl,
			value(request.getRuntime(), episode.getRuntime())
		);
		episodeRepository.flush();
		return toEpisodeResponse(episode);
	}

	@Override
	@Transactional
	public void deleteEpisode(UUID seasonId, UUID episodeId) {
		requireVisibleSeasonForUpdate(seasonId);
		Episode episode = episodeRepository.findByIdAndSeason_IdAndSeason_HiddenFalse(
			episodeId, seasonId)
			.orElseThrow(() -> new ContentNotFoundException(episodeId));
		if (dependencyQueryRepository.existsActiveWatchPartyForEpisode(
			seasonId, episode.getEpisodeNumber())) {
			throw new ContentDeletionBlockedException(seasonId);
		}
		episodeRepository.delete(episode);
		if (episode.getThumbnailUrl() != null) {
			registerPreviousImageCleanup(episode.getThumbnailUrl());
		}
	}

	private Content requireVisibleSeasonForUpdate(UUID seasonId) {
		Content season = contentRepository.findByIdForUpdate(seasonId)
			.filter(content -> !content.isHidden())
			.orElseThrow(() -> new ContentNotFoundException(seasonId));
		if (season.getType() != ContentType.TV_SEASON) {
			throw new InvalidContentSearchException();
		}
		return season;
	}

	private EpisodeResponse toEpisodeResponse(Episode episode) {
		return EpisodeResponse.builder()
			.id(episode.getId())
			.episodeNumber(episode.getEpisodeNumber())
			.title(episode.getTitle())
			.description(episode.getDescription())
			.thumbnailUrl(episode.getThumbnailUrl())
			.runtime(episode.getRuntime())
			.build();
	}

	@Override
	@Transactional
	public void delete(UUID contentId) {
		Content snapshot = contentRepository.findById(contentId)
			.orElseThrow(() -> new ContentNotFoundException(contentId));
		Content content;
		Content parent = null;
		if (snapshot.getType() == ContentType.TV_SEASON) {
			UUID parentId = snapshot.getParentContent().getId();
			parent = requireSeries(parentId);
			content = contentRepository.findByIdForUpdate(contentId)
				.filter(candidate -> candidate.getType() == ContentType.TV_SEASON
					&& candidate.getParentContent().getId().equals(parentId))
				.orElseThrow(() -> new ContentNotFoundException(contentId));
		} else {
			content = contentRepository.findByIdForUpdate(contentId)
				.orElseThrow(() -> new ContentNotFoundException(contentId));
		}
		if (content.getType() == ContentType.TV_SERIES) {
			throw new InvalidContentSearchException();
		}
		if (content.isHidden()) {
			return;
		}
		List<UUID> idsToCheck = new ArrayList<>();
		idsToCheck.add(contentId);
		boolean hideParent = false;
		if (parent != null) {
			hideParent = !hasOtherVisibleSeasonForUpdate(parent.getId(), contentId);
			if (hideParent) {
				idsToCheck.add(parent.getId());
			}
		}
		if (dependencyQueryRepository.existsWatchPartyByContentIdIn(idsToCheck)) {
			throw new ContentDeletionBlockedException(contentId);
		}
		content.hide();
		publishContentLifecycleEvent(contentId, ContentLifecycleEvent.Type.DELETED);
		if (hideParent) {
			parent.hide();
			publishContentLifecycleEvent(parent.getId(), ContentLifecycleEvent.Type.DELETED);
		}
	}

	private boolean hasOtherVisibleSeasonForUpdate(UUID parentId, UUID excludedSeasonId) {
		return dependencyQueryRepository.findChildSeasonsForUpdate(parentId).stream()
			.anyMatch(season -> !season.getId().equals(excludedSeasonId)
				&& !season.isHidden());
	}

	private Content lockContentForUpdate(
		UUID contentId,
		ContentUpdateRequest request,
		boolean requireVisible
	) {
		Content snapshot = contentRepository.findById(contentId)
			.orElseThrow(() -> new ContentNotFoundException(contentId));
		if (snapshot.getType() != ContentType.TV_SEASON) {
			return contentRepository.findByIdForUpdate(contentId)
				.filter(candidate -> !requireVisible || !candidate.isHidden())
				.orElseThrow(() -> new ContentNotFoundException(contentId));
		}

		UUID previousParentId = snapshot.getParentContent().getId();
		List<UUID> parentIds = new ArrayList<>();
		parentIds.add(previousParentId);
		if (!request.isCreateNewSeries() && request.getParentContentId().isPresent()) {
			UUID targetParentId = request.getParentContentId().orElseThrow();
			if (!targetParentId.equals(previousParentId)) {
				parentIds.add(targetParentId);
			}
		}
		parentIds.stream().sorted().forEach(this::requireSeries);

		return contentRepository.findByIdForUpdate(contentId)
			.filter(candidate -> candidate.getType() == ContentType.TV_SEASON
				&& candidate.getParentContent().getId().equals(previousParentId)
				&& (!requireVisible || !candidate.isHidden()))
			.orElseThrow(() -> new ContentNotFoundException(contentId));
	}

	private ContentCreateResponse createTvSeries(
		ContentCreateRequest request,
		Map<String, MultipartFile> images,
		List<String> uploadedKeys
	) {
		Content series = Content.builder()
			.title(request.getTitle())
			.type(ContentType.TV_SERIES)
			.metadata(originalTitleMetadata(request.getOriginalTitle()))
			.build();
		contentRepository.save(series);
		List<UUID> seasonIds = new ArrayList<>();
		for (SeasonCreateRequest seasonRequest : request.getSeasons()) {
			String partName = seasonRequest.getThumbnailKey() == null
				? null
				: "thumbnails[" + seasonRequest.getThumbnailKey() + "]";
			String thumbnailUrl = uploadImage(
				partName == null ? null : images.get(partName), uploadedKeys);
			Content season = Content.builder()
				.parentContent(series)
				.title(seasonRequest.getTitle())
				.type(ContentType.TV_SEASON)
				.seasonNumber(seasonRequest.getSeasonNumber())
				.episodeCount(seasonRequest.getEpisodeCount())
				.description(seasonRequest.getDescription())
				.thumbnailUrl(thumbnailUrl)
				.releaseDate(seasonRequest.getReleaseDate())
				.build();
			contentRepository.save(season);
			replaceRelations(season, seasonRequest.getGenreIds(), seasonRequest.getTags(),
				seasonRequest.getCasts(), seasonRequest.getPlatforms(), false);
			publishContentLifecycleEvent(season.getId(), ContentLifecycleEvent.Type.UPSERTED);
			seasonIds.add(season.getId());
		}
		return ContentCreateResponse.builder()
			.seriesId(series.getId())
			.contentIds(seasonIds)
			.createdAt(series.getCreatedAt())
			.build();
	}

	private ContentCreationResult createContent(
		ContentCreateRequest request,
		String thumbnailUrl
	) {
		Content parent = null;
		if (request.getType() == ContentType.TV_SEASON) {
			parent = requireSeries(request.getParentContentId());
			Content existingSeason = contentRepository
				.findByParentContent_IdAndSeasonNumber(
					parent.getId(), request.getSeasonNumber())
				.orElse(null);
			if (existingSeason != null) {
				if (!existingSeason.isHidden()) {
					throw new ContentSeasonAlreadyExistsException(
						parent.getId(), request.getSeasonNumber());
				}
				validateEpisodeCount(existingSeason.getId(), request.getEpisodeCount());
				String previousThumbnailUrl = existingSeason.getThumbnailUrl();
				existingSeason.show();
				existingSeason.updateCommonDetails(
					request.getTitle(),
					request.getDescription(),
					request.getReleaseDate(),
					null
				);
				existingSeason.updateTvSeasonDetails(
					parent,
					request.getSeasonNumber(),
					request.getEpisodeCount(),
					null
				);
				existingSeason.replaceThumbnailUrl(thumbnailUrl);
				existingSeason.markEmbeddingSourceUpdated();
				if (previousThumbnailUrl != null
					&& !Objects.equals(previousThumbnailUrl, thumbnailUrl)) {
					registerPreviousImageCleanup(previousThumbnailUrl);
				}
				parent.show();
				return new ContentCreationResult(existingSeason, true);
			}
			parent.show();
		}
		Content content = Content.builder()
			.parentContent(parent)
			.title(request.getTitle())
			.type(request.getType())
			.seasonNumber(request.getSeasonNumber())
			.episodeCount(request.getEpisodeCount())
			.description(request.getDescription())
			.thumbnailUrl(thumbnailUrl)
			.releaseDate(request.getType() == ContentType.SPORT ? null : request.getReleaseDate())
			.runtime(request.getType() == ContentType.MOVIE ? request.getRuntime() : null)
			.metadata(request.getType() == ContentType.MOVIE
				? originalTitleMetadata(request.getOriginalTitle()) : null)
			.build();
		return new ContentCreationResult(content, false);
	}

	private void validateImages(ContentCreateRequest request, Map<String, MultipartFile> images) {
		int maxImageCount = request.getType() == ContentType.TV_SERIES ? 15 : 10;
		if (images.size() > maxImageCount) {
			throw new ContentUploadLimitExceededException();
		}
		long totalSize = images.values().stream().mapToLong(MultipartFile::getSize).sum();
		if (totalSize > 50L * 1024 * 1024) {
			throw new ContentUploadLimitExceededException();
		}
		Set<String> expectedParts = new HashSet<>();
		if (request.getType() == ContentType.TV_SERIES) {
			for (SeasonCreateRequest season : request.getSeasons()) {
				if (season.getThumbnailKey() != null) {
					expectedParts.add("thumbnails[" + season.getThumbnailKey() + "]");
				}
			}
		} else {
			expectedParts.add("thumbnail");
		}
		if (!expectedParts.containsAll(images.keySet())) {
			throw new InvalidContentSearchException();
		}
		for (String expectedPart : expectedParts) {
			if (request.getType() == ContentType.TV_SERIES && !images.containsKey(expectedPart)) {
				throw new InvalidContentSearchException();
			}
		}
		images.values().forEach(this::validateImage);
	}

	private void validateImage(MultipartFile image) {
		if (image.isEmpty() || image.getSize() > 5L * 1024 * 1024) {
			if (image.isEmpty()) {
				throw new InvalidContentImageException();
			}
			throw new ContentUploadLimitExceededException();
		}
		try {
			byte[] bytes = image.getBytes();
			if (detectImageType(bytes) == null) {
				throw new UnsupportedContentImageTypeException();
			}
		} catch (IOException exception) {
			throw new ContentStorageUnavailableException(exception);
		}
	}

	private String uploadImage(MultipartFile image, List<String> uploadedKeys) {
		if (image == null) {
			return null;
		}
		try {
			byte[] bytes = image.getBytes();
			String contentType = detectImageType(bytes);
			String extension = switch (contentType) {
				case "image/jpeg" -> "jpg";
				case "image/png" -> "png";
				case "image/webp" -> "webp";
				default -> throw new UnsupportedContentImageTypeException();
			};
			String objectKey = "content-thumbnails/" + UUID.randomUUID() + "." + extension;
			String url = contentImageStorage.upload(objectKey, bytes, contentType);
			uploadedKeys.add(objectKey);
			return url;
		} catch (IOException exception) {
			throw new ContentStorageUnavailableException(exception);
		} catch (UnsupportedContentImageTypeException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			throw new ContentStorageUnavailableException(exception);
		}
	}

	private String detectImageType(byte[] bytes) {
		if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff
			&& (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) {
			return "image/jpeg";
		}
		if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50
			&& bytes[2] == 0x4e && bytes[3] == 0x47 && bytes[4] == 0x0d
			&& bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) {
			return "image/png";
		}
		if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I'
			&& bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W'
			&& bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
			return "image/webp";
		}
		return null;
	}

	private void deleteUploadedImages(List<String> uploadedKeys) {
		for (String objectKey : uploadedKeys) {
			try {
				contentImageStorage.delete(objectKey);
			} catch (RuntimeException exception) {
				log.warn("롤백된 콘텐츠 이미지 삭제에 실패했습니다. objectKey={}", objectKey, exception);
			}
		}
	}

	private void registerImageRollbackCleanup(List<String> uploadedKeys) {
		TransactionSynchronizationManager.registerSynchronization(
			new TransactionSynchronization() {
				@Override
				public void afterCompletion(int status) {
					if (status != STATUS_COMMITTED) {
						deleteUploadedImages(uploadedKeys);
					}
				}
			}
		);
	}

	private void registerPreviousImageCleanup(String previousThumbnailUrl) {
		TransactionSynchronizationManager.registerSynchronization(
			new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					try {
						contentImageStorage.deleteByUrl(previousThumbnailUrl);
					} catch (RuntimeException exception) {
						log.warn(
							"교체된 콘텐츠 이미지 삭제에 실패했습니다. thumbnailUrl={}",
							previousThumbnailUrl,
							exception
						);
					}
				}
			}
		);
	}

	private void publishContentLifecycleEvent(UUID contentId, ContentLifecycleEvent.Type type) {
		eventPublisher.publishEvent(new ContentLifecycleEvent(
			UuidCreator.getTimeOrderedEpoch(),
			type,
			contentId,
			Instant.now()
		));
	}

	private void createSportEvent(Content content, ContentCreateRequest request) {
		SportType sportType = sportTypeRepository.findById(request.getSportTypeId())
			.orElseThrow(() -> new SportTypeNotFoundException(request.getSportTypeId()));
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
		Content parent = contentRepository.findByIdForUpdate(parentId)
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
		List<ContentCastRequest> casts,
		List<ContentPlatformCreateRequest> platforms,
		boolean replaceExistingPlatforms
	) {
		replaceGenres(content, genreIds == null ? List.of() : genreIds);
		replaceTags(content, tags == null ? List.of() : tags);
		replaceCasts(content, casts == null ? List.of() : casts);
		List<ContentPlatformCreateRequest> platformRequests =
			platforms == null ? List.of() : platforms;
		if (replaceExistingPlatforms) {
			replacePlatforms(content, platformRequests);
		} else {
			createPlatforms(content, platformRequests);
		}
	}

	private void validateEpisodeCount(UUID seasonId, Integer episodeCount) {
		if (episodeCount != null
			&& episodeRepository.countBySeason_Id(seasonId) > episodeCount) {
			throw new InvalidContentSearchException();
		}
	}

	private void createPlatforms(
		Content content,
		List<ContentPlatformCreateRequest> platformRequests
	) {
		if (platformRequests.isEmpty()) {
			return;
		}
		Set<UUID> platformIds = platformRequests.stream()
			.map(ContentPlatformCreateRequest::getPlatformId)
			.collect(java.util.stream.Collectors.toSet());
		List<Platform> platforms = platformRepository.findAllById(platformIds);
		if (platforms.size() != platformIds.size()) {
			UUID missingId = platformIds.stream()
				.filter(id -> platforms.stream().noneMatch(platform -> platform.getId().equals(id)))
				.findFirst()
				.orElseThrow();
			throw new PlatformNotFoundException(missingId);
		}
		Map<UUID, Platform> platformById = platforms.stream()
			.collect(java.util.stream.Collectors.toMap(Platform::getId, platform -> platform));
		for (Platform platform : platforms) {
			if (platform.getTmdbProviderId() == null) {
				throw new PlatformNotFoundException(platform.getId());
			}
		}
		contentPlatformRepository.saveAll(platformRequests.stream()
			.map(request -> ContentPlatform.create(
				content,
				platformById.get(request.getPlatformId()),
				ContentPlatform.PlatformSource.MANUAL,
				"KR",
				request.getUrl()
			))
			.toList());
	}

	private void replacePlatforms(
		Content content,
		List<ContentPlatformCreateRequest> platformRequests
	) {
		contentPlatformRepository.deleteAllByContentId(content.getId());
		createPlatforms(content, platformRequests);
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
		if (normalized.size() > 3) {
			throw new InvalidContentSearchException();
		}
		Map<String, ContentTag> existingByName = contentTagRepository
			.findAllWithTagByContentIdIn(List.of(content.getId())).stream()
			.collect(java.util.stream.Collectors.toMap(
				relation -> relation.getTag().getName(),
				relation -> relation
			));
		Set<String> requestedNames = new HashSet<>(normalized);
		List<ContentTag> removedRelations = existingByName.values().stream()
			.filter(relation -> !requestedNames.contains(relation.getTag().getName()))
			.toList();
		contentTagRepository.deleteAll(removedRelations);
		List<ContentTag> newRelations = normalized.stream()
			.filter(name -> !existingByName.containsKey(name))
			.map(name -> {
				Tag tag = tagRepository.findByName(name)
					.orElseGet(() -> tagRepository.save(Tag.create(name)));
				return ContentTag.create(content, tag, TagSource.MANUAL);
			})
			.toList();
		contentTagRepository.saveAll(newRelations);
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

	private static Map<String, Object> originalTitleMetadata(String originalTitle) {
		return originalTitle == null
			? null
			: Map.of(ORIGINAL_TITLE_METADATA_KEY, originalTitle);
	}

	private static Map<String, Object> updateOriginalTitle(
		Map<String, Object> currentMetadata,
		JsonNullable<String> originalTitle
	) {
		if (!originalTitle.isPresent()) {
			return currentMetadata;
		}
		Map<String, Object> updated = currentMetadata == null
			? new LinkedHashMap<>()
			: new LinkedHashMap<>(currentMetadata);
		String value = originalTitle.orElse(null);
		if (value == null) {
			updated.remove(ORIGINAL_TITLE_METADATA_KEY);
		} else {
			updated.put(ORIGINAL_TITLE_METADATA_KEY, value);
		}
		return updated.isEmpty() ? null : updated;
	}

	private static <T> T value(JsonNullable<T> wrapper, T current) {
		return wrapper.isPresent() ? wrapper.orElse(null) : current;
	}

	private record TvSeasonUpdateContext(
		Content parent,
		Integer seasonNumber,
		boolean parentChanged,
		UUID activatedParentId,
		UUID hiddenParentId
	) {
	}

	private record ContentCreationResult(
		Content content,
		boolean reusedHiddenSeason
	) {
	}
}
