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

        for (VisualWire wire : context.visualWires) {
            boolean isHigh = (wire.from.node.getOut() & (1 << wire.outPin)) != 0;
            boolean isSelected = (wire == context.selectedWire);
            
            if (isSelected) {
                gc.setStroke(Color.web("#00FFFF"));
                gc.setLineWidth(5);
            } else {
                gc.setStroke(isHigh ? Color.web("#FF3366") : Color.web("#555555"));
                gc.setLineWidth(3);
            }
            
            double x1 = wire.from.getOutPinX(wire.outPin);
            double y1 = wire.from.getOutPinY(wire.outPin);
            double x2 = wire.to.getInPinX(wire.inPin);
            double y2 = wire.to.getInPinY(wire.inPin);
            
            gc.beginPath();
            gc.moveTo(x1, y1);
            gc.bezierCurveTo(x1 + 50, y1, x2 - 50, y2, x2, y2);
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

            gc.setStroke(isHigh ? Color.web("#FF3366") : Color.web("#888888"));
            gc.setLineWidth(3);
            gc.setLineDashes(5); 
            
            gc.beginPath();
            gc.moveTo(startX, startY);
            
            if (context.isWiringFromOut) {
                gc.bezierCurveTo(startX + 50, startY, context.worldMouseX - 50, context.worldMouseY, context.worldMouseX, context.worldMouseY);
            } else {
                gc.bezierCurveTo(startX - 50, startY, context.worldMouseX + 50, context.worldMouseY, context.worldMouseX, context.worldMouseY);
            }
            gc.stroke();
            gc.setLineDashes(null);
        }

        for (VisualNode vn : context.visualNodes) {
            boolean isHovered = (vn == context.hoveredNode);
            boolean isSelected = (vn == context.selectedNode);
            
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
                VisualNode dummyVn = new VisualNode(dummyNode, previewX, previewY, nd.label);
                dummyVn.draw(gc, false, false, -1, -1, null, false);
            }

            gc.setStroke(Color.web("#AAAAAA"));
            gc.setLineWidth(2);
            for (WireData wd : context.pendingProjectData.wires) {
                NodeData fromNd = context.pendingProjectData.nodes.get(wd.fromIdx);
                NodeData toNd = context.pendingProjectData.nodes.get(wd.toIdx);
                
                VisualNode fromVn = new VisualNode(NodeFactory.createNodeByType(fromNd.type), context.worldMouseX + fromNd.x, context.worldMouseY + fromNd.y, "");
                VisualNode toVn = new VisualNode(NodeFactory.createNodeByType(toNd.type), context.worldMouseX + toNd.x, context.worldMouseY + toNd.y, "");
                
                double x1 = fromVn.getOutPinX(wd.outPin);
                double y1 = fromVn.getOutPinY(wd.outPin);
                double x2 = toVn.getInPinX(wd.inPin);
                double y2 = toVn.getInPinY(wd.inPin);
                
                gc.beginPath();
                gc.moveTo(x1, y1);
                gc.bezierCurveTo(x1 + 50, y1, x2 - 50, y2, x2, y2);
                gc.stroke();
            }
            gc.setGlobalAlpha(1.0);
        }

        gc.restore();
    }
}