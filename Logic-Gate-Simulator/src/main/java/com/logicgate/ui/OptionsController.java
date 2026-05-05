package com.logicgate.ui;

import com.logicgate.editor.io.ProjectConfig;
import com.logicgate.editor.state.EditorContext;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.prefs.Preferences;

public class OptionsController {

    @FXML private BorderPane optionsRoot;
    @FXML private VBox sidebar;
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
    @FXML private ComboBox<String> languageCombo;

    private EditorContext context;
    private Stage stage;
    private ProjectConfig config;
    private Predicate<Boolean> beforeApplyCallback;
    private Consumer<Boolean> afterApplyCallback;
    private Preferences prefs;

    @FXML
    public void initialize() {
        ResponsiveTextSupport.apply(optionsRoot);
        ResponsiveTextSupport.useWrappingCells(categoryList);
    }

    public void setContext(
            EditorContext context,
            Stage stage,
            Predicate<Boolean> beforeApplyCallback,
            Consumer<Boolean> afterApplyCallback) {
        this.context = context;
        this.stage = stage;
        this.config = context.projectConfig;
        this.beforeApplyCallback = beforeApplyCallback;
        this.afterApplyCallback = afterApplyCallback;
        this.prefs = Preferences.userNodeForPackage(MainApp.class);

        initUI();
    }

    private void initUI() {
        if (config == null) return;

        ResourceBundle bundle = ResourceBundle.getBundle("com.logicgate.ui.strings", java.util.Locale.getDefault());

        categoryList.getItems().addAll(
            bundle.getString("options.category.sim"),
            bundle.getString("options.category.grid"),
            bundle.getString("options.category.visual"),
            bundle.getString("options.category.ux")
        );
        ResponsiveTextSupport.fitListWidthToItems(categoryList, sidebar, 180, 260);
        categoryList.getSelectionModel().select(0);

        categoryList.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            simPane.setVisible(newVal.intValue() == 0);
            gridPane.setVisible(newVal.intValue() == 1);
            visualPane.setVisible(newVal.intValue() == 2);
            uxPane.setVisible(newVal.intValue() == 3);
        });

        tickFreqField.setText(String.format("%.0f", config.tickFrequencyHz));

        tickFreqField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                tickFreqField.setText(newVal.replaceAll("[^\\d.]", ""));
            }
        });

        showGridCheck.setSelected(config.showGrid);
        snapToGridCheck.setSelected(config.snapToGrid);

        int unitSize = (int) com.logicgate.editor.rendering.symbol.GateSymbol.UNIT_SIZE;
        int currentUnits = config.gridSize / unitSize;
        gridSizeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, currentUnits, 1));

        showAlignGuidesCheck.setSelected(config.showAlignmentGuides);

        wireStyleCombo.getItems().addAll("Curved", "Orthogonal");
        wireStyleCombo.setValue(config.wireStyle);
        showWireStateCheck.setSelected(config.showWireState);
        wireHighColorPicker.setValue(Color.web(config.wireHighColor));
        wireLowColorPicker.setValue(Color.web(config.wireLowColor));

        wireHighColorPicker.disableProperty().bind(showWireStateCheck.selectedProperty().not());
        defaultShowLabelCheck.setSelected(config.defaultShowLabel);

        autosaveSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 60, config.autosaveIntervalMin, 1));
        zoomSensSlider.setValue(config.cameraZoomSensitivity);
        zoomSensLabel.setWrapText(false);
        zoomSensLabel.setMinWidth(42);
        zoomSensLabel.setPrefWidth(42);
        zoomSensLabel.setText(String.format("%.2f", config.cameraZoomSensitivity));
        zoomSensSlider.valueProperty().addListener((obs, oldVal, newVal) ->
            zoomSensLabel.setText(String.format("%.2f", newVal.doubleValue()))
        );

        String systemDefaultLabel = bundle.getString("options.language.system_default");
        String englishLabel = bundle.getString("options.language.english");
        String koreanLabel = bundle.getString("options.language.korean");
        languageCombo.getItems().addAll(systemDefaultLabel, englishLabel, koreanLabel);
        String currentLang = prefs.get("language", "system");
        if ("ko".equals(currentLang)) {
            languageCombo.setValue(koreanLabel);
        } else if ("en".equals(currentLang)) {
            languageCombo.setValue(englishLabel);
        } else {
            languageCombo.setValue(systemDefaultLabel);
        }
    }

    @FXML
    private void apply() {
        if (config != null) {
            ResourceBundle bundle = ResourceBundle.getBundle("com.logicgate.ui.strings", java.util.Locale.getDefault());
            String previousLang = prefs.get("language", "system");
            String selectedLang = languageCombo.getValue();
            String nextLang;
            if (bundle.getString("options.language.korean").equals(selectedLang)) {
                nextLang = "ko";
            } else if (bundle.getString("options.language.english").equals(selectedLang)) {
                nextLang = "en";
            } else {
                nextLang = "system";
            }

            boolean languageChanged = !previousLang.equals(nextLang);
            if (beforeApplyCallback != null && !beforeApplyCallback.test(languageChanged)) {
                return;
            }

            try {
                double hz = Double.parseDouble(tickFreqField.getText());
                if (hz <= 0) hz = 1.0;
                config.tickFrequencyHz = hz;
            } catch (NumberFormatException e) {
                config.tickFrequencyHz = 60.0;
            }

            config.showGrid = showGridCheck.isSelected();
            config.snapToGrid = snapToGridCheck.isSelected();

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

            prefs.put("language", nextLang);

            context.setDirty(true);
            stage.close();
            if (afterApplyCallback != null) afterApplyCallback.accept(languageChanged);
            return;
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
