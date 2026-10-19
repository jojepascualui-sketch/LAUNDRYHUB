package com.laundryhub.app.data.api.dto;

import java.util.Collections;
import java.util.List;

/** {@code GET /api/feedback/messages}. Filled by Gson. */
public final class ChatDto {

    // OOP: Encapsulation — read-only fields set by Gson.
    private Long threadId;
    private List<ChatMessageDto> messages;

    /** For Gson. */
    public ChatDto() {
    }

    /** @return conversation id, or {@code null} before the first message */
    public Long getThreadId() {
        return threadId;
    }

    /** @return messages, oldest first */
    public List<ChatMessageDto> getMessages() {
        return messages == null ? Collections.emptyList() : messages;
    }
}
