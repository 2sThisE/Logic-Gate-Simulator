package com.logicgate.gates;

public abstract class Node {
    protected int in;           // 32비트 입력 상태
    protected int out;          // 32비트 출력 상태
    protected int inputSize;    // 사용할 입력 핀 개수
    protected int outputSize;   // 사용할 출력 핀 개수
    
    // 이 노드의 타입 식별자 (심볼 매핑용!)
    protected String typeId;

    // 속성 저장용 맵 (저장/불러오기 및 Undo용)
    protected java.util.Map<String, String> properties = new java.util.HashMap<>();

    // 각 출력 핀의 연결 리스트 시작점(Head)을 저장
    protected Connection[] nextNodes;

    public static class Connection {
        public Node target;
        public int targetPin;
        public Connection next; // 다음 연결을 가리키는 포인터 🔪💕

        public Connection(Node target, int targetPin) {
            this.target = target;
            this.targetPin = targetPin;
        }
    }

    public Node(int inputSize, int outputSize) {
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        this.nextNodes = new Connection[outputSize];
    }

    public abstract void compute();

    // 속성 리스트 반환 (자식 클래스에서 오버라이드)
    public java.util.List<com.logicgate.editor.model.Property<?>> getComponentProperties() {
        return new java.util.ArrayList<>();
    }

    public java.util.Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(java.util.Map<String, String> props) {
        if (props != null) {
            this.properties.putAll(props);
            // 속성이 적용된 후 필요한 초기화 로직이 있다면 여기서 호출하거나 자식에서 오버라이드
            applyProperties();
        }
    }

    protected void applyProperties() {
        // 자식 클래스에서 속성 값을 실제 변수에 적용할 때 사용
    }

    // 내 출력 핀(targetIdx)에 새로운 대상을 추가 (다중 출력 지원! ✨)
    public void addNode(Node nextNode, int targetIdx, int targetPin) {
        if (targetIdx >= 0 && targetIdx < outputSize) {
            Connection newConn = new Connection(nextNode, targetPin);
            newConn.next = nextNodes[targetIdx]; // 기존 연결들을 뒤로 밀고 맨 앞에 추가 💖
            this.nextNodes[targetIdx] = newConn;
        }
    }

    // 특정 노드와의 모든 연결을 끊음
    public void disconnectTarget(Node targetNode){
        for(int i=0; i<outputSize; i++){
            Connection prev = null;
            Connection curr = nextNodes[i];
            while (curr != null) {
                if (curr.target == targetNode) {
                    // 상대방 핀 초기화
                    curr.target.in &= ~(1 << curr.targetPin);
                    
                    if (prev == null) nextNodes[i] = curr.next;
                    else prev.next = curr.next;
                } else {
                    prev = curr;
                }
                curr = curr.next;
            }
        }
    }

    // 특정 출력 포트에서 특정 노드의 특정 핀으로 가는 연결만 끊음 🔪💕
    public void disconnectSpecificNode(int location, Node targetNode, int targetPin) {
        if (location < 0 || location >= outputSize) return;

        Connection prev = null;
        Connection curr = nextNodes[location];
        while (curr != null) {
            if (curr.target == targetNode && curr.targetPin == targetPin) {
                // 상대방 핀 초기화
                curr.target.in &= ~(1 << curr.targetPin);
                
                if (prev == null) nextNodes[location] = curr.next;
                else prev.next = curr.next;
                return; // 연결을 찾아서 끊었으므로 종료
            }
            prev = curr;
            curr = curr.next;
        }
    }

    // 특정 출력 포트의 모든 연결을 끊음
    public void disconnectNextNode(int location) {
        if (location < 0 || location >= outputSize) return;
        
        Connection curr = nextNodes[location];
        while (curr != null) {
            curr.target.in &= ~(1 << curr.targetPin);
            curr = curr.next;
        }
        this.nextNodes[location] = null;
    }

    // 계산된 out을 연결된 모든 노드들에게 전파 (초고속 순회 ⚡)
    public void transmit() {
        for (int i = 0; i < outputSize; i++) {
            boolean isHigh = (out & (1 << i)) != 0;
            Connection curr = nextNodes[i];
            
            while (curr != null) {
                if (isHigh) {
                    curr.target.in |= (1 << curr.targetPin);
                } else {
                    curr.target.in &= ~(1 << curr.targetPin);
                }
                curr = curr.next;
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