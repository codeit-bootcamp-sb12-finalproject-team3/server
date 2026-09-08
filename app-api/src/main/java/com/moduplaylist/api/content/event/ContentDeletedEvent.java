package com.moduplaylist.api.content.event;

import java.util.List;
import java.util.UUID;

/** DB 커밋 후 외부 검색 인덱스·캐시·S3 정리에 사용할 이벤트. */
public record ContentDeletedEvent(List<DeletedContent> contents) {
    public ContentDeletedEvent { contents = List.copyOf(contents); }
    public record DeletedContent(UUID id, String thumbnailUrl) {}
}
