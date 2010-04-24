
/*
 * Copyright (c) 2010 Sven Gothel. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name Sven Gothel or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SVEN GOTHEL HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */
package com.jogamp.newt.awt.event;

import com.jogamp.common.util.IntIntHashMap;

class AWTNewtEventFactory {

    protected static final IntIntHashMap eventTypeAWT2NEWT;

    static {
        IntIntHashMap map = new IntIntHashMap();
        map.setKeyNotFoundValue(-1);
        // n/a map.put(java.awt.event.WindowEvent.WINDOW_OPENED, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_OPENED);
        map.put(java.awt.event.WindowEvent.WINDOW_CLOSING, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_DESTROY_NOTIFY);
        // n/a map.put(java.awt.event.WindowEvent.WINDOW_CLOSED, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_CLOSED);
        // n/a map.put(java.awt.event.WindowEvent.WINDOW_ICONIFIED, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_ICONIFIED);
        // n/a map.put(java.awt.event.WindowEvent.WINDOW_DEICONIFIED, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_DEICONIFIED);
        map.put(java.awt.event.WindowEvent.WINDOW_ACTIVATED, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_GAINED_FOCUS);
        map.put(java.awt.event.WindowEvent.WINDOW_GAINED_FOCUS, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_GAINED_FOCUS);
        map.put(java.awt.event.WindowEvent.WINDOW_DEACTIVATED, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_LOST_FOCUS);
        map.put(java.awt.event.WindowEvent.WINDOW_LOST_FOCUS, com.jogamp.newt.event.WindowEvent.EVENT_WINDOW_LOST_FOCUS);
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

    static final com.jogamp.newt.event.WindowEvent createWindowEvent(java.awt.event.WindowEvent event, com.jogamp.newt.Window newtSource) {
        int type = eventTypeAWT2NEWT.get(event.getID());
        if(-1 < type) {
            return new com.jogamp.newt.event.WindowEvent(type, ((null==newtSource)?(Object)event.getComponent():(Object)newtSource), System.currentTimeMillis());
        }
        return null; // no mapping ..
    }

    static final com.jogamp.newt.event.WindowEvent createWindowEvent(java.awt.event.ComponentEvent event, com.jogamp.newt.Window newtSource) {
        int type = eventTypeAWT2NEWT.get(event.getID());
        if(-1 < type) {
            return new com.jogamp.newt.event.WindowEvent(type, (null==newtSource)?(Object)event.getComponent():(Object)newtSource, System.currentTimeMillis());
        }
        return null; // no mapping ..
    }

    static final com.jogamp.newt.event.MouseEvent createMouseEvent(java.awt.event.MouseEvent event, com.jogamp.newt.Window newtSource) {
        int type = eventTypeAWT2NEWT.get(event.getID());
        if(-1 < type) {
            int rotation = 0;
            if (event instanceof java.awt.event.MouseWheelEvent) {
                rotation = ((java.awt.event.MouseWheelEvent)event).getWheelRotation();
            }

            return new com.jogamp.newt.event.MouseEvent(
                           type, (null==newtSource)?(Object)event.getComponent():(Object)newtSource, event.getWhen(),
                           awtModifiers2Newt(event.getModifiers(), true), 
                           event.getX(), event.getY(), event.getClickCount(), 
                           awtButton2Newt(event.getButton()), rotation);
        }
        return null; // no mapping ..
    }

    static final com.jogamp.newt.event.KeyEvent createKeyEvent(java.awt.event.KeyEvent event, com.jogamp.newt.Window newtSource) {
        int type = eventTypeAWT2NEWT.get(event.getID());
        if(-1 < type) {
            return new com.jogamp.newt.event.KeyEvent(
                           type, (null==newtSource)?(Object)event.getComponent():(Object)newtSource, event.getWhen(), 
                           awtModifiers2Newt(event.getModifiers(), false), 
                           event.getKeyCode(), event.getKeyChar());
        }
        return null; // no mapping ..
    }

}

