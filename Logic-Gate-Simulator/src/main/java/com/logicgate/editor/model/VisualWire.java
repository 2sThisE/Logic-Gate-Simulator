package com.logicgate.editor.model;

import java.util.ArrayList;
import java.util.List;

public class VisualWire {
    public VisualNode from;
    public int outPin;
    public VisualNode to;
    public int inPin;

    public VisualWire(VisualNode from, int outPin, VisualNode to, int inPin) {
        this.from = from;
        this.outPin = outPin;
        this.to = to;
        this.inPin = inPin;
    }
}