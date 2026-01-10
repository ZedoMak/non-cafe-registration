package com.noncafe.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

public class SystemState implements Serializable {
    private static final long serialVersionUID = 1L;

    // Registration Cycle
    private LocalDateTime registrationDeadline;
    private int registrationDurationMinutes = 5; // Default 5 mins
    private boolean isRegistrationOpen;
    private boolean isRegistrationPaused;
    private long remainingSecondsAtPause;

    // Budget Configuration
    private double monthlyCostPerStudent = 3000.0;
    private int semesterMonths = 5;
    private int semesterDays = 0;
    private double totalGovernmentBudget;
    private Map<Campus, Double> campusAllocatedBudgets;

    // Workflow State Flags
    private boolean isBudgetCalculated;
    private boolean isBudgetReleased;

    // Independent Campus Cycles
    // 0 = Not Started, 1..N = Active Month, N+1 = Finished
    private Map<Campus, Integer> campusCurrentMonthMap;
    private Map<Campus, LocalDateTime> campusNextDeadlineMap;

    public SystemState() {
        this.registrationDeadline = null;
        this.isRegistrationOpen = false;
        this.isRegistrationPaused = false;
        this.totalGovernmentBudget = 0.0;
        this.campusAllocatedBudgets = new EnumMap<>(Campus.class);

        this.isBudgetCalculated = false;
        this.isBudgetReleased = false;

        this.campusCurrentMonthMap = new EnumMap<>(Campus.class);
        this.campusNextDeadlineMap = new EnumMap<>(Campus.class);

        resetCampusCycles();
    }

    public void resetCampusCycles() {
        for (Campus c : Campus.values()) {
            campusCurrentMonthMap.put(c, 0);
            campusNextDeadlineMap.put(c, null);
            campusAllocatedBudgets.put(c, 0.0);
        }
    }

    // --- Getters & Setters ---
    public LocalDateTime getRegistrationDeadline() { return registrationDeadline; }
    public void setRegistrationDeadline(LocalDateTime registrationDeadline) { this.registrationDeadline = registrationDeadline; }

    public int getRegistrationDurationMinutes() { return registrationDurationMinutes; }
    public void setRegistrationDurationMinutes(int registrationDurationMinutes) { this.registrationDurationMinutes = registrationDurationMinutes; }

    public boolean isRegistrationOpen() { return isRegistrationOpen; }
    public void setRegistrationOpen(boolean isRegistrationOpen) { this.isRegistrationOpen = isRegistrationOpen; }

    public boolean isRegistrationPaused() { return isRegistrationPaused; }
    public void setRegistrationPaused(boolean registrationPaused) { isRegistrationPaused = registrationPaused; }

    public long getRemainingSecondsAtPause() { return remainingSecondsAtPause; }
    public void setRemainingSecondsAtPause(long remainingSecondsAtPause) { this.remainingSecondsAtPause = remainingSecondsAtPause; }

    public double getMonthlyCostPerStudent() { return monthlyCostPerStudent; }
    public void setMonthlyCostPerStudent(double monthlyCostPerStudent) { this.monthlyCostPerStudent = monthlyCostPerStudent; }

    public int getSemesterMonths() { return semesterMonths; }
    public void setSemesterMonths(int semesterMonths) { this.semesterMonths = semesterMonths; }

    public int getSemesterDays() { return semesterDays; }
    public void setSemesterDays(int semesterDays) { this.semesterDays = semesterDays; }

    public double getTotalGovernmentBudget() { return totalGovernmentBudget; }
    public void setTotalGovernmentBudget(double totalGovernmentBudget) { this.totalGovernmentBudget = totalGovernmentBudget; }

    public Map<Campus, Double> getCampusAllocatedBudgets() { return campusAllocatedBudgets; }

    public boolean isBudgetCalculated() { return isBudgetCalculated; }
    public void setBudgetCalculated(boolean budgetCalculated) { isBudgetCalculated = budgetCalculated; }

    public boolean isBudgetReleased() { return isBudgetReleased; }
    public void setBudgetReleased(boolean budgetReleased) { isBudgetReleased = budgetReleased; }

    public Map<Campus, Integer> getCampusCurrentMonthMap() { return campusCurrentMonthMap; }
    public Map<Campus, LocalDateTime> getCampusNextDeadlineMap() { return campusNextDeadlineMap; }
}