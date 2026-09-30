package com.moduplaylist.api.dm.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DirectMessageReadRequest {

    @NotNull
    private UUID lastReadMessageId;
}
