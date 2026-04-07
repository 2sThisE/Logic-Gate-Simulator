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

    /**
     * 1. 빵판(Board)에 새로운 게이트를 올려놓습니다.
     */
    public synchronized void addNode(Node node) {
        if (!nodes.contains(node)) {
            nodes.add(node);
            incomingGraph.put(node, new CopyOnWriteArrayList<>());
        }
    }

    /**
     * 2. 빵판에서 특정 게이트를 제거합니다.
     */
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

    /**
     * 3. 전선 연결 (A의 특정 출력 핀을 B의 특정 입력 핀에 꽂습니다)
     */
    public synchronized void connect(Node fromNode, int outPin, Node toNode, int inPin) {
        fromNode.addNode(toNode, outPin, inPin);
        
        if (incomingGraph.containsKey(toNode) && !incomingGraph.get(toNode).contains(fromNode)) {
            incomingGraph.get(toNode).add(fromNode);
        }
    }

    /**
     * 4. 전선 해제 (A의 특정 출력 핀에 꽂힌 선을 뽑습니다 - 기존, 모든 연결 해제)
     */
    public synchronized void disconnect(Node fromNode, int outPin) {
        Node targetNode = fromNode.getTargetNode(outPin);
        
        if (targetNode != null && incomingGraph.containsKey(targetNode)) {
            incomingGraph.get(targetNode).remove(fromNode);
        }
        
        fromNode.disconnectNextNode(outPin);
    }

    /**
     * 4.1 특정 전선 1개만 해제 (A의 특정 핀에서 B의 특정 핀으로 가는 선만 뽑습니다)
     */
    public synchronized void disconnectSpecific(Node fromNode, int outPin, Node toNode, int inPin) {
        if (incomingGraph.containsKey(toNode)) {
            incomingGraph.get(toNode).remove(fromNode);
        }
        fromNode.disconnectSpecificNode(outPin, toNode, inPin);
    }

    /**
     * 5. 전체 회로 시뮬레이션 (1 Tick)
     */
    public synchronized void tick() {
        for (Node node : nodes) {
            node.compute();
        }

        for (Node node : nodes) {
            node.transmit();
        }
    }

    /**
     * 6. 실시간 시뮬레이션 스레드 시작
     */
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

    /**
     * 7. 실시간 시뮬레이션 스레드 정지
     */
    public void stopSimulation() {
        isRunning = false;
        if (simulationThread != null) {
            simulationThread.interrupt();
        }
    }

    /**
     * 8. 틱 딜레이(속도) 설정
     */
    public void setTickDelayMs(int ms) {
        this.tickDelayMs = ms;
    }
    /**
     * 9. 회로 전체 초기화
     * 오빠, 새로운 프로젝트를 위해 기존 찌꺼기들을 싹 청소해줄게! 🧹✨
     */
    public synchronized void clear() {
        for (Node node : nodes) {
            removeNode(node);
        }
        nodes.clear();
        incomingGraph.clear();
    }
}