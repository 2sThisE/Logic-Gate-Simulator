package com.logicgate.editor.io;

public class WireData {
    public int fromIdx, outPin;
    public int toIdx, inPin;

    public WireData(int fromIdx, int outPin, int toIdx, int inPin) {
        this.fromIdx = fromIdx;
        this.outPin = outPin;
        this.toIdx = toIdx;
        this.inPin = inPin;
    }
}