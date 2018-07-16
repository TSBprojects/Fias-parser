package ru.neoflex.vak.fiasParser;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ru.neoflex.vak.fiasParser.config.DbType;
import ru.neoflex.vak.fiasParser.config.MssqlProperties;
import ru.neoflex.vak.fiasParser.config.MysqlProperties;
import ru.neoflex.vak.fiasParser.config.ParsConfig;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class MainController {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private TextField hostField;

    @FXML
    private TextField portField;

    @FXML
    private TextField dbNameField;

    @FXML
    private TextField userField;

    @FXML
    private ChoiceBox<String> dbTypeChb;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField fiasFilesPathField;

    @FXML
    private Button fiasFilesPathButton;

    @FXML
    private TextField stField;

    @FXML
    private CheckBox ldcChb;

    @FXML
    private CheckBox reqSslChb;

    @FXML
    private CheckBox sslChb;

    @FXML
    private CheckBox vscChb;

    @FXML
    private CheckBox integSecChb;

    @FXML
    private Button startButton;

    @FXML
    void initialize() throws IOException, InterruptedException {

        dbTypeChb.getItems().addAll("mssql", "mysql");
        dbTypeChb.setValue("mssql");

        if (Paths.get("config.ini").toFile().exists()) {
            initializeFields(new ParsConfig("config.ini"));
        }

        startButton.setOnAction(event -> {
            if (isValidFields()) {
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(getClass().getResource("/progressWin.fxml"));

                Parent root = null;
                try {
                    root = loader.load();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ProgressController progressController = loader.<ProgressController>getController();
                progressController.setConfig(createConfig());

                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle("Fias parser");
                stage.setScene(new Scene(root, 420, 230));
                stage.setResizable(false);
                stage.setOnCloseRequest(e -> {
                    progressController.stop();
                });
                stage.getIcons().add(new Image(getClass().getResourceAsStream("/save.png")));
                stage.showAndWait();
            }
        });

        fiasFilesPathButton.setOnAction(event -> {
            fiasFilesPathField.setStyle("-fx-border-color:none");
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select dbf files directory");
            File file = directoryChooser.showDialog(null);
            if (file != null) {
                fiasFilesPathField.setText(file.getPath());
            }
        });
        portField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                portField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        dbNameField.setOnKeyPressed(event -> {
            dbNameField.setStyle("-fx-border-color:none");
        });
    }

    private void initializeFields(ParsConfig config) {
        MssqlProperties mssqlProp = config.getMssqlProp();
        MysqlProperties mysqlProp = config.getMysqlProp();
        fiasFilesPathField.setText(config.getFiasDirPath());
        dbTypeChb.setValue(config.getDbType().toString());
        hostField.setText(mssqlProp.hostName);
        if (config.getDbType().equals(DbType.mssql)) {
            portField.setText(mssqlProp.port);
        } else {
            portField.setText(mysqlProp.port);
        }
        dbNameField.setText(mssqlProp.databaseName);
        userField.setText(mssqlProp.user);
        passwordField.setText(mssqlProp.password);
        vscChb.setSelected(Boolean.parseBoolean(mysqlProp.verifyServerCertificate));
        sslChb.setSelected(Boolean.parseBoolean(mysqlProp.useSSL));
        reqSslChb.setSelected(Boolean.parseBoolean(mysqlProp.requireSSL));
        ldcChb.setSelected(Boolean.parseBoolean(mysqlProp.useLegacyDatetimeCode));
        stField.setText(mysqlProp.serverTimezone);
        integSecChb.setSelected(Boolean.parseBoolean(mssqlProp.integratedSecurity));
    }

    private boolean isValidFields() {
        boolean errorExist = true;
        String message = "";
        if (!Paths.get(fiasFilesPathField.getText()).toFile().exists()) {
            fiasFilesPathField.setStyle("-fx-border-color:red");
            message += "Директория не существует!\n";
            errorExist = false;
        }
        if (dbNameField.getText().isEmpty()) {
            dbNameField.setStyle("-fx-border-color:red");
            message += "Введите database name!\n";
            errorExist = false;
        }
        if (!errorExist) {
            showError(message);
        }
        return errorExist;
    }

    private ParsConfig createConfig() {
        return new ParsConfig(dbTypeChb.getValue(),
                new MysqlProperties(
                        Boolean.toString(vscChb.isSelected()),
                        Boolean.toString(sslChb.isSelected()),
                        Boolean.toString(reqSslChb.isSelected()),
                        Boolean.toString(ldcChb.isSelected()),
                        stField.getText(),
                        hostField.getText(),
                        portField.getText(),
                        dbNameField.getText(),
                        userField.getText(),
                        passwordField.getText()),
                new MssqlProperties(
                        Boolean.toString(integSecChb.isSelected()),
                        hostField.getText(),
                        portField.getText(),
                        dbNameField.getText(),
                        userField.getText(),
                        passwordField.getText()),
                fiasFilesPathField.getText());
    }

    public static void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static void showMessage(String message, String title) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}

