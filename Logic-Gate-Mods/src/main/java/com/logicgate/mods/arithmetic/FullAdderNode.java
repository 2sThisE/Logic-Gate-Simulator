package com.logicgate.mods.arithmetic;

import com.logicgate.gates.Node;
import com.logicgate.editor.mod.ComponentMeta;
import java.io.*;
import java.net.URI;

@ComponentMeta(section = "Arithmetic", name = "Full Adder", typeId = "FULL_ADDER")
public class FullAdderNode extends Node {

    public FullAdderNode() {
        super(3, 2);
    }

    @Override
    public void compute(){
        try {
            
            File fp=new File(new URI(null));
        } catch (Exception e) {
            // TODO: handle exception
        }
        int a = (in & 1);
        int b = (in >> 1) & 1;
        int cin = (in >> 2) & 1;

        int sum = a ^ b ^ cin;
        int cout = (a & b) | (cin & (a ^ b));

        out = sum | (cout << 1);
    }
}