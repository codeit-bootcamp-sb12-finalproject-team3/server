package com.moduplaylist.core.playlist.ai;

import java.time.LocalDate;

public interface AiPlaylistSeasonalContextProvider {

  AiPlaylistSeasonalContext getContext(LocalDate date);
}
