package com.logicgate.gates;

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
