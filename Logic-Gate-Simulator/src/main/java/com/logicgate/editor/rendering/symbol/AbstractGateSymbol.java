package com.logicgate.editor.rendering.symbol;

import com.logicgate.editor.model.VisualNode;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public abstract class AbstractGateSymbol implements GateSymbol {
    
    public abstract String getSvgPathData(VisualNode vn);

    @Override
    public void draw(GraphicsContext gc, VisualNode vn, boolean isHovered, boolean isSelected) {
        gc.save();
        prepareFill(gc, vn, isHovered, isSelected);
        
        gc.beginPath();
        String path = getSvgPathData(vn);
        if (path != null && !path.isEmpty()) {
            gc.appendSVGPath(path);
        }
        // gc.fill();
        gc.stroke();
        
        drawExtra(gc, vn);
        // drawLabel(gc, vn); // VisualNode에서 통합 관리하도록 변경 ✨
        
        gc.restore();
    }

    protected void drawExtra(GraphicsContext gc, VisualNode vn) {
    }

    protected void drawBubble(GraphicsContext gc, VisualNode vn) {
        double bSize = 10;
        gc.setLineWidth(2);
        gc.setFill(Color.web("#2B2B2B"));
        gc.fillOval(vn.width - bSize, vn.height * 0.5 - bSize / 2, bSize, bSize);
        gc.strokeOval(vn.width - bSize, vn.height * 0.5 - bSize / 2, bSize, bSize);
    }

    protected void drawLabel(GraphicsContext gc, VisualNode vn) {
        if (vn.showLabel) {
            gc.save();
            // 라벨 위치로 이동 후 부품 회전의 반대 방향으로 회전 적용 ✨
            gc.translate(getLabelX(vn), getLabelY(vn));
            gc.rotate(-vn.rotation);
            
            gc.setFill(Color.WHITE);
            gc.fillText(vn.label, 0, 0); // 이미 이동했으므로 0,0에 그림
            gc.restore();
        }
    }
    
    protected void prepareFill(GraphicsContext gc, VisualNode vn, boolean isHovered, boolean isSelected) {
        gc.setFill(Color.web("#4A90E2", 0.8));
        if (isSelected) {
            gc.setLineWidth(4);
            gc.setStroke(Color.web("#00FFFF"));
        } else if (isHovered) {
            gc.setLineWidth(4);
            gc.setStroke(Color.web("#FFD700"));
        } else {
            gc.setLineWidth(2);
            gc.setStroke(Color.WHITE);
        }
        // gc.translate(vn.x, vn.y); // VisualNode에서 이미 처리함 ✨
    }

    @Override
    public double getInPinX(VisualNode vn, int index) {
        return vn.x;
    }

    @Override
    public double getInPinY(VisualNode vn, int index) {
        int max = Math.max(1, vn.node.getInputSize());
        return vn.y + (vn.height / (max + 1)) * (index + 1);
    }

    @Override
    public double getOutPinX(VisualNode vn, int index) {
        return vn.x + vn.width;
    }

    @Override
    public double getOutPinY(VisualNode vn, int index) {
        int max = Math.max(1, vn.node.getOutputSize());
        return vn.y + (vn.height / (max + 1)) * (index + 1);
    }

    @Override
    public double getLabelX(VisualNode vn) {
        return vn.width * 0.3;
    }

    @Override
    public double getLabelY(VisualNode vn) {
        return vn.height + 15;
    }

    @Override
    public String getDefaultLabel() {
        return this.getClass().getSimpleName().replace("Symbol", "");
    }

    @Override
    public String getInPinName(int index) {
        return "Input " + index;
    }

    @Override
    public String getOutPinName(int index) {
        return "Output " + index;
    }

    @Override
    public double getPreferredWidth() {
        return 80;
    }

    @Override
    public double getPreferredHeight() {
        return 50;
    }
}