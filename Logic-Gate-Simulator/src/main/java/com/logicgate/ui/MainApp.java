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

        // 2. 메인 창이 뜨자마자 그 위에 프로젝트 런처 띄우기
        showLauncher();
    }

    public void showLauncher() {
        try {
            URL launcherFxml = getClass().getResource("/com/logicgate/ui/launcher.fxml");
            FXMLLoader loader = new FXMLLoader(launcherFxml);
            Parent root = loader.load();
            
            Stage launcherStage = new Stage();
            launcherStage.initModality(Modality.APPLICATION_MODAL);
            launcherStage.initOwner(primaryStage);
            launcherStage.setTitle("Project Manager"); // 표준 창 제목 추가
            
            launcherStage.setOnCloseRequest(event -> {
                if (!projectInitialized) {
                    javafx.application.Platform.exit();
                    System.exit(0);
                }
            });
            
            LauncherController controller = loader.getController();
            controller.setMainApp(this);
            controller.setStage(launcherStage);

            Scene scene = new Scene(root, 800, 500);
            launcherStage.setScene(scene);
            launcherStage.setResizable(true);
            launcherStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onProjectSelected(File projectRoot, boolean isNewProject) {
        projectInitialized = true;
        mainController.initializeProject(projectRoot, isNewProject);
        mainController.setPrimaryStage(primaryStage); // 컨트롤러에 Stage 전달 ✨
    }

    public static void main(String[] args) {
        launch(args);
    }
}