package com.logicgate.gates;

public abstract class Node {
    protected int in;           // 32비트 입력 상태
    protected int out;          // 32비트 출력 상태
    protected final int inputSize;    // 사용할 입력 핀 개수
    protected final int outputSize;   // 사용할 출력 핀 개수
    
    // 이 노드의 타입 식별자 (심볼 매핑용!)
    protected String typeId;

    // 각 출력 핀이 '어느 노드'의 '몇 번 입력 핀'으로 가는지 저장
    protected Connection[] nextNodes;

    // 연결 정보를 저장하는 내부 클래스
    public static class Connection {
        public Node target;     // 연결될 대상 노드
        public int targetPin;   // 대상 노드의 입력 핀 번호 (0~31)

        public Connection(Node target, int targetPin) {
            this.target = target;
            this.targetPin = targetPin;
        }
    }

    public Node(int inputSize, int outputSize) {
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        // 출력 핀 개수만큼 연결 공간 확보 (모두 null로 초기화됨)
        this.nextNodes = new Connection[outputSize];
    }

    public abstract void compute();

    // 내 출력 핀(target)을 nextNode의 targetPin 입력에 연결
    public void addNode(Node nextNode, int target, int targetPin) {
        if (target >= 0 && target < outputSize) {
            this.nextNodes[target] = new Connection(nextNode, targetPin);
        }
    }

    public void disconnectTarget(Node targetNode){
        for(int i=0; i<outputSize; i++){
            if(nextNodes[i]!=null&&nextNodes[i].target==targetNode) disconnectNextNode(i);
        }
    }

    // 연결 끊기 및 상대방 핀 초기화
    public void disconnectNextNode(int location) {
        if (location < 0 || location >= outputSize || nextNodes[location] == null) return;
        
        Connection conn = nextNodes[location];
        // 상대방 노드의 해당 핀(targetPin) 비트를 0으로 초기화
        conn.target.in &= ~(1 << conn.targetPin);
        
        this.nextNodes[location] = null;
    }

    // 계산된 out을 다음 노드들에게 전파
    public void transmit() {
        for (int i = 0; i < outputSize; i++) {
            Connection conn = nextNodes[i];
            if (conn != null) {
                // 내 i번째 출력 비트가 1인지 확인
                boolean isHigh = (out & (1 << i)) != 0;
                
                if (isHigh) {
                    // 대상 노드의 targetPin 비트를 1로 설정 (OR 연산)
                    conn.target.in |= (1 << conn.targetPin);
                } else {
                    // 대상 노드의 targetPin 비트를 0으로 설정 (AND NOT 연산)
                    conn.target.in &= ~(1 << conn.targetPin);
                }
            }
        }
    }

    public Node getTargetNode(int location) {
        if (location < 0 || location >= outputSize || nextNodes[location] == null) return null;
        return nextNodes[location].target;
    }

    public void setInput(int in) { this.in = in; }
    public int getIn() { return in; }
    public int getOut() { return out; }
    public int getInputSize() { return inputSize; }
    public int getOutputSize() { return outputSize; }
    public String getTypeId() { return typeId; }
    public void setTypeId(String typeId) { this.typeId = typeId; }
}