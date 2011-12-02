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

@SuppressWarnings("serial")
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
        this.x = new int[]{x};
        this.y = new int[]{y};
        this.pressure = new float[]{0};
        this.pointerids = new int[]{-1};
        this.clickCount=clickCount;
        this.button=button;
        this.wheelRotation = rotation;
    }

    public MouseEvent(int eventType, Object source, long when,
            int modifiers, int[] x, int[] y, float[] pressure, int[] pointerids, int clickCount, int button,
            int rotation)
    {
        super(eventType, source, when, modifiers); 
        this.x = x;
        this.y = y;
        if(pointerids.length != pressure.length ||
           pointerids.length != x.length ||
           pointerids.length != y.length) {
            throw new IllegalArgumentException("All multiple pointer arrays must be of same size");
        }
        this.pressure = pressure;
        this.pointerids = pointerids;
        this.clickCount=clickCount;
        this.button=button;
        this.wheelRotation = rotation;
    }
    
    /**
     * @return the count of pointers involved in this event
     */
    public int getPointerCount() {
        return x.length;
    }
    
    /**
     * @return the pointer id for the data at index.
     *  return -1 if index not available.
     */
    public int getPointerId(int index) {
        if(index >= pointerids.length)
            return -1;
        return pointerids[index];
    }
    
    public int getButton() {
        return button;
    }
    
    public int getClickCount() {
        return clickCount;
    }
    public int getX() {
        return x[0];
    }
    
    public int getY() {
        return y[0];
    }

    /** 
     * @return x-coord at index where index refers to the 
     * data coming from a pointer. 
     * @see getPointerId(index)
     */
    public int getX(int index) {
        return x[index];
    }

    public int getY(int index) {
        return y[index];
    }
    
    public float getPressure(){
        return pressure[0];
    }
    
    /**
     * @return the pressure associated with the pointer at index.
     * the value of zero is return if not available.
     */
    public float getPressure(int index){
        return pressure[index];
    }
    
    /**
     * <i>Usually</i> a wheel rotation of <b>&gt; 0 is up</b>,
     * and  <b>&lt; 0 is down</b>.<br>
     * <i>However</i>, on some OS this might be flipped due to the OS <i>default</i> behavior.
     * The latter is true for OS X 10.7 (Lion) for example.
     * <p>
     * The events will be send usually in steps of one, ie. <i>-1</i> and <i>1</i>.
     * Higher values may result due to fast scrolling.
     * </p>
     * <p>
     * The button number refers to the wheel number.
     * </p> 
     * @return
     */
    public int getWheelRotation() {
        return wheelRotation;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("MouseEvent[").append(getEventTypeString(getEventType()))
        .append(", ").append(x).append("/").append(y)
        .append(", button ").append(button).append(", count ")
        .append(clickCount).append(", wheel rotation ").append(wheelRotation);
        if(pointerids.length>0) {
            sb.append(", pointer<").append(pointerids.length).append(">[");
            for(int i=0; i<pointerids.length; i++) {
                if(i>0) {
                    sb.append(", ");
                }
                sb.append(pointerids[i]).append(": ")
                .append(x[i]).append(" / ").append(y[i]).append(" ")
                .append(pressure[i]).append("p");
            }
            sb.append("]");
        }        
        sb.append(", ").append(super.toString()).append("]");
        return sb.toString();
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
    private final int x[], y[], clickCount, button, wheelRotation;
    private final float pressure[];
    private final int pointerids[];
    
    public static final int EVENT_MOUSE_CLICKED  = 200;
    public static final int EVENT_MOUSE_ENTERED  = 201;
    public static final int EVENT_MOUSE_EXITED   = 202;
    public static final int EVENT_MOUSE_PRESSED  = 203;
    public static final int EVENT_MOUSE_RELEASED = 204;
    public static final int EVENT_MOUSE_MOVED    = 205;
    public static final int EVENT_MOUSE_DRAGGED  = 206;
    public static final int EVENT_MOUSE_WHEEL_MOVED = 207;
}
