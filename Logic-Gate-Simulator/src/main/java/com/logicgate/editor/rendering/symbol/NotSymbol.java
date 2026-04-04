package com.logicgate.editor.rendering.symbol;

import com.logicgate.editor.model.VisualNode;
import javafx.scene.canvas.GraphicsContext;

public class NotSymbol extends AbstractGateSymbol {
    @Override
    public String getSvgPathData(VisualNode vn) {
        return String.format("M 0 0 L %f %f L 0 %f Z", 
            vn.width - 10, vn.height * 0.5, vn.height);
    }

    @Override
    protected void drawExtra(GraphicsContext gc, VisualNode vn) {
        drawBubble(gc, vn);
    }
}