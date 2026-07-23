package com.logicgate.mods.memory;

import com.logicgate.api.component.ComponentMeta;
import com.logicgate.api.rendering.SymbolContext;
import com.logicgate.api.rendering.AbstractGateSymbol;

import com.logicgate.api.rendering.DrawingContext;



@ComponentMeta(
    name = "256x8 RAM (8-bit Addressable)",
    section = "Memory",
    typeId = "RAM_256X8"
)
public class Ram8BitSymbol extends AbstractGateSymbol {

    @Override
    public String getSvgPathData(SymbolContext vn) {
        return String.format("M 0 0 H %f V %f H 0 Z", vn.width(), vn.height());
    }

    @Override
    public void draw(DrawingContext gc, SymbolContext vn, boolean isHovered, boolean isSelected) {
        gc.save();
        prepareFill(gc, vn, isHovered, isSelected);
        
        // 칩 배경
        gc.setFill("#2B2B2B");
        gc.fillRoundRect(0, 0, vn.width(), vn.height(), 5, 5);
        gc.strokeRoundRect(0, 0, vn.width(), vn.height(), 5, 5);

        // 칩 내부 텍스트
        gc.setFill("#FFFFFF");
        gc.setFont("Consolas", 14);
        gc.fillText("RAM 256x8", vn.width() * 0.2, vn.height() * 0.15);
        
        // 데이터 표시 (주소 0번지 값 살짝 보여주기)
        gc.setFont("Consolas", 10);
        gc.fillText("ADDR: " + (vn.node().getIn() & 0xFF), 10, vn.height() * 0.4);
        gc.fillText("DOUT: " + (vn.node().getOut() & 0xFF), 10, vn.height() * 0.55);

        gc.restore();
    }

    @Override
    public double getInPinX(SymbolContext vn, int index) {
        if (index < 16) return vn.x(); // Addr, DataIn (왼쪽)
        return vn.x() + (vn.width() / 3.0) * (index - 15); // WE, OE (하단)
    }

    @Override
    public double getInPinY(SymbolContext vn, int index) {
        if (index < 16) return vn.y() + 30 + (index * 15); // Addr, DataIn
        return vn.y() + vn.height(); // WE, OE
    }

    @Override
    public double getOutPinX(SymbolContext vn, int index) {
        return vn.x() + vn.width(); // Q0-Q7 (오른쪽)
    }

    @Override
    public double getOutPinY(SymbolContext vn, int index) {
        return vn.y() + 50 + (index * 20); // Q0-Q7
    }

    @Override
    public String getInPinName(int index) {
        if (index < 8) return "Address bit A" + index;
        if (index < 16) return "Data Input bit D" + (index - 8);
        if (index == 16) return "Write Enable (WE)";
        return "Output Enable (OE)";
    }

    @Override
    public String getOutPinName(int index) {
        return "Data Output Q" + index;
    }

    @Override
    public int getUnitWidth(){return 12;}
    @Override
    public int getUnitHeight(){return 30;}
}
