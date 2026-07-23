package com.logicgate.api.rendering;

public abstract class AbstractGateSymbol implements GateSymbol {
    public abstract String getSvgPathData(SymbolContext context);

    @Override
    public void draw(
        DrawingContext graphics,
        SymbolContext context,
        boolean hovered,
        boolean selected
    ) {
        graphics.save();
        prepareFill(graphics, context, hovered, selected);

        graphics.beginPath();
        String path = getSvgPathData(context);
        if (path != null && !path.isEmpty()) {
            graphics.appendSvgPath(path);
        }
        graphics.stroke();

        drawExtra(graphics, context);
        graphics.restore();
    }

    protected void drawExtra(DrawingContext graphics, SymbolContext context) {
    }

    protected void drawBubble(DrawingContext graphics, SymbolContext context) {
        double bubbleSize = 10;
        graphics.setLineWidth(2);
        graphics.setFill("#2B2B2B");
        graphics.fillOval(
            context.width() - bubbleSize,
            context.height() * 0.5 - bubbleSize / 2,
            bubbleSize,
            bubbleSize
        );
        graphics.strokeOval(
            context.width() - bubbleSize,
            context.height() * 0.5 - bubbleSize / 2,
            bubbleSize,
            bubbleSize
        );
    }

    protected void drawLabel(DrawingContext graphics, SymbolContext context) {
        if (context.showLabel()) {
            graphics.save();
            graphics.translate(getLabelX(context), getLabelY(context));
            graphics.rotate(-context.rotation());
            graphics.setFill("#FFFFFF");
            graphics.fillText(context.label(), 0, 0);
            graphics.restore();
        }
    }

    protected void prepareFill(
        DrawingContext graphics,
        SymbolContext context,
        boolean hovered,
        boolean selected
    ) {
        graphics.setFill("#4A90E2", 0.8);
        if (selected) {
            graphics.setLineWidth(4);
            graphics.setStroke("#00FFFF");
        } else if (hovered) {
            graphics.setLineWidth(4);
            graphics.setStroke("#FFD700");
        } else {
            graphics.setLineWidth(2);
            graphics.setStroke("#FFFFFF");
        }
    }

    @Override
    public double getInPinX(SymbolContext context, int index) {
        return context.x();
    }

    @Override
    public double getInPinY(SymbolContext context, int index) {
        int count = context.node().getInputSize();
        double centerY = context.y() + context.height() / 2;
        double spacing = 20.0;
        return centerY + (index - (count - 1) / 2.0) * spacing;
    }

    @Override
    public double getOutPinX(SymbolContext context, int index) {
        return context.x() + context.width();
    }

    @Override
    public double getOutPinY(SymbolContext context, int index) {
        int count = context.node().getOutputSize();
        double centerY = context.y() + context.height() / 2;
        double spacing = 20.0;
        return centerY + (index - (count - 1) / 2.0) * spacing;
    }

    @Override
    public double getLabelX(SymbolContext context) {
        return context.width() * 0.3;
    }

    @Override
    public double getLabelY(SymbolContext context) {
        return context.height() + 15;
    }

    @Override
    public String getDefaultLabel() {
        return getClass().getSimpleName().replace("Symbol", "");
    }

    @Override
    public String getInPinName(int index) {
        return "Input " + index;
    }

    @Override
    public String getOutPinName(int index) {
        return "Output " + index;
    }
}
