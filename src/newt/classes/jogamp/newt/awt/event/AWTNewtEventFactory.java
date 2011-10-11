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
import com.jogamp.newt.event.InputEvent;

public class AWTNewtEventFactory {

    protected static final IntIntHashMap eventTypeAWT2NEWT;

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
    }

    public static final int awtModifiers2Newt(int awtMods, boolean mouseHint) {
        int newtMods = 0;
        if ((awtMods & java.awt.event.InputEvent.SHIFT_MASK) != 0)     newtMods |= com.jogamp.newt.event.InputEvent.SHIFT_MASK;
        if ((awtMods & java.awt.event.InputEvent.CTRL_MASK) != 0)      newtMods |= com.jogamp.newt.event.InputEvent.CTRL_MASK;
        if ((awtMods & java.awt.event.InputEvent.META_MASK) != 0)      newtMods |= com.jogamp.newt.event.InputEvent.META_MASK;
        if ((awtMods & java.awt.event.InputEvent.ALT_MASK) != 0)       newtMods |= com.jogamp.newt.event.InputEvent.ALT_MASK;
        if ((awtMods & java.awt.event.InputEvent.ALT_GRAPH_MASK) != 0) newtMods |= com.jogamp.newt.event.InputEvent.ALT_GRAPH_MASK;
        return newtMods;
    }

    public static final int awtButton2Newt(int awtButton) {
        switch (awtButton) {
            case java.awt.event.MouseEvent.BUTTON1: return com.jogamp.newt.event.MouseEvent.BUTTON1;
            case java.awt.event.MouseEvent.BUTTON2: return com.jogamp.newt.event.MouseEvent.BUTTON2;
            case java.awt.event.MouseEvent.BUTTON3: return com.jogamp.newt.event.MouseEvent.BUTTON3;
        }
        return 0;
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
                rotation = ((java.awt.event.MouseWheelEvent)event).getWheelRotation();
            }

            int mods = awtModifiers2Newt(event.getModifiers(), true);
            if(null!=newtSource) {
                if(newtSource.isPointerConfined()) {
                    mods |= InputEvent.CONFINED_MASK;
                }
                if(!newtSource.isPointerVisible()) {
                    mods |= InputEvent.INVISIBLE_MASK;
                }
            }
                    
            return new com.jogamp.newt.event.MouseEvent(
                           type, (null==newtSource)?(Object)event.getComponent():(Object)newtSource, event.getWhen(),
                           mods, event.getX(), event.getY(), event.getClickCount(), 
                           awtButton2Newt(event.getButton()), rotation);
        }
        return null; // no mapping ..
    }

    public static final com.jogamp.newt.event.KeyEvent createKeyEvent(java.awt.event.KeyEvent event, com.jogamp.newt.Window newtSource) {
        int type = eventTypeAWT2NEWT.get(event.getID());
        if(0xFFFFFFFF != type) {
            return new com.jogamp.newt.event.KeyEvent(
                           type, (null==newtSource)?(Object)event.getComponent():(Object)newtSource, event.getWhen(), 
                           awtModifiers2Newt(event.getModifiers(), false), 
                           event.getKeyCode(), event.getKeyChar());
        }
        return null; // no mapping ..
    }

}

