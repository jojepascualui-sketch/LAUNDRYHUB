package com.laundryhub.admin.model;

/** Account roles (CLAUDE.md §4). Admin and staff use this console. */
public enum Role {
    /** Orders laundry in the Android app. */
    CUSTOMER,
    /** Picks up and delivers in the Android app. */
    RIDER,
    /** Owner: everything in the console. */
    ADMIN,
    /** Counter staff: orders in the console. */
    STAFF
}
