package com.logicgate.mods.bus;

import com.logicgate.editor.mod.ComponentMeta;
import com.logicgate.gates.Node;

@ComponentMeta(
    name = "8-bit Tri-state Buffer",
    section = "Bus & Buffer",
    typeId = "TRI_STATE_BUFFER_8BIT"
)
public class TriStateBuffer8BitNode extends Node {
    public TriStateBuffer8BitNode() {
        super(9, 8); // A0-A7 (8), EN (1) / Q0-Q7 (8)
        this.typeId = "TRI_STATE_BUFFER_8BIT";
    }

    @Override
    public void compute() {
        // EN은 8번 비트 (in & 0x100)
        boolean enabled = (in & (1 << 8)) != 0;
        if (enabled) {
            // A0-A7 (비트 0-7)을 그대로 출력 (비트 0-7)
            out = in & 0xFF;
        } else {
            // 비활성화 시 모든 출력 0 (High-Z 시뮬레이션: OR 버스용) 🔗💕
            out = 0;
        }
    }
}
