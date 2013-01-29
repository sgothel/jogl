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

/**
 * Key events are delivered in the following order:
 * <ol>
 *   <li>{@link #EVENT_KEY_PRESSED}</li>
 *   <li>{@link #EVENT_KEY_RELEASED}</li>
 *   <li>{@link #EVENT_KEY_TYPED}</li>
 * </ol>
 * In case the native platform does not
 * deliver keyboard events in the above order or skip events, 
 * the NEWT driver will reorder and inject synthetic events if required. 
 * <p>
 * Besides regular modifiers like {@link InputEvent#SHIFT_MASK} etc., 
 * the {@link InputEvent#AUTOREPEAT_MASK} bit is added if repetition is detected.
 * </p>
 * <p>
 * Auto-Repeat shall behave as follow:
 * <pre>
    D = pressed, U = released, T = typed
    0 = normal, 1 = auto-repeat

    D(0), [ U(1), T(1), D(1), U(1) T(1) ..], U(0) T(0)
 * </pre>
 * The idea is if you mask out auto-repeat in your event listener
 * you just get one long pressed key D/U/T triple.
 * </p>
 * <p>
 * {@link #isModifierKey() Modifiers keys} will produce regular events (pressed, released and typed),
 * however they will not produce Auto-Repeat events itself.
 * </p>
 */
@SuppressWarnings("serial")
public class KeyEvent extends InputEvent
{
    public KeyEvent(int eventType, Object source, long when, int modifiers, int keyCode, char keyChar) {
        super(eventType, source, when, modifiers); 
        this.keyCode=keyCode;
        this.keyChar=keyChar;
    }

    /** 
     * Returns the character matching the {@link #getKeyCode() virtual key code}, if exist.
     * <p>
     * <b>Disclaimer</b>: Only valid on all platforms at {@link KeyListener#keyTyped(KeyEvent)}.
     * Precisely, on the Windows platform we currently cannot deliver the proper character
     * in case of shifted keys where no uppercase exists, e.g. 'shift + 1' doesn't produce '!'.
     * </p> 
     */
    public char getKeyChar() {
        return keyChar;
    }

    /** Returns the virtual key code. */
    public int getKeyCode() {
        return keyCode;
    }

    public String toString() {
        return toString(null).toString();
    }

    public StringBuilder toString(StringBuilder sb) {
        if(null == sb) {
            sb = new StringBuilder();
        }
        sb.append("KeyEvent[").append(getEventTypeString(getEventType())).append(", code ").append(keyCode).append("(").append(toHexString(keyCode)).append("), char '").append(keyChar).append("' (").append(toHexString((int)keyChar)).append("), isActionKey ").append(isActionKey()).append(", ");
        return super.toString(sb).append("]");
    }

    public static String getEventTypeString(int type) {
        switch(type) {
        case EVENT_KEY_PRESSED: return "EVENT_KEY_PRESSED";
        case EVENT_KEY_RELEASED: return "EVENT_KEY_RELEASED";
        case EVENT_KEY_TYPED: return "EVENT_KEY_TYPED";
        default: return "unknown (" + type + ")";
        }
    }

    /** Returns true if <code>keyCode</code> represents a modifier key, i.e. one of {@link #VK_SHIFT}, {@link #VK_CONTROL}, {@link #VK_ALT}, {@link #VK_ALT_GRAPH}, {@link #VK_META}. */
    public static boolean isModifierKey(int keyCode) {
        switch (keyCode) {
            case VK_SHIFT:
            case VK_CONTROL:
            case VK_ALT:
            case VK_ALT_GRAPH:
            case VK_META:
                return true;
            default:
                return false;
        }
    }
    
    /** Returns true if {@link #getKeyCode()} represents a modifier key, i.e. one of {@link #VK_SHIFT}, {@link #VK_CONTROL}, {@link #VK_ALT}, {@link #VK_ALT_GRAPH}, {@link #VK_META}. */
    public boolean isModifierKey() {
        return isModifierKey(keyCode);
    }
    
    public boolean isActionKey() {
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

    private final int keyCode;
    private final char keyChar;

    public static final int EVENT_KEY_PRESSED = 300;
    public static final int EVENT_KEY_RELEASED= 301;
    public static final int EVENT_KEY_TYPED   = 302;

    /* Virtual key codes. */

    public static final int VK_CANCEL         = 0x03;
    public static final int VK_BACK_SPACE     = 0x08; // '\b'
    public static final int VK_TAB            = 0x09; // '\t'
    public static final int VK_ENTER          = 0x0A; // '\n'
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
     * @see #VK_KP_LEFT
     */
    public static final int VK_LEFT           = 0x25;

    /**
     * Constant for the non-numpad <b>up</b> arrow key.
     * @see #VK_KP_UP
     */
    public static final int VK_UP             = 0x26;

    /**
     * Constant for the non-numpad <b>right</b> arrow key.
     * @see #VK_KP_RIGHT
     */
    public static final int VK_RIGHT          = 0x27;

    /**
     * Constant for the non-numpad <b>down</b> arrow key.
     * @see #VK_KP_DOWN
     */
    public static final int VK_DOWN           = 0x28;

    /**
     * Constant for the comma key, ","
     */
    public static final int VK_COMMA          = 0x2C;

    /**
     * Constant for the minus key, "-"
     * @since 1.2
     */
    public static final int VK_MINUS          = 0x2D;

    /**
     * Constant for the period key, "."
     */
    public static final int VK_PERIOD         = 0x2E;

    /**
     * Constant for the forward slash key, "/"
     */
    public static final int VK_SLASH          = 0x2F;

    /** VK_0 thru VK_9 are the same as ASCII '0' thru '9' (0x30 - 0x39) */
    public static final int VK_0              = 0x30;
    public static final int VK_1              = 0x31;
    public static final int VK_2              = 0x32;
    public static final int VK_3              = 0x33;
    public static final int VK_4              = 0x34;
    public static final int VK_5              = 0x35;
    public static final int VK_6              = 0x36;
    public static final int VK_7              = 0x37;
    public static final int VK_8              = 0x38;
    public static final int VK_9              = 0x39;

    /**
     * Constant for the semicolon key, ";"
     */
    public static final int VK_SEMICOLON      = 0x3B;

    /**
     * Constant for the equals key, "="
     */
    public static final int VK_EQUALS         = 0x3D;

    /** VK_A thru VK_Z are the same as ASCII 'A' thru 'Z' (0x41 - 0x5A) */
    public static final int VK_A              = 0x41;
    public static final int VK_B              = 0x42;
    public static final int VK_C              = 0x43;
    public static final int VK_D              = 0x44;
    public static final int VK_E              = 0x45;
    public static final int VK_F              = 0x46;
    public static final int VK_G              = 0x47;
    public static final int VK_H              = 0x48;
    public static final int VK_I              = 0x49;
    public static final int VK_J              = 0x4A;
    public static final int VK_K              = 0x4B;
    public static final int VK_L              = 0x4C;
    public static final int VK_M              = 0x4D;
    public static final int VK_N              = 0x4E;
    public static final int VK_O              = 0x4F;
    public static final int VK_P              = 0x50;
    public static final int VK_Q              = 0x51;
    public static final int VK_R              = 0x52;
    public static final int VK_S              = 0x53;
    public static final int VK_T              = 0x54;
    public static final int VK_U              = 0x55;
    public static final int VK_V              = 0x56;
    public static final int VK_W              = 0x57;
    public static final int VK_X              = 0x58;
    public static final int VK_Y              = 0x59;
    public static final int VK_Z              = 0x5A;

    /**
     * Constant for the open bracket key, "["
     */
    public static final int VK_OPEN_BRACKET   = 0x5B;

    /**
     * Constant for the back slash key, "\"
     */
    public static final int VK_BACK_SLASH     = 0x5C;

    /**
     * Constant for the close bracket key, "]"
     */
    public static final int VK_CLOSE_BRACKET  = 0x5D;

    public static final int VK_NUMPAD0        = 0x60;
    public static final int VK_NUMPAD1        = 0x61;
    public static final int VK_NUMPAD2        = 0x62;
    public static final int VK_NUMPAD3        = 0x63;
    public static final int VK_NUMPAD4        = 0x64;
    public static final int VK_NUMPAD5        = 0x65;
    public static final int VK_NUMPAD6        = 0x66;
    public static final int VK_NUMPAD7        = 0x67;
    public static final int VK_NUMPAD8        = 0x68;
    public static final int VK_NUMPAD9        = 0x69;
    public static final int VK_MULTIPLY       = 0x6A;
    public static final int VK_ADD            = 0x6B;

    /** 
     * Constant for the Numpad Separator key. 
     */
    public static final int VK_SEPARATOR      = 0x6C;

    public static final int VK_SUBTRACT       = 0x6D;
    public static final int VK_DECIMAL        = 0x6E;
    public static final int VK_DIVIDE         = 0x6F;
    public static final int VK_DELETE         = 0x7F; /* ASCII DEL */
    public static final int VK_NUM_LOCK       = 0x90;
    public static final int VK_SCROLL_LOCK    = 0x91;

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
     * <p>F13 - F24 are used on IBM 3270 keyboard; use random range for constants.</p>
     */
    public static final int VK_F13            = 0xF000;

    /**
     * Constant for the F14 function key.
     * <p>F13 - F24 are used on IBM 3270 keyboard; use random range for constants.</p>
     */
    public static final int VK_F14            = 0xF001;

    /**
     * Constant for the F15 function key.
     * <p>F13 - F24 are used on IBM 3270 keyboard; use random range for constants.</p>
     */
    public static final int VK_F15            = 0xF002;

    /**
     * Constant for the F16 function key.
     * <p>F13 - F24 are used on IBM 3270 keyboard; use random range for constants.</p>
     */
    public static final int VK_F16            = 0xF003;

    /**
     * Constant for the F17 function key.
     * <p>F13 - F24 are used on IBM 3270 keyboard; use random range for constants.</p>
     */
    public static final int VK_F17            = 0xF004;

    /**
     * Constant for the F18 function key.
     * <p>F13 - F24 are used on IBM 3270 keyboard; use random range for constants.</p>
     */
    public static final int VK_F18            = 0xF005;

    /**
     * Constant for the F19 function key.
     * <p>F13 - F24 are used on IBM 3270 keyboard; use random range for constants.</p>
     */
    public static final int VK_F19            = 0xF006;

    /**
     * Constant for the F20 function key.
     * <p>F13 - F24 are used on IBM 3270 keyboard; use random range for constants.</p>
     */
    public static final int VK_F20            = 0xF007;

    /**
     * Constant for the F21 function key.
     * <p>F13 - F24 are used on IBM 3270 keyboard; use random range for constants.</p>
     */
    public static final int VK_F21            = 0xF008;

    /**
     * Constant for the F22 function key.
     * <p>F13 - F24 are used on IBM 3270 keyboard; use random range for constants.</p>
     */
    public static final int VK_F22            = 0xF009;

    /**
     * Constant for the F23 function key.
     * <p>F13 - F24 are used on IBM 3270 keyboard; use random range for constants.</p>
     */
    public static final int VK_F23            = 0xF00A;

    /**
     * Constant for the F24 function key.
     * <p>F13 - F24 are used on IBM 3270 keyboard; use random range for constants.</p>
     */
    public static final int VK_F24            = 0xF00B;

    public static final int VK_PRINTSCREEN    = 0x9A;
    public static final int VK_INSERT         = 0x9B;
    public static final int VK_HELP           = 0x9C;
    public static final int VK_META           = 0x9D;

    public static final int VK_BACK_QUOTE     = 0xC0;
    public static final int VK_QUOTE          = 0xDE;

    /**
     * Constant for the numeric keypad <b>up</b> arrow key.
     * @see #VK_UP
     */
    public static final int VK_KP_UP          = 0xE0;

    /**
     * Constant for the numeric keypad <b>down</b> arrow key.
     * @see #VK_DOWN
     */
    public static final int VK_KP_DOWN        = 0xE1;

    /**
     * Constant for the numeric keypad <b>left</b> arrow key.
     * @see #VK_LEFT
     */
    public static final int VK_KP_LEFT        = 0xE2;

    /**
     * Constant for the numeric keypad <b>right</b> arrow key.
     * @see #VK_RIGHT
     */
    public static final int VK_KP_RIGHT       = 0xE3;

    /** For European keyboards */
    public static final int VK_DEAD_GRAVE               = 0x80;
    /** For European keyboards */
    public static final int VK_DEAD_ACUTE               = 0x81;
    /** For European keyboards */
    public static final int VK_DEAD_CIRCUMFLEX          = 0x82;
    /** For European keyboards */
    public static final int VK_DEAD_TILDE               = 0x83;
    /** For European keyboards */
    public static final int VK_DEAD_MACRON              = 0x84;
    /** For European keyboards */
    public static final int VK_DEAD_BREVE               = 0x85;
    /** For European keyboards */
    public static final int VK_DEAD_ABOVEDOT            = 0x86;
    /** For European keyboards */
    public static final int VK_DEAD_DIAERESIS           = 0x87;
    /** For European keyboards */
    public static final int VK_DEAD_ABOVERING           = 0x88;
    /** For European keyboards */
    public static final int VK_DEAD_DOUBLEACUTE         = 0x89;
    /** For European keyboards */
    public static final int VK_DEAD_CARON               = 0x8a;
    /** For European keyboards */
    public static final int VK_DEAD_CEDILLA             = 0x8b;
    /** For European keyboards */
    public static final int VK_DEAD_OGONEK              = 0x8c;
    /** For European keyboards */
    public static final int VK_DEAD_IOTA                = 0x8d;
    /** For European keyboards */
    public static final int VK_DEAD_VOICED_SOUND        = 0x8e;
    /** For European keyboards */
    public static final int VK_DEAD_SEMIVOICED_SOUND    = 0x8f;

    /** For European keyboards */
    public static final int VK_AMPERSAND                = 0x96;
    /** For European keyboards */
    public static final int VK_ASTERISK                 = 0x97;
    /** For European keyboards */
    public static final int VK_QUOTEDBL                 = 0x98;
    /** For European keyboards */
    public static final int VK_LESS                     = 0x99;

    /** For European keyboards */
    public static final int VK_GREATER                  = 0xa0;
    /** For European keyboards */
    public static final int VK_BRACELEFT                = 0xa1;
    /** For European keyboards */
    public static final int VK_BRACERIGHT               = 0xa2;

    /**
     * Constant for the "@" key.
     */
    public static final int VK_AT                       = 0x0200;

    /**
     * Constant for the ":" key.
     */
    public static final int VK_COLON                    = 0x0201;

    /**
     * Constant for the "^" key.
     */
    public static final int VK_CIRCUMFLEX               = 0x0202;

    /**
     * Constant for the "$" key.
     */
    public static final int VK_DOLLAR                   = 0x0203;

    /**
     * Constant for the Euro currency sign key.
     */
    public static final int VK_EURO_SIGN                = 0x0204;

    /**
     * Constant for the "!" key.
     */
    public static final int VK_EXCLAMATION_MARK         = 0x0205;

    /**
     * Constant for the inverted exclamation mark key.
     */
    public static final int VK_INVERTED_EXCLAMATION_MARK = 0x0206;

    /**
     * Constant for the "(" key.
     */
    public static final int VK_LEFT_PARENTHESIS         = 0x0207;

    /**
     * Constant for the "#" key.
     */
    public static final int VK_NUMBER_SIGN              = 0x0208;

    /**
     * Constant for the "+" key.
     */
    public static final int VK_PLUS                     = 0x0209;

    /**
     * Constant for the ")" key.
     */
    public static final int VK_RIGHT_PARENTHESIS        = 0x020A;

    /**
     * Constant for the "_" key.
     */
    public static final int VK_UNDERSCORE               = 0x020B;

    /**
     * Constant for the Microsoft Windows "Windows" key.
     * It is used for both the left and right version of the key.  
     */
    public static final int VK_WINDOWS                  = 0x020C;

    /**
     * Constant for the Microsoft Windows Context Menu key.
     */
    public static final int VK_CONTEXT_MENU             = 0x020D;

    /* for input method support on Asian Keyboards */

    /* not clear what this means - listed in Microsoft Windows API */
    public static final int VK_FINAL                    = 0x0018;

    /** Constant for the Convert function key. */
    /* Japanese PC 106 keyboard, Japanese Solaris keyboard: henkan */
    public static final int VK_CONVERT                  = 0x001C;

    /** Constant for the Don't Convert function key. */
    /* Japanese PC 106 keyboard: muhenkan */
    public static final int VK_NONCONVERT               = 0x001D;

    /** Constant for the Accept or Commit function key. */
    /* Japanese Solaris keyboard: kakutei */
    public static final int VK_ACCEPT                   = 0x001E;

    /* not clear what this means - listed in Microsoft Windows API */
    public static final int VK_MODECHANGE               = 0x001F;

    /* replaced by VK_KANA_LOCK for Microsoft Windows and Solaris; 
       might still be used on other platforms */
    public static final int VK_KANA                     = 0x0015;

    /* replaced by VK_INPUT_METHOD_ON_OFF for Microsoft Windows and Solaris; 
       might still be used for other platforms */
    public static final int VK_KANJI                    = 0x0019;

    /**
     * Constant for the Alphanumeric function key.
     */
    /* Japanese PC 106 keyboard: eisuu */
    public static final int VK_ALPHANUMERIC             = 0x00F0;

    /**
     * Constant for the Katakana function key.
     */
    /* Japanese PC 106 keyboard: katakana */
    public static final int VK_KATAKANA                 = 0x00F1;

    /**
     * Constant for the Hiragana function key.
     */
    /* Japanese PC 106 keyboard: hiragana */
    public static final int VK_HIRAGANA                 = 0x00F2;

    /**
     * Constant for the Full-Width Characters function key.
     */
    /* Japanese PC 106 keyboard: zenkaku */
    public static final int VK_FULL_WIDTH               = 0x00F3;

    /**
     * Constant for the Half-Width Characters function key.
     */
    /* Japanese PC 106 keyboard: hankaku */
    public static final int VK_HALF_WIDTH               = 0x00F4;

    /**
     * Constant for the Roman Characters function key.
     */
    /* Japanese PC 106 keyboard: roumaji */
    public static final int VK_ROMAN_CHARACTERS         = 0x00F5;

    /**
     * Constant for the All Candidates function key.
     */
    /* Japanese PC 106 keyboard - VK_CONVERT + ALT: zenkouho */
    public static final int VK_ALL_CANDIDATES           = 0x0100;

    /**
     * Constant for the Previous Candidate function key.
     */
    /* Japanese PC 106 keyboard - VK_CONVERT + SHIFT: maekouho */
    public static final int VK_PREVIOUS_CANDIDATE       = 0x0101;

    /**
     * Constant for the Code Input function key.
     */
    /* Japanese PC 106 keyboard - VK_ALPHANUMERIC + ALT: kanji bangou */
    public static final int VK_CODE_INPUT               = 0x0102;

    /**
     * Constant for the Japanese-Katakana function key.
     * This key switches to a Japanese input method and selects its Katakana input mode.
     */
    /* Japanese Macintosh keyboard - VK_JAPANESE_HIRAGANA + SHIFT */
    public static final int VK_JAPANESE_KATAKANA        = 0x0103;

    /**
     * Constant for the Japanese-Hiragana function key.
     * This key switches to a Japanese input method and selects its Hiragana input mode.
     */
    /* Japanese Macintosh keyboard */
    public static final int VK_JAPANESE_HIRAGANA        = 0x0104;

    /**
     * Constant for the Japanese-Roman function key.
     * This key switches to a Japanese input method and selects its Roman-Direct input mode.
     */
    /* Japanese Macintosh keyboard */
    public static final int VK_JAPANESE_ROMAN           = 0x0105;

    /**
     * Constant for the locking Kana function key.
     * This key locks the keyboard into a Kana layout.
     */
    /* Japanese PC 106 keyboard with special Windows driver - eisuu + Control; Japanese Solaris keyboard: kana */
    public static final int VK_KANA_LOCK                = 0x0106;

    /**
     * Constant for the input method on/off key.
     */
    /* Japanese PC 106 keyboard: kanji. Japanese Solaris keyboard: nihongo */
    public static final int VK_INPUT_METHOD_ON_OFF      = 0x0107;

    /* for Sun keyboards */
    public static final int VK_CUT                      = 0xFFD1;
    public static final int VK_COPY                     = 0xFFCD;
    public static final int VK_PASTE                    = 0xFFCF;
    public static final int VK_UNDO                     = 0xFFCB;
    public static final int VK_AGAIN                    = 0xFFC9;
    public static final int VK_FIND                     = 0xFFD0;
    public static final int VK_PROPS                    = 0xFFCA;
    public static final int VK_STOP                     = 0xFFC8;

    /**
     * Constant for the Compose function key.
     */
    public static final int VK_COMPOSE                  = 0xFF20;

    /**
     * Constant for the AltGraph function key.
     */
    public static final int VK_ALT_GRAPH                = 0xFF7E;

    /**
     * Constant for the Begin key.
     */
    public static final int VK_BEGIN                    = 0xFF58;

    /**
     * Constant for the Android's soft keyboard Back button.
     */
    public static final int VK_KEYBOARD_INVISIBLE       = 0xBAC00BAC;

    /**
     * This value is used to indicate that the keyCode is unknown.
     * KEY_TYPED events do not have a keyCode value; this value 
     * is used instead.  
     */
    public static final int VK_UNDEFINED      = 0x0;
}

