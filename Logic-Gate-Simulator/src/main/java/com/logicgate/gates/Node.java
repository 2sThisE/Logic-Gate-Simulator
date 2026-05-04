package com.logicgate.gates;

public abstract class Node {
    protected int in;           // 32비트 입력 상태
    protected int out;          // 32비트 출력 상태
    protected int inputSize;    // 사용할 입력 핀 개수
    protected int outputSize;   // 사용할 출력 핀 개수

    protected String typeId;

    protected java.util.Map<String, String> properties = new java.util.HashMap<>();

    protected Connection[] nextNodes;

    public static class Connection {
        public Node target;
        public int targetPin;
        public Connection next;

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

    public void resetState() {
        this.in = 0;
        this.out = 0;
    }

    public abstract void compute();

    public java.util.List<com.logicgate.editor.model.Property<?>> getComponentProperties() {
        return new java.util.ArrayList<>();
    }

    public java.util.Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(java.util.Map<String, String> props) {
        if (props != null) {
            this.properties.putAll(props);
            applyProperties();
        }
    }

    protected void applyProperties() {
    }

    public void addNode(Node nextNode, int targetIdx, int targetPin) {
        if (targetIdx >= 0 && targetIdx < outputSize) {
            Connection newConn = new Connection(nextNode, targetPin);
            newConn.next = nextNodes[targetIdx];
            this.nextNodes[targetIdx] = newConn;
        }
    }

    public void disconnectTarget(Node targetNode){
        for(int i=0; i<outputSize; i++){
            Connection prev = null;
            Connection curr = nextNodes[i];
            while (curr != null) {
                if (curr.target == targetNode) {
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

    public void disconnectSpecificNode(int location, Node targetNode, int targetPin) {
        if (location < 0 || location >= outputSize) return;

        Connection prev = null;
        Connection curr = nextNodes[location];
        while (curr != null) {
            if (curr.target == targetNode && curr.targetPin == targetPin) {
                curr.target.in &= ~(1 << curr.targetPin);

                if (prev == null) nextNodes[location] = curr.next;
                else prev.next = curr.next;
                return;
            }
            prev = curr;
            curr = curr.next;
        }
    }

    public void disconnectNextNode(int location) {
        if (location < 0 || location >= outputSize) return;

        Connection curr = nextNodes[location];
        while (curr != null) {
            curr.target.in &= ~(1 << curr.targetPin);
            curr = curr.next;
        }
        this.nextNodes[location] = null;
    }

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
