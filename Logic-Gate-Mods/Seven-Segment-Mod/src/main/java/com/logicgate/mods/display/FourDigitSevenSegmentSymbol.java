package com.logicgate.mods.display;

import java.util.WeakHashMap;

import com.logicgate.editor.mod.ComponentMeta;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.rendering.symbol.AbstractGateSymbol;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

@ComponentMeta(
    name = "Clock 7-Segment (4-Digit)",
    section = "7 Segment",
    typeId = "FourDigitSevenSegment"
)
public class FourDigitSevenSegmentSymbol extends AbstractGateSymbol {

    // 잔상(Persistence of Vision) 시뮬레이션을 위한 상태 저장
    private final WeakHashMap<VisualNode, double[][]> povState = new WeakHashMap<>();

    @Override
    public String getSvgPathData(VisualNode vn) {
        return String.format("M 0 5 Q 0 0 5 0 H %f Q %f 0 %f 5 V %f Q %f %f %f %f H 5 Q 0 %f 0 %f Z", 
            vn.width - 5, vn.width, vn.width, vn.height - 5, vn.width, vn.height, vn.width - 5, vn.height, vn.height, vn.height - 5);
    }

    @Override
    public void draw(GraphicsContext gc, VisualNode vn, boolean isHovered, boolean isSelected) {
        gc.save();
        prepareFill(gc, vn, isHovered, isSelected);
        
        gc.setFill(Color.web("#222222"));
        gc.fillRoundRect(0, 0, vn.width, vn.height, 10, 10);
        gc.strokeRoundRect(0, 0, vn.width, vn.height, 10, 10);
        
        drawExtra(gc, vn);
        
        gc.restore();
    }

    @Override
    protected void drawExtra(GraphicsContext gc, VisualNode vn) {
        int in = vn.node.getIn();
        
        // 12핀 표준 배열 (5641AS 기준 - 하드웨어 친화적)
        // 0:E, 1:D, 2:DP, 3:C, 4:G, 5:Dig4, 6:B, 7:Dig3, 8:Dig2, 9:F, 10:A, 11:Dig1
        boolean[] segs = {
            (in & (1 << 10)) != 0, // A
            (in & (1 << 6)) != 0,  // B
            (in & (1 << 3)) != 0,  // C
            (in & (1 << 1)) != 0,  // D
            (in & (1 << 0)) != 0,  // E
            (in & (1 << 9)) != 0,  // F
            (in & (1 << 4)) != 0,  // G
            (in & (1 << 2)) != 0   // DP
        };

        // Active HIGH로 동작하게 구현 (회로 시뮬레이션의 직관성을 위해)
        boolean[] digs = {
            (in & (1 << 11)) != 0, // Dig1
            (in & (1 << 8)) != 0,  // Dig2
            (in & (1 << 7)) != 0,  // Dig3
            (in & (1 << 5)) != 0   // Dig4
        };

        double[][] alphas = povState.computeIfAbsent(vn, k -> new double[4][8]);

        double decay = 0.85; // 잔상 감소율 (멀티플렉싱 시 부드럽게 보이도록)
        for (int d = 0; d < 4; d++) {
            for (int s = 0; s < 8; s++) {
                if (digs[d] && segs[s]) {
                    alphas[d][s] = 1.0;
                } else {
                    alphas[d][s] *= decay;
                }
            }
        }

        double dw = vn.width / 4.5;
        double startX = vn.width * 0.05;
        double y = vn.height * 0.15;
        double h = vn.height * 0.7;

        for (int d = 0; d < 4; d++) {
            double x = startX + d * dw;
            if (d >= 2) x += vn.width * 0.05; // 콜론 자리를 위해 띄움

            drawDigit(gc, x, y, dw * 0.8, h, alphas[d]);
        }

        // 가운데 콜론 그리기 (장식용, 약하게 켜져있는 상태 시뮬레이션)
        gc.setFill(Color.web("#3a3a3a", 0.4));
        double cx = startX + 2 * dw - vn.width * 0.01;
        gc.fillOval(cx, vn.height * 0.35, 4, 4);
        gc.fillOval(cx, vn.height * 0.65, 4, 4);
    }

    private void drawDigit(GraphicsContext gc, double x, double y, double w, double h, double[] alpha) {
        double hM = w * 0.22;
        double vM = h * 0.18;
        double th = w * 0.12;
        double sW = w - (hM * 2) - (th * 2);
        double sH = (h / 2) - vM - (th * 1.5);

        drawSeg(gc, x + hM + th, y + vM, sW, th, alpha[0]); // a
        drawSeg(gc, x + w - hM - th, y + vM + th, th, sH, alpha[1]); // b
        drawSeg(gc, x + w - hM - th, y + h/2 + th/2, th, sH, alpha[2]); // c
        drawSeg(gc, x + hM + th, y + h - vM - th, sW, th, alpha[3]); // d
        drawSeg(gc, x + hM, y + h/2 + th/2, th, sH, alpha[4]); // e
        drawSeg(gc, x + hM, y + vM + th, th, sH, alpha[5]); // f
        drawSeg(gc, x + hM + th, y + h/2 - th/2, sW, th, alpha[6]); // g
        drawSeg(gc, x + w - hM, y + h - vM - th, th, th, alpha[7]); // dp
    }

    private void drawSeg(GraphicsContext gc, double x, double y, double w, double h, double alpha) {
        if (alpha < 0.05) {
            gc.setFill(Color.web("#3a3a3a")); // 꺼진 색
        } else {
            int r = (int) (0x3a + (0xFF - 0x3a) * alpha);
            int gb = (int) (0x3a * (1 - alpha) + 0x22 * alpha);
            gc.setFill(Color.rgb(r, gb, gb)); // 켜진 색 (빨강 베이스에 알파 혼합)
        }
        gc.fillRoundRect(x, y, w, h, 2, 2);
    }

    @Override
    public double getInPinX(VisualNode vn, int index) {
        double dw = vn.width / 6.0;
        if (index >= 6) { // Top pins: 12, 11, 10, 9, 8, 7 (index 11 to 6)
            int pos = 11 - index; // 0 to 5
            return vn.x + dw * 0.5 + pos * dw;
        } else { // Bottom pins: 1, 2, 3, 4, 5, 6 (index 0 to 5)
            return vn.x + dw * 0.5 + index * dw;
        }
    }

    @Override
    public double getInPinY(VisualNode vn, int index) {
        if (index >= 6) return vn.y; // Top pins
        return vn.y + vn.height; // Bottom pins
    }

    @Override
    public String getInPinName(int index) {
        return switch (index) {
            case 0 -> "Pin 1 (E)";
            case 1 -> "Pin 2 (D)";
            case 2 -> "Pin 3 (DP)";
            case 3 -> "Pin 4 (C)";
            case 4 -> "Pin 5 (G)";
            case 5 -> "Pin 6 (Dig4)";
            case 6 -> "Pin 7 (B)";
            case 7 -> "Pin 8 (Dig3)";
            case 8 -> "Pin 9 (Dig2)";
            case 9 -> "Pin 10 (F)";
            case 10 -> "Pin 11 (A)";
            case 11 -> "Pin 12 (Dig1)";
            default -> "NC";
        };
    }

    @Override
    public double getPreferredWidth() { return 180; }
    @Override
    public double getPreferredHeight() { return 80; }
}