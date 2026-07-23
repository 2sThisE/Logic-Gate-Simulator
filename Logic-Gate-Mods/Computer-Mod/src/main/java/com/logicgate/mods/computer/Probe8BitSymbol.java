package com.logicgate.mods.computer;

import com.logicgate.api.component.ComponentMeta;
import com.logicgate.api.rendering.SymbolContext;
import com.logicgate.api.rendering.AbstractGateSymbol;
import com.logicgate.api.rendering.DrawingContext;



@ComponentMeta(name = "8-bit Debug Probe", section = "SAP-1 Debug", typeId = "SAP1_PROBE_8BIT")
public class Probe8BitSymbol extends AbstractGateSymbol {
    @Override
    public String getSvgPathData(SymbolContext vn) {
        return String.format("M 0 0 H %f V %f H 0 Z", vn.width(), vn.height());
    }

    @Override
    public void draw(DrawingContext gc, SymbolContext vn, boolean isHovered, boolean isSelected) {
        gc.save();
        gc.setFill("#101820");
        gc.fillRoundRect(0, 0, vn.width(), vn.height(), 6, 6);
        gc.setStroke(isSelected ? "#4DA3FF" : "#58616A");
        gc.strokeRoundRect(0, 0, vn.width(), vn.height(), 6, 6);

        int value = vn.node().getIn() & 0xFF;
        gc.setFill("#7CFC98");
        gc.setFont("Consolas", 14);
        gc.fillText(String.format("%02X", value), 12, 24);
        gc.setFill("#FFFFFF");
        gc.setFont("Consolas", 9);
        gc.fillText(String.format("%8s", Integer.toBinaryString(value)).replace(' ', '0'), 12, 42);
        gc.restore();
    }

    @Override
    public double getInPinY(SymbolContext vn, int index) {
        return vn.y() + 12 + index * 10;
    }

    @Override
    public String getInPinName(int index) {
        return "Probe Bit D" + index;
    }

    public int getUnitWidth() { return 8; }
    public int getUnitHeight() { return 10; }
}
