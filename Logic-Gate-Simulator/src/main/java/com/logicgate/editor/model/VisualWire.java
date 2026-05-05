package com.logicgate.editor.model;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Point2D;

public class VisualWire {
    public enum RouteMode {
        ORTHOGONAL,
        CURVED
    }

    public VisualNode from;
    public int outPin;
    public VisualNode to;
    public int inPin;
    public RouteMode routeMode;
    public List<Point2D> bendPoints = new ArrayList<>();

    public VisualWire(VisualNode from, int outPin, VisualNode to, int inPin) {
        this.from = from;
        this.outPin = outPin;
        this.to = to;
        this.inPin = inPin;
    }

    public VisualWire copyFor(VisualNode copiedFrom, VisualNode copiedTo) {
        VisualWire copy = new VisualWire(copiedFrom, outPin, copiedTo, inPin);
        copy.routeMode = routeMode;
        copy.bendPoints = new ArrayList<>(bendPoints);
        return copy;
    }

    public RouteMode getEffectiveRouteMode(String projectWireStyle) {
        if (routeMode != null) {
            return routeMode;
        }
        return "Orthogonal".equals(projectWireStyle) ? RouteMode.ORTHOGONAL : RouteMode.CURVED;
    }

    public void setRouteModeFromProjectStyle(String projectWireStyle) {
        routeMode = "Orthogonal".equals(projectWireStyle) ? RouteMode.ORTHOGONAL : RouteMode.CURVED;
    }
}
