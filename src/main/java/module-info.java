module com.noncafe {
    // Requires
    requires javafx.controls;
    requires javafx.fxml;
    requires java.base;

    exports com.noncafe;
    exports com.noncafe.model;

    opens com.noncafe to javafx.fxml;
    opens com.noncafe.controller to javafx.fxml, javafx.controls, javafx.base;
    opens com.noncafe.views to javafx.fxml;
}