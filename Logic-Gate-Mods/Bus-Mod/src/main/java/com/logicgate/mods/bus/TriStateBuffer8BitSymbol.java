package com.logicgate.mods.bus;

import com.logicgate.api.component.ComponentMeta;
import com.logicgate.api.rendering.SymbolContext;
import com.logicgate.api.rendering.AbstractGateSymbol;

import com.logicgate.api.rendering.DrawingContext;


@ComponentMeta(
    name = "8-bit Tri-state Buffer",
    section = "Bus & Buffer",
    typeId = "TRI_STATE_BUFFER_8BIT"
)
public class TriStateBuffer8BitSymbol extends AbstractGateSymbol {

    @Override
    public String getSvgPathData(SymbolContext vn) {
        // 길쭉한 직각형 삼각형 모양 (버퍼 기호 8개짜리 느낌)
        return String.format("M 0 0 L %f %f L 0 %f Z", vn.width(), vn.height() / 2, vn.height());
    }

    @Override
    public void draw(DrawingContext gc, SymbolContext vn, boolean isHovered, boolean isSelected) {
        gc.save();
        prepareFill(gc, vn, isHovered, isSelected);
        
        // 배경색
        gc.setFill("#4A90E2", 0.3);
        gc.fillRoundRect(0, 0, vn.width(), vn.height(), 5, 5);
        gc.strokeRoundRect(0, 0, vn.width(), vn.height(), 5, 5);

        // 메인 버퍼 기호 (큰 삼각형 하나)
        gc.setFill("#FFFFFF", 0.2);
        gc.beginPath();
        gc.moveTo(vn.width() * 0.2, vn.height() * 0.1);
        gc.lineTo(vn.width() * 0.8, vn.height() * 0.5);
        gc.lineTo(vn.width() * 0.2, vn.height() * 0.9);
        gc.closePath();
        gc.fill();
        gc.stroke();

        gc.restore();
    }

    @Override
    public double getInPinX(SymbolContext vn, int index) {
        if (index == 8) return vn.x() + vn.width() / 2; // EN (중앙 하단)
        return vn.x(); // A0-A7 (왼쪽)
    }

    @Override
    public double getInPinY(SymbolContext vn, int index) {
        if (index == 8) return vn.y() + vn.height(); // EN
        return vn.y() + (vn.height() / 9.0) * (index + 1);
    }

    @Override
    public double getOutPinX(SymbolContext vn, int index) {
        return vn.x() + vn.width(); // Q0-Q7 (오른쪽)
    }

    @Override
    public double getOutPinY(SymbolContext vn, int index) {
        return vn.y() + (vn.height() / 9.0) * (index + 1);
    }

    @Override
    public String getInPinName(int index) {
        if (index == 8) return "Enable (EN)";
        return "Input A" + index;
    }

    @Override
    public String getOutPinName(int index) {
        return "Output Q" + index;
    }

    @Override
    public int getUnitWidth(){return 6;}
    @Override
    public int getUnitHeight(){return 18;}
}
