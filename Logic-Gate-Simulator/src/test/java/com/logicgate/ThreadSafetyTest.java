package com.logicgate;

import com.logicgate.api.component.Node;
import com.logicgate.gates.And;
import com.logicgate.gates.InputPin;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNull;

public class ThreadSafetyTest {
    private static final int THREAD_COUNT = 20;
    private static final int OPERATIONS_PER_THREAD = 1_000;

    @Test(timeout = 30_000)
    public void concurrentCircuitMutationsRemainStable() throws InterruptedException {
        Circuit circuit = new Circuit();
        circuit.setTickFrequencyHz(60);

        CountDownLatch startSignal = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread[] workers = new Thread[THREAD_COUNT];

        circuit.startSimulation();
        try {
            for (int i = 0; i < workers.length; i++) {
                int workerIndex = i;
                workers[i] = new Thread(
                    () -> mutateCircuit(circuit, startSignal, failure, workerIndex),
                    "circuit-stress-" + workerIndex
                );
                workers[i].start();
            }

            startSignal.countDown();
            for (Thread worker : workers) {
                worker.join();
            }
        } finally {
            circuit.stopSimulation();
        }

        assertNull("Concurrent circuit mutation failed", failure.get());
    }

    private static void mutateCircuit(
        Circuit circuit,
        CountDownLatch startSignal,
        AtomicReference<Throwable> failure,
        int workerIndex
    ) {
        try {
            startSignal.await();

            Random random = new Random(workerIndex);
            List<Node> localNodes = new ArrayList<>();

            for (int i = 0; i < OPERATIONS_PER_THREAD && failure.get() == null; i++) {
                int action = random.nextInt(4);

                if (action == 0) {
                    Node node = random.nextBoolean() ? new And() : new InputPin();
                    circuit.addNode(node);
                    localNodes.add(node);
                } else if (action == 1 && localNodes.size() > 1) {
                    Node from = randomNode(random, localNodes);
                    Node to = randomNode(random, localNodes);
                    if (from.getOutputSize() > 0 && to.getInputSize() > 0) {
                        circuit.connect(from, 0, to, 0);
                    }
                } else if (action == 2 && !localNodes.isEmpty()) {
                    Node from = randomNode(random, localNodes);
                    if (from.getOutputSize() > 0) {
                        circuit.disconnect(from, 0);
                    }
                } else if (action == 3 && !localNodes.isEmpty()) {
                    Node node = localNodes.remove(random.nextInt(localNodes.size()));
                    circuit.removeNode(node);
                }
            }
        } catch (Throwable throwable) {
            failure.compareAndSet(null, throwable);
        }
    }

    private static Node randomNode(Random random, List<Node> nodes) {
        return nodes.get(random.nextInt(nodes.size()));
    }
}
