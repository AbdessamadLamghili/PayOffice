package ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Main entry point for the PayOffice JavaFX application.
 * Bootstraps the JavaFX runtime and loads the main FXML view.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load the FXML layout
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MainView.fxml"));
        Parent root = loader.load();

        // Configure the primary stage
        primaryStage.setTitle("PayOffice — Gestion des Frais d'Inscription");
        primaryStage.setScene(new Scene(root, 1100, 720));
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
