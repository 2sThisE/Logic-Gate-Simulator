package com.logicgate.mods.display;

import com.logicgate.editor.mod.ComponentMeta;
import com.logicgate.gates.Node;

@ComponentMeta(
    name = "7-Segment Decoder",
    section = "7 Segment",
    typeId = "SevenSegmentDecoder"
)
public class SevenSegmentDecoderNode extends Node {

    private static final int[] BCD_TO_SEG = {
        0x3F, // 0: a,b,c,d,e,f
        0x06, // 1: b,c
        0x5B, // 2: a,b,d,e,g
        0x4F, // 3: a,b,c,d,g
        0x66, // 4: b,c,f,g
        0x6D, // 5: a,c,d,f,g
        0x7D, // 6: a,c,d,e,f,g
        0x07, // 7: a,b,c
        0x7F, // 8: a,b,c,d,e,f,g
        0x6F, // 9: a,b,c,d,f,g
        0x77, // A
        0x7C, // b
        0x39, // C
        0x5E, // d
        0x79, // E
        0x71  // F
    };

    public SevenSegmentDecoderNode() {
        super(4, 7); // 입력 4개(BCD), 출력 7개(a-g)
        this.typeId = "SevenSegmentDecoder";
    }

    @Override
    public void compute() {
        int bcdValue = in & 0x0F;
        int segPattern = BCD_TO_SEG[bcdValue];
        
        // 출력 비트 설정 (a:bit0, b:bit1, ..., g:bit6)
        out = segPattern;
    }
}
