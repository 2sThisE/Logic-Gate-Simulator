package com.logicgate.gates;

// 회로의 결과를 보여주는 전구(LED) 역할의 노드
public class OutputPin extends Node {
    
    public OutputPin() {
        super(1, 0); // 입력은 1개 받고, 다음으로 내보내는 출력 핀은 없음
        this.typeId="OutputPin";
    }

    @Override
    public void compute() {
        // 화면에서 불이 켜졌는지 확인하기 위해 in 값을 out 변수에 단순히 복사해둡니다.
        // out 핀이 0개(없음)이므로 transmit()에서 남에게 전파되지는 않습니다.
        this.out = this.in;
    }
}
