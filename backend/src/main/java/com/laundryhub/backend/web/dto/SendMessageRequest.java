package com.laundryhub.backend.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of a new chat message.
 *
 * @param body 1–1000 characters
 */
public record SendMessageRequest(@NotBlank @Size(max = 1000) String body) {
}
