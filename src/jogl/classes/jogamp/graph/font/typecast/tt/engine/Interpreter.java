/*
 * $Id: Interpreter.java,v 1.1.1.1 2004-12-05 23:15:05 davidsch Exp $
 *
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
import jogamp.graph.font.typecast.ot.Point;

/**
 * The interpreter shall remain ignorant of the table structure - the table
 * data will be extracted by supporting classes, whether it be the Parser
 * or some other.
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 * @version $Id: Interpreter.java,v 1.1.1.1 2004-12-05 23:15:05 davidsch Exp $
 */
public class Interpreter {

    private Parser parser = null;
    private final GraphicsState gs = new GraphicsState();
    private final Point[][] zone = new Point[2][];
    private int[] stack = null;
    private int[] store = null;
    private final int[] cvt = new int[256];
    private int[] functionMap = null;
    private int stackIndex = 0;
    private boolean inFuncDef = false;

    public Interpreter(final int stackMax, final int storeMax, final int funcMax) {
        zone[0] = new Point[256];
        zone[1] = new Point[256];
        stack = new int[stackMax];
        store = new int[storeMax];
        functionMap = new int[funcMax];
    }

    /**
     * ABSolute value
     */
    private void _abs() {
        final int n = pop();
        if (n >= 0) {
            push(n);
        } else {
            push(-n);
        }
    }

    /**
     * ADD
     */
    private void _add() {
        final int n1 = pop();
        final int n2 = pop();
        push(n2 + n1);
    }

    private void _alignpts() {
        pop();
        pop();
    }

    /**
     *
     *
     * USES: loop
     */
    private void _alignrp() {
        while (gs.loop-- > 0) {
            pop();
        }
        gs.loop = 1;
    }

    /**
     * logical AND
     */
    private void _and() {
        final int e2 = pop();
        final int e1 = pop();
        push(((e1 != 0) && (e2 != 0)) ? 1 : 0);
    }

    /**
     * CALL function
     */
    private void _call() {
        execute(functionMap[pop()]);
    }

    /**
     * CEILING
     */
    private void _ceiling() {
        final int n = pop();
        if (n >= 0) {
            push((n & 0xffc0) + (((n & 0x3f) != 0) ? 0x40 : 0));
        } else {
            push(n & 0xffc0);
        }
    }

    /**
     * Copy the INDEXed element to the top of the stack
     */
    private void _cindex() {
        push(stack[stackIndex - pop()]);
    }

    /**
     * CLEAR the entire stack
     */
    private void _clear() {
        stackIndex = 0;
    }

    private void _debug() {
        pop();
    }

    /**
     * DELTA exception C1
     */
    private void _deltac1() {
        final int n = pop();
        for (int i = 0; i < n; i++) {
            pop();    // pn
            pop();    // argn
        }
    }

    /**
     * DELTA exception C2
     */
    private void _deltac2() {
        final int n = pop();
        for (int i = 0; i < n; i++) {
            pop();    // pn
            pop();    // argn
        }
    }

    /**
     * DELTA exception C3
     */
    private void _deltac3() {
        final int n = pop();
        for (int i = 0; i < n; i++) {
            pop();    // pn
            pop();    // argn
        }
    }

    /**
     * DELTA exception P1
     */
    private void _deltap1() {
        final int n = pop();
        for (int i = 0; i < n; i++) {
            pop();    // pn
            pop();    // argn
        }
    }

    /**
     * DELTA exception P2
     */
    private void _deltap2() {
        final int n = pop();
        for (int i = 0; i < n; i++) {
            pop();    // pn
            pop();    // argn
        }
    }

    /**
     * DELTA exception P3
     */
    private void _deltap3() {
        final int n = pop();
        for (int i = 0; i < n; i++) {
            pop();    // pn
            pop();    // argn
        }
    }

    /**
     * Returns the DEPTH of the stack
     */
    private void _depth() {
        push(stackIndex);
    }

    /**
     * DIVide
     */
    private void _div() {
        final int n1 = pop();
        final int n2 = pop();
        push((n2 / n1) >> 6);
    }

    /**
     * DUPlicate top stack element
     */
    private void _dup() {
        final int n = pop();
        push(n);
        push(n);
    }

    /**
     * ELSE
     */
    private int _else(final int instructionIndex) {
        return parser.handleElse(instructionIndex);
    }

    /**
     * EQual
     */
    private void _eq() {
        final int e2 = pop();
        final int e1 = pop();
        push((e1 == e2) ? 1 : 0);
    }

    private void _even() {
        pop();
        push(0);
    }

    /**
     * Function DEFinition
     */
    private void _fdef(final int instructionIndex) {
        functionMap[pop()] = instructionIndex;
        inFuncDef = true;
    }

    /**
     * Set the auto_FLIP boolean to OFF
     */
    private void _flipoff() {
        gs.auto_flip = false;
    }

    /**
     * Set the auto_FLIP boolean to ON
     */
    private void _flipon() {
        gs.auto_flip = true;
    }

    /**
     * FLIP PoinT
     *
     * USES: loop
     */
    private void _flippt() {
        while(gs.loop-- > 0) {
            final int index = pop();
            zone[gs.zp0][index].onCurve = !zone[gs.zp0][index].onCurve;
        }
        gs.loop = 1;
    }

    /**
     * FLIP RanGe OFF
     */
    private void _fliprgoff() {
        final int end = pop();
        final int start = pop();
        for (int i = start; i <= end; i++) {
            zone[1][i].onCurve = false;
        }
    }

    /**
     * FLIP RanGe ON
     */
    private void _fliprgon() {
        final int end = pop();
        final int start = pop();
        for (int i = start; i <= end; i++) {
            zone[1][i].onCurve = true;
        }
    }

    /**
     * FLOOR
     */
    private void _floor() {
        final int n = pop();
        if (n >= 0) {
            push(n & 0xffc0);
        } else {
            push((n & 0xffc0) - (((n & 0x3f) != 0) ? 0x40 : 0));
        }
    }

    private void _gc(final short param) {
        pop();
        push(0);
    }

    private void _getinfo() {
        pop();
        push(0);
    }

    /**
     * Get Freedom_Vector
     */
    private void _gfv() {
        push(gs.freedom_vector[0]);
        push(gs.freedom_vector[1]);
    }

    /**
     * Get Projection_Vector
     */
    private void _gpv() {
        push(gs.projection_vector[0]);
        push(gs.projection_vector[1]);
    }

    /**
     * Greater Than
     */
    private void _gt() {
        final int e2 = pop();
        final int e1 = pop();
        push((e1 > e2) ? 1 : 0);
    }

    /**
     * Greater Than or EQual
     */
    private void _gteq() {
        final int e2 = pop();
        final int e1 = pop();
        push((e1 >= e2) ? 1 : 0);
    }

    /**
     * Instruction DEFinition
     */
    private void _idef() {
        pop();
        inFuncDef = true;
    }

    /**
     * IF test
     */
    private int _if(final int instructionIndex) {
        return parser.handleIf(pop() != 0, instructionIndex);
    }

    /**
     * INSTruction Execution ConTRol
     *
     * INSTCTRL[]
     *
     * Code Range
     * 0x8E
     *
     * Pops
     * s: selector flag (int32)
     * value: USHORT (padded to 32 bits) used to set value of instruction_control.
     *
     * Pushes
     * -
     *
     * Sets
     * instruction_control
     *
     * Sets the instruction control state variable making it possible to turn on or off
     * the execution of instructions and to regulate use of parameters set in the CVT
     * program. INSTCTRL[ ] can only be executed in the CVT program.
     *
     * This instruction clears and sets various control flags in the rasterizer. The
     * selector flag determines valid values for the value argument. The value determines
     * the new setting of the raterizer control flag. In version 1.0 there are only two
     * flags in use:
     *
     * Selector flag 1 is used to inhibit grid-fitting. If s=1, valid values for the
     * value argument are 0 (FALSE) and 1 (TRUE). If the value argument is set to TRUE
     * (v=1), any instructions associated with glyphs will not be executed. For example,
     * to inhibit grid-fitting when a glyph is being rotated or stretched, use the
     * following sequence on the preprogram:
     *
     * PUSHB[000] 6    ; ask GETINFO to check for stretching or rotation
     * GETINFO[]        ; will push TRUE if glyph is stretched or rotated
     * IF[]                ; tests value at top of stack
     * PUSHB[000] 1    ; value for INSTCTRL
     * PUSHB[000] 1    ; selector for INSTCTRL
     * INSTRCTRL[]        ; based on selector and value will turn grid-fitting off
     * EIF[]
     *
     * Selector flag 2 is used to establish that any parameters set in the CVT program
     * should be ignored when instructions associated with glyphs are executed. These
     * include, for example, the values for scantype and the CVT cut-in. If s=1, valid
     * values for the value argument are 0 (FALSE) and 2 (TRUE). If the value argument is
     * set to TRUE (v=2), the default values of those parameters will be used regardless
     * of any changes that may have been made in those values by the preprogram. If the
     * value argument is set to FALSE (v=0), parameter values changed by the CVT program
     * will be used in glyph instructions.
     */
    private void _instctrl() {
        final int s = pop();
        final int v = pop();
        if (s == 1) {
            gs.instruction_control |= v;
        } else if (s == 2) {
            gs.instruction_control |= v;
        }
    }

    private void _ip() {
        pop();
    }

    private void _isect() {
        pop();
        pop();
        pop();
        pop();
        pop();
    }

    private void _iup(final short param) {
    }

    /**
     * JuMP Relative
     */
    private int _jmpr(final int instructionIndex) {
        return instructionIndex + ( pop() - 1 );
    }

    /**
     * Jump Relative On False
     */
    private int _jrof(int instructionIndex) {
        final boolean test = pop() != 0;
        final int offset = pop();
        if (!test) {
            instructionIndex += offset - 1;
        }
        return instructionIndex;
    }

    /**
     * Jump Relative On True
     */
    private int _jrot(int instructionIndex) {
        final boolean test = pop() != 0;
        final int offset = pop();
        if (test) {
            instructionIndex += offset - 1;
        }
        return instructionIndex;
    }

    /**
     * LOOP and CALL function
     */
    private void _loopcall() {
        /* final int index = */ pop();
        final int count = pop();
        for (int i = 0; i < count; i++) {
            execute(functionMap[i]);
        }
    }

    /**
     * Less Than
     */
    private void _lt() {
        final int e2 = pop();
        final int e1 = pop();
        push((e1 < e2) ? 1 : 0);
    }

    /**
     * Less Than or EQual
     */
    private void _lteq() {
        final int e2 = pop();
        final int e1 = pop();
        push((e1 <= e2) ? 1 : 0);
    }

    /**
     * MAXimum of top two stack elements
     */
    private void _max() {
        final int n1 = pop();
        final int n2 = pop();
        push((n1 > n2) ? n1 : n2);
    }

    private void _md(final short param) {
        pop();
        pop();
        push(0);
    }

    private void _mdap(final short param) {
        pop();
    }

    private void _mdrp(final short param) {
        pop();
    }

    private void _miap(final short param) {
        pop();
        pop();
    }
    /**
     * MINimum of top two stack elements
     */
    private void _min() {
        final int n1 = pop();
        final int n2 = pop();
        push((n1 < n2) ? n1 : n2);
    }

    /**
     * Move the INDEXed element to the top of the stack
     */
    private void _mindex() {
        // Move the indexed element to stackIndex, and shift the others down
        final int k = pop();
        final int e = stack[stackIndex - k];
        for (int i = stackIndex - k; i < stackIndex - 1; i++) {
            stack[i] = stack[i+1];
        }
        stack[stackIndex - 1] = e;
    }

    private void _mirp(final short param) {
        pop();
        pop();
    }

    private void _mppem() {
        push(0);
    }

    private void _mps() {
        push(0);
    }

    private void _msirp(final short param) {
        pop();
        pop();
    }

    /**
     * MULtiply
     */
    private void _mul() {
        final int n1 = pop();
        final int n2 = pop();
        push((n1 * n2) >> 6);
    }

    /**
     * NEGate
     */
    private void _neg() {
        push(-pop());
    }

    /**
     * Not EQual
     */
    private void _neq() {
        final int e2 = pop();
        final int e1 = pop();
        push((e1 != e2) ? 1 : 0);
    }

    /**
     * logical NOT
     */
    private void _not() {
        push((pop() != 0) ? 0 : 1);
    }

    private void _nround(final short param) {
        pop();
        push(0);
    }

    private void _odd() {
        pop();
        push(0);
    }

    /**
     * logical OR
     */
    private void _or() {
        final int e2 = pop();
        final int e1 = pop();
        push(((e1 != 0) || (e2 != 0)) ? 1 : 0);
    }

    /**
     * PUSH N Bytes
     * PUSH N Words
     * PUSH Bytes
     * PUSH Words
     */
    private void _push(final int[] data) {
        for (int j = 0; j < data.length; j++) {
            push(data[j]);
        }
    }

    /**
     * Read Control Value Table
     */
    private void _rcvt() {
        push(cvt[pop()]);
    }

    /**
     * Round Down To Grid
     */
    private void _rdtg() {
        gs.round_state = 3;
    }

    /**
     * Round OFF
     */
    private void _roff() {
        gs.round_state = 5;
    }

    /**
     * ROLL the top three stack elements
     */
    private void _roll() {
        final int a = pop();
        final int b = pop();
        final int c = pop();
        push(b);
        push(a);
        push(c);
    }

    private void _round(final short param) {
        pop();
        push(0);
    }

    /**
     * Read Store
     */
    private void _rs() {
        push(store[pop()]);
    }

    /**
     * Round To Double Grid
     */
    private void _rtdg() {
        gs.round_state = 2;
    }

    /**
     * Round To Grid
     */
    private void _rtg() {
        gs.round_state = 1;
    }

    /**
     * Round To Half Grid
     */
    private void _rthg() {
        gs.round_state = 0;
    }

    /**
     * Round Up To Grid
     */
    private void _rutg() {
        gs.round_state = 4;
    }

    private void _s45round() {
        pop();
    }

    /**
     * SCAN conversion ConTRoL
     *
     * SCANCTRL[ ]
     *
     * Code Range
     * 0x85
     *
     * Pops
     * n: flags indicating when to turn on dropout control mode (16 bit word padded
     * to 32 bits)
     *
     * Pushes
     * -
     *
     * Sets
     * scan_control
     *
     * SCANCTRL is used to set the value of the Graphics State variable scan_control
     * which in turn determines whether the scan converter will activate dropout
     * control for this glyph. Use of the dropout control mode is determined by three
     * conditions:
     *
     * Is the glyph rotated?
     *
     * Is the glyph stretched?
     *
     * Is the current setting for ppem less than a specified threshold?
     *
     * The interpreter pops a word from the stack and looks at the lower 16 bits.
     *
     * Bits 0-7 represent the threshold value for ppem. A value of FF in bits 0-7
     * means invoke dropout_control for all sizes. A value of 0 in bits 0-7 means
     * never invoke dropout_control.
     *
     * Bits 8-13 are used to turn on dropout_control in cases where the specified
     * conditions are met. Bits 8, 9 and 10 are used to turn on the dropout_control
     * mode (assuming other conditions do not block it). Bits 11, 12, and 13 are
     * used to turn off the dropout mode unless other conditions force it. Bits 14
     * and 15 are reserved for future use.
     *
     * Bit   Meaning if set
     * ---   --------------
     *   8   Set dropout_control to TRUE if other conditions do not block and ppem
     *       is less than or equal to the threshold value.
     *
     *   9   Set dropout_control to TRUE if other conditions do not block and the
     *       glyph is rotated.
     *
     *  10   Set dropout_control to TRUE if other conditions do not block and the
     *       glyph is stretched.
     *
     *  11   Set dropout_control to FALSE unless ppem is less than or equal to the
     *       threshold value.
     *
     *  12   Set dropout_control to FALSE unless the glyph is rotated.
     *
     *  13   Set dropout_control to FALSE unless the glyph is stretched.
     *
     *  14   Reserved for future use.
     *
     *  15   Reserved for future use.
     *
     * For example
     * 0x0000 No dropout control is invoked
     * 0x01FF Always do dropout control
     * 0x0A10 Do dropout control if the glyph is rotated and has less than 16
     *        pixels per-em
     *
     * The scan converter can operate in either a "normal" mode or in a "fix dropout"
     * mode depending on the value of a set of enabling and disabling flags.
     */
    private void _scanctrl() {
        gs.scan_control = pop();
    }

    /**
     * SCANTYPE
     *
     * SCANTYPE[]
     *
     * Code Range
     * 0x8D
     *
     * Pops
     * n: 16 bit integer
     *
     * Pushes
     * -
     *
     * Sets
     * scan_control
     *
     * Pops a 16-bit integer whose value is used to determine which rules the scan
     * converter will use. If the value of the argument is 0, the fast scan converter
     * will be used. If the value of the integer is 1 or 2, simple dropout control will
     * be used. If the value of the integer is 4 or 5, smart dropout control will be
     * used. More specifically,
     *
     *   if n=0 rules 1, 2, and 3 are invoked (simple dropout control scan conversion
     *   including stubs)
     *
     *   if n=1 rules 1, 2, and 4 are invoked (simple dropout control scan conversion
     *   excluding stubs)
     *
     *   if n=2 rules 1 and 2 only are invoked (fast scan conversion; dropout control
     *   turned off)
     *
     *   if n=3 same as n = 2
     *
     *   if n = 4 rules 1, 2, and 5 are invoked (smart dropout control scan conversion
     *   including stubs)
     *
     *   if n = 5 rules 1, 2, and 6 are invoked (smart dropout control scan conversion
     *   excluding stubs)
     *
     *   if n = 6 same as n = 2
     *
     *   if n = 7 same as n = 2
     *
     * The scan conversion rules are shown here:
     *
     * Rule 1
     * If a pixel's center falls within the glyph outline, that pixel is turned on.
     *
     * Rule 2
     * If a contour falls exactly on a pixel's center, that pixel is turned on.
     *
     * Rule 3
     * If a scan line between two adjacent pixel centers (either vertical or
     * horizontal) is intersected by both an on-Transition contour and an off-Transition
     * contour and neither of the pixels was already turned on by rules 1 and 2, turn on
     * the left-most pixel (horizontal scan line) or the bottom-most pixel (vertical scan
     * line). This is "Simple" dropout control.
     *
     * Rule 4
     * Apply Rule 3 only if the two contours continue to intersect other scan lines in
     * both directions. That is, do not turn on pixels for 'stubs.' The scanline segments
     * that form a square with the intersected scan line segment are examined to verify
     * that they are intersected by two contours. It is possible that these could be
     * different contours than the ones intersecting the dropout scan line segment. This
     * is very unlikely but may have to be controlled with grid-fitting in some exotic
     * glyphs.
     *
     * Rule 5
     * If a scan line between two adjacent pixel centers (either vertical or horizontal)
     * is intersected by both an on-Transition contour and an off-Transition contour and
     * neither of the pixels was already turned on by rules 1 and 2, turn on the pixel
     * which is closer to the midpoint between the on-Transition contour and off-
     * Transition contour. This is "Smart" dropout control.
     *
     * Rule 6
     * Apply Rule 5 only if the two contours continue to intersect other scan lines in
     * both directions. That is, do not turn on pixels for 'stubs.'
     *
     * New fonts wishing to use the new modes of the ScanType instruction, but still
     * wishing to work correctly on old rasterizers that don't recognize the new modes
     * should:
     *
     * First execute a ScanType instruction using an old mode which will give the best
     * approximation to the desired new mode (e.g. Simple Stubs for Smart Stubs), and
     * then
     *
     * Immediately execute another ScanType instruction with the desired new mode.
     */
    private void _scantype() {
        pop();
    }

    private void _scfs() {
        pop();
        pop();
    }

    /**
     * Set Control Value Table Cut In
     */
    private void _scvtci() {
        gs.control_value_cut_in = pop();
    }

    /**
     * Set Delta_Base in the graphics state
     */
    private void _sdb() {
        gs.delta_base = pop();
    }

    /**
     * Set Dual Projection_Vector To Line
     */
    private void _sdpvtl(final short param) {
        pop();
        pop();
    }

    /**
     * Set Delta_Shift in the graphics state
     */
    private void _sds() {
        gs.delta_shift = pop();
    }

    /**
     * Set Freedom_Vector From Stack
     */
    private void _sfvfs() {
        gs.freedom_vector[1] = pop();    // y
        gs.freedom_vector[0] = pop();    // x
    }

    /*
     * Set Freedom_Vector to Coordinate Axis
     */
    private void _sfvtca(final short param) {
        if (param == 1) {
            gs.freedom_vector[0] = 0x4000;
            gs.freedom_vector[1] = 0x0000;
        } else {
            gs.freedom_vector[0] = 0x0000;
            gs.freedom_vector[1] = 0x4000;
        }
    }

    /*
     * Set Freedom_Vector To Line
     */
    private void _sfvtl(final short param) {
        pop();
        pop();
        // if (param == 1) {
            gs.freedom_vector[0] = 0x0000;
            gs.freedom_vector[1] = 0x0000;
        // } else {
        //    gs.freedom_vector[0] = 0x0000;
        //    gs.freedom_vector[1] = 0x0000;
        //}
    }

    /**
     * Set Freedom_Vector To Projection Vector
     */
    private void _sfvtpv() {
        gs.freedom_vector[0] = gs.projection_vector[0];
        gs.freedom_vector[1] = gs.projection_vector[1];
    }

    private void _shc(final short param) {
        pop();
    }

    /**
     * SHift Point by the last point
     *
     * USES: loop
     */
    private void _shp(final short param) {
        while(gs.loop-- > 0) {
            pop();
            if(param == 0) {
            } else {
            }
        }
        gs.loop = 1;
    }

    /**
     * SHift Point by a PIXel amount
     *
     * USES: loop
     */
    private void _shpix() {
        pop();    // amount
        while (gs.loop-- > 0) {
            pop();    // p
        }
        gs.loop = 1;
    }

    private void _shz(final short param) {
        pop();
    }

    /**
     * Set LOOP variable
     */
    private void _sloop() {
        gs.loop = pop();
    }

    /**
     * Set Minimum_Distance
     */
    private void _smd() {
        gs.minimum_distance = pop();
    }

    /**
     * Set Projection_Vector From Stack
     */
    private void _spvfs() {
        gs.projection_vector[1] = pop();    // y
        gs.projection_vector[0] = pop();    // x
    }

    /*
     * Set Projection_Vector To Coordinate Axis
     */
    private void _spvtca(final short param) {
        if (param == 1) {
            gs.projection_vector[0] = 0x4000;
            gs.projection_vector[1] = 0x0000;
        } else {
            gs.projection_vector[0] = 0x0000;
            gs.projection_vector[1] = 0x4000;
        }
    }

    /**
     * Set Projection_Vector To Line
     */
    private void _spvtl(final short param) {

        // below block is dead code, reduce to pop() calls.
        pop();
        pop();
        /**
        // We'll get a copy of the line and normalize it -
        // divide the x- and y-coords by the vector's dot product.
        final Point p1 = zone[gs.zp2][pop()];
        final Point p2 = zone[gs.zp1][pop()];
        final int x = p2.x - p1.x;
        final int y = p2.y - p1.y;
         */
        // if(param == 1) {
            gs.projection_vector[0] = 0x0000;
            gs.projection_vector[1] = 0x0000;
        // } else {
        //    gs.projection_vector[0] = 0x0000;
        //    gs.projection_vector[1] = 0x0000;
        // }
    }

    private void _sround() {
        pop();
    }

    /**
     * Set Reference Point 0
     */
    private void _srp0() {
        gs.rp0 = pop();
    }

    /**
     * Set Reference Point 1
     */
    private void _srp1() {
        gs.rp1 = pop();
    }

    /**
     * Set Reference Point 2
     */
    private void _srp2() {
        gs.rp2 = pop();
    }

    /**
     * Set Single-Width
     */
    private void _ssw() {
        gs.single_width_value = pop();
    }

    /**
     * Set Single_Width_Cut_In
     */
    private void _sswci() {
        gs.single_width_cut_in = pop();
    }

    /**
     * SUBtract
     */
    private void _sub() {
        final int n1 = pop();
        final int n2 = pop();
        push(n2 - n1);
    }

    /**
     * Set freedom and projection Vectors To Coordinate Axis
     */
    private void _svtca(final short param) {
        if (param == 1) {
            gs.projection_vector[0] = 0x4000;
            gs.projection_vector[1] = 0x0000;
            gs.freedom_vector[0] = 0x4000;
            gs.freedom_vector[1] = 0x0000;
        } else {
            gs.projection_vector[0] = 0x0000;
            gs.projection_vector[1] = 0x4000;
            gs.freedom_vector[0] = 0x0000;
            gs.freedom_vector[1] = 0x4000;
        }
    }

    /**
     * SWAP the top two elements on the stack
     */
    private void _swap() {
        final int n1 = pop();
        final int n2 = pop();
        push(n1);
        push(n2);
    }

    /**
     * Set Zone Pointer 0
     */
    private void _szp0() {
        gs.zp0 = pop();
    }

    /**
     * Set Zone Pointer 1
     */
    private void _szp1() {
        gs.zp1 = pop();
    }

    /**
     * Set Zone Pointer 2
     */
    private void _szp2() {
        gs.zp2 = pop();
    }

    /**
     * Set Zone PointerS
     */
    private void _szps() {
        gs.zp0 = gs.zp1 = gs.zp2 = pop();
    }

    private void _utp() {
        pop();
    }

    /**
     * Write Control Value Table in FUnits
     */
    private void _wcvtf() {
        final int value = pop();
        // Conversion of value goes here
        cvt[pop()] = value;
    }

    /**
     * Write Control Value Table in Pixel units
     */
    private void _wcvtp() {
        final int value = pop();
        // Conversion of value goes here
        cvt[pop()] = value;
    }

    /**
     * Write Store
     */
    private void _ws() {
        store[pop()] = pop();
    }

    public void execute(int ip) {
        while (ip < ((ip & 0xffff0000) | parser.getISLength(ip >> 16))) {
            final short opcode = parser.getOpcode(ip);
            if (inFuncDef) {

                // We're within a function definition, so don't execute the code
                if (opcode == Mnemonic.ENDF) {
                    inFuncDef = false;
                }
                ip = parser.advanceIP(ip);
                continue;
            }
            if (opcode >= Mnemonic.MIRP) _mirp((short)(opcode & 31));
            else if (opcode >= Mnemonic.MDRP) _mdrp((short)(opcode & 31));
            else if (opcode >= Mnemonic.PUSHW) _push(parser.getPushData(ip));
            else if (opcode >= Mnemonic.PUSHB) _push(parser.getPushData(ip));
            else if (opcode >= Mnemonic.INSTCTRL) _instctrl();
            else if (opcode >= Mnemonic.SCANTYPE) _scantype();
            else if (opcode >= Mnemonic.MIN) _min();
            else if (opcode >= Mnemonic.MAX) _max();
            else if (opcode >= Mnemonic.ROLL) _roll();
            else if (opcode >= Mnemonic.IDEF) _idef();
            else if (opcode >= Mnemonic.GETINFO) _getinfo();
            else if (opcode >= Mnemonic.SDPVTL) _sdpvtl((short)(opcode & 1));
            else if (opcode >= Mnemonic.SCANCTRL) _scanctrl();
            else if (opcode >= Mnemonic.FLIPRGOFF) _fliprgoff();
            else if (opcode >= Mnemonic.FLIPRGON) _fliprgon();
            else if (opcode >= Mnemonic.FLIPPT) _flippt();
            else if (opcode >= Mnemonic.AA); // AA (ignored)
            else if (opcode >= Mnemonic.SANGW); // SANGW (ignored)
            else if (opcode >= Mnemonic.RDTG) _rdtg();
            else if (opcode >= Mnemonic.RUTG) _rutg();
            else if (opcode >= Mnemonic.ROFF) _roff();
            else if (opcode >= Mnemonic.JROF) ip = _jrof(ip);
            else if (opcode >= Mnemonic.JROT) ip = _jrot(ip);
            else if (opcode >= Mnemonic.S45ROUND) _s45round();
            else if (opcode >= Mnemonic.SROUND) _sround();
            else if (opcode >= Mnemonic.DELTAC3) _deltac3();
            else if (opcode >= Mnemonic.DELTAC2) _deltac2();
            else if (opcode >= Mnemonic.DELTAC1) _deltac1();
            else if (opcode >= Mnemonic.DELTAP3) _deltap3();
            else if (opcode >= Mnemonic.DELTAP2) _deltap2();
            else if (opcode >= Mnemonic.WCVTF) _wcvtf();
            else if (opcode >= Mnemonic.NROUND) _nround((short)(opcode & 3));
            else if (opcode >= Mnemonic.ROUND) _round((short)(opcode & 3));
            else if (opcode >= Mnemonic.CEILING) _ceiling();
            else if (opcode >= Mnemonic.FLOOR) _floor();
            else if (opcode >= Mnemonic.NEG) _neg();
            else if (opcode >= Mnemonic.ABS) _abs();
            else if (opcode >= Mnemonic.MUL) _mul();
            else if (opcode >= Mnemonic.DIV) _div();
            else if (opcode >= Mnemonic.SUB) _sub();
            else if (opcode >= Mnemonic.ADD) _add();
            else if (opcode >= Mnemonic.SDS) _sds();
            else if (opcode >= Mnemonic.SDB) _sdb();
            else if (opcode >= Mnemonic.DELTAP1) _deltap1();
            else if (opcode >= Mnemonic.NOT) _not();
            else if (opcode >= Mnemonic.OR) _or();
            else if (opcode >= Mnemonic.AND) _and();
            else if (opcode >= Mnemonic.EIF); // EIF
            else if (opcode >= Mnemonic.IF) ip = _if(ip);
            else if (opcode >= Mnemonic.EVEN) _even();
            else if (opcode >= Mnemonic.ODD) _odd();
            else if (opcode >= Mnemonic.NEQ) _neq();
            else if (opcode >= Mnemonic.EQ) _eq();
            else if (opcode >= Mnemonic.GTEQ) _gteq();
            else if (opcode >= Mnemonic.GT) _gt();
            else if (opcode >= Mnemonic.LTEQ) _lteq();
            else if (opcode >= Mnemonic.LT) _lt();
            else if (opcode >= Mnemonic.DEBUG) _debug();
            else if (opcode >= Mnemonic.FLIPOFF) _flipoff();
            else if (opcode >= Mnemonic.FLIPON) _flipon();
            else if (opcode >= Mnemonic.MPS) _mps();
            else if (opcode >= Mnemonic.MPPEM) _mppem();
            else if (opcode >= Mnemonic.MD) _md((short)(opcode & 1));
            else if (opcode >= Mnemonic.SCFS) _scfs();
            else if (opcode >= Mnemonic.GC) _gc((short)(opcode & 1));
            else if (opcode >= Mnemonic.RCVT) _rcvt();
            else if (opcode >= Mnemonic.WCVTP) _wcvtp();
            else if (opcode >= Mnemonic.RS) _rs();
            else if (opcode >= Mnemonic.WS) _ws();
            else if (opcode >= Mnemonic.NPUSHW) _push(parser.getPushData(ip));
            else if (opcode >= Mnemonic.NPUSHB) _push(parser.getPushData(ip));
            else if (opcode >= Mnemonic.MIAP) _miap((short)(opcode & 1));
            else if (opcode >= Mnemonic.RTDG) _rtdg();
            else if (opcode >= Mnemonic.ALIGNRP) _alignrp();
            else if (opcode >= Mnemonic.IP) _ip();
            else if (opcode >= Mnemonic.MSIRP) _msirp((short)(opcode & 1));
            else if (opcode >= Mnemonic.SHPIX) _shpix();
            else if (opcode >= Mnemonic.SHZ) _shz((short)(opcode & 1));
            else if (opcode >= Mnemonic.SHC) _shc((short)(opcode & 1));
            else if (opcode >= Mnemonic.SHP) _shp((short)(opcode & 1));
            else if (opcode >= Mnemonic.IUP) _iup((short)(opcode & 1));
            else if (opcode >= Mnemonic.MDAP) _mdap((short)(opcode & 1));
            else if (opcode >= Mnemonic.ENDF) return;
            else if (opcode >= Mnemonic.FDEF) _fdef(ip + 1);
            else if (opcode >= Mnemonic.CALL) _call();
            else if (opcode >= Mnemonic.LOOPCALL) _loopcall();
            else if (opcode >= Mnemonic.UTP) _utp();
            else if (opcode >= Mnemonic.ALIGNPTS) _alignpts();
            else if (opcode >= Mnemonic.MINDEX) _mindex();
            else if (opcode >= Mnemonic.CINDEX) _cindex();
            else if (opcode >= Mnemonic.DEPTH) _depth();
            else if (opcode >= Mnemonic.SWAP) _swap();
            else if (opcode >= Mnemonic.CLEAR) _clear();
            else if (opcode >= Mnemonic.POP) pop();
            else if (opcode >= Mnemonic.DUP) _dup();
            else if (opcode >= Mnemonic.SSW) _ssw();
            else if (opcode >= Mnemonic.SSWCI) _sswci();
            else if (opcode >= Mnemonic.SCVTCI) _scvtci();
            else if (opcode >= Mnemonic.JMPR) ip = _jmpr(ip);
            else if (opcode >= Mnemonic.ELSE) ip = _else(ip);
            else if (opcode >= Mnemonic.SMD) _smd();
            else if (opcode >= Mnemonic.RTHG) _rthg();
            else if (opcode >= Mnemonic.RTG) _rtg();
            else if (opcode >= Mnemonic.SLOOP) _sloop();
            else if (opcode >= Mnemonic.SZPS) _szps();
            else if (opcode >= Mnemonic.SZP2) _szp2();
            else if (opcode >= Mnemonic.SZP1) _szp1();
            else if (opcode >= Mnemonic.SZP0) _szp0();
            else if (opcode >= Mnemonic.SRP2) _srp2();
            else if (opcode >= Mnemonic.SRP1) _srp1();
            else if (opcode >= Mnemonic.SRP0) _srp0();
            else if (opcode >= Mnemonic.ISECT) _isect();
            else if (opcode >= Mnemonic.SFVTPV) _sfvtpv();
            else if (opcode >= Mnemonic.GFV) _gfv();
            else if (opcode >= Mnemonic.GPV) _gpv();
            else if (opcode >= Mnemonic.SFVFS) _sfvfs();
            else if (opcode >= Mnemonic.SPVFS) _spvfs();
            else if (opcode >= Mnemonic.SFVTL) _sfvtl((short)(opcode & 1));
            else if (opcode >= Mnemonic.SPVTL) _spvtl((short)(opcode & 1));
            else if (opcode >= Mnemonic.SFVTCA) _sfvtca((short)(opcode & 1));
            else if (opcode >= Mnemonic.SPVTCA) _spvtca((short)(opcode & 1));
            else if (opcode >= Mnemonic.SVTCA) _svtca((short)(opcode & 1));
            ip = parser.advanceIP(ip);
        }
    }

    public Point[][] getZones() {
        return zone;
    }

    private int pop() {
        return stack[--stackIndex];
    }

    private void push(final int i) {
        stack[stackIndex++] = i;
    }

    public void runCvtProgram() {
        execute(0x00010000);
    }

    public void runFontProgram() {
        execute(0);
    }

    public void runGlyphProgram() {
        // instruction_control can be set to stop glyphs grid-fitting
        if ((gs.instruction_control & 1) == 0) {
            execute(0x00020000);
        }
    }

    public void setParser(final Parser p) {
        parser = p;
    }
}
