package com.logicgate.gates;

public class Xor extends Node {
    public Xor(){
        super(2,1);
    }

    @Override
    public void compute() {
        out=(in&1)^((in>>1)&1);
    }
    
}
