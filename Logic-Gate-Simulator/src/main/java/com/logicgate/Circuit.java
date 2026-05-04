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
    private Map<Node, List<Node>> incomingGraph = new ConcurrentHashMap<>();

    // 시뮬레이션 스레드 제어 플래그
    private volatile boolean isRunning = false;
    private Thread simulationThread;
    private int tickDelayMs = 16; // 기본 약 60Hz (16ms)

    public synchronized void addNode(Node node) {
        if (!nodes.contains(node)) {
            nodes.add(node);
            incomingGraph.put(node, new CopyOnWriteArrayList<>());
        }
    }

    public synchronized void removeNode(Node node) {
        if (!nodes.contains(node)) return;

        for (int i = 0; i < node.getOutputSize(); i++) {
            Node targetNode = node.getTargetNode(i);
            if (targetNode != null) {
                incomingGraph.get(targetNode).remove(node);
            }
            node.disconnectNextNode(i);
        }

        List<Node> prevNodes = incomingGraph.get(node);
        if (prevNodes != null) {
            for (Node prevNode : prevNodes) {
                prevNode.disconnectTarget(node);
            }
        }

        incomingGraph.remove(node);
        nodes.remove(node);
    }

    public synchronized void connect(Node fromNode, int outPin, Node toNode, int inPin) {
        fromNode.addNode(toNode, outPin, inPin);

        if (incomingGraph.containsKey(toNode) && !incomingGraph.get(toNode).contains(fromNode)) {
            incomingGraph.get(toNode).add(fromNode);
        }
    }

    public synchronized void disconnect(Node fromNode, int outPin) {
        Node targetNode = fromNode.getTargetNode(outPin);

        if (targetNode != null && incomingGraph.containsKey(targetNode)) {
            incomingGraph.get(targetNode).remove(fromNode);
        }

        fromNode.disconnectNextNode(outPin);
    }

    public synchronized void disconnectSpecific(Node fromNode, int outPin, Node toNode, int inPin) {
        if (incomingGraph.containsKey(toNode)) {
            incomingGraph.get(toNode).remove(fromNode);
        }
        fromNode.disconnectSpecificNode(outPin, toNode, inPin);
    }

    public synchronized void tick() {
        for (Node node : nodes) {
            node.compute();
        }

        for (Node node : nodes) {
            node.transmit();
        }
    }

    public void startSimulation() {
        if (isRunning) return;
        isRunning = true;
        simulationThread = new Thread(() -> {
            while (isRunning) {
                tick();
                try {
                    Thread.sleep(tickDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        simulationThread.setDaemon(true); // 프로그램 종료 시 스레드도 함께 종료
        simulationThread.start();
    }

    public void stopSimulation() {
        isRunning = false;
        if (simulationThread != null) {
            simulationThread.interrupt();
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setTickFrequencyHz(double hz) {
        if (hz <= 0) hz = 1.0;
        this.tickDelayMs = (int) (1000.0 / hz);
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
}
