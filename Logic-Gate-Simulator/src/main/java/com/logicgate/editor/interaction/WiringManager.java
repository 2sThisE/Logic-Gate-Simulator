package com.logicgate.editor.interaction;

import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.model.VisualWire;
import com.logicgate.editor.state.EditorContext;
import com.logicgate.gates.Joint;

import java.util.Iterator;

public class WiringManager {
    private final EditorContext context;

    public WiringManager(EditorContext context) {
        this.context = context;
    }

    public void startWiring(VisualNode node, int pinIndex, boolean isFromOut) {
        context.isWiring = true;
        context.isWiringFromOut = isFromOut;
        context.wiringNode = node;
        context.wiringPin = pinIndex;

        Iterator<VisualWire> it = context.visualWires.iterator();
        while (it.hasNext()) {
            VisualWire w = it.next();
            if (isFromOut && w.from == node && w.outPin == pinIndex) {
                context.getCircuit().disconnect(w.from.node, w.outPin);
                if (w == context.selectedWire) context.selectedWire = null;
                it.remove();
                context.setDirty(true); // 변경 감지 ✨
                break; 
            } else if (!isFromOut && w.to == node && w.inPin == pinIndex) {
                context.getCircuit().disconnect(w.from.node, w.outPin);
                if (w == context.selectedWire) context.selectedWire = null;
                it.remove();
                context.setDirty(true); // 변경 감지 ✨
            }
        }
    }

    public void connectWires(VisualNode fromNode, int outPin, VisualNode toNode, int inPin) {
        context.visualWires.removeIf(w -> {
            boolean removed = false;
            if (w.to == toNode && w.inPin == inPin) {
                context.getCircuit().disconnect(w.from.node, w.outPin);
                removed = true;
            }
            if (w.from == fromNode && w.outPin == outPin) {
                context.getCircuit().disconnect(w.from.node, w.outPin);
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
        context.setDirty(true); // 변경 감지 ✨
    }

    public boolean isValidConnection(VisualNode fromNode, int outPin, VisualNode toNode, int inPin) {
        if (fromNode == toNode) return false;

        if (fromNode.node instanceof Joint) {
            int currentOuts = 0;
            boolean isReplacingSelf = false;
            for (VisualWire w : context.visualWires) {
                if (w.from == fromNode) {
                    if (w.outPin == outPin) isReplacingSelf = true;
                    currentOuts++;
                }
            }
            if (!isReplacingSelf && currentOuts >= 3) return false;
        }

        return true;
    }
}