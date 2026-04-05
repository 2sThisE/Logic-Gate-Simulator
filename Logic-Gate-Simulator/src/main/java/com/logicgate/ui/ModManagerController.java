package com.logicgate.ui;

import com.logicgate.editor.state.EditorContext;
import com.logicgate.editor.io.ProjectManager;
import com.logicgate.editor.mod.JarSecurityScanner;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ModManagerController {

    @FXML
    private ListView<String> modListView;

    private EditorContext context;
    private ProjectManager projectManager;
    private Stage stage;
    private boolean isChanged = false; // 변경 여부 추적 ✨

    public void setContext(EditorContext context, ProjectManager projectManager) {
        this.context = context;
        this.projectManager = projectManager;
        loadModList();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public boolean isChanged() {
        return isChanged;
    }

    private void loadModList() {
        if (context.projectConfig != null) {
            modListView.setItems(FXCollections.observableArrayList(context.projectConfig.loadedMods));
        }
    }
@FXML
public void addMod() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("추가할 모드 파일(.jar) 선택");
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java Archive", "*.jar"));
    File selectedFile = fileChooser.showOpenDialog(stage);

    if (selectedFile != null) {
        
        // 보안 스캔 시작 🔪💕
        List<String> suspicious = JarSecurityScanner.scanJarForSuspiciousClasses(selectedFile);
        if (!suspicious.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("보안 경고: 미확인 클래스 참조 감지");
            alert.setHeaderText("이 모드는 화이트리스트에 등록되지 않은 외부 클래스를 참조하고 있습니다.");
            alert.setContentText("악성 코드가 포함되어 있을 위험이 있습니다. 정말로 이 모드를 추가하시겠습니까?");

            TextArea textArea = new TextArea(String.join("\n", suspicious));
            textArea.setEditable(false);
            textArea.setWrapText(false);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(new javafx.scene.control.Label("의심스러운 참조 목록:"), 0, 0);
            expContent.add(textArea, 0, 1);

            alert.getDialogPane().setExpandableContent(expContent);
            alert.getDialogPane().setExpanded(true);

            ButtonType btnYes = new ButtonType("무시하고 추가");
            ButtonType btnNo = new ButtonType("취소", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(btnYes, btnNo);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty() || result.get() == btnNo) {
                return; // 추가 취소 ✨
            }
        }

        File modsDir = new File(context.projectRoot, "mods");
        if (!modsDir.exists()) modsDir.mkdirs();

        File targetFile = new File(modsDir, selectedFile.getName());
        boolean copySuccess = false;
        try {
            Files.copy(selectedFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            copySuccess = true;
        } catch (IOException e) {
            if (targetFile.exists()) {
                System.out.println("[ModManager] 파일이 이미 존재하거나 사용 중입니다. 기존 파일을 유지합니다. 🔪💕");
                copySuccess = true; // 이미 있으니 성공으로 간주
            } else {
                e.printStackTrace();
                showError("모드 추가 실패", "파일을 복사할 수 없습니다: " + e.getMessage());
            }
        }

        if (copySuccess) {
            if (!context.projectConfig.loadedMods.contains(selectedFile.getName())) {
                context.projectConfig.loadedMods.add(selectedFile.getName());
                projectManager.saveProjectConfig();
                isChanged = true;
                loadModList();
            }
        }
    }
}

@FXML
public void removeMod() {
    String selected = modListView.getSelectionModel().getSelectedItem();
    if (selected != null) {
        context.projectConfig.loadedMods.remove(selected);
        projectManager.saveProjectConfig();

        File modFile = new File(context.projectRoot, "mods" + File.separator + selected);
        if (modFile.exists()) {
            if (!modFile.delete()) {
                System.out.println("[ModManager] 파일이 사용 중이라 물리적으로 삭제하지 못했습니다. 설정에서만 제거합니다. ✨");
                // 굳이 에러 창을 띄우진 않고 로그만 남김 (사용자가 앱을 끄면 삭제 가능)
            }
        }

        isChanged = true;
        loadModList();
    }
}

private void showError(String title, String content) {
    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
}
    @FXML
    public void close() {
        stage.close();
    }
}
