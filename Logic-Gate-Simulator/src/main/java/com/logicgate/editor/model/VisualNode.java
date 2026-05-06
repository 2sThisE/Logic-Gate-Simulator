package com.logicgate.editor.model;

import com.logicgate.editor.rendering.symbol.GateSymbol;
import com.logicgate.editor.rendering.symbol.SymbolRegistry;
import com.logicgate.gates.InputPin;
import com.logicgate.gates.Joint;
import com.logicgate.gates.Node;
import com.logicgate.gates.OutputPin;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class VisualNode {
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
                                System.err.println("연결된 선이 있는 단자는 제거할 수 없습니다! 먼저 선을 지워주세요.");
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

    public void draw(GraphicsContext gc, boolean isHovered, boolean isSelected, int hoveredInPin, int hoveredOutPin, VisualWire selectedWire, boolean isConnectionInvalid) {
        boolean isBodyHovered = isHovered && hoveredInPin == -1 && hoveredOutPin == -1;

        gc.save();
        gc.translate(x + width / 2, y + height / 2);
        gc.rotate(rotation);
        gc.translate(-width / 2, -height / 2);

        GateSymbol symbol = SymbolRegistry.getSymbol(node.getTypeId());
        if (symbol != null) {
            symbol.draw(gc, this, isBodyHovered, isSelected);
        } else {
            gc.setFill(Color.web("#4A90E2", 0.8));
            gc.fillRect(0, 0, width, height);
            gc.setStroke(isSelected ? Color.web("#00FFFF") : (isBodyHovered ? Color.web("#FFD700") : Color.WHITE));
            gc.setLineWidth(isSelected || isBodyHovered ? 4 : 2);
            gc.strokeRect(0, 0, width, height);
        }

        if (showLabel && label != null && !label.isEmpty()) {
            gc.save();
            double lx, ly;
            if (symbol != null) {
                lx = symbol.getLabelX(this);
                ly = symbol.getLabelY(this);
            } else {
                lx = width * 0.3;
                ly = height + 15;
            }
            gc.translate(lx, ly);
            gc.rotate(-rotation);
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", 12));
            gc.fillText(label, 0, 0);
            gc.restore();
        }
        gc.restore();

        for (int i = 0; i < node.getInputSize(); i++) {
            boolean isPinHovered = isHovered && hoveredInPin == i;
            boolean isPinSelected = (selectedWire != null && selectedWire.to == this && selectedWire.inPin == i);

            if (isPinHovered && isConnectionInvalid) {
                gc.setFill(Color.RED);
            } else if (isPinSelected) {
                gc.setFill(Color.web("#00FFFF"));
            } else if (isPinHovered) {
                gc.setFill(Color.web("#FFD700"));
            } else {
                gc.setFill(Color.web("#AAAAAA"));
            }

            double radius = (isPinHovered || isPinSelected) ? 6 : 4;
            gc.fillOval(getInPinX(i) - radius, getInPinY(i) - radius, radius * 2, radius * 2);
        }

        for (int i = 0; i < node.getOutputSize(); i++) {
            boolean isPinHovered = isHovered && hoveredOutPin == i;
            boolean isPinSelected = (selectedWire != null && selectedWire.from == this && selectedWire.outPin == i);

            if (isPinHovered && isConnectionInvalid) {
                gc.setFill(Color.RED);
            } else if (isPinSelected) {
                gc.setFill(Color.web("#00FFFF"));
            } else if (isPinHovered) {
                gc.setFill(Color.web("#FFD700"));
            } else {
                gc.setFill(Color.web("#AAAAAA"));
            }

            double radius = (isPinHovered || isPinSelected) ? 6 : 4;
            gc.fillOval(getOutPinX(i) - radius, getOutPinY(i) - radius, radius * 2, radius * 2);
        }
    }
}
