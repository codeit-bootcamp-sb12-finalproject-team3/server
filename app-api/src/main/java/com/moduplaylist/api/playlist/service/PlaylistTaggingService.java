package com.moduplaylist.api.playlist.service;

import com.moduplaylist.core.content.entity.ContentTag;
import com.moduplaylist.core.content.entity.Tag;
import com.moduplaylist.core.content.repository.ContentTagRepository;
import com.moduplaylist.core.playlist.entity.Playlist;
import com.moduplaylist.core.playlist.entity.PlaylistContent;
import com.moduplaylist.core.playlist.entity.PlaylistTag;
import com.moduplaylist.core.playlist.exception.PlaylistNotFoundException;
import com.moduplaylist.core.playlist.repository.PlaylistContentRepository;
import com.moduplaylist.core.playlist.repository.PlaylistRepository;
import com.moduplaylist.core.playlist.repository.PlaylistTagRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaylistTaggingService {

  private static final double CO_OCCURRENCE_THRESHOLD = 0.6;
  private static final int MIN_CO_OCCURRENCE_COUNT = 2;

  private final PlaylistContentRepository playlistContentRepository;
  private final ContentTagRepository contentTagRepository;
  private final PlaylistTagRepository playlistTagRepository;
  private final PlaylistRepository playlistRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recalculate(UUID playlistId) {
    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistNotFoundException(playlistId));

    List<PlaylistContent> playlistContents =
        playlistContentRepository.findAllByPlaylist_IdOrderByCreatedAtAscIdAsc(
            playlist.getId()
        );

    List<UUID> contentIds = playlistContents.stream()
        .map(PlaylistContent::getContent)
        .map(content -> content.getId())
        .toList();

    if (contentIds.isEmpty()) {
      playlistTagRepository.deleteAllByPlaylist_Id(playlist.getId());
      return;
    }

    List<ContentTag> contentTags =
        contentTagRepository.findAllWithTagByContentIdIn(contentIds);

    /*
     * 콘텐츠별 태그 목록
     *
     * contentA -> [힐링, 감성]
     * contentB -> [힐링, 잔잔함]
     */
    Map<UUID, List<Tag>> tagsByContent = new HashMap<>();

    for (ContentTag contentTag : contentTags) {
      UUID contentId = contentTag.getContent().getId();

      tagsByContent
          .computeIfAbsent(contentId, key -> new ArrayList<>())
          .add(contentTag.getTag());
    }

    /*
     * 태그별 등장 콘텐츠 수
     */
    Map<UUID, TagStat> tagStats = new HashMap<>();

    for (List<Tag> tags : tagsByContent.values()) {
      for (Tag tag : tags) {
        tagStats.compute(
            tag.getId(),
            (tagId, current) -> {
              if (current == null) {
                return new TagStat(tag, 1);
              }

              return new TagStat(
                  tag,
                  current.contentCount() + 1
              );
            }
        );
      }
    }

    /*
     * 태그 쌍별 동시 등장 콘텐츠 수
     */
    Map<TagPair, Integer> coOccurrenceCounts = new HashMap<>();

    for (List<Tag> tags : tagsByContent.values()) {
      for (int i = 0; i < tags.size(); i++) {
        for (int j = i + 1; j < tags.size(); j++) {
          TagPair pair = TagPair.of(
              tags.get(i).getId(),
              tags.get(j).getId()
          );

          coOccurrenceCounts.merge(pair, 1, Integer::sum);
        }
      }
    }

    List<TagStat> uniqueTags = tagStats.values().stream()
        .sorted(
            Comparator.comparing(
                    (TagStat tagStat) -> tagStat.tag().getName()
                )
                .thenComparing(tagStat -> tagStat.tag().getId())
        )
        .toList();

    List<List<TagStat>> clusters =
        buildClusters(
            uniqueTags,
            tagStats,
            coOccurrenceCounts
        );

    List<ClusterStat> clusterStats = clusters.stream()
        .map(cluster -> new ClusterStat(
            cluster,
            cluster.stream()
                .mapToInt(TagStat::contentCount)
                .sum(),
            selectRepresentative(cluster)
        ))
        .sorted(
            Comparator.comparingInt(ClusterStat::score)
                .reversed()
                .thenComparing(
                    clusterStat -> clusterStat.representative().tag().getName()
                )
                .thenComparing(
                    clusterStat -> clusterStat.representative().tag().getId()
                )
        )
        .toList();

    List<TagStat> representativeTags = clusterStats.stream()
        .limit(5)
        .map(ClusterStat::representative)
        .toList();

    playlistTagRepository.deleteAllByPlaylist_Id(playlist.getId());
    playlistTagRepository.flush();

    List<PlaylistTag> playlistTags = representativeTags.stream()
        .map(tagStat ->
            PlaylistTag.create(
                playlist,
                tagStat.tag()
            )
        )
        .toList();

    playlistTagRepository.saveAll(playlistTags);
  }

  private List<List<TagStat>> buildClusters(
      List<TagStat> tagStats,
      Map<UUID, TagStat> tagStatsById,
      Map<TagPair, Integer> coOccurrenceCounts
  ) {
    int size = tagStats.size();

    List<List<Integer>> adjacency = new ArrayList<>();

    for (int i = 0; i < size; i++) {
      adjacency.add(new ArrayList<>());
    }

    for (int i = 0; i < size; i++) {
      TagStat left = tagStats.get(i);

      for (int j = i + 1; j < size; j++) {
        TagStat right = tagStats.get(j);

        TagPair pair = TagPair.of(
            left.tag().getId(),
            right.tag().getId()
        );

        int coOccurrenceCount =
            coOccurrenceCounts.getOrDefault(pair, 0);

        int minContentCount = Math.min(
            tagStatsById.get(left.tag().getId()).contentCount(),
            tagStatsById.get(right.tag().getId()).contentCount()
        );

        double ratio =
            minContentCount == 0
                ? 0.0
                : (double) coOccurrenceCount / minContentCount;

        if (coOccurrenceCount >= MIN_CO_OCCURRENCE_COUNT
            && ratio >= CO_OCCURRENCE_THRESHOLD) {
          adjacency.get(i).add(j);
          adjacency.get(j).add(i);
        }
      }
    }

    boolean[] visited = new boolean[size];
    List<List<TagStat>> clusters = new ArrayList<>();

    for (int i = 0; i < size; i++) {
      if (visited[i]) {
        continue;
      }

      List<TagStat> cluster = new ArrayList<>();
      List<Integer> stack = new ArrayList<>();

      stack.add(i);
      visited[i] = true;

      while (!stack.isEmpty()) {
        int current = stack.remove(stack.size() - 1);

        cluster.add(tagStats.get(current));

        for (int next : adjacency.get(current)) {
          if (!visited[next]) {
            visited[next] = true;
            stack.add(next);
          }
        }
      }

      clusters.add(cluster);
    }

    return clusters;
  }

  private TagStat selectRepresentative(List<TagStat> members) {
    return members.stream()
        .sorted(
            Comparator.comparingInt(TagStat::contentCount)
                .reversed()
                .thenComparing(tagStat -> tagStat.tag().getName())
                .thenComparing(tagStat -> tagStat.tag().getId())
        )
        .findFirst()
        .orElseThrow();
  }

  private record TagStat(
      Tag tag,
      int contentCount
  ) {
  }

  private record ClusterStat(
      List<TagStat> members,
      int score,
      TagStat representative
  ) {
  }

  private record TagPair(
      UUID first,
      UUID second
  ) {

    private static TagPair of(
        UUID left,
        UUID right
    ) {
      if (left.compareTo(right) <= 0) {
        return new TagPair(left, right);
      }

      return new TagPair(right, left);
    }
  }
}