package com.logicgate.editor.state;

import com.logicgate.editor.io.NodeData;
import com.logicgate.editor.io.ProjectData;
import com.logicgate.editor.io.WireData;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.model.VisualWire;
import com.logicgate.editor.utils.NodeFactory;
import com.logicgate.gates.Node;

import java.util.Stack;
import javafx.geometry.Point2D;

public class HistoryManager {
    private final EditorContext context;
    private final Stack<ProjectData> undoStack = new Stack<>();
    private final Stack<ProjectData> redoStack = new Stack<>();
    private boolean isBatchOperation = false;

    public HistoryManager(EditorContext context) {
        this.context = context;
    }

    public void startBatchOperation() {
        this.isBatchOperation = true;
    }

    public void stopBatchOperation() {
        this.isBatchOperation = false;
    }

    public void saveState() {
        if (isBatchOperation) return;

        ProjectData data = captureCurrentState();
        undoStack.push(data);
        redoStack.clear();

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
            NodeData nd = new NodeData(
                vn.node.getTypeId(),
                vn.x, vn.y, vn.rotation, vn.label, vn.showLabel, vn.group
            );
            nd.properties.putAll(vn.node.getProperties());
            data.nodes.add(nd);
        }
        for (VisualWire vw : context.visualWires) {
            WireData wd = new WireData(
                context.visualNodes.indexOf(vw.from),
                vw.outPin,
                context.visualNodes.indexOf(vw.to),
                vw.inPin
            );
            wd.routeMode = vw.getEffectiveRouteMode(context.projectConfig != null ? context.projectConfig.wireStyle : null).name();
            for (Point2D point : vw.bendPoints) {
                wd.bendPoints.add(new WireData.PointData(point.getX(), point.getY()));
            }
            data.wires.add(wd);
        }
        return data;
    }

    private void restoreState(ProjectData data) {
        startBatchOperation();

        context.visualNodes.clear();
        context.visualWires.clear();
        context.getCircuit().clear();

        for (NodeData nd : data.nodes) {
            Node logicNode = NodeFactory.createNodeByType(nd.type);
            if (logicNode != null) {
                logicNode.setProperties(nd.properties);
                context.getCircuit().addNode(logicNode);
                VisualNode vn = new VisualNode(logicNode, nd.x, nd.y, nd.label);
                vn.showLabel = nd.showLabel;
                vn.rotation = nd.rotation;
                vn.group = nd.group;
                context.visualNodes.add(vn);
            }
        }

        for (WireData wd : data.wires) {
            if (wd.fromIdx >= 0 && wd.fromIdx < context.visualNodes.size() &&
                wd.toIdx >= 0 && wd.toIdx < context.visualNodes.size()) {

                VisualNode fromVn = context.visualNodes.get(wd.fromIdx);
                VisualNode toVn = context.visualNodes.get(wd.toIdx);

                context.getCircuit().connect(fromVn.node, wd.outPin, toVn.node, wd.inPin);
                VisualWire wire = new VisualWire(fromVn, wd.outPin, toVn, wd.inPin);
                applyWireData(wire, wd);
                context.visualWires.add(wire);
                // Advance once after each restored connection to reduce synchronized oscillator artifacts.
                context.getCircuit().tick();
            }
        }

        context.setSelectedNode(null);
        context.selectedNodes.clear();
        context.selectedWire = null;
        context.selectedWireBendIndex = -1;
        context.wireBendEditMode = false;
        context.setDirty(true);

        stopBatchOperation();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    private void applyWireData(VisualWire wire, WireData data) {
        if (data.routeMode != null) {
            try {
                wire.routeMode = VisualWire.RouteMode.valueOf(data.routeMode);
            } catch (IllegalArgumentException ignored) {
                wire.setRouteModeFromProjectStyle(context.projectConfig != null ? context.projectConfig.wireStyle : null);
            }
        } else {
            wire.setRouteModeFromProjectStyle(context.projectConfig != null ? context.projectConfig.wireStyle : null);
        }
        if (data.bendPoints != null) {
            for (WireData.PointData point : data.bendPoints) {
                wire.bendPoints.add(new Point2D(point.x, point.y));
            }
        }
    }
}
