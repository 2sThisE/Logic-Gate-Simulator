package com.logicgate.ui;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class HelpController {

    @FXML private ListView<String> helpCategoryList;
    @FXML private StackPane helpContentArea;
    
    @FXML private VBox introPane;
    @FXML private VBox componentPane;
    @FXML private VBox wiringPane;
    @FXML private VBox simControlPane;
    @FXML private VBox shortcutPane;

    private Stage stage;

    @FXML
    public void initialize() {
        helpCategoryList.getItems().addAll(
            "시뮬레이터 소개",
            "부품 조작 가이드",
            "전선 연결 및 관리",
            "시뮬레이션 제어",
            "단축키 일람"
        );

        helpCategoryList.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            updateContent(newVal.intValue());
        });

        helpCategoryList.getSelectionModel().select(0);
    }

    private void updateContent(int index) {
        introPane.setVisible(index == 0);
        componentPane.setVisible(index == 1);
        wiringPane.setVisible(index == 2);
        simControlPane.setVisible(index == 3);
        shortcutPane.setVisible(index == 4);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void close() {
        if (stage != null) stage.close();
    }
}
