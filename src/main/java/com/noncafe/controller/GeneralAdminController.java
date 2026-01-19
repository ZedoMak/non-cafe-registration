package com.noncafe.controller;

import com.noncafe.data.DataStore;
import com.noncafe.model.*;
import com.noncafe.util.SceneSwitcher;
import com.noncafe.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class GeneralAdminController {
    @FXML private Label adminNameLabel, statusLabel, deadlineLabel, registeredCountLabel;
    @FXML private Label mainCampusBudget, technoCampusBudget, agriCampusBudget, referralCampusBudget;

    // --- FIX START: Changed totalBudgetField from TextField to Label ---
    @FXML private Label totalBudgetField;
    // --- FIX END ---

    @FXML private TextField monthlyCostField, semesterMonthsField, semesterDaysField, regDurationField;

    @FXML private Button startSemesterBtn, pauseTimerBtn, resumeTimerBtn, stopTimerBtn, calculateBudgetBtn, distributeBtn;

    // Campus Monitoring Table
    @FXML private TableView<CampusStatus> campusStatusTable;
    @FXML private TableColumn<CampusStatus, String> colCampusName, colCampusState, colCampusMonth;

    // History & Admin Mgmt
    @FXML private ListView<String> historyListView;
    @FXML private TableView<Admin> adminsTable;
    @FXML private TableColumn<Admin, String> colAdminName, colAdminId, colAdminCampus;
    @FXML private TextField newAdminName, newAdminId, newAdminPass;
    @FXML private ComboBox<Campus> newAdminCampus;

    private SystemState systemState;
    private GeneralAdmin currentAdmin;
    private Timeline clock;

    @FXML
    public void initialize() {
        if (!(SessionManager.getCurrentUser() instanceof GeneralAdmin)) {
            onLogout();
            return;
        }
        currentAdmin = (GeneralAdmin) SessionManager.getCurrentUser();
        systemState = DataStore.getInstance().getSystemState();
        adminNameLabel.setText(currentAdmin.getName());

        // Load Configs
        monthlyCostField.setText(String.valueOf(systemState.getMonthlyCostPerStudent()));
        semesterMonthsField.setText(String.valueOf(systemState.getSemesterMonths()));
        semesterDaysField.setText(String.valueOf(systemState.getSemesterDays()));
        regDurationField.setText(String.valueOf(systemState.getRegistrationDurationMinutes()));

        setupTables();
        updateBudgetUI();
        refreshButtonStates();
        startTimerLoop();
    }

    private void setupTables() {
        // Admin Table
        colAdminName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colAdminId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAdminCampus.setCellValueFactory(new PropertyValueFactory<>("campus"));
        newAdminCampus.setItems(FXCollections.observableArrayList(Campus.values()));
        adminsTable.setItems(FXCollections.observableArrayList(DataStore.getInstance().getAdmins()));

        // Campus Status Table
        colCampusName.setCellValueFactory(new PropertyValueFactory<>("campusName"));
        colCampusState.setCellValueFactory(new PropertyValueFactory<>("status"));
        colCampusMonth.setCellValueFactory(new PropertyValueFactory<>("currentMonth"));

        loadHistory();
    }

    private void refreshCampusStatusTable() {
        ObservableList<CampusStatus> statuses = FXCollections.observableArrayList();
        for (Campus c : Campus.values()) {
            int month = systemState.getCampusCurrentMonthMap().getOrDefault(c, 0);
            String stateStr;
            String monthStr;

            if (month == 0) {
                stateStr = systemState.isBudgetReleased() ? "Ready to Start" : "Waiting for Budget";
                monthStr = "-";
            } else if (month > systemState.getSemesterMonths()) {
                stateStr = "Completed";
                monthStr = "Finished";
            } else {
                stateStr = "Active";
                monthStr = "Month " + month;
            }
            statuses.add(new CampusStatus(c.toString(), stateStr, monthStr));
        }
        campusStatusTable.setItems(statuses);
    }

    public static class CampusStatus {
        private final SimpleStringProperty campusName;
        private final SimpleStringProperty status;
        private final SimpleStringProperty currentMonth;

        public CampusStatus(String name, String status, String month) {
            this.campusName = new SimpleStringProperty(name);
            this.status = new SimpleStringProperty(status);
            this.currentMonth = new SimpleStringProperty(month);
        }
        public String getCampusName() { return campusName.get(); }
        public String getStatus() { return status.get(); }
        public String getCurrentMonth() { return currentMonth.get(); }
    }

    private void refreshButtonStates() {
        boolean regOpen = systemState.isRegistrationOpen();
        boolean regPaused = systemState.isRegistrationPaused();
        boolean calculated = systemState.isBudgetCalculated();
        boolean released = systemState.isBudgetReleased();

        startSemesterBtn.setVisible(!regOpen);
        startSemesterBtn.setDisable(regOpen);

        pauseTimerBtn.setVisible(regOpen && !regPaused);
        resumeTimerBtn.setVisible(regOpen && regPaused);
        stopTimerBtn.setVisible(regOpen);

        calculateBudgetBtn.setDisable(regOpen || released);
        distributeBtn.setDisable(!calculated || released);
    }

    @FXML
    public void onStartSemester() {
        if (systemState.isBudgetReleased()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "A semester cycle was previously active. Starting a new one will reset all campus cycles. Continue?", ButtonType.YES, ButtonType.NO);
            alert.showAndWait();
            if (alert.getResult() != ButtonType.YES) return;
        }

        try {
            int duration = Integer.parseInt(regDurationField.getText());
            systemState.setRegistrationDurationMinutes(duration);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Invalid Duration");
            return;
        }

        systemState.setRegistrationOpen(true);
        systemState.setRegistrationPaused(false);
        systemState.setRegistrationDeadline(LocalDateTime.now().plusMinutes(systemState.getRegistrationDurationMinutes()));

        systemState.setBudgetCalculated(false);
        systemState.setBudgetReleased(false);
        systemState.resetCampusCycles();

        DataStore.getInstance().saveSystemState();
        refreshButtonStates();
        currentAdmin.addLog("Started New Semester Registration (Duration: " + systemState.getRegistrationDurationMinutes() + " min)");
        loadHistory();
    }

    @FXML
    public void onCalculateBudget() {
        try {
            double cost = Double.parseDouble(monthlyCostField.getText());
            int months = Integer.parseInt(semesterMonthsField.getText());
            int days = Integer.parseInt(semesterDaysField.getText());

            List<Student> students = DataStore.getInstance().getStudents();
            long approvedCount = students.stream()
                    .filter(s -> s.getRegistrationStatus() == StudentRegistrationStatus.APPROVED ||
                            s.getRegistrationStatus() == StudentRegistrationStatus.PENDING_APPROVAL)
                    .count();

            systemState.setMonthlyCostPerStudent(cost);
            systemState.setSemesterMonths(months);
            systemState.setSemesterDays(days);

            double totalNeeded = (approvedCount * cost * months) + (approvedCount * (cost / 30.0) * days);
            systemState.setTotalGovernmentBudget(totalNeeded);
            systemState.setBudgetCalculated(true);

            Map<Campus, Double> allocations = systemState.getCampusAllocatedBudgets();
            allocations.clear();
            for (Campus c : Campus.values()) {
                long cCount = students.stream()
                        .filter(s -> s.getCampus() == c && (s.getRegistrationStatus() == StudentRegistrationStatus.APPROVED || s.getRegistrationStatus() == StudentRegistrationStatus.PENDING_APPROVAL))
                        .count();
                double cBudget = (cCount * cost * months) + (cCount * (cost / 30.0) * days);
                allocations.put(c, cBudget);
            }

            DataStore.getInstance().saveSystemState();
            updateBudgetUI();
            refreshButtonStates();

            String log = String.format("Budget Calculated: ETB %.2f for %d students", totalNeeded, approvedCount);
            currentAdmin.addLog(log);
            DataStore.getInstance().saveGeneralAdmin();
            loadHistory();
            showAlert(Alert.AlertType.INFORMATION, "Budget Calculated", log);

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Invalid numeric input in configuration fields.");
        }
    }

    @FXML
    public void onDistributeBudget() {
        if (!systemState.isBudgetCalculated()) return;

        systemState.setBudgetReleased(true);
        DataStore.getInstance().saveSystemState();
        refreshButtonStates();

        currentAdmin.addLog("Released Semester Budget to Campuses.");
        DataStore.getInstance().saveGeneralAdmin();
        loadHistory();
        showAlert(Alert.AlertType.INFORMATION, "Funds Released", "Campuses can now initiate their cycles.");
    }

    @FXML public void onPauseTimer() {
        if (systemState.isRegistrationOpen() && !systemState.isRegistrationPaused()) {
            systemState.setRemainingSecondsAtPause(ChronoUnit.SECONDS.between(LocalDateTime.now(), systemState.getRegistrationDeadline()));
            systemState.setRegistrationPaused(true);
            DataStore.getInstance().saveSystemState();
            refreshButtonStates();
        }
    }

    @FXML public void onResumeTimer() {
        if (systemState.isRegistrationPaused()) {
            systemState.setRegistrationDeadline(LocalDateTime.now().plusSeconds(systemState.getRemainingSecondsAtPause()));
            systemState.setRegistrationPaused(false);
            DataStore.getInstance().saveSystemState();
            refreshButtonStates();
        }
    }

    @FXML public void onStopTimer() {
        systemState.setRegistrationOpen(false);
        systemState.setRegistrationPaused(false);
        systemState.setRegistrationDeadline(null);
        DataStore.getInstance().saveSystemState();
        refreshButtonStates();
        currentAdmin.addLog("Registration Deadline Stopped manually/expired.");
        loadHistory();
    }

    @FXML public void onAddAdmin() {
        if (newAdminName.getText().isEmpty() || newAdminId.getText().isEmpty() || newAdminPass.getText().isEmpty() || newAdminCampus.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please fill all admin fields.");
            return;
        }
        if (DataStore.getInstance().getAdmins().stream().anyMatch(a -> a.getId().equals(newAdminId.getText()))) {
            showAlert(Alert.AlertType.ERROR, "Error", "Admin ID already exists.");
            return;
        }
        Admin newAdmin = new Admin(newAdminName.getText(), newAdminId.getText(), newAdminPass.getText(), newAdminCampus.getValue());
        DataStore.getInstance().getAdmins().add(newAdmin);
        DataStore.getInstance().saveAdmins();
        currentAdmin.addLog("Created Admin: " + newAdmin.getName());
        DataStore.getInstance().saveGeneralAdmin();
        loadAdmins(); loadHistory();
        newAdminName.clear(); newAdminId.clear(); newAdminPass.clear();
    }

    @FXML public void onDeleteAdmin() {
        Admin selected = adminsTable.getSelectionModel().getSelectedItem();
        if(selected != null) {
            DataStore.getInstance().getAdmins().remove(selected);
            DataStore.getInstance().saveAdmins();
            loadAdmins();
        }
    }

    private void loadAdmins() { adminsTable.setItems(FXCollections.observableArrayList(DataStore.getInstance().getAdmins())); }
    private void loadHistory() { historyListView.setItems(FXCollections.observableArrayList(currentAdmin.getTransactionHistory())); historyListView.scrollTo(historyListView.getItems().size()-1); }

    private void updateBudgetUI() {
        // Now calling setText on Label (valid)
        totalBudgetField.setText(String.format("ETB %.2f", systemState.getTotalGovernmentBudget()));
        mainCampusBudget.setText(String.format("ETB %.2f", systemState.getCampusAllocatedBudgets().getOrDefault(Campus.MAIN, 0.0)));
        technoCampusBudget.setText(String.format("ETB %.2f", systemState.getCampusAllocatedBudgets().getOrDefault(Campus.TECHNO, 0.0)));
        agriCampusBudget.setText(String.format("ETB %.2f", systemState.getCampusAllocatedBudgets().getOrDefault(Campus.AGRI, 0.0)));
        referralCampusBudget.setText(String.format("ETB %.2f", systemState.getCampusAllocatedBudgets().getOrDefault(Campus.REFERRAL, 0.0)));

        long count = DataStore.getInstance().getStudents().stream().filter(s -> s.getRegistrationStatus() == StudentRegistrationStatus.APPROVED || s.getRegistrationStatus() == StudentRegistrationStatus.PENDING_APPROVAL).count();
        registeredCountLabel.setText(String.valueOf(count));
    }

    private void startTimerLoop() {
        clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (systemState.isRegistrationPaused()) {
                statusLabel.setText("REGISTRATION PAUSED");
                deadlineLabel.setText("PAUSED");
            } else if (systemState.isRegistrationOpen()) {
                long sec = ChronoUnit.SECONDS.between(LocalDateTime.now(), systemState.getRegistrationDeadline());
                if (sec <= 0) {
                    onStopTimer();
                } else {
                    statusLabel.setText(String.format("REGISTRATION OPEN (%02d:%02d)", sec / 60, sec % 60));
                    deadlineLabel.setText(systemState.getRegistrationDeadline().toLocalTime().toString());
                }
            } else {
                statusLabel.setText(systemState.isBudgetReleased() ? "Budget Released" : "Setup Mode");
                deadlineLabel.setText("-");
            }
            refreshCampusStatusTable();
        }));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    @FXML public void onLogout() { if (clock != null) clock.stop(); SessionManager.logout(); SceneSwitcher.switchTo("login.fxml"); }
    private void showAlert(Alert.AlertType t, String title, String c) { Alert a = new Alert(t); a.setTitle(title); a.setHeaderText(null); a.setContentText(c); a.showAndWait(); }
}