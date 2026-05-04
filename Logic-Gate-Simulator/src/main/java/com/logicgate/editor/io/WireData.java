package com.logicgate.editor.io;

import java.util.ArrayList;
import java.util.List;

public class WireData {
    public int fromIdx, outPin;
    public int toIdx, inPin;
    public String routeMode;
    public List<PointData> bendPoints = new ArrayList<>();

    public WireData(int fromIdx, int outPin, int toIdx, int inPin) {
        this.fromIdx = fromIdx;
        this.outPin = outPin;
        this.toIdx = toIdx;
        this.inPin = inPin;
    }

    public static class PointData {
        public double x;
        public double y;

        public PointData() {}

        public PointData(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
