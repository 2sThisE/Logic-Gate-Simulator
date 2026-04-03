package com.logicgate.gates;

public class Nor extends Node {

    public Nor() {
        super(2, 1);
    }

    @Override
    public void compute() {
        out = (~((in & 1) | ((in >> 1) & 1)))&1;
    }

}
