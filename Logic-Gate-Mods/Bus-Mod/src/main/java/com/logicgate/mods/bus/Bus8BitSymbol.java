package com.logicgate.mods.bus;

import com.logicgate.api.component.ComponentMeta;
import com.logicgate.api.rendering.SymbolContext;
import com.logicgate.api.rendering.AbstractGateSymbol;

import com.logicgate.api.rendering.DrawingContext;



@ComponentMeta(
    name = "8-bit 3-Port Bus Aggregator",
    section = "Bus & Buffer",
    typeId = "BUS_AGGREGATOR_8BIT"
)
public class Bus8BitSymbol extends AbstractGateSymbol {

    @Override
    public String getSvgPathData(SymbolContext vn) {
        return String.format("M 0 0 H %f V %f H 0 Z", vn.width(), vn.height());
    }

    @Override
    public void draw(DrawingContext gc, SymbolContext vn, boolean isHovered, boolean isSelected) {
        gc.save();
        prepareFill(gc, vn, isHovered, isSelected);

        gc.setFill("#222222");
        gc.fillRoundRect(0, 0, vn.width(), vn.height(), 5, 5);
        gc.strokeRoundRect(0, 0, vn.width(), vn.height(), 5, 5);

        gc.setStroke("#FFD700", 0.5);
        gc.setLineWidth(1);
        for (int i = 0; i < 8; i++) {
            double y = (vn.height() / 33.0) * (i + 1) * 3 + 10;
            gc.strokeLine(5, y, vn.width() - 5, y);
        }

        gc.setFill("#FFFFFF");
        gc.setFont("System", 10);
        gc.fillText("PORT A", 5, 15);
        gc.fillText("PORT B", 5, vn.height() * 0.4);
        gc.fillText("PORT C", 5, vn.height() * 0.7);

        gc.restore();
    }

    @Override
    public double getInPinX(SymbolContext vn, int index) {
        return vn.x();
    }

    @Override
    public double getInPinY(SymbolContext vn, int index) {
        if (index < 8) return vn.y() + 20 + (index * 15);
        if (index < 16) return vn.y() + vn.height() * 0.4 + 10 + ((index - 8) * 15);
        return vn.y() + vn.height() * 0.7 + 10 + ((index - 16) * 15);
    }

    @Override
    public double getOutPinX(SymbolContext vn, int index) {
        return vn.x() + vn.width();
    }

    @Override
    public double getOutPinY(SymbolContext vn, int index) {
        return vn.y() + vn.height() * 0.3 + (index * 20);
    }

    @Override
    public String getInPinName(int index) {
        if (index < 8) return "Port A bit " + index;
        if (index < 16) return "Port B bit " + (index - 8);
        return "Port C bit " + (index - 16);
    }

    @Override
    public String getOutPinName(int index) {
        return "Bus Output Q" + index;
    }

    @Override
    public int getUnitWidth(){return 10;}

    @Override
    public int getUnitHeight(){return 45;}
}
