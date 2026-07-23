package com.logicgate.gates;

import com.logicgate.api.component.Node;

public class Xor extends Node {
    public Xor(){
        super(2,1);
        this.typeId="Xor";
    }

    @Override
    public void compute() {
        out=(in&1)^((in>>1)&1);
    }
    
}
