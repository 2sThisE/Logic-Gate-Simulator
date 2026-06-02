package com.logicgate.mods.computer;

import com.logicgate.editor.mod.ComponentMeta;
import com.logicgate.gates.Node;

@ComponentMeta(name = "8-bit Debug Probe", section = "SAP-1 Debug", typeId = "SAP1_PROBE_8BIT")
public class Probe8BitNode extends Node {
    public Probe8BitNode() {
        super(8, 0);
        this.typeId = "SAP1_PROBE_8BIT";
    }

    @Override
    public void compute() {
    }
}
