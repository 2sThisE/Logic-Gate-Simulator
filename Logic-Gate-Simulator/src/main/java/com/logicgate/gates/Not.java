package com.logicgate.gates;

import com.logicgate.api.component.Node;

public class Not extends Node{
    public Not(){
        super(1, 1);
        this.typeId="Not";
    }

    @Override
    public void compute() {
        out=(~in)&1;
    }
}
