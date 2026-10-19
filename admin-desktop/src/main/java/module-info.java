/** Laundry HUB admin console (JavaFX). */
module com.laundryhub.admin {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.net.http;
    requires java.desktop;
    requires com.fasterxml.jackson.databind;

    opens com.laundryhub.admin.ui to javafx.fxml;
    opens com.laundryhub.admin.api.dto to com.fasterxml.jackson.databind;
    exports com.laundryhub.admin;
    exports com.laundryhub.admin.api;
    exports com.laundryhub.admin.api.dto;
    exports com.laundryhub.admin.model;
    exports com.laundryhub.admin.ui;
}
