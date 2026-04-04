package com.logicgate.editor.rendering.symbol;

import java.util.HashMap;
import java.util.Map;

public class SymbolRegistry {
    private static final Map<String, GateSymbol> symbols = new HashMap<>();
    
    static {
        symbols.put("And", new AndSymbol());
        symbols.put("Nand", new NandSymbol());
        symbols.put("Or", new OrSymbol());
        symbols.put("Nor", new NorSymbol());
        symbols.put("Xor", new XorSymbol());
        symbols.put("Xnor", new XnorSymbol());
        symbols.put("Not", new NotSymbol());
        symbols.put("InputPin", new InputPinSymbol());
        symbols.put("OutputPin", new OutputPinSymbol());
        symbols.put("Joint", new JointSymbol());
    }
    
    public static GateSymbol getSymbol(String type) {
        return symbols.get(type);
    }

    public static void registerExternalSymbol(String type, GateSymbol symbol) {
        symbols.put(type, symbol);
    }
}