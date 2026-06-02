package com.logicgate.mods.computer;

import com.logicgate.editor.mod.ComponentMeta;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.rendering.symbol.AbstractGateSymbol;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

@ComponentMeta(name = "4-bit Step Counter", section = "SAP-1 Computer", typeId = "STEP_COUNTER")
public class StepCounterSymbol extends AbstractGateSymbol {
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

        int step = vn.node instanceof StepCounterNode sc ? sc.getCount() : (vn.node.getOut() & 0x07);
        gc.setFill(Color.WHITE);
        gc.setFont(new Font("System Bold", 10));
        gc.fillText("STEP COUNTER", 5, 15);
        gc.setFont(new Font("Consolas", 11));
        gc.fillText("T" + step, 5, 31);

        gc.setFont(new Font(9));
        gc.fillText("CLK", 5, 48);
        gc.fillText("CLR", 5, 68);
        gc.fillText("Q0", vn.width - 20, 35);
        gc.fillText("Q1", vn.width - 20, 50);
        gc.fillText("Q2", vn.width - 20, 65);
        gc.fillText("Q3", vn.width - 20, 80);
        gc.restore();
    }

    @Override
    public double getInPinY(VisualNode vn, int index) {
        return vn.y + 43 + index * 20;
    }

    @Override
    public double getOutPinY(VisualNode vn, int index) {
        return vn.y + 30 + index * 15;
    }

    @Override
    public String getInPinName(int index) {
        return index == 0 ? "Clock Input" : "Clear Step Counter";
    }

    @Override
    public String getOutPinName(int index) {
        return "Step Bit Q" + index;
    }

    public int getUnitWidth() { return 8; }
    public int getUnitHeight() { return 12; }
}
