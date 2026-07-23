package com.logicgate.editor.model;

import com.logicgate.api.component.Property;

import java.util.ArrayList;
import java.util.List;

import com.logicgate.editor.state.EditorContext;
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
    public boolean locked = false;
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
        copy.locked = locked;
        copy.bendPoints = new ArrayList<>(bendPoints);
        return copy;
    }

    public List<Property<?>> getProperties(EditorContext context) {
        List<Property<?>> props = new ArrayList<>();
        props.add(new Property<>("고정", locked, Property.Type.BOOLEAN, newVal -> {
            boolean newLocked = (Boolean) newVal;
            if (context.selectedWires.isEmpty()) {
                this.locked = newLocked;
            } else {
                for (VisualWire wire : context.selectedWires) {
                    wire.locked = newLocked;
                }
            }
            context.setDirty(true);
        }));
        return props;
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
