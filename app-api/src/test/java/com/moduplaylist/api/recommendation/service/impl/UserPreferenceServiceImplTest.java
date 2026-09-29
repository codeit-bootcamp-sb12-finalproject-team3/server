package com.moduplaylist.api.recommendation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moduplaylist.api.recommendation.dto.UserPreferenceCreateRequest;
import com.moduplaylist.api.recommendation.service.UserContentGenrePreferenceService;
import com.moduplaylist.api.recommendation.service.UserContentTagPreferenceService;
import com.moduplaylist.api.recommendation.service.UserPlaylistGenrePreferenceService;
import com.moduplaylist.api.recommendation.service.UserPlaylistTagPreferenceService;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.recommendation.entity.RecommendationOutboxEvent;
import com.moduplaylist.core.recommendation.entity.RecommendationOutboxEventType;
import com.moduplaylist.core.recommendation.entity.RecommendationOutboxStatus;
import com.moduplaylist.core.recommendation.repository.RecommendationOutboxEventRepository;
import com.moduplaylist.core.recommendation.repository.UserPreferenceContentRepository;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceImplTest {

    @Mock
    private UserPreferenceContentRepository userPreferenceContentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ContentRepository contentRepository;
    @Mock
    private UserContentTagPreferenceService userContentTagPreferenceService;
    @Mock
    private UserContentGenrePreferenceService userContentGenrePreferenceService;
    @Mock
    private UserPlaylistTagPreferenceService userPlaylistTagPreferenceService;
    @Mock
    private UserPlaylistGenrePreferenceService userPlaylistGenrePreferenceService;
    @Mock
    private RecommendationOutboxEventRepository recommendationOutboxEventRepository;
    private UserPreferenceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserPreferenceServiceImpl(
                userPreferenceContentRepository,
                userRepository,
                contentRepository,
                userContentTagPreferenceService,
                userContentGenrePreferenceService,
                userPlaylistTagPreferenceService,
                userPlaylistGenrePreferenceService,
                recommendationOutboxEventRepository
        );
    }

    @Test
    void savesPendingOutboxAfterPreferencePersistence() {
        UUID userId = UUID.randomUUID();
        List<UUID> contentIds = List.of(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );
        User user = mock(User.class);
        List<Content> contents = contentIds.stream()
                .map(contentId -> content(contentId))
                .toList();
        UserPreferenceCreateRequest request = UserPreferenceCreateRequest.builder()
                .contentIds(contentIds)
                .build();
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(userPreferenceContentRepository.existsByUser_Id(userId)).thenReturn(false);
        when(contentRepository.findAllById(contentIds)).thenReturn(contents);

        service.createUserPreference(userId, request);

        ArgumentCaptor<RecommendationOutboxEvent> outboxCaptor =
                ArgumentCaptor.forClass(RecommendationOutboxEvent.class);
        verify(recommendationOutboxEventRepository).save(outboxCaptor.capture());
        RecommendationOutboxEvent outboxEvent = outboxCaptor.getValue();
        assertThat(outboxEvent.getEventId()).isNotNull();
        assertThat(outboxEvent.getUserId()).isEqualTo(userId);
        assertThat(outboxEvent.getEventType())
                .isEqualTo(RecommendationOutboxEventType.INITIAL_PREFERENCE_CREATED);
        assertThat(outboxEvent.getStatus()).isEqualTo(RecommendationOutboxStatus.PENDING);
        assertThat(outboxEvent.getRetryCount()).isZero();
        assertThat(outboxEvent.getNextRetryAt()).isNull();
        assertThat(outboxEvent.getProcessingStartedAt()).isNull();
        assertThat(outboxEvent.getClaimToken()).isNull();
        assertThat(outboxEvent.getLastError()).isNull();

        InOrder order = inOrder(
                userPreferenceContentRepository,
                userContentGenrePreferenceService,
                userContentTagPreferenceService,
                userPlaylistGenrePreferenceService,
                userPlaylistTagPreferenceService,
                recommendationOutboxEventRepository
        );
        order.verify(userPreferenceContentRepository).saveAll(anyList());
        order.verify(userContentGenrePreferenceService)
                .createFromInitialPreferences(user, contentIds);
        order.verify(userContentTagPreferenceService)
                .createFromInitialPreferences(user, contentIds);
        order.verify(userPlaylistGenrePreferenceService)
                .createFromInitialPreferences(user, contentIds);
        order.verify(userPlaylistTagPreferenceService)
                .createFromInitialPreferences(user, contentIds);
        order.verify(recommendationOutboxEventRepository).save(outboxEvent);
    }

    @Test
    void propagatesFailureWhenPendingOutboxPersistenceFails() {
        UUID userId = UUID.randomUUID();
        List<UUID> contentIds = List.of(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );
        User user = mock(User.class);
        List<Content> contents = contentIds.stream()
                .map(this::content)
                .toList();
        UserPreferenceCreateRequest request = UserPreferenceCreateRequest.builder()
                .contentIds(contentIds)
                .build();
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(userPreferenceContentRepository.existsByUser_Id(userId)).thenReturn(false);
        when(contentRepository.findAllById(contentIds)).thenReturn(contents);
        when(recommendationOutboxEventRepository.save(any(RecommendationOutboxEvent.class)))
                .thenThrow(new DataIntegrityViolationException("outbox insert failed"));

        assertThatThrownBy(() -> service.createUserPreference(userId, request))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Content content(UUID contentId) {
        Content content = mock(Content.class);
        when(content.getId()).thenReturn(contentId);
        when(content.getType()).thenReturn(ContentType.MOVIE);
        return content;
    }
}
