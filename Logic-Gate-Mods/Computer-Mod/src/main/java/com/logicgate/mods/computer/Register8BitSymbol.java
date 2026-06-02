package com.logicgate.mods.computer;

import com.logicgate.editor.mod.ComponentMeta;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.rendering.symbol.AbstractGateSymbol;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

@ComponentMeta(name = "8-bit Register", section = "SAP-1 Computer", typeId = "REG_8BIT")
public class Register8BitSymbol extends AbstractGateSymbol {
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

        int value = vn.node instanceof Register8BitNode reg ? reg.getStoredValue() : (vn.node.getOut() & 0xFF);
        boolean load = (vn.node.getIn() & (1 << 8)) != 0;
        boolean enable = (vn.node.getIn() & (1 << 9)) != 0;

        gc.setFill(Color.WHITE);
        gc.setFont(new Font("System Bold", 11));
        gc.fillText("8-BIT REG", 10, 18);
        gc.setFont(new Font("Consolas", 10));
        gc.fillText(String.format("VAL %02X", value), 10, 33);
        gc.setFill(load ? Color.web("#7CFC98") : Color.web("#8A8A8A"));
        gc.fillText("L", vn.width - 34, 33);
        gc.setFill(enable ? Color.web("#7CFC98") : Color.web("#8A8A8A"));
        gc.fillText("E", vn.width - 18, 33);

        gc.setFill(Color.WHITE);
        gc.setFont(new Font(8));
        for (int i = 0; i < 8; i++) gc.fillText("D" + i, 5, 52 + i * 12);
        gc.fillText("LOAD", 5, vn.height - 35);
        gc.fillText("EN", 5, vn.height - 15);
        for (int i = 0; i < 8; i++) gc.fillText("Q" + i, vn.width - 25, 52 + i * 12);
        for (int i = 0; i < 8; i++) gc.fillText("B" + i, vn.width - 25, 150 + i * 12);
        gc.restore();
    }

    @Override
    public double getInPinY(VisualNode vn, int index) {
        if (index < 8) return vn.y + 47 + index * 12;
        if (index == 8) return vn.y + vn.height - 38;
        return vn.y + vn.height - 18;
    }

    @Override
    public double getOutPinY(VisualNode vn, int index) {
        if (index < 8) return vn.y + 47 + index * 12;
        return vn.y + 145 + (index - 8) * 12;
    }

    @Override
    public String getInPinName(int index) {
        if (index < 8) return "Data Input D" + index;
        if (index == 8) return "Load Value";
        return "Bus Output Enable";
    }

    @Override
    public String getOutPinName(int index) {
        if (index < 8) return "Always-On Output Q" + index;
        return "Bus Output B" + (index - 8);
    }

    public int getUnitWidth() { return 12; }
    public int getUnitHeight() { return 25; }
}
