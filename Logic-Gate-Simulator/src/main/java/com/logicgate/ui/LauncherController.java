package com.logicgate.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.util.Callback;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class LauncherController {

    @FXML private VBox recentProjectsView;
    @FXML private VBox newProjectView;
    
    @FXML private ListView<RecentProject> recentProjectsList;
    @FXML private TextField projectNameField;
    @FXML private TextField projectLocationField;

    private Stage stage;
    private Preferences prefs;

    /**
     * 프로젝트 선택 결과를 담는 클래스 ✨
     */
    public static class ProjectResult {
        public final File root;
        public final boolean isNew;
        public ProjectResult(File root, boolean isNew) {
            this.root = root;
            this.isNew = isNew;
        }
    }

    private ProjectResult result = null;

    public ProjectResult getResult() {
        return result;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void initialize() {
        prefs = Preferences.userNodeForPackage(LauncherController.class);
        
        recentProjectsList.setCellFactory(new Callback<ListView<RecentProject>, ListCell<RecentProject>>() {
            @Override
            public ListCell<RecentProject> call(ListView<RecentProject> param) {
                return new ListCell<RecentProject>() {
                    @Override
                    protected void updateItem(RecentProject item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            VBox vbox = new VBox();
                            Label nameLbl = new Label(item.name);
                            nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: white;");
                            Label pathLbl = new Label(item.path);
                            pathLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");
                            vbox.getChildren().addAll(nameLbl, pathLbl);
                            setGraphic(vbox);
                        }
                    }
                };
            }
        });

        recentProjectsList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                RecentProject selected = recentProjectsList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openProjectByFile(new File(selected.path));
                }
            }
        });

        loadRecentProjects();
        showRecentProjects();
    }

    @FXML
    public void showRecentProjects() {
        recentProjectsView.setVisible(true);
        newProjectView.setVisible(false);
        loadRecentProjects();
    }

    @FXML
    public void showNewProject() {
        recentProjectsView.setVisible(false);
        newProjectView.setVisible(true);
        if (projectLocationField.getText().isEmpty()) {
            projectLocationField.setText(System.getProperty("user.home") + File.separator + "LogicGateProjects");
        }
    }

    @FXML
    public void browseNewProjectLocation() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("프로젝트 생성 위치 선택");
        File defaultDir = new File(projectLocationField.getText());
        if (defaultDir.exists()) {
            dirChooser.setInitialDirectory(defaultDir);
        }
        File selectedDir = dirChooser.showDialog(projectNameField.getScene().getWindow());
        if (selectedDir != null) {
            projectLocationField.setText(selectedDir.getAbsolutePath());
        }
    }

    @FXML
    public void createNewProject() {
        String projectName = projectNameField.getText();
        String projectPath = projectLocationField.getText();

        if (projectName == null || projectName.trim().isEmpty()) {
            showAlert("오류", "프로젝트 이름을 입력해주세요.");
            return;
        }
        if (projectPath == null || projectPath.trim().isEmpty()) {
            showAlert("오류", "프로젝트 위치를 지정해주세요.");
            return;
        }

        File baseDir = new File(projectPath);
        File projectRoot = new File(baseDir, projectName.trim());
        
        if (!projectRoot.exists()) {
            projectRoot.mkdirs();
        }

        addRecentProject(projectName.trim(), projectRoot.getAbsolutePath());
        this.result = new ProjectResult(projectRoot, true); // 결과 저장 ✨
        stage.close();
    }

    @FXML
    public void openExistingProject() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("프로젝트 폴더 선택");
        File selectedDir = dirChooser.showDialog(projectNameField.getScene().getWindow());

        if (selectedDir != null) {
            openProjectByFile(selectedDir);
        }
    }

    private void openProjectByFile(File dir) {
        File prjFile = new File(dir, "project.prj");
        if (!prjFile.exists()) {
            showAlert("오류", "선택한 폴더에 project.prj 파일이 없습니다. 올바른 프로젝트 폴더를 선택해주세요.");
            return;
        }
        addRecentProject(dir.getName(), dir.getAbsolutePath());
        this.result = new ProjectResult(dir, false); // 결과 저장 ✨
        stage.close();
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

    private void loadRecentProjects() {
        List<RecentProject> list = new ArrayList<>();
        String recentStr = prefs.get("recent_projects", "");
        if (!recentStr.isEmpty()) {
            String[] entries = recentStr.split(";");
            for (String entry : entries) {
                String[] parts = entry.split("\\|");
                if (parts.length == 2) {
                    File f = new File(parts[1]);
                    if(f.exists()) {
                        list.add(new RecentProject(parts[0], parts[1]));
                    }
                }
            }
        }
        recentProjectsList.setItems(FXCollections.observableArrayList(list));
    }

    private void addRecentProject(String name, String path) {
        List<RecentProject> list = new ArrayList<>(recentProjectsList.getItems());
        list.removeIf(p -> p.path.equals(path));
        list.add(0, new RecentProject(name, path));
        if (list.size() > 10) {
            list = list.subList(0, 10);
        }
        
        StringBuilder sb = new StringBuilder();
        for (RecentProject p : list) {
            sb.append(p.name).append("|").append(p.path).append(";");
        }
        prefs.put("recent_projects", sb.toString());
    }

    public static class RecentProject {
        public String name;
        public String path;
        public RecentProject(String name, String path) {
            this.name = name;
            this.path = path;
        }
    }
}