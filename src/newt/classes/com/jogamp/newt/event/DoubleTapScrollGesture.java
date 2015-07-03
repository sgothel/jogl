/**
 * Copyright 2013 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package com.jogamp.newt.event;

import com.jogamp.common.util.PropertyAccess;

import jogamp.newt.Debug;

/**
 * 2 pointer scroll/rotate gesture handler processing {@link MouseEvent}s
 * while producing {@link MouseEvent#EVENT_MOUSE_WHEEL_MOVED} events if gesture is completed.
 * <p>
 * Criteria related to parameters:
 * <pre>
 *    - doubleTapSlop (scaled in pixels):
 *       - Max 2 finger distance to start 'scroll' mode
 *       - Max. distance diff of current 2-pointer middle and initiated 2-pointer middle.
 *
 *    - touchSlop (scaled in pixels):
 *       - Min. movement w/ 2 pointer within ScaledDoubleTapSlop starting 'scroll' mode
 *
 *    - Avoid computation if not within gesture, especially for MOVE/DRAG
 *
 *    - Only allow gesture to start with PRESS
 *
 *    - Leave gesture completely with RELEASE of both/all fingers, or dist-diff exceeds doubleTapSlop
 *
 *    - Tolerate temporary lift 1 of 2 pointer
 *
 *     - Always validate pointer-id
 * </pre>
 * </p>
 * Implementation uses a n-state to get detect gesture:
 * <p>
 * <table border="1">
 *   <tr><th>from</th>      <th>to</th>   <th>action</th></tr>
 *   <tr><td>NONE</td>      <td>1PRESS</td>    <td>1-pointer-pressed</td></tr>
 *   <tr><td>1PRESS</td>    <td>2PRESS_T</td>  <td>2-pointer-pressed within doubleTapSlope</td></tr>
 *   <tr><td>2PRESS_T</td>  <td>SCROLL</td>    <td>2-pointer dragged, dist-diff within doubleTapSlop and scrollLen >= scrollSlop</td></tr>
 *   <tr><td>2PRESS_C</td>  <td>SCROLL</td>    <td>2-pointer dragged, dist-diff within doubleTapSlop</td></tr>
 *   <tr><td>SCROLL</td>    <td>SCROLL</td>    <td>2-pointer dragged, dist-diff within doubleTapSlop</td></tr>
 * </table>
 * State ST_2PRESS_C merely exist to pick up gesture after one pointer has been lost temporarily.
 * </p>
 * <p>
 * {@link #isWithinGesture()} returns gestureState >= 2PRESS_C
 * </p>
 */
public class DoubleTapScrollGesture implements GestureHandler {
    /** Scroll threshold in pixels (fallback), defaults to 16 pixels. Can be overriden by integer property <code>newt.event.scroll_slop_pixel</code>.*/
    public static final int SCROLL_SLOP_PIXEL;
    /** Two pointer 'double tap' slop in pixels (fallback), defaults to 104 pixels. Can be overriden by integer property <code>newt.event.double_tap_slop_pixel</code>.*/
    public static final int DOUBLE_TAP_SLOP_PIXEL;

    /** Scroll threshold in millimeter, defaults to 3 mm. Can be overriden by integer property <code>newt.event.scroll_slop_mm</code>.*/
    public static final float SCROLL_SLOP_MM;
    /** Two pointer 'double tap' slop in millimeter, defaults to 20 mm. Can be overriden by integer property <code>newt.event.double_tap_slop_mm</code>.*/
    public static final float DOUBLE_TAP_SLOP_MM;

    static {
        Debug.initSingleton();

        SCROLL_SLOP_PIXEL = PropertyAccess.getIntProperty("newt.event.scroll_slop_pixel", true, 16);
        DOUBLE_TAP_SLOP_PIXEL = PropertyAccess.getIntProperty("newt.event.double_tap_slop_pixel", true, 104);
        SCROLL_SLOP_MM = PropertyAccess.getIntProperty("newt.event.scroll_slop_mm", true, 3);
        DOUBLE_TAP_SLOP_MM = PropertyAccess.getIntProperty("newt.event.double_tap_slop_mm", true, 20);
    }

    private static final int ST_NONE = 0;
    private static final int ST_1PRESS = 1;
    private static final int ST_2PRESS_T = 2;
    private static final int ST_2PRESS_C = 3;
    private static final int ST_SCROLL = 4;

    private final int scrollSlop, scrollSlopSquare, doubleTapSlop, doubleTapSlopSquare;
    private final float[] scrollDistance = new float[] { 0f, 0f };
    private final int[] pIds = new int[] { -1, -1 };
    /** See class docu */
    private int gestureState;
    private int sqStartDist;
    private int lastX, lastY;
    private int pointerDownCount;
    private MouseEvent hitGestureEvent;

    private static final int getSquareDistance(final float x1, final float y1, final float x2, final float y2) {
        final int deltaX = (int) x1 - (int) x2;
        final int deltaY = (int) y1 - (int) y2;
        return deltaX * deltaX + deltaY * deltaY;
    }

    private int gesturePointers(final MouseEvent e, final int excludeIndex) {
        int j = 0;
        for(int i=e.getPointerCount()-1; i>=0; i--) {
            if( excludeIndex != i ) {
                final int id = e.getPointerId(i);
                if( pIds[0] == id || pIds[1] == id ) {
                    j++;
                }
            }
        }
        return j;
    }

    /**
     * scaledScrollSlop < scaledDoubleTapSlop
     * @param scaledScrollSlop Distance a pointer can wander before we think the user is scrolling in <i>pixels</i>.
     * @param scaledDoubleTapSlop Distance in <i>pixels</i> between the first touch and second touch to still be considered a double tap.
     */
    public DoubleTapScrollGesture(final int scaledScrollSlop, final int scaledDoubleTapSlop) {
        scrollSlop = scaledScrollSlop;
        scrollSlopSquare = scaledScrollSlop * scaledScrollSlop;
        doubleTapSlop = scaledDoubleTapSlop;
        doubleTapSlopSquare = scaledDoubleTapSlop * scaledDoubleTapSlop;
        pointerDownCount = 0;
        clear(true);
        if(DEBUG) {
            System.err.println("DoubleTapScroll    scrollSlop (scaled) "+scrollSlop);
            System.err.println("DoubleTapScroll doubleTapSlop (scaled) "+doubleTapSlop);
        }
    }

    @Override
    public String toString() {
        return "DoubleTapScroll[state "+gestureState+", in "+isWithinGesture()+", has "+(null!=hitGestureEvent)+", pc "+pointerDownCount+"]";
    }

    @Override
    public void clear(final boolean clearStarted) {
        scrollDistance[0] = 0f;
        scrollDistance[1] = 0f;
        hitGestureEvent = null;
        if( clearStarted ) {
            gestureState = ST_NONE;
            sqStartDist = 0;
            pIds[0] = -1;
            pIds[1] = -1;
            lastX = 0;
            lastY = 0;
        }
    }

    @Override
    public boolean isWithinGesture() {
        return ST_2PRESS_C <= gestureState;
    }

    @Override
    public boolean hasGesture() {
        return null != hitGestureEvent;
    }

    @Override
    public InputEvent getGestureEvent() {
        if( null != hitGestureEvent ) {
            final MouseEvent ge = hitGestureEvent;
            int modifiers = ge.getModifiers();
            final float[] rotationXYZ = ge.getRotation();
            rotationXYZ[0] = scrollDistance[0] / scrollSlop;
            rotationXYZ[1] = scrollDistance[1] / scrollSlop;
            if( rotationXYZ[0]*rotationXYZ[0] > rotationXYZ[1]*rotationXYZ[1] ) {
                // Horizontal scroll -> SHIFT
                modifiers |= com.jogamp.newt.event.InputEvent.SHIFT_MASK;
            }
            return new MouseEvent(MouseEvent.EVENT_MOUSE_WHEEL_MOVED, ge.getSource(), ge.getWhen(), modifiers,
                                  ge.getAllPointerTypes(), ge.getAllPointerIDs(),
                                  ge.getAllX(), ge.getAllY(), ge.getAllPressures(), ge.getMaxPressure(),
                                  ge.getButton(), ge.getClickCount(), rotationXYZ, scrollSlop);
        }
        return null;
    }

    public final float[] getScrollDistanceXY() {
        return scrollDistance;
    }

    @Override
    public boolean process(final InputEvent in) {
        if( null != hitGestureEvent || !(in instanceof MouseEvent) ) {
            return true;
        }
        final MouseEvent pe = (MouseEvent)in;
        if( pe.getPointerType(0).getPointerClass() != MouseEvent.PointerClass.Onscreen ) {
            return false;
        }
        pointerDownCount = pe.getPointerCount();
        final int eventType = pe.getEventType();
        final int x0 = pe.getX(0);
        final int y0 = pe.getY(0);
        switch ( eventType ) {
            case MouseEvent.EVENT_MOUSE_PRESSED: {
                int gPtr = 0;
                if( ST_NONE == gestureState && 1 == pointerDownCount ) {
                    pIds[0] = pe.getPointerId(0);
                    pIds[1] = -1;
                    gestureState = ST_1PRESS;
                } else if( ST_NONE < gestureState && 2 == pointerDownCount && 1 == gesturePointers(pe, 0) /* w/o pressed pointer */ ) {
                    final int x1 = pe.getX(1);
                    final int y1 = pe.getY(1);
                    final int xm = (x0+x1)/2;
                    final int ym = (y0+y1)/2;

                    if( ST_1PRESS == gestureState ) {
                        final int sqDist = getSquareDistance(x0, y0, x1, y1);
                        final boolean isDistWithinDoubleTapSlop = sqDist < doubleTapSlopSquare;
                        if( isDistWithinDoubleTapSlop ) {
                            // very first 2-finger touch-down
                            gPtr = 2;
                            pIds[0] = pe.getPointerId(0);
                            pIds[1] = pe.getPointerId(1);
                            lastX = xm;
                            lastY = ym;
                            sqStartDist = sqDist;
                            gestureState = ST_2PRESS_T;
                        }
                        if(DEBUG) {
                            final int dist = (int)Math.round(Math.sqrt(sqDist));
                            System.err.println(this+".pressed.1: dist "+dist+", gPtr "+gPtr+", distWithin2DTSlop "+isDistWithinDoubleTapSlop+", last "+lastX+"/"+lastY+", "+pe);
                        }
                    } else if( ST_2PRESS_C == gestureState ) { // pick up gesture after temp loosing one pointer
                        gPtr = gesturePointers(pe, -1);
                        if( 2 == gPtr ) {
                            // same pointers re-touch-down
                            lastX = xm;
                            lastY = ym;
                        } else {
                            // other 2 pointers .. should rarely happen!
                            clear(true);
                        }
                    }
                }
                if(DEBUG) {
                    System.err.println(this+".pressed: gPtr "+gPtr+", this "+lastX+"/"+lastY+", "+pe);
                }
            } break;

            case MouseEvent.EVENT_MOUSE_RELEASED: {
                pointerDownCount--; // lifted
                final int gPtr = gesturePointers(pe, 0); // w/o lifted pointer
                if ( 1 == gPtr ) {
                    // tolerate lifting 1 of 2 gesture pointers temporary
                    gestureState = ST_2PRESS_C;
                } else if( 0 == gPtr ) {
                    // all lifted
                    clear(true);
                }
                if(DEBUG) {
                    System.err.println(this+".released: gPtr "+gPtr+", "+pe);
                }
            } break;

            case MouseEvent.EVENT_MOUSE_DRAGGED: {
                if( 2 == pointerDownCount && ST_1PRESS < gestureState ) {
                    final int gPtr = gesturePointers(pe, -1);
                    if( 2 == gPtr ) {
                        // same pointers
                        final int x1 = pe.getX(1);
                        final int y1 = pe.getY(1);
                        final int xm = (x0+x1)/2;
                        final int ym = (y0+y1)/2;
                        final int sqDist = getSquareDistance(x0, y0, x1, y1);
                        final boolean isDistDiffWithinDoubleTapSlop = Math.abs(sqDist - sqStartDist) <= doubleTapSlopSquare;
                        if( isDistDiffWithinDoubleTapSlop ) {
                            switch( gestureState ) {
                                case ST_2PRESS_T: {
                                    final int sqScrollLen = getSquareDistance(lastX, lastY, xm, ym);
                                    if( sqScrollLen > scrollSlopSquare ) { // min. scrolling threshold reached
                                        gestureState = ST_SCROLL;
                                    }
                                } break;

                                case ST_2PRESS_C:
                                    gestureState = ST_SCROLL;
                                    break;

                                case ST_SCROLL:
                                    scrollDistance[0] = lastX - xm;
                                    scrollDistance[1] = lastY - ym;
                                    hitGestureEvent = pe;
                                    break;
                            }
                            if(DEBUG) {
                                final boolean isDistWithinDoubleTapSlop = sqDist < doubleTapSlopSquare;
                                final int dist = (int)Math.round(Math.sqrt(sqDist));
                                final int sqScrollLen = getSquareDistance(lastX, lastY, xm, ym);
                                final int scrollLen = (int)Math.round(Math.sqrt(sqScrollLen));
                                System.err.println(this+".dragged.1: pDist "+dist+", scrollLen "+scrollLen+", gPtr "+gPtr+" ["+pIds[0]+", "+pIds[1]+"]"+
                                                   ", diffDistWithinTapSlop "+isDistDiffWithinDoubleTapSlop+
                                                   ", distWithin2DTSlop "+isDistWithinDoubleTapSlop+
                                                   ", this "+xm+"/"+ym+", last "+lastX+"/"+lastY+", d "+scrollDistance[0]+"/"+scrollDistance[1]);
                            }
                        } else {
                            // distance too big ..
                            if(DEBUG) {
                                final boolean isDistWithinDoubleTapSlop = sqDist < doubleTapSlopSquare;
                                final int dist = (int)Math.round(Math.sqrt(sqDist));
                                final int startDist = (int)Math.round(Math.sqrt(sqStartDist));
                                System.err.println(this+".dragged.X1: pDist "+dist+", distStart "+startDist+", gPtr "+gPtr+" ["+pIds[0]+", "+pIds[1]+"]"+
                                                   ", diffDistWithinTapSlop "+isDistDiffWithinDoubleTapSlop+
                                                   ", distWithin2DTSlop "+isDistWithinDoubleTapSlop+
                                                   ", this "+xm+"/"+ym+", last "+lastX+"/"+lastY+", d "+scrollDistance[0]+"/"+scrollDistance[1]);
                            }
                            clear(true);
                        }
                        if( ST_2PRESS_T < gestureState ) {
                            // state ST_2PRESS_T waits for min scroll threshold !
                            lastX = xm;
                            lastY = ym;
                        }
                    } else {
                        // other 2 pointers .. should rarely happen!
                        if(DEBUG) {
                            System.err.println(this+".dragged.X2: gPtr "+gPtr+" ["+pIds[0]+", "+pIds[1]+"]"+
                                               ", last "+lastX+"/"+lastY+", d "+scrollDistance[0]+"/"+scrollDistance[1]);
                        }
                        clear(true);
                    }
                }
            } break;

            default:
        }
        return null != hitGestureEvent;
    }
}
