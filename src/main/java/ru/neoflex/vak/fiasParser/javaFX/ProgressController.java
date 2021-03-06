package ru.neoflex.vak.fiasParser.javaFX;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.neoflex.vak.fiasParser.DbfParser;
import ru.neoflex.vak.fiasParser.config.ParsConfig;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ProgressController {

    private final Logger log = LogManager.getLogger(ProgressController.class.getName());

    public void setConfig(ParsConfig config) {
        this.config = config;
    }

    private boolean run = true;

    private ParsConfig config;

    private Stage currentWindow;

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private Label infoLabel;

    @FXML
    private ProgressBar allPb;

    @FXML
    private ProgressBar currPb;

    @FXML
    private Button stopButton;

    @FXML
    void initialize() throws IOException {
        Platform.runLater(() -> {
            currentWindow = (Stage) infoLabel.getScene().getWindow();
            new Thread(() -> {
                try {
                    DbfParser.start(config, (message, fullProgress, currentProgress) -> {
                        if (fullProgress != -1 && currentProgress != -1) {
                            allPb.setProgress(fullProgress);
                            currPb.setProgress(currentProgress);
                        }
                        Platform.runLater(() -> {
                            infoLabel.setText(message);
                        });
                        return run;
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        log.error(e.getMessage(), e);
                        MainController.showError(e.getMessage() + " \nSee logs: 'logs.log'");
                        currentWindow.close();
                    });
                }
            }).start();
        });
    }

    void stop() {
        run = false;
    }
}
