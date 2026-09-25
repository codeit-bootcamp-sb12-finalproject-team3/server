package com.moduplaylist.core.playlist.ai;

import java.util.List;

public interface AiPlaylistCandidateProvider {

  List<AiPlaylistCandidate> findCandidates(String theme);
}
