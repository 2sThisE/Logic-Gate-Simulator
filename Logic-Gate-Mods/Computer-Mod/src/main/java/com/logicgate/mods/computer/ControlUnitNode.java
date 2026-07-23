package com.logicgate.mods.computer;

import com.logicgate.api.component.ComponentMeta;
import com.logicgate.api.component.Node;

@ComponentMeta(name = "SAP-1 Control Unit", section = "SAP-1 Computer", typeId = "CONTROL_UNIT")
public class ControlUnitNode extends Node {
    public static final String[] SIGNAL_NAMES = {
        "PC_OUT", "MAR_LOAD", "RAM_OUT", "IR_LOAD", "PC_INC", "IR_ADDR_OUT", "ACC_LOAD", "B_LOAD",
        "ALU_OUT", "PC_LOAD", "OUT_LOAD", "ACC_OUT", "ALU_SUB", "STEP_RESET", "SPARE14", "HALT"
    };

    public ControlUnitNode() {
        super(12, 16); // IR0-IR7, STEP0-STEP3 / control signals
        this.typeId = "CONTROL_UNIT";
    }

    @Override
    public void compute() {
        int instruction = in & 0xFF;
        int step = (in >> 8) & 0x0F;
        int opcode = (instruction >> 4) & 0x0F;
        int signals = 0;

        if (step == 0) {
            signals |= bit(0); // PC_OUT
        } else if (step == 1) {
            signals |= bit(0); // PC_OUT
            signals |= bit(1); // MAR_LOAD
        } else if (step == 2) {
            signals |= bit(2); // RAM_OUT
        } else if (step == 3) {
            signals |= bit(2); // RAM_OUT
            signals |= bit(3); // IR_LOAD
            signals |= bit(4); // PC_INC
        } else {
            switch (opcode) {
                case 0x1 -> { // LDA addr
                    if (step == 4) signals |= bit(5);
                    if (step == 5) signals |= bit(5) | bit(1);
                    if (step == 6) signals |= bit(2);
                    if (step == 7) signals |= bit(2) | bit(6) | bit(13);
                }
                case 0x2 -> { // ADD addr
                    if (step == 4) signals |= bit(5);
                    if (step == 5) signals |= bit(5) | bit(1);
                    if (step == 6) signals |= bit(2);
                    if (step == 7) signals |= bit(2) | bit(7);
                    if (step == 8) signals |= bit(8);
                    if (step == 9) signals |= bit(8) | bit(6) | bit(13);
                }
                case 0x3 -> { // SUB addr
                    if (step == 4) signals |= bit(5);
                    if (step == 5) signals |= bit(5) | bit(1);
                    if (step == 6) signals |= bit(2);
                    if (step == 7) signals |= bit(2) | bit(7);
                    if (step == 8) signals |= bit(12) | bit(8);
                    if (step == 9) signals |= bit(12) | bit(8) | bit(6) | bit(13);
                }
                case 0x5 -> { // LDI value
                    if (step == 4) signals |= bit(5);
                    if (step == 5) signals |= bit(5) | bit(6) | bit(13);
                }
                case 0x6 -> { // JMP addr
                    if (step == 4) signals |= bit(5);
                    if (step == 5) signals |= bit(5) | bit(9) | bit(13);
                }
                case 0xE -> { // OUT
                    if (step == 4) signals |= bit(11);
                    if (step == 5) signals |= bit(11) | bit(10) | bit(13);
                }
                case 0xF -> {
                    if (step >= 4) signals |= bit(15);
                }
                default -> { }
            }
        }

        out = signals;
    }

    private int bit(int index) {
        return 1 << index;
    }
}
