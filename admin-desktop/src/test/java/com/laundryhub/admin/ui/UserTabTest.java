package com.laundryhub.admin.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.laundryhub.admin.api.dto.User;
import com.laundryhub.admin.model.Role;

import java.util.List;

import org.junit.jupiter.api.Test;

/** A4 columns per tab (Orders / Vehicle / Role) and badge texts. */
class UserTabTest {

    private static User user(String role, String mobile, String status, Long orders, String plate, Boolean onDuty,
                             String position) {
        return new User(1L, role, "Maria Santos", "maria@example.com", mobile, "MS", status, orders, plate, onDuty,
                position);
    }

    @Test
    void customersShowMobileAndOrderCount() {
        User maria = user("CUSTOMER", "09170001021", "ACTIVE", 1L, null, null, null);

        assertEquals("0917 000 1021", UserTab.CUSTOMERS.contact(maria));
        assertEquals("1 order", UserTab.CUSTOMERS.thirdColumn(maria));
        assertEquals("Active", UserTab.CUSTOMERS.statusText(maria));
        assertEquals("status-completed", UserTab.CUSTOMERS.statusStyle(maria));
        assertEquals("3 orders", UserTab.CUSTOMERS.thirdColumn(user("CUSTOMER", null, "ACTIVE", 3L, null, null, null)));
    }

    @Test
    void ridersShowPlateAndDuty() {
        User rider = user("RIDER", null, "ACTIVE", null, "SMP 0001", true, null);

        assertEquals("maria@example.com", UserTab.RIDERS.contact(rider));
        assertEquals("SMP 0001", UserTab.RIDERS.thirdColumn(rider));
        assertEquals("On duty", UserTab.RIDERS.statusText(rider));
        assertEquals("Off duty", UserTab.RIDERS.statusText(user("RIDER", null, "ACTIVE", null, null, false, null)));
        assertEquals("Inactive", UserTab.RIDERS.statusText(user("RIDER", null, "INACTIVE", null, null, true, null)));
    }

    @Test
    void staffShowEmailAndRole() {
        User staff = user("STAFF", "09170000000", "ACTIVE", null, null, null, "Counter staff");

        assertEquals("maria@example.com", UserTab.STAFF.contact(staff));
        assertEquals("Counter staff", UserTab.STAFF.thirdColumn(staff));
        assertEquals(List.of(Role.ADMIN, Role.STAFF), UserTab.STAFF.roles());
        assertEquals("+ Add staff", UserTab.STAFF.addLabel());
    }
}
