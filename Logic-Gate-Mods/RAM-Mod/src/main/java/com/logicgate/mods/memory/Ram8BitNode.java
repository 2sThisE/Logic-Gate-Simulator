package com.logicgate.mods.memory;

import com.logicgate.editor.mod.ComponentMeta;
import com.logicgate.gates.Node;

@ComponentMeta(
    name = "256x8 RAM (8-bit Addressable)",
    section = "Memory",
    typeId = "RAM_256X8"
)
public class Ram8BitNode extends Node {
    private final int[] memory = new int[256];
    private boolean lastWE = false;

    public Ram8BitNode() {
        super(18, 8); // 8-bit Addr (0-7), 8-bit Data In (8-15), WE (16), OE (17) / 8-bit Data Out (0-7)
        this.typeId = "RAM_256X8";
    }

    @Override
    public void compute() {
        int addr = in & 0xFF;           // A0-A7
        int dataIn = (in >> 8) & 0xFF;  // D0-D7
        boolean we = (in & (1 << 16)) != 0; // Write Enable
        boolean oe = (in & (1 << 17)) != 0; // Output Enable (Active High)

        // Positive Edge Triggered Write
        if (we && !lastWE) {
            memory[addr] = dataIn;
        }
        lastWE = we;

        // Output logic
        if (oe) {
            out = memory[addr] & 0xFF;
        } else {
            out = 0; // High-Z 대신 0 (OR 버스용) 🔗💕
        }
    }
}
