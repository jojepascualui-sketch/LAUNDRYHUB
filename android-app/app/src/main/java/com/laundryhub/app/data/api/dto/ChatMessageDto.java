package com.laundryhub.app.data.api.dto;

/** One message of {@code /api/feedback/messages}. Filled by Gson. */
public final class ChatMessageDto {

    // OOP: Encapsulation — read-only fields set by Gson.
    private Long id;
    private String side;
    private String senderName;
    private boolean mine;
    private String body;
    private String sentAt;

    /** For Gson. */
    public ChatMessageDto() {
    }

    /** @return message id */
    public Long getId() {
        return id;
    }

    /** @return USER or SHOP */
    public String getSide() {
        return side;
    }

    /** @return who wrote it */
    public String getSenderName() {
        return senderName;
    }

    /** @return whether the viewer wrote it */
    public boolean isMine() {
        return mine;
    }

    /** @return the text */
    public String getBody() {
        return body;
    }

    /** @return ISO date-time */
    public String getSentAt() {
        return sentAt;
    }
}
