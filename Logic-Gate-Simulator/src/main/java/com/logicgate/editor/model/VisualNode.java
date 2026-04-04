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
    public String label;
    public boolean showLabel = false;

    public VisualNode(Node node, double x, double y, String label) {
        this.node = node;
        this.x = x;
        this.y = y;
        
        GateSymbol symbol = SymbolRegistry.getSymbol(node.getTypeId());
        
        if (symbol != null) {
            this.width = symbol.getPreferredWidth();
            this.height = symbol.getPreferredHeight();
            this.label = (label == null || label.isEmpty()) ? symbol.getDefaultLabel() : label;
        } else {
            this.width = 80;
            this.height = 50;
            this.label = (label == null || label.isEmpty()) ? node.getClass().getSimpleName() : label;
        }
    }

    public boolean contains(double px, double py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    public double getInPinX(int index) {
        GateSymbol symbol = SymbolRegistry.getSymbol(node.getTypeId());
        return symbol != null ? symbol.getInPinX(this, index) : x;
    }

    public double getInPinY(int index) {
        GateSymbol symbol = SymbolRegistry.getSymbol(node.getTypeId());
        return symbol != null ? symbol.getInPinY(this, index) : y;
    }

    public double getOutPinX(int index) {
        GateSymbol symbol = SymbolRegistry.getSymbol(node.getTypeId());
        return symbol != null ? symbol.getOutPinX(this, index) : x;
    }

    public double getOutPinY(int index) {
        GateSymbol symbol = SymbolRegistry.getSymbol(node.getTypeId());
        return symbol != null ? symbol.getOutPinY(this, index) : y;
    }

    public void draw(GraphicsContext gc, boolean isHovered, boolean isSelected, int hoveredInPin, int hoveredOutPin, VisualWire selectedWire, boolean isConnectionInvalid) {
        boolean isBodyHovered = isHovered && hoveredInPin == -1 && hoveredOutPin == -1;
        
        GateSymbol symbol = SymbolRegistry.getSymbol(node.getTypeId());
        if (symbol != null) {
            symbol.draw(gc, this, isBodyHovered, isSelected);
        } else {
            gc.setFill(Color.web("#4A90E2"));
            gc.fillRect(x, y, width, height);
            gc.strokeRect(x, y, width, height);
            if (showLabel) {
                gc.setFill(Color.WHITE);
                gc.fillText(label, x + 25, y + 30);
            }
        }

        // 입력 핀 그리기
        for (int i = 0; i < node.getInputSize(); i++) {
            boolean isPinHovered = isHovered && hoveredInPin == i;
            boolean isPinSelected = (selectedWire != null && selectedWire.to == this && selectedWire.inPin == i);
            
            if (isPinHovered && isConnectionInvalid) {
                gc.setFill(Color.RED);
            } else if (isPinSelected) {
                gc.setFill(Color.web("#00FFFF")); // 강조색 (Cyan)
            } else if (isPinHovered) {
                gc.setFill(Color.web("#FFD700"));
            } else {
                gc.setFill(Color.web("#AAAAAA"));
            }
            
            double radius = (isPinHovered || isPinSelected) ? 6 : 4;
            gc.fillOval(getInPinX(i) - radius, getInPinY(i) - radius, radius * 2, radius * 2);
        }

        // 출력 핀 그리기
        for (int i = 0; i < node.getOutputSize(); i++) {
            boolean isPinHovered = isHovered && hoveredOutPin == i;
            boolean isPinSelected = (selectedWire != null && selectedWire.from == this && selectedWire.outPin == i);

            if (isPinHovered && isConnectionInvalid) {
                gc.setFill(Color.RED);
            } else if (isPinSelected) {
                gc.setFill(Color.web("#00FFFF")); // 강조색 (Cyan)
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