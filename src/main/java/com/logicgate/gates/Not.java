package com.logicgate.gates;

public class Not extends Node{
    public Not(){
        super(1, 1);
    }

    @Override
    public void compute() {
        out=(~in)&1;
    }
}
