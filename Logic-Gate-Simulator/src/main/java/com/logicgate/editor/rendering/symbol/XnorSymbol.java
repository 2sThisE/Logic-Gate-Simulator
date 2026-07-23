package com.logicgate.editor.rendering.symbol;

import com.logicgate.api.rendering.AbstractGateSymbol;
import com.logicgate.api.rendering.DrawingContext;
import com.logicgate.api.rendering.SymbolContext;


public class XnorSymbol extends AbstractGateSymbol {
    @Override
    public String getSvgPathData(SymbolContext vn) {
        return String.format("M 0 0 Q %f %f 0 %f Q %f %f %f %f Q %f 0 0 0 Z",
            vn.width() * 0.2, vn.height() * 0.5, vn.height(),
            vn.width() * 0.5, vn.height(), vn.width(), vn.height() * 0.5,
            vn.width() * 0.5);
    }

    @Override
    protected void drawExtra(DrawingContext gc, SymbolContext vn) {
        double offset = 8;
        gc.beginPath();
        gc.moveTo(-offset, 0);
        gc.quadraticCurveTo(vn.width() * 0.2 - offset, vn.height() * 0.5, -offset, vn.height());
        gc.stroke();
        drawBubble(gc, vn);
    }
    @Override
    public double getInPinX(SymbolContext vn, int index){
        return vn.x()-1;
    }
}