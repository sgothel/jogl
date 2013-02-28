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
    /** ID for button 1, value <code>1</code> */
    public static final short BUTTON1 = 1;
    /** ID for button 2, value <code>2</code> */
    public static final short BUTTON2 = 2;
    /** ID for button 3, value <code>3</code> */
    public static final short BUTTON3 = 3;
    /** ID for button 4, value <code>4</code> */
    public static final short BUTTON4 = 4;
    /** ID for button 5, value <code>5</code> */
    public static final short BUTTON5 = 5;
    /** ID for button 6, value <code>6</code> */
    public static final short BUTTON6 = 6;
    /** ID for button 6, value <code>7</code> */
    public static final short BUTTON7 = 7;
    /** ID for button 6, value <code>8</code> */
    public static final short BUTTON8 = 8;
    /** ID for button 6, value <code>9</code> */
    public static final short BUTTON9 = 9;
    
    /** Maximum number of buttons, value <code>16</code> */
    public static final short BUTTON_NUMBER =  16;

    public static final short getClickTimeout() { 
        return 300; 
    }

    public MouseEvent(short eventType, Object source, long when,
            int modifiers, int x, int y, short clickCount, short button,
            float rotation)
    {
        super(eventType, source, when, modifiers); 
        this.x = new int[]{x};
        this.y = new int[]{y};
        this.pressure = new float[]{0f};
        this.maxPressure= 1.0f;
        this.pointerids = new short[]{-1};
        this.clickCount=clickCount;
        this.button=button;
        this.wheelRotation = rotation;
    }

    public MouseEvent(short eventType, Object source, long when,
                      int modifiers, int[] x, int[] y, float[] pressure, float maxPressure, short[] pointerids, short clickCount,
                      short button, float rotation)
    {
        super(eventType, source, when, modifiers); 
        this.x = x;
        this.y = y;
        if(pointerids.length != pressure.length ||
           pointerids.length != x.length ||
           pointerids.length != y.length) {
            throw new IllegalArgumentException("All multiple pointer arrays must be of same size");
        }
        if( 0.0f >= maxPressure ) {
            throw new IllegalArgumentException("maxPressure must be > 0.0f");
        }
        this.pressure = pressure;
        this.maxPressure= maxPressure;
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
    public short getPointerId(int index) {
        if(index >= pointerids.length)
            return -1;
        return pointerids[index];
    }
    
    public short getButton() {
        return button;
    }
    
    public short getClickCount() {
        return clickCount;
    }
    public int getX() {
        return x[0];
    }
    
    public int getY() {
        return y[0];
    }

    /** 
     * @param index pointer-index within [0 .. {@link #getPointerCount()}-1]
     * @return X-Coord associated with the pointer-index.
     * @see getPointerId(index)
     */
    public int getX(int index) {
        return x[index];
    }

    /** 
     * @param index pointer-index within [0 .. {@link #getPointerCount()}-1]
     * @return Y-Coord associated with the pointer-index.
     * @see getPointerId(index)
     */
    public int getY(int index) {
        return y[index];
    }
    
    /**
     * @param normalized if true, method returns the normalized pressure, i.e. <code>pressure / maxPressure</code> 
     * @return The pressure associated with the pointer-index 0.
     *         The value of zero is return if not available.
     * @see #getMaxPressure()
     */
    public float getPressure(boolean normalized){
        return normalized ? pressure[0] / maxPressure : pressure[0];
    }
    
    /** 
     * Returns the maximum pressure known for the input device generating this event.
     * <p>
     * This value may be self calibrating on devices/OS, where no known maximum pressure is known.
     * Hence subsequent events may return a higher value. 
     * </p>
     * <p>
     * Self calibrating maximum pressure is performed on:
     * <ul>
     *   <li>Android</li>
     * </ul>
     * </p>
     */
    public float getMaxPressure() {
        return maxPressure;
    }
    
    /**
     * @param index pointer-index within [0 .. {@link #getPointerCount()}-1]
     * @param normalized if true, method returns the normalized pressure, i.e. <code>pressure / maxPressure</code> 
     * @return The pressure associated with the pointer-index.
     *         The value of zero is return if not available.
     * @see #getMaxPressure()
     */
    public float getPressure(int index, boolean normalized){
        return normalized ? pressure[index] / maxPressure : pressure[index];
    }
    
    /**
     * <i>Usually</i> a wheel rotation of <b>&gt; 0.0f is up</b>,
     * and <b>&lt; 0.0f is down</b>.
     * <p>
     * Usually a wheel rotations is considered a vertical scroll.<br/>
     * If {@link #isShiftDown()}, a wheel rotations is
     * considered a horizontal scroll, where <b>shift-up = left = &gt; 0.0f</b>,
     * and <b>shift-down = right = &lt; 0.0f</b>.   
     * </p>
     * <p>
     * <i>However</i>, on some OS this might be flipped due to the OS <i>default</i> behavior.
     * The latter is true for OS X 10.7 (Lion) for example.
     * </p>
     * <p>
     * The events will be send usually in steps of one, ie. <i>-1.0f</i> and <i>1.0f</i>.
     * Higher values may result due to fast scrolling.
     * Fractional values may result due to slow scrolling with high resolution devices.  
     * </p>
     * <p>
     * The button number refers to the wheel number.
     * </p> 
     * @return
     */
    public float getWheelRotation() {
        return wheelRotation;
    }

    public String toString() {
        return toString(null).toString();
    }

    public StringBuilder toString(StringBuilder sb) {
        if(null == sb) {
            sb = new StringBuilder();
        }
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
                .append(x[i]).append("/").append(y[i]).append(", ")
                .append("p[").append(pressure[i]).append("/").append(maxPressure).append("=").append(pressure[i]/maxPressure).append("]");
            }
            sb.append("]");
        }        
        sb.append(", ");
        return super.toString(sb).append("]");
    }

    public static String getEventTypeString(short type) {
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
    private final int x[], y[];;
    private final short clickCount, button;
    private final float wheelRotation;
    private final float pressure[];
    private final float maxPressure;
    private final short pointerids[];
    
    public static final short EVENT_MOUSE_CLICKED  = 200;
    public static final short EVENT_MOUSE_ENTERED  = 201;
    public static final short EVENT_MOUSE_EXITED   = 202;
    public static final short EVENT_MOUSE_PRESSED  = 203;
    public static final short EVENT_MOUSE_RELEASED = 204;
    public static final short EVENT_MOUSE_MOVED    = 205;
    public static final short EVENT_MOUSE_DRAGGED  = 206;
    public static final short EVENT_MOUSE_WHEEL_MOVED = 207;
}
