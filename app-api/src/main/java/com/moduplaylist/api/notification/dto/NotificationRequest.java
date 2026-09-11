package com.moduplaylist.api.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private String cursor;
    private String idAfter;
    private int limit;
    private String sortDirection;
}
