package com.logicgate.ui.main;

import java.util.ArrayList;

import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.state.EditorContext;

import javafx.scene.canvas.Canvas;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

public class EditorContextMenuController {
    private final EditorContext context;
    private final Canvas simulationCanvas;
    private final PropertyPaneController propertyPaneController;
    private final ContextMenu contextMenu = new ContextMenu();

    public EditorContextMenuController(
        EditorContext context,
        Canvas simulationCanvas,
        PropertyPaneController propertyPaneController
    ) {
        this.context = context;
        this.simulationCanvas = simulationCanvas;
        this.propertyPaneController = propertyPaneController;
    }

    public void setup() {
        context.onContextMenuRequested = this::updateAndShow;
    }

    public void hide() {
        if (contextMenu.isShowing()) contextMenu.hide();
    }

    private void updateAndShow(double screenX, double screenY) {
        hide();
        contextMenu.getItems().clear();

        MenuItem deleteItem = new MenuItem("삭제");

        if (!context.selectedNodes.isEmpty()) {
            addNodeSelectionItems(deleteItem);
        } else if (context.selectedWire != null) {
            addWireSelectionItems(deleteItem);
        } else {
            return;
        }

        contextMenu.show(simulationCanvas, screenX, screenY);
    }

    private void addNodeSelectionItems(MenuItem deleteItem) {
        MenuItem groupItem = new MenuItem("그룹화");
        MenuItem ungroupItem = new MenuItem("그룹화 취소");

        boolean hasUngrouped = false;
        boolean hasGrouped = false;

        for (VisualNode vn : context.selectedNodes) {
            if (vn.group == null) {
                hasUngrouped = true;
            } else {
                hasGrouped = true;
            }
        }

        if (hasUngrouped) {
            contextMenu.getItems().add(groupItem);
            groupItem.setOnAction(e -> groupSelectedNodes());
        } else if (hasGrouped) {
            contextMenu.getItems().add(ungroupItem);
            ungroupItem.setOnAction(e -> ungroupSelectedNodes());
        }

        contextMenu.getItems().add(deleteItem);
        deleteItem.setOnAction(e -> deleteSelectedNodes());
    }

    private void addWireSelectionItems(MenuItem deleteItem) {
        contextMenu.getItems().add(deleteItem);
        deleteItem.setOnAction(e -> {
            context.historyManager.saveState();
            context.getCircuit().disconnectSpecific(
                context.selectedWire.from.node,
                context.selectedWire.outPin,
                context.selectedWire.to.node,
                context.selectedWire.inPin
            );
            context.visualWires.remove(context.selectedWire);
            context.selectedWire = null;
            context.setDirty(true);
        });
    }

    private void groupSelectedNodes() {
        context.historyManager.saveState();
        String targetGroup = null;
        for (VisualNode vn : context.selectedNodes) {
            if (vn.group != null) {
                targetGroup = vn.group;
                break;
            }
        }
        if (targetGroup == null) {
            int n = 1;
            while (true) {
                targetGroup = "Group " + n;
                if (!isGroupExists(targetGroup, null)) break;
                n++;
            }
        }
        for (VisualNode vn : context.selectedNodes) {
            vn.group = targetGroup;
        }
        context.setDirty(true);
        propertyPaneController.update();
    }

    private void ungroupSelectedNodes() {
        context.historyManager.saveState();
        for (VisualNode vn : context.selectedNodes) {
            vn.group = null;
        }
        context.setDirty(true);
        propertyPaneController.update();
    }

    private void deleteSelectedNodes() {
        context.historyManager.saveState();
        for (VisualNode vn : new ArrayList<>(context.selectedNodes)) {
            removeNode(vn);
        }
        context.selectedNodes.clear();
        context.setSelectedNode(null);
    }

    private void removeNode(VisualNode vn) {
        context.getCircuit().removeNode(vn.node);
        context.visualNodes.remove(vn);
        context.visualWires.removeIf(w -> {
            boolean related = w.from == vn || w.to == vn;
            if (related && w == context.selectedWire) context.selectedWire = null;
            return related;
        });
        context.setDirty(true);
    }

    private boolean isGroupExists(String name, String excludeGroup) {
        if (name == null || name.isEmpty()) return false;
        for (VisualNode vn : context.visualNodes) {
            if (name.equals(vn.group) && (excludeGroup == null || !excludeGroup.equals(vn.group))) {
                return true;
            }
        }
        return false;
    }
}
