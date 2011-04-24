/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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
 * Neither the name of Sun Microsystems, Inc. or the names of
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
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 */

package com.jogamp.newt.event;

public class MouseEvent extends InputEvent
{
  public static final int BUTTON1 = 1;
  public static final int BUTTON2 = 2;
  public static final int BUTTON3 = 3;
  public static final int BUTTON4 = 4;
  public static final int BUTTON5 = 5;
  public static final int BUTTON6 = 6;
  public static final int BUTTON_NUMBER = 6;

  public static final int getClickTimeout() { 
    return 300; 
  }

 public MouseEvent(int eventType, Object source, long when,
                   int modifiers, int x, int y, int clickCount, int button,
                   int rotation)
 {
     super(eventType, source, when, modifiers); 
     this.x=x;
     this.y=y;
     this.clickCount=clickCount;
     this.button=button;
     this.wheelRotation = rotation;
 }

 public int getButton() {
    return button;
 }
 public int getClickCount() {
    return clickCount;
 }
 public int getX() {
    return x;
 }
 public int getY() {
    return y;
 }
 public int getWheelRotation() {
    return wheelRotation;
 }
 
 public String toString() {
    return "MouseEvent["+getEventTypeString(getEventType())+
                       ", "+x+"/"+y+", button "+button+", count "+clickCount+
                       ", wheel rotation "+wheelRotation+
                       ", "+super.toString()+"]";
 }

 public static String getEventTypeString(int type) {
    switch(type) {
        case EVENT_MOUSE_CLICKED: return "EVENT_MOUSE_CLICKED";
        case EVENT_MOUSE_ENTERED: return "EVENT_MOUSE_ENTERED";
        case EVENT_MOUSE_EXITED: return "EVENT_MOUSE_EXITED";
        case EVENT_MOUSE_PRESSED: return "EVENT_MOUSE_PRESSED";
        case EVENT_MOUSE_RELEASED: return "EVENT_MOUSE_RELEASED";
        case EVENT_MOUSE_MOVED: return "EVENT_MOUSE_MOVED";
        case EVENT_MOUSE_DRAGGED: return "EVENT_MOUSE_DRAGGED";
        case EVENT_MOUSE_WHEEL_MOVED: return "EVENT_MOUSE_WHEEL_MOVED";
        default: return "unknown (" + type + ")";
    }
 }

 private final int x, y, clickCount, button, wheelRotation;

 public static final int EVENT_MOUSE_CLICKED  = 200;
 public static final int EVENT_MOUSE_ENTERED  = 201;
 public static final int EVENT_MOUSE_EXITED   = 202;
 public static final int EVENT_MOUSE_PRESSED  = 203;
 public static final int EVENT_MOUSE_RELEASED = 204;
 public static final int EVENT_MOUSE_MOVED    = 205;
 public static final int EVENT_MOUSE_DRAGGED  = 206;
 public static final int EVENT_MOUSE_WHEEL_MOVED = 207;
}
