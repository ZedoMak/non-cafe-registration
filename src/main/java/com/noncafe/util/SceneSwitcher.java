package com.noncafe.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class SceneSwitcher {
    private static Stage primaryStage;
    public static void setStage(Stage stage) {
        primaryStage = stage;
    }
    public static void switchTo(String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(SceneSwitcher.class.getResource("/com/noncafe/views/" + fxmlFile)));
            Scene scene = new Scene(root);

            primaryStage.setScene(scene);
            primaryStage.show();

            primaryStage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading FXML: " + fxmlFile);
        }
    }

    public static Stage getStage() {
        return primaryStage;
    }
}


