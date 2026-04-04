package com.logicgate.editor.io;

public class NodeData {
    public String type;
    public double x, y;
    public String label;

    public NodeData(String type, double x, double y, String label) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.label = label;
    }
}