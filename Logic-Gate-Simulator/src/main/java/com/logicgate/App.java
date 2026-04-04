package com.logicgate;

import com.logicgate.gates.And;
import com.logicgate.gates.InputPin;
import com.logicgate.gates.Or;

public class App {
    public static void main(String[] args) {
        System.out.println("=== 회로 시뮬레이션 시작 ===");

        // 1. 회로판(매니저) 준비
        Circuit circuit = new Circuit();

        // 2. 부품(노드) 생성
        InputPin switch1 = new InputPin();
        InputPin switch2 = new InputPin();
        InputPin switch3 = new InputPin();
        InputPin switch4 = new InputPin();
        And andGate1 = new And();
        And andGate2 = new And();
        Or orGate=new Or();

        // 3. 회로판에 부품 배치
        circuit.addNode(switch1);
        circuit.addNode(switch2);
        circuit.addNode(switch3);
        circuit.addNode(switch4);
        circuit.addNode(andGate1);
        circuit.addNode(andGate2);
        circuit.addNode(orGate);

        // 4. 전선 연결 (switchA의 0번 출력을 andGate의 0번 입력으로...)
        circuit.connect(switch1, 0, andGate1, 0);
        circuit.connect(switch2, 0, andGate1, 1);
        circuit.connect(switch3, 0, andGate2, 0);
        circuit.connect(switch4, 0, andGate2, 1);
        circuit.connect(andGate1,0, orGate, 0);
        circuit.connect(andGate2,0, orGate, 1);

        // ==========================================
        // 테스트 케이스 1: A=1, B=1 (결과는 1이어야 함)
        System.out.println("\n[테스트 1] A=ON, B=ON");
        switch1.setState(true);
        switch2.setState(true);
        switch3.setState(true);
        switch4.setState(true);

        // 신호가 게이트를 통과하려면 Tick을 돌려야 함 (안정적으로 2틱)
        // 틱 1: 스위치 -> 게이트로 전파
        // 틱 2: 게이트가 받아서 계산 및 전파
        circuit.tick(); 
        circuit.tick(); 
        circuit.tick(); 
        System.out.println("OR 게이트의 최종 결과(out): " + orGate.getOut());
    }
}
