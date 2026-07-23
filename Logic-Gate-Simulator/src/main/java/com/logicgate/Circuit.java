package com.logicgate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

import com.logicgate.gates.Node;

/**
 * 대규모 논리 회로를 외부에서 관리하는 회로 매니저 클래스입니다.
 * 노드의 생성, 배치(추가), 선 연결, 그리고 전체 회로의 시뮬레이션(Tick)을 담당합니다.
 */
public class Circuit {
    // JavaFX UI(메인 스레드)와 시뮬레이션 엔진(백그라운드 스레드) 간의 충돌 방지용 동기화 컬렉션
    private List<Node> nodes = new CopyOnWriteArrayList<>();
    private Map<Node, Map<Node, Integer>> incomingGraph = new ConcurrentHashMap<>();

    // 시뮬레이션 스레드 제어 플래그
    private volatile boolean isRunning = false;
    private Thread simulationThread;
    private volatile int tickDelayMs = 16; // 기본 약 60Hz (16ms)
    private volatile long simulationGeneration = 0;

    public synchronized void addNode(Node node) {
        if (!nodes.contains(node)) {
            nodes.add(node);
            incomingGraph.put(node, new ConcurrentHashMap<>());
        }
    }

    public synchronized void removeNode(Node node) {
        if (!nodes.contains(node)) return;

        for (int i = 0; i < node.getOutputSize(); i++) {
            for (Node targetNode : node.getTargetNodes(i)) {
                removeIncomingConnection(targetNode, node);
            }
            node.disconnectNextNode(i);
        }

        Map<Node, Integer> prevNodes = incomingGraph.get(node);
        if (prevNodes != null) {
            for (Node prevNode : prevNodes.keySet()) {
                prevNode.disconnectTarget(node);
            }
        }

        incomingGraph.remove(node);
        nodes.remove(node);
    }

    public synchronized void connect(Node fromNode, int outPin, Node toNode, int inPin) {
        if (fromNode == null || toNode == null ||
            outPin < 0 || outPin >= fromNode.getOutputSize() ||
            inPin < 0 || inPin >= toNode.getInputSize()) {
            return;
        }
        fromNode.addNode(toNode, outPin, inPin);

        Map<Node, Integer> incoming = incomingGraph.get(toNode);
        if (incoming != null) {
            incoming.merge(fromNode, 1, Integer::sum);
        }
    }

    public synchronized void disconnect(Node fromNode, int outPin) {
        for (Node targetNode : fromNode.getTargetNodes(outPin)) {
            removeIncomingConnection(targetNode, fromNode);
        }

        fromNode.disconnectNextNode(outPin);
    }

    public synchronized void disconnectSpecific(Node fromNode, int outPin, Node toNode, int inPin) {
        if (fromNode.disconnectSpecificNode(outPin, toNode, inPin)) {
            removeIncomingConnection(toNode, fromNode);
        }
    }

    public synchronized void tick() {
        for (Node node : nodes) {
            node.compute();
        }

        for (Node node : nodes) {
            node.transmit();
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
        incomingGraph.clear();
    }

    public synchronized void replaceContentsFrom(Circuit replacement) {
        if (replacement == null || replacement == this) return;

        clear();
        nodes.addAll(replacement.nodes);
        for (Map.Entry<Node, Map<Node, Integer>> entry : replacement.incomingGraph.entrySet()) {
            incomingGraph.put(entry.getKey(), new ConcurrentHashMap<>(entry.getValue()));
        }
    }

    private void removeIncomingConnection(Node targetNode, Node fromNode) {
        Map<Node, Integer> incoming = incomingGraph.get(targetNode);
        if (incoming == null) return;

        incoming.computeIfPresent(fromNode, (node, count) -> count > 1 ? count - 1 : null);
    }
}
