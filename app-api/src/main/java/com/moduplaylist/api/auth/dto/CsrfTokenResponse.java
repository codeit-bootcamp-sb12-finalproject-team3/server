package com.moduplaylist.api.auth.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CsrfTokenResponse {

  private final String csrfToken;
}
