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
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class MainApp extends Application {

    private Stage primaryStage;
    private MainController mainController;
    private boolean projectInitialized = false;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;

        // 1. 설정된 언어 불러오기 및 적용
        Preferences prefs = Preferences.userNodeForPackage(MainApp.class);
        String lang = prefs.get("language", "system");

        if ("ko".equals(lang)) {
            Locale.setDefault(new Locale("ko", "KR"));
        } else if ("en".equals(lang)) {
            Locale.setDefault(new Locale("en", "US"));
        }

        ResourceBundle bundle = ResourceBundle.getBundle("com.logicgate.ui.strings", Locale.getDefault());

        // 1. 메인 에디터 화면 먼저 로드
        URL mainFxml = getClass().getResource("/com/logicgate/ui/main.fxml");
        FXMLLoader mainLoader = new FXMLLoader(mainFxml, bundle);
        Parent mainRoot = mainLoader.load();
        this.mainController = mainLoader.getController();

        Scene mainScene = new Scene(mainRoot, 1200, 800);
        primaryStage.setTitle(bundle.getString("status.title"));
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