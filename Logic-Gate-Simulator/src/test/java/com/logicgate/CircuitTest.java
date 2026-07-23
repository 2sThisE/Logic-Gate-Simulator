package com.logicgate;

import com.logicgate.gates.InputPin;
import com.logicgate.gates.OutputPin;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CircuitTest {
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
        assertTrue(source.getTargetNodes(0).isEmpty());
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

        assertTrue(source.getTargetNodes(0).isEmpty());
    }
}
