package com.logicgate.api.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Node {
    protected volatile int in;  // JavaFX/시뮬레이션 스레드가 공유하는 32비트 입력 상태
    protected volatile int out; // JavaFX/시뮬레이션 스레드가 공유하는 32비트 출력 상태
    protected int inputSize;    // 사용할 입력 핀 개수
    protected int outputSize;   // 사용할 출력 핀 개수

    protected String typeId;

    protected Map<String, String> properties = new HashMap<>();

    public Node(int inputSize, int outputSize) {
        this.inputSize = inputSize;
        this.outputSize = outputSize;
    }

    public void resetState() {
        this.in = 0;
        this.out = 0;
    }

    public abstract void compute();

    public List<Property<?>> getComponentProperties() {
        return new ArrayList<>();
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> props) {
        if (props != null) {
            this.properties.putAll(props);
            applyProperties();
        }
    }

    protected void applyProperties() {
    }

    /**
     * Allows components with a configurable shape to update their logical pin counts.
     * Circuit connections are owned and validated by the simulator engine.
     */
    protected final void resizePins(int inputSize, int outputSize) {
        this.inputSize = inputSize;
        this.outputSize = outputSize;
    }

    public void setInput(int in) { this.in = in; }
    public int getIn() { return in; }
    public int getOut() { return out; }
    public int getInputSize() { return inputSize; }
    public int getOutputSize() { return outputSize; }
    public String getTypeId() { return typeId; }
    public void setTypeId(String typeId) { this.typeId = typeId; }
}
