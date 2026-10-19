package com.laundryhub.backend.web.dto;

import java.util.List;

/**
 * A conversation's messages for one viewer.
 *
 * @param threadId conversation id ({@code null} before the first message)
 * @param messages oldest first
 */
public record ChatResponse(Long threadId, List<ChatMessageResponse> messages) {
}
