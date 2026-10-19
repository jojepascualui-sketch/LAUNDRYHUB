package com.laundryhub.admin.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.laundryhub.admin.api.dto.AuthResponse;
import com.laundryhub.admin.api.dto.UserProfile;

import org.junit.jupiter.api.Test;

class SessionTest {

    @Test
    void singleWordNameShowsTwoLettersLikeTheDesign() {
        Session session = Session.from(new AuthResponse("t", null,
                new UserProfile(1L, "ADMIN", "Admin", "admin@laundryhub.ph", "A", "Owner")));

        assertEquals("AD", session.initials());
        assertEquals("Owner", session.position());
    }

    @Test
    void staffWithoutPositionIsCalledStaff() {
        Session session = Session.from(new AuthResponse("t", null,
                new UserProfile(2L, "STAFF", "Rosa Cruz", "staff@laundryhub.ph", "RC", null)));

        assertEquals("RC", session.initials());
        assertEquals("Staff", session.position());
    }

    @Test
    void needsAToken() {
        assertThrows(IllegalArgumentException.class, () -> new Session(" ", "Admin", "AD", "Owner"));
    }
}
