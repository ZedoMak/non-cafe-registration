package com.noncafe.controller;

import com.noncafe.data.DataStore;
import com.noncafe.model.*;
import com.noncafe.util.SceneSwitcher;
import com.noncafe.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdminController {
    @FXML private Label adminNameLabel, campusLabel, budgetLabel, disbursementDeadlineLabel, disbursementMonthLabel;
    @FXML private Label registeredCountLabel, pendingCountLabel, approvedCountLabel;
    @FXML private TableView<Student> studentsTable;
    @FXML private TableColumn<Student, String> colName, colID, colDept;
    @FXML private TableColumn<Student, StudentRegistrationStatus> colStatus;
    @FXML private TableColumn<Student, Double> colBalance;
    @FXML private Button distributeCostSharingBtn, approveAllBtn;
    @FXML private ComboBox<String> filterComboBox;

    private Admin currentAdmin;
    private Campus adminCampus;
    private ObservableList<Student> campusStudents;
    private FilteredList<Student> filteredStudents;
    private SystemState systemState;
    private Timeline clock;

    @FXML
    public void initialize() {
        if (!(SessionManager.getCurrentUser() instanceof Admin)) {
            onLogout();
            return;
        }
        currentAdmin = (Admin) SessionManager.getCurrentUser();
        adminCampus = currentAdmin.getCampus();
        systemState = DataStore.getInstance().getSystemState();

        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colID.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDept.setCellValueFactory(new PropertyValueFactory<>("department"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("registrationStatus"));
        colBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));

        filterComboBox.setItems(FXCollections.observableArrayList("All", "Approved", "Pending", "Rejected"));
        filterComboBox.getSelectionModel().select("All");
        filterComboBox.setOnAction(e -> applyFilter());

        loadData();
        startMonthlyTimerLoop();
    }

    private void loadData() {
        adminNameLabel.setText(currentAdmin.getName());
        campusLabel.setText(adminCampus.toString());
        double allocatedBudget = systemState.getCampusAllocatedBudgets().getOrDefault(adminCampus, 0.0);
        budgetLabel.setText(String.format("ETB %.2f", allocatedBudget));

        List<Student> allCampusStudents = DataStore.getInstance().getStudentsByCampus(adminCampus);
        campusStudents = FXCollections.observableArrayList(allCampusStudents);
        filteredStudents = new FilteredList<>(campusStudents, p -> true);
        studentsTable.setItems(filteredStudents);

        updateStatistics(allCampusStudents);
        applyFilter();
    }

    private void applyFilter() {
        String filter = filterComboBox.getValue();
        filteredStudents.setPredicate(student -> {
            if ("All".equals(filter)) return true;
            if ("Approved".equals(filter)) return student.getRegistrationStatus() == StudentRegistrationStatus.APPROVED;
            if ("Pending".equals(filter)) return student.getRegistrationStatus() == StudentRegistrationStatus.PENDING_APPROVAL;
            if ("Rejected".equals(filter)) return student.getRegistrationStatus() == StudentRegistrationStatus.REJECTED;
            return true;
        });
    }
    private void updateStatistics(List<Student> students) {
        long pending = students.stream().filter(s -> s.getRegistrationStatus() == StudentRegistrationStatus.PENDING_APPROVAL).count();
        long approved = students.stream().filter(s -> s.getRegistrationStatus() == StudentRegistrationStatus.APPROVED).count();
        registeredCountLabel.setText(String.valueOf(pending + approved));
        pendingCountLabel.setText(String.valueOf(pending));
        approvedCountLabel.setText(String.valueOf(approved));
    }

    private void startMonthlyTimerLoop() {
        clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            systemState = DataStore.getInstance().getSystemState();
            LocalDateTime now = LocalDateTime.now();

            int currentMonth = systemState.getCampusCurrentMonthMap().getOrDefault(adminCampus, 0);
            LocalDateTime nextDeadline = systemState.getCampusNextDeadlineMap().get(adminCampus);
            int totalMonths = systemState.getSemesterMonths();
            boolean isReleased = systemState.isBudgetReleased();

            // Logic Fix: Ensure month display never exceeds totalMonths
            if (currentMonth > totalMonths) {
                disbursementMonthLabel.setText("Semester Completed");
                disbursementDeadlineLabel.setText("All payments done.");
                distributeCostSharingBtn.setDisable(true);
                return;
            }

            disbursementMonthLabel.setText("Month: " + (currentMonth == 0 ? "Not Started" : currentMonth));

            if (!isReleased) {
                disbursementDeadlineLabel.setText("Waiting for Gen. Admin");
                distributeCostSharingBtn.setDisable(true);
                return;
            }

            if (currentMonth == 0) {
                disbursementDeadlineLabel.setText("READY FOR MONTH 1");

                // LOGIC FIX: DISABLE IF NO STUDENTS OR NO BUDGET
                long approvedCount = campusStudents.stream().filter(s -> s.getRegistrationStatus() == StudentRegistrationStatus.APPROVED).count();
                double budget = systemState.getCampusAllocatedBudgets().getOrDefault(adminCampus, 0.0);

                if (approvedCount == 0 || budget <= 0) {
                    distributeCostSharingBtn.setDisable(true);
                    distributeCostSharingBtn.setText("NO STUDENTS/BUDGET");
                } else {
                    distributeCostSharingBtn.setDisable(false);
                    distributeCostSharingBtn.setText("INITIATE MONTH 1");
                }
                return;
            }

            if (currentMonth < totalMonths && nextDeadline != null) {
                long secondsLeft = ChronoUnit.SECONDS.between(now, nextDeadline);
                if (secondsLeft <= 0) {
                    performPayout(currentMonth + 1); // Auto Pay Next Month
                } else {
                    long minutes = secondsLeft / 60;
                    long seconds = secondsLeft % 60;
                    disbursementDeadlineLabel.setText(String.format("Next Release: %02d:%02d", minutes, seconds));
                    distributeCostSharingBtn.setDisable(true);
                }
            } else if (currentMonth == totalMonths) {
                // If we are on the last month, we don't count down to another month.
                disbursementDeadlineLabel.setText("Final Payment Done");
                // Explicitly mark as finished
                systemState.getCampusCurrentMonthMap().put(adminCampus, totalMonths + 1);
                DataStore.getInstance().saveSystemState();
            }
        }));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    @FXML
    public void onDistributeCostSharing() {
        int currentMonth = systemState.getCampusCurrentMonthMap().getOrDefault(adminCampus, 0);
        if (currentMonth == 0) {
            performPayout(1);
        }
    }
    private void performPayout(int monthToPay) {
        SystemState state = DataStore.getInstance().getSystemState();
        int maxMonths = state.getSemesterMonths();

        // LOGIC FIX: PREVENT OVER-MONTH PAYOUTS
        if (monthToPay > maxMonths) {
            state.getCampusCurrentMonthMap().put(adminCampus, maxMonths + 1);
            state.getCampusNextDeadlineMap().put(adminCampus, null);
            DataStore.getInstance().saveSystemState();
            return;
        }

        double monthlyCost = state.getMonthlyCostPerStudent();
        List<Student> approvedStudents = campusStudents.stream()
                .filter(Student::isTrulyRegistered)
                .collect(Collectors.toList());

        double currentCampusBudget = state.getCampusAllocatedBudgets().getOrDefault(adminCampus, 0.0);
        double totalMonthlyNeed = approvedStudents.size() * monthlyCost;

        if (currentCampusBudget >= totalMonthlyNeed && !approvedStudents.isEmpty()) {
            for (Student student : approvedStudents) {
                student.applyCost(monthlyCost);
            }
            state.getCampusAllocatedBudgets().put(adminCampus, currentCampusBudget - totalMonthlyNeed);

            // Update State
            state.getCampusCurrentMonthMap().put(adminCampus, monthToPay);

            // LOGIC FIX: ONLY SET DEADLINE IF THERE IS A NEXT MONTH
            if (monthToPay < maxMonths) {
                state.getCampusNextDeadlineMap().put(adminCampus, LocalDateTime.now().plusMinutes(1));
            } else {
                state.getCampusNextDeadlineMap().put(adminCampus, null); // No more deadlines
            }

            DataStore.getInstance().saveStudents();
            DataStore.getInstance().saveSystemState();
            studentsTable.refresh();
            loadData();
        } else {
            // Stop if budget fail
            clock.stop();
            showAlert(Alert.AlertType.ERROR, "Error", "Budget exhausted or no students.");
        }
    }

    // --- Actions ---
    @FXML public void onApproveAll() {
        // ... (Same as before) ...
        long pendingCount = campusStudents.stream().filter(s -> s.getRegistrationStatus() == StudentRegistrationStatus.PENDING_APPROVAL).count();
        if (pendingCount == 0) { showAlert(Alert.AlertType.INFORMATION, "Info", "No pending students."); return; }
        for (Student s : campusStudents) {
            if (s.getRegistrationStatus() == StudentRegistrationStatus.PENDING_APPROVAL) s.approveRegistration();
        }
        DataStore.getInstance().saveStudents();
        studentsTable.refresh();
        updateStatistics(campusStudents);
        loadData();
    }

    // ... Other actions (approve/reject/delete) same as previous ...
    @FXML public void onApproveRegistration() { processApproval(StudentRegistrationStatus.APPROVED); }
    @FXML public void onRejectRegistration() { processApproval(StudentRegistrationStatus.REJECTED); }
    @FXML public void onDeleteStudent() {
        Student s = studentsTable.getSelectionModel().getSelectedItem();
        if (s != null) { DataStore.getInstance().getStudents().remove(s); DataStore.getInstance().saveStudents(); loadData(); }
    }

    private void processApproval(StudentRegistrationStatus newStatus) {
        Student selected = studentsTable.getSelectionModel().getSelectedItem();
        if(selected == null) return;
        if(newStatus == StudentRegistrationStatus.APPROVED) selected.approveRegistration();
        else selected.rejectRegistration();
        DataStore.getInstance().saveStudents();
        studentsTable.refresh();
        updateStatistics(campusStudents);
    }

    @FXML public void onLogout() { if(clock!=null) clock.stop(); SessionManager.logout(); SceneSwitcher.switchTo("login.fxml"); }
    private void showAlert(Alert.AlertType type, String title, String content) { Alert alert = new Alert(type); alert.setTitle(title); alert.setContentText(content); alert.showAndWait(); }
}

