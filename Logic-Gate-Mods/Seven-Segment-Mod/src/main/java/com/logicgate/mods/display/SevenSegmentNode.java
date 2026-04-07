package com.logicgate.mods.display;

import com.logicgate.editor.mod.ComponentMeta;
import com.logicgate.gates.Node;

@ComponentMeta(
    name = "7-Segment Display",
    section = "7 Segment",
    typeId="SevenSegment"
)
public class SevenSegmentNode extends Node {
    public SevenSegmentNode() {
        super(8, 0); // A, B, C, D, E, F, G, DP 총 8개
        this.typeId = "SevenSegment";
    }

    @Override
    public void compute() {
        // 입력 상태만 시각화하므로 계산 로직 없음
    }
}
