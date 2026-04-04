package com.logicgate.gates;

public class And extends Node{
    public And(){
        super(2, 1);
        this.typeId="And";
    }
    @Override
    public void compute() {out=(in&1)&((in>>1)&1);}
}
