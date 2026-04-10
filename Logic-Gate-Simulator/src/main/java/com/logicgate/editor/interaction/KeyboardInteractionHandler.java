package com.logicgate.editor.interaction;

import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.state.EditorContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class KeyboardInteractionHandler {
    private final EditorContext context;

    public KeyboardInteractionHandler(EditorContext context) {
        this.context = context;
    }

    public void handleKeyPressed(KeyEvent event) {
        context.activeKeys.add(event.getCode());

        switch (event.getCode()) {
            case ESCAPE:
                context.isWiring = false;
                context.wiringNode = null;
                context.wiringPin = -1;
                context.draggingNode = null;
                context.isPanning = false;
                context.setSelectedNode(null);
                context.selectedWire = null;
                context.isPlacingImport = false;
                context.pendingProjectData = null;
                context.placingNodeTypeId = null; // 배치 모드 취소 ✨
                break;
            case DELETE:
            case BACK_SPACE:
                if (!context.selectedNodes.isEmpty()) {
                    context.historyManager.saveState();
                    for (VisualNode vn : new java.util.ArrayList<>(context.selectedNodes)) {
                        removeNode(vn);
                    }
                    context.selectedNodes.clear();
                    context.setSelectedNode(null);
                } else if (context.getSelectedNode() != null) {
                    context.historyManager.saveState();
                    removeNode(context.getSelectedNode());
                    context.setSelectedNode(null);
                } else if (context.selectedWire != null) {
                    context.historyManager.saveState();
                    context.getCircuit().disconnectSpecific(context.selectedWire.from.node, context.selectedWire.outPin, context.selectedWire.to.node, context.selectedWire.inPin);
                    context.visualWires.remove(context.selectedWire);
                    context.selectedWire = null;
                    context.setDirty(true); // 변경 감지 ✨
                }
                break;
            case Z:
                if (event.isShortcutDown()) {
                    if (event.isShiftDown()) {
                        context.historyManager.redo();
                    } else {
                        context.historyManager.undo();
                    }
                }
                break;
            case A:
                if (event.isShortcutDown()) {
                    context.selectedNodes.clear();
                    context.selectedNodes.addAll(context.visualNodes);
                    if (!context.selectedNodes.isEmpty()) {
                        context.setSelectedNode(context.selectedNodes.get(context.selectedNodes.size() - 1));
                    }
                    context.selectedWire = null;
                }
                break;
            case C:
                if (event.isShortcutDown() && context.onCopyRequested != null) {
                    context.onCopyRequested.run();
                }
                break;
            case V:
                if (event.isShortcutDown()) {
                    // 붙여넣기 시 기존 선택 해제 ✨
                    context.selectedNodes.clear();
                    context.setSelectedNode(null);
                    context.selectedWire = null;
                    
                    if (context.onPasteRequested != null) {
                        context.onPasteRequested.run();
                    }
                }
                break;
            case Q:
            case E:
                double angle = (event.getCode() == KeyCode.Q) ? -10 : 10;
                if (event.isShiftDown()) angle *= 9; // Shift 누르면 90도씩 회전 ✨

                if (context.placingNodeTypeId != null || context.isPlacingImport) {
                    // 배치 모드(단일/그룹)일 때는 배치 예정 각도 조절 ✨
                    context.placingRotation = (context.placingRotation + angle) % 360;
                } else if (!context.selectedNodes.isEmpty()) {
                    // 선택된 노드들이 있을 때는 그룹 회전 수행 🔪💕
                    rotateSelection(angle);
                }
                break;
            default: break;
        }
    }

    private void rotateSelection(double angle) {
        context.historyManager.saveState();

        // 1. 선택된 모든 노드의 전체 영역(Bounding Box)의 중앙을 찾음 ✨
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

        for (VisualNode vn : context.selectedNodes) {
            minX = Math.min(minX, vn.x);
            minY = Math.min(minY, vn.y);
            maxX = Math.max(maxX, vn.x + vn.width);
            maxY = Math.max(maxY, vn.y + vn.height);
        }

        double groupCx = (minX + maxX) / 2;
        double groupCy = (minY + maxY) / 2;

        double rad = Math.toRadians(angle);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        // 2. 각 노드를 그룹 중앙을 기준으로 위치 이동 및 개별 회전 ✨
        for (VisualNode vn : context.selectedNodes) {
            double nodeCx = vn.x + vn.width / 2;
            double nodeCy = vn.y + vn.height / 2;

            double dx = nodeCx - groupCx;
            double dy = nodeCy - groupCy;

            // 좌표 회전 공식 적용 🔪💕
            double newNodeCx = groupCx + (dx * cos - dy * sin);
            double newNodeCy = groupCy + (dx * sin + dy * cos);

            vn.x = newNodeCx - vn.width / 2;
            vn.y = newNodeCy - vn.height / 2;
            vn.rotation = (vn.rotation + angle) % 360;
        }
        context.setDirty(true);
    }

    public void handleKeyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.S && event.isShortcutDown() && context.onSaveRequested != null) {
            context.onSaveRequested.run(); // 키를 뗄 때 딱 한 번 저장! 🔪💕
        }
        context.activeKeys.remove(event.getCode());
    }

    public void updateCamera() {
        if (context.activeKeys.isEmpty()) return;

        double moveX = 0;
        double moveY = 0;

        if (context.activeKeys.contains(KeyCode.UP)    ) moveY += 1;
        if (context.activeKeys.contains(KeyCode.DOWN)  ) moveY -= 1;
        if (context.activeKeys.contains(KeyCode.LEFT)  ) moveX += 1;
        if (context.activeKeys.contains(KeyCode.RIGHT) ) moveX -= 1;

        if (moveX != 0 || moveY != 0) {
            double length = Math.hypot(moveX, moveY);
            double speed = 10.0;
            
            context.cameraX += (moveX / length) * speed;
            context.cameraY += (moveY / length) * speed;
            
            context.updateWorldCoordinates();
        }
    }

    private void removeNode(VisualNode vn) {
        context.getCircuit().removeNode(vn.node);
        context.visualNodes.remove(vn);
        context.visualWires.removeIf(w -> {
            boolean related = w.from == vn || w.to == vn;
            if (related && w == context.selectedWire) context.selectedWire = null;
            return related;
        });
        context.setDirty(true); // 변경 감지 ✨
    }
}