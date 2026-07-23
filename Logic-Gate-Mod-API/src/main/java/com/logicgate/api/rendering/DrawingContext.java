package com.logicgate.api.rendering;

/**
 * Toolkit-neutral drawing commands available to mod symbols.
 */
public interface DrawingContext {
    void save();

    void restore();

    void translate(double x, double y);

    void rotate(double degrees);

    void setFill(String color);

    void setFill(String color, double opacity);

    void setFill(int red, int green, int blue);

    void setStroke(String color);

    void setStroke(String color, double opacity);

    void setLineWidth(double width);

    void setFont(String family, double size);

    void beginPath();

    void closePath();

    void moveTo(double x, double y);

    void lineTo(double x, double y);

    void quadraticCurveTo(double controlX, double controlY, double x, double y);

    void appendSvgPath(String svgPath);

    void fill();

    void stroke();

    void fillRect(double x, double y, double width, double height);

    void strokeRect(double x, double y, double width, double height);

    void fillRoundRect(double x, double y, double width, double height, double arcWidth, double arcHeight);

    void strokeRoundRect(double x, double y, double width, double height, double arcWidth, double arcHeight);

    void fillOval(double x, double y, double width, double height);

    void strokeOval(double x, double y, double width, double height);

    void strokeLine(double x1, double y1, double x2, double y2);

    void fillPolygon(double[] xPoints, double[] yPoints, int pointCount);

    void strokePolygon(double[] xPoints, double[] yPoints, int pointCount);

    void fillText(String text, double x, double y);
}
