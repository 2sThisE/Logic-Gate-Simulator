package com.logicgate.editor.rendering.symbol;

import com.logicgate.editor.model.VisualNode;
import javafx.scene.canvas.GraphicsContext;

public class NandSymbol extends AbstractGateSymbol {
    @Override
    public String getSvgPathData(VisualNode vn) {
        double bubbleR = 5.0; // 버블(원)의 반지름
        double bodyWidth = vn.width - (bubbleR * 2); // 버블 공간만큼 몸통 너비 축소
        double r = vn.height / 2;

        // 1. 몸통 그리기 (AND 모양이지만 끝이 bodyWidth에서 끝남)
        // 2. 이어서 버블(원)까지 하나의 경로로 포함 (선택사항)
        // 여기서는 몸통 경로만 반환하고, 버블은 drawExtra에서 그리는 기존 방식을 유지하면서 좌표만 최적화합니다.
        
        return String.format("M 0 0 L %f 0 Q %f 0 %f %f Q %f %f %f %f L 0 %f Z", 
            bodyWidth - r, bodyWidth, bodyWidth, r, 
            bodyWidth, vn.height, bodyWidth - r, vn.height, vn.height);
    }

    @Override
    protected void drawExtra(GraphicsContext gc, VisualNode vn) {
        drawBubble(gc, vn);
    }
}