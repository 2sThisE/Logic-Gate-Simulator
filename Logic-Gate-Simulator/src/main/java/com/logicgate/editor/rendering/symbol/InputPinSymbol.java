package com.logicgate.editor.rendering.symbol;

import com.logicgate.api.rendering.AbstractGateSymbol;
import com.logicgate.api.rendering.DrawingContext;
import com.logicgate.api.rendering.SymbolContext;




public class InputPinSymbol extends AbstractGateSymbol {
    @Override
    public String getSvgPathData(SymbolContext vn) {
        return "";
    }

    @Override
    public void draw(DrawingContext gc, SymbolContext vn, boolean isHovered, boolean isSelected) {
        gc.save();
        boolean isOn = vn.node().getOut() > 0;

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

        gc.setFill(isOn ? "#FF3366" : "#444444");
        gc.fillRoundRect(0, 0, vn.width(), vn.height(), 10, 10);
        gc.strokeRoundRect(0, 0, vn.width(), vn.height(), 10, 10);
        gc.setFill("#FFFFFF");
        gc.fillText(isOn ? "ON" : "OFF", 13, 30);

        gc.restore();
    }

    @Override
    public String getDefaultLabel() {
        return "Switch";
    }
    @Override
    public int getUnitWidth(){return 5;}
    @Override
    public int getUnitHeight(){return 5;}
}
