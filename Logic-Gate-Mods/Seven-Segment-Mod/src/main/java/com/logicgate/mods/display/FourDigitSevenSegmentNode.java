package com.logicgate.mods.display;

import com.logicgate.editor.mod.ComponentMeta;
import com.logicgate.gates.Node;

@ComponentMeta(
    name = "Clock 7-Segment (4-Digit)",
    section = "7 Segment",
    typeId = "FourDigitSevenSegment"
)
public class FourDigitSevenSegmentNode extends Node {
    public FourDigitSevenSegmentNode() {
        super(12, 0); // 12핀 표준 규격 멀티플렉싱용
        this.typeId = "FourDigitSevenSegment";
    }

    @Override
    public void compute() {
        // 시각화 전용 노드
    }
}