package com.logicgate.mods.display;

import com.logicgate.editor.mod.ComponentMeta;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.rendering.symbol.AbstractGateSymbol;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

@ComponentMeta(
    name = "7-Segment Decoder",
    section = "7 Segment",
    typeId = "SevenSegmentDecoder"
)
public class SevenSegmentDecoderSymbol extends AbstractGateSymbol {

    @Override
    public String getSvgPathData(VisualNode vn) {
        return String.format("M 0 5 Q 0 0 5 0 H %f Q %f 0 %f 5 V %f Q %f %f %f %f H 5 Q 0 %f 0 %f Z", 
            vn.width - 5, vn.width, vn.width, vn.height - 5, vn.width, vn.height, vn.width - 5, vn.height, vn.height, vn.height - 5);
    }

    @Override
    protected void drawExtra(GraphicsContext gc, VisualNode vn) {
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Consolas", 10));
        gc.fillText("BCD to 7SEG", 10, vn.height / 2 + 5);
        
        // 입력 핀 라벨 (D, C, B, A)
        gc.setFont(javafx.scene.text.Font.font("Consolas", 8));
        gc.fillText("D", 5, getInPinY(vn, 3) - vn.y + 3);
        gc.fillText("C", 5, getInPinY(vn, 2) - vn.y + 3);
        gc.fillText("B", 5, getInPinY(vn, 1) - vn.y + 3);
        gc.fillText("A", 5, getInPinY(vn, 0) - vn.y + 3);

        // 출력 핀 라벨 (a-g)
        String[] segLabels = {"a", "b", "c", "d", "e", "f", "g"};
        for (int i = 0; i < 7; i++) {
            gc.fillText(segLabels[i], vn.width - 12, getOutPinY(vn, i) - vn.y + 3);
        }
    }

    @Override
    public double getInPinX(VisualNode vn, int index) { return vn.x; }

    @Override
    public double getInPinY(VisualNode vn, int index) {
        return vn.y + (vn.height / 5.0) * (index + 1);
    }

    @Override
    public double getOutPinX(VisualNode vn, int index) { return vn.x + vn.width; }

    @Override
    public double getOutPinY(VisualNode vn, int index) {
        return vn.y + (vn.height / 8.0) * (index + 1);
    }

    @Override
    public String getInPinName(int index) {
        return switch (index) {
            case 0 -> "Input A (1)";
            case 1 -> "Input B (2)";
            case 2 -> "Input C (4)";
            case 3 -> "Input D (8)";
            default -> "NC";
        };
    }

    @Override
    public String getOutPinName(int index) {
        char seg = (char) ('a' + index);
        return "Segment " + seg;
    }

    @Override
    public double getPreferredWidth() { return 100; }
    @Override
    public double getPreferredHeight() { return 120; }
}
