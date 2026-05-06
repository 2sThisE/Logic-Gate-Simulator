package com.logicgate.editor.state;

import com.logicgate.editor.io.NodeData;
import com.logicgate.editor.io.ProjectData;
import com.logicgate.editor.io.WireData;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.model.VisualWire;
import com.logicgate.editor.utils.NodeFactory;
import com.logicgate.Circuit;
import com.logicgate.gates.Node;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
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
        ProjectData previousState = undoStack.pop();
        if (restoreState(previousState)) {
            redoStack.push(currentState);
        }
    }

    public void redo() {
        if (redoStack.isEmpty()) return;

        ProjectData currentState = captureCurrentState();
        ProjectData nextState = redoStack.pop();
        if (restoreState(nextState)) {
            undoStack.push(currentState);
        }
    }

    private ProjectData captureCurrentState() {
        ProjectData data = new ProjectData();
        Map<VisualNode, Integer> nodeIndexes = new IdentityHashMap<>();
        for (VisualNode vn : context.visualNodes) {
            if (vn == null || vn.node == null) continue;

            NodeData nd = new NodeData(
                vn.node.getTypeId(),
                vn.x, vn.y, vn.rotation, vn.label, vn.showLabel, vn.locked, vn.group
            );
            if (vn.node.getProperties() != null) {
                nd.properties.putAll(vn.node.getProperties());
            }
            nodeIndexes.put(vn, data.nodes.size());
            data.nodes.add(nd);
        }
        for (VisualWire vw : context.visualWires) {
            if (vw == null) continue;
            Integer fromIdx = nodeIndexes.get(vw.from);
            Integer toIdx = nodeIndexes.get(vw.to);
            if (fromIdx == null || toIdx == null) continue;

            WireData wd = new WireData(
                fromIdx,
                vw.outPin,
                toIdx,
                vw.inPin
            );
            wd.routeMode = vw.getEffectiveRouteMode(context.projectConfig != null ? context.projectConfig.wireStyle : null).name();
            wd.locked = vw.locked;
            if (vw.bendPoints != null) {
                for (Point2D point : vw.bendPoints) {
                    if (point != null) {
                        wd.bendPoints.add(new WireData.PointData(point.getX(), point.getY()));
                    }
                }
            }
            data.wires.add(wd);
        }
        return data;
    }

    private boolean restoreState(ProjectData data) {
        if (data == null || data.nodes == null || data.wires == null) return false;
        startBatchOperation();

        try {
            Circuit restoredCircuit = new Circuit();
            List<VisualNode> restoredNodes = new ArrayList<>();
            List<VisualNode> nodesByOriginalIndex = new ArrayList<>();
            List<VisualWire> restoredWires = new ArrayList<>();

            List<NodeData> nodes = data.nodes;
            List<WireData> wires = data.wires;

            for (NodeData nd : nodes) {
                if (nd == null || nd.type == null) {
                    nodesByOriginalIndex.add(null);
                    continue;
                }

                Node logicNode = NodeFactory.createNodeByType(nd.type);
                if (logicNode == null) {
                    nodesByOriginalIndex.add(null);
                    continue;
                }
                logicNode.setProperties(nd.properties);
                restoredCircuit.addNode(logicNode);
                VisualNode vn = new VisualNode(logicNode, nd.x, nd.y, nd.label);
                vn.showLabel = nd.showLabel;
                vn.locked = nd.locked;
                vn.rotation = nd.rotation;
                vn.group = nd.group;
                restoredNodes.add(vn);
                nodesByOriginalIndex.add(vn);
            }

            for (WireData wd : wires) {
                if (wd == null) continue;
                if (wd.fromIdx < 0 || wd.fromIdx >= nodesByOriginalIndex.size() ||
                    wd.toIdx < 0 || wd.toIdx >= nodesByOriginalIndex.size()) {
                    continue;
                }

                VisualNode fromVn = nodesByOriginalIndex.get(wd.fromIdx);
                VisualNode toVn = nodesByOriginalIndex.get(wd.toIdx);
                if (fromVn == null || toVn == null) continue;

                restoredCircuit.connect(fromVn.node, wd.outPin, toVn.node, wd.inPin);
                VisualWire wire = new VisualWire(fromVn, wd.outPin, toVn, wd.inPin);
                applyWireData(wire, wd);
                restoredWires.add(wire);
                restoredCircuit.tick();
            }

            context.replaceCircuit(restoredCircuit);
            context.visualNodes.clear();
            context.visualNodes.addAll(restoredNodes);
            context.visualWires.clear();
            context.visualWires.addAll(restoredWires);

            context.setSelectedNode(null);
            context.selectedNodes.clear();
            context.selectedWire = null;
            context.selectedWires.clear();
            context.selectedWireBendIndex = -1;
            context.wireBendEditMode = false;
            context.setDirty(true);
            return true;
        } catch (RuntimeException ex) {
            System.err.println("Failed to restore history state. Current canvas was left unchanged.");
            ex.printStackTrace();
            return false;
        } finally {
            stopBatchOperation();
        }
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
        wire.locked = data.locked;
        if (data.bendPoints != null) {
            for (WireData.PointData point : data.bendPoints) {
                if (point == null) continue;
                wire.bendPoints.add(new Point2D(point.x, point.y));
            }
        }
    }
}
