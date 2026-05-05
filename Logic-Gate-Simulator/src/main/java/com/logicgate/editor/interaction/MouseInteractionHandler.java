package com.logicgate.editor.interaction;

import com.logicgate.editor.io.NodeData;
import com.logicgate.editor.io.WireData;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.model.VisualWire;
import com.logicgate.editor.rendering.symbol.GateSymbol;
import com.logicgate.editor.state.EditorContext;
import com.logicgate.editor.utils.NodeFactory;
import com.logicgate.gates.InputPin;
import com.logicgate.gates.Node;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.geometry.Point2D;
import java.util.List;

public class MouseInteractionHandler {
    private final EditorContext context;
    private final WiringManager wiringManager;

    public MouseInteractionHandler(EditorContext context, WiringManager wiringManager) {
        this.context = context;
        this.wiringManager = wiringManager;
    }

    private double getGridSize() {
        return GateSymbol.UNIT_SIZE;
    }

    private double snapToGrid(double value) {
        double gridSize = getGridSize();
        return Math.round(value / gridSize) * gridSize;
    }

    private void applyPlacementSnapping(MouseEvent event) {
        context.snapLineX = null;
        context.snapLineY = null;

        if (event.isShiftDown()) return;

        double targetX;
        double targetY;
        double nodeWidth;
        double nodeHeight;

        if (context.placingNodeTypeId != null) {
            Node logicNode = NodeFactory.createNodeByType(context.placingNodeTypeId);
            if (logicNode != null) {
                VisualNode dummyVn = new VisualNode(logicNode, 0, 0, "");
                nodeWidth = dummyVn.width;
                nodeHeight = dummyVn.height;
                targetX = context.worldMouseX - (nodeWidth / 2);
                targetY = context.worldMouseY - (nodeHeight / 2);
            } else {
                return;
            }
        } else if (context.isPlacingImport && context.pendingProjectData != null && !context.pendingProjectData.nodes.isEmpty()) {
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

            for (NodeData nd : context.pendingProjectData.nodes) {
                Node logicNode = NodeFactory.createNodeByType(nd.type);
                if (logicNode == null) continue;
                VisualNode dummyVn = new VisualNode(logicNode, 0, 0, nd.label);
                minX = Math.min(minX, nd.x);
                minY = Math.min(minY, nd.y);
                maxX = Math.max(maxX, nd.x + dummyVn.width);
                maxY = Math.max(maxY, nd.y + dummyVn.height);
            }

            if (minX == Double.MAX_VALUE) return;

            targetX = context.worldMouseX + minX;
            targetY = context.worldMouseY + minY;
            nodeWidth = maxX - minX;
            nodeHeight = maxY - minY;
        } else {
            return;
        }

        if (context.projectConfig != null && context.projectConfig.snapToGrid) {
            double snappedX = snapToGrid(targetX);
            double snappedY = snapToGrid(targetY);
            context.worldMouseX += snappedX - targetX;
            context.worldMouseY += snappedY - targetY;
            targetX = snappedX;
            targetY = snappedY;
        }

        double snapThreshold = 8.0 / context.zoom;
        double maxSnapDistance = 600.0;

        double[] primaryXs = { targetX, targetX + nodeWidth / 2, targetX + nodeWidth };
        double[] primaryYs = { targetY, targetY + nodeHeight / 2, targetY + nodeHeight };

        double minDiffX = snapThreshold;
        double minDiffY = snapThreshold;
        boolean snappedX = false;
        boolean snappedY = false;
        Double bestSnapLineX = null;
        Double bestSnapLineY = null;

        if (context.projectConfig == null || context.projectConfig.showAlignmentGuides) {
            for (VisualNode other : context.visualNodes) {
                if (context.selectedNodes.contains(other)) continue;

                double dist = Math.hypot(targetX - other.x, targetY - other.y);
                if (dist > maxSnapDistance) continue;

                double[] otherXs = { other.x, other.x + other.width / 2, other.x + other.width };
                double[] otherYs = { other.y, other.y + other.height / 2, other.y + other.height };

                for (double px : primaryXs) {
                    for (double ox : otherXs) {
                        double diff = ox - px;
                        if (Math.abs(diff) < Math.abs(minDiffX)) {
                            minDiffX = diff;
                            snappedX = true;
                            bestSnapLineX = ox;
                        }
                    }
                }

                for (double py : primaryYs) {
                    for (double oy : otherYs) {
                        double diff = oy - py;
                        if (Math.abs(diff) < Math.abs(minDiffY)) {
                            minDiffY = diff;
                            snappedY = true;
                            bestSnapLineY = oy;
                        }
                    }
                }
            }

            if (snappedX) {
                context.worldMouseX += minDiffX;
                context.snapLineX = bestSnapLineX;
            }
            if (snappedY) {
                context.worldMouseY += minDiffY;
                context.snapLineY = bestSnapLineY;
            }
        }
    }

    public void handleMouseMoved(MouseEvent event) {
        context.screenMouseX = event.getX();
        context.screenMouseY = event.getY();
        context.updateWorldCoordinates();
        applyPlacementSnapping(event);
        updateHoverState();
    }

    public void handleMouseScrolled(ScrollEvent event) {
        double zoomFactor = context.projectConfig != null ? context.projectConfig.cameraZoomSensitivity : 1.1;
        double oldZoom = context.zoom;

        if (event.getDeltaY() > 0) {
            context.zoom *= zoomFactor;
        } else if (event.getDeltaY() < 0) {
            context.zoom /= zoomFactor;
        }

        context.zoom = Math.max(0.1, Math.min(context.zoom, 5.0));

        double f = (context.zoom / oldZoom) - 1;
        context.cameraX -= (event.getX() - context.cameraX) * f;
        context.cameraY -= (event.getY() - context.cameraY) * f;

        context.updateWorldCoordinates();
        updateHoverState();
    }

    public void handleMousePressed(MouseEvent event) {
        context.screenMouseX = event.getX();
        context.screenMouseY = event.getY();
        context.updateWorldCoordinates();
        applyPlacementSnapping(event);
        updateHoverState();

        if (event.getButton() == MouseButton.PRIMARY) {
            if (context.isPlacingImport) {
                finalizePlacement();
                return;
            }

            if (context.placingNodeTypeId != null) {
                Node logicNode = NodeFactory.createNodeByType(context.placingNodeTypeId);
                if (logicNode != null) {
                    context.historyManager.saveState();
                    context.getCircuit().addNode(logicNode);

                    VisualNode vn = new VisualNode(logicNode, 0, 0, "");
                    if (context.projectConfig != null) {
                        vn.showLabel = context.projectConfig.defaultShowLabel;
                    }
                    vn.x = context.worldMouseX - (vn.width / 2);
                    vn.y = context.worldMouseY - (vn.height / 2);
                    vn.rotation = context.placingRotation;
                    context.visualNodes.add(vn);
                    context.setDirty(true);
                }
                return;
            }

            if (context.hoveredNode != null) {
                if (context.hoveredOutPin != -1) {
                    wiringManager.startWiring(context.hoveredNode, context.hoveredOutPin, true);
                    return;
                } else if (context.hoveredInPin != -1) {
                    wiringManager.startWiring(context.hoveredNode, context.hoveredInPin, false);
                    return;
                } else {
                    if (event.getClickCount() == 2 && context.hoveredNode.group != null) {
                        context.selectedNodes.clear();
                        String targetGroup = context.hoveredNode.group;
                        for (VisualNode vn : context.visualNodes) {
                            if (targetGroup.equals(vn.group)) {
                                context.selectedNodes.add(vn);
                            }
                        }
                    } else if (event.isShiftDown()) {
                        if (context.selectedNodes.contains(context.hoveredNode)) {
                            context.selectedNodes.remove(context.hoveredNode);
                        } else {
                            context.selectedNodes.add(context.hoveredNode);
                        }
                    } else {
                        if (!context.selectedNodes.contains(context.hoveredNode)) {
                            context.selectedNodes.clear();
                            context.selectedNodes.add(context.hoveredNode);
                        }
                    }

                    context.setSelectedNode(context.selectedNodes.isEmpty() ? null : context.selectedNodes.get(context.selectedNodes.size() - 1));
                    context.selectedWire = null;
                    context.selectedWireBendIndex = -1;
                    context.wireBendEditMode = false;

                    if (context.hoveredNode.node instanceof InputPin) {
                        InputPin pin = (InputPin) context.hoveredNode.node;
                        if ("Momentary".equals(pin.getMode())) {
                            pin.setState(true);
                        } else {
                            pin.setState(pin.getOut() == 0);
                        }
                        context.setDirty(true);
                    }
                    context.historyManager.saveState();
                    context.draggingNode = context.hoveredNode;
                    context.dragOffsetX = context.worldMouseX;
                    context.dragOffsetY = context.worldMouseY;

                    for (VisualNode vn : context.selectedNodes) {
                        vn.setDragStart(vn.x, vn.y);
                    }
                    return;
                }
            }

            WireBendHit bendHit = getWireBendAt(context.worldMouseX, context.worldMouseY);
            if (bendHit != null) {
                context.historyManager.saveState();
                context.selectedWire = bendHit.wire;
                context.selectedWireBendIndex = bendHit.index;
                context.setSelectedNode(null);
                context.selectedNodes.clear();
                context.draggingWire = bendHit.wire;
                context.draggingWireBendIndex = bendHit.index;
                return;
            }

            VisualWire clickedWire = getWireAt(context.worldMouseX, context.worldMouseY);
            if (clickedWire != null) {
                boolean wasSelected = context.selectedWire == clickedWire;
                context.selectedWire = clickedWire;
                context.selectedWireBendIndex = -1;
                context.setSelectedNode(null);
                context.selectedNodes.clear();
                if (event.getClickCount() == 2) {
                    clickedWire.routeMode = clickedWire.getEffectiveRouteMode(
                        context.projectConfig != null ? context.projectConfig.wireStyle : null
                    );
                    if (wasSelected) {
                        if (clickedWire.routeMode == VisualWire.RouteMode.ORTHOGONAL) {
                            ensureExplicitOrthogonalBends(clickedWire);
                        }
                        context.wireBendEditMode = true;
                    }
                } else {
                    if (!wasSelected) {
                        context.wireBendEditMode = false;
                    }
                    WireSegmentHit segmentHit = getOrthogonalWireSegmentAt(clickedWire, context.worldMouseX, context.worldMouseY);
                    if (segmentHit != null) {
                        context.historyManager.saveState();
                        startDraggingOrthogonalSegment(clickedWire, segmentHit);
                    }
                }
                return;
            }

            context.setSelectedNode(null);
            context.selectedNodes.clear();
            context.selectedWire = null;
            context.selectedWireBendIndex = -1;
            context.wireBendEditMode = false;

            context.isSelecting = true;
            context.selectionStartX = context.worldMouseX;
            context.selectionStartY = context.worldMouseY;
            context.selectionEndX = context.worldMouseX;
            context.selectionEndY = context.worldMouseY;
        } else if (event.getButton() == MouseButton.SECONDARY || event.getButton() == MouseButton.MIDDLE) {

            if (event.getButton() == MouseButton.SECONDARY && context.placingNodeTypeId != null) {
                context.placingNodeTypeId = null;
                context.placingRotation = 0;
                updateHoverState();
                return;
            }

            if (event.getButton() == MouseButton.SECONDARY) {
                if (context.hoveredNode != null) {
                    if (!context.selectedNodes.contains(context.hoveredNode)) {
                        context.selectedNodes.clear();
                        context.selectedNodes.add(context.hoveredNode);
                        context.setSelectedNode(context.hoveredNode);
                        context.selectedWire = null;
                        context.selectedWireBendIndex = -1;
                        context.wireBendEditMode = false;
                    }
                    if (context.onContextMenuRequested != null) {
                        context.contextMenuWorldX = context.worldMouseX;
                        context.contextMenuWorldY = context.worldMouseY;
                        context.onContextMenuRequested.accept(event.getScreenX(), event.getScreenY());
                    }
                    return;
                } else {
                    VisualWire clickedWire = getWireAt(context.worldMouseX, context.worldMouseY);
                    if (clickedWire != null) {
                        boolean wasSelected = context.selectedWire == clickedWire;
                        context.selectedWire = clickedWire;
                        context.selectedWireBendIndex = -1;
                        if (!wasSelected) {
                            context.wireBendEditMode = false;
                        }
                        context.setSelectedNode(null);
                        context.selectedNodes.clear();
                        if (context.onContextMenuRequested != null) {
                            context.contextMenuWorldX = context.worldMouseX;
                            context.contextMenuWorldY = context.worldMouseY;
                            context.onContextMenuRequested.accept(event.getScreenX(), event.getScreenY());
                        }
                        return;
                    }
                }
            }
            context.isPanning = true;
            context.panStartX = context.screenMouseX - context.cameraX;
            context.panStartY = context.screenMouseY - context.cameraY;
        }
    }

    public void handleMouseDragged(MouseEvent event) {
        context.screenMouseX = event.getX();
        context.screenMouseY = event.getY();
        context.updateWorldCoordinates();
        updateHoverState();

        if (context.draggingNode != null) {
            double dx = context.worldMouseX - context.dragOffsetX;
            double dy = context.worldMouseY - context.dragOffsetY;

            context.snapLineX = null;
            context.snapLineY = null;

            if (!event.isShiftDown()) {
                VisualNode primaryNode = context.draggingNode;
                double targetX = primaryNode.getDragStartX() + dx;
                double targetY = primaryNode.getDragStartY() + dy;

                if (context.projectConfig != null && context.projectConfig.snapToGrid) {
                    targetX = snapToGrid(targetX);
                    targetY = snapToGrid(targetY);
                    dx = targetX - primaryNode.getDragStartX();
                    dy = targetY - primaryNode.getDragStartY();
                }

                // Alignment guides take precedence over grid snapping.
                if (context.projectConfig == null || context.projectConfig.showAlignmentGuides) {
                    double snapThreshold = 10.0 / context.zoom;
                    double[] primaryXs = { targetX, targetX + primaryNode.width / 2, targetX + primaryNode.width };
                    double[] primaryYs = { targetY, targetY + primaryNode.height / 2, targetY + primaryNode.height };

                    double minDiffX = snapThreshold;
                    double minDiffY = snapThreshold;
                    boolean snappedX = false;
                    boolean snappedY = false;
                    Double bestSnapLineX = null;
                    Double bestSnapLineY = null;

                    for (VisualNode other : context.visualNodes) {
                        if (context.selectedNodes.contains(other)) continue;

                        double[] otherXs = { other.x, other.x + other.width / 2, other.x + other.width };
                        double[] otherYs = { other.y, other.y + other.height / 2, other.y + other.height };

                        for (double px : primaryXs) {
                            for (double ox : otherXs) {
                                double diff = ox - px;
                                if (Math.abs(diff) < Math.abs(minDiffX)) {
                                    minDiffX = diff;
                                    snappedX = true;
                                    bestSnapLineX = ox;
                                }
                            }
                        }

                        for (double py : primaryYs) {
                            for (double oy : otherYs) {
                                double diff = oy - py;
                                if (Math.abs(diff) < Math.abs(minDiffY)) {
                                    minDiffY = diff;
                                    snappedY = true;
                                    bestSnapLineY = oy;
                                }
                            }
                        }
                    }

                    if (snappedX) {
                        dx += minDiffX;
                        context.snapLineX = bestSnapLineX;
                    }
                    if (snappedY) {
                        dy += minDiffY;
                        context.snapLineY = bestSnapLineY;
                    }
                }
            }

            for (VisualNode vn : context.selectedNodes) {
                vn.x = vn.getDragStartX() + dx;
                vn.y = vn.getDragStartY() + dy;
            }
        } else if (context.draggingWire != null && context.draggingWireBendIndex >= 0) {
            double x = context.worldMouseX;
            double y = context.worldMouseY;
            if (!(event != null && event.isShiftDown()) && context.projectConfig != null && context.projectConfig.snapToGrid) {
                x = snapToGrid(x);
                y = snapToGrid(y);
            }
            context.draggingWire.bendPoints.set(context.draggingWireBendIndex, new Point2D(x, y));
            context.setDirty(true);
        } else if (context.draggingWire != null && context.draggingWireSegmentIndex >= 0) {
            dragOrthogonalWireSegment(event);
        } else if (context.isSelecting) {
            context.selectionEndX = context.worldMouseX;
            context.selectionEndY = context.worldMouseY;
        } else if (context.isPanning) {
            context.cameraX = context.screenMouseX - context.panStartX;
            context.cameraY = context.screenMouseY - context.panStartY;
            context.updateWorldCoordinates();
        }
    }

    public void handleMouseReleased(MouseEvent event) {
        context.screenMouseX = event.getX();
        context.screenMouseY = event.getY();
        context.updateWorldCoordinates();
        updateHoverState();

        context.snapLineX = null;
        context.snapLineY = null;

        if (context.isSelecting) {
            double x1 = Math.min(context.selectionStartX, context.selectionEndX);
            double y1 = Math.min(context.selectionStartY, context.selectionEndY);
            double x2 = Math.max(context.selectionStartX, context.selectionEndX);
            double y2 = Math.max(context.selectionStartY, context.selectionEndY);

            context.selectedNodes.clear();
            for (VisualNode vn : context.visualNodes) {
                if (vn.x >= x1 && vn.y >= y1 && (vn.x + vn.width) <= x2 && (vn.y + vn.height) <= y2) {
                    context.selectedNodes.add(vn);
                }
            }
            if (!context.selectedNodes.isEmpty()) {
                context.setSelectedNode(context.selectedNodes.get(context.selectedNodes.size() - 1));
            } else {
                context.setSelectedNode(null);
            }
            context.isSelecting = false;
        }

        if (context.isWiring && context.wiringNode != null) {
            if (context.hoveredNode != null) {
                if (context.isWiringFromOut && context.hoveredInPin != -1) {
                    if (wiringManager.isValidConnection(context.wiringNode, context.wiringPin, context.hoveredNode, context.hoveredInPin)) {
                        wiringManager.connectWires(context.wiringNode, context.wiringPin, context.hoveredNode, context.hoveredInPin);
                    }
                } else if (!context.isWiringFromOut && context.hoveredOutPin != -1) {
                    if (wiringManager.isValidConnection(context.hoveredNode, context.hoveredOutPin, context.wiringNode, context.wiringPin)) {
                        wiringManager.connectWires(context.hoveredNode, context.hoveredOutPin, context.wiringNode, context.wiringPin);
                    }
                }
            }
        }

        if (context.draggingNode != null) {
            if (context.worldMouseX != context.dragOffsetX || context.worldMouseY != context.dragOffsetY) {
                context.setDirty(true);
            }

            if (context.draggingNode.node instanceof InputPin) {
                InputPin pin = (InputPin) context.draggingNode.node;
                if ("Momentary".equals(pin.getMode())) {
                    pin.setState(false);
                    context.setDirty(true);
                }
            }
        }

        context.isWiring = false;
        context.wiringNode = null;
        context.wiringPin = -1;
        context.draggingNode = null;
        context.draggingWire = null;
        context.draggingWireBendIndex = -1;
        context.draggingWireSegmentIndex = -1;
        context.isPanning = false;
        updateHoverState();
    }

    private void updateHoverState() {
        if (context.isPlacingImport) {
            context.hoveredNode = null;
            context.hoveredInPin = -1;
            context.hoveredOutPin = -1;
            context.hoveredPinName = null;
            return;
        }

        if (context.placingNodeTypeId != null) {
            context.hoveredNode = null;
            context.hoveredInPin = -1;
            context.hoveredOutPin = -1;
            context.hoveredPinName = null;
            return;
        }

        context.hoveredNode = null;
        context.hoveredInPin = -1;
        context.hoveredOutPin = -1;
        context.hoveredPinName = null;

        final double pinDetectionRadius = 8.0;

        for (int i = context.visualNodes.size() - 1; i >= 0; i--) {
            VisualNode vn = context.visualNodes.get(i);
            double minDistance = Double.MAX_VALUE;
            boolean foundPin = false;

            for (int outIdx = 0; outIdx < vn.node.getOutputSize(); outIdx++) {
                double px = vn.getOutPinX(outIdx);
                double py = vn.getOutPinY(outIdx);
                double dist = Math.hypot(px - context.worldMouseX, py - context.worldMouseY);
                if (dist < pinDetectionRadius && dist <= minDistance) {
                    minDistance = dist;
                    context.hoveredNode = vn;
                    context.hoveredOutPin = outIdx;
                    foundPin = true;
                }
            }

            for (int inIdx = 0; inIdx < vn.node.getInputSize(); inIdx++) {
                double px = vn.getInPinX(inIdx);
                double py = vn.getInPinY(inIdx);
                double dist = Math.hypot(px - context.worldMouseX, py - context.worldMouseY);
                if (dist < pinDetectionRadius && dist <= minDistance) {
                    if (dist < minDistance) context.hoveredOutPin = -1;

                    minDistance = dist;
                    context.hoveredNode = vn;
                    context.hoveredInPin = inIdx;
                    foundPin = true;
                }
            }

            if (foundPin) {
                com.logicgate.editor.rendering.symbol.GateSymbol symbol = com.logicgate.editor.rendering.symbol.SymbolRegistry.getSymbol(context.hoveredNode.node.getTypeId());
                if (symbol != null) {
                    if (context.hoveredOutPin != -1) {
                        context.hoveredPinName = symbol.getOutPinName(context.hoveredOutPin);
                    } else if (context.hoveredInPin != -1) {
                        context.hoveredPinName = symbol.getInPinName(context.hoveredInPin);
                    }
                }
                context.tooltipX = context.screenMouseX + 15;
                context.tooltipY = context.screenMouseY + 15;
                return;
            }

            if (vn.contains(context.worldMouseX, context.worldMouseY)) {
                context.hoveredNode = vn;
                return;
            }
        }
    }

    private void finalizePlacement() {
        if (context.pendingProjectData == null) return;

        context.historyManager.saveState();
        context.historyManager.startBatchOperation();

        java.util.Map<String, String> groupRemap = new java.util.HashMap<>();

        int nodeCount = context.pendingProjectData.nodes.size();
        VisualNode[] newNodes = new VisualNode[nodeCount];

        double groupRotation = context.placingRotation;
        double rad = Math.toRadians(groupRotation);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        for (int i = 0; i < nodeCount; i++) {
            NodeData nd = context.pendingProjectData.nodes.get(i);
            Node logicNode = NodeFactory.createNodeByType(nd.type);
            if (logicNode != null) {
                logicNode.setProperties(nd.properties);
                context.getCircuit().addNode(logicNode);

                double rx = nd.x * cos - nd.y * sin;
                double ry = nd.x * sin + nd.y * cos;

                VisualNode vn = new VisualNode(logicNode, context.worldMouseX + rx, context.worldMouseY + ry, nd.label);
                vn.showLabel = nd.showLabel;
                vn.rotation = nd.rotation + groupRotation;

                if (nd.group != null && !nd.group.isEmpty()) {
                    if (!groupRemap.containsKey(nd.group)) {
                        String newGroupName = nd.group;
                        int suffix = 1;
                        while (isGroupExists(newGroupName)) {
                            newGroupName = nd.group + "_" + suffix;
                            suffix++;
                        }
                        groupRemap.put(nd.group, newGroupName);
                    }
                    vn.group = groupRemap.get(nd.group);
                }

                context.visualNodes.add(vn);
                newNodes[i] = vn;
            }
        }

        for (WireData wd : context.pendingProjectData.wires) {
            if (wd.fromIdx >= 0 && wd.fromIdx < nodeCount &&
                wd.toIdx >= 0 && wd.toIdx < nodeCount) {

                VisualNode from = newNodes[wd.fromIdx];
                VisualNode to = newNodes[wd.toIdx];

                if (from != null && to != null) {
                    wiringManager.connectWires(from, wd.outPin, to, wd.inPin);
                    VisualWire addedWire = context.selectedWire;
                    if (addedWire != null) {
                        applyWireRouteData(addedWire, wd, context.worldMouseX, context.worldMouseY, groupRotation);
                    }
                    context.getCircuit().tick();
                }
            }
        }

        context.isPlacingImport = false;
        context.pendingProjectData = null;
        context.setDirty(true);
        context.historyManager.stopBatchOperation();
        updateHoverState();
    }

    private VisualWire getWireAt(double x, double y) {
        double threshold = 3 / context.zoom;
        if (context.selectedWire != null && distanceToWire(x, y, context.selectedWire) < threshold) {
            return context.selectedWire;
        }
        for (VisualWire wire : context.visualWires) {
            if (distanceToWire(x, y, wire) < threshold) return wire;
        }
        return null;
    }

    private WireBendHit getWireBendAt(double x, double y) {
        if (!context.wireBendEditMode) {
            return null;
        }

        double threshold = 9 / context.zoom;
        for (VisualWire wire : context.visualWires) {
            for (int i = 0; i < wire.bendPoints.size(); i++) {
                Point2D point = wire.bendPoints.get(i);
                if (Math.hypot(point.getX() - x, point.getY() - y) <= threshold) {
                    return new WireBendHit(wire, i);
                }
            }
        }
        return null;
    }

    private WireSegmentHit getOrthogonalWireSegmentAt(VisualWire wire, double x, double y) {
        VisualWire.RouteMode routeMode = wire.getEffectiveRouteMode(context.projectConfig != null ? context.projectConfig.wireStyle : null);
        if (routeMode != VisualWire.RouteMode.ORTHOGONAL) {
            return null;
        }

        double threshold = 3 / context.zoom;
        List<Point2D> points = buildWireHitPath(wire);
        double minDistance = Double.MAX_VALUE;
        WireSegmentHit best = null;
        for (int i = 0; i < points.size() - 1; i++) {
            Point2D a = points.get(i);
            Point2D b = points.get(i + 1);
            double distance = distanceToSegment(x, y, a.getX(), a.getY(), b.getX(), b.getY());
            if (distance < threshold && distance < minDistance) {
                minDistance = distance;
                boolean horizontal = Math.abs(a.getY() - b.getY()) <= Math.abs(a.getX() - b.getX());
                best = new WireSegmentHit(i, horizontal);
            }
        }
        return best;
    }

    private void startDraggingOrthogonalSegment(VisualWire wire, WireSegmentHit segmentHit) {
        wire.routeMode = VisualWire.RouteMode.ORTHOGONAL;
        List<Point2D> points = buildWireHitPath(wire);
        int segmentIndex = Math.min(segmentHit.index, points.size() - 2);

        if (wire.bendPoints.isEmpty()) {
            wire.bendPoints.addAll(points.subList(1, points.size() - 1));
        } else {
            normalizeOrthogonalBendPoints(wire, points);
        }

        points = buildWireHitPath(wire);
        segmentIndex = prepareSegmentEndpointsForDrag(wire, points, segmentIndex, segmentHit.horizontal);

        context.draggingWire = wire;
        context.draggingWireSegmentIndex = segmentIndex;
        context.draggingWireSegmentHorizontal = segmentHit.horizontal;
    }

    private int prepareSegmentEndpointsForDrag(VisualWire wire, List<Point2D> points, int segmentIndex, boolean horizontal) {
        int lastIndex = points.size() - 1;

        if (segmentIndex == 0) {
            Point2D start = points.get(0);
            Point2D next = points.get(1);
            Point2D inserted = horizontal
                ? new Point2D(start.getX(), next.getY())
                : new Point2D(next.getX(), start.getY());
            wire.bendPoints.add(0, inserted);
            segmentIndex = 1;
            lastIndex++;
        }

        if (segmentIndex + 1 == lastIndex) {
            Point2D prev = points.get(segmentIndex);
            Point2D end = points.get(points.size() - 1);
            Point2D inserted = horizontal
                ? new Point2D(end.getX(), prev.getY())
                : new Point2D(prev.getX(), end.getY());
            wire.bendPoints.add(segmentIndex, inserted);
        }

        return segmentIndex;
    }

    private void dragOrthogonalWireSegment(MouseEvent event) {
        VisualWire wire = context.draggingWire;
        List<Point2D> points = buildWireHitPath(wire);
        int segmentIndex = context.draggingWireSegmentIndex;
        if (segmentIndex < 0 || segmentIndex >= points.size() - 1) {
            return;
        }

        double target = context.draggingWireSegmentHorizontal ? context.worldMouseY : context.worldMouseX;
        if (!(event != null && event.isShiftDown()) && context.projectConfig != null && context.projectConfig.snapToGrid) {
            target = snapToGrid(target);
        }

        updateBendPointAxis(wire, segmentIndex, context.draggingWireSegmentHorizontal, target);
        updateBendPointAxis(wire, segmentIndex + 1, context.draggingWireSegmentHorizontal, target);
        context.setDirty(true);
    }

    private void updateBendPointAxis(VisualWire wire, int pathPointIndex, boolean horizontal, double target) {
        int bendIndex = pathPointIndex - 1;
        if (bendIndex < 0 || bendIndex >= wire.bendPoints.size()) {
            return;
        }

        Point2D point = wire.bendPoints.get(bendIndex);
        wire.bendPoints.set(bendIndex, horizontal
            ? new Point2D(point.getX(), target)
            : new Point2D(target, point.getY()));
    }

    private double distanceToWire(double x, double y, VisualWire wire) {
        List<Point2D> points = buildWireHitPath(wire);
        double min = Double.MAX_VALUE;
        for (int i = 0; i < points.size() - 1; i++) {
            Point2D a = points.get(i);
            Point2D b = points.get(i + 1);
            min = Math.min(min, distanceToSegment(x, y, a.getX(), a.getY(), b.getX(), b.getY()));
        }
        return min;
    }

    private int getBendInsertIndex(VisualWire wire, double x, double y) {
        if (wire.bendPoints.isEmpty()) {
            return 0;
        }

        List<Point2D> points = buildWireHitPath(wire);
        double minDistance = Double.MAX_VALUE;
        int bestSegmentIndex = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            Point2D a = points.get(i);
            Point2D b = points.get(i + 1);
            double distance = distanceToSegment(x, y, a.getX(), a.getY(), b.getX(), b.getY());
            if (distance < minDistance) {
                minDistance = distance;
                bestSegmentIndex = i;
            }
        }
        return Math.max(0, Math.min(bestSegmentIndex, wire.bendPoints.size()));
    }

    private List<Point2D> buildWireHitPath(VisualWire wire) {
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
                for (Point2D point : wire.bendPoints) {
                    appendOrthogonalPoint(points, point);
                }
            }
            appendOrthogonalPoint(points, end);
            return points;
        }

        int samples = 28;
        for (int i = 0; i <= samples; i++) {
            double t = i / (double) samples;
            points.add(sampleCurvedWire(wire, start, end, t));
        }
        return points;
    }

    private void appendOrthogonalPoint(List<Point2D> points, Point2D target) {
        Point2D last = points.get(points.size() - 1);
        if (Math.abs(last.getX() - target.getX()) > 0.001 && Math.abs(last.getY() - target.getY()) > 0.001) {
            points.add(new Point2D(target.getX(), last.getY()));
        }
        if (points.get(points.size() - 1).distance(target) > 0.001) {
            points.add(target);
        }
    }

    private void normalizeOrthogonalBendPoints(VisualWire wire, List<Point2D> currentPath) {
        wire.bendPoints.clear();
        if (currentPath.size() > 2) {
            wire.bendPoints.addAll(currentPath.subList(1, currentPath.size() - 1));
        }
    }

    private void ensureExplicitOrthogonalBends(VisualWire wire) {
        if (!wire.bendPoints.isEmpty()) {
            normalizeOrthogonalBendPoints(wire, buildWireHitPath(wire));
            return;
        }

        List<Point2D> points = buildWireHitPath(wire);
        if (points.size() > 2) {
            wire.bendPoints.addAll(points.subList(1, points.size() - 1));
        }
    }

    private Point2D sampleCurvedWire(VisualWire wire, Point2D start, Point2D end, double t) {
        if (wire.bendPoints.isEmpty()) {
            Point2D c1 = new Point2D(start.getX() + 50, start.getY());
            Point2D c2 = new Point2D(end.getX() - 50, end.getY());
            return cubic(start, c1, c2, end, t);
        }
        if (wire.bendPoints.size() == 1) {
            return quadratic(start, wire.bendPoints.get(0), end, t);
        }
        return cubic(start, wire.bendPoints.get(0), wire.bendPoints.get(1), end, t);
    }

    private Point2D quadratic(Point2D a, Point2D b, Point2D c, double t) {
        double u = 1 - t;
        return new Point2D(
            u * u * a.getX() + 2 * u * t * b.getX() + t * t * c.getX(),
            u * u * a.getY() + 2 * u * t * b.getY() + t * t * c.getY()
        );
    }

    private Point2D cubic(Point2D a, Point2D b, Point2D c, Point2D d, double t) {
        double u = 1 - t;
        return new Point2D(
            u * u * u * a.getX() + 3 * u * u * t * b.getX() + 3 * u * t * t * c.getX() + t * t * t * d.getX(),
            u * u * u * a.getY() + 3 * u * u * t * b.getY() + 3 * u * t * t * c.getY() + t * t * t * d.getY()
        );
    }

    private void applyWireRouteData(VisualWire wire, WireData data, double offsetX, double offsetY, double rotationDegrees) {
        wire.bendPoints.clear();
        if (data.routeMode != null) {
            try {
                wire.routeMode = VisualWire.RouteMode.valueOf(data.routeMode);
            } catch (IllegalArgumentException ignored) {
                wire.routeMode = null;
            }
        }
        if (data.bendPoints == null) return;

        double rad = Math.toRadians(rotationDegrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        for (WireData.PointData point : data.bendPoints) {
            double rx = point.x * cos - point.y * sin;
            double ry = point.x * sin + point.y * cos;
            wire.bendPoints.add(new Point2D(offsetX + rx, offsetY + ry));
        }
    }

    private record WireBendHit(VisualWire wire, int index) {}
    private record WireSegmentHit(int index, boolean horizontal) {}

    private boolean isGroupExists(String name) {
        if (name == null || name.isEmpty()) return false;
        for (VisualNode vn : context.visualNodes) {
            if (name.equals(vn.group)) return true;
        }
        return false;
    }

    private double distanceToSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double l2 = Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2);
        if (l2 == 0) return Math.hypot(px - x1, py - y1);
        double t = Math.max(0, Math.min(1, ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2));
        double projX = x1 + t * (x2 - x1);
        double projY = y1 + t * (y2 - y1);
        return Math.hypot(px - projX, py - projY);
    }
}
