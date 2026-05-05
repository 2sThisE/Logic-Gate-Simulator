package com.logicgate.ui;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;

public final class ResponsiveTextSupport {
    private ResponsiveTextSupport() {
    }

    public static void apply(Node root) {
        if (root == null) return;

        if (root instanceof Label || root instanceof CheckBox) {
            Labeled labeled = (Labeled) root;
            boolean fixedValue = root.getStyleClass().contains("options-value");
            labeled.setWrapText(!fixedValue);
            if (fixedValue) {
                labeled.setMinWidth(Region.USE_PREF_SIZE);
            } else {
                labeled.setMinWidth(0);
                labeled.setMaxWidth(Double.MAX_VALUE);
            }
            if (!fixedValue && root.getParent() instanceof HBox) {
                HBox.setHgrow(root, Priority.ALWAYS);
            }
        }

        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                apply(child);
            }
        }
    }

    public static <T> void useWrappingCells(ListView<T> listView) {
        if (listView == null) return;

        listView.setCellFactory(view -> {
            ListCell<T> cell = new ListCell<>() {
                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.toString());
                }
            };
            cell.setWrapText(true);
            cell.setMinWidth(0);
            cell.setTextOverrun(OverrunStyle.CLIP);
            cell.prefWidthProperty().bind(view.widthProperty().subtract(4));
            cell.maxWidthProperty().bind(view.widthProperty().subtract(4));
            return cell;
        });
    }

    public static void fitListWidthToItems(ListView<?> listView, Region container, double minWidth, double maxWidth) {
        if (listView == null || container == null) return;

        double widest = minWidth;
        for (Object item : listView.getItems()) {
            if (item == null) continue;
            Text text = new Text(item.toString());
            text.setFont(Font.getDefault());
            widest = Math.max(widest, text.getLayoutBounds().getWidth() + 72);
        }

        double width = Math.max(minWidth, Math.min(maxWidth, Math.ceil(widest)));
        container.setMinWidth(width);
        container.setPrefWidth(width);
    }

    public static void fitStageToContent(Stage stage, Parent root, double maxWidthRatio, double maxHeightRatio) {
        if (stage == null || root == null) return;

        if (root.getScene() == null) {
            stage.setScene(new Scene(root));
        }

        root.applyCss();
        root.layout();
        stage.sizeToScene();

        var bounds = Screen.getPrimary().getVisualBounds();
        double maxWidth = bounds.getWidth() * maxWidthRatio;
        double maxHeight = bounds.getHeight() * maxHeightRatio;
        stage.setWidth(Math.min(stage.getWidth(), maxWidth));
        stage.setHeight(Math.min(stage.getHeight(), maxHeight));
        stage.centerOnScreen();
    }
}
