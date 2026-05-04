package com.logicgate.editor.interaction;

import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.model.VisualWire;
import com.logicgate.editor.state.EditorContext;
import com.logicgate.gates.Joint;

import java.util.Iterator;

public class WiringManager {
    private final EditorContext context;
    private boolean historySavedForCurrentWiring = false;

    public WiringManager(EditorContext context) {
        this.context = context;
    }

    public void startWiring(VisualNode node, int pinIndex, boolean isFromOut) {
        historySavedForCurrentWiring = false;
        context.isWiring = true;
        context.isWiringFromOut = isFromOut;
        context.wiringNode = node;
        context.wiringPin = pinIndex;

        // Input pins accept one wire, so starting from an input removes the existing connection.
        if (!isFromOut) {
            boolean willDisconnect = false;
            for (VisualWire w : context.visualWires) {
                if (w.to == node && w.inPin == pinIndex) willDisconnect = true;
            }
            if (willDisconnect) {
                context.historyManager.saveState();
                historySavedForCurrentWiring = true;
            }

            Iterator<VisualWire> it = context.visualWires.iterator();
            while (it.hasNext()) {
                VisualWire w = it.next();
                if (w.to == node && w.inPin == pinIndex) {
                    context.getCircuit().disconnectSpecific(w.from.node, w.outPin, w.to.node, w.inPin);
                    if (w == context.selectedWire) context.selectedWire = null;
                    it.remove();
                    context.setDirty(true);
                }
            }
        }
    }

    public void connectWires(VisualNode fromNode, int outPin, VisualNode toNode, int inPin) {
        if (!isValidConnection(fromNode, outPin, toNode, inPin)) {
            historySavedForCurrentWiring = false;
            return;
        }

        if (!historySavedForCurrentWiring) {
            context.historyManager.saveState();
        }
        context.visualWires.removeIf(w -> {
            boolean removed = false;
            if (w.to == toNode && w.inPin == inPin) {
                context.getCircuit().disconnectSpecific(w.from.node, w.outPin, w.to.node, w.inPin);
                removed = true;
            }

            if (removed && w == context.selectedWire) {
                context.selectedWire = null;
            }
            return removed;
        });

        context.getCircuit().connect(fromNode.node, outPin, toNode.node, inPin);
        VisualWire newWire = new VisualWire(fromNode, outPin, toNode, inPin);
        context.visualWires.add(newWire);

        context.selectedWire = newWire;
        context.setSelectedNode(null);
        context.setDirty(true);
        historySavedForCurrentWiring = false;
    }

    public boolean isValidConnection(VisualNode fromNode, int outPin, VisualNode toNode, int inPin) {
        if (fromNode == toNode) return false;
        if (isJointAlreadyDriven(toNode)) return false;

        if (fromNode.node instanceof Joint) {
            java.util.Set<Integer> usedOutPins = new java.util.HashSet<>();
            for (VisualWire w : context.visualWires) {
                if (w.from == fromNode) {
                    usedOutPins.add(w.outPin);
                }
            }
            if (!usedOutPins.contains(outPin) && usedOutPins.size() >= 3) return false;
        }

        return true;
    }

    private boolean isJointAlreadyDriven(VisualNode toNode) {
        if (!(toNode.node instanceof Joint)) return false;

        for (VisualWire wire : context.visualWires) {
            if (wire.to == toNode) {
                return true;
            }
        }
        return false;
    }
}
