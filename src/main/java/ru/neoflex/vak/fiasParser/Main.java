package ru.neoflex.vak.fiasParser;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import ru.neoflex.vak.fiasParser.config.ParsConfig;
import ru.neoflex.vak.fiasParser.javaFX.MainController;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/main.fxml"));
            primaryStage.setTitle("Fias parser");
            primaryStage.setScene(new Scene(root, 363, 394));
            primaryStage.setOnCloseRequest(event -> {
                System.exit(0);
            });
            primaryStage.setResizable(false);
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/main.png")));
            primaryStage.show();

        } catch (IOException e) {
            MainController.showError(e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length >= 1 && args[0].equals("-g")) {
            launch(args);
        } else {
            try {
                //ParsConfig config = new ParsConfig("G:\\Основное\\OneDrive\\Учёба\\ПРАКТИКА\\Fias parser\\Packaged application\\config.ini");
                ParsConfig config = new ParsConfig(args[0]);
                DbfParser.start(config, null);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            System.exit(0);
        }
    }
}