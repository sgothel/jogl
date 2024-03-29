/*
 * Typecast - The Font Development Environment
 *
 * Copyright (c) 2004-2016 David Schweinsberg
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

package jogamp.graph.font.typecast.cff;

import java.util.ArrayList;
import jogamp.graph.font.typecast.ot.Point;

/**
 * Type 2 Charstring Interpreter.  Operator descriptions are quoted from
 * Adobe's Type 2 Charstring Format document -- 5117.Type2.pdf.
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class T2Interpreter {

    private static final boolean DEBUG = false;
    
    private static class SubrPair {
        final CharstringType2 cs;
        final int ip;
        SubrPair(CharstringType2 cs, int ip) {
            this.cs = cs;
            this.ip = ip;
        }
    }
    
    private static final int ARGUMENT_STACK_LIMIT = 48;
    private static final int SUBR_STACK_LIMIT = 10;
    private static final int TRANSIENT_ARRAY_ELEMENT_COUNT = 32;
    
    private final Number[] _argStack = new Number[ARGUMENT_STACK_LIMIT];
    private int _argStackIndex = 0;
    private final SubrPair[] _subrStack = new SubrPair[SUBR_STACK_LIMIT];
    private int _subrStackIndex = 0;
    private final Number[] _transientArray = new Number[TRANSIENT_ARRAY_ELEMENT_COUNT];
    
    private int _stemCount = 0;
    private ArrayList<Integer> _hstems;
    private ArrayList<Integer> _vstems;
    
    private ArrayList<Point> _points;
    private Index _localSubrIndex;
    private Index _globalSubrIndex;
    private CharstringType2 _localSubrs;
    private CharstringType2 _globalSubrs;
    private CharstringType2 _cs;
    private int _ip;

    /** Creates a new instance of T2Interpreter */
    public T2Interpreter() {
    }
    
    public Integer[] getHStems() {
        Integer[] array = new Integer[_hstems.size()];
        return _hstems.toArray(array);
    }
    
    public Integer[] getVStems() {
        Integer[] array = new Integer[_vstems.size()];
        return _vstems.toArray(array);
    }
    
    /**
     * Moves the current point to a position at the relative coordinates
     * (dx1, dy1).
     */
    private void _rmoveto() {
        int dy1 = popArg().intValue();
        int dx1 = popArg().intValue();
        clearArg();
        Point lastPoint = getLastPoint();
        moveTo(lastPoint.x + dx1, lastPoint.y + dy1);
    }

    /**
     * Moves the current point dx1 units in the horizontal direction.
     */
    private void _hmoveto() {
        int dx1 = popArg().intValue();
        clearArg();
        Point lastPoint = getLastPoint();
        moveTo(lastPoint.x + dx1, lastPoint.y);
    }
    
    /**
     * Moves the current point dy1 units in the vertical direction.
     */
    private void _vmoveto() {
        int dy1 = popArg().intValue();
        clearArg();
        Point lastPoint = getLastPoint();
        moveTo(lastPoint.x, lastPoint.y + dy1);
    }
    
    /**
     * Appends a line from the current point to a position at the
     * relative coordinates dxa, dya. Additional rlineto operations are
     * performed for all subsequent argument pairs. The number of
     * lines is determined from the number of arguments on the stack.
     */
    private void _rlineto() {
        int count = getArgCount() / 2;
        int[] dx = new int[count];
        int[] dy = new int[count];
        for (int i = 0; i < count; ++i) {
            dy[count - i - 1] = popArg().intValue();
            dx[count - i - 1] = popArg().intValue();
        }
        for (int i = 0; i < count; ++i) {
            Point lastPoint = getLastPoint();
            lineTo(lastPoint.x + dx[i], lastPoint.y + dy[i]);
        }
        clearArg();
    }
    
    /**
     * Appends a horizontal line of length dx1 to the current point.
     * With an odd number of arguments, subsequent argument pairs
     * are interpreted as alternating values of dy and dx, for which
     * additional lineto operators draw alternating vertical and
     * horizontal lines. With an even number of arguments, the
     * arguments are interpreted as alternating horizontal and
     * vertical lines. The number of lines is determined from the
     * number of arguments on the stack.
     */
    private void _hlineto() {
        int count = getArgCount();
        Number[] nums = new Number[count];
        for (int i = 0; i < count; ++i) {
            nums[count - i - 1] = popArg();
        }
        for (int i = 0; i < count; ++i) {
            Point lastPoint = getLastPoint();
            if (i % 2 == 0) {
                lineTo(lastPoint.x + nums[i].intValue(), lastPoint.y);
            } else {
                lineTo(lastPoint.x, lastPoint.y + nums[i].intValue());
            }
        }
        clearArg();
    }
    
    /**
     * Appends a vertical line of length dy1 to the current point. With
     * an odd number of arguments, subsequent argument pairs are
     * interpreted as alternating values of dx and dy, for which
     * additional lineto operators draw alternating horizontal and
     * vertical lines. With an even number of arguments, the
     * arguments are interpreted as alternating vertical and
     * horizontal lines. The number of lines is determined from the
     * number of arguments on the stack.
     */
    private void _vlineto() {
        int count = getArgCount();
        Number[] nums = new Number[count];
        for (int i = 0; i < count; ++i) {
            nums[count - i - 1] = popArg();
        }
        for (int i = 0; i < count; ++i) {
            Point lastPoint = getLastPoint();
            if (i % 2 == 0) {
                lineTo(lastPoint.x, lastPoint.y + nums[i].intValue());
            } else {
                lineTo(lastPoint.x + nums[i].intValue(), lastPoint.y);
            }
        }
        clearArg();
    }
    
    /**
     * Appends a Bezier curve, defined by dxa...dyc, to the current
     * point. For each subsequent set of six arguments, an additional
     * curve is appended to the current point. The number of curve
     * segments is determined from the number of arguments on the
     * number stack and is limited only by the size of the number
     * stack.
     */
    private void _rrcurveto() {
        int count = getArgCount() / 6;
        int[] dxa = new int[count];
        int[] dya = new int[count];
        int[] dxb = new int[count];
        int[] dyb = new int[count];
        int[] dxc = new int[count];
        int[] dyc = new int[count];
        for (int i = 0; i < count; ++i) {
            dyc[count - i - 1] = popArg().intValue();
            dxc[count - i - 1] = popArg().intValue();
            dyb[count - i - 1] = popArg().intValue();
            dxb[count - i - 1] = popArg().intValue();
            dya[count - i - 1] = popArg().intValue();
            dxa[count - i - 1] = popArg().intValue();
        }
        for (int i = 0; i < count; ++i) {
            Point lastPoint = getLastPoint();
            int xa = lastPoint.x + dxa[i];
            int ya = lastPoint.y + dya[i];
            int xb = xa + dxb[i];
            int yb = ya + dyb[i];
            int xc = xb + dxc[i];
            int yc = yb + dyc[i];
            curveTo(xa, ya, xb, yb, xc, yc);
        }
        clearArg();
    }
    
    /**
     * Appends one or more Bezier curves, as described by the
     * dxa...dxc set of arguments, to the current point. For each curve,
     * if there are 4 arguments, the curve starts and ends horizontal.
     * The first curve need not start horizontal (the odd argument
     * case). Note the argument order for the odd argument case.
     */
    private void _hhcurveto() {
        int count = getArgCount() / 4;
        int dy1 = 0;
        int[] dxa = new int[count];
        int[] dxb = new int[count];
        int[] dyb = new int[count];
        int[] dxc = new int[count];
        for (int i = 0; i < count; ++i) {
            dxc[count - i - 1] = popArg().intValue();
            dyb[count - i - 1] = popArg().intValue();
            dxb[count - i - 1] = popArg().intValue();
            dxa[count - i - 1] = popArg().intValue();
        }
        if (getArgCount() == 1) {
            dy1 = popArg().intValue();
        }
        for (int i = 0; i < count; ++i) {
            Point lastPoint = getLastPoint();
            int xa = lastPoint.x + dxa[i];
            int ya = lastPoint.y + (i == 0 ? dy1 : 0);
            int xb = xa + dxb[i];
            int yb = ya + dyb[i];
            int xc = xb + dxc[i];
            curveTo(xa, ya, xb, yb, xc, yb);
        }
        clearArg();
    }
    
    /**
     * Appends one or more Bezier curves to the current point. The
     * tangent for the first Bezier must be horizontal, and the second
     * must be vertical (except as noted below).
     * If there is a multiple of four arguments, the curve starts
     * horizontal and ends vertical. Note that the curves alternate
     * between start horizontal, end vertical, and start vertical, and
     * end horizontal. The last curve (the odd argument case) need not
     * end horizontal/vertical.
     */
    private void _hvcurveto() {
        if (getArgCount() % 8 <= 1) {
            int count = getArgCount() / 8;
            int[] dxa = new int[count];
            int[] dxb = new int[count];
            int[] dyb = new int[count];
            int[] dyc = new int[count];
            int[] dyd = new int[count];
            int[] dxe = new int[count];
            int[] dye = new int[count];
            int[] dxf = new int[count];
            int dyf = 0;
            if (getArgCount() % 8 == 1) {
                dyf = popArg().intValue();
            }
            for (int i = 0; i < count; ++i) {
                dxf[count - i - 1] = popArg().intValue();
                dye[count - i - 1] = popArg().intValue();
                dxe[count - i - 1] = popArg().intValue();
                dyd[count - i - 1] = popArg().intValue();
                dyc[count - i - 1] = popArg().intValue();
                dyb[count - i - 1] = popArg().intValue();
                dxb[count - i - 1] = popArg().intValue();
                dxa[count - i - 1] = popArg().intValue();
            }
            for (int i = 0; i < count; ++i) {
                Point lastPoint = getLastPoint();
                int xa = lastPoint.x + dxa[i];
                int ya = lastPoint.y;
                int xb = xa + dxb[i];
                int yb = ya + dyb[i];
                int yc = yb + dyc[i];
                int yd = yc + dyd[i];
                int xe = xb + dxe[i];
                int ye = yd + dye[i];
                int xf = xe + dxf[i];
                int yf = ye + (i == count - 1 ? dyf : 0);
                curveTo(xa, ya, xb, yb, xb, yc);
                curveTo(xb, yd, xe, ye, xf, yf);
            }
        } else {
            int count = getArgCount() / 8;
            int[] dya = new int[count];
            int[] dxb = new int[count];
            int[] dyb = new int[count];
            int[] dxc = new int[count];
            int[] dxd = new int[count];
            int[] dxe = new int[count];
            int[] dye = new int[count];
            int[] dyf = new int[count];
            int dxf = 0;
            if (getArgCount() % 4 == 1) {
                dxf = popArg().intValue();
            }
            for (int i = 0; i < count; ++i) {
                dyf[count - i - 1] = popArg().intValue();
                dye[count - i - 1] = popArg().intValue();
                dxe[count - i - 1] = popArg().intValue();
                dxd[count - i - 1] = popArg().intValue();
                dxc[count - i - 1] = popArg().intValue();
                dyb[count - i - 1] = popArg().intValue();
                dxb[count - i - 1] = popArg().intValue();
                dya[count - i - 1] = popArg().intValue();
            }
            int dy3 = popArg().intValue();
            int dy2 = popArg().intValue();
            int dx2 = popArg().intValue();
            int dx1 = popArg().intValue();
            
            Point lastPoint = getLastPoint();
            int x1 = lastPoint.x + dx1;
            int y1 = lastPoint.y;
            int x2 = x1 + dx2;
            int y2 = y1 + dy2;
            int x3 = x2 + (count == 0 ? dxf : 0);
            int y3 = y2 + dy3;
            curveTo(x1, y1, x2, y2, x3, y3);

            for (int i = 0; i < count; ++i) {
                lastPoint = getLastPoint();
                int xa = lastPoint.x;
                int ya = lastPoint.y + dya[i];
                int xb = xa + dxb[i];
                int yb = ya + dyb[i];
                int xc = xb + dxc[i];
                int xd = xc + dxd[i];
                int xe = xd + dxe[i];
                int ye = yb + dye[i];
                int xf = xe + (i == count - 1 ? dxf : 0);
                int yf = ye + dyf[i];
                curveTo(xa, ya, xb, yb, xc, yb);
                curveTo(xd, yb, xe, ye, xf, yf);
            }
        }
        clearArg();
    }
    
    /**
     * Is equivalent to one rrcurveto for each set of six arguments
     * dxa...dyc, followed by exactly one rlineto using the dxd, dyd
     * arguments. The number of curves is determined from the count
     * on the argument stack.
     */
    private void _rcurveline() {
        int count = (getArgCount() - 2) / 6;
        int[] dxa = new int[count];
        int[] dya = new int[count];
        int[] dxb = new int[count];
        int[] dyb = new int[count];
        int[] dxc = new int[count];
        int[] dyc = new int[count];
        int dyd = popArg().intValue();
        int dxd = popArg().intValue();
        for (int i = 0; i < count; ++i) {
            dyc[count - i - 1] = popArg().intValue();
            dxc[count - i - 1] = popArg().intValue();
            dyb[count - i - 1] = popArg().intValue();
            dxb[count - i - 1] = popArg().intValue();
            dya[count - i - 1] = popArg().intValue();
            dxa[count - i - 1] = popArg().intValue();
        }
        int xc = 0;
        int yc = 0;
        for (int i = 0; i < count; ++i) {
            Point lastPoint = getLastPoint();
            int xa = lastPoint.x + dxa[i];
            int ya = lastPoint.y + dya[i];
            int xb = xa + dxb[i];
            int yb = ya + dyb[i];
            xc = xb + dxc[i];
            yc = yb + dyc[i];
            curveTo(xa, ya, xb, yb, xc, yc);
        }
        lineTo(xc + dxd, yc + dyd);
        clearArg();
    }
    
    /**
     * Is equivalent to one rlineto for each pair of arguments beyond
     * the six arguments dxb...dyd needed for the one rrcurveto
     * command. The number of lines is determined from the count of
     * items on the argument stack.
     */
    private void _rlinecurve() {
        int count = (getArgCount() - 6) / 2;
        int[] dxa = new int[count];
        int[] dya = new int[count];
        int dyd = popArg().intValue();
        int dxd = popArg().intValue();
        int dyc = popArg().intValue();
        int dxc = popArg().intValue();
        int dyb = popArg().intValue();
        int dxb = popArg().intValue();
        for (int i = 0; i < count; ++i) {
            dya[count - i - 1] = popArg().intValue();
            dxa[count - i - 1] = popArg().intValue();
        }
        int xa = 0;
        int ya = 0;
        for (int i = 0; i < count; ++i) {
            Point lastPoint = getLastPoint();
            xa = lastPoint.x + dxa[i];
            ya = lastPoint.y + dya[i];
            lineTo(xa, ya);
        }
        int xb = xa + dxb;
        int yb = ya + dyb;
        int xc = xb + dxc;
        int yc = yb + dyc;
        int xd = xc + dxd;
        int yd = yc + dyd;
        curveTo(xb, yb, xc, yc, xd, yd);
        clearArg();
    }
    
    /**
     * Appends one or more Bezier curves to the current point, where
     * the first tangent is vertical and the second tangent is horizontal.
     * This command is the complement of hvcurveto; see the
     * description of hvcurveto for more information.
     */
    private void _vhcurveto() {
        if (getArgCount() % 8 <= 1) {
            int count = getArgCount() / 8;
            int[] dya = new int[count];
            int[] dxb = new int[count];
            int[] dyb = new int[count];
            int[] dxc = new int[count];
            int[] dxd = new int[count];
            int[] dxe = new int[count];
            int[] dye = new int[count];
            int[] dyf = new int[count];
            int dxf = 0;
            if (getArgCount() % 8 == 1) {
                dxf = popArg().intValue();
            }
            for (int i = 0; i < count; ++i) {
                dyf[count - i - 1] = popArg().intValue();
                dye[count - i - 1] = popArg().intValue();
                dxe[count - i - 1] = popArg().intValue();
                dxd[count - i - 1] = popArg().intValue();
                dxc[count - i - 1] = popArg().intValue();
                dyb[count - i - 1] = popArg().intValue();
                dxb[count - i - 1] = popArg().intValue();
                dya[count - i - 1] = popArg().intValue();
            }
            for (int i = 0; i < count; ++i) {
                Point lastPoint = getLastPoint();
                int xa = lastPoint.x;
                int ya = lastPoint.y + dya[i];
                int xb = xa + dxb[i];
                int yb = ya + dyb[i];
                int xc = xb + dxc[i];
                int xd = xc + dxd[i];
                int xe = xd + dxe[i];
                int ye = yb + dye[i];
                int xf = xe + (i == count - 1 ? dxf : 0);
                int yf = ye + dyf[i];
                curveTo(xa, ya, xb, yb, xc, yb);
                curveTo(xd, yb, xe, ye, xf, yf);
            }
        } else {
            int count = getArgCount() / 8;
            int[] dxa = new int[count];
            int[] dxb = new int[count];
            int[] dyb = new int[count];
            int[] dyc = new int[count];
            int[] dyd = new int[count];
            int[] dxe = new int[count];
            int[] dye = new int[count];
            int[] dxf = new int[count];
            int dyf = 0;
            if (getArgCount() % 4 == 1) {
                dyf = popArg().intValue();
            }
            for (int i = 0; i < count; ++i) {
                dxf[count - i - 1] = popArg().intValue();
                dye[count - i - 1] = popArg().intValue();
                dxe[count - i - 1] = popArg().intValue();
                dyd[count - i - 1] = popArg().intValue();
                dyc[count - i - 1] = popArg().intValue();
                dyb[count - i - 1] = popArg().intValue();
                dxb[count - i - 1] = popArg().intValue();
                dxa[count - i - 1] = popArg().intValue();
            }
            int dx3 = popArg().intValue();
            int dy2 = popArg().intValue();
            int dx2 = popArg().intValue();
            int dy1 = popArg().intValue();

            Point lastPoint = getLastPoint();
            int x1 = lastPoint.x;
            int y1 = lastPoint.y + dy1;
            int x2 = x1 + dx2;
            int y2 = y1 + dy2;
            int x3 = x2 + dx3;
            int y3 = y2 + (count == 0 ? dyf : 0);
            curveTo(x1, y1, x2, y2, x3, y3);

            for (int i = 0; i < count; ++i) {
                lastPoint = getLastPoint();
                int xa = lastPoint.x + dxa[i];
                int ya = lastPoint.y;
                int xb = xa + dxb[i];
                int yb = ya + dyb[i];
                int yc = yb + dyc[i];
                int yd = yc + dyd[i];
                int xe = xb + dxe[i];
                int ye = yd + dye[i];
                int xf = xe + dxf[i];
                int yf = ye + (i == count - 1 ? dyf : 0);
                curveTo(xa, ya, xb, yb, xb, yc);
                curveTo(xb, yd, xe, ye, xf, yf);
            }
        }
        clearArg();
    }
    
    /**
     * Appends one or more curves to the current point. If the argument
     * count is a multiple of four, the curve starts and ends vertical. If
     * the argument count is odd, the first curve does not begin with a
     * vertical tangent.
     */
    private void _vvcurveto() {
        int count = getArgCount() / 4;
        int dx1 = 0;
        int[] dya = new int[count];
        int[] dxb = new int[count];
        int[] dyb = new int[count];
        int[] dyc = new int[count];
        for (int i = 0; i < count; ++i) {
            dyc[count - i - 1] = popArg().intValue();
            dyb[count - i - 1] = popArg().intValue();
            dxb[count - i - 1] = popArg().intValue();
            dya[count - i - 1] = popArg().intValue();
        }
        if (getArgCount() == 1) {
            dx1 = popArg().intValue();
        }
        for (int i = 0; i < count; ++i) {
            Point lastPoint = getLastPoint();
            int xa = lastPoint.x + (i == 0 ? dx1 : 0);
            int ya = lastPoint.y + dya[i];
            int xb = xa + dxb[i];
            int yb = ya + dyb[i];
            int yc = yb + dyc[i];
            curveTo(xa, ya, xb, yb, xb, yc);
        }
        
        clearArg();
    }
    
    /**
     * Causes two Bézier curves, as described by the arguments (as
     * shown in Figure 2 below), to be rendered as a straight line when
     * the flex depth is less than fd /100 device pixels, and as curved lines
     * when the flex depth is greater than or equal to fd/100 device
     * pixels.
     */
    private void _flex() {
        
        clearArg();
    }
    
    /**
     * Causes the two curves described by the arguments dx1...dx6 to
     * be rendered as a straight line when the flex depth is less than
     * 0.5 (that is, fd is 50) device pixels, and as curved lines when the
     * flex depth is greater than or equal to 0.5 device pixels.
     */
    private void _hflex() {
        
        clearArg();
    }
    
    /**
     * Causes the two curves described by the arguments to be
     * rendered as a straight line when the flex depth is less than 0.5
     * device pixels, and as curved lines when the flex depth is greater
     * than or equal to 0.5 device pixels.
     */
    private void _hflex1() {
        
        clearArg();
    }
    
    /**
     * Causes the two curves described by the arguments to be
     * rendered as a straight line when the flex depth is less than 0.5
     * device pixels, and as curved lines when the flex depth is greater
     * than or equal to 0.5 device pixels.
     */
    private void _flex1() {
        
        clearArg();
    }
    
    /**
     * Finishes a charstring outline definition, and must be the
     * last operator in a character's outline.
     */
    private void _endchar() {
        endContour();
        clearArg();
        while (_subrStackIndex > 0) {
            SubrPair sp = popSubr();
            _cs = sp.cs;
            _ip = sp.ip;
        }
    }
    
    /**
     * Specifies one or more horizontal stem hints. This allows multiple pairs
     * of numbers, limited by the stack depth, to be used as arguments to a
     * single hstem operator.
     */
    private void _hstem() {
        int pairCount = getArgCount() / 2;
        for (int i = 0; i < pairCount; ++i) {
            _hstems.add(0, popArg().intValue());
            _hstems.add(0, popArg().intValue());
        }

        if (getArgCount() > 0) {
            
            // This will be the width value
            popArg();
        }
    }
    
    /**
     * Specifies one or more vertical stem hints between the x coordinates x
     * and x+dx, where x is relative to the origin of the coordinate axes.
     */
    private void _vstem() {
        int pairCount = getArgCount() / 2;
        for (int i = 0; i < pairCount; ++i) {
            _vstems.add(0, popArg().intValue());
            _vstems.add(0, popArg().intValue());
        }

        if (getArgCount() > 0) {
            
            // This will be the width value
            popArg();
        }
    }
    
    /**
     * Has the same meaning as hstem, except that it must be used in place
     * of hstem if the charstring contains one or more hintmask operators.
     */
    private void _hstemhm() {
        _stemCount += getArgCount() / 2;
        int pairCount = getArgCount() / 2;
        for (int i = 0; i < pairCount; ++i) {
            _hstems.add(0, popArg().intValue());
            _hstems.add(0, popArg().intValue());
        }

        if (getArgCount() > 0) {
            
            // This will be the width value
            popArg();
        }
    }
    
    /**
     * Has the same meaning as vstem, except that it must be used in place
     * of vstem if the charstring contains one or more hintmask operators.
     */
    private void _vstemhm() {
        _stemCount += getArgCount() / 2;
        int pairCount = getArgCount() / 2;
        for (int i = 0; i < pairCount; ++i) {
            _vstems.add(0, popArg().intValue());
            _vstems.add(0, popArg().intValue());
        }

        if (getArgCount() > 0) {
            
            // This will be the width value
            popArg();
        }
    }
    
    /**
     * Specifies which hints are active and which are not active.
     */
    private void _hintmask() {
        _stemCount += getArgCount() / 2;
        _ip += (_stemCount - 1) / 8 + 1;
        clearArg();
    }
    
    /**
     * Specifies the counter spaces to be controlled, and their
     * relative priority.
     */
    private void _cntrmask() {
        _stemCount += getArgCount() / 2;
        _ip += (_stemCount - 1) / 8 + 1;
        clearArg();
    }
    
    /**
     * Returns the absolute value of num.
     */
    private void _abs() {
        double num = popArg().doubleValue();
        pushArg(Math.abs(num));
    }
    
    /**
     * Returns the sum of the two numbers num1 and num2.
     */
    private void _add() {
        double num2 = popArg().doubleValue();
        double num1 = popArg().doubleValue();
        pushArg(num1 + num2);
    }
    
    /**
     * Returns the result of subtracting num2 from num1.
     */
    private void _sub() {
        double num2 = popArg().doubleValue();
        double num1 = popArg().doubleValue();
        pushArg(num1 - num2);
    }
    
    /**
     * Returns the quotient of num1 divided by num2. The result is
     * undefined if overflow occurs and is zero for underflow.
     */
    private void _div() {
        double num2 = popArg().doubleValue();
        double num1 = popArg().doubleValue();
        pushArg(num1 / num2);
    }
    
    /**
     * Returns the negative of num.
     */
    private void _neg() {
        double num = popArg().doubleValue();
        pushArg(-num);
    }
    
    /**
     * Returns a pseudo random number num2 in the range (0,1], that
     * is, greater than zero and less than or equal to one.
     */
    private void _random() {
        pushArg(1.0 - Math.random());
    }
    
    /**
     * Returns the product of num1 and num2. If overflow occurs, the
     * result is undefined, and zero is returned for underflow.
     */
    private void _mul() {
        double num2 = popArg().doubleValue();
        double num1 = popArg().doubleValue();
        pushArg(num1 * num2);
    }
    
    /**
     * Returns the square root of num. If num is negative, the result is
     * undefined.
     */
    private void _sqrt() {
        double num = popArg().doubleValue();
        pushArg(Math.sqrt(num));
    }
    
    /**
     * Removes the top element num from the Type 2 argument stack.
     */
    private void _drop() {
        popArg();
    }
    
    /**
     * Exchanges the top two elements on the argument stack.
     */
    private void _exch() {
        Number num2 = popArg();
        Number num1 = popArg();
        pushArg(num2);
        pushArg(num1);
    }
    
    /**
     * Retrieves the element i from the top of the argument stack and
     * pushes a copy of that element onto that stack. If i is negative,
     * the top element is copied. If i is greater than X, the operation is
     * undefined.
     */
    private void _index() {
        int i = popArg().intValue();
        Number[] nums = new Number[i];
        for (int j = 0; j < i; ++j) {
            nums[j] = popArg();
        }
        for (int j = i - 1; j >= 0; --j) {
            pushArg(nums[j]);
        }
        pushArg(nums[i]);
    }
    
    /**
     * Performs a circular shift of the elements num(N-1) ... num0 on
     * the argument stack by the amount J. Positive J indicates upward
     * motion of the stack; negative J indicates downward motion.
     * The value N must be a non-negative integer, otherwise the
     * operation is undefined.
     */
    private void _roll() {
        int j = popArg().intValue();
        int n = popArg().intValue();
        Number[] nums = new Number[n];
        for (int i = 0; i < n; ++i) {
            nums[i] = popArg();
        }
        for (int i = n - 1; i >= 0; --i) {
            pushArg(nums[(n + i + j) % n]);
        }
    }
    
    /**
     * Duplicates the top element on the argument stack.
     */
    private void _dup() {
        Number any = popArg();
        pushArg(any);
        pushArg(any);
    }
    
    /**
     * Stores val into the transient array at the location given by i.
     */
    private void _put() {
        int i = popArg().intValue();
        Number val = popArg();
        _transientArray[i] = val;
    }
    
    /**
     * Retrieves the value stored in the transient array at the location
     * given by i and pushes the value onto the argument stack. If get
     * is executed prior to put for i during execution of the current
     * charstring, the value returned is undefined.
     */
    private void _get() {
        int i = popArg().intValue();
        pushArg(_transientArray[i]);
    }
    
    /**
     * Puts a 1 on the stack if num1 and num2 are both non-zero, and
     * puts a 0 on the stack if either argument is zero.
     */
    private void _and() {
        double num2 = popArg().doubleValue();
        double num1 = popArg().doubleValue();
        pushArg((num1!=0.0) && (num2!=0.0) ? 1 : 0);
    }
    
    /**
     * Puts a 1 on the stack if either num1 or num2 are non-zero, and
     * puts a 0 on the stack if both arguments are zero.
     */
    private void _or() {
        double num2 = popArg().doubleValue();
        double num1 = popArg().doubleValue();
        pushArg((num1!=0.0) || (num2!=0.0) ? 1 : 0);
    }
    
    /**
     * Returns a 0 if num1 is non-zero; returns a 1 if num1 is zero.
     */
    private void _not() {
        double num1 = popArg().doubleValue();
        pushArg((num1!=0.0) ? 0 : 1);
    }
    
    /**
     * Puts a 1 on the stack if num1 equals num2, otherwise a 0 (zero)
     * is put on the stack.
     */
    private void _eq() {
        double num2 = popArg().doubleValue();
        double num1 = popArg().doubleValue();
        pushArg(num1 == num2 ? 1 : 0);
    }
    
    /**
     * Leaves the value s1 on the stack if v1 ? v2, or leaves s2 on the
     * stack if v1 > v2. The value of s1 and s2 is usually the biased
     * number of a subroutine.
     */
    private void _ifelse() {
        double v2 = popArg().doubleValue();
        double v1 = popArg().doubleValue();
        Number s2 = popArg();
        Number s1 = popArg();
        pushArg(v1 <= v2 ? s1 : s2);
    }
    
    /**
     * Calls a charstring subroutine with index subr# (actually the subr
     * number plus the subroutine bias number, as described in section
     * 2.3) in the Subrs array. Each element of the Subrs array is a
     * charstring encoded like any other charstring. Arguments
     * pushed on the Type 2 argument stack prior to calling the
     * subroutine, and results pushed on this stack by the subroutine,
     * act according to the manner in which the subroutine is coded.
     * Calling an undefined subr (gsubr) has undefined results.
     */
    private void _callsubr() {
        int bias;
        int subrsCount = _localSubrIndex.getCount();
        if (subrsCount < 1240) {
            bias = 107;
        } else if (subrsCount < 33900) {
            bias = 1131;
        } else {
            bias = 32768;
        }
        int i = popArg().intValue();
        int offset = _localSubrIndex.getOffset(i + bias) - 1;
        pushSubr(new SubrPair(_cs, _ip));
        _cs = _localSubrs;
        _ip = offset;
    }
    
    /**
     * Operates in the same manner as callsubr except that it calls a
     * global subroutine.
     */
    private void _callgsubr() {
        int bias;
        int subrsCount = _globalSubrIndex.getCount();
        if (subrsCount < 1240) {
            bias = 107;
        } else if (subrsCount < 33900) {
            bias = 1131;
        } else {
            bias = 32768;
        }
        int i = popArg().intValue();
        int offset = _globalSubrIndex.getOffset(i + bias) - 1;
        pushSubr(new SubrPair(_cs, _ip));
        _cs = _globalSubrs;
        _ip = offset;
    }
    
    /**
     * Returns from either a local or global charstring subroutine, and
     * continues execution after the corresponding call(g)subr.
     */
    private void _return() {
        SubrPair sp = popSubr();
        _cs = sp.cs;
        _ip = sp.ip;
    }
    
    public Point[] execute(CharstringType2 cs) {
        _localSubrIndex = cs.getFont().getLocalSubrIndex();
        _globalSubrIndex = cs.getFont().getTable().getGlobalSubrIndex();
        _localSubrs = new CharstringType2(
                null,
                0,
                "Local subrs",
                _localSubrIndex.getData(),
                _localSubrIndex.getOffset(0) - 1,
                _localSubrIndex.getDataLength());
        _globalSubrs = new CharstringType2(
                null,
                0,
                "Global subrs",
                _globalSubrIndex.getData(),
                _globalSubrIndex.getOffset(0) - 1,
                _globalSubrIndex.getDataLength());
        _cs = cs;

        _hstems = new ArrayList<>();
        _vstems = new ArrayList<>();

        _points = new ArrayList<>();
        _ip = _cs.getFirstIndex();
        while (_cs.moreBytes(_ip)) {
            while (_cs.isOperandAtIndex(_ip)) {
                pushArg(_cs.operandAtIndex(_ip));
                _ip = _cs.nextOperandIndex(_ip);
            }
            int operator = _cs.byteAtIndex(_ip++);
            if (operator == 12) {
                operator = _cs.byteAtIndex(_ip++);

                // Two-byte operators
                switch (operator) {
                case T2Mnemonic.AND:
                    _and();
                    break;
                case T2Mnemonic.OR:
                    _or();
                    break;
                case T2Mnemonic.NOT:
                    _not();
                    break;
                case T2Mnemonic.ABS:
                    _abs();
                    break;
                case T2Mnemonic.ADD:
                    _add();
                    break;
                case T2Mnemonic.SUB:
                    _sub();
                    break;
                case T2Mnemonic.DIV:
                    _div();
                    break;
                case T2Mnemonic.NEG:
                    _neg();
                    break;
                case T2Mnemonic.EQ:
                    _eq();
                    break;
                case T2Mnemonic.DROP:
                    _drop();
                    break;
                case T2Mnemonic.PUT:
                    _put();
                    break;
                case T2Mnemonic.GET:
                    _get();
                    break;
                case T2Mnemonic.IFELSE:
                    _ifelse();
                    break;
                case T2Mnemonic.RANDOM:
                    _random();
                    break;
                case T2Mnemonic.MUL:
                    _mul();
                    break;
                case T2Mnemonic.SQRT:
                    _sqrt();
                    break;
                case T2Mnemonic.DUP:
                    _dup();
                    break;
                case T2Mnemonic.EXCH:
                    _exch();
                    break;
                case T2Mnemonic.INDEX:
                    _index();
                    break;
                case T2Mnemonic.ROLL:
                    _roll();
                    break;
                case T2Mnemonic.HFLEX:
                    _hflex();
                    break;
                case T2Mnemonic.FLEX:
                    _flex();
                    break;
                case T2Mnemonic.HFLEX1:
                    _hflex1();
                    break;
                case T2Mnemonic.FLEX1:
                    _flex1();
                    break;
                default:
                    //throw new Exception();
                    return null;
                }
            } else {

                // One-byte operators
                switch (operator) {
                case T2Mnemonic.HSTEM:
                    _hstem();
                    break;
                case T2Mnemonic.VSTEM:
                    _vstem();
                    break;
                case T2Mnemonic.VMOVETO:
                    _vmoveto();
                    break;
                case T2Mnemonic.RLINETO:
                    _rlineto();
                    break;
                case T2Mnemonic.HLINETO:
                    _hlineto();
                    break;
                case T2Mnemonic.VLINETO:
                    _vlineto();
                    break;
                case T2Mnemonic.RRCURVETO:
                    _rrcurveto();
                    break;
                case T2Mnemonic.CALLSUBR:
                    _callsubr();
                    break;
                case T2Mnemonic.RETURN:
                    _return();
                    break;
                case T2Mnemonic.ENDCHAR:
                    _endchar();
                    break;
                case T2Mnemonic.HSTEMHM:
                    _hstemhm();
                    break;
                case T2Mnemonic.HINTMASK:
                    _hintmask();
                    break;
                case T2Mnemonic.CNTRMASK:
                    _cntrmask();
                    break;
                case T2Mnemonic.RMOVETO:
                    _rmoveto();
                    break;
                case T2Mnemonic.HMOVETO:
                    _hmoveto();
                    break;
                case T2Mnemonic.VSTEMHM:
                    _vstemhm();
                    break;
                case T2Mnemonic.RCURVELINE:
                    _rcurveline();
                    break;
                case T2Mnemonic.RLINECURVE:
                    _rlinecurve();
                    break;
                case T2Mnemonic.VVCURVETO:
                    _vvcurveto();
                    break;
                case T2Mnemonic.HHCURVETO:
                    _hhcurveto();
                    break;
                case T2Mnemonic.CALLGSUBR:
                    _callgsubr();
                    break;
                case T2Mnemonic.VHCURVETO:
                    _vhcurveto();
                    break;
                case T2Mnemonic.HVCURVETO:
                    _hvcurveto();
                    break;
                default:
                    //throw new Exception();
                    return null;
                }
            }
        }
        Point[] pointArray = new Point[_points.size()];
        _points.toArray(pointArray);
        return pointArray;
    }

    /**
     * The number of arguments on the argument stack
     */
    private int getArgCount() {
        return _argStackIndex;
    }
    
    /**
     * Pop a value off the argument stack
     */
    private Number popArg() {
        if (DEBUG) {
            System.err.printf(
                    "T2I: popArg: %s %s%n",
                    _argStack[_argStackIndex - 1],
                    java.util.Arrays.copyOfRange(_argStack, 0, _argStackIndex - 1));
        }
        return _argStack[--_argStackIndex];
    }

    /**
     * Push a value on to the argument stack
     */
    private void pushArg(Number n) {
        _argStack[_argStackIndex++] = n;
        if (DEBUG) {
            System.err.printf(
                    "T2I: pushArg: %s %s%n",
                    n,
                    java.util.Arrays.copyOfRange(_argStack, 0, _argStackIndex - 1));
        }
    }
    
    /**
     * Pop a value off the subroutine stack
     */
    private SubrPair popSubr() {
        return _subrStack[--_subrStackIndex];
    }

    /**
     * Push a value on to the subroutine stack
     */
    private void pushSubr(SubrPair sp) {
        _subrStack[_subrStackIndex] = sp;
        _subrStackIndex++;
    }
    
    /**
     * Clear the argument stack
     */
    private void clearArg() {
        _argStackIndex = 0;
    }
    
    private Point getLastPoint() {
        int size = _points.size();
        if (size > 0) {
            return _points.get(size - 1);
        } else {
            return new Point(0, 0, true, false);
        }
    }
    
    private void moveTo(int x, int y) {
        endContour();
        _points.add(new Point(x, y, true, false));
    }
    
    private void lineTo(int x, int y) {
        _points.add(new Point(x, y, true, false));
    }
    
    private void curveTo(int cx1, int cy1, int cx2, int cy2, int x, int y) {
        _points.add(new Point(cx1, cy1, false, false));
        _points.add(new Point(cx2, cy2, false, false));
        _points.add(new Point(x, y, true, false));
    }
    
    private void endContour() {
        Point lastPoint = getLastPoint();
        if (lastPoint != null) {
            lastPoint.endOfContour = true;
        }
    }
}
