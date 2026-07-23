package com.logicgate.editor.rendering;

import com.logicgate.api.rendering.GateSymbol;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.model.VisualWire;
import com.logicgate.editor.rendering.symbol.SymbolRegistry;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * JavaFX rendering for a visual node. VisualNode itself remains editor state only.
 */
public final class NodeRenderer {
    public void draw(
        GraphicsContext graphics,
        VisualNode visualNode,
        boolean hovered,
        boolean selected,
        int hoveredInPin,
        int hoveredOutPin,
        VisualWire selectedWire,
        boolean connectionInvalid
    ) {
        boolean bodyHovered = hovered && hoveredInPin == -1 && hoveredOutPin == -1;

        graphics.save();
        graphics.translate(
            visualNode.x + visualNode.width / 2,
            visualNode.y + visualNode.height / 2
        );
        graphics.rotate(visualNode.rotation);
        graphics.translate(-visualNode.width / 2, -visualNode.height / 2);

        GateSymbol symbol = SymbolRegistry.getSymbol(visualNode.node.getTypeId());
        if (symbol != null) {
            symbol.draw(
                new JavaFxDrawingContext(graphics),
                visualNode,
                bodyHovered,
                selected
            );
        } else {
            graphics.setFill(Color.web("#4A90E2", 0.8));
            graphics.fillRect(0, 0, visualNode.width, visualNode.height);
            graphics.setStroke(
                selected
                    ? Color.web("#00FFFF")
                    : bodyHovered ? Color.web("#FFD700") : Color.WHITE
            );
            graphics.setLineWidth(selected || bodyHovered ? 4 : 2);
            graphics.strokeRect(0, 0, visualNode.width, visualNode.height);
        }

        drawLabel(graphics, visualNode, symbol);
        graphics.restore();

        drawPins(
            graphics,
            visualNode,
            hovered,
            hoveredInPin,
            hoveredOutPin,
            selectedWire,
            connectionInvalid
        );
    }

    private void drawLabel(
        GraphicsContext graphics,
        VisualNode visualNode,
        GateSymbol symbol
    ) {
        if (!visualNode.showLabel || visualNode.label == null || visualNode.label.isEmpty()) {
            return;
        }

        double labelX = symbol != null
            ? symbol.getLabelX(visualNode)
            : visualNode.width * 0.3;
        double labelY = symbol != null
            ? symbol.getLabelY(visualNode)
            : visualNode.height + 15;

        graphics.save();
        graphics.translate(labelX, labelY);
        graphics.rotate(-visualNode.rotation);
        graphics.setFill(Color.WHITE);
        graphics.setFont(Font.font("Arial", 12));
        graphics.fillText(visualNode.label, 0, 0);
        graphics.restore();
    }

    private void drawPins(
        GraphicsContext graphics,
        VisualNode visualNode,
        boolean hovered,
        int hoveredInPin,
        int hoveredOutPin,
        VisualWire selectedWire,
        boolean connectionInvalid
    ) {
        for (int index = 0; index < visualNode.node.getInputSize(); index++) {
            boolean pinHovered = hovered && hoveredInPin == index;
            boolean pinSelected =
                selectedWire != null &&
                selectedWire.to == visualNode &&
                selectedWire.inPin == index;
            setPinFill(graphics, pinHovered, pinSelected, connectionInvalid);

            double radius = pinHovered || pinSelected ? 6 : 4;
            graphics.fillOval(
                visualNode.getInPinX(index) - radius,
                visualNode.getInPinY(index) - radius,
                radius * 2,
                radius * 2
            );
        }

        for (int index = 0; index < visualNode.node.getOutputSize(); index++) {
            boolean pinHovered = hovered && hoveredOutPin == index;
            boolean pinSelected =
                selectedWire != null &&
                selectedWire.from == visualNode &&
                selectedWire.outPin == index;
            setPinFill(graphics, pinHovered, pinSelected, connectionInvalid);

            double radius = pinHovered || pinSelected ? 6 : 4;
            graphics.fillOval(
                visualNode.getOutPinX(index) - radius,
                visualNode.getOutPinY(index) - radius,
                radius * 2,
                radius * 2
            );
        }
    }

    private void setPinFill(
        GraphicsContext graphics,
        boolean hovered,
        boolean selected,
        boolean connectionInvalid
    ) {
        if (hovered && connectionInvalid) {
            graphics.setFill(Color.RED);
        } else if (selected) {
            graphics.setFill(Color.web("#00FFFF"));
        } else if (hovered) {
            graphics.setFill(Color.web("#FFD700"));
        } else {
            graphics.setFill(Color.web("#AAAAAA"));
        }
    }
}
