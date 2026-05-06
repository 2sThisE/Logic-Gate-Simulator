package com.logicgate.editor.io;

public class NodeData {
    public String type;
    public double x, y;
    public double rotation;
    public String label;
    public boolean showLabel;
    public boolean locked;
    public String group;
    public java.util.Map<String, String> properties = new java.util.HashMap<>();

    public NodeData(String type, double x, double y, double rotation, String label, boolean showLabel, String group) {
        this(type, x, y, rotation, label, showLabel, false, group);
    }

    public NodeData(String type, double x, double y, double rotation, String label, boolean showLabel, boolean locked, String group) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.label = label;
        this.showLabel = showLabel;
        this.locked = locked;
        this.group = group;
    }
}
