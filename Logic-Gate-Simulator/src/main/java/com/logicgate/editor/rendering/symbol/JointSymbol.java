package com.logicgate.editor.rendering.symbol;

import com.logicgate.api.rendering.AbstractGateSymbol;
import com.logicgate.api.rendering.DrawingContext;
import com.logicgate.api.rendering.SymbolContext;




public class JointSymbol extends AbstractGateSymbol {
    @Override
    public String getSvgPathData(SymbolContext vn) {
        return "";
    }

    @Override
    public void draw(DrawingContext gc, SymbolContext vn, boolean isHovered, boolean isSelected) {
        gc.save();
        if (isSelected) {
            gc.setLineWidth(4);
            gc.setStroke("#00FFFF");
        } else if (isHovered) {
            gc.setLineWidth(4);
            gc.setStroke("#FFD700");
        } else {
            gc.setLineWidth(2);
            gc.setStroke("#FFFFFF");
        }

        gc.setFill("#888888");
        gc.fillOval(0, 0, vn.width(), vn.height());
        gc.strokeOval(0, 0, vn.width(), vn.height());

        gc.restore();
    }

    @Override
    public double getInPinX(SymbolContext vn, int index) {
        int count = vn.node().getInputSize();
        if (count <= 0) return vn.x();

        double angle = (2 * Math.PI / count) * index;
        double radius = vn.width() / 2;
        return (vn.x() + radius) + Math.cos(angle) * radius;
    }

    @Override
    public double getInPinY(SymbolContext vn, int index) {
        int count = vn.node().getInputSize();
        if (count <= 0) return vn.y();

        double angle = (2 * Math.PI / count) * index;
        double radius = vn.height() / 2;
        return (vn.y() + radius) + Math.sin(angle) * radius;
    }

    @Override
    public double getOutPinX(SymbolContext vn, int index) {
        return getInPinX(vn, index);
    }

    @Override
    public double getOutPinY(SymbolContext vn, int index) {
        return getInPinY(vn, index);
    }

    @Override
    public double getLabelX(SymbolContext vn) {
        return vn.width() * 0.5 - 10;
    }

    @Override
    public double getLabelY(SymbolContext vn) {
        return vn.height() + 15;
    }

    @Override
    public String getDefaultLabel() {
        return "Joint";
    }
    @Override
    public int getUnitWidth(){return 5;}
    @Override
    public int getUnitHeight(){return 5;}
}
