package com.laundryhub.admin.ui;

/** A console page in the main area (A1–A5). The shell shows its kicker and title. */
// OOP: Abstraction — the shell refreshes any page through this type.
// OOP: Inheritance — a BaseController; DashboardController, OrdersController, ... extend it.
public abstract class PageController extends BaseController {

    /** @return which page this is (sidebar item, kicker, title) */
    public abstract Page page();

    /** Reloads the page's data from the server (called every time the page is shown). */
    // OOP: Polymorphism — each page loads its own data.
    public abstract void refresh();

    /** Hook: the page left the screen (stop timers). */
    public void onHide() {
    }
}
