package com.logicgate.gates;

import com.logicgate.api.component.Node;

public class Nor extends Node {

    public Nor() {
        super(2, 1);
        this.typeId="Nor";
    }

    @Override
    public void compute() {
        out = (~((in & 1) | ((in >> 1) & 1)))&1;
    }

}
