package com.logicgate.editor.interaction;

import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.model.VisualWire;
import com.logicgate.editor.state.EditorContext;
import com.logicgate.editor.io.NodeData;
import com.logicgate.editor.io.WireData;
import com.logicgate.editor.utils.NodeFactory;
import com.logicgate.gates.InputPin;
import com.logicgate.gates.Joint;
import com.logicgate.gates.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

import java.util.ArrayList;
import java.util.List;

public class MouseInteractionHandler {
    private final EditorContext context;
    private final WiringManager wiringManager;

    public MouseInteractionHandler(EditorContext context, WiringManager wiringManager) {
        this.context = context;
        this.wiringManager = wiringManager;
    }

    public void handleMouseMoved(MouseEvent event) {
        context.screenMouseX = event.getX();
        context.screenMouseY = event.getY();
        context.updateWorldCoordinates();
        updateHoverState();
    }

    public void handleMouseScrolled(ScrollEvent event) {
        double zoomFactor = 1.1;
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
        updateHoverState();

        if (event.getButton() == MouseButton.PRIMARY) {
            if (context.isPlacingImport) {
                finalizePlacement();
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
                    context.setSelectedNode(context.hoveredNode);
                    context.selectedWire = null;
                    
                    if (context.hoveredNode.node instanceof InputPin) {
                        InputPin pin = (InputPin) context.hoveredNode.node;
                        pin.setState(pin.getOut() == 0);
                    }
                    context.draggingNode = context.hoveredNode;
                    context.dragOffsetX = context.worldMouseX - context.hoveredNode.x;
                    context.dragOffsetY = context.worldMouseY - context.hoveredNode.y;
                    return; 
                }
            }

            for (VisualWire wire : context.visualWires) {
                double p1x = wire.from.getOutPinX(wire.outPin);
                double p1y = wire.from.getOutPinY(wire.outPin);
                double p2x = wire.to.getInPinX(wire.inPin);
                double p2y = wire.to.getInPinY(wire.inPin);
                
                if (distanceToSegment(context.worldMouseX, context.worldMouseY, p1x, p1y, p2x, p2y) < 10 / context.zoom) {
                    context.selectedWire = wire;
                    context.setSelectedNode(null);
                    return;
                }
            }

            context.setSelectedNode(null);
            context.selectedWire = null;
            
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
            context.draggingNode.x = context.worldMouseX - context.dragOffsetX;
            context.draggingNode.y = context.worldMouseY - context.dragOffsetY;
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

        if (context.isWiring && context.wiringNode != null) {
            boolean connectionMade = false;
            if (context.hoveredNode != null && context.hoveredNode != context.wiringNode) {
                if (context.isWiringFromOut && context.hoveredInPin != -1) {
                    if (wiringManager.isValidConnection(context.wiringNode, context.wiringPin, context.hoveredNode, context.hoveredInPin)) {
                        wiringManager.connectWires(context.wiringNode, context.wiringPin, context.hoveredNode, context.hoveredInPin);
                        connectionMade = true;
                    }
                } else if (!context.isWiringFromOut && context.hoveredOutPin != -1) {
                    if (wiringManager.isValidConnection(context.hoveredNode, context.hoveredOutPin, context.wiringNode, context.wiringPin)) {
                        wiringManager.connectWires(context.hoveredNode, context.hoveredOutPin, context.wiringNode, context.wiringPin);
                        connectionMade = true;
                    }
                }
            }

            if (!connectionMade && context.hoveredNode == null) {
                double startX = context.isWiringFromOut ? context.wiringNode.getOutPinX(context.wiringPin) : context.wiringNode.getInPinX(context.wiringPin);
                double startY = context.isWiringFromOut ? context.wiringNode.getOutPinY(context.wiringPin) : context.wiringNode.getInPinY(context.wiringPin);
                
                if (Math.hypot(startX - context.worldMouseX, startY - context.worldMouseY) > 20 / context.zoom) {
                    Joint joint = new Joint();
                    VisualNode jointVn = new VisualNode(joint, context.worldMouseX - 15, context.worldMouseY - 15, "JNT");
                    
                    int bestPin = 0;
                    double minDist = Double.MAX_VALUE;
                    for (int i = 0; i < 4; i++) {
                        double d = Math.hypot(jointVn.getInPinX(i) - context.worldMouseX, jointVn.getInPinY(i) - context.worldMouseY);
                        if (d < minDist) {
                            minDist = d;
                            bestPin = i;
                        }
                    }

                    boolean autoConnectValid = false;
                    if (context.isWiringFromOut) {
                        autoConnectValid = wiringManager.isValidConnection(context.wiringNode, context.wiringPin, jointVn, bestPin);
                    } else {
                        autoConnectValid = wiringManager.isValidConnection(jointVn, bestPin, context.wiringNode, context.wiringPin);
                    }

                    if (autoConnectValid) {
                        context.getCircuit().addNode(joint);
                        context.visualNodes.add(jointVn);
                        if (context.isWiringFromOut) {
                            wiringManager.connectWires(context.wiringNode, context.wiringPin, jointVn, bestPin);
                        } else {
                            wiringManager.connectWires(jointVn, bestPin, context.wiringNode, context.wiringPin);
                        }
                    }
                }
            }
        }

        context.isWiring = false;
        context.wiringNode = null;
        context.wiringPin = -1;
        context.draggingNode = null;
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

        List<VisualNode> newNodes = new ArrayList<>();
        for (NodeData nd : context.pendingProjectData.nodes) {
            Node logicNode = NodeFactory.createNodeByType(nd.type);
            if (logicNode != null) {
                context.getCircuit().addNode(logicNode);
                VisualNode vn = new VisualNode(logicNode, context.worldMouseX + nd.x, context.worldMouseY + nd.y, nd.label);
                vn.showLabel = nd.showLabel;
                context.visualNodes.add(vn);
                newNodes.add(vn);
            }
        }

        for (WireData wd : context.pendingProjectData.wires) {
            if (wd.fromIdx >= 0 && wd.fromIdx < newNodes.size() &&
                wd.toIdx >= 0 && wd.toIdx < newNodes.size()) {
                wiringManager.connectWires(newNodes.get(wd.fromIdx), wd.outPin, newNodes.get(wd.toIdx), wd.inPin);
            }
        }

        context.isPlacingImport = false;
        context.pendingProjectData = null;
        context.setDirty(true); // 가져오기 완료 후 변경 감지 ✨
        updateHoverState();
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