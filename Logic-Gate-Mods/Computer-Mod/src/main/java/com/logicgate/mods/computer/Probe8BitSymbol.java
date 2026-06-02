package com.logicgate.mods.computer;

import com.logicgate.editor.mod.ComponentMeta;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.rendering.symbol.AbstractGateSymbol;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

@ComponentMeta(name = "8-bit Debug Probe", section = "SAP-1 Debug", typeId = "SAP1_PROBE_8BIT")
public class Probe8BitSymbol extends AbstractGateSymbol {
    @Override
    public String getSvgPathData(VisualNode vn) {
        return String.format("M 0 0 H %f V %f H 0 Z", vn.width, vn.height);
    }

    @Override
    public void draw(GraphicsContext gc, VisualNode vn, boolean isHovered, boolean isSelected) {
        gc.save();
        gc.setFill(Color.web("#101820"));
        gc.fillRoundRect(0, 0, vn.width, vn.height, 6, 6);
        gc.setStroke(isSelected ? Color.web("#4DA3FF") : Color.web("#58616A"));
        gc.strokeRoundRect(0, 0, vn.width, vn.height, 6, 6);

        int value = vn.node.getIn() & 0xFF;
        gc.setFill(Color.web("#7CFC98"));
        gc.setFont(new Font("Consolas", 14));
        gc.fillText(String.format("%02X", value), 12, 24);
        gc.setFill(Color.WHITE);
        gc.setFont(new Font("Consolas", 9));
        gc.fillText(String.format("%8s", Integer.toBinaryString(value)).replace(' ', '0'), 12, 42);
        gc.restore();
    }

    @Override
    public double getInPinY(VisualNode vn, int index) {
        return vn.y + 12 + index * 10;
    }

    @Override
    public String getInPinName(int index) {
        return "Probe Bit D" + index;
    }

    public int getUnitWidth() { return 8; }
    public int getUnitHeight() { return 10; }
}
