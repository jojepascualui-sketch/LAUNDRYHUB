package com.laundryhub.admin.ui;

import com.laundryhub.admin.api.dto.User;
import com.laundryhub.admin.model.Role;

import java.util.List;

/** The three A4 tabs. Each knows its roles, column title, add button and third-column text. */
public enum UserTab {

    /** Customers: contact = mobile, third column = orders. */
    CUSTOMERS("Customers", "Orders", "+ Add customer", List.of(Role.CUSTOMER), Role.CUSTOMER) {
        @Override
        public String thirdColumn(User user) {
            long count = user.orderCount() == null ? 0 : user.orderCount();
            return count + (count == 1 ? " order" : " orders");
        }
    },
    /** Riders: third column = plate number; status "On duty" / "Off duty". */
    RIDERS("Riders", "Vehicle", "+ Add rider", List.of(Role.RIDER), Role.RIDER) {
        @Override
        public String thirdColumn(User user) {
            return user.plateNumber() == null ? "" : user.plateNumber();
        }

        @Override
        public String statusText(User user) {
            if (!ACTIVE.equals(user.status())) {
                return super.statusText(user);
            }
            return Boolean.TRUE.equals(user.onDuty()) ? "On duty" : "Off duty";
        }
    },
    /** Admin and staff: contact = email, third column = role. */
    STAFF("Staff", "Role", "+ Add staff", List.of(Role.ADMIN, Role.STAFF), Role.STAFF) {
        @Override
        public String thirdColumn(User user) {
            return user.position() == null ? "" : user.position();
        }

        @Override
        public String contact(User user) {
            return user.email();
        }
    };

    static final String ACTIVE = "ACTIVE";
    private static final String ACTIVE_TEXT = "Active";
    private static final String INACTIVE_TEXT = "Inactive";
    private static final String OFF_DUTY = "Off duty";

    // OOP: Encapsulation — fixed texts and roles per tab.
    private final String label;
    private final String columnTitle;
    private final String addLabel;
    private final List<Role> roles;
    private final Role newRole;

    UserTab(String label, String columnTitle, String addLabel, List<Role> roles, Role newRole) {
        this.label = label;
        this.columnTitle = columnTitle;
        this.addLabel = addLabel;
        this.roles = roles;
        this.newRole = newRole;
    }

    /**
     * @param user an account
     * @return Orders / Vehicle / Role cell
     */
    // OOP: Polymorphism — each tab fills the third column differently.
    public abstract String thirdColumn(User user);

    /**
     * @param user an account
     * @return Contact cell: mobile number, or e-mail when there is none
     */
    public String contact(User user) {
        String mobile = Ui.formatMobile(user.mobileNumber());
        return mobile.isEmpty() ? user.email() : mobile;
    }

    /**
     * @param user an account
     * @return status badge text
     */
    public String statusText(User user) {
        return ACTIVE.equals(user.status()) ? ACTIVE_TEXT : INACTIVE_TEXT;
    }

    /**
     * @param user an account
     * @return green badge for Active / On duty; neutral (no design color) otherwise
     */
    public String statusStyle(User user) {
        String text = statusText(user);
        return INACTIVE_TEXT.equals(text) || OFF_DUTY.equals(text) ? "status-pending" : "status-completed";
    }

    /** @return segment text */
    public String label() {
        return label;
    }

    /** @return third column title */
    public String columnTitle() {
        return columnTitle;
    }

    /** @return add button text */
    public String addLabel() {
        return addLabel;
    }

    /** @return roles listed on this tab */
    public List<Role> roles() {
        return roles;
    }

    /** @return role of a new account added from this tab */
    public Role newRole() {
        return newRole;
    }
}
