package com.laundryhub.backend.domain.user;

/** Account roles. The value is stored in {@code users.role} and picks the {@link User} subclass. */
public enum Role {
    /** Books laundry in the Android app. */
    CUSTOMER,
    /** Accepts pickup and return-delivery jobs in the Android app. */
    RIDER,
    /** Shop owner: full admin console access. */
    ADMIN,
    /** Counter staff: admin console without pricing and accounts. */
    STAFF
}
