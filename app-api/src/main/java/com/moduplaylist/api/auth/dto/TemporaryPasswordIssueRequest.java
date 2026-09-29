package com.moduplaylist.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TemporaryPasswordIssueRequest(
    @NotBlank @Email String email
) {
}