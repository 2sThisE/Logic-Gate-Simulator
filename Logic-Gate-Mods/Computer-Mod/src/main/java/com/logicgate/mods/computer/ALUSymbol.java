package com.logicgate.mods.computer;

import com.logicgate.api.component.ComponentMeta;
import com.logicgate.api.rendering.SymbolContext;
import com.logicgate.api.rendering.AbstractGateSymbol;
import com.logicgate.api.rendering.DrawingContext;



@ComponentMeta(name = "8-bit ALU (Adder/Subtractor)", section = "Computer", typeId = "ALU_8BIT")
public class ALUSymbol extends AbstractGateSymbol {
    @Override
    public String getSvgPathData(SymbolContext vn) {
        return String.format("M 0 0 L %f 40 V %f L 0 %f L 0 %f L 40 %f L 0 0", 
            vn.width(), vn.height() - 40, vn.height(), vn.height() * 0.6, vn.height() * 0.5, vn.height() * 0.4);
    }
    
    @Override
    public void draw(DrawingContext gc, SymbolContext vn, boolean isHovered, boolean isSelected) {
        gc.save();
        prepareFill(gc, vn, isHovered, isSelected);
        double[] xPoints = {0, vn.width(), vn.width(), 0, 40, 0};
        double[] yPoints = {0, 40, vn.height() - 40, vn.height(), vn.height() / 2.0, 0};
        gc.fillPolygon(xPoints, yPoints, 6);
        gc.strokePolygon(xPoints, yPoints, 6);

        gc.setFill("#FFFFFF");
        gc.setFont("System Bold", 14);
        gc.fillText("ALU", vn.width() * 0.45, vn.height() / 2.0 + 5);

        gc.setFont("System", 9);
        for(int i=0; i<8; i++) gc.fillText("A"+i, 5, 25 + i*15);
        for(int i=0; i<8; i++) gc.fillText("B"+i, 5, vn.height() - 135 + i*15);
        gc.fillText("SUB", 45, vn.height() / 2.0 - 10);
        gc.fillText("EN", 45, vn.height() / 2.0 + 10);

        for(int i=0; i<8; i++) gc.fillText("Q"+i, vn.width() - 25, vn.height() * 0.3 + i*15);
        gc.fillText("ZERO", vn.width() - 35, 35);
        gc.fillText("CARRY", vn.width() - 40, vn.height() - 35);
        gc.restore();
    }

    @Override
    public double getInPinY(SymbolContext vn, int index) {
        if (index < 8) return vn.y() + 20 + (index * 15);
        if (index < 16) return vn.y() + vn.height() - 140 + ((index - 8) * 15);
        if (index == 16) return vn.y() + vn.height() * 0.5 - 15;
        return vn.y() + vn.height() * 0.5 + 15;
    }

    @Override
    public double getOutPinY(SymbolContext vn, int index) {
        if (index < 8) return vn.y() + vn.height() * 0.3 + (index * 15);
        if (index == 8) return vn.y() + 40;
        return vn.y() + vn.height() - 40;
    }

    @Override
    public String getInPinName(int index) {
        if (index < 8) return "Operand A bit " + index;
        if (index < 16) return "Operand B bit " + (index - 8);
        if (index == 16) return "Subtract Mode (1: Sub, 0: Add)";
        return "ALU Output Enable";
    }

    @Override
    public String getOutPinName(int index) {
        if (index < 8) return "Result Q" + index;
        if (index == 8) return "Zero Flag";
        return "Carry Flag";
    }

    public int getUnitWidth() { return 18; }
    public int getUnitHeight() { return 35; }
}
