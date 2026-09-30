package com.moduplaylist.api.dm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConversationSearchRequest {

    private String cursor;
    private String idAfter;

    @Min(1)
    @Max(100)
    private int limit = 20;
}
