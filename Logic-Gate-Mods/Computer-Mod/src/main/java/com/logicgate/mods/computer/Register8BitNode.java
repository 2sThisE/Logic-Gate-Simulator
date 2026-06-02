package com.logicgate.mods.computer;

import com.logicgate.editor.mod.ComponentMeta;
import com.logicgate.gates.Node;

@ComponentMeta(name = "8-bit Register", section = "SAP-1 Computer", typeId = "REG_8BIT")
public class Register8BitNode extends Node {
    private int storedValue = 0;

    public Register8BitNode() {
        super(10, 16); // D0-D7, LOAD, EN / Q0-Q7(always), B0-B7(bus)
        this.typeId = "REG_8BIT";
    }

    @Override
    public void compute() {
        int dataIn = in & 0xFF;
        boolean load = (in & (1 << 8)) != 0;
        boolean enable = (in & (1 << 9)) != 0;

        if (load) {
            storedValue = dataIn;
        }

        out = (storedValue & 0xFF) | ((enable ? storedValue : 0) << 8);
    }

    @Override
    public void resetState() {
        super.resetState();
        storedValue = 0;
    }

    public int getStoredValue() {
        return storedValue & 0xFF;
    }
}
