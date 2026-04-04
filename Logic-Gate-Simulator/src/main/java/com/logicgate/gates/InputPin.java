package com.logicgate.gates;

// 사용자가 강제로 0 또는 1을 설정할 수 있는 스위치 노드
public class InputPin extends Node {
    
    public InputPin() {
        super(0, 1); // 입력은 없고, 출력만 1개 나감
        this.typeId="InputPin";
    }

    // 사용자가 스위치를 켜거나 끌 때 호출
    public void setState(boolean isHigh) {
        this.in = isHigh ? 1 : 0;
    }

    @Override
    public void compute() {
        // 자신의 in 값을 그대로 out으로 보냄
        this.out = this.in;
    }
}
