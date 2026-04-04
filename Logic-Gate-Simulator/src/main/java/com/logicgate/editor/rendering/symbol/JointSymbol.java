package com.logicgate.editor.rendering.symbol;

import com.logicgate.editor.model.VisualNode;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class JointSymbol extends AbstractGateSymbol {
    @Override
    public String getSvgPathData(VisualNode vn) {
        return "";
    }

    @Override
    public void draw(GraphicsContext gc, VisualNode vn, boolean isHovered, boolean isSelected) {
        gc.save();
        gc.translate(vn.x, vn.y);
        
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

        gc.setFill(Color.web("#888888"));
        gc.fillOval(0, 0, vn.width, vn.height);
        gc.strokeOval(0, 0, vn.width, vn.height);
        
        drawLabel(gc, vn);
        gc.restore();
    }

    @Override
    public double getInPinX(VisualNode vn, int index) {
        if (index == 0) return vn.x;
        if (index == 1) return vn.x + vn.width;
        if (index == 2) return vn.x + vn.width / 2;
        if (index == 3) return vn.x + vn.width / 2;
        return vn.x;
    }

    @Override
    public double getInPinY(VisualNode vn, int index) {
        if (index == 0) return vn.y + vn.height / 2;
        if (index == 1) return vn.y + vn.height / 2;
        if (index == 2) return vn.y;
        if (index == 3) return vn.y + vn.height;
        return vn.y;
    }

    @Override
    public double getOutPinX(VisualNode vn, int index) {
        return getInPinX(vn, index);
    }

    @Override
    public double getOutPinY(VisualNode vn, int index) {
        return getInPinY(vn, index);
    }

    @Override
    public double getLabelX(VisualNode vn) {
        return vn.width * 0.5 - 10;
    }

    @Override
    public double getLabelY(VisualNode vn) {
        return vn.height + 15;
    }

    @Override
    public String getDefaultLabel() {
        return "Joint";
    }
    @Override
    public double getPreferredWidth(){return 50;}
    @Override
    public double getPreferredHeight(){return 50;}
}