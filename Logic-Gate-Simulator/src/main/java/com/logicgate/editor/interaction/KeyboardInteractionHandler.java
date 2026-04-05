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
                    context.getCircuit().disconnect(context.selectedWire.from.node, context.selectedWire.outPin);
                    context.visualWires.remove(context.selectedWire);
                    context.selectedWire = null;
                    context.setDirty(true); // 변경 감지 ✨
                }
                break;
            case Z:
                if (event.isControlDown()) {
                    if (event.isShiftDown()) {
                        context.historyManager.redo();
                    } else {
                        context.historyManager.undo();
                    }
                }
                break;
            case A:
                if (event.isControlDown()) {
                    context.selectedNodes.clear();
                    context.selectedNodes.addAll(context.visualNodes);
                    if (!context.selectedNodes.isEmpty()) {
                        context.setSelectedNode(context.selectedNodes.get(context.selectedNodes.size() - 1));
                    }
                    context.selectedWire = null;
                }
                break;
            case C:
                if (event.isControlDown() && context.onCopyRequested != null) {
                    context.onCopyRequested.run();
                }
                break;
            case V:
                if (event.isControlDown() && context.onPasteRequested != null) {
                    context.onPasteRequested.run();
                }
                break;
            default: break;
        }
    }

    public void handleKeyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.S && event.isControlDown() && context.onSaveRequested != null) {
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