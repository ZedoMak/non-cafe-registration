package com.noncafe.controller;

import com.noncafe.data.DataStore;
import com.noncafe.model.Student;
import com.noncafe.model.SystemState;
import com.noncafe.util.SceneSwitcher;
import com.noncafe.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class StudentController {
    @FXML private Label nameLabel, idLabel, campusLabel, deptYearLabel, bankAccountLabel;
    @FXML private Label deadlineLabel, systemStatusLabel, disbursementDeadlineLabel, registrationStatusLabel, balanceLabel;
    @FXML private Button registerBtn, unregisterBtn;
    @FXML private ListView<String> transactionHistoryList;

    private Student currentStudent;
    private SystemState systemState;
    private Timeline clock;

    @FXML
    public void initialize() {
        if (!(SessionManager.getCurrentUser() instanceof Student)) {
            onLogout();
            return;
        }
        currentStudent = (Student) SessionManager.getCurrentUser();
        systemState = DataStore.getInstance().getSystemState();
        loadPersonalInfo();
        updateCoreData();
        startTimerLoop();
    }

    private void loadPersonalInfo() {
        nameLabel.setText(currentStudent.getName());
        idLabel.setText(currentStudent.getId());
        campusLabel.setText(currentStudent.getCampus().toString());
        deptYearLabel.setText(currentStudent.getDepartment() + " - Year " + currentStudent.getYear());
        bankAccountLabel.setText(currentStudent.getBankAccount() == null ? "N/A" : currentStudent.getBankAccount());
    }

    private void updateCoreData() {
        registrationStatusLabel.setText(currentStudent.getRegistrationStatus().getDisplayName());
        registrationStatusLabel.setStyle("-fx-text-fill: " + currentStudent.getRegistrationStatus().getColorCode() + "; -fx-font-weight: bold;");
        balanceLabel.setText(String.format("ETB %.0f", currentStudent.getBalance()));
        transactionHistoryList.setItems(javafx.collections.FXCollections.observableArrayList(currentStudent.getTransactionHistory()));
        transactionHistoryList.scrollTo(transactionHistoryList.getItems().size() - 1);
        bankAccountLabel.setText(currentStudent.getBankAccount() == null ? "N/A" : currentStudent.getBankAccount());
    }

    private void startTimerLoop() {
        clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            systemState = DataStore.getInstance().getSystemState();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime regDeadline = systemState.getRegistrationDeadline();

            // Registration Timer Logic
            if (systemState.isRegistrationOpen() && regDeadline != null && !systemState.isRegistrationPaused()) {
                long sec = ChronoUnit.SECONDS.between(now, regDeadline);
                deadlineLabel.setText(sec > 0 ? "Deadline: " + regDeadline.format(DateTimeFormatter.ofPattern("HH:mm:ss")) : "Deadline: CLOSED");
                systemStatusLabel.setText(sec > 0 ? "STATUS: OPEN" : "STATUS: CLOSED");

                boolean canRegister = sec > 0;
                boolean alreadyRegistered = currentStudent.getRegistrationStatus() != com.noncafe.model.StudentRegistrationStatus.UNREGISTERED;
                registerBtn.setDisable(!canRegister || alreadyRegistered);
                unregisterBtn.setDisable(!canRegister || !alreadyRegistered);
            } else if (systemState.isRegistrationPaused()) {
                deadlineLabel.setText("Deadline: PAUSED");
                registerBtn.setDisable(true);
            } else {
                deadlineLabel.setText("Deadline: CLOSED");
                registerBtn.setDisable(true);
            }

            // Budget Cycle Timer Logic (Check Campus Specific Cycle)
            int month = systemState.getCampusCurrentMonthMap().getOrDefault(currentStudent.getCampus(), 0);

            if (month > 0 && month <= systemState.getSemesterMonths()) {
                disbursementDeadlineLabel.setText("Cycle Active: Month " + month);
            } else if (month > systemState.getSemesterMonths()) {
                disbursementDeadlineLabel.setText("Semester Ended");
            } else if (systemState.isBudgetReleased()) {
                disbursementDeadlineLabel.setText("Budget Ready. Waiting for Admin.");
            } else {
                disbursementDeadlineLabel.setText("Waiting for Semester");
            }
        }));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    @FXML
    public void onRegister() {
        // DIALOG FOR DATA ENTRY
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Registration Details");
        dialog.setHeaderText("Please confirm your details for Non-Cafe.");

        ButtonType registerType = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registerType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);

        TextField bankField = new TextField();
        bankField.setPromptText("Bank Account Number");
        if(currentStudent.getBankAccount() != null) bankField.setText(currentStudent.getBankAccount());

        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone Number");
        if(currentStudent.getPhoneNumber() != null) phoneField.setText(currentStudent.getPhoneNumber());

        grid.add(new Label("Bank Account:"), 0, 0);
        grid.add(bankField, 1, 0);
        grid.add(new Label("Phone Number:"), 0, 1);
        grid.add(phoneField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == registerType) {
                if(bankField.getText().isEmpty() || phoneField.getText().isEmpty()) return null;
                currentStudent.setBankAccount(bankField.getText());
                currentStudent.setPhoneNumber(phoneField.getText());
                return true;
            }
            return null;
        });

        Optional<Boolean> result = dialog.showAndWait();

        if (result.isPresent() && result.get()) {
            currentStudent.registerForNonCafe();
            DataStore.getInstance().saveStudents();
            updateCoreData();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Application Submitted.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Incomplete", "Registration cancelled or fields empty.");
        }
    }

    @FXML
    public void onUnregister() {
        currentStudent.unregisterFromNonCafe();
        DataStore.getInstance().saveStudents();
        updateCoreData();
    }

    @FXML public void onLogout() { if(clock!=null) clock.stop(); SessionManager.logout(); SceneSwitcher.switchTo("login.fxml"); }
    private void showAlert(Alert.AlertType t, String title, String c) { Alert a = new Alert(t); a.setTitle(title); a.setContentText(c); a.showAndWait(); }
}