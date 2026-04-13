package com.logicgate.editor.rendering;

import com.logicgate.editor.interaction.WiringManager;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.model.VisualWire;
import com.logicgate.editor.rendering.symbol.GateSymbol;
import com.logicgate.editor.rendering.symbol.SymbolRegistry;
import com.logicgate.editor.state.EditorContext;
import com.logicgate.editor.utils.NodeFactory;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class CanvasRenderer {
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

        // 배경 ✨
        gc.setFill(Color.web("#1E1E1E"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.save();
        gc.translate(context.cameraX, context.cameraY);
        gc.scale(context.zoom, context.zoom);

        // 그리드 렌더링 ✨
        if (context.projectConfig != null && context.projectConfig.showGrid) {
            gc.setStroke(Color.web("#3A3A3A"));
            gc.setLineWidth(1 / context.zoom);
            int gridSize = context.projectConfig.gridSize;
            double startX = (Math.floor(-context.cameraX / context.zoom / gridSize) * gridSize);
            double endX = startX + canvas.getWidth() / context.zoom + gridSize;
            double startY = (Math.floor(-context.cameraY / context.zoom / gridSize) * gridSize);
            double endY = startY + canvas.getHeight() / context.zoom + gridSize;
            
            for (double x = startX; x <= endX; x += gridSize) {
                gc.strokeLine(x, startY, x, endY);
            }
            for (double y = startY; y <= endY; y += gridSize) {
                gc.strokeLine(startX, y, endX, y);
            }
        }

        // 전선 그리기 💖
        for (VisualWire wire : context.visualWires) {
            drawWire(gc, wire);
        }

        // 배치 중인 노드 잔상 (Ghost) ✨
        if (context.placingNodeTypeId != null) {
            drawPlacementGhost(gc);
        }

        // 붙여넣기/가져오기 중인 데이터 잔상 ✨
        if (context.isPlacingImport && context.pendingProjectData != null) {
            drawImportGhost(gc);
        }

        // 노드 그리기 ✨
        for (VisualNode vn : context.visualNodes) {
            boolean isHovered = (vn == context.hoveredNode);
            boolean isSelected = context.selectedNodes.contains(vn);
            int hi = (isHovered) ? context.hoveredInPin : -1;
            int ho = (isHovered) ? context.hoveredOutPin : -1;
            
            // 배선 가능 여부 시각화 ✨
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

        // 현재 배선 중인 선 ✨
        if (context.isWiring && context.wiringNode != null) {
            drawActiveWiring(gc);
        }

        // 선택 영역 사각형 ✨
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

        // 스냅 가이드선 ✨
        drawSnapLines(gc);

        gc.restore();

        // 툴팁 렌더링 (줌 영향을 받지 않도록 restore 이후에 수행) ✨
        if (context.hoveredPinName != null) {
            gc.setFill(Color.web("#333333", 0.9));
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1);
            double tw = context.hoveredPinName.length() * 7 + 10;
            gc.fillRoundRect(context.tooltipX, context.tooltipY - 25, tw, 20, 5, 5);
            gc.strokeRoundRect(context.tooltipX, context.tooltipY - 25, tw, 20, 5, 5);
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", 11));
            gc.fillText(context.hoveredPinName, context.tooltipX + 5, context.tooltipY - 11);
        }
    }

    private void drawWire(GraphicsContext gc, VisualWire wire) {
        boolean isHigh = (wire.from.node.getOut() & (1 << wire.outPin)) != 0;
        boolean isSelected = (wire == context.selectedWire);
        
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

        boolean isOrthogonal = context.projectConfig != null && "Orthogonal".equals(context.projectConfig.wireStyle);

        // 경유지(Waypoints)를 거쳐서 그리기 🔪💕
        for (VisualWire.Point wp : wire.waypoints) {
            if (isOrthogonal) {
                double midX = (lastX + wp.x) / 2;
                gc.lineTo(midX, lastY);
                gc.lineTo(midX, wp.y);
                gc.lineTo(wp.x, wp.y);
            } else {
                // 곡선 모드에서도 구간별로는 직선으로 잇거나 베지어로 연결
                gc.lineTo(wp.x, wp.y);
            }
            lastX = wp.x;
            lastY = wp.y;
        }

        // 마지막 지점(목적지 핀) 연결 ✨
        double endX = wire.to.getInPinX(wire.inPin);
        double endY = wire.to.getInPinY(wire.inPin);

        if (isOrthogonal) {
            double midX = (lastX + endX) / 2;
            gc.lineTo(midX, lastY);
            gc.lineTo(midX, endY);
            gc.lineTo(endX, endY);
        } else {
            if (wire.waypoints.isEmpty()) {
                // 경유지가 없을 때만 전통적인 베지어 곡선 사용
                gc.bezierCurveTo(lastX + 50, lastY, endX - 50, endY, endX, endY);
            } else {
                gc.lineTo(endX, endY);
            }
        }
        gc.stroke();

        // 선택되었을 때 경유지 포인트 시각화 🔪💕
        if (isSelected) {
            gc.setFill(Color.web("#00FFFF"));
            for (VisualWire.Point wp : wire.waypoints) {
                gc.fillOval(wp.x - 4, wp.y - 4, 8, 8);
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(1);
                gc.strokeOval(wp.x - 4, wp.y - 4, 8, 8);
            }
        }
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
        GateSymbol symbol = SymbolRegistry.getSymbol(context.placingNodeTypeId);
        if (symbol != null) {
            gc.save();
            gc.setGlobalAlpha(0.4);
            
            double width = symbol.getUnitWidth() * GateSymbol.UNIT_SIZE;
            double height = symbol.getUnitHeight() * GateSymbol.UNIT_SIZE;
            
            gc.translate(context.worldMouseX, context.worldMouseY);
            gc.rotate(context.placingRotation);
            gc.translate(-width / 2, -height / 2);
            
            // 더미 VisualNode 생성하여 그리기 ✨
            VisualNode dummy = new VisualNode(NodeFactory.createNodeByType(context.placingNodeTypeId), 0, 0, "");
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
}
