package com.logicgate.mods.computer;

import com.logicgate.api.component.ComponentMeta;
import com.logicgate.api.rendering.SymbolContext;
import com.logicgate.api.rendering.AbstractGateSymbol;
import com.logicgate.api.rendering.DrawingContext;



@ComponentMeta(name = "4-bit Step Counter", section = "SAP-1 Computer", typeId = "STEP_COUNTER")
public class StepCounterSymbol extends AbstractGateSymbol {
    @Override
    public String getSvgPathData(SymbolContext vn) {
        return String.format("M 0 0 H %f V %f H 0 Z", vn.width(), vn.height());
    }

    @Override
    public void draw(DrawingContext gc, SymbolContext vn, boolean isHovered, boolean isSelected) {
        gc.save();
        prepareFill(gc, vn, isHovered, isSelected);
        gc.fillRoundRect(0, 0, vn.width(), vn.height(), 8, 8);
        gc.strokeRoundRect(0, 0, vn.width(), vn.height(), 8, 8);

        int step = vn.node() instanceof StepCounterNode sc ? sc.getCount() : (vn.node().getOut() & 0x07);
        gc.setFill("#FFFFFF");
        gc.setFont("System Bold", 10);
        gc.fillText("STEP COUNTER", 5, 15);
        gc.setFont("Consolas", 11);
        gc.fillText("T" + step, 5, 31);

        gc.setFont("System", 9);
        gc.fillText("CLK", 5, 48);
        gc.fillText("CLR", 5, 68);
        gc.fillText("Q0", vn.width() - 20, 35);
        gc.fillText("Q1", vn.width() - 20, 50);
        gc.fillText("Q2", vn.width() - 20, 65);
        gc.fillText("Q3", vn.width() - 20, 80);
        gc.restore();
    }

    @Override
    public double getInPinY(SymbolContext vn, int index) {
        return vn.y() + 43 + index * 20;
    }

    @Override
    public double getOutPinY(SymbolContext vn, int index) {
        return vn.y() + 30 + index * 15;
    }

    @Override
    public String getInPinName(int index) {
        return index == 0 ? "Clock Input" : "Clear Step Counter";
    }

    @Override
    public String getOutPinName(int index) {
        return "Step Bit Q" + index;
    }

    public int getUnitWidth() { return 8; }
    public int getUnitHeight() { return 12; }
}
