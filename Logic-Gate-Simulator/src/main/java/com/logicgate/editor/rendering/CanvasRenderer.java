package com.logicgate.editor.rendering;

import com.logicgate.editor.io.NodeData;
import com.logicgate.editor.io.WireData;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.model.VisualWire;
import com.logicgate.editor.state.EditorContext;
import com.logicgate.editor.interaction.WiringManager;
import com.logicgate.editor.utils.NodeFactory;
import com.logicgate.gates.Node;
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

        gc.setFill(Color.web("#2B2B2B"));
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

        for (VisualWire wire : context.visualWires) {
            boolean isHigh = (wire.from.node.getOut() & (1 << wire.outPin)) != 0;
            boolean isSelected = (wire == context.selectedWire);
            
            // 시각적 설정 적용 ✨
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
            
            double x1 = wire.from.getOutPinX(wire.outPin);
            double y1 = wire.from.getOutPinY(wire.outPin);
            double x2 = wire.to.getInPinX(wire.inPin);
            double y2 = wire.to.getInPinY(wire.inPin);
            
            gc.beginPath();
            gc.moveTo(x1, y1);
            if (context.projectConfig != null && "Orthogonal".equals(context.projectConfig.wireStyle)) {
                double midX = (x1 + x2) / 2;
                gc.lineTo(midX, y1);
                gc.lineTo(midX, y2);
                gc.lineTo(x2, y2);
            } else {
                gc.bezierCurveTo(x1 + 50, y1, x2 - 50, y2, x2, y2);
            }
            gc.stroke();
        }

        if (context.isWiring && context.wiringNode != null) {
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

            boolean showState = context.projectConfig == null || context.projectConfig.showWireState;
            String highColor = context.projectConfig != null ? context.projectConfig.wireHighColor : "#FF3366";
            String lowColor = context.projectConfig != null ? context.projectConfig.wireLowColor : "#888888";

            gc.setStroke((isHigh && showState) ? Color.web(highColor) : Color.web(lowColor));
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
                if (context.isWiringFromOut) {
                    gc.bezierCurveTo(startX + 50, startY, context.worldMouseX - 50, context.worldMouseY, context.worldMouseX, context.worldMouseY);
                } else {
                    gc.bezierCurveTo(startX - 50, startY, context.worldMouseX + 50, context.worldMouseY, context.worldMouseX, context.worldMouseY);
                }
            }
            gc.stroke();
            gc.setLineDashes(null);
        }

        for (VisualNode vn : context.visualNodes) {
            boolean isHovered = (vn == context.hoveredNode);
            boolean isSelected = (vn == context.getSelectedNode() || context.selectedNodes.contains(vn));
            
            boolean isConnectionInvalid = false;
            if (context.isWiring && isHovered) {
                if (context.isWiringFromOut && context.hoveredInPin != -1) {
                    isConnectionInvalid = !wiringManager.isValidConnection(context.wiringNode, context.wiringPin, vn, context.hoveredInPin);
                } else if (!context.isWiringFromOut && context.hoveredOutPin != -1) {
                    isConnectionInvalid = !wiringManager.isValidConnection(vn, context.hoveredOutPin, context.wiringNode, context.wiringPin);
                }
            }
            
            vn.draw(gc, isHovered, isSelected, isHovered ? context.hoveredInPin : -1, isHovered ? context.hoveredOutPin : -1, context.selectedWire, isConnectionInvalid);
        }

        if (context.isPlacingImport && context.pendingProjectData != null) {
            gc.setGlobalAlpha(0.5);
            
            for (NodeData nd : context.pendingProjectData.nodes) {
                double previewX = context.worldMouseX + nd.x;
                double previewY = context.worldMouseY + nd.y;
                Node dummyNode = NodeFactory.createNodeByType(nd.type);
                if (dummyNode != null) {
                    VisualNode dummyVn = new VisualNode(dummyNode, previewX, previewY, nd.label);
                    dummyVn.draw(gc, false, false, -1, -1, null, false);
                }
            }

            gc.setStroke(Color.web("#AAAAAA"));
            gc.setLineWidth(2);
            for (WireData wd : context.pendingProjectData.wires) {
                NodeData fromNd = context.pendingProjectData.nodes.get(wd.fromIdx);
                NodeData toNd = context.pendingProjectData.nodes.get(wd.toIdx);
                
                Node fNode = NodeFactory.createNodeByType(fromNd.type);
                Node tNode = NodeFactory.createNodeByType(toNd.type);
                if(fNode != null && tNode != null) {
                    VisualNode fromVn = new VisualNode(fNode, context.worldMouseX + fromNd.x, context.worldMouseY + fromNd.y, "");
                    VisualNode toVn = new VisualNode(tNode, context.worldMouseX + toNd.x, context.worldMouseY + toNd.y, "");
                    
                    double x1 = fromVn.getOutPinX(wd.outPin);
                    double y1 = fromVn.getOutPinY(wd.outPin);
                    double x2 = toVn.getInPinX(wd.inPin);
                    double y2 = toVn.getInPinY(wd.inPin);
                    
                    gc.beginPath();
                    gc.moveTo(x1, y1);
                    gc.bezierCurveTo(x1 + 50, y1, x2 - 50, y2, x2, y2);
                    gc.stroke();
                }
            }
            gc.setGlobalAlpha(1.0);
        } else if (context.placingNodeTypeId != null) {
            gc.setGlobalAlpha(0.5);
            Node dummyNode = NodeFactory.createNodeByType(context.placingNodeTypeId);
            if (dummyNode != null) {
                VisualNode dummyVn = new VisualNode(dummyNode, 0, 0, "");
                dummyVn.x = context.worldMouseX - (dummyVn.width / 2);
                dummyVn.y = context.worldMouseY - (dummyVn.height / 2);
                dummyVn.draw(gc, false, false, -1, -1, null, false);
            }
            gc.setGlobalAlpha(1.0);
        }

        if (context.isSelecting) {
            double x1 = Math.min(context.selectionStartX, context.selectionEndX);
            double y1 = Math.min(context.selectionStartY, context.selectionEndY);
            double width = Math.abs(context.selectionEndX - context.selectionStartX);
            double height = Math.abs(context.selectionEndY - context.selectionStartY);

            gc.setFill(Color.web("#4A90E2", 0.3));
            gc.fillRect(x1, y1, width, height);
            gc.setStroke(Color.web("#4A90E2", 0.8));
            gc.setLineWidth(1 / context.zoom);
            gc.strokeRect(x1, y1, width, height);
        }

        if (context.snapLineX != null) {
            gc.setStroke(Color.web("#00FFFF", 0.8));
            gc.setLineWidth(1 / context.zoom);
            gc.setLineDashes(5 / context.zoom);
            double worldMinY = -context.cameraY / context.zoom;
            double worldMaxY = (canvas.getHeight() - context.cameraY) / context.zoom;
            gc.strokeLine(context.snapLineX, worldMinY, context.snapLineX, worldMaxY);
            gc.setLineDashes(null);
        }

        if (context.snapLineY != null) {
            gc.setStroke(Color.web("#00FFFF", 0.8));
            gc.setLineWidth(1 / context.zoom);
            gc.setLineDashes(5 / context.zoom);
            double worldMinX = -context.cameraX / context.zoom;
            double worldMaxX = (canvas.getWidth() - context.cameraX) / context.zoom;
            gc.strokeLine(worldMinX, context.snapLineY, worldMaxX, context.snapLineY);
            gc.setLineDashes(null);
        }

        gc.restore();

        // 툴팁 그리기 (화면 좌표계 사용)
        if (context.hoveredPinName != null) {
            gc.save();
            gc.setFont(javafx.scene.text.Font.font("Arial", 12));
            javafx.scene.text.Text textNode = new javafx.scene.text.Text(context.hoveredPinName);
            textNode.setFont(gc.getFont());
            double textWidth = textNode.getLayoutBounds().getWidth();
            double textHeight = textNode.getLayoutBounds().getHeight();
            
            double padding = 6;
            double boxWidth = textWidth + padding * 2;
            double boxHeight = textHeight + padding * 2;
            
            double tx = context.tooltipX;
            double ty = context.tooltipY;
            
            // 캔버스 밖으로 나가지 않게 보정
            if (tx + boxWidth > canvas.getWidth()) {
                tx = canvas.getWidth() - boxWidth - 5;
            }
            if (ty + boxHeight > canvas.getHeight()) {
                ty = canvas.getHeight() - boxHeight - 5;
            }

            // 반투명 검은색 배경
            gc.setFill(Color.rgb(0, 0, 0, 0.8));
            gc.fillRoundRect(tx, ty, boxWidth, boxHeight, 8, 8);
            
            // 하얀색 텍스트
            gc.setFill(Color.WHITE);
            gc.fillText(context.hoveredPinName, tx + padding, ty + padding + textHeight * 0.8);
            gc.restore();
        }
    }
}