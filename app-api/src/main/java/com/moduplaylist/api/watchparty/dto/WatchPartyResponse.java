package com.moduplaylist.api.watchparty.dto;

import java.time.Instant;
import java.util.UUID;

public class WatchPartyResponse {

    private UUID id;
    private HostSummary host;
    private ContentSummary content;
    private String title;
    private String description;
    private Instant scheduledAt;
    private String status;              // SCHEDULED / LIVE / ENDED
    private Integer maxParticipants;
    private Integer currentParticipantCount; // 저장값 아님 - JOINED 상태 count로 계산
    private Integer startEpisode;
    private Integer endEpisode;
    private Instant createdAt;
    private Instant endedAt;

    public WatchPartyResponse(UUID id, HostSummary host, ContentSummary content,
                              String title, String description, Instant scheduledAt,
                              String status, Integer maxParticipants, Integer currentParticipantCount,
                              Integer startEpisode, Integer endEpisode,
                              Instant createdAt, Instant endedAt) {
        this.id = id;
        this.host = host;
        this.content = content;
        this.title = title;
        this.description = description;
        this.scheduledAt = scheduledAt;
        this.status = status;
        this.maxParticipants = maxParticipants;
        this.currentParticipantCount = currentParticipantCount;
        this.startEpisode = startEpisode;
        this.endEpisode = endEpisode;
        this.createdAt = createdAt;
        this.endedAt = endedAt;
    }


    //공용 UserSummary 생기면 이 내부 클래스 대신 그걸로 교체
    public static class HostSummary {
        private UUID userId;
        private String name;
        private String profileImageUrl;

        public HostSummary(UUID userId, String name, String profileImageUrl) {
            this.userId = userId;
            this.name = name;
            this.profileImageUrl = profileImageUrl;
        }
    }

    public static class ContentSummary {
        private UUID id;
        // Content 엔티티에서 type 필드 타입 확인하기 (지금은 String이지만, enum으로 매핑되어 있음 변경해야 함)
        private String type;
        private String title;
        private String thumbnailUrl;

        public ContentSummary(UUID id, String type, String title, String thumbnailUrl) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.thumbnailUrl = thumbnailUrl;  //평점이나 태그 보여주고 싶음 추후 추가 에정(프론트 관련)
        }
    }
}
