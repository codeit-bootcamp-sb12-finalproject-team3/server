package com.moduplaylist.api.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OAuth2CodeExchangeRequest {

  private String code;
}
