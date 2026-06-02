package com.logicgate.mods.computer;

import com.logicgate.editor.mod.ComponentMeta;
import com.logicgate.gates.Node;

@ComponentMeta(
    name = "8-bit ALU (Adder/Subtractor)",
    section = "Computer",
    typeId = "ALU_8BIT"
)
public class ALUNode extends Node {

    public ALUNode() {
        super(18, 10); // A (8), B (8), Sub (1), Enable (1) / Q (8), Zero (1), Carry (1)
        this.typeId = "ALU_8BIT";
    }

    @Override
    public void compute() {
        int a = in & 0xFF;
        int b = (in >> 8) & 0xFF;
        boolean sub = (in & (1 << 16)) != 0;
        boolean enable = (in & (1 << 17)) != 0;

        int result;
        int carry = 0;
        
        if (sub) {
            result = a - b;
            if (result < 0) carry = 1;
            result &= 0xFF;
        } else {
            result = a + b;
            if (result > 0xFF) carry = 1;
            result &= 0xFF;
        }

        int zero = (result == 0) ? 1 : 0;

        if (enable) {
            out = result | (zero << 8) | (carry << 9);
        } else {
            out = 0;
        }
    }
}
