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

import java.awt.event.MouseEvent;

import com.jogamp.common.util.IntIntHashMap;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.InputEvent;

public class AndroidNewtEventFactory {

    protected static final IntIntHashMap eventTypeANDROID2NEWT;

    private static final String names[] = { "DOWN" , "UP" , "MOVE" , "CANCEL" , "OUTSIDE" ,
                                            "POINTER_DOWN" , "POINTER_UP" , "7?" , "8?" , "9?" };
    
    static {
        IntIntHashMap map = new IntIntHashMap();
        map.setKeyNotFoundValue(0xFFFFFFFF);
        
        map.put(android.view.MotionEvent.ACTION_DOWN, com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_PRESSED);
        map.put(android.view.MotionEvent.ACTION_UP, com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_RELEASED);
        map.put(android.view.MotionEvent.ACTION_CANCEL, com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_RELEASED);
        map.put(android.view.MotionEvent.ACTION_MOVE, com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_DRAGGED);
        map.put(android.view.MotionEvent.ACTION_OUTSIDE, com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_MOVED);
        
        map.put(android.view.MotionEvent.ACTION_POINTER_DOWN, com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_PRESSED);
        map.put(android.view.MotionEvent.ACTION_POINTER_UP, com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_RELEASED);
        
        map.put(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_GAINED_FOCUS);

        eventTypeANDROID2NEWT = map;
    }

    static final int androidKeyCode2Newt(int androidKeyCode) {
        if(android.view.KeyEvent.KEYCODE_0 <= androidKeyCode && androidKeyCode <= android.view.KeyEvent.KEYCODE_9) {
            return com.jogamp.newt.event.KeyEvent.VK_0 + ( androidKeyCode - android.view.KeyEvent.KEYCODE_0 ) ; 
        }
        if(android.view.KeyEvent.KEYCODE_A <= androidKeyCode && androidKeyCode <= android.view.KeyEvent.KEYCODE_Z) {
            return com.jogamp.newt.event.KeyEvent.VK_A + ( androidKeyCode - android.view.KeyEvent.KEYCODE_A ) ; 
        }
        if(android.view.KeyEvent.KEYCODE_F1 <= androidKeyCode && androidKeyCode <= android.view.KeyEvent.KEYCODE_F12) {
            return com.jogamp.newt.event.KeyEvent.VK_F1 + ( androidKeyCode - android.view.KeyEvent.KEYCODE_F1 ) ; 
        }
        if(android.view.KeyEvent.KEYCODE_NUMPAD_0 <= androidKeyCode && androidKeyCode <= android.view.KeyEvent.KEYCODE_NUMPAD_9) {
            return com.jogamp.newt.event.KeyEvent.VK_NUMPAD0 + ( androidKeyCode - android.view.KeyEvent.KEYCODE_NUMPAD_0 ) ; 
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
            // case android.view.KeyEvent.KEYCODE_HOME: return com.jogamp.newt.event.KeyEvent.VK_HOME;
            // case android.view.KeyEvent.KEYCODE_BACK: return com.jogamp.newt.event.KeyEvent.VK_BACK_SPACE;
            case android.view.KeyEvent.KEYCODE_ESCAPE: return com.jogamp.newt.event.KeyEvent.VK_ESCAPE;
            case android.view.KeyEvent.KEYCODE_CTRL_LEFT: return com.jogamp.newt.event.KeyEvent.VK_CONTROL;
            case android.view.KeyEvent.KEYCODE_CTRL_RIGHT: return com.jogamp.newt.event.KeyEvent.VK_CONTROL; // ??
        }        
        return 0;
    }
    
    public static final com.jogamp.newt.event.WindowEvent createWindowEvent(android.view.accessibility.AccessibilityEvent event, com.jogamp.newt.Window newtSource) {
        int type = eventTypeANDROID2NEWT.get(event.getEventType());
        if(0xFFFFFFFF != type) {
            return new com.jogamp.newt.event.WindowEvent(type, ((null==newtSource)?null:(Object)newtSource), event.getEventTime());
        }
        return null; // no mapping ..
    }

    static final int androidKeyModifiers2Newt(int androidMods) {
        int newtMods = 0;
        if ((androidMods & android.view.KeyEvent.META_SYM_ON)   != 0)   newtMods |= com.jogamp.newt.event.InputEvent.META_MASK;
        if ((androidMods & android.view.KeyEvent.META_SHIFT_ON) != 0)   newtMods |= com.jogamp.newt.event.InputEvent.SHIFT_MASK;
        if ((androidMods & android.view.KeyEvent.META_ALT_ON)   != 0)   newtMods |= com.jogamp.newt.event.InputEvent.ALT_MASK;
        
        return newtMods;
    }
    
    private static final int androidKeyAction2NewtEventType(int androidKeyAction) {
        switch(androidKeyAction) {
            case android.view.KeyEvent.ACTION_DOWN: 
            case android.view.KeyEvent.ACTION_MULTIPLE:
                return com.jogamp.newt.event.KeyEvent.EVENT_KEY_PRESSED;
            case android.view.KeyEvent.ACTION_UP:
                return com.jogamp.newt.event.KeyEvent.EVENT_KEY_RELEASED;
            default: 
                return 0;
        }
    }
    
    public static final com.jogamp.newt.event.KeyEvent[] createKeyEvents(int keyCode, android.view.KeyEvent event, com.jogamp.newt.Window newtSource) {
        final int type = androidKeyAction2NewtEventType(event.getAction());        
        if(Window.DEBUG_MOUSE_EVENT) {
            System.err.println("createKeyEvent: type 0x"+Integer.toHexString(type)+", keyCode 0x"+Integer.toHexString(keyCode)+", "+event);
        }
        if(0xFFFFFFFF != type) {
            final int newtKeyCode = androidKeyCode2Newt(keyCode);
            if(0 != newtKeyCode) {
                final Object src = (null==newtSource)?null:(Object)newtSource;
                final long unixTime = System.currentTimeMillis() + ( event.getEventTime() - android.os.SystemClock.uptimeMillis() );
                final int newtMods = androidKeyModifiers2Newt(event.getMetaState());
                
                final com.jogamp.newt.event.KeyEvent ke1 = new com.jogamp.newt.event.KeyEvent(
                                    type, src, unixTime, newtMods, newtKeyCode, event.getDisplayLabel());
                
                if( com.jogamp.newt.event.KeyEvent.EVENT_KEY_RELEASED == type ) {
                    return new com.jogamp.newt.event.KeyEvent[] { ke1,
                            new com.jogamp.newt.event.KeyEvent(
                                    com.jogamp.newt.event.KeyEvent.EVENT_KEY_TYPED,
                                    src, unixTime, newtMods, newtKeyCode, event.getDisplayLabel()) };
                } else {
                    return new com.jogamp.newt.event.KeyEvent[] { ke1 };
                }
            }
        }
        return null;
    }

    public static final com.jogamp.newt.event.MouseEvent[] createMouseEvents(android.view.MotionEvent event, com.jogamp.newt.Window newtSource) {
        if(Window.DEBUG_MOUSE_EVENT) {
            System.err.println("createMouseEvent: "+toString(event));
        }
        int type = eventTypeANDROID2NEWT.get(event.getAction());
        if(0xFFFFFFFF != type) {
            int rotation = 0;
            int clickCount = 1;
            int modifiers = 0;
            
            int[] x = new int[event.getPointerCount()];
            int[] y = new int[event.getPointerCount()];
            float[] pressure = new float[event.getPointerCount()];
            int[] pointers = new int[event.getPointerCount()];
            int index = 0;
            while(index < event.getPointerCount()) {
                x[index] = (int)event.getX(index);
                y[index] = (int)event.getY(index);
                pressure[index] = event.getPressure(index);
                pointers[index] = event.getPointerId(index);  
                index++;
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
            final int button = pointers.length==1 ? MouseEvent.BUTTON1 : 0;
            
            final com.jogamp.newt.event.MouseEvent me1 = new com.jogamp.newt.event.MouseEvent(
                           type,  src, unixTime,
                           modifiers, x, y, pressure, pointers, clickCount, 
                           button, rotation);
            
            if(type == com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_RELEASED) {
                return new com.jogamp.newt.event.MouseEvent[] { me1, 
                    new com.jogamp.newt.event.MouseEvent(
                           com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_CLICKED, 
                           src, unixTime, modifiers, x, y, pressure, pointers, clickCount, 
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
}

