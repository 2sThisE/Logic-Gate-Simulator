package com.logicgate.editor.rendering.symbol;

import com.logicgate.editor.model.VisualNode;
import javafx.scene.canvas.GraphicsContext;

public interface GateSymbol {
    /**
     * 그리드 한 칸의 픽셀 크기입니다. ✨
     */
    double UNIT_SIZE = 10.0;

    void draw(GraphicsContext gc, VisualNode vn, boolean isHovered, boolean isSelected);
    double getInPinX(VisualNode vn, int index);
    double getInPinY(VisualNode vn, int index);
    double getOutPinX(VisualNode vn, int index);
    double getOutPinY(VisualNode vn, int index);
    double getLabelX(VisualNode vn);
    double getLabelY(VisualNode vn);
    String getDefaultLabel();
    
    // 툴팁용 핀 이름 가져오기
    String getInPinName(int index);
    String getOutPinName(int index);

    /**
     * 노드의 상태에 따라 동적으로 핀 이름을 반환합니다.
     */
    default String getInPinName(VisualNode vn, int index) {
        return getInPinName(index);
    }
    
    default String getOutPinName(VisualNode vn, int index) {
        return getOutPinName(index);
    }

    /**
     * 심볼의 권장 너비와 높이를 '칸(Unit)' 단위로 반환합니다. ✨
     * 1 Unit = 10px
     */
    default int getUnitWidth() { return 8; }  // 80px
    default int getUnitHeight() { return 6; } // 60px
}