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
import java.util.List;

public class MouseInteractionHandler {
    private final EditorContext context;
    private final WiringManager wiringManager;

    public MouseInteractionHandler(EditorContext context, WiringManager wiringManager) {
        this.context = context;
        this.wiringManager = wiringManager;
    }

    private void applyPlacementSnapping(MouseEvent event) {
        context.snapLineX = null;
        context.snapLineY = null;
        
        if (event.isShiftDown()) return;

        double targetX = context.worldMouseX;
        double targetY = context.worldMouseY;

        // 그리드 스냅 적용 (UNIT_SIZE 단위로) ✨
        if (context.projectConfig != null && context.projectConfig.snapToGrid) {
            double gs = GateSymbol.UNIT_SIZE;
            targetX = Math.round(targetX / gs) * gs;
            targetY = Math.round(targetY / gs) * gs;
            context.worldMouseX = targetX;
            context.worldMouseY = targetY;
        }
        
        double nodeWidth = 0;
        double nodeHeight = 0;
        
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
            targetX = context.worldMouseX;
            targetY = context.worldMouseY;
            nodeWidth = 0;
            nodeHeight = 0;
        } else {
            return;
        }
        
        double snapThreshold = 8.0 / context.zoom; // 감도 약간 하향 ✨
        double maxSnapDistance = 600.0; // 너무 멀면 스냅 안 함 🔪💕
        
        double[] primaryXs = { targetX, targetX + nodeWidth / 2, targetX + nodeWidth };
        double[] primaryYs = { targetY, targetY + nodeHeight / 2, targetY + nodeHeight };
        
        double minDiffX = snapThreshold;
        double minDiffY = snapThreshold;
        boolean snappedX = false;
        boolean snappedY = false;
        Double bestSnapLineX = null;
        Double bestSnapLineY = null;

        // 정렬 가이드선 적용 ✨
        if (context.projectConfig == null || context.projectConfig.showAlignmentGuides) {
            for (VisualNode other : context.visualNodes) {
                if (context.selectedNodes.contains(other)) continue;
                
                // 거리 체크: 너무 멀리 있는 부품은 무시 ✨
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
            
            // 단일 부품 배치 모드 처리 💖
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
                    vn.rotation = context.placingRotation; // 배치 시 현재 회전각 적용 ✨
                    context.visualNodes.add(vn);
                    context.setDirty(true);
                }
                return; // 배치 후 연속 배치를 위해 리턴 (선택 모드 진입 방지)
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
                    } else if (event.isShortcutDown()) {
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
                    
                    if (context.hoveredNode.node instanceof InputPin) {
                        InputPin pin = (InputPin) context.hoveredNode.node;
                        if ("Momentary".equals(pin.getMode())) {
                            pin.setState(true); // 누르고 있는 동안 켬 ✨
                        } else {
                            pin.setState(pin.getOut() == 0); // 토글 ✨
                        }
                        context.setDirty(true);
                    }
                    context.historyManager.saveState();
                    context.draggingNode = context.hoveredNode;
                    context.dragOffsetX = context.worldMouseX; // 기준점 저장 💖
                    context.dragOffsetY = context.worldMouseY;
                    
                    // 선택된 모든 노드의 시작 위치 저장 ✨
                    for (VisualNode vn : context.selectedNodes) {
                        vn.setDragStart(vn.x, vn.y);
                    }
                    return; 
                }
            }

            // 전선 상호작용 (꺾임점 추가 및 이동) ✨
            VisualWire clickedWire = getWireAt(context.worldMouseX, context.worldMouseY);
            if (clickedWire != null) {
                context.selectedWire = clickedWire;
                context.setSelectedNode(null);
                context.selectedNodes.clear();

                // 기존 Waypoint를 잡았는지 확인 🔪💕
                double threshold = 8.0 / context.zoom;
                VisualWire.Point grabbedPoint = null;
                for (VisualWire.Point p : clickedWire.waypoints) {
                    if (Math.hypot(p.x - context.worldMouseX, p.y - context.worldMouseY) < threshold) {
                        grabbedPoint = p;
                        break;
                    }
                }

                // 잡은 게 없으면 현재 위치에 새로 추가 ✨
                if (grabbedPoint == null) {
                    context.historyManager.saveState();
                    grabbedPoint = new VisualWire.Point(context.worldMouseX, context.worldMouseY);
                    
                    // 시작점과 끝점 사이에 적절히 삽입 🔪💕 (가장 가까운 세그먼트 찾기)
                    int insertIdx = findBestWaypointIndex(clickedWire, context.worldMouseX, context.worldMouseY);
                    clickedWire.waypoints.add(insertIdx, grabbedPoint);
                }

                context.draggingWire = clickedWire;
                context.draggingWaypoint = grabbedPoint;
                return;
            }

            context.setSelectedNode(null);
            context.selectedNodes.clear();
            context.selectedWire = null;
            
            context.isSelecting = true;
            context.selectionStartX = context.worldMouseX;
            context.selectionStartY = context.worldMouseY;
            context.selectionEndX = context.worldMouseX;
            context.selectionEndY = context.worldMouseY;
        } else if (event.getButton() == MouseButton.SECONDARY || event.getButton() == MouseButton.MIDDLE) {
            
            // 우클릭 시 배치 모드 취소 기능 추가 ✨
            if (event.getButton() == MouseButton.SECONDARY && context.placingNodeTypeId != null) {
                context.placingNodeTypeId = null;
                context.placingRotation = 0; // 회전각 초기화 ✨
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
                    }
                    if (context.onContextMenuRequested != null) {
                        context.onContextMenuRequested.accept(event.getScreenX(), event.getScreenY());
                    }
                    return;
                } else {
                    // 노드가 없으면 선이라도 있는지 확인 🔪💕
                    VisualWire clickedWire = getWireAt(context.worldMouseX, context.worldMouseY);
                    if (clickedWire != null) {
                        context.selectedWire = clickedWire;
                        context.setSelectedNode(null);
                        context.selectedNodes.clear();
                        if (context.onContextMenuRequested != null) {
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

    private int findBestWaypointIndex(VisualWire wire, double x, double y) {
        double minDistance = Double.MAX_VALUE;
        int bestIdx = 0;

        double px = wire.from.getOutPinX(wire.outPin);
        double py = wire.from.getOutPinY(wire.outPin);

        for (int i = 0; i <= wire.waypoints.size(); i++) {
            double nextX, nextY;
            if (i < wire.waypoints.size()) {
                nextX = wire.waypoints.get(i).x;
                nextY = wire.waypoints.get(i).y;
            } else {
                nextX = wire.to.getInPinX(wire.inPin);
                nextY = wire.to.getInPinY(wire.inPin);
            }

            double dist = distanceToSegment(x, y, px, py, nextX, nextY);
            if (dist < minDistance) {
                minDistance = dist;
                bestIdx = i;
            }
            px = nextX;
            py = nextY;
        }
        return bestIdx;
    }

    public void handleMouseDragged(MouseEvent event) {
        context.screenMouseX = event.getX();
        context.screenMouseY = event.getY();
        context.updateWorldCoordinates();
        updateHoverState();

        if (context.draggingWaypoint != null) {
            double targetX = context.worldMouseX;
            double targetY = context.worldMouseY;

            // 꺾임점 그리드 스냅 ✨
            if (!event.isShiftDown() && context.projectConfig != null && context.projectConfig.snapToGrid) {
                double gs = GateSymbol.UNIT_SIZE;
                targetX = Math.round(targetX / gs) * gs;
                targetY = Math.round(targetY / gs) * gs;
            }

            context.draggingWaypoint.x = targetX;
            context.draggingWaypoint.y = targetY;
            context.setDirty(true);
        } else if (context.draggingNode != null) {
            double dx = context.worldMouseX - context.dragOffsetX;
            double dy = context.worldMouseY - context.dragOffsetY;
            
            context.snapLineX = null;
            context.snapLineY = null;
            
            if (!event.isShiftDown()) {
                VisualNode primaryNode = context.draggingNode;
                double targetX = primaryNode.getDragStartX() + dx;
                double targetY = primaryNode.getDragStartY() + dy;

                // 1. 그리드 스냅 적용 ✨
                if (context.projectConfig != null && context.projectConfig.snapToGrid) {
                    int gs = context.projectConfig.gridSize;
                    targetX = Math.round(targetX / gs) * gs;
                    targetY = Math.round(targetY / gs) * gs;
                    // dx, dy를 그리드에 맞게 보정
                    dx = targetX - primaryNode.getDragStartX();
                    dy = targetY - primaryNode.getDragStartY();
                }

                // 2. 정렬 가이드 스냅 (가이드선에 걸리면 그리드보다 가이드선이 우선함) ✨
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

            for (VisualNode vn : context.selectedNodes) {
                vn.x = vn.getDragStartX() + dx;
                vn.y = vn.getDragStartY() + dy;
            }
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
                // 노드가 선택 영역에 완전히 포함되는지 확인 (너비 80, 높이 50 기준) 💖
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
            if (context.hoveredNode != null && context.hoveredNode != context.wiringNode) {
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
            
            // Momentary 스위치 해제 처리 💖
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
        context.draggingWaypoint = null;
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
        
        // 배치 모드 중일 때는 호버(선택) 표시가 안 되도록 무시 💖
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

        context.historyManager.startBatchOperation();
        context.historyManager.saveState(); // 대량 작업 시작 전 상태 저장 ✨

        // 그룹 이름 재매핑을 위한 맵 (원본 그룹명 -> 새 그룹명) 🔪💕
        java.util.Map<String, String> groupRemap = new java.util.HashMap<>();

        // 노드 개수만큼 고정 크기 리스트 생성 (인덱스 보존 ✨)
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
                
                // 회전 변환 적용된 좌표 계산 ✨
                double rx = nd.x * cos - nd.y * sin;
                double ry = nd.x * sin + nd.y * cos;

                VisualNode vn = new VisualNode(logicNode, context.worldMouseX + rx, context.worldMouseY + ry, nd.label);
                vn.showLabel = nd.showLabel;
                vn.rotation = nd.rotation + groupRotation; // 개별 회전 + 그룹 회전 ✨
                
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
                    context.getCircuit().tick();
                }
            }
        }

        context.isPlacingImport = false;
        context.pendingProjectData = null;
        context.setDirty(true);
        context.historyManager.stopBatchOperation(); // 작업 완료 ✨
        updateHoverState();
    }

    private VisualWire getWireAt(double x, double y) {
        double threshold = 10 / context.zoom;
        for (VisualWire wire : context.visualWires) {
            double p1x = wire.from.getOutPinX(wire.outPin);
            double p1y = wire.from.getOutPinY(wire.outPin);

            // Waypoints를 포함하여 모든 세그먼트 체크 ✨
            for (int i = 0; i <= wire.waypoints.size(); i++) {
                double p2x, p2y;
                if (i < wire.waypoints.size()) {
                    p2x = wire.waypoints.get(i).x;
                    p2y = wire.waypoints.get(i).y;
                } else {
                    p2x = wire.to.getInPinX(wire.inPin);
                    p2y = wire.to.getInPinY(wire.inPin);
                }

                if (context.projectConfig != null && "Orthogonal".equals(context.projectConfig.wireStyle)) {
                    double midX = (p1x + p2x) / 2;
                    double d1 = distanceToSegment(x, y, p1x, p1y, midX, p1y);
                    double d2 = distanceToSegment(x, y, midX, p1y, midX, p2y);
                    double d3 = distanceToSegment(x, y, midX, p2y, p2x, p2y);
                    if (Math.min(d1, Math.min(d2, d3)) < threshold) return wire;
                } else {
                    if (distanceToSegment(x, y, p1x, p1y, p2x, p2y) < threshold) return wire;
                }
                p1x = p2x;
                p1y = p2y;
            }
        }
        return null;
    }

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
