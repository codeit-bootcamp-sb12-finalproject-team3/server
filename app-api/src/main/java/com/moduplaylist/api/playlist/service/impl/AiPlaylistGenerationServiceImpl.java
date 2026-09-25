package com.moduplaylist.api.playlist.service.impl;

import com.moduplaylist.api.playlist.dto.AiPlaylistCreateRequest;
import com.moduplaylist.api.playlist.dto.PlaylistResponse;
import com.moduplaylist.api.playlist.service.AiPlaylistGenerationService;
import com.moduplaylist.api.playlist.service.PlaylistResponseAssembler;
import com.moduplaylist.core.playlist.ai.AiPlaylistCandidate;
import com.moduplaylist.core.playlist.ai.AiPlaylistCandidateProvider;
import com.moduplaylist.core.playlist.ai.AiPlaylistGenerationResult;
import com.moduplaylist.core.playlist.ai.AiPlaylistGenerator;
import com.moduplaylist.core.playlist.entity.Playlist;
import com.moduplaylist.core.playlist.exception.InvalidAiPlaylistContentResultException;
import com.moduplaylist.core.playlist.policy.PlaylistContentPolicy;
import com.moduplaylist.core.playlist.service.AiPlaylistGenerationValidator;
import com.moduplaylist.core.playlist.service.AiPlaylistPersistenceService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiPlaylistGenerationServiceImpl implements AiPlaylistGenerationService {

  private final AiPlaylistGenerator aiPlaylistGenerator;
  private final AiPlaylistCandidateProvider candidateProvider;
  private final PlaylistResponseAssembler playlistResponseAssembler;
  private final AiPlaylistPersistenceService aiPlaylistPersistenceService;
  private final AiPlaylistGenerationValidator aiPlaylistGenerationValidator;

  @Override
  public PlaylistResponse create(
      UUID userId,
      AiPlaylistCreateRequest request
  ) {
    String theme = request.getTheme();

    List<AiPlaylistCandidate> candidates = candidateProvider.findCandidates(theme);

    if (candidates.size() < PlaylistContentPolicy.MIN_CONTENT_COUNT) {
      throw new InvalidAiPlaylistContentResultException(
          "플레이리스트를 생성할 수 있는 콘텐츠 후보가 부족합니다."
      );
    }

    AiPlaylistGenerationResult result = aiPlaylistGenerator.generate(theme, candidates);

    aiPlaylistGenerationValidator.validate(result, candidates);

    Playlist playlist = aiPlaylistPersistenceService.save(
        userId,
        result.getTitle(),
        result.getDescription(),
        result.getContentIds(),
        result.getTags()
    );

    return playlistResponseAssembler.toResponse(playlist, userId);
  }
}
