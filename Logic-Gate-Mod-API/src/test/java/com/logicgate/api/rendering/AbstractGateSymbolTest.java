package com.logicgate.api.rendering;

import com.logicgate.api.component.Node;

import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AbstractGateSymbolTest {
    @Test
    public void drawsThroughToolkitNeutralContext() {
        List<String> calls = new ArrayList<>();
        DrawingContext drawing = (DrawingContext) Proxy.newProxyInstance(
            DrawingContext.class.getClassLoader(),
            new Class<?>[]{ DrawingContext.class },
            (proxy, method, args) -> {
                calls.add(method.getName());
                return null;
            }
        );

        AbstractGateSymbol symbol = new AbstractGateSymbol() {
            @Override
            public String getSvgPathData(SymbolContext context) {
                return "M 0 0 L 10 10";
            }
        };

        symbol.draw(drawing, context(), false, false);

        assertEquals("save", calls.get(0));
        assertTrue(calls.contains("setFill"));
        assertTrue(calls.contains("appendSvgPath"));
        assertTrue(calls.contains("stroke"));
        assertEquals("restore", calls.get(calls.size() - 1));
    }

    @Test
    public void defaultPinsUseReadOnlySymbolContext() {
        AbstractGateSymbol symbol = new AbstractGateSymbol() {
            @Override
            public String getSvgPathData(SymbolContext context) {
                return "";
            }
        };

        SymbolContext context = context();

        assertEquals(10.0, symbol.getInPinX(context, 0), 0.001);
        assertEquals(90.0, symbol.getOutPinX(context, 0), 0.001);
        assertEquals(30.0, symbol.getInPinY(context, 0), 0.001);
    }

    private SymbolContext context() {
        Node node = new Node(2, 1) {
            @Override
            public void compute() {
            }
        };

        return new SymbolContext() {
            @Override
            public Node node() {
                return node;
            }

            @Override
            public double x() {
                return 10;
            }

            @Override
            public double y() {
                return 20;
            }

            @Override
            public double width() {
                return 80;
            }

            @Override
            public double height() {
                return 40;
            }

            @Override
            public double rotation() {
                return 0;
            }

            @Override
            public String label() {
                return "Test";
            }

            @Override
            public boolean showLabel() {
                return true;
            }
        };
    }
}
