package com.logicgate.editor.utils;

import com.logicgate.gates.*;

public class NodeFactory {
    public static Node createNodeByType(String type) {
        return switch (type) {
            case "InputPin" -> new InputPin();
            case "OutputPin" -> new OutputPin();
            case "Joint" -> new Joint();
            case "And" -> new And();
            case "Or" -> new Or();
            case "Not" -> new Not();
            case "Xor" -> new Xor();
            case "Nor" -> new Nor();
            case "Nand" -> new Nand();
            case "Xnor" -> new Xnor();
            default -> null;
        };
    }
}