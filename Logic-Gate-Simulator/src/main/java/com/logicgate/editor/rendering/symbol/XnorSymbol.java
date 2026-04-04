package com.logicgate.editor.rendering.symbol;

import com.logicgate.editor.model.VisualNode;
import javafx.scene.canvas.GraphicsContext;

public class XnorSymbol extends AbstractGateSymbol {
    @Override
    public String getSvgPathData(VisualNode vn) {
        return String.format("M 0 0 Q %f %f 0 %f Q %f %f %f %f Q %f 0 0 0 Z",
            vn.width * 0.2, vn.height * 0.5, vn.height,
            vn.width * 0.5, vn.height, vn.width, vn.height * 0.5,
            vn.width * 0.5);
    }

    @Override
    protected void drawExtra(GraphicsContext gc, VisualNode vn) {
        double offset = 8;
        gc.beginPath();
        gc.moveTo(-offset, 0);
        gc.quadraticCurveTo(vn.width * 0.2 - offset, vn.height * 0.5, -offset, vn.height);
        gc.stroke();
        drawBubble(gc, vn);
    }
    @Override
    public double getInPinX(VisualNode vn, int index){
        return vn.x-1;
    }
}