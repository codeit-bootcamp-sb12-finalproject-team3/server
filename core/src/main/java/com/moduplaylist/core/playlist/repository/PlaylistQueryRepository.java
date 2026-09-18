package com.moduplaylist.core.playlist.repository;

import com.moduplaylist.core.playlist.entity.Playlist;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

public interface PlaylistQueryRepository {

  SearchResult search(PlaylistSearch search);

  List<Item> findAllByIds(List<UUID> playlistIds);

  @Getter
  class SearchResult {

    private final List<Item> items;
    private final long totalCount;
    private final boolean hasNext;

    public SearchResult(
        List<Item> items,
        long totalCount,
        boolean hasNext
    ) {
      this.items = List.copyOf(items);
      this.totalCount = totalCount;
      this.hasNext = hasNext;
    }
  }

  @Getter
  class Item {
    private final Playlist playlist;
    private final long subscriberCount;
    private final long contentCount;

    public Item(
        Playlist playlist,
        long subscriberCount,
        long contentCount
    ) {
      this.playlist = playlist;
      this.subscriberCount = subscriberCount;
      this.contentCount = contentCount;
    }
  }
}
