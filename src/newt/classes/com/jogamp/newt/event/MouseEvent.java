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

import java.util.Arrays;

/**
 * Pointer event of type {@link PointerType}.
 * <p>
 * The historical misleading class name may change in the future to <code>PointerEvent</code>.
 * </p>
 * <p>
 * http://www.w3.org/Submission/pointer-events/#pointerevent-interface
 * </p>
 * <a name="coordUnit"><h5>Unit of Coordinates</h5></a>
 * <p>
 * All pointer coordinates of this interface are represented in pixel units,
 * see {@link NativeSurface} and {@link NativeWindow}.
 * </p>
 * <a name="multiPtrEvent"><h5>Multiple-Pointer Events</h5></a>
 * <p>
 * In case an instance represents a multiple-pointer event, i.e. {@link #getPointerCount()} is &gt; 1,
 * the first data element of the multiple-pointer fields represents the pointer triggering this event.<br/>
 * For example {@link #getX(int) e.getX(0)} at {@link #EVENT_MOUSE_PRESSED} returns the data of the pressed pointer, etc.
 * </p>
 * <p>
 * Multiple-pointer event's {@link #getButton() button number} is mapped to the <i>first {@link #getPointerId(int) pointer ID}</i>
 * triggering the event and the {@link InputEvent#BUTTON1_MASK button mask bits} in the {@link #getModifiers() modifiers}
 * field  represent the pressed pointer IDs.
 * </p>
 * <p>
 * Users can query the pressed button and pointer count via {@link InputEvent#getButtonDownCount()}
 * or use the simple query {@link InputEvent#isAnyButtonDown()}.
 * </p>
 * <p>
 * If representing a single-pointer {@link PointerType#Mouse} event, {@link #getPointerId(int) pointer-ID} is <code>0</code>
 * and a {@link #getButton() button value} of <code>0</code> denotes no button activity, i.e. {@link PointerType#Mouse} move.
 * </p>
 */
@SuppressWarnings("serial")
public class MouseEvent extends InputEvent
{
    /** Class of pointer types */
    public static enum PointerClass implements InputEvent.InputClass {
        Offscreen, Onscreen, Undefined;
    }

    /** Type of pointer devices */
    public static enum PointerType implements InputEvent.InputType {
        /** {@link PointerClass#Offscreen} mouse. Ordinal 0. */
        Mouse(PointerClass.Offscreen),
        /** {@link PointerClass#Offscreen} touch pad, usually using fingers. Ordinal 1. */
        TouchPad(PointerClass.Offscreen),
        /** {@link PointerClass#Onscreen} touch screen, usually using fingers. Ordinal 2. */
        TouchScreen(PointerClass.Onscreen),
        /** {@link PointerClass#Onscreen} pen usually on screen? Ordinal 3. FIXME*/
        Pen(PointerClass.Onscreen),
        /** {@link PointerClass#Undefined} ?. Ordinal 4. */
        Undefined(PointerClass.Undefined);

        public PointerClass getPointerClass() { return pc; }

        /**
         * Returns the matching PointerType value corresponding to the given PointerType's integer ordinal.
         * <pre>
         *   given:
         *     ordinal = enumValue.ordinal()
         *   reverse:
         *     enumValue = EnumClass.values()[ordinal]
         * </pre>
         * @throws IllegalArgumentException if the given ordinal is out of range, i.e. not within [ 0 .. PointerType.values().length-1 ]
         */
        public static PointerType valueOf(final int ordinal) throws IllegalArgumentException {
            final PointerType[] all = PointerType.values();
            if( 0 <= ordinal && ordinal < all.length ) {
                return all[ordinal];
            }
            throw new IllegalArgumentException("Ordinal "+ordinal+" out of range of PointerType.values()[0.."+(all.length-1)+"]");
        }

        /**
         * Returns the PointerType array of matching PointerType values corresponding to the given PointerType's integer ordinal values.
         * <p>
         * See {@link #valueOf(int)}.
         * </p>
         * @throws IllegalArgumentException if one of the given ordinal values is out of range, i.e. not within [ 0 .. PointerType.values().length-1 ]
         */
        public static PointerType[] valuesOf(final int[] ordinals) throws IllegalArgumentException {
            final int count = ordinals.length;
            final PointerType[] types = new PointerType[count];
            for(int i=count-1; i>=0; i--) {
                types[i] = PointerType.valueOf(ordinals[i]);
            }
            return types;
        }

        private PointerType(final PointerClass pc) {
            this.pc = pc;
        }
        PointerClass pc;
    }

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
    public static final short BUTTON_COUNT =  16;

    /** Returns the 3-axis XYZ rotation array by given rotation on Y axis or X axis (if SHIFT_MASK is given in mods). */
    public static final float[] getRotationXYZ(final float rotationXorY, final int mods) {
        final float[] rotationXYZ = new float[] { 0f, 0f, 0f };
        if( 0 != ( mods & InputEvent.SHIFT_MASK ) ) {
            rotationXYZ[0] = rotationXorY;
        } else {
            rotationXYZ[1] = rotationXorY;
        }
        return rotationXYZ;
    }

    public static final short getClickTimeout() {
        return 300;
    }

    /**
     * Constructor for traditional one-pointer event.
     *
     * @param eventType
     * @param source
     * @param when
     * @param modifiers
     * @param x X-axis
     * @param y Y-axis
     * @param clickCount Mouse-button click-count
     * @param button button number, e.g. [{@link #BUTTON1}..{@link #BUTTON_COUNT}-1].
     *               A button value of <code>0</code> denotes no button activity, i.e. {@link PointerType#Mouse} move.
     * @param rotationXYZ Rotation of all axis
     * @param rotationScale Rotation scale
     */
    public MouseEvent(final short eventType, final Object source, final long when,
            final int modifiers, final int x, final int y, final short clickCount, final short button,
            final float[] rotationXYZ, final float rotationScale)
    {
        super(eventType, source, when, modifiers);
        this.x = new int[]{x};
        this.y = new int[]{y};
        switch(eventType) {
            case EVENT_MOUSE_CLICKED:
            case EVENT_MOUSE_PRESSED:
            case EVENT_MOUSE_DRAGGED:
                this.pressure = constMousePressure1;
                break;
            default:
                this.pressure = constMousePressure0;
        }
        this.maxPressure= 1.0f;
        this.pointerID = new short[] { (short)0 };
        this.clickCount=clickCount;
        this.button=button;
        this.rotationXYZ = rotationXYZ;
        this.rotationScale = rotationScale;
        this.pointerType = constMousePointerTypes;
    }

    /**
     * Constructor for a multiple-pointer event.
     * <p>
     * First element of multiple-pointer arrays represents the pointer which triggered the event!
     * </p>
     * <p>
     * See details for <a href="#multiPtrEvent">multiple-pointer events</a>.
     * </p>
     *
     * @param eventType
     * @param source
     * @param when
     * @param modifiers
     * @param pointerType PointerType for each pointer (multiple pointer)
     * @param pointerID Pointer ID for each pointer (multiple pointer). IDs start w/ 0 and are consecutive numbers.
     *                  A pointer-ID of -1 may also denote no pointer/button activity, i.e. {@link PointerType#Mouse} move.
     * @param x X-axis for each pointer (multiple pointer)
     * @param y Y-axis for each pointer (multiple pointer)
     * @param pressure Pressure for each pointer (multiple pointer)
     * @param maxPressure Maximum pointer pressure for all pointer
     * @param button Corresponding mouse-button
     * @param clickCount Mouse-button click-count
     * @param rotationXYZ Rotation of all axis
     * @param rotationScale Rotation scale
     */
    public MouseEvent(final short eventType, final Object source, final long when, final int modifiers,
                      final PointerType pointerType[], final short[] pointerID,
                      final int[] x, final int[] y, final float[] pressure, final float maxPressure,
                      final short button, final short clickCount, final float[] rotationXYZ, final float rotationScale)
    {
        super(eventType, source, when, modifiers);
        this.x = x;
        this.y = y;
        final int pointerCount = pointerType.length;
        if(pointerCount != pointerID.length ||
           pointerCount != x.length ||
           pointerCount != y.length ||
           pointerCount != pressure.length) {
            throw new IllegalArgumentException("All multiple pointer arrays must be of same size");
        }
        if( 0.0f >= maxPressure ) {
            throw new IllegalArgumentException("maxPressure must be > 0.0f");
        }
        this.pressure = pressure;
        this.maxPressure= maxPressure;
        this.pointerID = pointerID;
        this.clickCount=clickCount;
        this.button=button;
        this.rotationXYZ = rotationXYZ;
        this.rotationScale = rotationScale;
        this.pointerType = pointerType;
    }

    public final MouseEvent createVariant(final short newEventType) {
        return new MouseEvent(newEventType, source, getWhen(), getModifiers(), pointerType, pointerID,
                              x, y, pressure, maxPressure, button, clickCount, rotationXYZ, rotationScale);
    }

    /**
     * See details for <a href="#multiPtrEvent">multiple-pointer events</a>.
     * @return the count of pointers involved in this event
     */
    public final int getPointerCount() {
        return pointerType.length;
    }

    /**
     * See details for <a href="#multiPtrEvent">multiple-pointer events</a>.
     * @return the {@link PointerType} for the data at index or null if index not available.
     */
    public final PointerType getPointerType(final int index) {
        if(0 > index || index >= pointerType.length) {
            return null;
        }
        return pointerType[index];
    }

    /**
     * See details for <a href="#multiPtrEvent">multiple-pointer events</a>.
     * @return array of all {@link PointerType}s for all pointers
     */
    public final PointerType[] getAllPointerTypes() {
        return pointerType;
    }

    /**
     * Return the pointer id for the given index or -1 if index not available.
     * <p>
     * IDs start w/ 0 and are consecutive numbers.
     * </p>
     * <p>
     * See details for <a href="#multiPtrEvent">multiple-pointer events</a>.
     * </p>
     */
    public final short getPointerId(final int index) {
        if(0 > index || index >= pointerID.length) {
            return -1;
        }
        return pointerID[index];
    }

    /**
     * See details for <a href="#multiPtrEvent">multiple-pointer events</a>.
     * @return the pointer index for the given pointer id or -1 if id not available.
     */
    public final int getPointerIdx(final short id) {
        if( id >= 0 ) {
            for(int i=pointerID.length-1; i>=0; i--) {
                if( pointerID[i] == id ) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * See details for <a href="#multiPtrEvent">multiple-pointer events</a>.
     * @return array of all pointer IDs for all pointers. IDs start w/ 0 and are consecutive numbers.
     */
    public final short[] getAllPointerIDs() {
        return pointerID;
    }

    /**
     * Returns the button number, e.g. [{@link #BUTTON1}..{@link #BUTTON_COUNT}-1].
     * <p>
     * A button value of <code>0</code> denotes no button activity, i.e. {@link PointerType#Mouse} move.
     * </p>
     * <p>
     * See details for <a href="#multiPtrEvent">multiple-pointer events</a>.
     * </p>
     */
    public final short getButton() {
        return button;
    }

    public final short getClickCount() {
        return clickCount;
    }

    /**
     * See details for <a href="#multiPtrEvent">multiple-pointer events</a>.
     * @return X-Coord of the triggering pointer-index zero in pixel units.
     */
    public final int getX() {
        return x[0];
    }

    /**
     * See details for <a href="#multiPtrEvent">multiple-pointer events</a>.
     * @return Y-Coord of the triggering pointer-index zero in pixel units.
     */
    public final int getY() {
        return y[0];
    }

    /**
     * See details for <a href="#multiPtrEvent">multiple-pointer events</a>.
     * @param index pointer-index within [0 .. {@link #getPointerCount()}-1]
     * @return X-Coord associated with the pointer-index in pixel units.
     * @see getPointerId(index)
     */
    public final int getX(final int index) {
        return x[index];
    }

    /**
     * See details for <a href="#multiPtrEvent">multiple-pointer events</a>.
     * @param index pointer-index within [0 .. {@link #getPointerCount()}-1]
     * @return Y-Coord associated with the pointer-index in pixel units.
     * @see getPointerId(index)
     */
    public final int getY(final int index) {
        return y[index];
    }

    /**
     * See details for <a href="#multiPtrEvent">multiple-pointer events</a>.
     * @return array of all X-Coords for all pointers in pixel units.
     */
    public final int[] getAllX() {
        return x;
    }

    /**
     * See details for <a href="#multiPtrEvent">multiple-pointer events</a>.
     * @return array of all Y-Coords for all pointers in pixel units.
     */
    public final int[] getAllY() {
        return y;
    }

    /**
     * @param normalized if true, method returns the normalized pressure, i.e. <code>pressure / maxPressure</code>
     * @return The pressure associated with the pointer-index 0.
     *         The value of zero is return if not available.
     * @see #getMaxPressure()
     */
    public final float getPressure(final boolean normalized){
        return normalized ? pressure[0] / maxPressure : pressure[0];
    }

    /**
     * See details for <a href="#multiPtrEvent">multiple-pointer events</a>.
     * @param index pointer-index within [0 .. {@link #getPointerCount()}-1]
     * @param normalized if true, method returns the normalized pressure, i.e. <code>pressure / maxPressure</code>
     * @return The pressure associated with the pointer-index.
     *         The value of zero is return if not available.
     * @see #getMaxPressure()
     */
    public final float getPressure(final int index, final boolean normalized){
        return normalized ? pressure[index] / maxPressure : pressure[index];
    }

    /**
     * See details for <a href="#multiPtrEvent">multiple-pointer events</a>.
     * @return array of all raw, un-normalized pressures for all pointers
     */
    public final float[] getAllPressures() {
        return pressure;
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
    public final float getMaxPressure() {
        return maxPressure;
    }

    /**
     * Returns a 3-component float array filled with the values of the rotational axis
     * in the following order: horizontal-, vertical- and z-axis.
     * <p>
     * A vertical rotation of <b>&gt; 0.0f is up</b> and <b>&lt; 0.0f is down</b>.
     * </p>
     * <p>
     * A horizontal rotation of <b>&gt; 0.0f is left</b> and <b>&lt; 0.0f is right</b>.
     * </p>
     * <p>
     * A z-axis rotation of <b>&gt; 0.0f is back</b> and <b>&lt; 0.0f is front</b>.
     * </p>
     * <p>
     * <i>However</i>, on some OS this might be flipped due to the OS <i>default</i> behavior.
     * The latter is true for OS X 10.7 (Lion) for example.
     * </p>
     * <p>
     * On PointerClass {@link PointerClass#Onscreen onscreen} devices, i.e. {@link PointerType#TouchScreen touch screens},
     * rotation events are usually produced by a 2-finger movement, where horizontal and vertical rotation values are filled.
     * </p>
     * <p>
     * On PointerClass {@link PointerClass#Offscreen offscreen} devices, i.e. {@link PointerType#Mouse mouse},
     * either the horizontal or the vertical rotation value is filled.
     * </p>
     * <p>
     * The {@link InputEvent#SHIFT_MASK} modifier is set in case <b>|horizontal| &gt; |vertical|</b> value.<br/>
     * This can be utilized to implement only one 2d rotation direction, you may use {@link #isShiftDown()} to query it.
     * </p>
     * <p>
     * In case the pointer type is {@link PointerType#Mouse mouse},
     * events are usually send in steps of one, ie. <i>-1.0f</i> and <i>1.0f</i>.
     * Higher values may result due to fast scrolling.
     * Fractional values may result due to slow scrolling with high resolution devices.<br/>
     * Here the button number refers to the wheel number.
     * </p>
     * <p>
     * In case the pointer type is of class {@link PointerClass#Onscreen}, e.g. {@link PointerType#TouchScreen touch screen},
     * see {@link #getRotationScale()} for semantics.
     * </p>
     */
    public final float[] getRotation() {
        return rotationXYZ;
    }

    /**
     * Returns the scale used to determine the {@link #getRotation() rotation value},
     * which semantics depends on the {@link #getPointerType() pointer type's} {@link PointerClass}.
     * <p>
     * For {@link PointerClass#Offscreen}, the scale is usually <code>1.0f</code> and denominates
     * an abstract value without association to a physical value.
     * </p>
     * <p>
     * For {@link PointerClass#Onscreen}, the scale varies and denominates
     * the divisor of the distance the finger[s] have moved on the screen.
     * Hence <code>scale * rotation</code> reproduces the screen distance in pixels the finger[s] have moved.
     * </p>
     */
    public final float getRotationScale() {
        return rotationScale;
    }

    @Override
    public final String toString() {
        return toString(null).toString();
    }

    @Override
    public final StringBuilder toString(StringBuilder sb) {
        if(null == sb) {
            sb = new StringBuilder();
        }
        sb.append("MouseEvent[").append(getEventTypeString(getEventType()))
        .append(", ").append(Arrays.toString(x)).append("/").append(Arrays.toString(y))
        .append(", button ").append(button).append(", count ")
        .append(clickCount).append(", rotation [").append(rotationXYZ[0]).append(", ").append(rotationXYZ[1]).append(", ").append(rotationXYZ[2]).append("] * ").append(rotationScale);
        if(pointerID.length>0) {
            sb.append(", pointer<").append(pointerID.length).append(">[");
            for(int i=0; i<pointerID.length; i++) {
                if(i>0) {
                    sb.append(", ");
                }
                sb.append(pointerID[i]).append("/").append(pointerType[i]).append(": ")
                .append(x[i]).append("/").append(y[i]).append(", ")
                .append("p[").append(pressure[i]).append("/").append(maxPressure).append("=").append(pressure[i]/maxPressure).append("]");
            }
            sb.append("]");
        }
        sb.append(", ");
        return super.toString(sb).append("]");
    }

    public static String getEventTypeString(final short type) {
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

    /** PointerType for each pointer (multiple pointer) */
    private final PointerType pointerType[];
    /** Pointer-ID for each pointer (multiple pointer). IDs start w/ 0 and are consecutive numbers. */
    private final short pointerID[];
    /** X-axis for each pointer (multiple pointer) */
    private final int x[];
    /** Y-axis for each pointer (multiple pointer) */
    private final int y[];
    /** Pressure for each pointer (multiple pointer) */
    private final float pressure[];
    // private final short tiltX[], tiltY[]; // TODO: A generic way for pointer axis information, see Android MotionEvent!
    private final short clickCount;
    /**
     * Returns the button number, e.g. [{@link #BUTTON1}..{@link #BUTTON_COUNT}-1].
     * <p>
     * A button value of <code>0</code> denotes no button activity, i.e. {@link PointerType#Mouse} move.
     * </p>
     */
    private final short button;
    /** Rotation around the X, Y and X axis */
    private final float[] rotationXYZ;
    /** Rotation scale */
    private final float rotationScale;
    private final float maxPressure;

    private static final float[] constMousePressure0 = new float[]{0f};
    private static final float[] constMousePressure1 = new float[]{1f};
    private static final PointerType[] constMousePointerTypes = new PointerType[] { PointerType.Mouse };

    public static final short EVENT_MOUSE_CLICKED  = 200;
    /** Only generated for {@link PointerType#Mouse} */
    public static final short EVENT_MOUSE_ENTERED  = 201;
    /** Only generated for {@link PointerType#Mouse} */
    public static final short EVENT_MOUSE_EXITED   = 202;
    public static final short EVENT_MOUSE_PRESSED  = 203;
    public static final short EVENT_MOUSE_RELEASED = 204;
    public static final short EVENT_MOUSE_MOVED    = 205;
    public static final short EVENT_MOUSE_DRAGGED  = 206;
    public static final short EVENT_MOUSE_WHEEL_MOVED = 207;
}
