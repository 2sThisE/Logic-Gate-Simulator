package com.logicgate.gates;

public class Nand extends Node{
    public Nand(){
        super(2, 1);
        this.typeId="Nand";
    }
    @Override
    public void compute() {out=(~((in&1)&((in>>1)&1)))&1;}
}
