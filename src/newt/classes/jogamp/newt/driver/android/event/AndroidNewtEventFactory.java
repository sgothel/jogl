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
        
        map.put(android.view.KeyEvent.ACTION_DOWN, com.jogamp.newt.event.KeyEvent.EVENT_KEY_PRESSED);
        map.put(android.view.KeyEvent.ACTION_UP, com.jogamp.newt.event.KeyEvent.EVENT_KEY_RELEASED);
        map.put(android.view.KeyEvent.ACTION_MULTIPLE, com.jogamp.newt.event.KeyEvent.EVENT_KEY_PRESSED);
        
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
        //safest ...but ugly
        if (android.view.KeyEvent.KEYCODE_0 == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_0;
        if (android.view.KeyEvent.KEYCODE_1 == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_1;
        if (android.view.KeyEvent.KEYCODE_2 == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_2;
        if (android.view.KeyEvent.KEYCODE_3 == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_3;
        if (android.view.KeyEvent.KEYCODE_4 == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_4;
        if (android.view.KeyEvent.KEYCODE_5 == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_5;
        if (android.view.KeyEvent.KEYCODE_6 == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_6;
        if (android.view.KeyEvent.KEYCODE_7 == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_7;
        if (android.view.KeyEvent.KEYCODE_8 == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_8;
        if (android.view.KeyEvent.KEYCODE_9 == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_9;
        
        if (android.view.KeyEvent.KEYCODE_A == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_A;
        if (android.view.KeyEvent.KEYCODE_B == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_B;
        if (android.view.KeyEvent.KEYCODE_C == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_C;
        if (android.view.KeyEvent.KEYCODE_D == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_D;
        if (android.view.KeyEvent.KEYCODE_E == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_E;
        if (android.view.KeyEvent.KEYCODE_F == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_F;
        if (android.view.KeyEvent.KEYCODE_G == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_G;
        if (android.view.KeyEvent.KEYCODE_H == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_H;
        if (android.view.KeyEvent.KEYCODE_I == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_I;
        if (android.view.KeyEvent.KEYCODE_J == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_J;
        if (android.view.KeyEvent.KEYCODE_K == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_K;
        if (android.view.KeyEvent.KEYCODE_L == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_L;
        if (android.view.KeyEvent.KEYCODE_M == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_M;
        if (android.view.KeyEvent.KEYCODE_N == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_N;
        if (android.view.KeyEvent.KEYCODE_O == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_O;
        if (android.view.KeyEvent.KEYCODE_P == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_P;
        if (android.view.KeyEvent.KEYCODE_Q == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_Q;
        if (android.view.KeyEvent.KEYCODE_R == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_R;
        if (android.view.KeyEvent.KEYCODE_S == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_S;
        if (android.view.KeyEvent.KEYCODE_T == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_T;
        if (android.view.KeyEvent.KEYCODE_U == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_U;
        if (android.view.KeyEvent.KEYCODE_V == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_V;
        if (android.view.KeyEvent.KEYCODE_W == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_W;
        if (android.view.KeyEvent.KEYCODE_X == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_X;
        if (android.view.KeyEvent.KEYCODE_Y == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_Y;
        if (android.view.KeyEvent.KEYCODE_Z == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_Z;
          
        if (android.view.KeyEvent.KEYCODE_AT == androidKeyCode) return com.jogamp.newt.event.KeyEvent.VK_AT;
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
    
    public static final com.jogamp.newt.event.KeyEvent createKeyEvent(android.view.KeyEvent event, com.jogamp.newt.Window newtSource) {
        int type = eventTypeANDROID2NEWT.get(event.getAction());
        if(0xFFFFFFFF != type) {
            return new com.jogamp.newt.event.KeyEvent(
                           type, (null==newtSource)?null:(Object)newtSource, event.getEventTime(), 
                           androidKeyModifiers2Newt(event.getMetaState()), 
                           androidKeyCode2Newt(event.getKeyCode()), event.getDisplayLabel());
        }
        return null;
    }

    public static final com.jogamp.newt.event.MouseEvent[] createMouseEvents(com.jogamp.newt.Window newtSource, android.view.MotionEvent event) {
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
                                
            com.jogamp.newt.event.MouseEvent res[];

            com.jogamp.newt.event.MouseEvent me1 = 
                    new com.jogamp.newt.event.MouseEvent(
                           type, 
                           (null==newtSource)?null:(Object)newtSource, event.getEventTime(),
                           modifiers, x, y, pressure, pointers, clickCount, 
                           pointers.length==1 ? MouseEvent.BUTTON1 : 0, rotation);
            
            if(type == com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_RELEASED) {
                com.jogamp.newt.event.MouseEvent me2 =
                    new com.jogamp.newt.event.MouseEvent(
                           com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_CLICKED, 
                           (null==newtSource)?null:(Object)newtSource, event.getEventTime(),
                           modifiers, x, y, pressure, pointers, clickCount, 
                           pointers.length==1 ? MouseEvent.BUTTON1 : 0, rotation);
                res = new com.jogamp.newt.event.MouseEvent[2];
                res[0] = me1;
                res[1] = me2;
            } else {
                res = new com.jogamp.newt.event.MouseEvent[1];
                res[0] = me1;                
            }
            return res;
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

