package com.logicgate.mods.arithmetic;

import com.logicgate.editor.rendering.symbol.AbstractGateSymbol;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.mod.ComponentMeta;

/**
 * 전가산기 전용 심볼 디자인입니다.
 * 정사각형 칩 모양에 입력 3개, 출력 2개를 예쁘게 배치해줄게! 🔪💕
 */
@ComponentMeta(section = "Arithmetic", name = "Full Adder Symbol", typeId = "FULL_ADDER")
public class FullAdderSymbol extends AbstractGateSymbol {

    @Override
    public String getSvgPathData(VisualNode vn) {
        // 정사각형 칩 모양
        return String.format("M 0 0 L %f 0 L %f %f L 0 %f Z", 
            vn.width, vn.width, vn.height, vn.height);
    }

    @Override
    public double getInPinX(VisualNode vn, int index) {
        return vn.x; // 왼쪽 변
    }

    @Override
    public double getInPinY(VisualNode vn, int index) {
        // 3개의 입력 (A, B, Cin)을 세로로 균등 배치
        return vn.y + (vn.height / 4.0) * (index + 1);
    }

    @Override
    public double getOutPinX(VisualNode vn, int index) {
        return vn.x + vn.width; // 오른쪽 변
    }

    @Override
    public double getOutPinY(VisualNode vn, int index) {
        // 2개의 출력 (Sum, Cout)을 세로로 균등 배치
        return vn.y + (vn.height / 3.0) * (index + 1);
    }

    @Override
    public String getDefaultLabel() {
        return "FULL ADDER";
    }

    @Override
    public String getInPinName(int index) {
        return switch (index) {
            case 0 -> "Input A";
            case 1 -> "Input B";
            case 2 -> "Carry In (Cin)";
            default -> "Input " + index;
        };
    }

    @Override
    public String getOutPinName(int index) {
        return switch (index) {
            case 0 -> "Sum (S)";
            case 1 -> "Carry Out (Cout)";
            default -> "Output " + index;
        };
    }
}