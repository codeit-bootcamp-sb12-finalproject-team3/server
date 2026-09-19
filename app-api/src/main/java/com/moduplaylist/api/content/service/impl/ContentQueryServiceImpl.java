package com.moduplaylist.api.content.service.impl;

import com.moduplaylist.api.content.dto.ContentSearchRequest;
import com.moduplaylist.api.content.dto.ContentSort;
import com.moduplaylist.api.content.dto.ContentSummaryResponse;
import com.moduplaylist.api.content.dto.ContentTypeFilter;
import com.moduplaylist.api.content.dto.GenreResponse;
import com.moduplaylist.api.content.dto.SportTypeResponse;
import com.moduplaylist.api.content.dto.PlatformResponse;
import com.moduplaylist.api.content.dto.ContentSeriesSearchResponse;
import com.moduplaylist.api.content.dto.ContentSeriesSuggestionResponse;
import com.moduplaylist.api.content.dto.TagResponse;
import com.moduplaylist.api.content.dto.CastResponse;
import com.moduplaylist.api.content.dto.ContentAutocompleteRequest;
import com.moduplaylist.api.content.dto.ContentAutocompleteResponse;
import com.moduplaylist.api.content.dto.ContentPlatformItemResponse;
import com.moduplaylist.api.content.dto.ContentPlatformResponse;
import com.moduplaylist.api.content.dto.ContentPlaylistResponse;
import com.moduplaylist.api.content.dto.ContentResponse;
import com.moduplaylist.api.content.dto.ContentSearchSuggestionResponse;
import com.moduplaylist.api.content.dto.ContentWatchPartyResponse;
import com.moduplaylist.api.content.dto.MovieDetail;
import com.moduplaylist.api.content.dto.SportDetail;
import com.moduplaylist.api.content.dto.TvSeasonDetail;
import com.moduplaylist.api.content.service.ContentQueryService;
import com.moduplaylist.api.content.service.ContentSummaryResponseAssembler;
import com.moduplaylist.api.content.service.ContentViewActivityService;
import com.moduplaylist.api.content.service.ContentAutocompletePopularityScoreProvider;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.dto.SortDirection;
import com.moduplaylist.api.playlist.dto.PlaylistSummaryResponse;
import com.moduplaylist.api.playlist.service.PlaylistService;
import com.moduplaylist.api.watchparty.service.WatchPartyService;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.entity.Genre;
import com.moduplaylist.core.content.entity.SportEvent;
import com.moduplaylist.core.content.entity.SportType;
import com.moduplaylist.core.content.entity.Platform;
import com.moduplaylist.core.content.exception.ContentSearchUnavailableException;
import com.moduplaylist.core.content.exception.GenreNotFoundException;
import com.moduplaylist.core.content.exception.InvalidContentSearchException;
import com.moduplaylist.core.content.exception.ContentNotFoundException;
import com.moduplaylist.core.content.exception.ContentTypeNotSupportedException;
import com.moduplaylist.core.content.exception.ContentTypeNotViewableException;
import com.moduplaylist.core.content.repository.ContentLikeRepository;
import com.moduplaylist.core.content.repository.ContentQueryRepository.SearchResult;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.content.repository.ContentSearch;
import com.moduplaylist.core.content.repository.ContentRelationRepository;
import com.moduplaylist.core.content.repository.EpisodeRepository;
import com.moduplaylist.core.content.repository.GenreRepository;
import com.moduplaylist.core.content.repository.SportEventRepository;
import com.moduplaylist.core.content.repository.SportTypeRepository;
import com.moduplaylist.core.content.repository.PlatformRepository;
import com.moduplaylist.core.playlist.repository.PlaylistSearch;
import com.moduplaylist.infrastructure.opensearch.content.ContentKeywordSearchRepository;
import com.moduplaylist.infrastructure.opensearch.content.ContentAutocompleteCandidate;
import com.moduplaylist.infrastructure.redis.recommendation.ContentRecommendationRedisRepository;
import com.moduplaylist.infrastructure.redis.recommendation.RecommendationCachePage;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentQueryServiceImpl implements ContentQueryService {

	private static final ContentSort DEFAULT_SORT = ContentSort.LATEST;
	private static final int AUTOCOMPLETE_LIMIT = 10;
	private static final int AUTOCOMPLETE_CANDIDATE_LIMIT = 30;
	private static final int ADMIN_SERIES_SEARCH_LIMIT = 10;

	private final ContentRepository contentRepository;
	private final GenreRepository genreRepository;
	private final SportTypeRepository sportTypeRepository;
	private final PlatformRepository platformRepository;
	private final ContentLikeRepository contentLikeRepository;
	private final EpisodeRepository episodeRepository;
	private final SportEventRepository sportEventRepository;
	private final ContentRelationRepository contentRelationRepository;
	private final ContentViewActivityService contentViewActivityService;
	private final PlaylistService playlistService;
	private final WatchPartyService watchPartyService;
	private final ObjectProvider<ContentKeywordSearchRepository> keywordSearchRepositoryProvider;
	private final ObjectProvider<ContentAutocompletePopularityScoreProvider>
		popularityScoreProvider;
	private final ContentRecommendationRedisRepository recommendationRedisRepository;
	private final ContentSummaryResponseAssembler contentSummaryResponseAssembler;

	@Override
	@Transactional(readOnly = true)
	public CursorPageResponse<ContentSummaryResponse> findAll(
		UUID userId,
		ContentSearchRequest request
	) {
		ContentType contentType = request.getTypeEqual() == null
			? null
			: request.getTypeEqual().toQueryType();
		ContentSort sort = request.getSortBy() == null ? DEFAULT_SORT : request.getSortBy();

		validateGenre(request.getGenreIdEqual());
		validateSportType(request.getSportTypeEqual());

		List<UUID> matchedContentIds = findMatchedContentIds(
			request.getKeywordLike(),
			contentType,
			request.getLikedByUserIdEqual()
		);
		ParsedCursor cursor = parseCursor(
			request.getCursor(),
			request.getIdAfter(),
			sort,
			request.getLikedByUserIdEqual() != null
		);

		ContentSearch search = new ContentSearch(
			contentType,
			request.getGenreIdEqual(),
			request.getSportTypeEqual(),
			request.getLikedByUserIdEqual(),
			matchedContentIds,
			request.getLikedByUserIdEqual() == null ? toRepositorySort(sort) : null,
			cursor.createdAt(),
			cursor.likedAt(),
			cursor.rating(),
			cursor.reviewCount(),
			request.getIdAfter(),
			request.getLimit()
		);

		SearchResult result = contentRepository.search(search);
		List<ContentSummaryResponse> data = contentSummaryResponseAssembler
			.toResponses(result.getContents(), userId);
		Content lastContent = result.getContents().isEmpty()
			? null
			: result.getContents().get(result.getContents().size() - 1);

		return CursorPageResponse.<ContentSummaryResponse>builder()
			.data(data)
			.nextCursor(nextCursor(result, lastContent, sort, search.isLikedContentsSearch()))
			.nextIdAfter(result.isHasNext() && lastContent != null ? lastContent.getId() : null)
			.hasNext(result.isHasNext())
			.totalCount(result.getTotalCount())
			.sortBy(search.isLikedContentsSearch() ? "likedAt" : sort.getValue())
			.sortDirection(SortDirection.DESCENDING)
			.build();
	}

	@Override
	@Transactional(readOnly = true)
	public List<GenreResponse> findGenres(ContentTypeFilter type) {
		if (type != ContentTypeFilter.MOVIE && type != ContentTypeFilter.TV_SERIES) {
			throw new InvalidContentSearchException();
		}
		return genreRepository.findAllByOrderByNameAsc().stream()
			.map(this::toGenreResponse)
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<SportTypeResponse> findSportTypes() {
		return sportTypeRepository.findAllByOrderByNameAsc().stream()
			.map(this::toSportTypeResponse)
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<PlatformResponse> findPlatforms() {
		return platformRepository.findAllByTmdbProviderIdNotNullOrderByNameAsc().stream()
			.map(this::toPlatformResponse)
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public ContentSeriesSearchResponse searchSeriesForAdmin(String query) {
		String normalized = query == null ? "" : query.strip();
		if (normalized.isEmpty() || normalized.length() > 255) {
			throw new InvalidContentSearchException();
		}
		List<Content> matches = contentRepository.searchSeriesForAdmin(
			ContentType.TV_SERIES,
			normalized,
			PageRequest.of(0, ADMIN_SERIES_SEARCH_LIMIT)
		);
		List<ContentSeriesSuggestionResponse> data = matches.stream()
			.map(content -> ContentSeriesSuggestionResponse.builder()
				.id(content.getId())
				.title(content.getTitle())
				.build())
			.toList();
		return ContentSeriesSearchResponse.builder()
			.data(data)
			.build();
	}

	@Override
	@Transactional(readOnly = true)
	public ContentResponse findById(UUID userId, UUID contentId) {
		ContentResponse response = findByIdForCommand(contentId);
		contentViewActivityService.record(userId, contentId);
		return response;
	}

	@Override
	@Transactional(readOnly = true)
	public ContentResponse findByIdForCommand(UUID contentId) {
		Content content = findVisibleContent(contentId);
		if (content.getType() == ContentType.TV_SERIES) {
			throw new ContentTypeNotViewableException(contentId, content.getType());
		}

		List<GenreResponse> genres = List.of();
		List<TagResponse> tags = List.of();
		List<CastResponse> cast = List.of();
		MovieDetail movie = null;
		TvSeasonDetail tvSeason = null;
		SportDetail sport = null;

		if (content.getType() == ContentType.SPORT) {
			sport = sportEventRepository.findWithSportTypeByContentId(contentId)
				.map(this::toSportDetail)
				.orElseThrow(() -> new IllegalStateException(
					"스포츠 콘텐츠에 경기 정보가 없습니다. contentId=" + contentId
				));
		} else {
			genres = findGenres(contentId);
			tags = findTags(contentId);
			cast = findCast(contentId);
			if (content.getType() == ContentType.MOVIE) {
				movie = MovieDetail.builder()
					.runtime(content.getRuntime())
					.build();
			} else {
				tvSeason = TvSeasonDetail.builder()
					.parentContentId(content.getParentContent().getId())
					.seasonNumber(content.getSeasonNumber())
					.episodeCount(content.getEpisodeCount())
					.registeredEpisodeCount(Math.toIntExact(episodeRepository.countBySeason_Id(contentId)))
					.build();
			}
		}

		return ContentResponse.builder()
			.id(content.getId())
			.type(content.getType())
			.title(content.getTitle())
			.description(content.getDescription())
			.thumbnailUrl(content.getThumbnailUrl())
			.releaseDate(content.getReleaseDate())
			.averageRating(content.getAverageRating())
			.reviewCount(content.getReviewCount())
			.likeCount(content.getLikeCount())
			.originalTitle(findOriginalTitle(content))
			.genres(genres)
			.tags(tags)
			.cast(cast)
			.movie(movie)
			.tvSeason(tvSeason)
			.sport(sport)
			.createdAt(content.getCreatedAt())
			.updatedAt(content.getUpdatedAt())
			.build();
	}

	private String findOriginalTitle(Content content) {
		if (content.getType() == ContentType.TV_SEASON) {
			return content.getParentContent().getOriginalTitle();
		}
		return content.getOriginalTitle();
	}

	@Override
	@Transactional(readOnly = true)
	public ContentPlatformResponse findOtt(UUID contentId) {
		Content content = requireMovieOrSeason(contentId);
		List<ContentPlatformItemResponse> otts = contentRelationRepository
			.platforms(content.getId()).stream()
			.map(value -> ContentPlatformItemResponse.builder()
				.platformId(value.getId())
				.name(value.getName())
				.logoUrl(value.getLogoUrl())
				.url(value.getUrl())
				.build())
			.toList();
		return ContentPlatformResponse.builder()
			.regionCode("KR")
			.otts(otts)
			.build();
	}

	@Override
	@Transactional(readOnly = true)
	public ContentPlaylistResponse findPlaylists(UUID userId, UUID contentId) {
		requireMovieOrSeason(contentId);
		PlaylistSearch search = new PlaylistSearch(
			null,
			null,
			contentId,
			null,
			null,
			null,
			20,
			PlaylistSearch.Sort.WEEKLY_POPULARITY_SCORE,
			PlaylistSearch.Direction.DESCENDING
		);
		CursorPageResponse<PlaylistSummaryResponse> result = playlistService.findAll(userId, search);
		return ContentPlaylistResponse.builder()
			.data(result.getData())
			.hasMore(Boolean.TRUE.equals(result.getHasNext()))
			.build();
	}

	@Override
	@Transactional(readOnly = true)
	public ContentWatchPartyResponse findWatchParties(UUID contentId) {
		requireMovieOrSeason(contentId);
		return watchPartyService.getWatchPartiesForContentWidget(contentId);
	}

	@Override
	@Transactional(readOnly = true)
	public ContentAutocompleteResponse autocomplete(UUID userId, ContentAutocompleteRequest request) {
		ContentKeywordSearchRepository repository = keywordSearchRepositoryProvider.getIfAvailable();
		if (repository == null) {
			throw new ContentSearchUnavailableException();
		}
		List<ContentAutocompleteCandidate> candidates = repository.findAutocompleteCandidates(
			request.getQuery(),
			AUTOCOMPLETE_CANDIDATE_LIMIT
		);
		if (candidates.isEmpty()) {
			return ContentAutocompleteResponse.builder().build();
		}
		List<UUID> candidateIds = candidates.stream()
			.map(ContentAutocompleteCandidate::contentId)
			.distinct()
			.toList();
		Map<UUID, Integer> personalizedRankById = findPersonalizedRanks(userId);
		Map<UUID, Double> popularityScoreById = findPopularityScores(candidateIds);
		Set<UUID> visibleContentIds = contentRepository.findAllById(candidateIds).stream()
			.filter(content -> !content.isHidden() && content.getType() != ContentType.TV_SERIES)
			.map(Content::getId)
			.collect(java.util.stream.Collectors.toSet());
		Set<String> seenTexts = new HashSet<>();

		List<ContentSearchSuggestionResponse> suggestions = candidates.stream()
			.filter(candidate -> visibleContentIds.contains(candidate.contentId()))
			.sorted(Comparator
				.comparingInt(ContentAutocompleteCandidate::matchRank)
				.thenComparingInt(candidate -> personalizedRankById.getOrDefault(
					candidate.contentId(), Integer.MAX_VALUE))
				.thenComparing(Comparator.comparingDouble(
					(ContentAutocompleteCandidate candidate) -> popularityScoreById
						.getOrDefault(candidate.contentId(), 0.0)).reversed())
				.thenComparing(ContentAutocompleteCandidate::text, String.CASE_INSENSITIVE_ORDER)
				.thenComparing(ContentAutocompleteCandidate::contentId))
			.filter(candidate -> seenTexts.add(candidate.text().toLowerCase(Locale.ROOT)))
			.limit(AUTOCOMPLETE_LIMIT)
			.map(candidate -> ContentSearchSuggestionResponse.builder()
				.text(candidate.text())
				.type(candidate.type())
				.build())
			.toList();
		return ContentAutocompleteResponse.builder().suggestions(suggestions).build();
	}

	private Map<UUID, Integer> findPersonalizedRanks(UUID userId) {
		try {
			RecommendationCachePage page = recommendationRedisRepository.findPage(
				userId,
				0,
				AUTOCOMPLETE_CANDIDATE_LIMIT
			);
			Map<UUID, Integer> ranks = new HashMap<>();
			for (int index = 0; index < page.contentIds().size(); index++) {
				ranks.putIfAbsent(page.contentIds().get(index), index);
			}
			return ranks;
		} catch (RuntimeException exception) {
			log.warn("자동완성 개인화 순위를 조회하지 못했습니다. userId={}", userId, exception);
			return Map.of();
		}
	}

	private Map<UUID, Double> findPopularityScores(List<UUID> contentIds) {
		ContentAutocompletePopularityScoreProvider provider = popularityScoreProvider.getIfAvailable();
		if (provider == null) {
			// TODO: 트렌딩 도메인의 Redis ZSet 구현이 완료되면 ZMSCORE 기반 provider를 연결한다.
			return Map.of();
		}
		try {
			return provider.findScores(contentIds);
		} catch (RuntimeException exception) {
			// 트렌딩 장애가 자동완성 실패로 이어지지 않도록 제목 정렬로 대체한다.
			log.warn("자동완성 트렌딩 점수를 조회하지 못했습니다.", exception);
			return Map.of();
		}
	}

	private Content findVisibleContent(UUID contentId) {
		return contentRepository.findByIdAndHiddenFalse(contentId)
			.orElseThrow(() -> new ContentNotFoundException(contentId));
	}

	private Content requireMovieOrSeason(UUID contentId) {
		Content content = findVisibleContent(contentId);
		if (content.getType() != ContentType.MOVIE && content.getType() != ContentType.TV_SEASON) {
			throw new ContentTypeNotSupportedException(contentId, content.getType());
		}
		return content;
	}

	private List<GenreResponse> findGenres(UUID contentId) {
		return contentRelationRepository.genres(contentId).stream()
			.map(value -> GenreResponse.builder().id(value.getId()).name(value.getName()).build())
			.toList();
	}

	private List<TagResponse> findTags(UUID contentId) {
		return contentRelationRepository.tags(contentId).stream()
			.map(value -> TagResponse.builder()
				.id(value.getId()).name(value.getName()).build())
			.toList();
	}

	private List<CastResponse> findCast(UUID contentId) {
		return contentRelationRepository.casts(contentId).stream()
			.map(value -> CastResponse.builder()
				.name(value.getName())
				.roleName(value.getRoleName())
				.profileImageUrl(value.getProfileImageUrl())
				.build())
			.toList();
	}

	private SportDetail toSportDetail(SportEvent event) {
		return SportDetail.builder()
			.sportType(toSportTypeResponse(event.getSportType()))
			.scheduledAt(event.getScheduledAt())
			.league(event.getLeagueName())
			.season(event.getSeason())
			.round(event.getRound())
			.homeTeam(event.getHomeTeamName())
			.awayTeam(event.getAwayTeamName())
			.venue(event.getVenue())
			.country(event.getCountry())
			.homeScore(event.getHomeScore())
			.awayScore(event.getAwayScore())
			.build();
	}

	private void validateGenre(UUID genreId) {
		if (genreId != null && !genreRepository.existsById(genreId)) {
			throw new GenreNotFoundException(genreId);
		}
	}

	private void validateSportType(String sportType) {
		if (sportType != null && !sportType.isBlank()
			&& sportTypeRepository.findByCode(sportType).isEmpty()) {
			throw new InvalidContentSearchException();
		}
	}

	private List<UUID> findMatchedContentIds(
		String keyword,
		ContentType contentType,
		UUID likedByUserId
	) {
		if (keyword == null || keyword.isBlank()) {
			return null;
		}
		ContentKeywordSearchRepository repository =
			keywordSearchRepositoryProvider.getIfAvailable();
		if (repository == null) {
			throw new ContentSearchUnavailableException();
		}
		String contentTypeValue = contentType == null ? null : contentType.getValue();
		if (likedByUserId == null) {
			return repository.findContentIds(keyword, contentTypeValue);
		}
		return repository.findContentIdsWithin(
			keyword,
			contentTypeValue,
			contentLikeRepository.findContentIdsByUserId(likedByUserId)
		);
	}

	private ParsedCursor parseCursor(
		String value,
		UUID idAfter,
		ContentSort sort,
		boolean likedSearch
	) {
		if (value == null || value.isBlank()) {
			if (idAfter != null) {
				throw new InvalidContentSearchException();
			}
			return ParsedCursor.empty();
		}
		if (idAfter == null) {
			throw new InvalidContentSearchException();
		}

		try {
			if (likedSearch) {
				return ParsedCursor.liked(Instant.parse(value));
			}
			if (sort == ContentSort.LATEST) {
				return ParsedCursor.latest(Instant.parse(value));
			}
			String[] parts = value.split("\\|", -1);
			if (parts.length != 2) {
				throw new InvalidContentSearchException();
			}
			BigDecimal rating = new BigDecimal(parts[0]);
			long reviewCount = Long.parseLong(parts[1]);
			if (rating.signum() < 0 || rating.compareTo(new BigDecimal("5.00")) > 0
				|| reviewCount < 0) {
				throw new InvalidContentSearchException();
			}
			return ParsedCursor.rating(rating, reviewCount);
		} catch (DateTimeParseException | NumberFormatException exception) {
			throw new InvalidContentSearchException();
		}
	}

	private ContentSearch.Sort toRepositorySort(ContentSort sort) {
		return sort == ContentSort.LATEST
			? ContentSearch.Sort.LATEST
			: ContentSearch.Sort.RATING;
	}

	private String nextCursor(
		SearchResult result,
		Content lastContent,
		ContentSort sort,
		boolean likedSearch
	) {
		if (!result.isHasNext() || lastContent == null) {
			return null;
		}
		if (likedSearch) {
			return result.getNextCursorLikedAt().toString();
		}
		if (sort == ContentSort.LATEST) {
			return lastContent.getCreatedAt().toString();
		}
		return lastContent.getAverageRating().toPlainString()
			+ "|"
			+ lastContent.getReviewCount();
	}

	private GenreResponse toGenreResponse(Genre genre) {
		return GenreResponse.builder()
			.id(genre.getId())
			.name(genre.getName())
			.build();
	}

	private SportTypeResponse toSportTypeResponse(SportType sportType) {
		return SportTypeResponse.builder()
			.id(sportType.getId())
			.code(sportType.getCode())
			.name(sportType.getName())
			.build();
	}

	private PlatformResponse toPlatformResponse(Platform platform) {
		return PlatformResponse.builder()
			.id(platform.getId())
			.name(platform.getName())
			.logoUrl(platform.getLogoUrl())
			.build();
	}

	private record ParsedCursor(
		Instant createdAt,
		Instant likedAt,
		BigDecimal rating,
		Long reviewCount
	) {
		private static ParsedCursor empty() {
			return new ParsedCursor(null, null, null, null);
		}

		private static ParsedCursor latest(Instant createdAt) {
			return new ParsedCursor(createdAt, null, null, null);
		}

		private static ParsedCursor liked(Instant likedAt) {
			return new ParsedCursor(null, likedAt, null, null);
		}

		private static ParsedCursor rating(BigDecimal rating, long reviewCount) {
			return new ParsedCursor(null, null, rating, reviewCount);
		}
	}
}
