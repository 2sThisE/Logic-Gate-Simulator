package com.logicgate.ui.main;

import java.util.ArrayList;

import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.model.VisualWire;
import com.logicgate.editor.state.EditorContext;

import javafx.geometry.Point2D;
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
        if (context.wireBendEditMode && context.selectedWireBendIndex >= 0) {
            MenuItem deleteBendItem = new MenuItem("꺾임점 삭제");
            contextMenu.getItems().add(deleteBendItem);
            deleteBendItem.setOnAction(e -> deleteSelectedWireBend());
        }

        MenuItem addBendItem = new MenuItem("꺾임점 추가");
        MenuItem orthogonalItem = new MenuItem("직각 방식으로 전환");
        MenuItem curvedItem = new MenuItem("곡선 방식으로 전환");
        MenuItem resetRouteItem = new MenuItem("꺾임점 초기화");

        contextMenu.getItems().addAll(addBendItem, orthogonalItem, curvedItem);
        if (!context.selectedWire.bendPoints.isEmpty()) {
            contextMenu.getItems().add(resetRouteItem);
        }

        addBendItem.setOnAction(e -> addBendAtContextMenuPosition());
        orthogonalItem.setOnAction(e -> setSelectedWireRouteMode(VisualWire.RouteMode.ORTHOGONAL));
        curvedItem.setOnAction(e -> setSelectedWireRouteMode(VisualWire.RouteMode.CURVED));
        resetRouteItem.setOnAction(e -> resetSelectedWireRoute());

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
            context.selectedWireBendIndex = -1;
            context.wireBendEditMode = false;
            context.setDirty(true);
        });
    }

    private void addBendAtContextMenuPosition() {
        if (context.selectedWire == null) return;
        context.historyManager.saveState();

        VisualWire wire = context.selectedWire;
        wire.routeMode = wire.getEffectiveRouteMode(context.projectConfig != null ? context.projectConfig.wireStyle : null);

        Point2D target = new Point2D(snapToGrid(context.contextMenuWorldX), snapToGrid(context.contextMenuWorldY));
        int insertIndex = getBendInsertIndex(wire, target);
        wire.bendPoints.add(insertIndex, target);

        if (wire.routeMode == VisualWire.RouteMode.ORTHOGONAL) {
            normalizeOrthogonalBendPoints(wire, buildWirePath(wire));
            context.selectedWireBendIndex = findClosestBendIndex(wire, target);
        } else {
            context.selectedWireBendIndex = insertIndex;
        }

        context.wireBendEditMode = true;
        context.setDirty(true);
    }

    private void deleteSelectedWireBend() {
        if (context.selectedWire == null || context.selectedWireBendIndex < 0) return;
        context.historyManager.saveState();
        if (context.selectedWireBendIndex < context.selectedWire.bendPoints.size()) {
            context.selectedWire.bendPoints.remove(context.selectedWireBendIndex);
        }
        context.selectedWireBendIndex = -1;
        context.setDirty(true);
    }

    private void setSelectedWireRouteMode(VisualWire.RouteMode routeMode) {
        if (context.selectedWire == null) return;
        context.historyManager.saveState();
        context.selectedWire.routeMode = routeMode;
        if (routeMode == VisualWire.RouteMode.ORTHOGONAL) {
            normalizeOrthogonalBendPoints(context.selectedWire, buildWirePath(context.selectedWire));
        }
        context.setDirty(true);
    }

    private void resetSelectedWireRoute() {
        if (context.selectedWire == null) return;
        context.historyManager.saveState();
        context.selectedWire.bendPoints.clear();
        context.selectedWireBendIndex = -1;
        context.wireBendEditMode = false;
        context.setDirty(true);
    }

    private int getBendInsertIndex(VisualWire wire, Point2D point) {
        if (wire.bendPoints.isEmpty()) {
            return 0;
        }

        java.util.List<Point2D> points = buildWirePath(wire);
        double minDistance = Double.MAX_VALUE;
        int bestSegmentIndex = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            Point2D a = points.get(i);
            Point2D b = points.get(i + 1);
            double distance = distanceToSegment(point.getX(), point.getY(), a.getX(), a.getY(), b.getX(), b.getY());
            if (distance < minDistance) {
                minDistance = distance;
                bestSegmentIndex = i;
            }
        }
        return Math.max(0, Math.min(bestSegmentIndex, wire.bendPoints.size()));
    }

    private java.util.List<Point2D> buildWirePath(VisualWire wire) {
        java.util.ArrayList<Point2D> points = new java.util.ArrayList<>();
        Point2D start = new Point2D(wire.from.getOutPinX(wire.outPin), wire.from.getOutPinY(wire.outPin));
        Point2D end = new Point2D(wire.to.getInPinX(wire.inPin), wire.to.getInPinY(wire.inPin));
        VisualWire.RouteMode routeMode = wire.getEffectiveRouteMode(context.projectConfig != null ? context.projectConfig.wireStyle : null);

        if (routeMode == VisualWire.RouteMode.ORTHOGONAL) {
            points.add(start);
            if (wire.bendPoints.isEmpty()) {
                double midX = (start.getX() + end.getX()) / 2;
                points.add(new Point2D(midX, start.getY()));
                points.add(new Point2D(midX, end.getY()));
            } else {
                for (Point2D bendPoint : wire.bendPoints) {
                    appendOrthogonalPoint(points, bendPoint);
                }
            }
            appendOrthogonalPoint(points, end);
            return points;
        }

        points.add(start);
        points.addAll(wire.bendPoints);
        points.add(end);
        return points;
    }

    private void appendOrthogonalPoint(java.util.List<Point2D> points, Point2D target) {
        Point2D last = points.get(points.size() - 1);
        if (Math.abs(last.getX() - target.getX()) > 0.001 && Math.abs(last.getY() - target.getY()) > 0.001) {
            points.add(new Point2D(target.getX(), last.getY()));
        }
        if (points.get(points.size() - 1).distance(target) > 0.001) {
            points.add(target);
        }
    }

    private void normalizeOrthogonalBendPoints(VisualWire wire, java.util.List<Point2D> currentPath) {
        wire.bendPoints.clear();
        if (currentPath.size() > 2) {
            wire.bendPoints.addAll(currentPath.subList(1, currentPath.size() - 1));
        }
    }

    private int findClosestBendIndex(VisualWire wire, Point2D point) {
        int closest = -1;
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < wire.bendPoints.size(); i++) {
            double distance = wire.bendPoints.get(i).distance(point);
            if (distance < minDistance) {
                minDistance = distance;
                closest = i;
            }
        }
        return closest;
    }

    private double snapToGrid(double value) {
        double gridSize = com.logicgate.editor.rendering.symbol.GateSymbol.UNIT_SIZE;
        return Math.round(value / gridSize) * gridSize;
    }

    private double distanceToSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double l2 = Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2);
        if (l2 == 0) return Math.hypot(px - x1, py - y1);
        double t = Math.max(0, Math.min(1, ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2));
        double projX = x1 + t * (x2 - x1);
        double projY = y1 + t * (y2 - y1);
        return Math.hypot(px - projX, py - projY);
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
