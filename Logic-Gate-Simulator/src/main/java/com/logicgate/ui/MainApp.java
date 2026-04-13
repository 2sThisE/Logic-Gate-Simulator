package com.logicgate.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.net.URL;

public class MainApp extends Application {

    private Stage primaryStage;
    private MainController mainController;
    private boolean projectInitialized = false;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        
        // 1. 메인 에디터 화면 먼저 로드
        URL mainFxml = getClass().getResource("/com/logicgate/ui/main.fxml");
        FXMLLoader mainLoader = new FXMLLoader(mainFxml);
        Parent mainRoot = mainLoader.load();
        this.mainController = mainLoader.getController();

        Scene mainScene = new Scene(mainRoot, 1200, 800);
        primaryStage.setTitle("Logic Gate Simulator");
        primaryStage.setScene(mainScene);
        primaryStage.show();

        // 2. 메인 창이 뜨자마자 프로젝트 런처 띄우기
        showLauncher();
    }

    public void showLauncher() {
        if (mainController != null) {
            LauncherController.ProjectResult res = mainController.getProjectManager().showLauncher(primaryStage, projectInitialized);
            if (res != null) {
                onProjectSelected(res.root, res.isNew);
            }
        }
    }

    public void onProjectSelected(File projectRoot, boolean isNewProject) {
        projectInitialized = true;
        mainController.initializeProject(projectRoot, isNewProject);
        mainController.setPrimaryStage(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}