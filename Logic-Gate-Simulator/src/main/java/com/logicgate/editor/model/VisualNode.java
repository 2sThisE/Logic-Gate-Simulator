package com.logicgate.editor.model;

import com.logicgate.api.component.Node;
import com.logicgate.api.component.Property;
import com.logicgate.api.rendering.GateSymbol;
import com.logicgate.api.rendering.SymbolContext;
import com.logicgate.editor.rendering.symbol.SymbolRegistry;

public class VisualNode implements SymbolContext {
    public Node node;
    public double x, y;
    public double width = 80, height = 50;
    public double rotation = 0;
    public String label;
    public boolean showLabel = false;
    public boolean locked = false;
    public String group = null;

    private double dragStartX, dragStartY;

    public VisualNode(Node node, double x, double y, String label) {
        this.node = node;
        this.x = x;
        this.y = y;

        GateSymbol symbol = SymbolRegistry.getSymbol(node.getTypeId());

        if (symbol != null) {
            this.width = symbol.getUnitWidth() * GateSymbol.UNIT_SIZE;
            this.height = symbol.getUnitHeight() * GateSymbol.UNIT_SIZE;
            this.label = (label == null || label.isEmpty()) ? symbol.getDefaultLabel() : label;
        } else {
            this.width = 80;
            this.height = 60;
            this.label = (label == null || label.isEmpty()) ? node.getClass().getSimpleName() : label;
        }
    }

    public java.util.List<Property<?>> getProperties(com.logicgate.editor.state.EditorContext context) {
        java.util.List<Property<?>> props = new java.util.ArrayList<>();

        props.add(new Property<>("라벨", label, Property.Type.STRING, newVal -> {
            this.label = (String) newVal;
            context.setDirty(true);
        }));

        props.add(new Property<>("라벨 표시", showLabel, Property.Type.BOOLEAN, newVal -> {
            this.showLabel = (Boolean) newVal;
            context.setDirty(true);
        }));

        props.add(new Property<>("고정", locked, Property.Type.BOOLEAN, newVal -> {
            this.locked = (Boolean) newVal;
            context.setDirty(true);
        }));

        props.add(new Property<>("그룹 이름", group == null ? "" : group, Property.Type.STRING, newVal -> {
            String newGroup = (String) newVal;
            if (newGroup.isEmpty()) {
                this.group = null;
            } else {
                if (this.group != null && !newGroup.equals(this.group)) {
                    String oldGroup = this.group;
                    String targetName = newGroup;
                    int suffix = 1;

                    while (true) {
                        boolean exists = false;
                        for (VisualNode vn : context.visualNodes) {
                            if (targetName.equals(vn.group) && (oldGroup == null || !oldGroup.equals(vn.group))) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) break;
                        targetName = newGroup + "-" + suffix;
                        suffix++;
                    }

                    for (VisualNode vn : context.visualNodes) {
                        if (oldGroup.equals(vn.group)) {
                            vn.group = targetName;
                        }
                    }

                    if (!targetName.equals(newGroup)) {
                        javafx.application.Platform.runLater(() -> {
                            if (context.onSelectionChanged != null) {
                                context.onSelectionChanged.run();
                            }
                        });
                    }
                } else {
                    this.group = newGroup;
                }
            }
            context.setDirty(true);
        }));

        for (Property<?> nodeProp : node.getComponentProperties()) {
            props.add(new Property(nodeProp.getName(), nodeProp.getValue(), nodeProp.getType(), nodeProp.getOptions(), newVal -> {

                if (this.node instanceof com.logicgate.gates.Joint && "단자 수 (2~8)".equals(nodeProp.getName())) {
                    int newCount = (Integer) newVal;
                    int currentCount = (Integer) nodeProp.getValue();

                    if (newCount < currentCount) {
                        for (com.logicgate.editor.model.VisualWire vw : context.visualWires) {
                            if ((vw.from == this && vw.outPin >= newCount) ||
                                (vw.to == this && vw.inPin >= newCount)) {
                                context.notify(
                                    com.logicgate.editor.state.EditorContext.NotificationType.WARNING,
                                    getNotificationText("notification.joint_pin_blocked.title", "단자 수 변경 불가"),
                                    getNotificationText("notification.joint_pin_blocked.body", "연결된 선이 있는 단자는 제거할 수 없습니다. 먼저 해당 단자에 연결된 선을 지워주세요.")
                                );
                                return;
                            }
                        }
                    }
                }

                ((Property<Object>) nodeProp).setValue(newVal);
                context.setDirty(true);
            }));
        }

        return props;
    }

    private String getNotificationText(String key, String fallback) {
        try {
            return java.util.ResourceBundle
                .getBundle("com.logicgate.ui.strings", java.util.Locale.getDefault())
                .getString(key);
        } catch (Exception e) {
            return fallback;
        }
    }

    public void setDragStart(double x, double y) {
        this.dragStartX = x;
        this.dragStartY = y;
    }

    public double getDragStartX() { return dragStartX; }
    public double getDragStartY() { return dragStartY; }

    public boolean contains(double px, double py) {
        // Transform the point into local coordinates before testing the bounds.
        double cx = x + width / 2;
        double cy = y + height / 2;

        double rad = Math.toRadians(-rotation);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        double dx = px - cx;
        double dy = py - cy;

        double rx = dx * cos - dy * sin;
        double ry = dx * sin + dy * cos;

        return rx >= -width / 2 && rx <= width / 2 && ry >= -height / 2 && ry <= height / 2;
    }

    private double getRotatedX(double localX, double localY) {
        double cx = width / 2;
        double cy = height / 2;
        double rad = Math.toRadians(rotation);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double dx = localX - cx;
        double dy = localY - cy;
        return (x + cx) + (dx * cos - dy * sin);
    }

    private double getRotatedY(double localX, double localY) {
        double cx = width / 2;
        double cy = height / 2;
        double rad = Math.toRadians(rotation);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double dx = localX - cx;
        double dy = localY - cy;
        return (y + cy) + (dx * sin + dy * cos);
    }

    public double getInPinX(int index) {
        GateSymbol symbol = SymbolRegistry.getSymbol(node.getTypeId());
        if (symbol == null) return x;
        return getRotatedX(symbol.getInPinX(this, index) - x, symbol.getInPinY(this, index) - y);
    }

    public double getInPinY(int index) {
        GateSymbol symbol = SymbolRegistry.getSymbol(node.getTypeId());
        if (symbol == null) return y;
        return getRotatedY(symbol.getInPinX(this, index) - x, symbol.getInPinY(this, index) - y);
    }

    public double getOutPinX(int index) {
        GateSymbol symbol = SymbolRegistry.getSymbol(node.getTypeId());
        if (symbol == null) return x + width;
        return getRotatedX(symbol.getOutPinX(this, index) - x, symbol.getOutPinY(this, index) - y);
    }

    public double getOutPinY(int index) {
        GateSymbol symbol = SymbolRegistry.getSymbol(node.getTypeId());
        if (symbol == null) return y + height / 2;
        return getRotatedY(symbol.getOutPinX(this, index) - x, symbol.getOutPinY(this, index) - y);
    }

    @Override
    public Node node() {
        return node;
    }

    @Override
    public double x() {
        return x;
    }

    @Override
    public double y() {
        return y;
    }

    @Override
    public double width() {
        return width;
    }

    @Override
    public double height() {
        return height;
    }

    @Override
    public double rotation() {
        return rotation;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public boolean showLabel() {
        return showLabel;
    }
}
