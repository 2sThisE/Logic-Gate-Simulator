package com.logicgate.gates;

import com.logicgate.api.component.Node;

public class Or extends Node {

    public Or() {
        super(2, 1);
        this.typeId="Or";
    }

    @Override
    public void compute() {
        out = (in & 1) | ((in >> 1) & 1);
    }

}
