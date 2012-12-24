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

import com.jogamp.common.util.IntIntHashMap;

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

    protected static final IntIntHashMap eventTypeAWT2NEWT;
    
    /** zero-based AWT button mask array filled by {@link #getAWTButtonDownMask(int)}, allowing fast lookup. */
    private static int awtButtonDownMasks[] ;

    static {
        IntIntHashMap map = new IntIntHashMap();
        map.setKeyNotFoundValue(0xFFFFFFFF);
        // n/a map.put(java.awt.event.WindowEvent.WINDOW_OPENED, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_OPENED);
        map.put(java.awt.event.WindowEvent.WINDOW_CLOSING, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_DESTROY_NOTIFY);
        map.put(java.awt.event.WindowEvent.WINDOW_CLOSED, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_DESTROYED);
        // n/a map.put(java.awt.event.WindowEvent.WINDOW_ICONIFIED, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_ICONIFIED);
        // n/a map.put(java.awt.event.WindowEvent.WINDOW_DEICONIFIED, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_DEICONIFIED);
        map.put(java.awt.event.WindowEvent.WINDOW_ACTIVATED, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_GAINED_FOCUS);
        map.put(java.awt.event.WindowEvent.WINDOW_GAINED_FOCUS, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_GAINED_FOCUS);
        map.put(java.awt.event.FocusEvent.FOCUS_GAINED, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_GAINED_FOCUS);
        map.put(java.awt.event.WindowEvent.WINDOW_DEACTIVATED, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_LOST_FOCUS);
        map.put(java.awt.event.WindowEvent.WINDOW_LOST_FOCUS, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_LOST_FOCUS);
        map.put(java.awt.event.FocusEvent.FOCUS_LOST, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_LOST_FOCUS);
        // n/a map.put(java.awt.event.WindowEvent.WINDOW_STATE_CHANGED, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_STATE_CHANGED);

        map.put(java.awt.event.ComponentEvent.COMPONENT_MOVED, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_MOVED);
        map.put(java.awt.event.ComponentEvent.COMPONENT_RESIZED, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_RESIZED);
        // n/a map.put(java.awt.event.ComponentEvent.COMPONENT_SHOWN, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_SHOWN);
        // n/a map.put(java.awt.event.ComponentEvent.COMPONENT_HIDDEN, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_HIDDEN);

        map.put(java.awt.event.MouseEvent.MOUSE_CLICKED, com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_CLICKED);
        map.put(java.awt.event.MouseEvent.MOUSE_PRESSED, com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_PRESSED);
        map.put(java.awt.event.MouseEvent.MOUSE_RELEASED, com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_RELEASED);
        map.put(java.awt.event.MouseEvent.MOUSE_MOVED, com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_MOVED);
        map.put(java.awt.event.MouseEvent.MOUSE_ENTERED, com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_ENTERED);
        map.put(java.awt.event.MouseEvent.MOUSE_EXITED, com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_EXITED);
        map.put(java.awt.event.MouseEvent.MOUSE_DRAGGED, com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_DRAGGED);
        map.put(java.awt.event.MouseEvent.MOUSE_WHEEL, com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_WHEEL_MOVED);

        map.put(java.awt.event.KeyEvent.KEY_PRESSED, com.jogamp.newt.event.KeyEvent.EVENT_KEY_PRESSED);
        map.put(java.awt.event.KeyEvent.KEY_RELEASED, com.jogamp.newt.event.KeyEvent.EVENT_KEY_RELEASED);
        map.put(java.awt.event.KeyEvent.KEY_TYPED, com.jogamp.newt.event.KeyEvent.EVENT_KEY_TYPED);

        eventTypeAWT2NEWT = map;
        
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
        
        awtButtonDownMasks = new int[com.jogamp.newt.event.MouseEvent.BUTTON_NUMBER] ; // java.awt.MouseInfo.getNumberOfButtons() ;
        for (int n = 0 ; n < awtButtonDownMasks.length ; ++n) {
            awtButtonDownMasks[n] = getAWTButtonDownMaskImpl(n+1);
        }
    }

    private static int getAWTButtonDownMaskImpl(int button) {
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
                if( button <= com.jogamp.newt.event.MouseEvent.BUTTON_NUMBER ) {
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
    public static int getAWTButtonDownMask(int button) {
        if( 0 < button && button <= awtButtonDownMasks.length ) {
            return awtButtonDownMasks[button-1];
        } else {
            return 0;
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
    
    public static final int awtButton2Newt(int awtButton) {
        if( 0 < awtButton && awtButton <= com.jogamp.newt.event.MouseEvent.BUTTON_NUMBER ) {
            return awtButton;
        } else {
            return 0;
        }
    }

    public static final com.jogamp.newt.event.WindowEvent createWindowEvent(java.awt.event.WindowEvent event, com.jogamp.newt.Window newtSource) {
        int type = eventTypeAWT2NEWT.get(event.getID());
        if(0xFFFFFFFF != type) {
            return new com.jogamp.newt.event.WindowEvent(type, ((null==newtSource)?(Object)event.getComponent():(Object)newtSource), System.currentTimeMillis());
        }
        return null; // no mapping ..
    }

    public static final com.jogamp.newt.event.WindowEvent createWindowEvent(java.awt.event.ComponentEvent event, com.jogamp.newt.Window newtSource) {
        int type = eventTypeAWT2NEWT.get(event.getID());
        if(0xFFFFFFFF != type) {
            return new com.jogamp.newt.event.WindowEvent(type, (null==newtSource)?(Object)event.getComponent():(Object)newtSource, System.currentTimeMillis());
        }
        return null; // no mapping ..
    }

    public static final com.jogamp.newt.event.WindowEvent createWindowEvent(java.awt.event.FocusEvent event, com.jogamp.newt.Window newtSource) {
        int type = eventTypeAWT2NEWT.get(event.getID());
        if(0xFFFFFFFF != type) {
            return new com.jogamp.newt.event.WindowEvent(type, (null==newtSource)?(Object)event.getComponent():(Object)newtSource, System.currentTimeMillis());
        }
        return null; // no mapping ..
    }

    public static final com.jogamp.newt.event.MouseEvent createMouseEvent(java.awt.event.MouseEvent event, com.jogamp.newt.Window newtSource) {
        int type = eventTypeAWT2NEWT.get(event.getID());
        if(0xFFFFFFFF != type) {
            int rotation = 0;
            if (event instanceof java.awt.event.MouseWheelEvent) {
                // AWT/NEWT rotation is reversed - AWT +1 is down, NEWT +1 is up.
                rotation = -1 * ((java.awt.event.MouseWheelEvent)event).getWheelRotation();
            }

            final int newtButton = awtButton2Newt(event.getButton());
            int mods = awtModifiers2Newt(event.getModifiers(), event.getModifiersEx());
            mods |= com.jogamp.newt.event.InputEvent.getButtonMask(newtButton); // always include NEWT BUTTON_MASK
            if(null!=newtSource) {
                if(newtSource.isPointerConfined()) {
                    mods |= com.jogamp.newt.event.InputEvent.CONFINED_MASK;
                }
                if(!newtSource.isPointerVisible()) {
                    mods |= com.jogamp.newt.event.InputEvent.INVISIBLE_MASK;
                }
            }
            return new com.jogamp.newt.event.MouseEvent(
                           type, (null==newtSource)?(Object)event.getComponent():(Object)newtSource, event.getWhen(),
                           mods, event.getX(), event.getY(), event.getClickCount(), 
                           newtButton, rotation);
        }
        return null; // no mapping ..
    }

    public static final com.jogamp.newt.event.KeyEvent createKeyEvent(java.awt.event.KeyEvent event, com.jogamp.newt.Window newtSource) {
        int type = eventTypeAWT2NEWT.get(event.getID());
        if(0xFFFFFFFF != type) {
            return new com.jogamp.newt.event.KeyEvent(
                           type, (null==newtSource)?(Object)event.getComponent():(Object)newtSource, event.getWhen(), 
                           awtModifiers2Newt(event.getModifiers(), event.getModifiersEx()), 
                           event.getKeyCode(), event.getKeyChar());
        }
        return null; // no mapping ..
    }

}

