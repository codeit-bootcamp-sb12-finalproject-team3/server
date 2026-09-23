package com.moduplaylist.core.playlist.ai;

import java.util.List;

public interface AiPlaylistGenerator {

  AiPlaylistGenerationResult generate(
      String theme,
      List<AiPlaylistCandidate> candidates
  );
}
