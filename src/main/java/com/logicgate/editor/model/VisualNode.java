package com.logicgate.editor.model;

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

    public VisualNode(Node node, double x, double y, String label) {
        this.node = node;
        this.x = x;
        this.y = y;
        this.label = label;
        if (node instanceof InputPin || node instanceof OutputPin) {
            width = 50;
        } else if (node instanceof Joint) {
            width = 30;
            height = 30;
        }
    }

    public boolean contains(double px, double py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    public double getInPinX(int index) {
        if (node instanceof Joint) {
            if (index == 0) return x;
            if (index == 1) return x + width;
            if (index == 2) return x + width / 2;
            if (index == 3) return x + width / 2;
        }
        return x;
    }

    public double getInPinY(int index) {
        if (node instanceof Joint) {
            if (index == 0) return y + height / 2;
            if (index == 1) return y + height / 2;
            if (index == 2) return y;
            if (index == 3) return y + height;
        }
        int max = Math.max(1, node.getInputSize());
        return y + (height / (max + 1)) * (index + 1);
    }

    public double getOutPinX(int index) {
        if (node instanceof Joint) return getInPinX(index);
        return x + width;
    }

    public double getOutPinY(int index) {
        if (node instanceof Joint) return getInPinY(index);
        int max = Math.max(1, node.getOutputSize());
        return y + (height / (max + 1)) * (index + 1);
    }

    public void draw(GraphicsContext gc, boolean isHovered, boolean isSelected, int hoveredInPin, int hoveredOutPin, VisualWire selectedWire, boolean isConnectionInvalid) {
        boolean isOn = node.getOut() > 0;
        
        boolean isBodyHovered = isHovered && hoveredInPin == -1 && hoveredOutPin == -1;
        
        if (isSelected) {
            gc.setLineWidth(4);
            gc.setStroke(Color.web("#00FFFF"));
        } else if (isBodyHovered) {
            gc.setLineWidth(4);
            gc.setStroke(Color.web("#FFD700"));
        } else {
            gc.setLineWidth(2);
            gc.setStroke(Color.WHITE);
        }

        if (node instanceof Joint) {
            gc.setFill(Color.web("#888888"));
            gc.fillOval(x, y, width, height);
            gc.strokeOval(x, y, width, height);
        } else if (node instanceof InputPin) {
            gc.setFill(isOn ? Color.web("#FF3366") : Color.web("#444444"));
            gc.fillRoundRect(x, y, width, height, 10, 10);
            gc.strokeRoundRect(x, y, width, height, 10, 10);
            gc.setFill(Color.WHITE);
            gc.fillText(isOn ? "ON" : "OFF", x + 13, y + 30);
        } else if (node instanceof OutputPin) {
            gc.setFill(isOn ? Color.web("#33FF66") : Color.web("#333333"));
            gc.fillOval(x, y, width, height);
            gc.strokeOval(x, y, width, height);
            gc.setFill(isOn ? Color.BLACK : Color.WHITE);
            gc.fillText("LED", x + 13, y + 30);
        } else {
            gc.setFill(Color.web("#4A90E2"));
            gc.fillRect(x, y, width, height);
            gc.strokeRect(x, y, width, height);
            gc.setFill(Color.WHITE);
            gc.fillText(label, x + 25, y + 30);
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