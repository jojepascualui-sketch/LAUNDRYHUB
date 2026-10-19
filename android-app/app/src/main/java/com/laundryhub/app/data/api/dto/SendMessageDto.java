package com.laundryhub.app.data.api.dto;

/** Body of {@code POST /api/feedback/messages}. */
public final class SendMessageDto {

    // OOP: Encapsulation — an immutable request.
    private final String body;

    /**
     * @param body the message
     */
    public SendMessageDto(String body) {
        this.body = body;
    }
}
