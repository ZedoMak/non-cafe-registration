package com.noncafe.data;

import com.noncafe.model.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DataStore {
    private static DataStore instance;
    private List<Student> students;
    private List<Admin> admins;
    private GeneralAdmin generalAdmin;
    private SystemState systemState;

    private final String DATA_DIR = "noncafe_data";
    private final String STUDENTS_FILE = DATA_DIR + "/students.dat";
    private final String ADMINS_FILE = DATA_DIR + "/admins.dat";
    private final String GEN_ADMIN_FILE = DATA_DIR + "/gen_admin.dat";
    private final String STATE_FILE = DATA_DIR + "/system_state.dat";

    private DataStore() {
        students = new ArrayList<>();
        admins = new ArrayList<>();
        new File(DATA_DIR).mkdirs();
        loadData();
    }

    public static DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    private void loadData() {
        try {
            students = (List<Student>) loadFile(STUDENTS_FILE);
            if (students == null)
                students = new ArrayList<>();

            admins = (List<Admin>) loadFile(ADMINS_FILE);
            if (admins == null) {
                admins = new ArrayList<>();
                // Initialize default Admins if empty
                admins.add(new Admin("Main Admin", "admin1", "123", Campus.MAIN));
                admins.add(new Admin("Tech Admin", "admin2", "123", Campus.TECHNO));
                admins.add(new Admin("Agri Admin", "admin3", "123", Campus.AGRI));
                admins.add(new Admin("Ref Admin", "admin4", "123", Campus.REFERRAL));
                saveAdmins();
            }

            generalAdmin = (GeneralAdmin) loadFile(GEN_ADMIN_FILE);
            if (generalAdmin == null) {
                generalAdmin = new GeneralAdmin("Super Boss", "genadmin", "admin123");
                saveGeneralAdmin();
            }
            systemState = (SystemState) loadFile(STATE_FILE);
            if (systemState == null) {
                systemState = new SystemState();
                saveSystemState();
            }
        } catch (Exception e) {
            System.out.println("Error loading data: " + e.getMessage());
        }
    }

    private Object loadFile(String path) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    private void saveObject(Object obj, String path) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
            oos.writeObject(obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveStudents() {
        saveObject(students, STUDENTS_FILE);
    }

    public void saveAdmins() {
        saveObject(admins, ADMINS_FILE);
    }

    public void saveGeneralAdmin() {
        saveObject(generalAdmin, GEN_ADMIN_FILE);
    }

    public void saveSystemState() {
        saveObject(systemState, STATE_FILE);
    }

    public List<Student> getStudents() {
        return students;
    }

    public List<Student> getStudentsByCampus(Campus campus) {
        return students.stream()
                .filter(s -> s.getCampus() == campus)
                .collect(Collectors.toList());
    }

    public List<Admin> getAdmins() {
        return admins;
    }

    public GeneralAdmin getGeneralAdmin() {
        return generalAdmin;
    }

    public SystemState getSystemState() {
        return systemState;
    }

    public User authenticateUser(String id, String password) {
        if (generalAdmin.getId().equals(id) && generalAdmin.getPassword().equals(password)) {
            return generalAdmin;
        }
        for (Admin a : admins) {
            if (a.getId().equals(id) && a.getPassword().equals(password))
                return a;
        }
        for (Student s : students) {
            if (s.getId().equals(id) && s.getPassword().equals(password))
                return s;
        }
        return null;
    }

    // here this is a dummy data 😒.. well it is used to create a student if there
    // is non
    public void createDummyStudents() {
        if (students.isEmpty()) {
            students.add(new Student("John Doe", "std1", "123", Campus.MAIN, "CS", 2, "CBE100", "0911"));
            students.add(new Student("Jane Smith", "std2", "123", Campus.TECHNO, "Eng", 3, "CBE200", "0912"));
            students.add(new Student("Eren Yeager", "std3", "123", Campus.TECHNO, "CS", 3, "CBE300", "0913"));
            saveStudents();
        }
    }
}