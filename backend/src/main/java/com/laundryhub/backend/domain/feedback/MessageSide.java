package com.laundryhub.backend.domain.feedback;

/** Who wrote a chat message: the customer/rider who owns the conversation, or the shop (admin/staff). */
public enum MessageSide {
    /** The customer or rider. */
    USER,
    /** Any admin or staff member answering for the shop. */
    SHOP
}
