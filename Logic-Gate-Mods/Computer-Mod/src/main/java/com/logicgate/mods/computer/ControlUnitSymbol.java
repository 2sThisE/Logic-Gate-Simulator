package com.logicgate.mods.computer;

import com.logicgate.editor.mod.ComponentMeta;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.rendering.symbol.AbstractGateSymbol;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

@ComponentMeta(name = "SAP-1 Control Unit", section = "SAP-1 Computer", typeId = "CONTROL_UNIT")
public class ControlUnitSymbol extends AbstractGateSymbol {
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

        int instruction = vn.node.getIn() & 0xFF;
        int step = (vn.node.getIn() >> 8) & 0x0F;
        int signals = vn.node.getOut();

        gc.setFill(Color.WHITE);
        gc.setFont(new Font("System Bold", 12));
        gc.fillText("SAP-1 CONTROL", 15, 20);
        gc.setFont(new Font("Consolas", 10));
        gc.fillText(String.format("IR %02X  OP %X  T%d", instruction, (instruction >> 4) & 0x0F, step), 15, 37);

        gc.setFont(new Font(8));
        for (int i = 0; i < 8; i++) gc.fillText("IR" + i, 5, 60 + i * 15);
        for (int i = 0; i < 4; i++) gc.fillText("T" + i, 5, 195 + i * 15);

        for (int i = 0; i < ControlUnitNode.SIGNAL_NAMES.length; i++) {
            boolean active = (signals & (1 << i)) != 0;
            gc.setFill(active ? Color.web("#7CFC98") : Color.web("#B8B8B8"));
            gc.fillText(ControlUnitNode.SIGNAL_NAMES[i], vn.width - 70, 60 + i * 15);
        }
        gc.restore();
    }

    @Override
    public double getInPinY(VisualNode vn, int index) {
        if (index < 8) return vn.y + 55 + index * 15;
        return vn.y + 190 + (index - 8) * 15;
    }

    @Override
    public double getOutPinY(VisualNode vn, int index) {
        return vn.y + 55 + index * 15;
    }

    @Override
    public String getInPinName(int index) {
        if (index < 8) return "Instruction Bit IR" + index;
        return "Step Bit T" + (index - 8);
    }

    @Override
    public String getOutPinName(int index) {
        return ControlUnitNode.SIGNAL_NAMES[index];
    }

    public int getUnitWidth() { return 24; }
    public int getUnitHeight() { return 38; }
}
