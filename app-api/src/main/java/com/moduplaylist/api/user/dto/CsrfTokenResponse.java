package com.moduplaylist.api.user.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CsrfTokenResponse {

  private final String csrfToken;
}
