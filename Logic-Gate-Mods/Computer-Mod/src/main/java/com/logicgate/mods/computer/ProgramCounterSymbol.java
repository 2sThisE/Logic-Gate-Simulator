package com.logicgate.mods.computer;

import com.logicgate.api.component.ComponentMeta;
import com.logicgate.api.rendering.SymbolContext;
import com.logicgate.api.rendering.AbstractGateSymbol;
import com.logicgate.api.rendering.DrawingContext;



@ComponentMeta(name = "8-bit Program Counter", section = "SAP-1 Computer", typeId = "PC_8BIT")
public class ProgramCounterSymbol extends AbstractGateSymbol {
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

        int value = vn.node() instanceof ProgramCounterNode pc ? pc.getCount() : (vn.node().getOut() & 0xFF);
        gc.setFill("#FFFFFF");
        gc.setFont("System Bold", 12);
        gc.fillText("PROGRAM COUNTER", 15, 20);
        gc.setFont("Consolas", 11);
        gc.fillText(String.format("PC %02X", value), 15, 38);

        gc.setFont("System", 9);
        for (int i = 0; i < 8; i++) gc.fillText("J" + i, 5, 60 + i * 15);
        gc.fillText("LOAD", 5, vn.height() - 80);
        gc.fillText("INC", 5, vn.height() - 60);
        gc.fillText("CLR", 5, vn.height() - 40);
        gc.fillText("EN", 5, vn.height() - 20);
        for (int i = 0; i < 8; i++) gc.fillText("A" + i, vn.width() - 25, 60 + i * 15);
        gc.restore();
    }

    @Override
    public double getInPinY(SymbolContext vn, int index) {
        if (index < 8) return vn.y() + 55 + index * 15;
        if (index == 8) return vn.y() + vn.height() - 85;
        if (index == 9) return vn.y() + vn.height() - 65;
        if (index == 10) return vn.y() + vn.height() - 45;
        return vn.y() + vn.height() - 25;
    }

    @Override
    public double getOutPinY(SymbolContext vn, int index) {
        return vn.y() + 55 + index * 15;
    }

    @Override
    public String getInPinName(int index) {
        if (index < 8) return "Jump Address J" + index;
        if (index == 8) return "Load Jump Address";
        if (index == 9) return "Increment Counter";
        if (index == 10) return "Clear Counter";
        return "Output Enable";
    }

    @Override
    public String getOutPinName(int index) {
        return "Address Output A" + index;
    }

    public int getUnitWidth() { return 14; }
    public int getUnitHeight() { return 28; }
}
