package com.logicgate.mods.computer;

import com.logicgate.editor.mod.ComponentMeta;
import com.logicgate.gates.Node;

@ComponentMeta(name = "8-bit Program Counter", section = "SAP-1 Computer", typeId = "PC_8BIT")
public class ProgramCounterNode extends Node {
    private int count = 0;
    private boolean lastInc = false;
    private boolean lastLoad = false;

    public ProgramCounterNode() {
        super(12, 8); // J0-J7, LOAD, INC, CLR, EN / A0-A7
        this.typeId = "PC_8BIT";
    }

    @Override
    public void compute() {
        int jumpAddr = in & 0xFF;
        boolean load = (in & (1 << 8)) != 0;
        boolean inc = (in & (1 << 9)) != 0;
        boolean clear = (in & (1 << 10)) != 0;
        boolean enable = (in & (1 << 11)) != 0;

        if (clear) {
            count = 0;
        } else if (load && !lastLoad) {
            count = jumpAddr;
        } else if (inc && !lastInc) {
            count = (count + 1) & 0xFF;
        }

        lastLoad = load;
        lastInc = inc;
        out = enable ? count : 0;
    }

    @Override
    public void resetState() {
        super.resetState();
        count = 0;
        lastInc = false;
        lastLoad = false;
    }

    public int getCount() {
        return count & 0xFF;
    }
}
