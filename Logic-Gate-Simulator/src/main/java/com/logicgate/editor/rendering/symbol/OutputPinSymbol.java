package com.logicgate.editor.rendering.symbol;

import com.logicgate.api.rendering.AbstractGateSymbol;
import com.logicgate.api.rendering.DrawingContext;
import com.logicgate.api.rendering.SymbolContext;




public class OutputPinSymbol extends AbstractGateSymbol {
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

        String colorHex = "#33FF66";
        if (vn.node() instanceof com.logicgate.gates.OutputPin) {
            colorHex = ((com.logicgate.gates.OutputPin) vn.node()).getOnColor();
        }

        gc.setFill(isOn ? colorHex : "#333333");
        gc.fillOval(0, 0, vn.width(), vn.height());
        gc.strokeOval(0, 0, vn.width(), vn.height());

        gc.restore();
    }

    @Override
    public String getDefaultLabel() {
        return "LED";
    }
    @Override
    public int getUnitWidth(){return 5;}
    @Override
    public int getUnitHeight(){return 5;}
}
