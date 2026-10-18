module com.laundryhub.admin {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;

    opens com.laundryhub.admin to javafx.fxml;
    exports com.laundryhub.admin;
}
