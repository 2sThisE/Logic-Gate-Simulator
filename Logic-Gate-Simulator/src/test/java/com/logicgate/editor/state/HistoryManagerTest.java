package com.logicgate.editor.state;

import com.logicgate.Circuit;
import com.logicgate.editor.io.NodeData;
import com.logicgate.editor.io.ProjectData;
import com.logicgate.editor.io.WireData;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.model.VisualWire;
import com.logicgate.gates.And;
import com.logicgate.gates.InputPin;
import com.logicgate.api.component.Node;
import com.logicgate.gates.OutputPin;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class HistoryManagerTest {
    @Test
    public void undoAndRedoRestoreNodesAndWires() {
        EditorContext context = new EditorContext(new Circuit());
        Circuit originalCircuit = context.getCircuit();
        VisualNode input = addNode(context, new InputPin(), 10, 20, "Input");
        input.locked = true;
        context.historyManager.saveState();

        VisualNode output = addNode(context, new OutputPin(), 120, 20, "Output");
        addWire(context, input, 0, output, 0);

        context.historyManager.undo();
        assertEquals(1, context.visualNodes.size());
        assertEquals(0, context.visualWires.size());
        assertEquals("Input", context.visualNodes.get(0).label);
        assertTrue(context.visualNodes.get(0).locked);
        assertSame(originalCircuit, context.getCircuit());

        context.historyManager.redo();
        assertEquals(2, context.visualNodes.size());
        assertEquals(1, context.visualWires.size());
        assertSame(context.visualNodes.get(0), context.visualWires.get(0).from);
        assertSame(context.visualNodes.get(1), context.visualWires.get(0).to);
        assertSame(originalCircuit, context.getCircuit());
    }

    @Test
    public void failedRestoreDoesNotClearCurrentCanvas() throws Exception {
        EditorContext context = new EditorContext(new Circuit());
        VisualNode input = addNode(context, new InputPin(), 10, 20, "Input");
        VisualNode output = addNode(context, new OutputPin(), 120, 20, "Output");
        addWire(context, input, 0, output, 0);

        ProjectData corrupted = new ProjectData();
        corrupted.nodes = null;
        undoStack(context.historyManager).push(corrupted);

        context.historyManager.undo();

        assertEquals(2, context.visualNodes.size());
        assertEquals(1, context.visualWires.size());
        assertSame(input, context.visualWires.get(0).from);
        assertSame(output, context.visualWires.get(0).to);
        assertEquals(0, undoStack(context.historyManager).size());
    }

    @Test
    public void failedRestoreWithNullWireListDoesNotClearCurrentCanvas() throws Exception {
        EditorContext context = new EditorContext(new Circuit());
        VisualNode input = addNode(context, new InputPin(), 10, 20, "Input");
        VisualNode output = addNode(context, new OutputPin(), 120, 20, "Output");
        addWire(context, input, 0, output, 0);

        ProjectData corrupted = new ProjectData();
        corrupted.wires = null;
        undoStack(context.historyManager).push(corrupted);

        context.historyManager.undo();

        assertEquals(2, context.visualNodes.size());
        assertEquals(1, context.visualWires.size());
        assertSame(input, context.visualWires.get(0).from);
        assertSame(output, context.visualWires.get(0).to);
    }

    @Test
    public void saveStateSkipsWireWithNullBendPointList() {
        EditorContext context = new EditorContext(new Circuit());
        VisualNode input = addNode(context, new InputPin(), 10, 20, "Input");
        VisualNode output = addNode(context, new OutputPin(), 120, 20, "Output");
        VisualWire wire = addWire(context, input, 0, output, 0);
        wire.bendPoints = null;

        context.historyManager.saveState();

        assertCanvasConsistent(context);
    }

    @Test
    public void saveStateSkipsWireAfterEndpointWasRemovedFromNodeList() {
        EditorContext context = new EditorContext(new Circuit());
        VisualNode input = addNode(context, new InputPin(), 10, 20, "Input");
        VisualNode output = addNode(context, new OutputPin(), 120, 20, "Output");
        addWire(context, input, 0, output, 0);
        context.visualNodes.remove(output);

        context.historyManager.saveState();
        context.historyManager.undo();

        assertEquals(1, context.visualNodes.size());
        assertEquals(0, context.visualWires.size());
    }

    @Test
    public void restoreSkipsMalformedItemsWithoutThrowing() throws Exception {
        EditorContext context = new EditorContext(new Circuit());
        addNode(context, new InputPin(), 10, 20, "Before");

        ProjectData data = new ProjectData();
        data.nodes.add(null);
        data.nodes.add(new NodeData("InputPin", 20, 30, 0, "Input", false, null));
        data.nodes.add(new NodeData("OutputPin", 140, 30, 0, "Output", false, null));
        data.wires.add(null);
        data.wires.add(new WireData(-1, 0, 1, 0));
        data.wires.add(new WireData(0, 0, 99, 0));
        WireData validWire = new WireData(1, 0, 2, 0);
        validWire.routeMode = "NotARealMode";
        validWire.bendPoints.add(null);
        validWire.bendPoints.add(new WireData.PointData(50, 60));
        data.wires.add(validWire);

        undoStack(context.historyManager).push(data);
        context.historyManager.undo();

        assertEquals(2, context.visualNodes.size());
        assertEquals(1, context.visualWires.size());
        assertEquals(1, context.visualWires.get(0).bendPoints.size());
        assertNotNull(context.getCircuit());
    }

    @Test
    public void repeatedUndoRedoKeepsWireReferencesValid() {
        EditorContext context = new EditorContext(new Circuit());
        Random random = new Random(10403);

        for (int i = 0; i < 80; i++) {
            context.historyManager.saveState();
            mutateCanvas(context, random, i);
            assertCanvasConsistent(context);
        }

        for (int i = 0; i < 160; i++) {
            if (random.nextBoolean()) {
                context.historyManager.undo();
            } else {
                context.historyManager.redo();
            }
            assertCanvasConsistent(context);
        }
    }

    private static void mutateCanvas(EditorContext context, Random random, int step) {
        if (context.visualNodes.size() < 2 || random.nextInt(4) == 0) {
            Node node = (step % 3 == 0) ? new And() : (step % 3 == 1 ? new InputPin() : new OutputPin());
            addNode(context, node, random.nextInt(400), random.nextInt(300), "N" + step);
            return;
        }

        if (random.nextBoolean()) {
            VisualNode node = context.visualNodes.get(random.nextInt(context.visualNodes.size()));
            node.x += random.nextInt(21) - 10;
            node.y += random.nextInt(21) - 10;
            return;
        }

        List<VisualNode> nodes = context.visualNodes;
        VisualNode from = nodes.get(random.nextInt(nodes.size()));
        VisualNode to = nodes.get(random.nextInt(nodes.size()));
        if (from.node.getOutputSize() > 0 && to.node.getInputSize() > 0) {
            addWire(context, from, 0, to, 0);
        }
    }

    private static void assertCanvasConsistent(EditorContext context) {
        for (VisualNode node : context.visualNodes) {
            assertNotNull(node);
            assertNotNull(node.node);
        }
        for (VisualWire wire : context.visualWires) {
            assertNotNull(wire);
            assertTrue(context.visualNodes.contains(wire.from));
            assertTrue(context.visualNodes.contains(wire.to));
        }
        assertNotNull(context.getCircuit());
    }

    private static VisualNode addNode(EditorContext context, Node node, double x, double y, String label) {
        context.getCircuit().addNode(node);
        VisualNode visualNode = new VisualNode(node, x, y, label);
        context.visualNodes.add(visualNode);
        return visualNode;
    }

    private static VisualWire addWire(EditorContext context, VisualNode from, int outPin, VisualNode to, int inPin) {
        context.getCircuit().connect(from.node, outPin, to.node, inPin);
        VisualWire wire = new VisualWire(from, outPin, to, inPin);
        wire.setRouteModeFromProjectStyle(context.projectConfig != null ? context.projectConfig.wireStyle : null);
        context.visualWires.add(wire);
        return wire;
    }

    @SuppressWarnings("unchecked")
    private static Stack<ProjectData> undoStack(HistoryManager historyManager) throws Exception {
        Field field = HistoryManager.class.getDeclaredField("undoStack");
        field.setAccessible(true);
        return (Stack<ProjectData>) field.get(historyManager);
    }
}
