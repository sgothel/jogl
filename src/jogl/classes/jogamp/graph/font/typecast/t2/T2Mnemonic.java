/*
 * $Id: T2Mnemonic.java,v 1.1 2007-02-21 12:30:48 davidsch Exp $
 *
 * Typecast - The Font Development Environment
 *
 * Copyright (c) 2004-2007 David Schweinsberg
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

package jogamp.graph.font.typecast.t2;

/**
 * The Mnemonic representations of the Type 2 charstring instruction set.
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 * @version $Id: T2Mnemonic.java,v 1.1 2007-02-21 12:30:48 davidsch Exp $
 */
public class T2Mnemonic {

    /**
     * One byte operators
     */
    public static final short HSTEM = 0x01;
    public static final short VSTEM = 0x03;
    public static final short VMOVETO = 0x04;
    public static final short RLINETO = 0x05;
    public static final short HLINETO = 0x06;
    public static final short VLINETO = 0x07;
    public static final short RRCURVETO = 0x08;
    public static final short CALLSUBR = 0x0a;
    public static final short RETURN = 0x0b;
    public static final short ESCAPE = 0x0c;
    public static final short ENDCHAR = 0x0e;
    public static final short HSTEMHM = 0x12;
    public static final short HINTMASK = 0x13;
    public static final short CNTRMASK = 0x14;
    public static final short RMOVETO = 0x15;
    public static final short HMOVETO = 0x16;
    public static final short VSTEMHM = 0x17;
    public static final short RCURVELINE = 0x18;
    public static final short RLINECURVE = 0x19;
    public static final short VVCURVETO = 0x1a;
    public static final short HHCURVETO = 0x1b;
    public static final short CALLGSUBR = 0x1d;
    public static final short VHCURVETO = 0x1e;
    public static final short HVCURVETO = 0x1f;

    /**
     * Two byte operators
     */
    public static final short DOTSECTION = 0x00;
    public static final short AND = 0x03;
    public static final short OR = 0x04;
    public static final short NOT = 0x05;
    public static final short ABS = 0x09;
    public static final short ADD = 0x0a;
    public static final short SUB = 0x0b;
    public static final short DIV = 0x0c;
    public static final short NEG = 0x0e;
    public static final short EQ = 0x0f;
    public static final short DROP = 0x12;
    public static final short PUT = 0x14;
    public static final short GET = 0x15;
    public static final short IFELSE = 0x16;
    public static final short RANDOM = 0x17;
    public static final short MUL = 0x18;
    public static final short SQRT = 0x1a;
    public static final short DUP = 0x1b;
    public static final short EXCH = 0x1c;
    public static final short INDEX = 0x1d;
    public static final short ROLL = 0x1e;
    public static final short HFLEX = 0x22;
    public static final short FLEX = 0x23;
    public static final short HFLEX1 = 0x24;
    public static final short FLEX1 = 0x25;
}
