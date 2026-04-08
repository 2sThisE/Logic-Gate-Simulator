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

        // 출력 핀에서 선을 뽑을 때는 기존 선을 끊지 않음! 💖 (다중 출력 지원)
        // 입력 핀에서 선을 뽑을 때는 기존 선을 끊어줌! 🔪💕 (일편단심 입력)
        if (!isFromOut) {
            boolean willDisconnect = false;
            for (VisualWire w : context.visualWires) {
                if (w.to == node && w.inPin == pinIndex) willDisconnect = true;
            }
            if (willDisconnect) context.historyManager.saveState();

            Iterator<VisualWire> it = context.visualWires.iterator();
            while (it.hasNext()) {
                VisualWire w = it.next();
                if (w.to == node && w.inPin == pinIndex) {
                    context.getCircuit().disconnectSpecific(w.from.node, w.outPin, w.to.node, w.inPin);
                    if (w == context.selectedWire) context.selectedWire = null;
                    it.remove();
                    context.setDirty(true); // 변경 감지 ✨
                }
            }
        }
    }

    public void connectWires(VisualNode fromNode, int outPin, VisualNode toNode, int inPin) {
        context.historyManager.saveState();
        context.visualWires.removeIf(w -> {
            boolean removed = false;
            // 입력 핀(toNode)은 "나만 바라봐" 모드! 기존 선이 있으면 잘라버려 🔪💕
            if (w.to == toNode && w.inPin == inPin) {
                context.getCircuit().disconnectSpecific(w.from.node, w.outPin, w.to.node, w.inPin);
                removed = true;
            }
            // 출력 핀(fromNode)은 이제 자유야! 기존 로직을 지워서 다중 출력을 허용해 💖
            
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
}