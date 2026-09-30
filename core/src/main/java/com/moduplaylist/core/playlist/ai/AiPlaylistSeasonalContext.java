package com.moduplaylist.core.playlist.ai;

public record AiPlaylistSeasonalContext(
    String holidaySummary,
    String weatherSummary
) {

  public AiPlaylistSeasonalContext {
    holidaySummary = holidaySummary == null ? "" : holidaySummary;
    weatherSummary = weatherSummary == null ? "" : weatherSummary;
  }

  public static AiPlaylistSeasonalContext empty() {
    return new AiPlaylistSeasonalContext("", "");
  }
}
