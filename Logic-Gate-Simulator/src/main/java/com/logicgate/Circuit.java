package com.logicgate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.logicgate.api.component.Node;

/**
 * 대규모 논리 회로를 외부에서 관리하는 회로 매니저 클래스입니다.
 * 노드의 생성, 배치(추가), 선 연결, 그리고 전체 회로의 시뮬레이션(Tick)을 담당합니다.
 */
public class Circuit {
    private static final class Connection {
        private final Node target;
        private final int targetPin;

        private Connection(Node target, int targetPin) {
            this.target = target;
            this.targetPin = targetPin;
        }
    }

    // JavaFX UI(메인 스레드)와 시뮬레이션 엔진(백그라운드 스레드) 간의 충돌 방지용 동기화 컬렉션
    private List<Node> nodes = new CopyOnWriteArrayList<>();
    private Map<Node, Map<Integer, List<Connection>>> outgoingGraph = new HashMap<>();

    // 시뮬레이션 스레드 제어 플래그
    private volatile boolean isRunning = false;
    private Thread simulationThread;
    private volatile int tickDelayMs = 16; // 기본 약 60Hz (16ms)
    private volatile long simulationGeneration = 0;

    public synchronized void addNode(Node node) {
        if (!nodes.contains(node)) {
            nodes.add(node);
            outgoingGraph.put(node, new HashMap<>());
        }
    }

    public synchronized void removeNode(Node node) {
        if (!nodes.contains(node)) return;

        Map<Integer, List<Connection>> outgoing = outgoingGraph.remove(node);
        if (outgoing != null) {
            for (List<Connection> connections : outgoing.values()) {
                for (Connection connection : connections) {
                    clearInput(connection);
                }
            }
        }

        for (Map<Integer, List<Connection>> connectionsByPin : outgoingGraph.values()) {
            Iterator<Map.Entry<Integer, List<Connection>>> pinIterator =
                connectionsByPin.entrySet().iterator();
            while (pinIterator.hasNext()) {
                List<Connection> connections = pinIterator.next().getValue();
                connections.removeIf(connection -> connection.target == node);
                if (connections.isEmpty()) {
                    pinIterator.remove();
                }
            }
        }

        node.setInput(0);
        nodes.remove(node);
    }

    public synchronized void connect(Node fromNode, int outPin, Node toNode, int inPin) {
        if (fromNode == null || toNode == null ||
            outPin < 0 || outPin >= fromNode.getOutputSize() ||
            inPin < 0 || inPin >= toNode.getInputSize()) {
            return;
        }
        outgoingGraph
            .computeIfAbsent(fromNode, ignored -> new HashMap<>())
            .computeIfAbsent(outPin, ignored -> new ArrayList<>())
            .add(new Connection(toNode, inPin));
    }

    public synchronized void disconnect(Node fromNode, int outPin) {
        Map<Integer, List<Connection>> connectionsByPin = outgoingGraph.get(fromNode);
        if (connectionsByPin == null) return;

        List<Connection> removed = connectionsByPin.remove(outPin);
        if (removed == null) return;

        for (Connection connection : removed) {
            clearInput(connection);
        }
    }

    public synchronized void disconnectSpecific(Node fromNode, int outPin, Node toNode, int inPin) {
        Map<Integer, List<Connection>> connectionsByPin = outgoingGraph.get(fromNode);
        if (connectionsByPin == null) return;

        List<Connection> connections = connectionsByPin.get(outPin);
        if (connections == null) return;

        Iterator<Connection> iterator = connections.iterator();
        while (iterator.hasNext()) {
            Connection connection = iterator.next();
            if (connection.target == toNode && connection.targetPin == inPin) {
                iterator.remove();
                clearInput(connection);
                break;
            }
        }
        if (connections.isEmpty()) {
            connectionsByPin.remove(outPin);
        }
    }

    public synchronized void tick() {
        pruneInvalidConnections();

        for (Node node : nodes) {
            node.compute();
        }

        for (Node node : nodes) {
            transmit(node);
        }
    }

    public synchronized void startSimulation() {
        if (isRunning) return;
        isRunning = true;
        long generation = ++simulationGeneration;
        Thread startedThread = new Thread(() -> {
            try {
                while (isRunning && simulationGeneration == generation) {
                    tick();
                    try {
                        Thread.sleep(tickDelayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } finally {
                synchronized (Circuit.this) {
                    if (simulationThread == Thread.currentThread()) {
                        simulationThread = null;
                    }
                }
            }
        }, "logic-gate-simulation");
        simulationThread = startedThread;
        startedThread.setDaemon(true); // 프로그램 종료 시 스레드도 함께 종료
        startedThread.start();
    }

    public synchronized void stopSimulation() {
        isRunning = false;
        simulationGeneration++;
        if (simulationThread != null) {
            simulationThread.interrupt();
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setTickFrequencyHz(double hz) {
        if (hz <= 0) hz = 1.0;
        this.tickDelayMs = Math.max(1, (int) (1000.0 / hz));
    }
    public synchronized void resetState() {
        for (Node node : nodes) {
            node.resetState();
        }
    }

    public synchronized void clear() {
        for (Node node : nodes) {
            removeNode(node);
        }
        nodes.clear();
        outgoingGraph.clear();
    }

    public synchronized void replaceContentsFrom(Circuit replacement) {
        if (replacement == null || replacement == this) return;

        clear();
        nodes.addAll(replacement.nodes);
        for (Map.Entry<Node, Map<Integer, List<Connection>>> nodeEntry :
            replacement.outgoingGraph.entrySet()) {
            Map<Integer, List<Connection>> connectionsByPin = new HashMap<>();
            for (Map.Entry<Integer, List<Connection>> pinEntry : nodeEntry.getValue().entrySet()) {
                connectionsByPin.put(pinEntry.getKey(), new ArrayList<>(pinEntry.getValue()));
            }
            outgoingGraph.put(nodeEntry.getKey(), connectionsByPin);
        }
    }

    public synchronized List<Node> getTargetNodes(Node fromNode, int outPin) {
        Map<Integer, List<Connection>> connectionsByPin = outgoingGraph.get(fromNode);
        if (connectionsByPin == null) return List.of();

        List<Connection> connections = connectionsByPin.get(outPin);
        if (connections == null) return List.of();

        List<Node> targets = new ArrayList<>(connections.size());
        for (Connection connection : connections) {
            targets.add(connection.target);
        }
        return List.copyOf(targets);
    }

    private void transmit(Node source) {
        Map<Integer, List<Connection>> connectionsByPin = outgoingGraph.get(source);
        if (connectionsByPin == null) return;

        int output = source.getOut();
        for (int outPin = 0; outPin < source.getOutputSize(); outPin++) {
            List<Connection> connections = connectionsByPin.get(outPin);
            if (connections == null) continue;

            boolean high = (output & (1 << outPin)) != 0;
            for (Connection connection : connections) {
                int input = connection.target.getIn();
                int mask = 1 << connection.targetPin;
                connection.target.setInput(high ? input | mask : input & ~mask);
            }
        }
    }

    private void pruneInvalidConnections() {
        for (Map.Entry<Node, Map<Integer, List<Connection>>> nodeEntry :
            outgoingGraph.entrySet()) {
            Node source = nodeEntry.getKey();
            Iterator<Map.Entry<Integer, List<Connection>>> pinIterator =
                nodeEntry.getValue().entrySet().iterator();

            while (pinIterator.hasNext()) {
                Map.Entry<Integer, List<Connection>> pinEntry = pinIterator.next();
                List<Connection> connections = pinEntry.getValue();

                if (pinEntry.getKey() < 0 || pinEntry.getKey() >= source.getOutputSize()) {
                    connections.forEach(this::clearInput);
                    pinIterator.remove();
                    continue;
                }

                Iterator<Connection> connectionIterator = connections.iterator();
                while (connectionIterator.hasNext()) {
                    Connection connection = connectionIterator.next();
                    if (connection.targetPin < 0 ||
                        connection.targetPin >= connection.target.getInputSize()) {
                        clearInput(connection);
                        connectionIterator.remove();
                    }
                }

                if (connections.isEmpty()) {
                    pinIterator.remove();
                }
            }
        }
    }

    private void clearInput(Connection connection) {
        int mask = 1 << connection.targetPin;
        connection.target.setInput(connection.target.getIn() & ~mask);
    }
}
