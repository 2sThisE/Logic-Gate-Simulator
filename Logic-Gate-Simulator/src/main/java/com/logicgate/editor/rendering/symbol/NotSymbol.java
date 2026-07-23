package com.logicgate.editor.rendering.symbol;

import com.logicgate.api.rendering.AbstractGateSymbol;
import com.logicgate.api.rendering.DrawingContext;
import com.logicgate.api.rendering.SymbolContext;


public class NotSymbol extends AbstractGateSymbol {
    @Override
    public String getSvgPathData(SymbolContext vn) {
        return String.format("M 0 0 L %f %f L 0 %f Z", 
            vn.width() - 10, vn.height() * 0.5, vn.height());
    }

    @Override
    protected void drawExtra(DrawingContext gc, SymbolContext vn) {
        drawBubble(gc, vn);
    }
}
