package com.logicgate.ui;

import com.logicgate.editor.io.ProjectConfig;
import com.logicgate.editor.state.EditorContext;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class OptionsController {
    
    @FXML private ListView<String> categoryList;
    @FXML private VBox simPane;
    @FXML private VBox gridPane;
    @FXML private VBox visualPane;
    @FXML private VBox uxPane;

    @FXML private TextField tickFreqField;
    
    @FXML private CheckBox showGridCheck;
    @FXML private CheckBox snapToGridCheck;
    @FXML private Spinner<Integer> gridSizeSpinner;
    @FXML private CheckBox showAlignGuidesCheck;
    
    @FXML private ComboBox<String> wireStyleCombo;
    @FXML private CheckBox showWireStateCheck;
    @FXML private ColorPicker wireHighColorPicker;
    @FXML private ColorPicker wireLowColorPicker;
    @FXML private CheckBox defaultShowLabelCheck;
    
    @FXML private Spinner<Integer> autosaveSpinner;
    @FXML private Slider zoomSensSlider;
    @FXML private Label zoomSensLabel;

    private EditorContext context;
    private Stage stage;
    private ProjectConfig config;
    private Runnable onApplyCallback;

    public void setContext(EditorContext context, Stage stage, Runnable onApplyCallback) {
        this.context = context;
        this.stage = stage;
        this.config = context.projectConfig;
        this.onApplyCallback = onApplyCallback;
        
        initUI();
    }

    private void initUI() {
        if (config == null) return;

        // 좌측 리스트 뷰 설정 ✨
        categoryList.getItems().addAll("시뮬레이션", "에디터 & 그리드", "시각적 설정", "편의성");
        categoryList.getSelectionModel().select(0);
        
        categoryList.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            simPane.setVisible(newVal.intValue() == 0);
            gridPane.setVisible(newVal.intValue() == 1);
            visualPane.setVisible(newVal.intValue() == 2);
            uxPane.setVisible(newVal.intValue() == 3);
        });

        // 시뮬레이션
        tickFreqField.setText(String.format("%.0f", config.tickFrequencyHz));
        
        // 숫자와 소수점만 입력 가능하게 필터링 ✨
        tickFreqField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                tickFreqField.setText(newVal.replaceAll("[^\\d.]", ""));
            }
        });

        // 에디터 & 그리드
        showGridCheck.setSelected(config.showGrid);
        snapToGridCheck.setSelected(config.snapToGrid);
        
        // 픽셀 단위를 칸(Unit) 단위로 변환하여 표시 (1 Unit = 10px) ✨
        int unitSize = (int) com.logicgate.editor.rendering.symbol.GateSymbol.UNIT_SIZE;
        int currentUnits = config.gridSize / unitSize;
        gridSizeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, currentUnits, 1));
        
        showAlignGuidesCheck.setSelected(config.showAlignmentGuides);

        // 시각적 설정
        wireStyleCombo.getItems().addAll("Curved", "Orthogonal");
        wireStyleCombo.setValue(config.wireStyle);
        showWireStateCheck.setSelected(config.showWireState);
        wireHighColorPicker.setValue(Color.web(config.wireHighColor));
        wireLowColorPicker.setValue(Color.web(config.wireLowColor));
        
        // 상태 변화 토글에 따른 색상 피커 활성/비활성화
        wireHighColorPicker.disableProperty().bind(showWireStateCheck.selectedProperty().not());
        defaultShowLabelCheck.setSelected(config.defaultShowLabel);

        // 편의성
        autosaveSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 60, config.autosaveIntervalMin, 1));
        zoomSensSlider.setValue(config.cameraZoomSensitivity);
        zoomSensLabel.setText(String.format("%.2f", config.cameraZoomSensitivity));
        zoomSensSlider.valueProperty().addListener((obs, oldVal, newVal) -> 
            zoomSensLabel.setText(String.format("%.2f", newVal.doubleValue()))
        );
    }

    @FXML
    private void apply() {
        if (config != null) {
            try {
                double hz = Double.parseDouble(tickFreqField.getText());
                if (hz <= 0) hz = 1.0;
                config.tickFrequencyHz = hz;
            } catch (NumberFormatException e) {
                config.tickFrequencyHz = 60.0;
            }
            
            config.showGrid = showGridCheck.isSelected();
            config.snapToGrid = snapToGridCheck.isSelected();
            
            // 칸(Unit) 단위를 다시 픽셀 단위로 변환하여 저장 ✨
            int unitSize = (int) com.logicgate.editor.rendering.symbol.GateSymbol.UNIT_SIZE;
            config.gridSize = gridSizeSpinner.getValue() * unitSize;
            
            config.showAlignmentGuides = showAlignGuidesCheck.isSelected();
            
            config.wireStyle = wireStyleCombo.getValue();
            config.showWireState = showWireStateCheck.isSelected();
            config.wireHighColor = toHex(wireHighColorPicker.getValue());
            config.wireLowColor = toHex(wireLowColorPicker.getValue());
            config.defaultShowLabel = defaultShowLabelCheck.isSelected();
            
            config.autosaveIntervalMin = autosaveSpinner.getValue();
            config.cameraZoomSensitivity = zoomSensSlider.getValue();
            
            context.setDirty(true);
            if (onApplyCallback != null) onApplyCallback.run();
        }
        stage.close();
    }

    @FXML
    private void cancel() {
        stage.close();
    }

    private String toHex(Color color) {
        return String.format("#%02X%02X%02X", 
            (int)(color.getRed() * 255), 
            (int)(color.getGreen() * 255), 
            (int)(color.getBlue() * 255));
    }
}
