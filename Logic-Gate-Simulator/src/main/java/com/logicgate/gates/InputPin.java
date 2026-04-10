package com.logicgate.gates;

import java.util.List;
import com.logicgate.editor.model.Property;

// 사용자가 강제로 0 또는 1을 설정할 수 있는 스위치 노드
public class InputPin extends Node {

    private String mode = "Toggle"; // Toggle 또는 Momentary ✨

    public InputPin() {
        super(0, 1); // 입력은 없고, 출력만 1개 나감
        this.typeId="InputPin";
    }

    @Override
    public List<Property<?>> getComponentProperties() {
        List<Property<?>> props = super.getComponentProperties();
        props.add(new Property<>("작동 방식", mode, Property.Type.CHOICE, 
            new String[]{"Toggle", "Momentary"}, newVal -> {
                this.mode = (String) newVal;
                this.properties.put("mode", mode);
            }
        ));
        return props;
    }

    @Override
    protected void applyProperties() {
        if (properties.containsKey("mode")) {
            this.mode = properties.get("mode");
        }
    }

    public String getMode() { return mode; }

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
