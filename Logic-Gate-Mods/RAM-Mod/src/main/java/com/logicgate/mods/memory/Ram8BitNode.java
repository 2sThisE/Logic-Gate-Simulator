package com.logicgate.mods.memory;

import com.logicgate.api.component.ComponentMeta;
import com.logicgate.api.component.Property;
import com.logicgate.api.component.Node;
import java.util.List;

@ComponentMeta(
    name = "256x8 RAM (8-bit Addressable)",
    section = "Memory",
    typeId = "RAM_256X8"
)
public class Ram8BitNode extends Node {
    private final int[] memory = new int[256];
    private boolean lastWE = false;
    private String hexData = "";

    public Ram8BitNode() {
        super(18, 8);
        this.typeId = "RAM_256X8";
    }

    @Override
    public void compute() {
        int addr = in & 0xFF;
        int dataIn = (in >> 8) & 0xFF;
        boolean we = (in & (1 << 16)) != 0;
        boolean oe = (in & (1 << 17)) != 0;

        if (we && !lastWE) {
            memory[addr] = dataIn;
        }
        lastWE = we;
        out = oe ? (memory[addr] & 0xFF) : 0;
    }

    @Override
    public List<Property<?>> getComponentProperties() {
        List<Property<?>> props = super.getComponentProperties();
        props.add(new Property<>("Hex Data (Space separated)", hexData, Property.Type.STRING, value -> {
            hexData = (String) value;
            properties.put("hexData", hexData);
            loadHexData();
        }));
        return props;
    }

    @Override
    protected void applyProperties() {
        if (properties.containsKey("hexData")) {
            hexData = properties.get("hexData");
            loadHexData();
        }
    }

    private void loadHexData() {
        if (hexData == null || hexData.isEmpty()) return;
        String[] parts = hexData.split("\\s+");
        for (int i = 0; i < Math.min(parts.length, 256); i++) {
            try {
                memory[i] = Integer.parseInt(parts[i], 16) & 0xFF;
            } catch (NumberFormatException e) {
                // Ignore invalid hex
            }
        }
    }
}
