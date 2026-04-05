package com.logicgate.editor.io;

public class NodeData {
    public String type;
    public double x, y;
    public String label;
    public boolean showLabel;
    public String group;

    public NodeData(String type, double x, double y, String label, boolean showLabel, String group) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.label = label;
        this.showLabel = showLabel;
        this.group = group;
    }
}