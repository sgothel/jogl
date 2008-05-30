/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.javafx.newt;

public class MouseEvent extends InputEvent
{
  public static final int BUTTON1 = 1;
  public static final int BUTTON2 = 2;
  public static final int BUTTON3 = 3;

 protected MouseEvent(boolean sysEvent, int eventType, Window source, long when, int modifiers, int x, int y, int clickCount, int button) 
 {
     super(sysEvent, source, when, modifiers); 
     this.eventType=eventType;
     this.x=x;
     this.y=y;
     this.clickCount=clickCount;
     this.button=button;
 }
 public MouseEvent(int eventType, Window source, long when, int modifiers, int x, int y, int clickCount, int button) {
     this(false, eventType, source, when, modifiers, x, y, clickCount, button); 
 }

 public int getEventType() {
    return eventType;
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
 
 public String toString() {
    return "MouseEvent["+x+"/"+y+", button "+button+", count "+clickCount+", "+super.toString();
 }

 private int eventType, x, y, clickCount, button;

 public static final int EVENT_MOUSE_CLICKED  = 1 << 0;
 public static final int EVENT_MOUSE_ENTERED  = 1 << 1;
 public static final int EVENT_MOUSE_EXITED   = 1 << 2;
 public static final int EVENT_MOUSE_PRESSED  = 1 << 3;
 public static final int EVENT_MOUSE_RELEASED = 1 << 4;
 public static final int EVENT_MOUSE_MOVED    = 1 << 5;
 public static final int EVENT_MOUSE_DRAGGED  = 1 << 6;

}

