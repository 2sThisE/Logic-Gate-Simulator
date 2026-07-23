package com.logicgate.editor.rendering;

import com.logicgate.api.rendering.DrawingContext;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Adapts the toolkit-neutral mod drawing API to JavaFX Canvas.
 */
public final class JavaFxDrawingContext implements DrawingContext {
    private final GraphicsContext graphics;

    public JavaFxDrawingContext(GraphicsContext graphics) {
        this.graphics = graphics;
    }

    @Override
    public void save() {
        graphics.save();
    }

    @Override
    public void restore() {
        graphics.restore();
    }

    @Override
    public void translate(double x, double y) {
        graphics.translate(x, y);
    }

    @Override
    public void rotate(double degrees) {
        graphics.rotate(degrees);
    }

    @Override
    public void setFill(String color) {
        graphics.setFill(Color.web(color));
    }

    @Override
    public void setFill(String color, double opacity) {
        graphics.setFill(Color.web(color, opacity));
    }

    @Override
    public void setFill(int red, int green, int blue) {
        graphics.setFill(Color.rgb(red, green, blue));
    }

    @Override
    public void setStroke(String color) {
        graphics.setStroke(Color.web(color));
    }

    @Override
    public void setStroke(String color, double opacity) {
        graphics.setStroke(Color.web(color, opacity));
    }

    @Override
    public void setLineWidth(double width) {
        graphics.setLineWidth(width);
    }

    @Override
    public void setFont(String family, double size) {
        graphics.setFont(Font.font(family, size));
    }

    @Override
    public void beginPath() {
        graphics.beginPath();
    }

    @Override
    public void closePath() {
        graphics.closePath();
    }

    @Override
    public void moveTo(double x, double y) {
        graphics.moveTo(x, y);
    }

    @Override
    public void lineTo(double x, double y) {
        graphics.lineTo(x, y);
    }

    @Override
    public void quadraticCurveTo(double controlX, double controlY, double x, double y) {
        graphics.quadraticCurveTo(controlX, controlY, x, y);
    }

    @Override
    public void appendSvgPath(String svgPath) {
        graphics.appendSVGPath(svgPath);
    }

    @Override
    public void fill() {
        graphics.fill();
    }

    @Override
    public void stroke() {
        graphics.stroke();
    }

    @Override
    public void fillRect(double x, double y, double width, double height) {
        graphics.fillRect(x, y, width, height);
    }

    @Override
    public void strokeRect(double x, double y, double width, double height) {
        graphics.strokeRect(x, y, width, height);
    }

    @Override
    public void fillRoundRect(
        double x,
        double y,
        double width,
        double height,
        double arcWidth,
        double arcHeight
    ) {
        graphics.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void strokeRoundRect(
        double x,
        double y,
        double width,
        double height,
        double arcWidth,
        double arcHeight
    ) {
        graphics.strokeRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void fillOval(double x, double y, double width, double height) {
        graphics.fillOval(x, y, width, height);
    }

    @Override
    public void strokeOval(double x, double y, double width, double height) {
        graphics.strokeOval(x, y, width, height);
    }

    @Override
    public void strokeLine(double x1, double y1, double x2, double y2) {
        graphics.strokeLine(x1, y1, x2, y2);
    }

    @Override
    public void fillPolygon(double[] xPoints, double[] yPoints, int pointCount) {
        graphics.fillPolygon(xPoints, yPoints, pointCount);
    }

    @Override
    public void strokePolygon(double[] xPoints, double[] yPoints, int pointCount) {
        graphics.strokePolygon(xPoints, yPoints, pointCount);
    }

    @Override
    public void fillText(String text, double x, double y) {
        graphics.fillText(text, x, y);
    }
}
