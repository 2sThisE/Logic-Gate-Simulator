package com.logicgate.editor.state;

import com.logicgate.editor.io.NodeData;
import com.logicgate.editor.io.ProjectData;
import com.logicgate.editor.io.WireData;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.model.VisualWire;
import com.logicgate.editor.utils.NodeFactory;
import com.logicgate.gates.Node;

import java.util.Stack;

public class HistoryManager {
    private final EditorContext context;
    private final Stack<ProjectData> undoStack = new Stack<>();
    private final Stack<ProjectData> redoStack = new Stack<>();
    private boolean isRestoring = false;

    public HistoryManager(EditorContext context) {
        this.context = context;
    }

    public void saveState() {
        if (isRestoring) return;
        
        ProjectData data = captureCurrentState();
        undoStack.push(data);
        redoStack.clear();
        
        // 메모리 제한 (최대 50단계)
        if (undoStack.size() > 50) {
            undoStack.remove(0);
        }
    }

    public void undo() {
        if (undoStack.isEmpty()) return;
        
        ProjectData currentState = captureCurrentState();
        redoStack.push(currentState);
        
        ProjectData previousState = undoStack.pop();
        restoreState(previousState);
    }

    public void redo() {
        if (redoStack.isEmpty()) return;
        
        ProjectData currentState = captureCurrentState();
        undoStack.push(currentState);
        
        ProjectData nextState = redoStack.pop();
        restoreState(nextState);
    }

    private ProjectData captureCurrentState() {
        ProjectData data = new ProjectData();
        for (VisualNode vn : context.visualNodes) {
            data.nodes.add(new NodeData(
                vn.node.getClass().getName(),
                vn.x, vn.y, vn.label, vn.showLabel, vn.group
            ));
        }
        for (VisualWire vw : context.visualWires) {
            data.wires.add(new WireData(
                context.visualNodes.indexOf(vw.from),
                vw.outPin,
                context.visualNodes.indexOf(vw.to),
                vw.inPin
            ));
        }
        return data;
    }

    private void restoreState(ProjectData data) {
        isRestoring = true;
        
        // 현재 상태 정리
        context.visualNodes.clear();
        context.visualWires.clear();
        context.getCircuit().clear();
        
        // 노드 복원
        for (NodeData nd : data.nodes) {
            Node logicNode = NodeFactory.createNodeByType(nd.type);
            if (logicNode != null) {
                context.getCircuit().addNode(logicNode);
                VisualNode vn = new VisualNode(logicNode, nd.x, nd.y, nd.label);
                vn.showLabel = nd.showLabel;
                vn.group = nd.group;
                context.visualNodes.add(vn);
            }
        }
        
        // 와이어 복원
        for (WireData wd : data.wires) {
            if (wd.fromIdx >= 0 && wd.fromIdx < context.visualNodes.size() &&
                wd.toIdx >= 0 && wd.toIdx < context.visualNodes.size()) {
                
                VisualNode fromVn = context.visualNodes.get(wd.fromIdx);
                VisualNode toVn = context.visualNodes.get(wd.toIdx);
                
                context.getCircuit().connect(fromVn.node, wd.outPin, toVn.node, wd.inPin);
                context.visualWires.add(new VisualWire(fromVn, wd.outPin, toVn, wd.inPin));
            }
        }
        
        context.setSelectedNode(null);
        context.selectedWire = null;
        context.setDirty(true);
        
        isRestoring = false;
    }
    
    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}