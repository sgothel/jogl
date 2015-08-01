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

import com.jogamp.common.util.Bitfield;
import com.jogamp.newt.Window;

@SuppressWarnings("serial")
public abstract class InputEvent extends NEWTEvent
{
 /** Interface marking class of input types */
 public static interface InputClass {
 }

 /** Interface marking type of input devices */
 public static interface InputType {
 }

 public static final int  SHIFT_MASK       = 1 <<  0;
 public static final int  CTRL_MASK        = 1 <<  1;
 public static final int  META_MASK        = 1 <<  2;
 public static final int  ALT_MASK         = 1 <<  3;
 public static final int  ALT_GRAPH_MASK   = 1 <<  4;

 public static final int  BUTTON1_MASK     = 1 <<  5;
 public static final int  BUTTON2_MASK     = 1 <<  6;
 public static final int  BUTTON3_MASK     = 1 <<  7;
 public static final int  BUTTON4_MASK     = 1 <<  8;
 public static final int  BUTTON5_MASK     = 1 <<  9;
 public static final int  BUTTON6_MASK     = 1 << 10;
 public static final int  BUTTON7_MASK     = 1 << 11;
 public static final int  BUTTON8_MASK     = 1 << 12;
 public static final int  BUTTON9_MASK     = 1 << 13;

 public static final int  BUTTONLAST_MASK  = 1 << 20;  // 16 buttons
 public static final int  BUTTONALL_MASK   = 0xffff << 5 ;  // 16 buttons

 /** Event is caused by auto-repeat. */
 public static final int  AUTOREPEAT_MASK  = 1 << 29;

 /** Pointer is confined, see {@link Window#confinePointer(boolean)}. */
 public static final int  CONFINED_MASK    = 1 << 30;

 /** Pointer is invisible, see {@link Window#setPointerVisible(boolean)}. */
 public static final int  INVISIBLE_MASK   = 1 << 31;

 /**
  * Returns the corresponding button mask for the given button.
  * <p>
  * In case the given button lies outside
  * of the valid range [{@link MouseEvent#BUTTON1} .. {@link MouseEvent#BUTTON_COUNT}],
  * null is returned.
  * </p>
  */
 public static final int getButtonMask(final int button)  {
     if( 0 < button && button <= MouseEvent.BUTTON_COUNT ) {
         return 1 << ( 4 + button ) ;
     }
     return 0;
 }

 protected InputEvent(final short eventType, final Object source, final long when, final int modifiers) {
    super(eventType, source, when);
    this.modifiers=modifiers;
 }

 /** Return the modifier bits of this event, e.g. see {@link #SHIFT_MASK} .. etc. */
 public final int getModifiers() {
    return modifiers;
 }
 /** {@link #getModifiers()} contains {@link #ALT_MASK}. */
 public final boolean isAltDown() {
    return (modifiers&ALT_MASK)!=0;
 }
 /** {@link #getModifiers()} contains {@link #ALT_GRAPH_MASK}. */
 public final boolean isAltGraphDown() {
    return (modifiers&ALT_GRAPH_MASK)!=0;
 }
 /** {@link #getModifiers()} contains {@link #CTRL_MASK}. */
 public final boolean isControlDown() {
    return (modifiers&CTRL_MASK)!=0;
 }
 /** {@link #getModifiers()} contains {@link #META_MASK}. */
 public final boolean isMetaDown() {
    return (modifiers&META_MASK)!=0;
 }
 /** {@link #getModifiers()} contains {@link #SHIFT_MASK}. */
 public final boolean isShiftDown()  {
    return (modifiers&SHIFT_MASK)!=0;
 }
 /** {@link #getModifiers()} contains {@link #AUTOREPEAT_MASK}. */
 public final boolean isAutoRepeat()  {
    return (modifiers&AUTOREPEAT_MASK)!=0;
 }
 /** {@link #getModifiers()} contains {@link #CONFINED_MASK}. Pointer is confined, see {@link Window#confinePointer(boolean)}. */
 public final boolean isConfined()  {
    return (modifiers&CONFINED_MASK)!=0;
 }
 /** {@link #getModifiers()} contains {@link #INVISIBLE_MASK}. Pointer is invisible, see {@link Window#setPointerVisible(boolean)}. */
 public final boolean isInvisible()  {
    return (modifiers&INVISIBLE_MASK)!=0;
 }

 public final StringBuilder getModifiersString(StringBuilder sb) {
    if(null == sb) {
        sb = new StringBuilder();
    }
    sb.append("[");
    boolean isFirst = true;
    if(isShiftDown()) { if(!isFirst) { sb.append(", "); } isFirst = false; sb.append("shift"); }
    if(isControlDown()) { if(!isFirst) { sb.append(", "); } isFirst = false; sb.append("ctrl"); }
    if(isMetaDown()) { if(!isFirst) { sb.append(", "); } isFirst = false; sb.append("meta"); }
    if(isAltDown()) { if(!isFirst) { sb.append(", "); } isFirst = false; sb.append("alt"); }
    if(isAltGraphDown()) { if(!isFirst) { sb.append(", "); } isFirst = false; sb.append("altgr"); }
    if(isAutoRepeat()) { if(!isFirst) { sb.append(", "); } isFirst = false; sb.append("repeat"); }
    for(int i=1; i<=MouseEvent.BUTTON_COUNT; i++) {
        if(isButtonDown(i)) { if(!isFirst) { sb.append(", "); } isFirst = false; sb.append("button").append(i); }
    }
    if(isConfined()) { if(!isFirst) { sb.append(", "); } isFirst = false; sb.append("confined"); }
    if(isInvisible()) { if(!isFirst) { sb.append(", "); } isFirst = false; sb.append("invisible"); }
    sb.append("]");

    return sb;
 }

 /**
  * See also {@link MouseEvent}'s section about <i>Multiple-Pointer Events</i>.
  * @return Array of pressed mouse buttons  [{@link MouseEvent#BUTTON1} .. {@link MouseEvent#BUTTON6}].
  *         If none is down, the resulting array is of length 0.
  */
 public final short[] getButtonsDown()  {
     final int len = getButtonDownCount();

     final short[] res = new short[len];
     int j = 0;
     for(int i=1; i<=MouseEvent.BUTTON_COUNT; i++) {
         if( isButtonDown(i) ) { res[j++] = (short) ( ( MouseEvent.BUTTON1 - 1 ) + i ); }
     }
     return res;
 }

 /**
  * See also {@link MouseEvent}'s section about <i>Multiple-Pointer Events</i>.
  * @param button the button to test
  * @return true if the given button is down
  */
 public final boolean isButtonDown(final int button)  {
    return ( modifiers & getButtonMask(button) ) != 0;
 }

 /**
  * Returns the number of pressed buttons by counting the set bits:
  * <pre>
  *     getBitCount(modifiers & BUTTONALL_MASK);
  * </pre>
  * <p>
  * See also {@link MouseEvent}'s section about <i>Multiple-Pointer Events</i>.
  * </p>
  * @see Bitfield.Util#bitCount(int)
  * @see #BUTTONALL_MASK
  */
 public final int getButtonDownCount() {
     return Bitfield.Util.bitCount(modifiers & BUTTONALL_MASK);
 }

 /**
  * Returns true if at least one button is pressed, otherwise false:
  * <pre>
  *     0 != ( modifiers & BUTTONALL_MASK )
  * </pre>
  * <p>
  * See also {@link MouseEvent}'s section about <i>Multiple-Pointer Events</i>.
  * </p>
  * @see Bitfield.Util#bitCount(int)
  * @see #BUTTONALL_MASK
  */
 public final boolean isAnyButtonDown() {
     return 0 != ( modifiers & BUTTONALL_MASK );
 }

 @Override
 public String toString() {
     return toString(null).toString();
 }

 @Override
 public StringBuilder toString(StringBuilder sb) {
     if(null == sb) {
         sb = new StringBuilder();
     }
     sb.append("InputEvent[modifiers: ");
     getModifiersString(sb);
     sb.append(", ");
     super.toString(sb).append("]");
     return sb;
 }

 private final int modifiers;
}
