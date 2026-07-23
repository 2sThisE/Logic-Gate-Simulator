package com.logicgate.api.rendering;

import com.logicgate.api.component.Node;

/**
 * Read-only component state exposed to a symbol renderer.
 */
public interface SymbolContext {
    Node node();

    double x();

    double y();

    double width();

    double height();

    double rotation();

    String label();

    boolean showLabel();
}
