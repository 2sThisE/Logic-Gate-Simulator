package com.logicgate.ui.main;

import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.state.EditorContext;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class PropertyPaneController {
    private final EditorContext context;
    private final VBox propertyPane;

    public PropertyPaneController(EditorContext context, VBox propertyPane) {
        this.context = context;
        this.propertyPane = propertyPane;
    }

    public void setup() {
        context.onSelectionChanged = this::update;
        update();
    }

    @SuppressWarnings("unchecked")
    public void update() {
        propertyPane.getChildren().clear();
        VisualNode selected = context.getSelectedNode();

        if (selected == null) {
            propertyPane.setDisable(true);
            Label placeholder = new Label("선택된 컴포넌트 없음");
            placeholder.setStyle("-fx-text-fill: #888888; -fx-font-style: italic;");
            propertyPane.getChildren().add(placeholder);
            return;
        }

        propertyPane.setDisable(false);

        for (com.logicgate.editor.model.Property<?> prop : selected.getProperties(context)) {
            VBox row = new VBox(5);
            Label nameLabel = new Label(prop.getName());
            nameLabel.getStyleClass().add("property-label");
            row.getChildren().add(nameLabel);

            switch (prop.getType()) {
                case STRING -> {
                    TextField tf = new TextField((String) prop.getValue());
                    boolean[] editSnapshotSaved = { false };
                    tf.textProperty().addListener((obs, oldVal, newVal) -> {
                        if (tf.isFocused() && !editSnapshotSaved[0]) {
                            context.historyManager.saveState();
                            editSnapshotSaved[0] = true;
                        }
                        ((com.logicgate.editor.model.Property<String>) prop).setValue(newVal);
                    });
                    tf.focusedProperty().addListener((obs, oldF, newF) -> {
                        if (newF) editSnapshotSaved[0] = false;
                    });
                    row.getChildren().add(tf);
                }
                case BOOLEAN -> {
                    CheckBox cb = new CheckBox("");
                    cb.setSelected((Boolean) prop.getValue());
                    cb.selectedProperty().addListener((obs, oldVal, newVal) -> {
                        context.historyManager.saveState();
                        ((com.logicgate.editor.model.Property<Boolean>) prop).setValue(newVal);
                    });
                    row.getChildren().add(cb);
                }
                case COLOR -> {
                    ColorPicker cp = new ColorPicker(javafx.scene.paint.Color.web((String) prop.getValue()));
                    cp.setMaxWidth(Double.MAX_VALUE);
                    cp.setOnAction(e -> {
                        context.historyManager.saveState();
                        javafx.scene.paint.Color c = cp.getValue();
                        String hex = String.format("#%02X%02X%02X",
                            (int)(c.getRed() * 255), (int)(c.getGreen() * 255), (int)(c.getBlue() * 255));
                        ((com.logicgate.editor.model.Property<String>) prop).setValue(hex);
                    });
                    row.getChildren().add(cp);
                }
                case INTEGER -> {
                    Slider slider = new Slider(2, 8, (Integer) prop.getValue());
                    slider.setShowTickLabels(true);
                    slider.setShowTickMarks(true);
                    slider.setMajorTickUnit(1);
                    slider.setSnapToTicks(true);
                    slider.setMinorTickCount(0);
                    slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                        if (!slider.isValueChanging()) {
                            context.historyManager.saveState();
                            ((com.logicgate.editor.model.Property<Integer>) prop).setValue(newVal.intValue());
                            context.setDirty(true);
                        }
                    });
                    row.getChildren().add(slider);
                }
                case CHOICE -> {
                    ComboBox<String> combo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(prop.getOptions()));
                    combo.setValue((String) prop.getValue());
                    combo.setMaxWidth(Double.MAX_VALUE);
                    combo.setOnAction(e -> {
                        context.historyManager.saveState();
                        ((com.logicgate.editor.model.Property<String>) prop).setValue(combo.getValue());
                    });
                    row.getChildren().add(combo);
                }
            }
            propertyPane.getChildren().add(row);
        }
    }
}
