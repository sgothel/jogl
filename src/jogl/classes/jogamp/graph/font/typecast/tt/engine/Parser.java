/*
 * Typecast - The Font Development Environment
 *
 * Copyright (c) 2004 David Schweinsberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jogamp.graph.font.typecast.tt.engine;

import jogamp.graph.font.typecast.ot.Mnemonic;

/**
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
class Parser {

    private final short[][] instructions = new short[3][];

    /**
     * Advance the instruction pointer to the next executable opcode.
     * This will be the next byte, unless the current opcode is a push
     * instruction, in which case it will be the byte immediately beyond
     * the last data byte.
     * @param ip The current instruction pointer
     * @return The new instruction pointer
     */
    public int advanceIP(int ip) {

        // The high word specifies font, cvt, or glyph program
        int prog = ip >> 16;
        int i = ip & 0xffff;
        int dataCount;
        ip++;
        if (Mnemonic.NPUSHB == instructions[prog][i]) {
            // Next byte is the data byte count
            dataCount = instructions[prog][++i];
            ip += dataCount + 1;
        } else if (Mnemonic.NPUSHW == instructions[prog][i]) {
            // Next byte is the data word count
            dataCount = instructions[prog][++i];
            ip += dataCount*2 + 1;
        } else if (Mnemonic.PUSHB == (instructions[prog][i] & 0xf8)) {
            dataCount = (short)((instructions[prog][i] & 0x07) + 1);
            ip += dataCount;
        } else if (Mnemonic.PUSHW == (instructions[prog][i] & 0xf8)) {
            dataCount = (short)((instructions[prog][i] & 0x07) + 1);
            ip += dataCount*2;
        }
        return ip;
    }

    public int getISLength(int prog) {
        return instructions[prog].length;
    }

    public short getOpcode(int ip) {
        return instructions[ip >> 16][ip & 0xffff];
    }

    private short getPushCount(int ip) {
        short instr = instructions[ip >> 16][ip & 0xffff];
        if ((Mnemonic.NPUSHB == instr) || (Mnemonic.NPUSHW == instr)) {
            return instructions[ip >> 16][(ip & 0xffff) + 1];
        } else if ((Mnemonic.PUSHB == (instr & 0xf8)) || (Mnemonic.PUSHW == (instr & 0xf8))) {
            return (short)((instr & 0x07) + 1);
        }
        return 0;
    }

    public int[] getPushData(int ip) {
        int count = getPushCount(ip);
        int[] data = new int[count];
        int prog = ip >> 16;
        int i = ip & 0xffff;
        short instr = instructions[prog][i];
        if (Mnemonic.NPUSHB == instr) {
            for (int j = 0; j < count; j++) {
                data[j] = instructions[prog][i + j + 2];
            }
        } else if (Mnemonic.PUSHB == (instr & 0xf8)) {
            for (int j = 0; j < count; j++) {
                data[j] = instructions[prog][i + j + 1];
            }
        } else if (Mnemonic.NPUSHW == instr) {
            for (int j = 0; j < count; j++) {
                data[j] = (instructions[prog][i + j*2 + 2] << 8) | instructions[prog][i + j*2 + 3];
            }
        } else if (Mnemonic.PUSHW == (instr & 0xf8)) {
            for (int j = 0; j < count; j++) {
                data[j] = (instructions[prog][i + j*2 + 1] << 8) | instructions[prog][i + j*2 + 2];
            }
        }
        return data;
    }

    public int handleElse(int ip) {
        while (instructions[ip >> 16][ip & 0xffff] != Mnemonic.EIF) {
            ip = advanceIP(ip);
        }
        return ip;
    }

    public int handleIf(boolean test, int ip) {
        if (!test) {
            // The TrueType spec says that we merely jump to the *next* ELSE or EIF
            // instruction in the instruction stream.  So therefore no nesting!
            // Looking at actual code, IF-ELSE-EIF can be nested!
            while ((instructions[ip >> 16][ip & 0xffff] != Mnemonic.ELSE)
                    && (instructions[ip >> 16][ip & 0xffff] != Mnemonic.EIF)) {
                ip = advanceIP(ip);
            }
        }
        return ip;
    }

    /**
     * This program is run everytime we scale the font
     */
    public void setCvtProgram(short[] program) {
        instructions[1] = program;
    }

    /**
     * This program is only run once
     */
    public void setFontProgram(short[] program) {
        instructions[0] = program;
    }

    /**
     * This program is run everytime we scale the glyph
     */
    public void setGlyphProgram(short[] program) {
        instructions[2] = program;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        int ip = 0;
        while (ip < instructions[0].length) {
            sb.append(Mnemonic.getMnemonic(getOpcode(ip)));
            if (getPushCount(ip) > 0) {
                int[] data = getPushData(ip);
                for (int datum : data) sb.append(" ").append(datum);
            }
            sb.append("\n");
            ip = advanceIP(ip);
        }
        sb.append("\n");
        ip = 0x10000;
        while (ip < (0x10000 | instructions[1].length)) {
            sb.append(Mnemonic.getMnemonic(getOpcode(ip)));
            if(getPushCount(ip) > 0) {
                int[] data = getPushData(ip);
                for (int datum : data) {
                    sb.append(" ").append(datum);
                }
            }
            sb.append("\n");
            ip = advanceIP(ip);
        }
        sb.append("\n");
        ip = 0x20000;
        while (ip < (0x20000 | instructions[2].length)) {
            sb.append(Mnemonic.getMnemonic(getOpcode(ip)));
            if (getPushCount(ip) > 0) {
                int[] data = getPushData(ip);
                for (int datum : data) {
                    sb.append(" ").append(datum);
                }
            }
            sb.append("\n");
            ip = advanceIP(ip);
        }
        return sb.toString();
    }
}
