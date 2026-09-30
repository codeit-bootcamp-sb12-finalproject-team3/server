package com.moduplaylist.api.watchparty.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WatchPartyContentSummary {

    private UUID id;
    private String type;
    private String title;
    private String thumbnailUrl;
}