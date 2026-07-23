package com.logicgate.editor.rendering.symbol;

import com.logicgate.api.rendering.AbstractGateSymbol;
import com.logicgate.api.rendering.DrawingContext;
import com.logicgate.api.rendering.SymbolContext;

public class AndSymbol extends AbstractGateSymbol {
    @Override
    public String getSvgPathData(SymbolContext vn) {
        double r = vn.height() / 2;
        return String.format("M 0 0 L %f 0 Q %f 0 %f %f Q %f %f %f %f L 0 %f Z", 
            vn.width() - r, vn.width(), vn.width(), r,
            vn.width(), vn.height(), vn.width() - r, vn.height(), vn.height());
    }
}
