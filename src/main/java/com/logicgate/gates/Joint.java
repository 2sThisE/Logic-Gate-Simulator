package com.logicgate.gates;

// 십자형 분기점(Joint) 노드: 4개의 입출력 포트를 가지며 어느 쪽으로든 신호 전파 가능
public class Joint extends Node {
    
    public Joint() {
        super(4, 4); // 4개의 입력 핀과 4개의 출력 핀이 쌍을 이룸
    }

    @Override
    public void compute() {
        // 4개의 입력 핀 중 하나라도 1(HIGH)이면, 모든 출력 포트로 1을 내보냄
        // 이를 통해 하나의 입력이 들어오는 순간 나머지가 출력 단자처럼 동작함
        boolean anyHigh = (in & 15) != 0; // 하위 4비트 확인
        out = anyHigh ? 15 : 0;           // 전체 4개 포트 출력 (1111)
    }
}
