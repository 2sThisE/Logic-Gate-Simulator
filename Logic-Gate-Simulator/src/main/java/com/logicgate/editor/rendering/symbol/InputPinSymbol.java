package com.logicgate.editor.rendering.symbol;

import com.logicgate.editor.model.VisualNode;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class InputPinSymbol extends AbstractGateSymbol {
    @Override
    public String getSvgPathData(VisualNode vn) {
        return "";
    }

    @Override
    public void draw(GraphicsContext gc, VisualNode vn, boolean isHovered, boolean isSelected) {
        gc.save();
        gc.translate(vn.x, vn.y);
        boolean isOn = vn.node.getOut() > 0;
        
        if (isSelected) {
            gc.setLineWidth(4);
            gc.setStroke(Color.web("#00FFFF"));
        } else if (isHovered) {
            gc.setLineWidth(4);
            gc.setStroke(Color.web("#FFD700"));
        } else {
            gc.setLineWidth(2);
            gc.setStroke(Color.WHITE);
        }

        gc.setFill(isOn ? Color.web("#FF3366") : Color.web("#444444"));
        gc.fillRoundRect(0, 0, vn.width, vn.height, 10, 10);
        gc.strokeRoundRect(0, 0, vn.width, vn.height, 10, 10);
        gc.setFill(Color.WHITE);
        gc.fillText(isOn ? "ON" : "OFF", 13, 30);
        
        drawLabel(gc, vn);
        gc.restore();
    }

    @Override
    public String getDefaultLabel() {
        return "Switch";
    }
    @Override
    public double getPreferredWidth(){return 50;}
    @Override
    public double getPreferredHeight(){return 50;}
}