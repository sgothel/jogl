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
public abstract class InputEvent extends NEWTEvent
{
 public static final int  SHIFT_MASK     = 1 <<  0;
 public static final int  CTRL_MASK      = 1 <<  1;
 public static final int  META_MASK      = 1 <<  2;
 public static final int  ALT_MASK       = 1 <<  3;
 public static final int  ALT_GRAPH_MASK = 1 <<  5;
 public static final int  BUTTON1_MASK   = 1 <<  6;
 public static final int  BUTTON2_MASK   = 1 <<  7;
 public static final int  BUTTON3_MASK   = 1 <<  8;
 public static final int  BUTTON4_MASK   = 1 <<  9;
 public static final int  BUTTON5_MASK   = 1 << 10;
 public static final int  BUTTON6_MASK   = 1 << 11;
 public static final int  CONFINED_MASK  = 1 << 16;
 public static final int  INVISIBLE_MASK = 1 << 17;

 /** 
  * Returns the corresponding button mask for the given button.
  * <p>
  * In case the given button lies outside 
  * of the valid range [{@link MouseEvent#BUTTON1} .. {@link MouseEvent#BUTTON6}],
  * null is returned.
  * </p>
  */
 public static final int getButtonMask(int button)  {
     if( 0 < button && button <= MouseEvent.BUTTON_NUMBER ) {
         return 1 << ( 5 + button ) ;
     }
     return 0;
 }
 
 /** Object when attached via {@link #setAttachment(Object)} marks the event consumed,
  * ie. stops propagating the event any further to the event listener. 
  */
 public static final Object consumedTag = new Object();
 
 protected InputEvent(int eventType, Object source, long when, int modifiers) {
    super(eventType, source, when);
    this.modifiers=modifiers;
 }

 public int getModifiers() {
    return modifiers;
 }
 public boolean isAltDown() {
    return (modifiers&ALT_MASK)!=0;
 }
 public boolean isAltGraphDown() {
    return (modifiers&ALT_GRAPH_MASK)!=0;
 }
 public boolean isControlDown() {
    return (modifiers&CTRL_MASK)!=0;
 }
 public boolean isMetaDown() {
    return (modifiers&META_MASK)!=0;
 }
 public boolean isShiftDown()  {
    return (modifiers&SHIFT_MASK)!=0;
 }
 public boolean isConfined()  {
    return (modifiers&CONFINED_MASK)!=0;
 }
 public boolean isInvisible()  {
    return (modifiers&INVISIBLE_MASK)!=0;
 }

 /**
  * @return Array of pressed mouse buttons  [{@link MouseEvent#BUTTON1} .. {@link MouseEvent#BUTTON6}]. 
  *         If none is down, the resulting array is of length 0.
  */
 public final int[] getButtonsDown()  {
     int len = 0;
     for(int i=1; i<=MouseEvent.BUTTON_NUMBER; i++) {
         if(isButtonDown(i)) { len++; }
     }
     
     int[] res = new int[len];
     int j = 0;
     for(int i=1; i<=MouseEvent.BUTTON_NUMBER; i++) {
         if(isButtonDown(i)) { res[j++] = ( MouseEvent.BUTTON1 - 1 ) + i; }
     }
     return res;
 }

 public final boolean isButtonDown(int button)  {
    return ( modifiers & getButtonMask(button) ) != 0;
 }
 
 public String toString() {
     return "InputEvent[modifiers: 0x"+Integer.toHexString(modifiers)+", "+super.toString()+"]";
 }

 private final int modifiers;
}
