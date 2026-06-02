package com.logicgate.mods.computer;

import com.logicgate.editor.mod.ComponentMeta;
import com.logicgate.gates.Node;

@ComponentMeta(name = "4-bit Step Counter", section = "SAP-1 Computer", typeId = "STEP_COUNTER")
public class StepCounterNode extends Node {
    private int count = 0;
    private boolean lastClock = false;

    public StepCounterNode() {
        super(2, 4); // CLK, CLR / Q0-Q3
        this.typeId = "STEP_COUNTER";
    }

    @Override
    public void compute() {
        boolean clock = (in & 1) != 0;
        boolean clear = (in & 2) != 0;

        if (clear) {
            count = 0;
        } else if (clock && !lastClock) {
            count = (count + 1) & 0x0F;
        }

        lastClock = clock;
        out = count;
    }

    @Override
    public void resetState() {
        super.resetState();
        count = 0;
        lastClock = false;
    }

    public int getCount() {
        return count & 0x0F;
    }
}
