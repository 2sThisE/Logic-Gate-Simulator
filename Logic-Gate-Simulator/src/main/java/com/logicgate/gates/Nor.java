package com.logicgate.gates;

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
