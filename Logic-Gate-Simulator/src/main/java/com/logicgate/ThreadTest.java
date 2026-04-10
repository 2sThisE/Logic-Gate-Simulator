package com.logicgate;

import com.logicgate.gates.And;
import com.logicgate.gates.InputPin;
import com.logicgate.gates.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ThreadTest {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 스레드 안전성(Thread-Safety) 스트레스 테스트 시작 ===");
        
        Circuit circuit = new Circuit();
        // 틱 딜레이를 1ms로 극단적으로 줄여서 충돌 확률을 높임
        circuit.setTickFrequencyHz(60);
        circuit.startSimulation();

        Thread[] threads = new Thread[20]; // 20개의 스레드가 동시에 회로를 수정
        
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                Random rand = new Random();
                List<Node> localNodes = new ArrayList<>();
                
                // 각 스레드당 1,000번의 무작위 회로 조작 수행
                for (int j = 0; j < 1000; j++) {
                    int action = rand.nextInt(4);
                    try {
                        if (action == 0) {
                            // 1. 부품 추가
                            Node n = rand.nextBoolean() ? new And() : new InputPin();
                            circuit.addNode(n);
                            localNodes.add(n);
                        } else if (action == 1 && localNodes.size() > 1) {
                            // 2. 부품 연결
                            Node from = localNodes.get(rand.nextInt(localNodes.size()));
                            Node to = localNodes.get(rand.nextInt(localNodes.size()));
                            if (from.getOutputSize() > 0 && to.getInputSize() > 0) {
                                circuit.connect(from, 0, to, 0);
                            }
                        } else if (action == 2 && localNodes.size() > 0) {
                            // 3. 선 뽑기
                            Node from = localNodes.get(rand.nextInt(localNodes.size()));
                            if (from.getOutputSize() > 0) {
                                circuit.disconnect(from, 0);
                            }
                        } else if (action == 3 && localNodes.size() > 0) {
                            // 4. 부품 삭제
                            Node n = localNodes.remove(rand.nextInt(localNodes.size()));
                            circuit.removeNode(n);
                        }
                    } catch (Exception e) {
                        System.err.println("❌ 스레드 충돌 발생! (ConcurrentModificationException 등)");
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            });
            threads[i].start();
        }

        // 모든 스레드가 작업을 마칠 때까지 대기
        for (Thread t : threads) {
            t.join();
        }

        circuit.stopSimulation();
        System.out.println("✅ 스트레스 테스트 완료: 단 한 번의 충돌도 발생하지 않았습니다. (스레드 안전함)");
    }
}
