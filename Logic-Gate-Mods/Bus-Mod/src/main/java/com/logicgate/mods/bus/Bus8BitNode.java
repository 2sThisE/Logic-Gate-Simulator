package com.logicgate.mods.bus;

import com.logicgate.editor.mod.ComponentMeta;
import com.logicgate.gates.Node;

@ComponentMeta(
    name = "8-bit 3-Port Bus Aggregator",
    section = "Bus & Buffer",
    typeId = "BUS_AGGREGATOR_8BIT"
)
public class Bus8BitNode extends Node {
    public Bus8BitNode() {
        super(24, 8); // A (8), B (8), C (8) / Q (8)
        this.typeId = "BUS_AGGREGATOR_8BIT";
    }

    @Override
    public void compute() {
        int a = in & 0xFF;
        int b = (in >> 8) & 0xFF;
        int c = (in >> 16) & 0xFF;
        
        // 여러 소스에서 오는 데이터를 OR하여 통합 (High-Z 대체 시뮬레이션) 🔗💕
        out = a | b | c;
    }
}
