package com.logicgate.api.rendering;

public interface GateSymbol {
    double UNIT_SIZE = 10.0;

    void draw(DrawingContext graphics, SymbolContext context, boolean hovered, boolean selected);

    double getInPinX(SymbolContext context, int index);

    double getInPinY(SymbolContext context, int index);

    double getOutPinX(SymbolContext context, int index);

    double getOutPinY(SymbolContext context, int index);

    double getLabelX(SymbolContext context);

    double getLabelY(SymbolContext context);

    String getDefaultLabel();

    String getInPinName(int index);

    String getOutPinName(int index);

    default String getInPinName(SymbolContext context, int index) {
        return getInPinName(index);
    }

    default String getOutPinName(SymbolContext context, int index) {
        return getOutPinName(index);
    }

    default int getUnitWidth() {
        return 8;
    }

    default int getUnitHeight() {
        return 6;
    }
}
