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
        // gc.translate(vn.x, vn.y); // VisualNode에서 이미 처리함 ✨
        
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
        
        gc.restore();
    }

    @Override
    public double getInPinX(VisualNode vn, int index) {
        int count = vn.node.getInputSize();
        if (count <= 0) return vn.x;
        
        // 원형 배치: 각도를 계산하여 핀 위치 결정 💖
        double angle = (2 * Math.PI / count) * index;
        double radius = vn.width / 2;
        return (vn.x + radius) + Math.cos(angle) * radius;
    }

    @Override
    public double getInPinY(VisualNode vn, int index) {
        int count = vn.node.getInputSize();
        if (count <= 0) return vn.y;
        
        double angle = (2 * Math.PI / count) * index;
        double radius = vn.height / 2;
        return (vn.y + radius) + Math.sin(angle) * radius;
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