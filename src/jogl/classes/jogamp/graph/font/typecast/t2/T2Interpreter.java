/*
 * $Id: T2Interpreter.java,v 1.2 2007-07-26 11:10:18 davidsch Exp $
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

import java.util.ArrayList;

import jogamp.graph.font.typecast.ot.Point;
import jogamp.graph.font.typecast.ot.table.CharstringType2;



/**
 * Type 2 Charstring Interpreter.  Operator descriptions are quoted from
 * Adobe's Type 2 Charstring Format document -- 5117.Type2.pdf.
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 * @version $Id: T2Interpreter.java,v 1.2 2007-07-26 11:10:18 davidsch Exp $
 */
public class T2Interpreter {

    private static final int ARGUMENT_STACK_LIMIT = 48;
    private static final int SUBR_STACK_LIMIT = 10;
    private static final int TRANSIENT_ARRAY_ELEMENT_COUNT = 32;

    private final Number[] _argStack = new Number[ARGUMENT_STACK_LIMIT];
    private int _argStackIndex = 0;
    private final int[] _subrStack = new int[SUBR_STACK_LIMIT];
    private int _subrStackIndex = 0;
    private final Number[] _transientArray = new Number[TRANSIENT_ARRAY_ELEMENT_COUNT];

    private ArrayList<Point> _points;

    /** Creates a new instance of T2Interpreter */
    public T2Interpreter() {
    }

    /**
     * Moves the current point to a position at the relative coordinates
     * (dx1, dy1).
     */
    private void _rmoveto() {
        final int dy1 = popArg().intValue();
        final int dx1 = popArg().intValue();
        clearArg();
        final Point lastPoint = getLastPoint();
        moveTo(lastPoint.x + dx1, lastPoint.y + dy1);
    }

    /**
     * Moves the current point dx1 units in the horizontal direction.
     */
    private void _hmoveto() {
        final int dx1 = popArg().intValue();
        clearArg();
        final Point lastPoint = getLastPoint();
        moveTo(lastPoint.x + dx1, lastPoint.y);
    }

    /**
     * Moves the current point dy1 units in the vertical direction.
     */
    private void _vmoveto() {
        final int dy1 = popArg().intValue();
        clearArg();
        final Point lastPoint = getLastPoint();
        moveTo(lastPoint.x, lastPoint.y + dy1);
    }

    /**
     * Appends a line from the current point to a position at the
     * relative coordinates dxa, dya. Additional rlineto operations are
     * performed for all subsequent argument pairs. The number of
     * lines is determined from the number of arguments on the stack.
     */
    private void _rlineto() {
        final int count = getArgCount() / 2;
        final int[] dx = new int[count];
        final int[] dy = new int[count];
        for (int i = 0; i < count; ++i) {
            dy[count - i - 1] = popArg().intValue();
            dx[count - i - 1] = popArg().intValue();
        }
        for (int i = 0; i < count; ++i) {
            final Point lastPoint = getLastPoint();
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
        final int count = getArgCount();
        final Number[] nums = new Number[count];
        for (int i = 0; i < count; ++i) {
            nums[count - i - 1] = popArg();
        }
        for (int i = 0; i < count; ++i) {
            final Point lastPoint = getLastPoint();
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
        final int count = getArgCount();
        final Number[] nums = new Number[count];
        for (int i = 0; i < count; ++i) {
            nums[count - i - 1] = popArg();
        }
        for (int i = 0; i < count; ++i) {
            final Point lastPoint = getLastPoint();
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
        final int count = getArgCount() / 6;
        final int[] dxa = new int[count];
        final int[] dya = new int[count];
        final int[] dxb = new int[count];
        final int[] dyb = new int[count];
        final int[] dxc = new int[count];
        final int[] dyc = new int[count];
        for (int i = 0; i < count; ++i) {
            dyc[count - i - 1] = popArg().intValue();
            dxc[count - i - 1] = popArg().intValue();
            dyb[count - i - 1] = popArg().intValue();
            dxb[count - i - 1] = popArg().intValue();
            dya[count - i - 1] = popArg().intValue();
            dxa[count - i - 1] = popArg().intValue();
        }
        for (int i = 0; i < count; ++i) {
            final Point lastPoint = getLastPoint();
            final int xa = lastPoint.x + dxa[i];
            final int ya = lastPoint.y + dya[i];
            final int xb = xa + dxb[i];
            final int yb = ya + dyb[i];
            final int xc = xb + dxc[i];
            final int yc = yb + dyc[i];
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
        final int count = getArgCount() / 4;
        int dy1 = 0;
        final int[] dxa = new int[count];
        final int[] dxb = new int[count];
        final int[] dyb = new int[count];
        final int[] dxc = new int[count];
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
            final Point lastPoint = getLastPoint();
            final int xa = lastPoint.x + dxa[i];
            final int ya = lastPoint.y + (i == 0 ? dy1 : 0);
            final int xb = xa + dxb[i];
            final int yb = ya + dyb[i];
            final int xc = xb + dxc[i];
            final int yc = yb;
            curveTo(xa, ya, xb, yb, xc, yc);
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
            final int count = getArgCount() / 8;
            final int[] dxa = new int[count];
            final int[] dxb = new int[count];
            final int[] dyb = new int[count];
            final int[] dyc = new int[count];
            final int[] dyd = new int[count];
            final int[] dxe = new int[count];
            final int[] dye = new int[count];
            final int[] dxf = new int[count];
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
                final Point lastPoint = getLastPoint();
                final int xa = lastPoint.x + dxa[i];
                final int ya = lastPoint.y;
                final int xb = xa + dxb[i];
                final int yb = ya + dyb[i];
                final int xc = xb;
                final int yc = yb + dyc[i];
                final int xd = xc;
                final int yd = yc + dyd[i];
                final int xe = xd + dxe[i];
                final int ye = yd + dye[i];
                final int xf = xe + dxf[i];
                final int yf = ye + dyf;
                curveTo(xa, ya, xb, yb, xc, yc);
                curveTo(xd, yd, xe, ye, xf, yf);
            }
        } else {
            final int count = getArgCount() / 8;
            final int[] dya = new int[count];
            final int[] dxb = new int[count];
            final int[] dyb = new int[count];
            final int[] dxc = new int[count];
            final int[] dxd = new int[count];
            final int[] dxe = new int[count];
            final int[] dye = new int[count];
            final int[] dyf = new int[count];
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
            /**
             * Not using the 'popped' arguments,
             * hence simply pop them from stack!
             *
            final int dy3 = popArg().intValue();
            final int dy2 = popArg().intValue();
            final int dx2 = popArg().intValue();
            final int dx1 = popArg().intValue();
            */
            popArg();
            popArg();
            popArg();
            popArg();

            for (int i = 0; i < count; ++i) {
                final Point lastPoint = getLastPoint();
                final int xa = lastPoint.x;
                final int ya = lastPoint.y + dya[i];
                final int xb = xa + dxb[i];
                final int yb = ya + dyb[i];
                final int xc = xb + dxc[i];
                final int yc = yb;
                final int xd = xc + dxd[i];
                final int yd = yc;
                final int xe = xd + dxe[i];
                final int ye = yd + dye[i];
                final int xf = xe + dxf;
                final int yf = ye + dyf[i];
                curveTo(xa, ya, xb, yb, xc, yc);
                curveTo(xd, yd, xe, ye, xf, yf);

                // What on earth do we do with dx1, dx2, dy2 and dy3?
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
        final int count = (getArgCount() - 2) / 6;
        final int[] dxa = new int[count];
        final int[] dya = new int[count];
        final int[] dxb = new int[count];
        final int[] dyb = new int[count];
        final int[] dxc = new int[count];
        final int[] dyc = new int[count];
        final int dyd = popArg().intValue();
        final int dxd = popArg().intValue();
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
            final Point lastPoint = getLastPoint();
            final int xa = lastPoint.x + dxa[i];
            final int ya = lastPoint.y + dya[i];
            final int xb = xa + dxb[i];
            final int yb = ya + dyb[i];
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
        final int count = (getArgCount() - 6) / 2;
        final int[] dxa = new int[count];
        final int[] dya = new int[count];
        final int dyd = popArg().intValue();
        final int dxd = popArg().intValue();
        final int dyc = popArg().intValue();
        final int dxc = popArg().intValue();
        final int dyb = popArg().intValue();
        final int dxb = popArg().intValue();
        for (int i = 0; i < count; ++i) {
            dya[count - i - 1] = popArg().intValue();
            dxa[count - i - 1] = popArg().intValue();
        }
        int xa = 0;
        int ya = 0;
        for (int i = 0; i < count; ++i) {
            final Point lastPoint = getLastPoint();
            xa = lastPoint.x + dxa[i];
            ya = lastPoint.y + dya[i];
            lineTo(xa, ya);
        }
        final int xb = xa + dxb;
        final int yb = ya + dyb;
        final int xc = xb + dxc;
        final int yc = yb + dyc;
        final int xd = xc + dxd;
        final int yd = yc + dyd;
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
            final int count = getArgCount() / 8;
            final int[] dya = new int[count];
            final int[] dxb = new int[count];
            final int[] dyb = new int[count];
            final int[] dxc = new int[count];
            final int[] dxd = new int[count];
            final int[] dxe = new int[count];
            final int[] dye = new int[count];
            final int[] dyf = new int[count];
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
                final Point lastPoint = getLastPoint();
                final int xa = lastPoint.x;
                final int ya = lastPoint.y + dya[i];
                final int xb = xa + dxb[i];
                final int yb = ya + dyb[i];
                final int xc = xb + dxc[i];
                final int yc = yb;
                final int xd = xc + dxd[i];
                final int yd = yc;
                final int xe = xd + dxe[i];
                final int ye = yd + dye[i];
                final int xf = xe + dxf;
                final int yf = ye + dyf[i];
                curveTo(xa, ya, xb, yb, xc, yc);
                curveTo(xd, yd, xe, ye, xf, yf);
            }
        } else {
            final int foo = 0;
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

        clearArg();
    }

    /**
     * Causes two Bezier curves, as described by the arguments (as
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
    }

    private void _hstem() {

        clearArg();
    }

    private void _vstem() {

        clearArg();
    }

    private void _hstemhm() {

        clearArg();
    }

    private void _vstemhm() {

        clearArg();
    }

    private void _hintmask() {

        clearArg();
    }

    private void _cntrmask() {

        clearArg();
    }

    /**
     * Returns the absolute value of num.
     */
    private void _abs() {
        final double num = popArg().doubleValue();
        pushArg(Math.abs(num));
    }

    /**
     * Returns the sum of the two numbers num1 and num2.
     */
    private void _add() {
        final double num2 = popArg().doubleValue();
        final double num1 = popArg().doubleValue();
        pushArg(num1 + num2);
    }

    /**
     * Returns the result of subtracting num2 from num1.
     */
    private void _sub() {
        final double num2 = popArg().doubleValue();
        final double num1 = popArg().doubleValue();
        pushArg(num1 - num2);
    }

    /**
     * Returns the quotient of num1 divided by num2. The result is
     * undefined if overflow occurs and is zero for underflow.
     */
    private void _div() {
        final double num2 = popArg().doubleValue();
        final double num1 = popArg().doubleValue();
        pushArg(num1 / num2);
    }

    /**
     * Returns the negative of num.
     */
    private void _neg() {
        final double num = popArg().doubleValue();
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
        final double num2 = popArg().doubleValue();
        final double num1 = popArg().doubleValue();
        pushArg(num1 * num2);
    }

    /**
     * Returns the square root of num. If num is negative, the result is
     * undefined.
     */
    private void _sqrt() {
        final double num = popArg().doubleValue();
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
        final Number num2 = popArg();
        final Number num1 = popArg();
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
        final int i = popArg().intValue();
        final Number[] nums = new Number[i];
        for (int j = 0; j < i; ++j) {
            nums[j] = popArg();
        }
        for (int j = i - 1; j >= 0; --j) {
            pushArg(nums[j]);
        }
        pushArg(nums[i]);
    }

    /**
     * Performs a circular shift of the elements num(Nx1) ... num0 on
     * the argument stack by the amount J. Positive J indicates upward
     * motion of the stack; negative J indicates downward motion.
     * The value N must be a non-negative integer, otherwise the
     * operation is undefined.
     */
    private void _roll() {
        final int j = popArg().intValue();
        final int n = popArg().intValue();
        final Number[] nums = new Number[n];
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
        final Number any = popArg();
        pushArg(any);
        pushArg(any);
    }

    /**
     * Stores val into the transient array at the location given by i.
     */
    private void _put() {
        final int i = popArg().intValue();
        final Number val = popArg();
        _transientArray[i] = val;
    }

    /**
     * Retrieves the value stored in the transient array at the location
     * given by i and pushes the value onto the argument stack. If get
     * is executed prior to put for i during execution of the current
     * charstring, the value returned is undefined.
     */
    private void _get() {
        final int i = popArg().intValue();
        pushArg(_transientArray[i]);
    }

    /**
     * Puts a 1 on the stack if num1 and num2 are both non-zero, and
     * puts a 0 on the stack if either argument is zero.
     */
    private void _and() {
        final double num2 = popArg().doubleValue();
        final double num1 = popArg().doubleValue();
        pushArg((num1!=0.0) && (num2!=0.0) ? 1 : 0);
    }

    /**
     * Puts a 1 on the stack if either num1 or num2 are non-zero, and
     * puts a 0 on the stack if both arguments are zero.
     */
    private void _or() {
        final double num2 = popArg().doubleValue();
        final double num1 = popArg().doubleValue();
        pushArg((num1!=0.0) || (num2!=0.0) ? 1 : 0);
    }

    /**
     * Returns a 0 if num1 is non-zero; returns a 1 if num1 is zero.
     */
    private void _not() {
        final double num1 = popArg().doubleValue();
        pushArg((num1!=0.0) ? 0 : 1);
    }

    /**
     * Puts a 1 on the stack if num1 equals num2, otherwise a 0 (zero)
     * is put on the stack.
     */
    private void _eq() {
        final double num2 = popArg().doubleValue();
        final double num1 = popArg().doubleValue();
        pushArg(num1 == num2 ? 1 : 0);
    }

    /**
     * Leaves the value s1 on the stack if v1 ? v2, or leaves s2 on the
     * stack if v1 > v2. The value of s1 and s2 is usually the biased
     * number of a subroutine.
     */
    private void _ifelse() {
        final double v2 = popArg().doubleValue();
        final double v1 = popArg().doubleValue();
        final Number s2 = popArg();
        final Number s1 = popArg();
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

    }

    /**
     * Operates in the same manner as callsubr except that it calls a
     * global subroutine.
     */
    private void _callgsubr() {

    }

    /**
     * Returns from either a local or global charstring subroutine, and
     * continues execution after the corresponding call(g)subr.
     */
    private void _return() {

    }

    public Point[] execute(final CharstringType2 cs) {
        _points = new ArrayList<Point>();
        cs.resetIP();
        while (cs.moreBytes()) {
            while (cs.isOperandAtIndex()) {
                pushArg(cs.nextOperand());
            }
            int operator = cs.nextByte();
            if (operator == 12) {
                operator = cs.nextByte();

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
        final Point[] pointArray = new Point[_points.size()];
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
        return _argStack[--_argStackIndex];
    }

    /**
     * Push a value on to the argument stack
     */
    private void pushArg(final Number n) {
        _argStack[_argStackIndex++] = n;
    }

    /**
     * Pop a value off the subroutine stack
     */
    private int popSubr() {
        return _subrStack[--_subrStackIndex];
    }

    /**
     * Push a value on to the subroutine stack
     */
    private void pushSubr(final int n) {
        _subrStack[_subrStackIndex++] = n;
    }

    /**
     * Clear the argument stack
     */
    private void clearArg() {
        _argStackIndex = 0;
    }

    private Point getLastPoint() {
        final int size = _points.size();
        if (size > 0) {
            return _points.get(size - 1);
        } else {
            return new Point(0, 0, true, false);
        }
    }

    private void moveTo(final int x, final int y) {
        endContour();
        _points.add(new Point(x, y, true, false));
    }

    private void lineTo(final int x, final int y) {
        _points.add(new Point(x, y, true, false));
    }

    private void curveTo(final int cx1, final int cy1, final int cx2, final int cy2, final int x, final int y) {
        _points.add(new Point(cx1, cy1, false, false));
        _points.add(new Point(cx2, cy2, false, false));
        _points.add(new Point(x, y, true, false));
    }

    private void endContour() {
        final Point lastPoint = getLastPoint();
        if (lastPoint != null) {
            lastPoint.endOfContour = true;
        }
    }
}
