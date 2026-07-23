package com.logicgate;

import com.logicgate.gates.InputPin;
import com.logicgate.api.component.Node;
import com.logicgate.gates.OutputPin;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CircuitTest {
    private static final class ResizableNode extends Node {
        private ResizableNode(int inputSize, int outputSize) {
            super(inputSize, outputSize);
        }

        private void resize(int inputSize, int outputSize) {
            resizePins(inputSize, outputSize);
        }

        @Override
        public void compute() {
            out = in;
        }
    }

    @Test
    public void circuitGraphPropagatesNodeOutput() {
        Circuit circuit = new Circuit();
        InputPin source = new InputPin();
        OutputPin target = new OutputPin();
        circuit.addNode(source);
        circuit.addNode(target);
        circuit.connect(source, 0, target, 0);

        source.setState(true);
        circuit.tick();

        assertEquals(1, target.getIn());
        assertEquals(List.of(target), circuit.getTargetNodes(source, 0));
    }

    @Test
    public void replacingCircuitPreservesEngineConnections() {
        Circuit loaded = new Circuit();
        InputPin source = new InputPin();
        OutputPin target = new OutputPin();
        loaded.addNode(source);
        loaded.addNode(target);
        loaded.connect(source, 0, target, 0);

        Circuit active = new Circuit();
        active.replaceContentsFrom(loaded);
        source.setState(true);
        active.tick();

        assertEquals(1, target.getIn());
        assertEquals(List.of(target), active.getTargetNodes(source, 0));
    }

    @Test
    public void shrinkingPinsPrunesEngineConnections() {
        Circuit circuit = new Circuit();
        InputPin source = new InputPin();
        ResizableNode target = new ResizableNode(2, 0);
        circuit.addNode(source);
        circuit.addNode(target);
        circuit.connect(source, 0, target, 1);

        source.setState(true);
        circuit.tick();
        assertEquals(2, target.getIn());

        target.resize(1, 0);
        circuit.tick();

        assertEquals(0, target.getIn());
        assertTrue(circuit.getTargetNodes(source, 0).isEmpty());
    }

    @Test
    public void removingTargetClearsRemainingParallelConnection() {
        Circuit circuit = new Circuit();
        InputPin source = new InputPin();
        OutputPin target = new OutputPin();
        circuit.addNode(source);
        circuit.addNode(target);

        circuit.connect(source, 0, target, 0);
        circuit.connect(source, 0, target, 0);
        circuit.disconnectSpecific(source, 0, target, 0);
        circuit.removeNode(target);

        source.setState(true);
        circuit.tick();

        assertEquals(0, target.getIn());
        assertTrue(circuit.getTargetNodes(source, 0).isEmpty());
    }

    @Test
    public void rapidRestartKeepsSimulationControllable() {
        Circuit circuit = new Circuit();
        circuit.startSimulation();
        circuit.stopSimulation();
        circuit.startSimulation();

        assertTrue(circuit.isRunning());
        circuit.stopSimulation();
        assertFalse(circuit.isRunning());
    }

    @Test
    public void invalidPinsDoNotCreateConnections() {
        Circuit circuit = new Circuit();
        InputPin source = new InputPin();
        OutputPin target = new OutputPin();
        circuit.addNode(source);
        circuit.addNode(target);

        circuit.connect(source, 5, target, 0);
        circuit.connect(source, 0, target, 5);

        assertTrue(circuit.getTargetNodes(source, 0).isEmpty());
    }
}
