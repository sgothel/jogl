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

import com.jogamp.common.os.AndroidVersion;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.NEWTEvent;

public class AndroidNewtEventFactory {

    private static final String names[] = { "DOWN" , "UP" , "MOVE", "CANCEL" , "OUTSIDE",              // 0 -  4
                                            "POINTER_DOWN" , "POINTER_UP" , "HOVER_MOVE" , "SCROLL",   // 5 -  8
                                            "HOVER_ENTER", "HOVER_EXIT"                                // 0 - 10
                                          };
    
    /** API Level 12: {@link android.view.MotionEvent#ACTION_SCROLL} = {@value} */
    private static final int ACTION_SCROLL = 8;
    
    private static final short aMotionEventType2Newt(int aType) {
        switch( aType ) {
            case android.view.MotionEvent.ACTION_DOWN: 
                return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_PRESSED; 
            case android.view.MotionEvent.ACTION_UP: 
                return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_RELEASED; 
            case android.view.MotionEvent.ACTION_MOVE: 
                return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_DRAGGED; 
            case android.view.MotionEvent.ACTION_CANCEL: 
                return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_RELEASED; 
            case android.view.MotionEvent.ACTION_OUTSIDE: 
                return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_MOVED; 
            case android.view.MotionEvent.ACTION_POINTER_DOWN: 
                return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_PRESSED; 
            case android.view.MotionEvent.ACTION_POINTER_UP: 
                return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_RELEASED; 
            // case ACTION_HOVER_MOVE
            case ACTION_SCROLL:  // API Level 12 !
                return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_WHEEL_MOVED; 
            // case ACTION_HOVER_ENTER
            // case ACTION_HOVER_EXIT
        }
        return (short)0;
    }
    
    private static final short aAccessibilityEventType2Newt(int aType) {
        switch( aType ) {
            case android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED: 
                return com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_GAINED_FOCUS;
        }
        return (short)0;
    }

    private static final short aKeyEventType2NewtEventType(int androidKeyAction) {
        switch(androidKeyAction) {
            case android.view.KeyEvent.ACTION_DOWN: 
            case android.view.KeyEvent.ACTION_MULTIPLE:
                return com.jogamp.newt.event.KeyEvent.EVENT_KEY_PRESSED;
            case android.view.KeyEvent.ACTION_UP:
                return com.jogamp.newt.event.KeyEvent.EVENT_KEY_RELEASED;
        }
        return (short)0;
    }
    
    private static final short aKeyCode2NewtKeyCode(int androidKeyCode, boolean inclSysKeys) {
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
            case android.view.KeyEvent.KEYCODE_DEL: return com.jogamp.newt.event.KeyEvent.VK_DELETE;
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
                    return com.jogamp.newt.event.KeyEvent.VK_KEYBOARD_INVISIBLE;
                }
                break;
            case android.view.KeyEvent.KEYCODE_HOME:
                if( inclSysKeys ) {
                    return com.jogamp.newt.event.KeyEvent.VK_HOME;
                }
                break;
        }        
        return (short)0;
    }
    
    private static final int aKeyModifiers2Newt(int androidMods) {
        int newtMods = 0;
        if ((androidMods & android.view.KeyEvent.META_SYM_ON)   != 0)   newtMods |= com.jogamp.newt.event.InputEvent.META_MASK;
        if ((androidMods & android.view.KeyEvent.META_SHIFT_ON) != 0)   newtMods |= com.jogamp.newt.event.InputEvent.SHIFT_MASK;
        if ((androidMods & android.view.KeyEvent.META_ALT_ON)   != 0)   newtMods |= com.jogamp.newt.event.InputEvent.ALT_MASK;
        
        return newtMods;
    }
    
    public static com.jogamp.newt.event.WindowEvent createWindowEvent(android.view.accessibility.AccessibilityEvent event, com.jogamp.newt.Window newtSource) {
        final int aType = event.getEventType();
        final short nType = aAccessibilityEventType2Newt(aType);
        
        if( (short)0 != nType) {
            return new com.jogamp.newt.event.WindowEvent(nType, ((null==newtSource)?null:(Object)newtSource), event.getEventTime());
        }
        return null; // no mapping ..
    }

    
    public static com.jogamp.newt.event.KeyEvent createKeyEvent(android.view.KeyEvent aEvent, com.jogamp.newt.Window newtSource, boolean inclSysKeys) {
        final com.jogamp.newt.event.KeyEvent res;
        final short newtType = aKeyEventType2NewtEventType(aEvent.getAction());        
        if( (short)0 != newtType) {
            final short newtKeyCode = aKeyCode2NewtKeyCode(aEvent.getKeyCode(), inclSysKeys);
            res = createKeyEventImpl(aEvent, newtType, newtKeyCode, newtSource);
        } else {
            res = null;
        }
        if(Window.DEBUG_KEY_EVENT) {
            System.err.println("createKeyEvent0: "+aEvent+" -> "+res);
        }
        return res;
    }

    public static com.jogamp.newt.event.KeyEvent createKeyEvent(android.view.KeyEvent aEvent, short newtType, com.jogamp.newt.Window newtSource, boolean inclSysKeys) {
        final short newtKeyCode = aKeyCode2NewtKeyCode(aEvent.getKeyCode(), inclSysKeys);
        final com.jogamp.newt.event.KeyEvent res = createKeyEventImpl(aEvent, newtType, newtKeyCode, newtSource);
        if(Window.DEBUG_KEY_EVENT) {
            System.err.println("createKeyEvent1: newtType "+NEWTEvent.toHexString(newtType)+", "+aEvent+" -> "+res);
        }
        return res;
    }
    
    private static com.jogamp.newt.event.KeyEvent createKeyEventImpl(android.view.KeyEvent aEvent, short newtType, short newtKeyCode, com.jogamp.newt.Window newtSource) {
        if( (short)0 != newtType && (short)0 != newtKeyCode ) {
            final Object src = null==newtSource ? null : newtSource;
            final long unixTime = System.currentTimeMillis() + ( aEvent.getEventTime() - android.os.SystemClock.uptimeMillis() );
            final int newtMods = aKeyModifiers2Newt(aEvent.getMetaState());
            
            return new com.jogamp.newt.event.KeyEvent(
                                newtType, src, unixTime, newtMods, newtKeyCode, newtKeyCode, (char) aEvent.getUnicodeChar());
        }
        return null;
    }
    
    private final NewtGestureListener gestureListener;
    private final android.view.GestureDetector gestureDetector;
    private final float touchSlop;
    
    public AndroidNewtEventFactory(android.content.Context context, android.os.Handler handler) {
        gestureListener = new NewtGestureListener();
        gestureDetector = new android.view.GestureDetector(context, gestureListener, handler, false /* ignoreMultitouch */);
        gestureDetector.setIsLongpressEnabled(false); // favor scroll event!
        final android.view.ViewConfiguration configuration = android.view.ViewConfiguration.get(context);
        touchSlop = configuration.getScaledTouchSlop();
    }
        
    private int gestureScrollPointerDown = 0;
    
    public com.jogamp.newt.event.MouseEvent[] createMouseEvents(boolean isOnTouchEvent, 
                                                                android.view.MotionEvent event, com.jogamp.newt.Window newtSource) {
        if(Window.DEBUG_MOUSE_EVENT) {
            System.err.println("createMouseEvent: "+toString(event));                               
        }

        //
        // Prefilter Android Event (Gesture, ..) and determine final type
        //
        final int aType;
        final short nType;
        float[] rotationXY = null;
        int rotationSource = 0; // 1 - Gesture, 2 - ACTION_SCROLL
        {                
            final int pointerCount = event.getPointerCount();
            final boolean gestureEvent = isOnTouchEvent && pointerCount>1 && gestureDetector.onTouchEvent(event);
            int _aType = 0xFFFFFFFF;
            if( gestureEvent ) {
                rotationXY = gestureListener.getScrollDistanceXY();
                if( null != rotationXY) {
                    final boolean skip = 0 == gestureScrollPointerDown; // skip 1st .. too bug distance
                    gestureScrollPointerDown = pointerCount;
                    if( skip ) {
                        if(Window.DEBUG_MOUSE_EVENT) {
                            System.err.println("createMouseEvent: GestureEvent Scroll Start - SKIP "+rotationXY[0]+"/"+rotationXY[1]+", gestureScrollPointerDown "+gestureScrollPointerDown);
                        }
                        return null;
                    }
                    _aType = ACTION_SCROLL; // 8
                    rotationSource = 1;
                } else {
                    throw new InternalError("Gesture Internal Error: consumed onTouchEvent, but no result (Scroll)");
                }
            }
            if( 0xFFFFFFFF == _aType ) {
                _aType = event.getActionMasked();
            }
            aType = _aType;
            nType = aMotionEventType2Newt(aType);
            
            //
            // Check whether events shall be skipped
            //
            if( !gestureEvent ) {
                // Scroll Gesture: Wait for all pointers up - ACTION_UP, ACTION_POINTER_UP
                if( 0 < gestureScrollPointerDown ) {
                    if( com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_RELEASED == nType ) {
                        gestureScrollPointerDown--;
                    }
                    if(Window.DEBUG_MOUSE_EVENT) {
                        System.err.println("createMouseEvent: !GestureEvent SKIP gestureScrollPointerDown "+gestureScrollPointerDown);
                    }
                    return null;
                }
            }
        }
        
        if( (short)0 != nType ) {            
            final short clickCount = 1;
            int modifiers = 0;
            
            if( null == rotationXY && AndroidVersion.SDK_INT >= 12 && ACTION_SCROLL == aType ) { // API Level 12
                rotationXY = new float[] { event.getAxisValue(android.view.MotionEvent.AXIS_X),
                                           event.getAxisValue(android.view.MotionEvent.AXIS_Y) };
                rotationSource = 2;
            }
            
            final float rotation;
            if( null != rotationXY ) {
                final float _rotation;
                if( rotationXY[0]*rotationXY[0] > rotationXY[1]*rotationXY[1] ) {
                    // Horizontal
                    modifiers |= com.jogamp.newt.event.InputEvent.SHIFT_MASK;
                    _rotation = rotationXY[0];
                } else {
                    // Vertical
                    _rotation = rotationXY[1];
                }
                rotation =  _rotation / touchSlop;                
                if(Window.DEBUG_MOUSE_EVENT) {
                    System.err.println("createMouseEvent: Scroll "+rotationXY[0]+"/"+rotationXY[1]+" -> "+_rotation+" / "+touchSlop+" -> "+rotation+" scaled -- mods "+modifiers+", source "+rotationSource);
                }      
            } else {
                rotation = 0.0f;
            }
            
            //
            // Determine newt-button and whether dedicated pointer is pressed
            //
            final int pCount;
            final int pIndex;
            final short button;
            switch( aType ) {
                case android.view.MotionEvent.ACTION_POINTER_DOWN:
                case android.view.MotionEvent.ACTION_POINTER_UP: {
                        pIndex = event.getActionIndex();
                        pCount = 1;
                        final int b = event.getPointerId(pIndex) + 1; // FIXME: Assumption that Pointer-ID starts w/ 0 !
                        if( com.jogamp.newt.event.MouseEvent.BUTTON1 <= b && b <= com.jogamp.newt.event.MouseEvent.BUTTON_NUMBER ) {
                            button = (short)b;
                        } else {
                            button = com.jogamp.newt.event.MouseEvent.BUTTON1;
                        }
                    }
                    break;
                default: {
                        pIndex = 0;
                        pCount = event.getPointerCount(); // all
                        button = com.jogamp.newt.event.MouseEvent.BUTTON1;
                    }
            }
            
            //
            // Collect common data
            //
            final int[] x = new int[pCount];
            final int[] y = new int[pCount];
            final float[] pressure = new float[pCount];
            final short[] pointerIds = new short[pCount];
            {
                if(Window.DEBUG_MOUSE_EVENT) {
                    System.err.println("createMouseEvent: collect ptr-data ["+pIndex+".."+(pIndex+pCount-1)+", "+pCount+"], aType "+aType+", button "+button+", gestureScrollPointerDown "+gestureScrollPointerDown);
                }
                int i = pIndex;
                int j = 0;
                while(j < pCount) {
                    x[j] = (int)event.getX(i);
                    y[j] = (int)event.getY(i);
                    pressure[j] = event.getPressure(i);
                    pointerIds[j] = (short)event.getPointerId(i);
                    if(Window.DEBUG_MOUSE_EVENT) {
                        System.err.println("createMouseEvent: ptr-data["+i+" -> "+j+"] "+x[j]+"/"+y[j]+", pressure "+pressure[j]+", id "+pointerIds[j]);
                    }
                    i++;
                    j++;
                }
            }
            
            if(null!=newtSource) {
                if(newtSource.isPointerConfined()) {
                    modifiers |= InputEvent.CONFINED_MASK;
                }
                if(!newtSource.isPointerVisible()) {
                    modifiers |= InputEvent.INVISIBLE_MASK;
                }
            }
                                
            final Object src = (null==newtSource)?null:(Object)newtSource;
            final long unixTime = System.currentTimeMillis() + ( event.getEventTime() - android.os.SystemClock.uptimeMillis() );
            
            final com.jogamp.newt.event.MouseEvent me1 = new com.jogamp.newt.event.MouseEvent(
                           nType,  src, unixTime,
                           modifiers, x, y, pressure, pointerIds, clickCount, 
                           button, rotation);
            
            if( com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_RELEASED == nType ) {
                return new com.jogamp.newt.event.MouseEvent[] { me1, 
                    new com.jogamp.newt.event.MouseEvent(
                           com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_CLICKED, 
                           src, unixTime, modifiers, x, y, pressure, pointerIds, clickCount, 
                           button, rotation) };
            } else {
                return new com.jogamp.newt.event.MouseEvent[] { me1 };
            }
        } 
        return null; // no mapping ..
    }
    
    
    public static String toString(android.view.MotionEvent event) {
        StringBuilder sb = new StringBuilder();
        int action = event.getAction();
        int actionCode = action & android.view.MotionEvent.ACTION_MASK;
        sb.append("ACTION_" ).append(names[actionCode]);
        if (actionCode == android.view.MotionEvent.ACTION_POINTER_DOWN
                || actionCode == android.view.MotionEvent.ACTION_POINTER_UP) {
            sb.append("(pid " ).append(
                    action >> android.view.MotionEvent.ACTION_POINTER_ID_SHIFT);
            sb.append(")" );
        }
        sb.append("[" );
        for (int i = 0; i < event.getPointerCount(); i++) {
            sb.append("#" ).append(i);
            sb.append("(pid " ).append(event.getPointerId(i));
            sb.append(")=" ).append((int) event.getX(i));
            sb.append("," ).append((int) event.getY(i));
            if (i + 1 < event.getPointerCount())
                sb.append(";" );
        }
        sb.append("]" );
        return sb.toString();
    }
    
    class NewtGestureListener implements android.view.GestureDetector.OnGestureListener {
        private float[] scrollDistance;
        
        NewtGestureListener() {
            scrollDistance = null;
        }
        
        /** Returns non null w/ 2 float values, XY, if storing onScroll's XY distance - otherwise null */ 
        public float[] getScrollDistanceXY() {
            float[] sd = scrollDistance;
            scrollDistance = null;
            return sd;
        }
        
        //
        // Simple feedback
        //
        
        @Override
        public void onShowPress(android.view.MotionEvent e) {
        }

        @Override
        public void onLongPress(android.view.MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(android.view.MotionEvent e) {
            return false;
        }
        
        //
        // Consumed or not consumed !
        //

        @Override
        public boolean onDown(android.view.MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(android.view.MotionEvent e1, android.view.MotionEvent e2, float distanceX, float distanceY) {
            scrollDistance = new float[] { distanceX, distanceY };
            return true;
        }

        @Override
        public boolean onFling(android.view.MotionEvent e1, android.view.MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }        
    };
}

