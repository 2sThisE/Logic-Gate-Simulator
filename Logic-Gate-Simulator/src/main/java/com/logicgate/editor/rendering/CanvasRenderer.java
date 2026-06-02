package com.logicgate.editor.rendering;

import com.logicgate.editor.interaction.WiringManager;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.model.VisualWire;
import com.logicgate.editor.rendering.symbol.GateSymbol;
import com.logicgate.editor.rendering.symbol.SymbolRegistry;
import com.logicgate.editor.state.EditorContext;
import com.logicgate.editor.utils.NodeFactory;
import com.logicgate.gates.Node;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;

public class CanvasRenderer {
    private static final double BEND_HANDLE_SIZE_PX = 10.0;
    private static final double BEND_HANDLE_STROKE_PX = 2.0;
    private static final double SELECTED_BEND_RING_PADDING_PX = 3.0;

    private final Canvas canvas;
    private final EditorContext context;
    private final WiringManager wiringManager;

    public CanvasRenderer(Canvas canvas, EditorContext context, WiringManager wiringManager) {
        this.canvas = canvas;
        this.context = context;
        this.wiringManager = wiringManager;
    }

    public void draw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setFill(Color.web("#1E1E1E"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.save();
        gc.translate(context.cameraX, context.cameraY);
        gc.scale(context.zoom, context.zoom);

        if (context.projectConfig != null && context.projectConfig.showGrid) {
            gc.setLineWidth(1 / context.zoom);
            double unitSize = GateSymbol.UNIT_SIZE;
            double majorGridSize = Math.max(unitSize, context.projectConfig.gridSize);
            double startX = (Math.floor(-context.cameraX / context.zoom / unitSize) * unitSize);
            double endX = startX + canvas.getWidth() / context.zoom + unitSize;
            double startY = (Math.floor(-context.cameraY / context.zoom / unitSize) * unitSize);
            double endY = startY + canvas.getHeight() / context.zoom + unitSize;

            gc.setStroke(Color.web("#2A2A2A"));
            for (double x = startX; x <= endX; x += unitSize) {
                gc.strokeLine(x, startY, x, endY);
            }
            for (double y = startY; y <= endY; y += unitSize) {
                gc.strokeLine(startX, y, endX, y);
            }

            if (majorGridSize > unitSize) {
                double majorStartX = (Math.floor(-context.cameraX / context.zoom / majorGridSize) * majorGridSize);
                double majorStartY = (Math.floor(-context.cameraY / context.zoom / majorGridSize) * majorGridSize);

                gc.setStroke(Color.web("#3A3A3A"));
                for (double x = majorStartX; x <= endX; x += majorGridSize) {
                    gc.strokeLine(x, startY, x, endY);
                }
                for (double y = majorStartY; y <= endY; y += majorGridSize) {
                    gc.strokeLine(startX, y, endX, y);
                }
            }
        }

        for (VisualWire wire : context.visualWires) {
            drawWire(gc, wire);
        }
        for (VisualWire wire : context.visualWires) {
            if (isConnectedToHoveredPin(wire) && !context.selectedWires.contains(wire)) {
                drawWireHighlight(gc, wire);
            }
        }

        if (context.placingNodeTypeId != null) {
            drawPlacementGhost(gc);
        }

        if (context.isPlacingImport && context.pendingProjectData != null) {
            drawImportGhost(gc);
        }

        for (VisualNode vn : context.visualNodes) {
            boolean isHovered = (vn == context.hoveredNode);
            boolean isSelected = context.selectedNodes.contains(vn);
            int hi = (isHovered) ? context.hoveredInPin : -1;
            int ho = (isHovered) ? context.hoveredOutPin : -1;

            boolean isInvalid = false;
            if (context.isWiring && isHovered) {
                if (context.isWiringFromOut) {
                    isInvalid = !wiringManager.isValidConnection(context.wiringNode, context.wiringPin, vn, hi);
                } else {
                    isInvalid = !wiringManager.isValidConnection(vn, ho, context.wiringNode, context.wiringPin);
                }
            }

            vn.draw(gc, isHovered, isSelected, hi, ho, context.selectedWire, isInvalid);
        }

        if (context.isWiring && context.wiringNode != null) {
            drawActiveWiring(gc);
        }

        if (context.isSelecting) {
            gc.setStroke(Color.web("#00FFFF", 0.5));
            gc.setLineWidth(1 / context.zoom);
            gc.setFill(Color.web("#00FFFF", 0.1));
            double x1 = Math.min(context.selectionStartX, context.selectionEndX);
            double y1 = Math.min(context.selectionStartY, context.selectionEndY);
            double w = Math.abs(context.selectionStartX - context.selectionEndX);
            double h = Math.abs(context.selectionStartY - context.selectionEndY);
            gc.fillRect(x1, y1, w, h);
            gc.strokeRect(x1, y1, w, h);
        }

        drawSnapLines(gc);

        gc.restore();

        // Tooltips are drawn after restoring the camera transform.
        String tooltipText = getPinTooltipText();
        if (tooltipText != null) {
            gc.setFill(Color.web("#333333", 0.9));
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1);
            double x = context.screenMouseX + 15;
            double y = context.screenMouseY + 15;
            double tw = tooltipText.length() * 7 + 10;
            gc.fillRoundRect(x, y - 25, tw, 20, 5, 5);
            gc.strokeRoundRect(x, y - 25, tw, 20, 5, 5);
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", 11));
            gc.fillText(tooltipText, x + 5, y - 11);
        }
    }

    private String getPinTooltipText() {
        if (!context.isWiring) {
            return context.hoveredPinName;
        }
        if (context.wiringPinName == null) {
            return null;
        }
        boolean hoveringTargetPin = context.isWiringFromOut
            ? context.hoveredInPin != -1
            : context.hoveredOutPin != -1;
        return hoveringTargetPin ? context.hoveredPinName : context.wiringPinName;
    }

    private void drawWire(GraphicsContext gc, VisualWire wire) {
        boolean isHigh = (wire.from.node.getOut() & (1 << wire.outPin)) != 0;
        boolean isSelected = context.selectedWires.contains(wire) || wire == context.selectedWire;

        boolean showState = context.projectConfig == null || context.projectConfig.showWireState;
        String highColor = context.projectConfig != null ? context.projectConfig.wireHighColor : "#FF3366";
        String lowColor = context.projectConfig != null ? context.projectConfig.wireLowColor : "#555555";

        if (isSelected) {
            gc.setStroke(Color.web("#00FFFF"));
            gc.setLineWidth(5);
        } else {
            gc.setStroke((isHigh && showState) ? Color.web(highColor) : Color.web(lowColor));
            gc.setLineWidth(3);
        }

        double lastX = wire.from.getOutPinX(wire.outPin);
        double lastY = wire.from.getOutPinY(wire.outPin);

        gc.beginPath();
        gc.moveTo(lastX, lastY);

        double endX = wire.to.getInPinX(wire.inPin);
        double endY = wire.to.getInPinY(wire.inPin);

        traceWirePath(gc, wire, lastX, lastY, endX, endY);
        gc.stroke();

        if (wire == context.selectedWire && context.wireBendEditMode && !wire.locked) {
            drawBendPointHandles(gc, wire);
        }
    }

    private boolean isConnectedToHoveredPin(VisualWire wire) {
        if (context.hoveredNode == null) {
            return false;
        }
        if (context.hoveredOutPin != -1) {
            return wire.from == context.hoveredNode && wire.outPin == context.hoveredOutPin;
        }
        if (context.hoveredInPin != -1) {
            return wire.to == context.hoveredNode && wire.inPin == context.hoveredInPin;
        }
        return false;
    }

    private void drawWireHighlight(GraphicsContext gc, VisualWire wire) {
        double startX = wire.from.getOutPinX(wire.outPin);
        double startY = wire.from.getOutPinY(wire.outPin);
        double endX = wire.to.getInPinX(wire.inPin);
        double endY = wire.to.getInPinY(wire.inPin);

        gc.save();
        gc.setStroke(Color.web("#FFD700", 0.9));
        gc.setLineWidth(7 / context.zoom);
        gc.beginPath();
        gc.moveTo(startX, startY);
        traceWirePath(gc, wire, startX, startY, endX, endY);
        gc.stroke();

        gc.setStroke(Color.web("#FFFFFF", 0.85));
        gc.setLineWidth(2 / context.zoom);
        gc.beginPath();
        gc.moveTo(startX, startY);
        traceWirePath(gc, wire, startX, startY, endX, endY);
        gc.stroke();
        gc.restore();
    }

    private void traceWirePath(GraphicsContext gc, VisualWire wire, double startX, double startY, double endX, double endY) {
        VisualWire.RouteMode routeMode = wire.getEffectiveRouteMode(context.projectConfig != null ? context.projectConfig.wireStyle : null);
        if (routeMode == VisualWire.RouteMode.ORTHOGONAL) {
            for (Point2D point : buildOrthogonalPath(wire, startX, startY, endX, endY)) {
                gc.lineTo(point.getX(), point.getY());
            }
        } else {
            drawCurvedWirePath(gc, startX, startY, endX, endY, wire);
        }
    }

    private java.util.List<Point2D> buildOrthogonalPath(VisualWire wire, double startX, double startY, double endX, double endY) {
        java.util.List<Point2D> path = new java.util.ArrayList<>();
        path.add(new Point2D(startX, startY));
        if (wire.bendPoints.isEmpty()) {
            double midX = (startX + endX) / 2;
            path.add(new Point2D(midX, startY));
            path.add(new Point2D(midX, endY));
            path.add(new Point2D(endX, endY));
            return path.subList(1, path.size());
        }

        for (Point2D point : wire.bendPoints) {
            appendOrthogonalPoint(path, point);
        }
        appendOrthogonalPoint(path, new Point2D(endX, endY));
        return path.subList(1, path.size());
    }

    private void appendOrthogonalPoint(java.util.List<Point2D> path, Point2D target) {
        Point2D last = path.get(path.size() - 1);
        if (Math.abs(last.getX() - target.getX()) > 0.001 && Math.abs(last.getY() - target.getY()) > 0.001) {
            path.add(new Point2D(target.getX(), last.getY()));
        }
        if (path.isEmpty() || path.get(path.size() - 1).distance(target) > 0.001) {
            path.add(target);
        }
    }

    private void drawCurvedWirePath(GraphicsContext gc, double startX, double startY, double endX, double endY, VisualWire wire) {
        if (wire.bendPoints.isEmpty()) {
            gc.bezierCurveTo(startX + 50, startY, endX - 50, endY, endX, endY);
        } else if (wire.bendPoints.size() == 1) {
            Point2D control = wire.bendPoints.get(0);
            gc.quadraticCurveTo(control.getX(), control.getY(), endX, endY);
        } else {
            Point2D c1 = wire.bendPoints.get(0);
            Point2D c2 = wire.bendPoints.get(1);
            gc.bezierCurveTo(c1.getX(), c1.getY(), c2.getX(), c2.getY(), endX, endY);
            for (int i = 2; i < wire.bendPoints.size(); i++) {
                Point2D point = wire.bendPoints.get(i);
                gc.lineTo(point.getX(), point.getY());
            }
        }
    }

    private void drawBendPointHandles(GraphicsContext gc, VisualWire wire) {
        gc.save();
        gc.setFill(Color.web("#1E1E1E"));
        gc.setStroke(Color.web("#00FFFF"));
        gc.setLineWidth(screenToWorld(BEND_HANDLE_STROKE_PX));
        double size = screenToWorld(BEND_HANDLE_SIZE_PX);
        double selectedPadding = screenToWorld(SELECTED_BEND_RING_PADDING_PX);
        for (int i = 0; i < wire.bendPoints.size(); i++) {
            Point2D point = wire.bendPoints.get(i);
            double x = point.getX() - size / 2;
            double y = point.getY() - size / 2;
            gc.fillOval(x, y, size, size);
            gc.strokeOval(x, y, size, size);
            if (i == context.selectedWireBendIndex) {
                gc.setStroke(Color.web("#FFD700"));
                gc.strokeOval(x - selectedPadding, y - selectedPadding, size + selectedPadding * 2, size + selectedPadding * 2);
                gc.setStroke(Color.web("#00FFFF"));
            }
        }
        gc.restore();
    }

    private void drawActiveWiring(GraphicsContext gc) {
        boolean isHigh = false;
        double startX, startY;

        if (context.isWiringFromOut) {
            isHigh = (context.wiringNode.node.getOut() & (1 << context.wiringPin)) != 0;
            startX = context.wiringNode.getOutPinX(context.wiringPin);
            startY = context.wiringNode.getOutPinY(context.wiringPin);
        } else {
            startX = context.wiringNode.getInPinX(context.wiringPin);
            startY = context.wiringNode.getInPinY(context.wiringPin);
        }

        gc.setStroke(Color.web("#00FFFF", 0.7));
        gc.setLineWidth(3);
        gc.setLineDashes(5);
        gc.beginPath();
        gc.moveTo(startX, startY);

        if (context.projectConfig != null && "Orthogonal".equals(context.projectConfig.wireStyle)) {
            double midX = (startX + context.worldMouseX) / 2;
            gc.lineTo(midX, startY);
            gc.lineTo(midX, context.worldMouseY);
            gc.lineTo(context.worldMouseX, context.worldMouseY);
        } else {
            gc.bezierCurveTo(startX + 50, startY, context.worldMouseX - 50, context.worldMouseY, context.worldMouseX, context.worldMouseY);
        }
        gc.stroke();
        gc.setLineDashes(null);
    }

    private void drawSnapLines(GraphicsContext gc) {
        double worldMinX = -context.cameraX / context.zoom;
        double worldMaxX = (canvas.getWidth() - context.cameraX) / context.zoom;
        double worldMinY = -context.cameraY / context.zoom;
        double worldMaxY = (canvas.getHeight() - context.cameraY) / context.zoom;

        gc.setStroke(Color.web("#FFD700", 0.5));
        gc.setLineWidth(1 / context.zoom);

        if (context.snapLineX != null) {
            gc.strokeLine(context.snapLineX, worldMinY, context.snapLineX, worldMaxY);
        }
        if (context.snapLineY != null) {
            gc.strokeLine(worldMinX, context.snapLineY, worldMaxX, context.snapLineY);
        }
    }

    private void drawPlacementGhost(GraphicsContext gc) {
        Node node = NodeFactory.createNodeByType(context.placingNodeTypeId);
        if (node == null) return;

        GateSymbol symbol = SymbolRegistry.getSymbol(context.placingNodeTypeId);
        if (symbol == null) {
            symbol = SymbolRegistry.getSymbol(node.getTypeId());
        }

        if (symbol != null) {
            gc.save();
            gc.setGlobalAlpha(0.4);

            double width = symbol.getUnitWidth() * GateSymbol.UNIT_SIZE;
            double height = symbol.getUnitHeight() * GateSymbol.UNIT_SIZE;

            gc.translate(context.worldMouseX, context.worldMouseY);
            gc.rotate(context.placingRotation);
            gc.translate(-width / 2, -height / 2);

            VisualNode dummy = new VisualNode(node, 0, 0, "");
            symbol.draw(gc, dummy, false, false);
            gc.restore();
        }
    }

    private void drawImportGhost(GraphicsContext gc) {
        gc.save();
        gc.setGlobalAlpha(0.4);

        double rad = Math.toRadians(context.placingRotation);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        for (com.logicgate.editor.io.NodeData nd : context.pendingProjectData.nodes) {
            double rx = nd.x * cos - nd.y * sin;
            double ry = nd.x * sin + nd.y * cos;

            GateSymbol s = SymbolRegistry.getSymbol(nd.type);
            if (s != null) {
                double w = s.getUnitWidth() * GateSymbol.UNIT_SIZE;
                double h = s.getUnitHeight() * GateSymbol.UNIT_SIZE;

                gc.save();
                gc.translate(context.worldMouseX + rx, context.worldMouseY + ry);
                gc.rotate(nd.rotation + context.placingRotation);
                gc.translate(-w / 2, -h / 2);

                VisualNode d = new VisualNode(NodeFactory.createNodeByType(nd.type), 0, 0, "");
                s.draw(gc, d, false, false);
                gc.restore();
            }
        }
        gc.restore();
    }

    private double screenToWorld(double pixels) {
        return pixels / Math.max(context.zoom, 0.0001);
    }
}
