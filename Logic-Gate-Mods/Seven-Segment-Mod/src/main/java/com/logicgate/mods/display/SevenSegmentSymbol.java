package com.logicgate.mods.display;

import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.rendering.symbol.AbstractGateSymbol;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class SevenSegmentSymbol extends AbstractGateSymbol {

    @Override
    public String getSvgPathData(VisualNode vn) {
        return String.format("M 0 5 Q 0 0 5 0 H %f Q %f 0 %f 5 V %f Q %f %f %f %f H 5 Q 0 %f 0 %f Z", 
            vn.width - 5, vn.width, vn.width, vn.height - 5, vn.width, vn.height, vn.width - 5, vn.height, vn.height, vn.height - 5);
    }

    @Override
    public void draw(GraphicsContext gc, VisualNode vn, boolean isHovered, boolean isSelected) {
        gc.save();
        prepareFill(gc, vn, isHovered, isSelected);
        
        // 디스플레이 배경
        gc.setFill(Color.web("#3c3c3c"));
        gc.fillRoundRect(0, 0, vn.width, vn.height, 8, 8);
        gc.strokeRoundRect(0, 0, vn.width, vn.height, 8, 8);
        
        drawExtra(gc, vn);
        
        // 핀 라벨 그리기 (G, F, A, B 상단 / E, D, C, DP 하단)
        
        gc.restore();
    }


    @Override
    protected void drawExtra(GraphicsContext gc, VisualNode vn) {
        int in = vn.node.getIn();
        Color onColor = Color.web("#FF2222");
        Color offColor = Color.web("#696969");

        double w = vn.width;
        double h = vn.height;
        double hM = w * 0.22;
        double vM = h * 0.18;
        double th = w * 0.1;
        double sW = w - (hM * 2) - (th * 2);
        double sH = (h / 2) - vM - (th * 1.5);

        // A: Bit 0
        drawSeg(gc, hM + th, vM, sW, th, (in & (1 << 0)) != 0 ? onColor : offColor);
        // B: Bit 1
        drawSeg(gc, w - hM - th, vM + th, th, sH, (in & (1 << 1)) != 0 ? onColor : offColor);
        // C: Bit 2
        drawSeg(gc, w - hM - th, h/2 + th/2, th, sH, (in & (1 << 2)) != 0 ? onColor : offColor);
        // D: Bit 3
        drawSeg(gc, hM + th, h - vM - th, sW, th, (in & (1 << 3)) != 0 ? onColor : offColor);
        // E: Bit 4
        drawSeg(gc, hM, h/2 + th/2, th, sH, (in & (1 << 4)) != 0 ? onColor : offColor);
        // F: Bit 5
        drawSeg(gc, hM, vM + th, th, sH, (in & (1 << 5)) != 0 ? onColor : offColor);
        // G: Bit 6
        drawSeg(gc, hM + th, h/2 - th/2, sW, th, (in & (1 << 6)) != 0 ? onColor : offColor);
        
        // DP (Dot): Bit 7
        boolean dpOn = (in & (1 << 7)) != 0;
        gc.setFill(dpOn ? onColor : offColor);
        gc.fillOval(w - hM + 2, h - vM - th, th, th);
    }

    private void drawSeg(GraphicsContext gc, double x, double y, double w, double h, Color color) {
        gc.setFill(color);
        gc.fillRoundRect(x, y, w, h, 2, 2);
    }

    // --- 핀 배치 오버라이드 (상단/하단 배치) ---

    @Override
    public double getInPinX(VisualNode vn, int index) {
        double w = vn.width;
        return switch (index) {
            case 6 -> vn.x + w * 0.15; // G
            case 5 -> vn.x + w * 0.35; // F
            case 0 -> vn.x + w * 0.65; // A
            case 1 -> vn.x + w * 0.85; // B
            case 4 -> vn.x + w * 0.15; // E
            case 3 -> vn.x + w * 0.35; // D
            case 2 -> vn.x + w * 0.65; // C
            case 7 -> vn.x + w * 0.85; // DP
            default -> vn.x;
        };
    }

    @Override
    public double getInPinY(VisualNode vn, int index) {
        // 상단 핀들 (0, 1, 5, 6) - 위쪽 경계에서 1px 안으로
        if (index == 0 || index == 1 || index == 5 || index == 6) return vn.y;
        // 하단 핀들 (2, 3, 4, 7) - 아래쪽 경계에서 1px 안으로
        return vn.y + vn.height;
    }

    @Override
    public String getInPinName(int index) {
        return switch (index) {
            case 0 -> "Input A"; case 1 -> "Input B"; case 2 -> "Input C"; case 3 -> "Input D";
            case 4 -> "Input E"; case 5 -> "Input F"; case 6 -> "Input G"; case 7 -> "Input DP";
            default -> "NC";
        };
    }

    @Override
    public double getPreferredWidth() { return 70; }
    @Override
    public double getPreferredHeight() { return 110; }
}
