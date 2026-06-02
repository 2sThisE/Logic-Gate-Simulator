package com.logicgate.mods.computer;

import com.logicgate.editor.mod.ComponentMeta;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.rendering.symbol.AbstractGateSymbol;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

@ComponentMeta(name = "8-bit Program Counter", section = "SAP-1 Computer", typeId = "PC_8BIT")
public class ProgramCounterSymbol extends AbstractGateSymbol {
    @Override
    public String getSvgPathData(VisualNode vn) {
        return String.format("M 0 0 H %f V %f H 0 Z", vn.width, vn.height);
    }

    @Override
    public void draw(GraphicsContext gc, VisualNode vn, boolean isHovered, boolean isSelected) {
        gc.save();
        prepareFill(gc, vn, isHovered, isSelected);
        gc.fillRoundRect(0, 0, vn.width, vn.height, 8, 8);
        gc.strokeRoundRect(0, 0, vn.width, vn.height, 8, 8);

        int value = vn.node instanceof ProgramCounterNode pc ? pc.getCount() : (vn.node.getOut() & 0xFF);
        gc.setFill(Color.WHITE);
        gc.setFont(new Font("System Bold", 12));
        gc.fillText("PROGRAM COUNTER", 15, 20);
        gc.setFont(new Font("Consolas", 11));
        gc.fillText(String.format("PC %02X", value), 15, 38);

        gc.setFont(new Font(9));
        for (int i = 0; i < 8; i++) gc.fillText("J" + i, 5, 60 + i * 15);
        gc.fillText("LOAD", 5, vn.height - 80);
        gc.fillText("INC", 5, vn.height - 60);
        gc.fillText("CLR", 5, vn.height - 40);
        gc.fillText("EN", 5, vn.height - 20);
        for (int i = 0; i < 8; i++) gc.fillText("A" + i, vn.width - 25, 60 + i * 15);
        gc.restore();
    }

    @Override
    public double getInPinY(VisualNode vn, int index) {
        if (index < 8) return vn.y + 55 + index * 15;
        if (index == 8) return vn.y + vn.height - 85;
        if (index == 9) return vn.y + vn.height - 65;
        if (index == 10) return vn.y + vn.height - 45;
        return vn.y + vn.height - 25;
    }

    @Override
    public double getOutPinY(VisualNode vn, int index) {
        return vn.y + 55 + index * 15;
    }

    @Override
    public String getInPinName(int index) {
        if (index < 8) return "Jump Address J" + index;
        if (index == 8) return "Load Jump Address";
        if (index == 9) return "Increment Counter";
        if (index == 10) return "Clear Counter";
        return "Output Enable";
    }

    @Override
    public String getOutPinName(int index) {
        return "Address Output A" + index;
    }

    public int getUnitWidth() { return 14; }
    public int getUnitHeight() { return 28; }
}
