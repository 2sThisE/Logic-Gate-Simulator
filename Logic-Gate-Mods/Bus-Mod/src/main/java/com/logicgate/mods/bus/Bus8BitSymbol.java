package com.logicgate.mods.bus;

import com.logicgate.editor.mod.ComponentMeta;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.rendering.symbol.AbstractGateSymbol;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

@ComponentMeta(
    name = "8-bit 3-Port Bus Aggregator",
    section = "Bus & Buffer",
    typeId = "BUS_AGGREGATOR_8BIT"
)
public class Bus8BitSymbol extends AbstractGateSymbol {

    @Override
    public String getSvgPathData(VisualNode vn) {
        return String.format("M 0 0 H %f V %f H 0 Z", vn.width, vn.height);
    }

    @Override
    public void draw(GraphicsContext gc, VisualNode vn, boolean isHovered, boolean isSelected) {
        gc.save();
        prepareFill(gc, vn, isHovered, isSelected);
        
        // 버스 배경 (어두운 회색 바)
        gc.setFill(Color.web("#222222"));
        gc.fillRoundRect(0, 0, vn.width, vn.height, 5, 5);
        gc.strokeRoundRect(0, 0, vn.width, vn.height, 5, 5);

        // 버스 내부 장식 (금색 라인들 - 고속도로 느낌)
        gc.setStroke(Color.web("#FFD700", 0.5));
        gc.setLineWidth(1);
        for (int i = 0; i < 8; i++) {
            double y = (vn.height / 33.0) * (i + 1) * 3 + 10;
            gc.strokeLine(5, y, vn.width - 5, y);
        }

        // 라벨
        gc.setFill(Color.WHITE);
        gc.setFont(new Font(10));
        gc.fillText("PORT A", 5, 15);
        gc.fillText("PORT B", 5, vn.height * 0.4);
        gc.fillText("PORT C", 5, vn.height * 0.7);

        gc.restore();
    }

    @Override
    public double getInPinX(VisualNode vn, int index) {
        return vn.x;
    }

    @Override
    public double getInPinY(VisualNode vn, int index) {
        // 인덱스에 따라 포트별로 분산 배치 (총 24개)
        if (index < 8) return vn.y + 20 + (index * 15); // Port A
        if (index < 16) return vn.y + vn.height * 0.4 + 10 + ((index - 8) * 15); // Port B
        return vn.y + vn.height * 0.7 + 10 + ((index - 16) * 15); // Port C
    }

    @Override
    public double getOutPinX(VisualNode vn, int index) {
        return vn.x + vn.width;
    }

    @Override
    public double getOutPinY(VisualNode vn, int index) {
        return vn.y + vn.height * 0.3 + (index * 20); // 중앙 우측에 출력 8개
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
    public double getPreferredWidth() { return 100; }
    @Override
    public double getPreferredHeight() { return 450; } // 24개의 핀을 수용하기 위해 매우 길게!
}
