package com.laundryhub.admin.ui;

/** The five console pages in sidebar order, with the design's kicker and title. */
public enum Page {

    /** A1. */
    DASHBOARD("Dashboard", "Today\u2019s overview", "Dashboard", "dashboard.fxml"),
    /** A2. */
    ORDERS("Orders", "Manage and update", "Orders", "orders.fxml"),
    /** A3. */
    PRICING("Pricing & soaps", "Settings", "Pricing & soaps", "pricing.fxml"),
    /** A4. */
    USERS("Customers & riders", "Accounts", "Customers & riders", "users.fxml"),
    /** A5. */
    REPORTS("Sales reports", "Insights", "Sales reports", "reports.fxml"),
    /** Feedback chat and ratings (no design, CLAUDE.md §7). */
    FEEDBACK("Feedback", "Customers and riders", "Feedback", "feedback.fxml");

    // OOP: Encapsulation — fixed texts per page.
    private final String navLabel;
    private final String kicker;
    private final String title;
    private final String fxml;

    Page(String navLabel, String kicker, String title, String fxml) {
        this.navLabel = navLabel;
        this.kicker = kicker;
        this.title = title;
        this.fxml = fxml;
    }

    /** @return sidebar text */
    public String navLabel() {
        return navLabel;
    }

    /** @return small line above the title, e.g. "Today’s overview" */
    public String kicker() {
        return kicker;
    }

    /** @return 30 px page title */
    public String title() {
        return title;
    }

    /** @return FXML file next to the controllers */
    public String fxml() {
        return fxml;
    }
}
