/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- * 
 * This software is published under the terms of the Apache Software License * 
 * version 1.1, a copy of which has been included with this distribution in  * 
 * the LICENSE file.                                                         * 
 *****************************************************************************/

package jogamp.graph.font.typecast.ot;

/**
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 * @version $Id: Disassembler.java,v 1.1.1.1 2004-12-05 23:14:25 davidsch Exp $
 */
public class Disassembler {

    /**
     * Advance the instruction pointer to the next executable opcode.
     * This will be the next byte, unless the current opcode is a push
     * instruction, in which case it will be the byte immediately beyond
     * the last data byte.
     * @param ip The current instruction pointer
     * @return The new instruction pointer
     */
    public static int advanceIP(short[] instructions, int ip) {

        // The high word specifies font, cvt, or glyph program
        int i = ip & 0xffff;
        int dataCount;
        ip++;
        if (Mnemonic.NPUSHB == instructions[i]) {
            // Next byte is the data byte count
            dataCount = instructions[++i];
            ip += dataCount + 1;
        } else if (Mnemonic.NPUSHW == instructions[i]) {
            // Next byte is the data word count
            dataCount = instructions[++i];
            ip += dataCount*2 + 1;
        } else if (Mnemonic.PUSHB == (instructions[i] & 0xf8)) {
            dataCount = (short)((instructions[i] & 0x07) + 1);
            ip += dataCount;
        } else if (Mnemonic.PUSHW == (instructions[i] & 0xf8)) {
            dataCount = (short)((instructions[i] & 0x07) + 1);
            ip += dataCount*2;
        }
        return ip;
    }

    public static short getPushCount(short[] instructions, int ip) {
        short instr = instructions[ip & 0xffff];
        if ((Mnemonic.NPUSHB == instr) || (Mnemonic.NPUSHW == instr)) {
            return instructions[(ip & 0xffff) + 1];
        } else if ((Mnemonic.PUSHB == (instr & 0xf8)) || (Mnemonic.PUSHW == (instr & 0xf8))) {
            return (short)((instr & 0x07) + 1);
        }
        return 0;
    }

    public static int[] getPushData(short[] instructions, int ip) {
        int count = getPushCount(instructions, ip);
        int[] data = new int[count];
        int i = ip & 0xffff;
        short instr = instructions[i];
        if (Mnemonic.NPUSHB == instr) {
            for (int j = 0; j < count; j++) {
                data[j] = instructions[i + j + 2];
            }
        } else if (Mnemonic.PUSHB == (instr & 0xf8)) {
            for (int j = 0; j < count; j++) {
                data[j] = instructions[i + j + 1];
            }
        } else if (Mnemonic.NPUSHW == instr) {
            for (int j = 0; j < count; j++) {
                data[j] = (instructions[i + j*2 + 2] << 8) | instructions[i + j*2 + 3];
            }
        } else if (Mnemonic.PUSHW == (instr & 0xf8)) {
            for (int j = 0; j < count; j++) {
                data[j] = (instructions[i + j*2 + 1] << 8) | instructions[i + j*2 + 2];
            }
        }
        return data;
    }

     public static String disassemble(short[] instructions, int leadingSpaces) {
        StringBuffer sb = new StringBuffer();
        int ip = 0;
        while (ip < instructions.length) {
            for (int i = 0; i < leadingSpaces; i++) {
                sb.append(" ");
            }
            sb.append(ip).append(": ");
            sb.append(Mnemonic.getMnemonic(instructions[ip]));
            if (getPushCount(instructions, ip) > 0) {
                int[] data = getPushData(instructions, ip);
                for(int j = 0; j < data.length; j++) {
                    if ((instructions[ip] == Mnemonic.PUSHW) ||
                        (instructions[ip] == Mnemonic.NPUSHW)) {
                        sb.append(" ").append((short) data[j]);
                    } else {
                        sb.append(" ").append(data[j]);
                    }
                }
            }
            sb.append("\n");
            ip = advanceIP(instructions, ip);
        }
        return sb.toString();
    }
}
