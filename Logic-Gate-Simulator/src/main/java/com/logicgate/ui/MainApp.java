package com.logicgate.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class MainApp extends Application {

    private static final Locale SYSTEM_LOCALE = Locale.getDefault();

    private Stage primaryStage;
    private MainController mainController;
    private boolean projectInitialized = false;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;

        applyLocaleFromPreferences();

        ResourceBundle bundle = ResourceBundle.getBundle("com.logicgate.ui.strings", Locale.getDefault());

        URL mainFxml = getClass().getResource("/com/logicgate/ui/main.fxml");
        FXMLLoader mainLoader = new FXMLLoader(mainFxml, bundle);
        Parent mainRoot = mainLoader.load();
        this.mainController = mainLoader.getController();
        this.mainController.setRestartCallback(this::restartCurrentProject);

        Scene mainScene = new Scene(mainRoot, 1200, 800);
        primaryStage.setTitle(bundle.getString("status.title"));
        primaryStage.setScene(mainScene);
        primaryStage.show();

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

    private void applyLocaleFromPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(MainApp.class);
        String lang = prefs.get("language", "system");

        if ("ko".equals(lang)) {
            Locale.setDefault(new Locale("ko", "KR"));
        } else if ("en".equals(lang)) {
            Locale.setDefault(new Locale("en", "US"));
        } else {
            Locale.setDefault(SYSTEM_LOCALE);
        }
    }

    private void restartCurrentProject() {
        try {
            File projectRoot = mainController != null ? mainController.getProjectRoot() : null;
            boolean hasProject = projectInitialized && projectRoot != null;

            if (mainController != null) {
                mainController.prepareForRestart();
            }

            applyLocaleFromPreferences();
            ResourceBundle bundle = ResourceBundle.getBundle("com.logicgate.ui.strings", Locale.getDefault());
            URL mainFxml = getClass().getResource("/com/logicgate/ui/main.fxml");
            FXMLLoader mainLoader = new FXMLLoader(mainFxml, bundle);
            Parent mainRoot = mainLoader.load();

            MainController newController = mainLoader.getController();
            newController.setRestartCallback(this::restartCurrentProject);

            Scene oldScene = primaryStage.getScene();
            double width = oldScene != null ? oldScene.getWidth() : 1200;
            double height = oldScene != null ? oldScene.getHeight() : 800;
            primaryStage.setTitle(bundle.getString("status.title"));
            primaryStage.setScene(new Scene(mainRoot, width, height));

            this.mainController = newController;
            if (hasProject) {
                mainController.initializeProject(projectRoot, false);
                mainController.setPrimaryStage(primaryStage);
            } else {
                projectInitialized = false;
                showLauncher();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
