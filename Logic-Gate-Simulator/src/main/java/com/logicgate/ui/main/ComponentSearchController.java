package com.logicgate.ui.main;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.state.EditorContext;

import javafx.scene.canvas.Canvas;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class ComponentSearchController {
    private final EditorContext context;
    private final Canvas simulationCanvas;
    private final TextField searchTextField;
    private final ListView<SearchResult> searchResultsListView;

    public ComponentSearchController(
        EditorContext context,
        Canvas simulationCanvas,
        TextField searchTextField,
        ListView<SearchResult> searchResultsListView
    ) {
        this.context = context;
        this.simulationCanvas = simulationCanvas;
        this.searchTextField = searchTextField;
        this.searchResultsListView = searchResultsListView;
    }

    public void setup() {
        searchResultsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SearchResult result, boolean empty) {
                super.updateItem(result, empty);
                if (empty || result == null) {
                    setText(null);
                } else {
                    String icon = result.type().equals("Group") ? "📁" : "🧩";
                    setText(String.format("%s %s (%s)", icon, result.name(), result.type()));
                }
            }
        });

        searchResultsListView.getItems().addListener((javafx.collections.ListChangeListener<SearchResult>) c -> {
            int count = searchResultsListView.getItems().size();
            double cellHeight = 26.0;
            double height = Math.min(count, 5) * cellHeight + 2;
            searchResultsListView.setPrefHeight(height);
        });

        searchTextField.textProperty().addListener((obs, oldVal, newVal) -> updateResults(newVal));

        searchTextField.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                clear();
                simulationCanvas.requestFocus();
            }
        });

        searchResultsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, result) -> {
            if (result == null) return;
            selectResult(result);
        });
    }

    public void clear() {
        searchTextField.setText("");
        hideResults();
    }

    private void updateResults(String rawQuery) {
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            hideResults();
            return;
        }

        String query = rawQuery.toLowerCase();
        List<SearchResult> results = new ArrayList<>();

        context.visualNodes.stream()
            .filter(vn -> vn.label != null && vn.label.toLowerCase().contains(query))
            .forEach(vn -> results.add(new SearchResult(vn.label, vn.node.getTypeId(), vn)));

        Set<String> uniqueGroups = context.visualNodes.stream()
            .map(vn -> vn.group)
            .filter(g -> g != null && !g.isEmpty())
            .collect(Collectors.toSet());

        uniqueGroups.stream()
            .filter(g -> g.toLowerCase().contains(query))
            .forEach(g -> results.add(new SearchResult(g, "Group", g)));

        searchResultsListView.getItems().setAll(results);
        searchResultsListView.setVisible(!results.isEmpty());
        searchResultsListView.setManaged(!results.isEmpty());
    }

    private void hideResults() {
        searchResultsListView.setVisible(false);
        searchResultsListView.setManaged(false);
    }

    private void selectResult(SearchResult result) {
        if (result.target() instanceof VisualNode vn) {
            centerCameraOnNode(vn);
            context.selectedNodes.clear();
            context.selectedNodes.add(vn);
            context.setSelectedNode(vn);
        } else if (result.target() instanceof String groupName) {
            List<VisualNode> members = context.visualNodes.stream()
                .filter(vn -> groupName.equals(vn.group))
                .collect(Collectors.toList());

            if (!members.isEmpty()) {
                fitCameraToNodes(members);
                context.selectedNodes.clear();
                context.selectedNodes.addAll(members);
                context.setSelectedNode(members.get(members.size() - 1));
            }
        }
        context.selectedWire = null;
        context.selectedWires.clear();
    }

    private void centerCameraOnNode(VisualNode vn) {
        context.cameraX = (simulationCanvas.getWidth() / 2) - (vn.x + vn.width / 2) * context.zoom;
        context.cameraY = (simulationCanvas.getHeight() / 2) - (vn.y + vn.height / 2) * context.zoom;
        context.updateWorldCoordinates();
    }

    private void fitCameraToNodes(List<VisualNode> nodes) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

        for (VisualNode vn : nodes) {
            minX = Math.min(minX, vn.x);
            minY = Math.min(minY, vn.y);
            maxX = Math.max(maxX, vn.x + vn.width);
            maxY = Math.max(maxY, vn.y + vn.height);
        }

        double groupWidth = maxX - minX;
        double groupHeight = maxY - minY;
        double padding = 100.0;

        double availableWidth = simulationCanvas.getWidth();
        double availableHeight = simulationCanvas.getHeight();

        double zoomX = availableWidth / (groupWidth + padding);
        double zoomY = availableHeight / (groupHeight + padding);
        context.zoom = Math.max(0.2, Math.min(2.0, Math.min(zoomX, zoomY)));

        context.cameraX = (availableWidth / 2) - (minX + maxX) / 2 * context.zoom;
        context.cameraY = (availableHeight / 2) - (minY + maxY) / 2 * context.zoom;
        context.updateWorldCoordinates();
    }

    public record SearchResult(String name, String type, Object target) {
    }
}
