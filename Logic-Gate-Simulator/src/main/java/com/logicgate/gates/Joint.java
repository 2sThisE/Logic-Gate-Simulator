package com.logicgate.gates;

import java.util.List;

import com.logicgate.editor.model.Property;

// 십자형 분기점(Joint) 노드: 4개의 입출력 포트를 가지며 어느 쪽으로든 신호 전파 가능
public class Joint extends Node {
    
    private int pinCount = 4; // 기본 4개 💖

    public Joint() {
        super(4, 4); // 4개의 입력 핀과 4개의 출력 핀이 쌍을 이룸
        this.typeId="Joint";
    }

    @Override
    public List<Property<?>> getComponentProperties() {
        List<Property<?>> props = super.getComponentProperties();
        props.add(new Property<>("단자 수 (2~8)", pinCount, Property.Type.INTEGER, newVal -> {
            int newCount = Math.max(2, Math.min(8, (Integer) newVal));
            if (this.pinCount != newCount) {
                this.pinCount = newCount;
                this.properties.put("pinCount", String.valueOf(pinCount));
                rebuildPins(); // 핀 개수 변경 시 연결 정리 🔪💕
            }
        }));
        return props;
    }

    private void rebuildPins() {
        // 기존 연결들을 유지하려고 시도하지만, 범위 밖으로 나가는 핀들은 정리됨
        Connection[] oldConns = this.nextNodes;
        this.nextNodes = new Connection[pinCount];
        System.arraycopy(oldConns, 0, this.nextNodes, 0, Math.min(oldConns.length, pinCount));
        
        // Node 클래스의 inputSize, outputSize는 final이 아니므로 직접 수정 (주의!)
        try {
            java.lang.reflect.Field inField = Node.class.getDeclaredField("inputSize");
            inField.setAccessible(true);
            inField.set(this, pinCount);
            
            java.lang.reflect.Field outField = Node.class.getDeclaredField("outputSize");
            outField.setAccessible(true);
            outField.set(this, pinCount);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    protected void applyProperties() {
        if (properties.containsKey("pinCount")) {
            this.pinCount = Integer.parseInt(properties.get("pinCount"));
            rebuildPins();
        }
    }

    @Override
    public void compute() {
        // 모든 입력 핀 중 하나라도 HIGH면 모든 출력 핀 HIGH
        int mask = (1 << pinCount) - 1;
        boolean anyHigh = (in & mask) != 0;
        out = anyHigh ? mask : 0;
    }
}
