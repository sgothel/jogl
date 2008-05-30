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

public class KeyEvent extends InputEvent
{
 protected KeyEvent(boolean sysEvent, int eventType, Window source, long when, int modifiers, int keyCode, char keyChar) {
     super(sysEvent, source, when, modifiers); 
     this.eventType=eventType;
     this.keyCode=keyCode;
     this.keyChar=keyChar;
 }
 public KeyEvent(int eventType, Window source, long when, int modifiers, int keyCode, char keyChar) {
     this(false, eventType, source, when, modifiers, keyCode, keyChar);
 }

 public int getEventType() {
    return eventType;
 }
 public char getKeyChar() {
    return keyChar;
 }
 public int getKeyCode() {
    return keyCode;
 }

 public String toString() {
    return "KeyEvent["+getEventTypeString(eventType)+
                     ", code "+keyCode+", char "+keyChar+", "+super.toString();
 }

 public static String getEventTypeString(int type) {
    switch(type) {
        case EVENT_KEY_PRESSED: return "EVENT_KEY_PRESSED";
        case EVENT_KEY_RELEASED: return "EVENT_KEY_RELEASED";
        case EVENT_KEY_TYPED: return "EVENT_KEY_TYPED";
        default: return "unknown";
    }
 }

 public boolean   isActionKey() {
    switch (keyCode) {
      case VK_HOME:
      case VK_END:
      case VK_PAGE_UP:
      case VK_PAGE_DOWN:
      case VK_UP:
      case VK_DOWN:
      case VK_LEFT:
      case VK_RIGHT:

      case VK_F1:
      case VK_F2:
      case VK_F3:
      case VK_F4:
      case VK_F5:
      case VK_F6:
      case VK_F7:
      case VK_F8:
      case VK_F9:
      case VK_F10:
      case VK_F11:
      case VK_F12:
      case VK_F13:
      case VK_F14:
      case VK_F15:
      case VK_F16:
      case VK_F17:
      case VK_F18:
      case VK_F19:
      case VK_F20:
      case VK_F21:
      case VK_F22:
      case VK_F23:
      case VK_F24:
      case VK_PRINTSCREEN:
      case VK_CAPS_LOCK:
      case VK_PAUSE:
      case VK_INSERT:

      case VK_HELP:
      case VK_WINDOWS:
          return true;
    }
    return false;
 }

    private int eventType;
    private int keyCode;
    private char keyChar;

    public static final int EVENT_KEY_PRESSED = 1 << 0;
    public static final int EVENT_KEY_RELEASED= 1 << 1;
    public static final int EVENT_KEY_TYPED   = 1 << 2;

    /* Virtual key codes. */

    public static final int VK_ENTER          = '\n';
    public static final int VK_BACK_SPACE     = '\b';
    public static final int VK_TAB            = '\t';
    public static final int VK_CANCEL         = 0x03;
    public static final int VK_CLEAR          = 0x0C;
    public static final int VK_SHIFT          = 0x10;
    public static final int VK_CONTROL        = 0x11;
    public static final int VK_ALT            = 0x12;
    public static final int VK_PAUSE          = 0x13;
    public static final int VK_CAPS_LOCK      = 0x14;
    public static final int VK_ESCAPE         = 0x1B;
    public static final int VK_SPACE          = 0x20;
    public static final int VK_PAGE_UP        = 0x21;
    public static final int VK_PAGE_DOWN      = 0x22;
    public static final int VK_END            = 0x23;
    public static final int VK_HOME           = 0x24;

    /**
     * Constant for the non-numpad <b>left</b> arrow key.
     */
    public static final int VK_LEFT           = 0x25;

    /**
     * Constant for the non-numpad <b>up</b> arrow key.
     */
    public static final int VK_UP             = 0x26;

    /**
     * Constant for the non-numpad <b>right</b> arrow key.
     */
    public static final int VK_RIGHT          = 0x27;

    /**
     * Constant for the non-numpad <b>down</b> arrow key.
     */
    public static final int VK_DOWN           = 0x28;

    /** Constant for the F1 function key. */
    public static final int VK_F1             = 0x70;

    /** Constant for the F2 function key. */
    public static final int VK_F2             = 0x71;

    /** Constant for the F3 function key. */
    public static final int VK_F3             = 0x72;

    /** Constant for the F4 function key. */
    public static final int VK_F4             = 0x73;

    /** Constant for the F5 function key. */
    public static final int VK_F5             = 0x74;

    /** Constant for the F6 function key. */
    public static final int VK_F6             = 0x75;

    /** Constant for the F7 function key. */
    public static final int VK_F7             = 0x76;

    /** Constant for the F8 function key. */
    public static final int VK_F8             = 0x77;

    /** Constant for the F9 function key. */
    public static final int VK_F9             = 0x78;

    /** Constant for the F10 function key. */
    public static final int VK_F10            = 0x79;

    /** Constant for the F11 function key. */
    public static final int VK_F11            = 0x7A;

    /** Constant for the F12 function key. */
    public static final int VK_F12            = 0x7B;

    /**
     * Constant for the F13 function key.
     * @since 1.2
     */
    /* F13 - F24 are used on IBM 3270 keyboard; use random range for constants. */
    public static final int VK_F13            = 0xF000;
 
    /**
     * Constant for the F14 function key.
     * @since 1.2
     */
    public static final int VK_F14            = 0xF001;
 
    /**
     * Constant for the F15 function key.
     * @since 1.2
     */
    public static final int VK_F15            = 0xF002;
 
    /**
     * Constant for the F16 function key.
     * @since 1.2
     */
    public static final int VK_F16            = 0xF003;
 
    /**
     * Constant for the F17 function key.
     * @since 1.2
     */
    public static final int VK_F17            = 0xF004;
 
    /**
     * Constant for the F18 function key.
     * @since 1.2
     */
    public static final int VK_F18            = 0xF005;
 
    /**
     * Constant for the F19 function key.
     * @since 1.2
     */
    public static final int VK_F19            = 0xF006;
 
    /**
     * Constant for the F20 function key.
     * @since 1.2
     */
    public static final int VK_F20            = 0xF007;
 
    /**
     * Constant for the F21 function key.
     * @since 1.2
     */
    public static final int VK_F21            = 0xF008;
 
    /**
     * Constant for the F22 function key.
     * @since 1.2
     */
    public static final int VK_F22            = 0xF009;
 
    /**
     * Constant for the F23 function key.
     * @since 1.2
     */
    public static final int VK_F23            = 0xF00A;
 
    /**
     * Constant for the F24 function key.
     * @since 1.2
     */
    public static final int VK_F24            = 0xF00B;
 
    public static final int VK_PRINTSCREEN    = 0x9A;
    public static final int VK_INSERT         = 0x9B;
    public static final int VK_HELP           = 0x9C;
    public static final int VK_META           = 0x9D;

    public static final int VK_BACK_QUOTE     = 0xC0;
    public static final int VK_QUOTE          = 0xDE;

    public static final int VK_WINDOWS        = 0x020C;
}

