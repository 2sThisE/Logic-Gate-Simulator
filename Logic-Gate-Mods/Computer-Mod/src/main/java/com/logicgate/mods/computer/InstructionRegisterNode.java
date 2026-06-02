package com.logicgate.mods.computer;

import com.logicgate.editor.mod.ComponentMeta;
import com.logicgate.gates.Node;

@ComponentMeta(name = "SAP-1 Instruction Register", section = "SAP-1 Computer", typeId = "IR_8BIT")
public class InstructionRegisterNode extends Node {
    private int storedValue = 0;

    public InstructionRegisterNode() {
        super(10, 16); // D0-D7, LOAD, ADDR_OUT / Q0-Q7(always), A0-A3(bus low nibble)
        this.typeId = "IR_8BIT";
    }

    @Override
    public void compute() {
        int dataIn = in & 0xFF;
        boolean load = (in & (1 << 8)) != 0;
        boolean addressOut = (in & (1 << 9)) != 0;

        if (load) {
            storedValue = dataIn;
        }

        int busOut = addressOut ? (storedValue & 0x0F) : 0;
        out = (storedValue & 0xFF) | (busOut << 8);
    }

    @Override
    public void resetState() {
        super.resetState();
        storedValue = 0;
    }

    public int getStoredValue() {
        return storedValue & 0xFF;
    }

    public int getOpcode() {
        return (storedValue >> 4) & 0x0F;
    }

    public int getOperand() {
        return storedValue & 0x0F;
    }
}
