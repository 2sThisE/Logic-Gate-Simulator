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
                context.selectedNode = null;
                context.selectedWire = null;
                context.isPlacingImport = false;
                context.pendingProjectData = null;
                break;
            case DELETE:
            case BACK_SPACE:
                if (context.selectedNode != null) {
                    removeNode(context.selectedNode);
                    context.selectedNode = null;
                } else if (context.selectedWire != null) {
                    context.getCircuit().disconnect(context.selectedWire.from.node, context.selectedWire.outPin);
                    context.visualWires.remove(context.selectedWire);
                    context.selectedWire = null;
                }
                break;
            default: break;
        }
    }

    public void handleKeyReleased(KeyEvent event) {
        context.activeKeys.remove(event.getCode());
    }

    public void updateCamera() {
        if (context.activeKeys.isEmpty()) return;

        double moveX = 0;
        double moveY = 0;

        if (context.activeKeys.contains(KeyCode.UP)    || context.activeKeys.contains(KeyCode.W)) moveY += 1;
        if (context.activeKeys.contains(KeyCode.DOWN)  || context.activeKeys.contains(KeyCode.S)) moveY -= 1;
        if (context.activeKeys.contains(KeyCode.LEFT)  || context.activeKeys.contains(KeyCode.A)) moveX += 1;
        if (context.activeKeys.contains(KeyCode.RIGHT) || context.activeKeys.contains(KeyCode.D)) moveX -= 1;

        if (moveX != 0 || moveY != 0) {
            double length = Math.hypot(moveX, moveY);
            double speed = 10.0 / context.zoom;
            
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
    }
}