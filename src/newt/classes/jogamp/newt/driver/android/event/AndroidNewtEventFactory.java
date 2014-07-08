/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

package jogamp.newt.driver.android.event;

import jogamp.newt.Debug;
import android.view.MotionEvent;

import com.jogamp.common.os.AndroidVersion;
import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.NEWTEvent;

public class AndroidNewtEventFactory {
    private static final boolean DEBUG_MOUSE_EVENT = Debug.debug("Android.MouseEvent");
    private static final boolean DEBUG_KEY_EVENT = Debug.debug("Android.KeyEvent");

    /** API Level 12: {@link android.view.MotionEvent#ACTION_SCROLL} = {@value} */
    private static final int ACTION_SCROLL = 8;

    private static final com.jogamp.newt.event.MouseEvent.PointerType aToolType2PointerType(final int aToolType) {
        switch( aToolType ) {
            case MotionEvent.TOOL_TYPE_FINGER:
                return com.jogamp.newt.event.MouseEvent.PointerType.TouchScreen;
            case MotionEvent.TOOL_TYPE_MOUSE:
                return com.jogamp.newt.event.MouseEvent.PointerType.Mouse;
            case MotionEvent.TOOL_TYPE_STYLUS:
            case MotionEvent.TOOL_TYPE_ERASER:
                return com.jogamp.newt.event.MouseEvent.PointerType.Pen;
            default:
                return com.jogamp.newt.event.MouseEvent.PointerType.Undefined;
        }
    }

    private static final short aMotionEventType2Newt(final int aType) {
        switch( aType ) {
            case android.view.MotionEvent.ACTION_DOWN:
            case android.view.MotionEvent.ACTION_POINTER_DOWN:
                return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_PRESSED;
            case android.view.MotionEvent.ACTION_UP:
            case android.view.MotionEvent.ACTION_POINTER_UP:
            case android.view.MotionEvent.ACTION_CANCEL:
                return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_RELEASED;
            case android.view.MotionEvent.ACTION_MOVE:
                return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_DRAGGED;
            case android.view.MotionEvent.ACTION_OUTSIDE:
                return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_MOVED;
            // case ACTION_HOVER_MOVE
            case ACTION_SCROLL:  // API Level 12 !
                return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_WHEEL_MOVED;
            // case ACTION_HOVER_ENTER
            // case ACTION_HOVER_EXIT
        }
        return (short)0;
    }

    private static final short aAccessibilityEventType2Newt(final int aType) {
        switch( aType ) {
            case android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED:
                return com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_GAINED_FOCUS;
        }
        return (short)0;
    }

    private static final short aKeyEventType2NewtEventType(final int androidKeyAction) {
        switch(androidKeyAction) {
            case android.view.KeyEvent.ACTION_DOWN:
            case android.view.KeyEvent.ACTION_MULTIPLE:
                return com.jogamp.newt.event.KeyEvent.EVENT_KEY_PRESSED;
            case android.view.KeyEvent.ACTION_UP:
                return com.jogamp.newt.event.KeyEvent.EVENT_KEY_RELEASED;
        }
        return (short)0;
    }

    private static final short aKeyCode2NewtKeyCode(final int androidKeyCode, final boolean inclSysKeys) {
        if(android.view.KeyEvent.KEYCODE_0 <= androidKeyCode && androidKeyCode <= android.view.KeyEvent.KEYCODE_9) {
            return (short) ( com.jogamp.newt.event.KeyEvent.VK_0 + ( androidKeyCode - android.view.KeyEvent.KEYCODE_0 ) );
        }
        if(android.view.KeyEvent.KEYCODE_A <= androidKeyCode && androidKeyCode <= android.view.KeyEvent.KEYCODE_Z) {
            return (short) ( com.jogamp.newt.event.KeyEvent.VK_A + ( androidKeyCode - android.view.KeyEvent.KEYCODE_A ) );
        }
        if(android.view.KeyEvent.KEYCODE_F1 <= androidKeyCode && androidKeyCode <= android.view.KeyEvent.KEYCODE_F12) {
            return (short) ( com.jogamp.newt.event.KeyEvent.VK_F1 + ( androidKeyCode - android.view.KeyEvent.KEYCODE_F1 ) );
        }
        if(android.view.KeyEvent.KEYCODE_NUMPAD_0 <= androidKeyCode && androidKeyCode <= android.view.KeyEvent.KEYCODE_NUMPAD_9) {
            return (short) ( com.jogamp.newt.event.KeyEvent.VK_NUMPAD0 + ( androidKeyCode - android.view.KeyEvent.KEYCODE_NUMPAD_0 ) );
        }
        switch(androidKeyCode) {
            case android.view.KeyEvent.KEYCODE_COMMA: return com.jogamp.newt.event.KeyEvent.VK_COMMA;
            case android.view.KeyEvent.KEYCODE_PERIOD: return com.jogamp.newt.event.KeyEvent.VK_PERIOD;
            case android.view.KeyEvent.KEYCODE_ALT_LEFT: return com.jogamp.newt.event.KeyEvent.VK_ALT;
            case android.view.KeyEvent.KEYCODE_ALT_RIGHT: return com.jogamp.newt.event.KeyEvent.VK_ALT_GRAPH;
            case android.view.KeyEvent.KEYCODE_SHIFT_LEFT: return com.jogamp.newt.event.KeyEvent.VK_SHIFT;
            case android.view.KeyEvent.KEYCODE_SHIFT_RIGHT: return com.jogamp.newt.event.KeyEvent.VK_SHIFT;
            case android.view.KeyEvent.KEYCODE_TAB: return com.jogamp.newt.event.KeyEvent.VK_TAB;
            case android.view.KeyEvent.KEYCODE_SPACE: return com.jogamp.newt.event.KeyEvent.VK_SPACE;
            case android.view.KeyEvent.KEYCODE_ENTER: return com.jogamp.newt.event.KeyEvent.VK_ENTER;
            case android.view.KeyEvent.KEYCODE_DEL: return com.jogamp.newt.event.KeyEvent.VK_BACK_SPACE;
            case android.view.KeyEvent.KEYCODE_MINUS: return com.jogamp.newt.event.KeyEvent.VK_MINUS;
            case android.view.KeyEvent.KEYCODE_EQUALS: return com.jogamp.newt.event.KeyEvent.VK_EQUALS;
            case android.view.KeyEvent.KEYCODE_LEFT_BRACKET: return com.jogamp.newt.event.KeyEvent.VK_LEFT_PARENTHESIS;
            case android.view.KeyEvent.KEYCODE_RIGHT_BRACKET: return com.jogamp.newt.event.KeyEvent.VK_RIGHT_PARENTHESIS;
            case android.view.KeyEvent.KEYCODE_BACKSLASH: return com.jogamp.newt.event.KeyEvent.VK_BACK_SLASH;
            case android.view.KeyEvent.KEYCODE_SEMICOLON: return com.jogamp.newt.event.KeyEvent.VK_SEMICOLON;
            // case android.view.KeyEvent.KEYCODE_APOSTROPHE: ??
            case android.view.KeyEvent.KEYCODE_SLASH: return com.jogamp.newt.event.KeyEvent.VK_SLASH;
            case android.view.KeyEvent.KEYCODE_AT: return com.jogamp.newt.event.KeyEvent.VK_AT;
            // case android.view.KeyEvent.KEYCODE_MUTE: ??
            case android.view.KeyEvent.KEYCODE_PAGE_UP: return com.jogamp.newt.event.KeyEvent.VK_PAGE_UP;
            case android.view.KeyEvent.KEYCODE_PAGE_DOWN: return com.jogamp.newt.event.KeyEvent.VK_PAGE_DOWN;
            case android.view.KeyEvent.KEYCODE_ESCAPE: return com.jogamp.newt.event.KeyEvent.VK_ESCAPE;
            case android.view.KeyEvent.KEYCODE_CTRL_LEFT: return com.jogamp.newt.event.KeyEvent.VK_CONTROL;
            case android.view.KeyEvent.KEYCODE_CTRL_RIGHT: return com.jogamp.newt.event.KeyEvent.VK_CONTROL; // ??
            case android.view.KeyEvent.KEYCODE_BACK:
                if( inclSysKeys ) {
                    // Note that manual mapping is performed, based on the keyboard state.
                    // I.e. we map to VK_KEYBOARD_INVISIBLE if keyboard was visible and now becomes invisible!
                    // Otherwise we map to VK_ESCAPE, and if not consumed by user, the application will be terminated.
                    return com.jogamp.newt.event.KeyEvent.VK_ESCAPE;
                }
                break;
            case android.view.KeyEvent.KEYCODE_HOME:
                if( inclSysKeys ) {
                    // If not consumed by user, the application will be 'paused',
                    // i.e. resources (GLEventListener) pulled before surface gets destroyed!
                    return com.jogamp.newt.event.KeyEvent.VK_HOME;
                }
                break;
        }
        return com.jogamp.newt.event.KeyEvent.VK_UNDEFINED;
    }

    private static final int aKeyModifiers2Newt(final int androidMods) {
        int newtMods = 0;
        if ((androidMods & android.view.KeyEvent.META_SYM_ON)   != 0)   newtMods |= com.jogamp.newt.event.InputEvent.META_MASK;
        if ((androidMods & android.view.KeyEvent.META_SHIFT_ON) != 0)   newtMods |= com.jogamp.newt.event.InputEvent.SHIFT_MASK;
        if ((androidMods & android.view.KeyEvent.META_ALT_ON)   != 0)   newtMods |= com.jogamp.newt.event.InputEvent.ALT_MASK;

        return newtMods;
    }

    public static com.jogamp.newt.event.WindowEvent createWindowEvent(final android.view.accessibility.AccessibilityEvent event, final com.jogamp.newt.Window newtSource) {
        final int aType = event.getEventType();
        final short nType = aAccessibilityEventType2Newt(aType);

        if( (short)0 != nType) {
            return new com.jogamp.newt.event.WindowEvent(nType, ((null==newtSource)?null:(Object)newtSource), event.getEventTime());
        }
        return null; // no mapping ..
    }


    public static com.jogamp.newt.event.KeyEvent createKeyEvent(final android.view.KeyEvent aEvent, final com.jogamp.newt.Window newtSource, final boolean inclSysKeys) {
        final com.jogamp.newt.event.KeyEvent res;
        final short newtType = aKeyEventType2NewtEventType(aEvent.getAction());
        if( (short)0 != newtType) {
            final short newtKeyCode = aKeyCode2NewtKeyCode(aEvent.getKeyCode(), inclSysKeys);
            res = createKeyEventImpl(aEvent, newtType, newtKeyCode, newtSource);
        } else {
            res = null;
        }
        if(DEBUG_KEY_EVENT) {
            System.err.println("createKeyEvent0: "+aEvent+" -> "+res);
        }
        return res;
    }

    public static com.jogamp.newt.event.KeyEvent createKeyEvent(final android.view.KeyEvent aEvent, final short newtType, final com.jogamp.newt.Window newtSource, final boolean inclSysKeys) {
        final short newtKeyCode = aKeyCode2NewtKeyCode(aEvent.getKeyCode(), inclSysKeys);
        final com.jogamp.newt.event.KeyEvent res = createKeyEventImpl(aEvent, newtType, newtKeyCode, newtSource);
        if(DEBUG_KEY_EVENT) {
            System.err.println("createKeyEvent1: newtType "+NEWTEvent.toHexString(newtType)+", "+aEvent+" -> "+res);
        }
        return res;
    }

    public static com.jogamp.newt.event.KeyEvent createKeyEvent(final android.view.KeyEvent aEvent, final short newtKeyCode, final short newtType, final com.jogamp.newt.Window newtSource) {
        final com.jogamp.newt.event.KeyEvent res = createKeyEventImpl(aEvent, newtType, newtKeyCode, newtSource);
        if(DEBUG_KEY_EVENT) {
            System.err.println("createKeyEvent2: newtType "+NEWTEvent.toHexString(newtType)+", "+aEvent+" -> "+res);
        }
        return res;
    }

    private static com.jogamp.newt.event.KeyEvent createKeyEventImpl(final android.view.KeyEvent aEvent, final short newtType, final short newtKeyCode, final com.jogamp.newt.Window newtSource) {
        if( (short)0 != newtType && com.jogamp.newt.event.KeyEvent.VK_UNDEFINED != newtKeyCode ) {
            final Object src = null==newtSource ? null : newtSource;
            final long unixTime = System.currentTimeMillis() + ( aEvent.getEventTime() - android.os.SystemClock.uptimeMillis() );
            final int newtMods = aKeyModifiers2Newt(aEvent.getMetaState());

            return com.jogamp.newt.event.KeyEvent.create(
                                newtType, src, unixTime, newtMods, newtKeyCode, newtKeyCode, (char) aEvent.getUnicodeChar());
        }
        return null;
    }

    private static float maxPressure = 0.7f; // experienced maximum value (Amazon HD = 0.8f)

    /**
     * Dynamic calibration of maximum MotionEvent pressure, starting from 0.7f
     * <p>
     * Specification says no pressure is 0.0f and
     * normal pressure is 1.0f, where &gt; 1.0f denominates very high pressure.
     * </p>
     * <p>
     * Some devices exceed this spec, or better, most devices do.
     * <ul>
     *   <li>Asus TF2*: Pressure always &gt; 1.0f</li>
     *   <li>Amazon HD: Pressure always &le; 0.8f</li>
     * </ul>
     * </p>
     *
     * @return
     */
    public static float getMaxPressure() {
        return maxPressure;
    }

    private final int touchSlop;
    public AndroidNewtEventFactory(final android.content.Context context, final android.os.Handler handler) {
        final android.view.ViewConfiguration configuration = android.view.ViewConfiguration.get(context);
        touchSlop = configuration.getScaledTouchSlop();
        final int doubleTapSlop = configuration.getScaledDoubleTapSlop();
        if(DEBUG_MOUSE_EVENT) {
            System.err.println("AndroidNewtEventFactory    scrollSlop (scaled) "+touchSlop);
            System.err.println("AndroidNewtEventFactory doubleTapSlop (scaled) "+doubleTapSlop);
        }
    }

    private static void collectPointerData(final MotionEvent e, final int count, final int[] x, final int[] y, final float[] pressure,
                                           final short[] pointerIds, final MouseEvent.PointerType[] pointerTypes) {
        for(int i=0; i < count; i++) {
            x[i] = (int)e.getX(i);
            y[i] = (int)e.getY(i);
            pressure[i] = e.getPressure(i);
            pointerIds[i] = (short)e.getPointerId(i);
            if( pressure[i] > maxPressure ) {
                maxPressure = pressure[i];
            }
            pointerTypes[i] = aToolType2PointerType( e.getToolType(i) );
            if(DEBUG_MOUSE_EVENT) {
                System.err.println("createMouseEvent: ptr-data["+i+"] "+x[i]+"/"+y[i]+", pressure "+pressure[i]+", id "+pointerIds[i]+", type "+pointerTypes[i]);
            }
        }
    }

    public boolean sendPointerEvent(final boolean enqueue, final boolean wait, final boolean setFocusOnDown, final boolean isOnTouchEvent,
                                    final android.view.MotionEvent event, final jogamp.newt.driver.android.WindowDriver newtSource) {
        if(DEBUG_MOUSE_EVENT) {
            System.err.println("createMouseEvent: isOnTouchEvent "+isOnTouchEvent+", "+event);
        }

        if( event.getPressure() > maxPressure ) {
            maxPressure = event.getPressure(); // write to static field intended
        }

        //
        // Prefilter Android Event (Gesture, ..) and determine final type
        //
        final int aType = event.getActionMasked();
        final short nType = aMotionEventType2Newt(aType);
        final float rotationScale = touchSlop;
        final float[] rotationXYZ = new float[] { 0f, 0f, 0f };

        if( (short)0 != nType ) {
            int modifiers = 0;

            //
            // Determine SDK 12 SCROLL, newt-button and whether dedicated pointer is pressed
            //
            final int pIndex;
            final short button;
            switch( aType ) {
                case android.view.MotionEvent.ACTION_POINTER_DOWN:
                case android.view.MotionEvent.ACTION_POINTER_UP: {
                        pIndex = event.getActionIndex();
                        final int b = event.getPointerId(pIndex) + 1; // FIXME: Assumption that Pointer-ID starts w/ 0 !
                        if( com.jogamp.newt.event.MouseEvent.BUTTON1 <= b && b <= com.jogamp.newt.event.MouseEvent.BUTTON_COUNT ) {
                            button = (short)b;
                        } else {
                            button = com.jogamp.newt.event.MouseEvent.BUTTON1;
                        }
                    }
                    break;

                case ACTION_SCROLL:
                    if( AndroidVersion.SDK_INT >= 12 ) { // API Level 12
                        rotationXYZ[0] = event.getAxisValue(android.view.MotionEvent.AXIS_X) / rotationScale;
                        rotationXYZ[1] = event.getAxisValue(android.view.MotionEvent.AXIS_Y) / rotationScale;

                        if( rotationXYZ[0]*rotationXYZ[0] > rotationXYZ[1]*rotationXYZ[1] ) {
                            // Horizontal
                            modifiers |= com.jogamp.newt.event.InputEvent.SHIFT_MASK;
                        }
                        if(DEBUG_MOUSE_EVENT) {
                            System.err.println("createMouseEvent: SDK-12 Scroll "+rotationXYZ[0]+"/"+rotationXYZ[1]+", "+rotationScale+", mods "+modifiers);
                        }
                    }
                    // Fall through intended!

                default: {
                        pIndex = 0;
                        button = com.jogamp.newt.event.MouseEvent.BUTTON1;
                    }
            }
            final int pCount = event.getPointerCount(); // all

            switch( aType ) {
                case android.view.MotionEvent.ACTION_DOWN:
                case android.view.MotionEvent.ACTION_POINTER_DOWN:
                    // modifier button-mask will be set on doPointerEvent(..)
                    if( setFocusOnDown ) {
                        newtSource.focusChanged(false, true);
                    }
            }

            //
            // Collect common data
            //
            final int[] x = new int[pCount];
            final int[] y = new int[pCount];
            final float[] pressure = new float[pCount];
            final short[] pointerIds = new short[pCount];
            final MouseEvent.PointerType[] pointerTypes = new MouseEvent.PointerType[pCount];
            if( 0 < pCount ) {
                if(DEBUG_MOUSE_EVENT) {
                    System.err.println("createMouseEvent: collect ptr-data [0.."+(pCount-1)+", count "+pCount+", action "+pIndex+"], aType "+aType+", button "+button);
                }
                collectPointerData(event, pCount, x, y, pressure, pointerIds, pointerTypes);
            }
            newtSource.doPointerEvent(enqueue, wait, pointerTypes, nType, modifiers,
                                      pIndex, pointerIds, button, x, y, pressure, maxPressure, rotationXYZ, rotationScale);
            return true;
        }
        return false; // no mapping ..
    }
}

