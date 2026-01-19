package com.noncafe.controller;

import com.noncafe.data.DataStore;
import com.noncafe.model.Admin;
import com.noncafe.model.GeneralAdmin;
import com.noncafe.model.Student;
import com.noncafe.model.User;
import com.noncafe.util.SceneSwitcher;
import com.noncafe.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML
    private TextField idField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;
    @FXML
    public void onLoginClick() {
        String id = idField.getText();
        String pass = passwordField.getText();
        if (id.isEmpty() || pass.isEmpty()) {
            errorLabel.setText("Please enter both ID and Password.");
            errorLabel.setVisible(true);
            return;
        }
        User user = DataStore.getInstance().authenticateUser(id, pass);
        if (user != null) {
            SessionManager.setCurrentUser(user);
            System.out.println("Login Successful: " + user.getName() + " (" + user.getClass().getSimpleName() + ")");
            if (user instanceof GeneralAdmin) {
                SceneSwitcher.switchTo("general_admin_dashboard.fxml");
            } else if (user instanceof Admin) {
                SceneSwitcher.switchTo("admin_dashboard.fxml");
            } else if (user instanceof Student) {
                SceneSwitcher.switchTo("student_dashboard.fxml");
            }
        } else {
            errorLabel.setText("Invalid Credentials. Try Again");
            errorLabel.setVisible(true);
        }
    }
}