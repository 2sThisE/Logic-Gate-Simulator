package com.logicgate.gates;

import com.logicgate.api.component.Node;

public class Nand extends Node{
    public Nand(){
        super(2, 1);
        this.typeId="Nand";
    }
    @Override
    public void compute() {out=(~((in&1)&((in>>1)&1)))&1;}
}
