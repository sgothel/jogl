/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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

package jogamp.newt.awt.event;

import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.NativeSurfaceHolder;

import com.jogamp.newt.event.MouseEvent;

/**
 *
 * <a name="AWTEventModifierMapping"><h5>AWT Event Modifier Mapping</h5></a>
 * <pre>
    Modifier       AWT Constant                     AWT Bit  AWT Ex  NEWT Constant              NEWT Bit
    -------------  -------------------------------  -------  ------  -------------------------  --------
    Shift          Event.SHIFT_MASK                 0
    Ctrl           Event.CTRL_MASK                  1
    Meta           Event.META_MASK                  2
    Alt            Event.ALT_MASK                   3
    Button1        InputEvent.BUTTON1_MASK          4
    Button2        InputEvent.BUTTON2_MASK          3
    Button3        InputEvent.BUTTON3_MASK          2
    Shift Down     InputEvent.SHIFT_DOWN_MASK       6        *       InputEvent.SHIFT_MASK      0
    Ctrl Down      InputEvent.CTRL_DOWN_MASK        7        *       InputEvent.CTRL_MASK       1
    Meta Down      InputEvent.META_DOWN_MASK        8        *       InputEvent.META_MASK       2
    Alt Down       InputEvent.ALT_DOWN_MASK         9        *       InputEvent.ALT_MASK        3
    Button1 Down   InputEvent.BUTTON1_DOWN_MASK     10       *       InputEvent.BUTTON1_MASK    5
    Button2 Down   InputEvent.BUTTON2_DOWN_MASK     11       *       InputEvent.BUTTON2_MASK    6
    Button3 Down   InputEvent.BUTTON3_DOWN_MASK     12       *       InputEvent.BUTTON3_MASK    7
    AltGraph Down  InputEvent.ALT_GRAPH_DOWN_MASK   13       *       InputEvent.ALT_GRAPH_MASK  4
    Button4 Down   --                               14       *       InputEvent.BUTTON4_MASK    8
    Button5 Down   --                               15       *       InputEvent.BUTTON5_MASK    9
    Button6 Down   --                               16       *       InputEvent.BUTTON6_MASK    10
    Button7 Down   --                               17       *       InputEvent.BUTTON7_MASK    11
    Button8 Down   --                               18       *       InputEvent.BUTTON8_MASK    12
    Button9 Down   --                               19       *       InputEvent.BUTTON9_MASK    13
    Button10 Down  --                               20       *                                  14
    Button11 Down  --                               21       *                                  15
    Button12 Down  --                               22       *                                  16
    Button13 Down  --                               23       *                                  17
    Button14 Down  --                               24       *                                  18
    Button15 Down  --                               25       *                                  19
    Button16 Down  --                               26       *       InputEvent.BUTTONLAST_MASK 20
    Button17 Down  --                               27       *
    Button18 Down  --                               28       *
    Button19 Down  --                               29       *
    Button20 Down  --                               30       *
    Autorepeat     --                               -                InputEvent.AUTOREPEAT_MASK 29
    Confined       --                               -                InputEvent.CONFINED_MASK   30
    Invisible      --                               -                InputEvent.INVISIBLE_MASK  31
 * </pre>
 *
 */
public class AWTNewtEventFactory {

    /** zero-based AWT button mask array filled by {@link #getAWTButtonDownMask(int)}, allowing fast lookup. */
    private static int awtButtonDownMasks[] ;

    static {
        // There is an assumption in awtModifiers2Newt(int,int,boolean)
        // that the awtButtonMasks and newtButtonMasks are peers, i.e.
        // a given index refers to the same button in each array.

        /* {
            Method _getMaskForButtonMethod = null;
            try {
                _getMaskForButtonMethod = ReflectionUtil.getMethod(java.awt.event.InputEvent.class, "getMaskForButton", int.class);
            } catch(Throwable t) {}
            getMaskForButtonMethod = _getMaskForButtonMethod;
        } */

        awtButtonDownMasks = new int[com.jogamp.newt.event.MouseEvent.BUTTON_COUNT] ; // java.awt.MouseInfo.getNumberOfButtons() ;
        for (int n = 0 ; n < awtButtonDownMasks.length ; ++n) {
            awtButtonDownMasks[n] = getAWTButtonDownMaskImpl(n+1);
        }
    }

    public static final short eventTypeAWT2NEWT(final int awtType) {
        switch( awtType ) {
            // n/a case java.awt.event.WindowEvent.WINDOW_OPENED: return com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_OPENED;
            case java.awt.event.WindowEvent.WINDOW_CLOSING: return com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_DESTROY_NOTIFY;
            case java.awt.event.WindowEvent.WINDOW_CLOSED: return com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_DESTROYED;
            // n/a case java.awt.event.WindowEvent.WINDOW_ICONIFIED: return com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_ICONIFIED;
            // n/a case java.awt.event.WindowEvent.WINDOW_DEICONIFIED: return com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_DEICONIFIED;
            case java.awt.event.WindowEvent.WINDOW_ACTIVATED: return com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_GAINED_FOCUS;
            case java.awt.event.WindowEvent.WINDOW_GAINED_FOCUS: return com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_GAINED_FOCUS;
            case java.awt.event.FocusEvent.FOCUS_GAINED: return com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_GAINED_FOCUS;
            case java.awt.event.WindowEvent.WINDOW_DEACTIVATED: return com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_LOST_FOCUS;
            case java.awt.event.WindowEvent.WINDOW_LOST_FOCUS: return com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_LOST_FOCUS;
            case java.awt.event.FocusEvent.FOCUS_LOST: return com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_LOST_FOCUS;
            // n/a case java.awt.event.WindowEvent.WINDOW_STATE_CHANGED: return com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_STATE_CHANGED;

            case java.awt.event.ComponentEvent.COMPONENT_MOVED: return com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_MOVED;
            case java.awt.event.ComponentEvent.COMPONENT_RESIZED: return com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_RESIZED;
            // n/a case java.awt.event.ComponentEvent.COMPONENT_SHOWN: return com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_SHOWN;
            // n/a case java.awt.event.ComponentEvent.COMPONENT_HIDDEN: return com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_HIDDEN;

            case java.awt.event.MouseEvent.MOUSE_CLICKED: return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_CLICKED;
            case java.awt.event.MouseEvent.MOUSE_PRESSED: return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_PRESSED;
            case java.awt.event.MouseEvent.MOUSE_RELEASED: return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_RELEASED;
            case java.awt.event.MouseEvent.MOUSE_MOVED: return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_MOVED;
            case java.awt.event.MouseEvent.MOUSE_ENTERED: return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_ENTERED;
            case java.awt.event.MouseEvent.MOUSE_EXITED: return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_EXITED;
            case java.awt.event.MouseEvent.MOUSE_DRAGGED: return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_DRAGGED;
            case java.awt.event.MouseEvent.MOUSE_WHEEL: return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_WHEEL_MOVED;

            case java.awt.event.KeyEvent.KEY_PRESSED: return com.jogamp.newt.event.KeyEvent.EVENT_KEY_PRESSED;
            case java.awt.event.KeyEvent.KEY_RELEASED: return com.jogamp.newt.event.KeyEvent.EVENT_KEY_RELEASED;
        }
        return (short)0;
    }

    private static int getAWTButtonDownMaskImpl(final int button) {
        /**
         * java.awt.event.InputEvent.getMaskForButton(button);
         *
        if(null != getMaskForButtonMethod) {
            Object r=null;
            try {
                r = getMaskForButtonMethod.invoke(null, new Integer(button));
            } catch (Throwable t) { }
            if(null != r) {
                return ((Integer)r).intValue();
            }
        } */
        final int m;
        switch(button) {
            case 0 : m = 0; break;
            case 1 : m = java.awt.event.InputEvent.BUTTON1_DOWN_MASK; break; // 1<<10
            case 2 : m = java.awt.event.InputEvent.BUTTON2_DOWN_MASK; break; // 1<<11
            case 3 : m = java.awt.event.InputEvent.BUTTON3_DOWN_MASK; break; // 1<<12
            default:
                if( button <= com.jogamp.newt.event.MouseEvent.BUTTON_COUNT ) {
                    m = 1 << ( 10 + button ) ; // b4 = 1<<14, b5 = 1<<15, etc
                } else {
                    m = 0;
                }
        }
        return m;
    }

    /**
     * <p>
     * See <a href="#AWTEventModifierMapping"> AWT event modifier mapping details</a>.
     * </p>
     *
     * @param button
     * @return
     */
    public static int getAWTButtonDownMask(final int button) {
        if( 0 < button && button <= awtButtonDownMasks.length ) {
            return awtButtonDownMasks[button-1];
        } else {
            return 0;
        }
    }

    public static final short awtButton2Newt(final int awtButton) {
        if( 0 < awtButton && awtButton <= com.jogamp.newt.event.MouseEvent.BUTTON_COUNT ) {
            return (short)awtButton;
        } else {
            return (short)0;
        }
    }

    /**
     * Converts the specified set of AWT event modifiers and extended event
     * modifiers to the equivalent NEWT event modifiers.
     *
     * <p>
     * See <a href="#AWTEventModifierMapping"> AWT event modifier mapping details</a>.
     * </p>
     *
     * @param awtMods
     * The AWT event modifiers.
     *
     * @param awtModsEx
     * The AWT extended event modifiers.
     * AWT passes mouse button specific bits here and are the preferred way check the mouse button state.
     */
    public static final int awtModifiers2Newt(final int awtMods, final int awtModsEx) {
        int newtMods = 0;

        /** Redundant old modifiers ..
        if ((awtMods & java.awt.event.InputEvent.SHIFT_MASK) != 0)     newtMods |= com.jogamp.newt.event.InputEvent.SHIFT_MASK;
        if ((awtMods & java.awt.event.InputEvent.CTRL_MASK) != 0)      newtMods |= com.jogamp.newt.event.InputEvent.CTRL_MASK;
        if ((awtMods & java.awt.event.InputEvent.META_MASK) != 0)      newtMods |= com.jogamp.newt.event.InputEvent.META_MASK;
        if ((awtMods & java.awt.event.InputEvent.ALT_MASK) != 0)       newtMods |= com.jogamp.newt.event.InputEvent.ALT_MASK;
        if ((awtMods & java.awt.event.InputEvent.ALT_GRAPH_MASK) != 0) newtMods |= com.jogamp.newt.event.InputEvent.ALT_GRAPH_MASK; */

        if ((awtModsEx & java.awt.event.InputEvent.SHIFT_DOWN_MASK) != 0)     newtMods |= com.jogamp.newt.event.InputEvent.SHIFT_MASK;
        if ((awtModsEx & java.awt.event.InputEvent.CTRL_DOWN_MASK) != 0)      newtMods |= com.jogamp.newt.event.InputEvent.CTRL_MASK;
        if ((awtModsEx & java.awt.event.InputEvent.META_DOWN_MASK) != 0)      newtMods |= com.jogamp.newt.event.InputEvent.META_MASK;
        if ((awtModsEx & java.awt.event.InputEvent.ALT_DOWN_MASK) != 0)       newtMods |= com.jogamp.newt.event.InputEvent.ALT_MASK;
        if ((awtModsEx & java.awt.event.InputEvent.ALT_GRAPH_DOWN_MASK) != 0) newtMods |= com.jogamp.newt.event.InputEvent.ALT_GRAPH_MASK;

        // The BUTTON1_MASK, BUTTON2_MASK, and BUTTON3_MASK bits are
        // being ignored intentionally.  The AWT docs say that the
        // BUTTON1_DOWN_MASK etc bits in the extended modifiers are
        // the preferred place to check current button state.

        if( 0 != awtModsEx ) {
            for (int n = 0 ; n < awtButtonDownMasks.length ; ++n) {
                if ( (awtModsEx & awtButtonDownMasks[n]) != 0 ) {
                    newtMods |= com.jogamp.newt.event.InputEvent.getButtonMask(n+1);
                }
            }
        }

        return newtMods;
    }

    public static short awtKeyCode2NewtKeyCode(final int awtKeyCode) {
        final short defNEWTKeyCode = (short)awtKeyCode;
        switch (awtKeyCode) {
            case java.awt.event.KeyEvent.VK_HOME          : return com.jogamp.newt.event.KeyEvent.VK_HOME;
            case java.awt.event.KeyEvent.VK_END           : return com.jogamp.newt.event.KeyEvent.VK_END;
            case java.awt.event.KeyEvent.VK_FINAL         : return com.jogamp.newt.event.KeyEvent.VK_FINAL;
            case java.awt.event.KeyEvent.VK_PRINTSCREEN   : return com.jogamp.newt.event.KeyEvent.VK_PRINTSCREEN;
            case java.awt.event.KeyEvent.VK_BACK_SPACE    : return com.jogamp.newt.event.KeyEvent.VK_BACK_SPACE;
            case java.awt.event.KeyEvent.VK_TAB           : return com.jogamp.newt.event.KeyEvent.VK_TAB;
            case java.awt.event.KeyEvent.VK_ENTER         : return com.jogamp.newt.event.KeyEvent.VK_ENTER;
            case java.awt.event.KeyEvent.VK_PAGE_DOWN     : return com.jogamp.newt.event.KeyEvent.VK_PAGE_DOWN;
            case java.awt.event.KeyEvent.VK_CLEAR         : return com.jogamp.newt.event.KeyEvent.VK_CLEAR;
            case java.awt.event.KeyEvent.VK_SHIFT         : return com.jogamp.newt.event.KeyEvent.VK_SHIFT;
            case java.awt.event.KeyEvent.VK_PAGE_UP       : return com.jogamp.newt.event.KeyEvent.VK_PAGE_UP;
            case java.awt.event.KeyEvent.VK_CONTROL       : return com.jogamp.newt.event.KeyEvent.VK_CONTROL;
            case java.awt.event.KeyEvent.VK_ALT           : return com.jogamp.newt.event.KeyEvent.VK_ALT;
            case java.awt.event.KeyEvent.VK_ALT_GRAPH     : return com.jogamp.newt.event.KeyEvent.VK_ALT_GRAPH;
            case java.awt.event.KeyEvent.VK_CAPS_LOCK     : return com.jogamp.newt.event.KeyEvent.VK_CAPS_LOCK;
            case java.awt.event.KeyEvent.VK_PAUSE         : return com.jogamp.newt.event.KeyEvent.VK_PAUSE;
            case java.awt.event.KeyEvent.VK_SCROLL_LOCK   : return com.jogamp.newt.event.KeyEvent.VK_SCROLL_LOCK;
            case java.awt.event.KeyEvent.VK_CANCEL        : return com.jogamp.newt.event.KeyEvent.VK_CANCEL;
            case java.awt.event.KeyEvent.VK_INSERT        : return com.jogamp.newt.event.KeyEvent.VK_INSERT;
            case java.awt.event.KeyEvent.VK_ESCAPE        : return com.jogamp.newt.event.KeyEvent.VK_ESCAPE;
            case java.awt.event.KeyEvent.VK_CONVERT       : return com.jogamp.newt.event.KeyEvent.VK_CONVERT;
            case java.awt.event.KeyEvent.VK_NONCONVERT    : return com.jogamp.newt.event.KeyEvent.VK_NONCONVERT;
            case java.awt.event.KeyEvent.VK_ACCEPT        : return com.jogamp.newt.event.KeyEvent.VK_ACCEPT;
            case java.awt.event.KeyEvent.VK_MODECHANGE    : return com.jogamp.newt.event.KeyEvent.VK_MODECHANGE;
            case java.awt.event.KeyEvent.VK_SPACE         : return com.jogamp.newt.event.KeyEvent.VK_SPACE;
            case java.awt.event.KeyEvent.VK_EXCLAMATION_MARK: return com.jogamp.newt.event.KeyEvent.VK_EXCLAMATION_MARK;
            case java.awt.event.KeyEvent.VK_QUOTEDBL      : return com.jogamp.newt.event.KeyEvent.VK_QUOTEDBL;
            case java.awt.event.KeyEvent.VK_NUMBER_SIGN   : return com.jogamp.newt.event.KeyEvent.VK_NUMBER_SIGN;
            case java.awt.event.KeyEvent.VK_DOLLAR        : return com.jogamp.newt.event.KeyEvent.VK_DOLLAR;
            // case                         0x25             : return com.jogamp.newt.event.KeyEvent.VK_PERCENT;
            case java.awt.event.KeyEvent.VK_AMPERSAND     : return com.jogamp.newt.event.KeyEvent.VK_AMPERSAND;
            case java.awt.event.KeyEvent.VK_QUOTE         : return com.jogamp.newt.event.KeyEvent.VK_QUOTE;
            case java.awt.event.KeyEvent.VK_LEFT_PARENTHESIS : return com.jogamp.newt.event.KeyEvent.VK_LEFT_PARENTHESIS;
            case java.awt.event.KeyEvent.VK_RIGHT_PARENTHESIS: return com.jogamp.newt.event.KeyEvent.VK_RIGHT_PARENTHESIS;
            case java.awt.event.KeyEvent.VK_ASTERISK      : return com.jogamp.newt.event.KeyEvent.VK_ASTERISK;
            case java.awt.event.KeyEvent.VK_PLUS          : return com.jogamp.newt.event.KeyEvent.VK_PLUS;
            case java.awt.event.KeyEvent.VK_COMMA         : return com.jogamp.newt.event.KeyEvent.VK_COMMA;
            case java.awt.event.KeyEvent.VK_MINUS         : return com.jogamp.newt.event.KeyEvent.VK_MINUS;
            case java.awt.event.KeyEvent.VK_PERIOD        : return com.jogamp.newt.event.KeyEvent.VK_PERIOD;
            case java.awt.event.KeyEvent.VK_SLASH         : return com.jogamp.newt.event.KeyEvent.VK_SLASH;
            case java.awt.event.KeyEvent.VK_0             : return com.jogamp.newt.event.KeyEvent.VK_0;
            case java.awt.event.KeyEvent.VK_1             : return com.jogamp.newt.event.KeyEvent.VK_1;
            case java.awt.event.KeyEvent.VK_2             : return com.jogamp.newt.event.KeyEvent.VK_2;
            case java.awt.event.KeyEvent.VK_3             : return com.jogamp.newt.event.KeyEvent.VK_3;
            case java.awt.event.KeyEvent.VK_4             : return com.jogamp.newt.event.KeyEvent.VK_4;
            case java.awt.event.KeyEvent.VK_5             : return com.jogamp.newt.event.KeyEvent.VK_5;
            case java.awt.event.KeyEvent.VK_6             : return com.jogamp.newt.event.KeyEvent.VK_6;
            case java.awt.event.KeyEvent.VK_7             : return com.jogamp.newt.event.KeyEvent.VK_7;
            case java.awt.event.KeyEvent.VK_8             : return com.jogamp.newt.event.KeyEvent.VK_8;
            case java.awt.event.KeyEvent.VK_9             : return com.jogamp.newt.event.KeyEvent.VK_9;
            case java.awt.event.KeyEvent.VK_COLON         : return com.jogamp.newt.event.KeyEvent.VK_COLON;
            case java.awt.event.KeyEvent.VK_SEMICOLON     : return com.jogamp.newt.event.KeyEvent.VK_SEMICOLON;
            case java.awt.event.KeyEvent.VK_LESS          : return com.jogamp.newt.event.KeyEvent.VK_LESS;
            case java.awt.event.KeyEvent.VK_EQUALS        : return com.jogamp.newt.event.KeyEvent.VK_EQUALS;
            case java.awt.event.KeyEvent.VK_GREATER       : return com.jogamp.newt.event.KeyEvent.VK_GREATER;
            case                         0x3f             : return com.jogamp.newt.event.KeyEvent.VK_QUESTIONMARK;
            case java.awt.event.KeyEvent.VK_AT            : return com.jogamp.newt.event.KeyEvent.VK_AT;
            case java.awt.event.KeyEvent.VK_A             : return com.jogamp.newt.event.KeyEvent.VK_A;
            case java.awt.event.KeyEvent.VK_B             : return com.jogamp.newt.event.KeyEvent.VK_B;
            case java.awt.event.KeyEvent.VK_C             : return com.jogamp.newt.event.KeyEvent.VK_C;
            case java.awt.event.KeyEvent.VK_D             : return com.jogamp.newt.event.KeyEvent.VK_D;
            case java.awt.event.KeyEvent.VK_E             : return com.jogamp.newt.event.KeyEvent.VK_E;
            case java.awt.event.KeyEvent.VK_F             : return com.jogamp.newt.event.KeyEvent.VK_F;
            case java.awt.event.KeyEvent.VK_G             : return com.jogamp.newt.event.KeyEvent.VK_G;
            case java.awt.event.KeyEvent.VK_H             : return com.jogamp.newt.event.KeyEvent.VK_H;
            case java.awt.event.KeyEvent.VK_I             : return com.jogamp.newt.event.KeyEvent.VK_I;
            case java.awt.event.KeyEvent.VK_J             : return com.jogamp.newt.event.KeyEvent.VK_J;
            case java.awt.event.KeyEvent.VK_K             : return com.jogamp.newt.event.KeyEvent.VK_K;
            case java.awt.event.KeyEvent.VK_L             : return com.jogamp.newt.event.KeyEvent.VK_L;
            case java.awt.event.KeyEvent.VK_M             : return com.jogamp.newt.event.KeyEvent.VK_M;
            case java.awt.event.KeyEvent.VK_N             : return com.jogamp.newt.event.KeyEvent.VK_N;
            case java.awt.event.KeyEvent.VK_O             : return com.jogamp.newt.event.KeyEvent.VK_O;
            case java.awt.event.KeyEvent.VK_P             : return com.jogamp.newt.event.KeyEvent.VK_P;
            case java.awt.event.KeyEvent.VK_Q             : return com.jogamp.newt.event.KeyEvent.VK_Q;
            case java.awt.event.KeyEvent.VK_R             : return com.jogamp.newt.event.KeyEvent.VK_R;
            case java.awt.event.KeyEvent.VK_S             : return com.jogamp.newt.event.KeyEvent.VK_S;
            case java.awt.event.KeyEvent.VK_T             : return com.jogamp.newt.event.KeyEvent.VK_T;
            case java.awt.event.KeyEvent.VK_U             : return com.jogamp.newt.event.KeyEvent.VK_U;
            case java.awt.event.KeyEvent.VK_V             : return com.jogamp.newt.event.KeyEvent.VK_V;
            case java.awt.event.KeyEvent.VK_W             : return com.jogamp.newt.event.KeyEvent.VK_W;
            case java.awt.event.KeyEvent.VK_X             : return com.jogamp.newt.event.KeyEvent.VK_X;
            case java.awt.event.KeyEvent.VK_Y             : return com.jogamp.newt.event.KeyEvent.VK_Y;
            case java.awt.event.KeyEvent.VK_Z             : return com.jogamp.newt.event.KeyEvent.VK_Z;
            case java.awt.event.KeyEvent.VK_OPEN_BRACKET  : return com.jogamp.newt.event.KeyEvent.VK_OPEN_BRACKET;
            case java.awt.event.KeyEvent.VK_BACK_SLASH    : return com.jogamp.newt.event.KeyEvent.VK_BACK_SLASH;
            case java.awt.event.KeyEvent.VK_CLOSE_BRACKET : return com.jogamp.newt.event.KeyEvent.VK_CLOSE_BRACKET;
            case java.awt.event.KeyEvent.VK_CIRCUMFLEX    : return com.jogamp.newt.event.KeyEvent.VK_CIRCUMFLEX;
            case java.awt.event.KeyEvent.VK_UNDERSCORE    : return com.jogamp.newt.event.KeyEvent.VK_UNDERSCORE;
            case java.awt.event.KeyEvent.VK_BACK_QUOTE    : return com.jogamp.newt.event.KeyEvent.VK_BACK_QUOTE;
            case java.awt.event.KeyEvent.VK_F1            : return com.jogamp.newt.event.KeyEvent.VK_F1;
            case java.awt.event.KeyEvent.VK_F2            : return com.jogamp.newt.event.KeyEvent.VK_F2;
            case java.awt.event.KeyEvent.VK_F3            : return com.jogamp.newt.event.KeyEvent.VK_F3;
            case java.awt.event.KeyEvent.VK_F4            : return com.jogamp.newt.event.KeyEvent.VK_F4;
            case java.awt.event.KeyEvent.VK_F5            : return com.jogamp.newt.event.KeyEvent.VK_F5;
            case java.awt.event.KeyEvent.VK_F6            : return com.jogamp.newt.event.KeyEvent.VK_F6;
            case java.awt.event.KeyEvent.VK_F7            : return com.jogamp.newt.event.KeyEvent.VK_F7;
            case java.awt.event.KeyEvent.VK_F8            : return com.jogamp.newt.event.KeyEvent.VK_F8;
            case java.awt.event.KeyEvent.VK_F9            : return com.jogamp.newt.event.KeyEvent.VK_F9;
            case java.awt.event.KeyEvent.VK_F10           : return com.jogamp.newt.event.KeyEvent.VK_F10;
            case java.awt.event.KeyEvent.VK_F11           : return com.jogamp.newt.event.KeyEvent.VK_F11;
            case java.awt.event.KeyEvent.VK_F12           : return com.jogamp.newt.event.KeyEvent.VK_F12;
            case java.awt.event.KeyEvent.VK_F13           : return com.jogamp.newt.event.KeyEvent.VK_F13;
            case java.awt.event.KeyEvent.VK_F14           : return com.jogamp.newt.event.KeyEvent.VK_F14;
            case java.awt.event.KeyEvent.VK_F15           : return com.jogamp.newt.event.KeyEvent.VK_F15;
            case java.awt.event.KeyEvent.VK_F16           : return com.jogamp.newt.event.KeyEvent.VK_F16;
            case java.awt.event.KeyEvent.VK_F17           : return com.jogamp.newt.event.KeyEvent.VK_F17;
            case java.awt.event.KeyEvent.VK_F18           : return com.jogamp.newt.event.KeyEvent.VK_F18;
            case java.awt.event.KeyEvent.VK_F19           : return com.jogamp.newt.event.KeyEvent.VK_F19;
            case java.awt.event.KeyEvent.VK_F20           : return com.jogamp.newt.event.KeyEvent.VK_F20;
            case java.awt.event.KeyEvent.VK_F21           : return com.jogamp.newt.event.KeyEvent.VK_F21;
            case java.awt.event.KeyEvent.VK_F22           : return com.jogamp.newt.event.KeyEvent.VK_F22;
            case java.awt.event.KeyEvent.VK_F23           : return com.jogamp.newt.event.KeyEvent.VK_F23;
            case java.awt.event.KeyEvent.VK_F24           : return com.jogamp.newt.event.KeyEvent.VK_F24;
            case java.awt.event.KeyEvent.VK_BRACELEFT     : return com.jogamp.newt.event.KeyEvent.VK_LEFT_BRACE;
            case                         0x7c             : return com.jogamp.newt.event.KeyEvent.VK_PIPE;
            case java.awt.event.KeyEvent.VK_BRACERIGHT    : return com.jogamp.newt.event.KeyEvent.VK_RIGHT_BRACE;
            case java.awt.event.KeyEvent.VK_DEAD_TILDE    : return com.jogamp.newt.event.KeyEvent.VK_TILDE;
            case java.awt.event.KeyEvent.VK_DELETE        : return com.jogamp.newt.event.KeyEvent.VK_DELETE;
            case java.awt.event.KeyEvent.VK_NUMPAD0       : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD0;
            case java.awt.event.KeyEvent.VK_NUMPAD1       : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD1;
            case java.awt.event.KeyEvent.VK_NUMPAD2       : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD2;
            case java.awt.event.KeyEvent.VK_NUMPAD3       : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD3;
            case java.awt.event.KeyEvent.VK_NUMPAD4       : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD4;
            case java.awt.event.KeyEvent.VK_NUMPAD5       : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD5;
            case java.awt.event.KeyEvent.VK_NUMPAD6       : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD6;
            case java.awt.event.KeyEvent.VK_NUMPAD7       : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD7;
            case java.awt.event.KeyEvent.VK_NUMPAD8       : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD8;
            case java.awt.event.KeyEvent.VK_NUMPAD9       : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD9;
            case java.awt.event.KeyEvent.VK_DECIMAL       : return com.jogamp.newt.event.KeyEvent.VK_DECIMAL;
            case java.awt.event.KeyEvent.VK_SEPARATOR     : return com.jogamp.newt.event.KeyEvent.VK_SEPARATOR;
            case java.awt.event.KeyEvent.VK_ADD           : return com.jogamp.newt.event.KeyEvent.VK_ADD;
            case java.awt.event.KeyEvent.VK_SUBTRACT      : return com.jogamp.newt.event.KeyEvent.VK_SUBTRACT;
            case java.awt.event.KeyEvent.VK_MULTIPLY      : return com.jogamp.newt.event.KeyEvent.VK_MULTIPLY;
            case java.awt.event.KeyEvent.VK_DIVIDE        : return com.jogamp.newt.event.KeyEvent.VK_DIVIDE;
            case java.awt.event.KeyEvent.VK_NUM_LOCK      : return com.jogamp.newt.event.KeyEvent.VK_NUM_LOCK;
            case java.awt.event.KeyEvent.VK_KP_LEFT       : /** Fall through intended .. */
            case java.awt.event.KeyEvent.VK_LEFT          : return com.jogamp.newt.event.KeyEvent.VK_LEFT;
            case java.awt.event.KeyEvent.VK_KP_UP         : /** Fall through intended .. */
            case java.awt.event.KeyEvent.VK_UP            : return com.jogamp.newt.event.KeyEvent.VK_UP;
            case java.awt.event.KeyEvent.VK_KP_RIGHT      : /** Fall through intended .. */
            case java.awt.event.KeyEvent.VK_RIGHT         : return com.jogamp.newt.event.KeyEvent.VK_RIGHT;
            case java.awt.event.KeyEvent.VK_KP_DOWN       : /** Fall through intended .. */
            case java.awt.event.KeyEvent.VK_DOWN          : return com.jogamp.newt.event.KeyEvent.VK_DOWN;
            case java.awt.event.KeyEvent.VK_CONTEXT_MENU  : return com.jogamp.newt.event.KeyEvent.VK_CONTEXT_MENU;
            case java.awt.event.KeyEvent.VK_WINDOWS       : return com.jogamp.newt.event.KeyEvent.VK_WINDOWS;
            case java.awt.event.KeyEvent.VK_META          : return com.jogamp.newt.event.KeyEvent.VK_META;
            case java.awt.event.KeyEvent.VK_HELP          : return com.jogamp.newt.event.KeyEvent.VK_HELP;
            case java.awt.event.KeyEvent.VK_COMPOSE       : return com.jogamp.newt.event.KeyEvent.VK_COMPOSE;
            case java.awt.event.KeyEvent.VK_BEGIN         : return com.jogamp.newt.event.KeyEvent.VK_BEGIN;
            case java.awt.event.KeyEvent.VK_STOP          : return com.jogamp.newt.event.KeyEvent.VK_STOP;
            case java.awt.event.KeyEvent.VK_INVERTED_EXCLAMATION_MARK: return com.jogamp.newt.event.KeyEvent.VK_INVERTED_EXCLAMATION_MARK;
            case java.awt.event.KeyEvent.VK_EURO_SIGN     : return com.jogamp.newt.event.KeyEvent.VK_EURO_SIGN;
            case java.awt.event.KeyEvent.VK_CUT           : return com.jogamp.newt.event.KeyEvent.VK_CUT;
            case java.awt.event.KeyEvent.VK_COPY          : return com.jogamp.newt.event.KeyEvent.VK_COPY;
            case java.awt.event.KeyEvent.VK_PASTE         : return com.jogamp.newt.event.KeyEvent.VK_PASTE;
            case java.awt.event.KeyEvent.VK_UNDO          : return com.jogamp.newt.event.KeyEvent.VK_UNDO;
            case java.awt.event.KeyEvent.VK_AGAIN         : return com.jogamp.newt.event.KeyEvent.VK_AGAIN;
            case java.awt.event.KeyEvent.VK_FIND          : return com.jogamp.newt.event.KeyEvent.VK_FIND;
            case java.awt.event.KeyEvent.VK_PROPS         : return com.jogamp.newt.event.KeyEvent.VK_PROPS;
            case java.awt.event.KeyEvent.VK_INPUT_METHOD_ON_OFF: return com.jogamp.newt.event.KeyEvent.VK_INPUT_METHOD_ON_OFF;
            case java.awt.event.KeyEvent.VK_CODE_INPUT    : return com.jogamp.newt.event.KeyEvent.VK_CODE_INPUT;
            case java.awt.event.KeyEvent.VK_ROMAN_CHARACTERS: return com.jogamp.newt.event.KeyEvent.VK_ROMAN_CHARACTERS;
            case java.awt.event.KeyEvent.VK_ALL_CANDIDATES: return com.jogamp.newt.event.KeyEvent.VK_ALL_CANDIDATES;
            case java.awt.event.KeyEvent.VK_PREVIOUS_CANDIDATE: return com.jogamp.newt.event.KeyEvent.VK_PREVIOUS_CANDIDATE;
            case java.awt.event.KeyEvent.VK_ALPHANUMERIC  : return com.jogamp.newt.event.KeyEvent.VK_ALPHANUMERIC;
            case java.awt.event.KeyEvent.VK_KATAKANA      : return com.jogamp.newt.event.KeyEvent.VK_KATAKANA;
            case java.awt.event.KeyEvent.VK_HIRAGANA      : return com.jogamp.newt.event.KeyEvent.VK_HIRAGANA;
            case java.awt.event.KeyEvent.VK_FULL_WIDTH    : return com.jogamp.newt.event.KeyEvent.VK_FULL_WIDTH;
            case java.awt.event.KeyEvent.VK_HALF_WIDTH    : return com.jogamp.newt.event.KeyEvent.VK_HALF_WIDTH;
            case java.awt.event.KeyEvent.VK_JAPANESE_KATAKANA: return com.jogamp.newt.event.KeyEvent.VK_JAPANESE_KATAKANA;
            case java.awt.event.KeyEvent.VK_JAPANESE_HIRAGANA: return com.jogamp.newt.event.KeyEvent.VK_JAPANESE_HIRAGANA;
            case java.awt.event.KeyEvent.VK_JAPANESE_ROMAN: return com.jogamp.newt.event.KeyEvent.VK_JAPANESE_ROMAN;
            case java.awt.event.KeyEvent.VK_KANA_LOCK     : return com.jogamp.newt.event.KeyEvent.VK_KANA_LOCK;
        }
        return defNEWTKeyCode;
    }

    public static int newtKeyCode2AWTKeyCode(final short newtKeyCode) {
        final int defAwtKeyCode = 0xFFFF & newtKeyCode;
        switch (newtKeyCode) {
            case com.jogamp.newt.event.KeyEvent.VK_HOME          : return java.awt.event.KeyEvent.VK_HOME;
            case com.jogamp.newt.event.KeyEvent.VK_END           : return java.awt.event.KeyEvent.VK_END;
            case com.jogamp.newt.event.KeyEvent.VK_FINAL         : return java.awt.event.KeyEvent.VK_FINAL;
            case com.jogamp.newt.event.KeyEvent.VK_PRINTSCREEN   : return java.awt.event.KeyEvent.VK_PRINTSCREEN;
            case com.jogamp.newt.event.KeyEvent.VK_BACK_SPACE    : return java.awt.event.KeyEvent.VK_BACK_SPACE;
            case com.jogamp.newt.event.KeyEvent.VK_TAB           : return java.awt.event.KeyEvent.VK_TAB;
            case com.jogamp.newt.event.KeyEvent.VK_ENTER         : return java.awt.event.KeyEvent.VK_ENTER;
            case com.jogamp.newt.event.KeyEvent.VK_PAGE_DOWN     : return java.awt.event.KeyEvent.VK_PAGE_DOWN;
            case com.jogamp.newt.event.KeyEvent.VK_CLEAR         : return java.awt.event.KeyEvent.VK_CLEAR;
            case com.jogamp.newt.event.KeyEvent.VK_SHIFT         : return java.awt.event.KeyEvent.VK_SHIFT;
            case com.jogamp.newt.event.KeyEvent.VK_PAGE_UP       : return java.awt.event.KeyEvent.VK_PAGE_UP;
            case com.jogamp.newt.event.KeyEvent.VK_CONTROL       : return java.awt.event.KeyEvent.VK_CONTROL;
            case com.jogamp.newt.event.KeyEvent.VK_ALT           : return java.awt.event.KeyEvent.VK_ALT;
            // FIXME: On X11 it results to 0xff7e w/ AWTRobot, which is wrong. 0xffea Alt_R is expected AFAIK.
            case com.jogamp.newt.event.KeyEvent.VK_ALT_GRAPH     : return java.awt.event.KeyEvent.VK_ALT_GRAPH;
            case com.jogamp.newt.event.KeyEvent.VK_CAPS_LOCK     : return java.awt.event.KeyEvent.VK_CAPS_LOCK;
            case com.jogamp.newt.event.KeyEvent.VK_PAUSE         : return java.awt.event.KeyEvent.VK_PAUSE;
            case com.jogamp.newt.event.KeyEvent.VK_SCROLL_LOCK   : return java.awt.event.KeyEvent.VK_SCROLL_LOCK;
            case com.jogamp.newt.event.KeyEvent.VK_CANCEL        : return java.awt.event.KeyEvent.VK_CANCEL;
            case com.jogamp.newt.event.KeyEvent.VK_INSERT        : return java.awt.event.KeyEvent.VK_INSERT;
            case com.jogamp.newt.event.KeyEvent.VK_ESCAPE        : return java.awt.event.KeyEvent.VK_ESCAPE;
            case com.jogamp.newt.event.KeyEvent.VK_CONVERT       : return java.awt.event.KeyEvent.VK_CONVERT;
            case com.jogamp.newt.event.KeyEvent.VK_NONCONVERT    : return java.awt.event.KeyEvent.VK_NONCONVERT;
            case com.jogamp.newt.event.KeyEvent.VK_ACCEPT        : return java.awt.event.KeyEvent.VK_ACCEPT;
            case com.jogamp.newt.event.KeyEvent.VK_MODECHANGE    : return java.awt.event.KeyEvent.VK_MODECHANGE;
            case com.jogamp.newt.event.KeyEvent.VK_SPACE         : return java.awt.event.KeyEvent.VK_SPACE;
            case com.jogamp.newt.event.KeyEvent.VK_EXCLAMATION_MARK: return java.awt.event.KeyEvent.VK_EXCLAMATION_MARK;
            case com.jogamp.newt.event.KeyEvent.VK_QUOTEDBL      : return java.awt.event.KeyEvent.VK_QUOTEDBL;
            case com.jogamp.newt.event.KeyEvent.VK_NUMBER_SIGN   : return java.awt.event.KeyEvent.VK_NUMBER_SIGN;
            case com.jogamp.newt.event.KeyEvent.VK_DOLLAR        : return java.awt.event.KeyEvent.VK_DOLLAR;
            case com.jogamp.newt.event.KeyEvent.VK_PERCENT       : return defAwtKeyCode;
            case com.jogamp.newt.event.KeyEvent.VK_AMPERSAND     : return java.awt.event.KeyEvent.VK_AMPERSAND;
            case com.jogamp.newt.event.KeyEvent.VK_QUOTE         : return java.awt.event.KeyEvent.VK_QUOTE;
            case com.jogamp.newt.event.KeyEvent.VK_LEFT_PARENTHESIS : return java.awt.event.KeyEvent.VK_LEFT_PARENTHESIS;
            case com.jogamp.newt.event.KeyEvent.VK_RIGHT_PARENTHESIS: return java.awt.event.KeyEvent.VK_RIGHT_PARENTHESIS;
            case com.jogamp.newt.event.KeyEvent.VK_ASTERISK      : return java.awt.event.KeyEvent.VK_ASTERISK;
            case com.jogamp.newt.event.KeyEvent.VK_PLUS          : return java.awt.event.KeyEvent.VK_PLUS;
            case com.jogamp.newt.event.KeyEvent.VK_COMMA         : return java.awt.event.KeyEvent.VK_COMMA;
            case com.jogamp.newt.event.KeyEvent.VK_MINUS         : return java.awt.event.KeyEvent.VK_MINUS;
            case com.jogamp.newt.event.KeyEvent.VK_PERIOD        : return java.awt.event.KeyEvent.VK_PERIOD;
            case com.jogamp.newt.event.KeyEvent.VK_SLASH         : return java.awt.event.KeyEvent.VK_SLASH;
            case com.jogamp.newt.event.KeyEvent.VK_0             : return java.awt.event.KeyEvent.VK_0;
            case com.jogamp.newt.event.KeyEvent.VK_1             : return java.awt.event.KeyEvent.VK_1;
            case com.jogamp.newt.event.KeyEvent.VK_2             : return java.awt.event.KeyEvent.VK_2;
            case com.jogamp.newt.event.KeyEvent.VK_3             : return java.awt.event.KeyEvent.VK_3;
            case com.jogamp.newt.event.KeyEvent.VK_4             : return java.awt.event.KeyEvent.VK_4;
            case com.jogamp.newt.event.KeyEvent.VK_5             : return java.awt.event.KeyEvent.VK_5;
            case com.jogamp.newt.event.KeyEvent.VK_6             : return java.awt.event.KeyEvent.VK_6;
            case com.jogamp.newt.event.KeyEvent.VK_7             : return java.awt.event.KeyEvent.VK_7;
            case com.jogamp.newt.event.KeyEvent.VK_8             : return java.awt.event.KeyEvent.VK_8;
            case com.jogamp.newt.event.KeyEvent.VK_9             : return java.awt.event.KeyEvent.VK_9;
            case com.jogamp.newt.event.KeyEvent.VK_COLON         : return java.awt.event.KeyEvent.VK_COLON;
            case com.jogamp.newt.event.KeyEvent.VK_SEMICOLON     : return java.awt.event.KeyEvent.VK_SEMICOLON;
            case com.jogamp.newt.event.KeyEvent.VK_LESS          : return java.awt.event.KeyEvent.VK_LESS;
            case com.jogamp.newt.event.KeyEvent.VK_EQUALS        : return java.awt.event.KeyEvent.VK_EQUALS;
            case com.jogamp.newt.event.KeyEvent.VK_GREATER       : return java.awt.event.KeyEvent.VK_GREATER;
            case com.jogamp.newt.event.KeyEvent.VK_QUESTIONMARK  : return defAwtKeyCode;
            case com.jogamp.newt.event.KeyEvent.VK_AT            : return java.awt.event.KeyEvent.VK_AT;
            case com.jogamp.newt.event.KeyEvent.VK_A             : return java.awt.event.KeyEvent.VK_A;
            case com.jogamp.newt.event.KeyEvent.VK_B             : return java.awt.event.KeyEvent.VK_B;
            case com.jogamp.newt.event.KeyEvent.VK_C             : return java.awt.event.KeyEvent.VK_C;
            case com.jogamp.newt.event.KeyEvent.VK_D             : return java.awt.event.KeyEvent.VK_D;
            case com.jogamp.newt.event.KeyEvent.VK_E             : return java.awt.event.KeyEvent.VK_E;
            case com.jogamp.newt.event.KeyEvent.VK_F             : return java.awt.event.KeyEvent.VK_F;
            case com.jogamp.newt.event.KeyEvent.VK_G             : return java.awt.event.KeyEvent.VK_G;
            case com.jogamp.newt.event.KeyEvent.VK_H             : return java.awt.event.KeyEvent.VK_H;
            case com.jogamp.newt.event.KeyEvent.VK_I             : return java.awt.event.KeyEvent.VK_I;
            case com.jogamp.newt.event.KeyEvent.VK_J             : return java.awt.event.KeyEvent.VK_J;
            case com.jogamp.newt.event.KeyEvent.VK_K             : return java.awt.event.KeyEvent.VK_K;
            case com.jogamp.newt.event.KeyEvent.VK_L             : return java.awt.event.KeyEvent.VK_L;
            case com.jogamp.newt.event.KeyEvent.VK_M             : return java.awt.event.KeyEvent.VK_M;
            case com.jogamp.newt.event.KeyEvent.VK_N             : return java.awt.event.KeyEvent.VK_N;
            case com.jogamp.newt.event.KeyEvent.VK_O             : return java.awt.event.KeyEvent.VK_O;
            case com.jogamp.newt.event.KeyEvent.VK_P             : return java.awt.event.KeyEvent.VK_P;
            case com.jogamp.newt.event.KeyEvent.VK_Q             : return java.awt.event.KeyEvent.VK_Q;
            case com.jogamp.newt.event.KeyEvent.VK_R             : return java.awt.event.KeyEvent.VK_R;
            case com.jogamp.newt.event.KeyEvent.VK_S             : return java.awt.event.KeyEvent.VK_S;
            case com.jogamp.newt.event.KeyEvent.VK_T             : return java.awt.event.KeyEvent.VK_T;
            case com.jogamp.newt.event.KeyEvent.VK_U             : return java.awt.event.KeyEvent.VK_U;
            case com.jogamp.newt.event.KeyEvent.VK_V             : return java.awt.event.KeyEvent.VK_V;
            case com.jogamp.newt.event.KeyEvent.VK_W             : return java.awt.event.KeyEvent.VK_W;
            case com.jogamp.newt.event.KeyEvent.VK_X             : return java.awt.event.KeyEvent.VK_X;
            case com.jogamp.newt.event.KeyEvent.VK_Y             : return java.awt.event.KeyEvent.VK_Y;
            case com.jogamp.newt.event.KeyEvent.VK_Z             : return java.awt.event.KeyEvent.VK_Z;
            case com.jogamp.newt.event.KeyEvent.VK_OPEN_BRACKET  : return java.awt.event.KeyEvent.VK_OPEN_BRACKET;
            case com.jogamp.newt.event.KeyEvent.VK_BACK_SLASH    : return java.awt.event.KeyEvent.VK_BACK_SLASH;
            case com.jogamp.newt.event.KeyEvent.VK_CLOSE_BRACKET : return java.awt.event.KeyEvent.VK_CLOSE_BRACKET;
            case com.jogamp.newt.event.KeyEvent.VK_CIRCUMFLEX    : return java.awt.event.KeyEvent.VK_CIRCUMFLEX;
            case com.jogamp.newt.event.KeyEvent.VK_UNDERSCORE    : return java.awt.event.KeyEvent.VK_UNDERSCORE;
            case com.jogamp.newt.event.KeyEvent.VK_BACK_QUOTE    : return java.awt.event.KeyEvent.VK_BACK_QUOTE;
            case com.jogamp.newt.event.KeyEvent.VK_F1            : return java.awt.event.KeyEvent.VK_F1;
            case com.jogamp.newt.event.KeyEvent.VK_F2            : return java.awt.event.KeyEvent.VK_F2;
            case com.jogamp.newt.event.KeyEvent.VK_F3            : return java.awt.event.KeyEvent.VK_F3;
            case com.jogamp.newt.event.KeyEvent.VK_F4            : return java.awt.event.KeyEvent.VK_F4;
            case com.jogamp.newt.event.KeyEvent.VK_F5            : return java.awt.event.KeyEvent.VK_F5;
            case com.jogamp.newt.event.KeyEvent.VK_F6            : return java.awt.event.KeyEvent.VK_F6;
            case com.jogamp.newt.event.KeyEvent.VK_F7            : return java.awt.event.KeyEvent.VK_F7;
            case com.jogamp.newt.event.KeyEvent.VK_F8            : return java.awt.event.KeyEvent.VK_F8;
            case com.jogamp.newt.event.KeyEvent.VK_F9            : return java.awt.event.KeyEvent.VK_F9;
            case com.jogamp.newt.event.KeyEvent.VK_F10           : return java.awt.event.KeyEvent.VK_F10;
            case com.jogamp.newt.event.KeyEvent.VK_F11           : return java.awt.event.KeyEvent.VK_F11;
            case com.jogamp.newt.event.KeyEvent.VK_F12           : return java.awt.event.KeyEvent.VK_F12;
            case com.jogamp.newt.event.KeyEvent.VK_F13           : return java.awt.event.KeyEvent.VK_F13;
            case com.jogamp.newt.event.KeyEvent.VK_F14           : return java.awt.event.KeyEvent.VK_F14;
            case com.jogamp.newt.event.KeyEvent.VK_F15           : return java.awt.event.KeyEvent.VK_F15;
            case com.jogamp.newt.event.KeyEvent.VK_F16           : return java.awt.event.KeyEvent.VK_F16;
            case com.jogamp.newt.event.KeyEvent.VK_F17           : return java.awt.event.KeyEvent.VK_F17;
            case com.jogamp.newt.event.KeyEvent.VK_F18           : return java.awt.event.KeyEvent.VK_F18;
            case com.jogamp.newt.event.KeyEvent.VK_F19           : return java.awt.event.KeyEvent.VK_F19;
            case com.jogamp.newt.event.KeyEvent.VK_F20           : return java.awt.event.KeyEvent.VK_F20;
            case com.jogamp.newt.event.KeyEvent.VK_F21           : return java.awt.event.KeyEvent.VK_F21;
            case com.jogamp.newt.event.KeyEvent.VK_F22           : return java.awt.event.KeyEvent.VK_F22;
            case com.jogamp.newt.event.KeyEvent.VK_F23           : return java.awt.event.KeyEvent.VK_F23;
            case com.jogamp.newt.event.KeyEvent.VK_F24           : return java.awt.event.KeyEvent.VK_F24;
            case com.jogamp.newt.event.KeyEvent.VK_LEFT_BRACE    : return java.awt.event.KeyEvent.VK_BRACELEFT;
            case com.jogamp.newt.event.KeyEvent.VK_PIPE          : return defAwtKeyCode;
            case com.jogamp.newt.event.KeyEvent.VK_RIGHT_BRACE   : return java.awt.event.KeyEvent.VK_BRACERIGHT;
            case com.jogamp.newt.event.KeyEvent.VK_TILDE         : return java.awt.event.KeyEvent.VK_DEAD_TILDE;
            case com.jogamp.newt.event.KeyEvent.VK_DELETE        : return java.awt.event.KeyEvent.VK_DELETE;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD0       : return java.awt.event.KeyEvent.VK_NUMPAD0;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD1       : return java.awt.event.KeyEvent.VK_NUMPAD1;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD2       : return java.awt.event.KeyEvent.VK_NUMPAD2;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD3       : return java.awt.event.KeyEvent.VK_NUMPAD3;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD4       : return java.awt.event.KeyEvent.VK_NUMPAD4;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD5       : return java.awt.event.KeyEvent.VK_NUMPAD5;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD6       : return java.awt.event.KeyEvent.VK_NUMPAD6;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD7       : return java.awt.event.KeyEvent.VK_NUMPAD7;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD8       : return java.awt.event.KeyEvent.VK_NUMPAD8;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD9       : return java.awt.event.KeyEvent.VK_NUMPAD9;
            case com.jogamp.newt.event.KeyEvent.VK_DECIMAL       : return java.awt.event.KeyEvent.VK_DECIMAL;
            case com.jogamp.newt.event.KeyEvent.VK_SEPARATOR     : return java.awt.event.KeyEvent.VK_SEPARATOR;
            case com.jogamp.newt.event.KeyEvent.VK_ADD           : return java.awt.event.KeyEvent.VK_ADD;
            case com.jogamp.newt.event.KeyEvent.VK_SUBTRACT      : return java.awt.event.KeyEvent.VK_SUBTRACT;
            case com.jogamp.newt.event.KeyEvent.VK_MULTIPLY      : return java.awt.event.KeyEvent.VK_MULTIPLY;
            case com.jogamp.newt.event.KeyEvent.VK_DIVIDE        : return java.awt.event.KeyEvent.VK_DIVIDE;
            case com.jogamp.newt.event.KeyEvent.VK_NUM_LOCK      : return java.awt.event.KeyEvent.VK_NUM_LOCK;
            case com.jogamp.newt.event.KeyEvent.VK_LEFT          : return java.awt.event.KeyEvent.VK_LEFT;
            case com.jogamp.newt.event.KeyEvent.VK_UP            : return java.awt.event.KeyEvent.VK_UP;
            case com.jogamp.newt.event.KeyEvent.VK_RIGHT         : return java.awt.event.KeyEvent.VK_RIGHT;
            case com.jogamp.newt.event.KeyEvent.VK_DOWN          : return java.awt.event.KeyEvent.VK_DOWN;
            case com.jogamp.newt.event.KeyEvent.VK_CONTEXT_MENU  : return java.awt.event.KeyEvent.VK_CONTEXT_MENU;
            case com.jogamp.newt.event.KeyEvent.VK_WINDOWS       : return java.awt.event.KeyEvent.VK_WINDOWS;
            case com.jogamp.newt.event.KeyEvent.VK_META          : return java.awt.event.KeyEvent.VK_META;
            case com.jogamp.newt.event.KeyEvent.VK_HELP          : return java.awt.event.KeyEvent.VK_HELP;
            case com.jogamp.newt.event.KeyEvent.VK_COMPOSE       : return java.awt.event.KeyEvent.VK_COMPOSE;
            case com.jogamp.newt.event.KeyEvent.VK_BEGIN         : return java.awt.event.KeyEvent.VK_BEGIN;
            case com.jogamp.newt.event.KeyEvent.VK_STOP          : return java.awt.event.KeyEvent.VK_STOP;
            case com.jogamp.newt.event.KeyEvent.VK_INVERTED_EXCLAMATION_MARK: return java.awt.event.KeyEvent.VK_INVERTED_EXCLAMATION_MARK;
            case com.jogamp.newt.event.KeyEvent.VK_EURO_SIGN     : return java.awt.event.KeyEvent.VK_EURO_SIGN;
            case com.jogamp.newt.event.KeyEvent.VK_CUT           : return java.awt.event.KeyEvent.VK_CUT;
            case com.jogamp.newt.event.KeyEvent.VK_COPY          : return java.awt.event.KeyEvent.VK_COPY;
            case com.jogamp.newt.event.KeyEvent.VK_PASTE         : return java.awt.event.KeyEvent.VK_PASTE;
            case com.jogamp.newt.event.KeyEvent.VK_UNDO          : return java.awt.event.KeyEvent.VK_UNDO;
            case com.jogamp.newt.event.KeyEvent.VK_AGAIN         : return java.awt.event.KeyEvent.VK_AGAIN;
            case com.jogamp.newt.event.KeyEvent.VK_FIND          : return java.awt.event.KeyEvent.VK_FIND;
            case com.jogamp.newt.event.KeyEvent.VK_PROPS         : return java.awt.event.KeyEvent.VK_PROPS;
            case com.jogamp.newt.event.KeyEvent.VK_INPUT_METHOD_ON_OFF: return java.awt.event.KeyEvent.VK_INPUT_METHOD_ON_OFF;
            case com.jogamp.newt.event.KeyEvent.VK_CODE_INPUT    : return java.awt.event.KeyEvent.VK_CODE_INPUT;
            case com.jogamp.newt.event.KeyEvent.VK_ROMAN_CHARACTERS: return java.awt.event.KeyEvent.VK_ROMAN_CHARACTERS;
            case com.jogamp.newt.event.KeyEvent.VK_ALL_CANDIDATES: return java.awt.event.KeyEvent.VK_ALL_CANDIDATES;
            case com.jogamp.newt.event.KeyEvent.VK_PREVIOUS_CANDIDATE: return java.awt.event.KeyEvent.VK_PREVIOUS_CANDIDATE;
            case com.jogamp.newt.event.KeyEvent.VK_ALPHANUMERIC  : return java.awt.event.KeyEvent.VK_ALPHANUMERIC;
            case com.jogamp.newt.event.KeyEvent.VK_KATAKANA      : return java.awt.event.KeyEvent.VK_KATAKANA;
            case com.jogamp.newt.event.KeyEvent.VK_HIRAGANA      : return java.awt.event.KeyEvent.VK_HIRAGANA;
            case com.jogamp.newt.event.KeyEvent.VK_FULL_WIDTH    : return java.awt.event.KeyEvent.VK_FULL_WIDTH;
            case com.jogamp.newt.event.KeyEvent.VK_HALF_WIDTH    : return java.awt.event.KeyEvent.VK_HALF_WIDTH;
            case com.jogamp.newt.event.KeyEvent.VK_JAPANESE_KATAKANA: return java.awt.event.KeyEvent.VK_JAPANESE_KATAKANA;
            case com.jogamp.newt.event.KeyEvent.VK_JAPANESE_HIRAGANA: return java.awt.event.KeyEvent.VK_JAPANESE_HIRAGANA;
            case com.jogamp.newt.event.KeyEvent.VK_JAPANESE_ROMAN: return java.awt.event.KeyEvent.VK_JAPANESE_ROMAN;
            case com.jogamp.newt.event.KeyEvent.VK_KANA_LOCK     : return java.awt.event.KeyEvent.VK_KANA_LOCK;
        }
        return defAwtKeyCode;
    }

    public static final com.jogamp.newt.event.WindowEvent createWindowEvent(final java.awt.event.WindowEvent event, final NativeSurfaceHolder sourceHolder) {
        final short newtType = eventTypeAWT2NEWT(event.getID());
        if( (short)0 != newtType ) {
            return new com.jogamp.newt.event.WindowEvent(newtType, sourceHolder, System.currentTimeMillis());
        }
        return null; // no mapping ..
    }

    public static final com.jogamp.newt.event.WindowEvent createWindowEvent(final java.awt.event.ComponentEvent event, final NativeSurfaceHolder sourceHolder) {
        final short newtType = eventTypeAWT2NEWT(event.getID());
        if( (short)0 != newtType ) {
            return new com.jogamp.newt.event.WindowEvent(newtType, sourceHolder, System.currentTimeMillis());
        }
        return null; // no mapping ..
    }

    public static final com.jogamp.newt.event.WindowEvent createWindowEvent(final java.awt.event.FocusEvent event, final NativeSurfaceHolder sourceHolder) {
        final short newtType = eventTypeAWT2NEWT(event.getID());
        if( (short)0 != newtType ) {
            return new com.jogamp.newt.event.WindowEvent(newtType, sourceHolder, System.currentTimeMillis());
        }
        return null; // no mapping ..
    }

    public static final com.jogamp.newt.event.MouseEvent createMouseEvent(final java.awt.event.MouseEvent event, final NativeSurfaceHolder sourceHolder) {
        final short newtType = eventTypeAWT2NEWT(event.getID());
        if( (short)0 != newtType ) {
            float rotation = 0;
            if (event instanceof java.awt.event.MouseWheelEvent) {
                // AWT/NEWT rotation is reversed - AWT +1 is down, NEWT +1 is up.
                rotation = -1f * ((java.awt.event.MouseWheelEvent)event).getWheelRotation();
            }

            final short newtButton = awtButton2Newt(event.getButton());
            int mods = awtModifiers2Newt(event.getModifiers(), event.getModifiersEx());
            mods |= com.jogamp.newt.event.InputEvent.getButtonMask(newtButton); // always include NEWT BUTTON_MASK
            final NativeSurface source = sourceHolder.getNativeSurface();
            final int[] pixelPos;
            if( null != source ) {
                if( source instanceof com.jogamp.newt.Window ) {
                    final com.jogamp.newt.Window newtSource = (com.jogamp.newt.Window) source;
                    if(newtSource.isPointerConfined()) {
                        mods |= com.jogamp.newt.event.InputEvent.CONFINED_MASK;
                    }
                    if(!newtSource.isPointerVisible()) {
                        mods |= com.jogamp.newt.event.InputEvent.INVISIBLE_MASK;
                    }
                }
                pixelPos = source.convertToPixelUnits(new int[] { event.getX(), event.getY() });
            } else {
                pixelPos = new int[] { event.getX(), event.getY() };
            }

            return new com.jogamp.newt.event.MouseEvent(
                           newtType, sourceHolder, event.getWhen(),
                           mods, pixelPos[0], pixelPos[1], (short)event.getClickCount(),
                           newtButton, MouseEvent.getRotationXYZ(rotation, mods), 1f);
        }
        return null; // no mapping ..
    }

    public static final com.jogamp.newt.event.KeyEvent createKeyEvent(final java.awt.event.KeyEvent event, final NativeSurfaceHolder sourceHolder) {
        return createKeyEvent(eventTypeAWT2NEWT(event.getID()), event, sourceHolder);
    }

    public static final com.jogamp.newt.event.KeyEvent createKeyEvent(final short newtType, final java.awt.event.KeyEvent event, final NativeSurfaceHolder sourceHolder) {
        if( (short)0 != newtType ) {
            final short newtKeyCode = awtKeyCode2NewtKeyCode( event.getKeyCode() );
            return com.jogamp.newt.event.KeyEvent.create(
                           newtType, sourceHolder, event.getWhen(),
                           awtModifiers2Newt(event.getModifiers(), event.getModifiersEx()),
                           newtKeyCode, newtKeyCode, event.getKeyChar());
        }
        return null; // no mapping ..
    }

}
