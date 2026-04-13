package com.logicgate.editor.model;

import java.util.ArrayList;
import java.util.List;

public class VisualWire {
    public VisualNode from;
    public int outPin;
    public VisualNode to;
    public int inPin;

    /**
     * 전선이 경유하는 중간 지점들 (월드 좌표계) ✨
     */
    public List<Point> waypoints = new ArrayList<>();

    public VisualWire(VisualNode from, int outPin, VisualNode to, int inPin) {
        this.from = from;
        this.outPin = outPin;
        this.to = to;
        this.inPin = inPin;
    }

    public static class Point {
        public double x, y;
        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}