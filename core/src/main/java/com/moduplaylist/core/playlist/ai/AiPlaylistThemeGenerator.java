package com.moduplaylist.core.playlist.ai;

import java.time.LocalDate;
import java.util.List;

public interface AiPlaylistThemeGenerator {

  String generate(
      LocalDate date,
      List<String> existingPlaylistTitles
  );
}
