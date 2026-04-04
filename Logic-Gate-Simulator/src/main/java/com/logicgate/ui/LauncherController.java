package com.logicgate.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

public class LauncherController {

    @FXML
    private TextField projectNameField;

    private MainApp mainApp;
    private Stage stage;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void createNewProject() {
        String projectName = projectNameField.getText();
        if (projectName == null || projectName.trim().isEmpty()) {
            showAlert("오류", "프로젝트 이름을 입력해주세요.");
            return;
        }

        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("프로젝트 생성 위치 선택");
        File selectedDir = dirChooser.showDialog(projectNameField.getScene().getWindow());

        if (selectedDir != null) {
            File projectRoot = new File(selectedDir, projectName.trim());
            if (!projectRoot.exists()) {
                projectRoot.mkdirs();
            }
            mainApp.onProjectSelected(projectRoot, true);
            stage.close(); // 런처 창 닫기
        }
    }

    @FXML
    public void openExistingProject() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("프로젝트 폴더 선택");
        File selectedDir = dirChooser.showDialog(projectNameField.getScene().getWindow());

        if (selectedDir != null) {
            File prjFile = new File(selectedDir, "project.prj");
            if (!prjFile.exists()) {
                showAlert("오류", "선택한 폴더에 project.prj 파일이 없습니다. 올바른 프로젝트 폴더를 선택해주세요.");
                return;
            }
            mainApp.onProjectSelected(selectedDir, false);
            stage.close(); // 런처 창 닫기
        }
    }

    @FXML
    public void exitApp() {
        Platform.exit();
        System.exit(0);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}