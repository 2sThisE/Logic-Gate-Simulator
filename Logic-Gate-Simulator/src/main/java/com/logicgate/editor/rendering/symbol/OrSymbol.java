package com.logicgate.editor.rendering.symbol;

import com.logicgate.api.rendering.AbstractGateSymbol;
import com.logicgate.api.rendering.DrawingContext;
import com.logicgate.api.rendering.SymbolContext;

public class OrSymbol extends AbstractGateSymbol {
    @Override
    public String getSvgPathData(SymbolContext vn) {
        return String.format("M 0 0 Q %f %f 0 %f Q %f %f %f %f Q %f 0 0 0 Z",
            vn.width() * 0.2, vn.height() * 0.5, vn.height(),
            vn.width() * 0.5, vn.height(), vn.width(), vn.height() * 0.5,
            vn.width() * 0.5);
    }
    @Override
    public double getInPinX(SymbolContext vn, int index){
        return vn.x() + (vn.width()*0.09);
    }
    
}
