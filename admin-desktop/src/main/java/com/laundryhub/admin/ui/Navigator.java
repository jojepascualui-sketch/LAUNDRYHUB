package com.laundryhub.admin.ui;

/** Moves between the sign-in screen, the console pages and shows short messages. */
// OOP: Abstraction — controllers navigate without knowing about stages and scenes.
public interface Navigator {

    /** Shows the sign-in screen (after Log out or an expired session). */
    void showSignIn();

    /** Shows the console on the dashboard (after sign-in). */
    void showConsole();

    /**
     * @param page page to show
     */
    void open(Page page);

    /**
     * Opens Orders with one order selected (A1 "Open").
     *
     * @param orderId the order
     */
    void openOrder(long orderId);

    /**
     * Opens Orders filtered by the header search.
     *
     * @param query order no. or customer
     */
    void searchOrders(String query);

    /**
     * Shows a short message at the bottom right.
     *
     * @param message text
     */
    void toast(String message);
}
