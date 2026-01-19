package com.noncafe;
import com.noncafe.data.DataStore;
import com.noncafe.util.SceneSwitcher;
import javafx.application.Application;
import javafx.stage.Stage;
import java.util.Objects;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        // Initialize Data
        DataStore.getInstance().createDummyStudents();

        SceneSwitcher.setStage(stage);
        stage.setTitle("Non-Cafe Registration System");
        SceneSwitcher.switchTo("login.fxml");
    }
    public static void main(String[] args) {
        launch();
    }
}
