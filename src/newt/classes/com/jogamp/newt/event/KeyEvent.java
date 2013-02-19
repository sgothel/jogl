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
 * <p>
 * <table border="0">
 *   <tr><th>#</th><th>Event Type</th>      <th>Constraints</th>  <th>Notes</th></tr>
 *   <tr><td>1</td><td>{@link #EVENT_KEY_PRESSED}  </td><td> <i> excluding {@link #isAutoRepeat() auto-repeat} {@link #isModifierKey() modifier} keys</i></td><td></td></tr>
 *   <tr><td>2</td><td>{@link #EVENT_KEY_RELEASED} </td><td> <i> excluding {@link #isAutoRepeat() auto-repeat} {@link #isModifierKey() modifier} keys</i></td><td></td></tr>
 *   <tr><td>3</td><td>{@link #EVENT_KEY_TYPED}    </td><td> <i>only for {@link #isPrintableKey() printable} and non {@link #isAutoRepeat() auto-repeat} keys</i></td><td><b>Deprecated</b>: Use {@link #EVENT_KEY_RELEASED} and apply constraints.</td></tr>
 * </table>
 * </p>
 * In case the native platform does not
 * deliver keyboard events in the above order or skip events, 
 * the NEWT driver will reorder and inject synthetic events if required. 
 * <p>
 * Besides regular modifiers like {@link InputEvent#SHIFT_MASK} etc., 
 * the {@link InputEvent#AUTOREPEAT_MASK} bit is added if repetition is detected, following above constraints.
 * </p>
 * <p>
 * Auto-Repeat shall behave as follow:
 * <pre>
    P = pressed, R = released, T = typed
    0 = normal, 1 = auto-repeat

    P(0), [ R(1), P(1), R(1), ..], R(0) T(0)    
 * </pre>
 * The idea is if you mask out auto-repeat in your event listener
 * or catch {@link #EVENT_KEY_TYPED typed} events only, 
 * you just get one long pressed P/R/T triple for {@link #isPrintableKey() printable} keys.
 * {@link #isActionKey() Action} keys would produce one long pressed P/R tuple in case you mask out auto-repeat . 
 * </p>
 * <p>
 * {@link #isActionKey() Action} keys will produce {@link #EVENT_KEY_PRESSED pressed} 
 * and {@link #EVENT_KEY_RELEASED released} events including {@link #isAutoRepeat() auto-repeat}.
 * </p>
 * <p>
 * {@link #isPrintableKey() Printable} keys will produce {@link #EVENT_KEY_PRESSED pressed}, 
 * {@link #EVENT_KEY_RELEASED released} and {@link #EVENT_KEY_TYPED typed} events, the latter is excluded for {@link #isAutoRepeat() auto-repeat} events.
 * </p>
 * <p>
 * {@link #isModifierKey() Modifier} keys will produce {@link #EVENT_KEY_PRESSED pressed} 
 * and {@link #EVENT_KEY_RELEASED released} events excluding {@link #isAutoRepeat() auto-repeat}.
 * They will also influence subsequent event's {@link #getModifiers() modifier} bits while pressed.
 * </p>
 */
@SuppressWarnings("serial")
public class KeyEvent extends InputEvent
{
    public KeyEvent(short eventType, Object source, long when, int modifiers, short keyCode, short keySym, char keyChar) {
        super(eventType, source, when, modifiers); 
        this.keyCode=keyCode;
        this.keySym=keySym;
        this.keyChar=keyChar;
        { // cache modifier and action flags
            byte _flags = 0;
            if( isModifierKey(keySym) ) {
                _flags |= F_MODIFIER_MASK;
            }
            if( isActionKey(keySym) ) {
                _flags |= F_ACTION_MASK;
            }
            flags = _flags;
        }
    }

    /** 
     * Returns the <i>UTF-16</i> character reflecting the {@link #getKeySymbol() key symbol}.
     * @see #getKeySymbol()
     * @see #getKeyCode()
     */
    public final char getKeyChar() {
        return keyChar;
    }

    /** 
     * Returns the virtual <i>key symbol</i> reflecting the current <i>keyboard layout</i>.
     * @see #getKeyChar()
     * @see #getKeyCode()
     */
    public final short getKeySymbol() {
        return keySym;
    }
    
    /** 
     * Returns the virtual <i>key code</i> using a fixed mapping to the <i>US keyboard layout</i>.
     * <p>
     * In contrast to {@link #getKeySymbol() key symbol}, <i>key code</i> 
     * uses a fixed <i>US keyboard layout</i> and therefore is keyboard layout independent. 
     * </p>
     * <p>
     * E.g. <i>virtual key code</i> {@link #VK_Y} denotes the same physical key 
     * regardless whether <i>keyboard layout</i> <code>QWERTY</code> or 
     * <code>QWERTZ</code> is active. The {@link #getKeySymbol() key symbol} of the former is
     * {@link #VK_Y}, where the latter produces {@link #VK_Y}. 
     * </p>
     * <p>
     * <b>Disclaimer</b>: In case <i>key code</i> is not implemented on your platform (OSX, ..)
     * the {@link #getKeySymbol() key symbol} is returned. 
     * </p>
     * @see #getKeyChar()
     * @see #getKeySymbol()
     */
    public final short getKeyCode() {
        return keyCode;
    }

    public final String toString() {
        return toString(null).toString();
    }

    public final StringBuilder toString(StringBuilder sb) {
        if(null == sb) {
            sb = new StringBuilder();
        }
        sb.append("KeyEvent[").append(getEventTypeString(getEventType())).append(", code ").append(toHexString(keyCode)).append(", sym ").append(toHexString(keySym)).append(", char '").append(keyChar).append("' (").append(toHexString((short)keyChar))
        .append("), isModifierKey ").append(isModifierKey()).append(", isActionKey ").append(isActionKey()).append(", ");
        return super.toString(sb).append("]");
    }

    public static String getEventTypeString(short type) {
        switch(type) {
        case EVENT_KEY_PRESSED: return "EVENT_KEY_PRESSED";
        case EVENT_KEY_RELEASED: return "EVENT_KEY_RELEASED";
        case EVENT_KEY_TYPED: return "EVENT_KEY_TYPED";
        default: return "unknown (" + type + ")";
        }
    }

    /** 
     * Returns <code>true</code> if the given <code>virtualKey</code> represents a modifier key, otherwise <code>false</code>. 
     * <p>
     * A modifier key is one of {@link #VK_SHIFT}, {@link #VK_CONTROL}, {@link #VK_ALT}, {@link #VK_ALT_GRAPH}, {@link #VK_META}.
     * </p>
     */
    public static boolean isModifierKey(short vKey) {
        switch (vKey) {
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
    
    /** 
     * Returns <code>true</code> if {@link #getKeySymbol() key symbol} represents a modifier key,
     * otherwise <code>false</code>. 
     * <p>
     * See {@link #isModifierKey(short)} for details.
     * </p>
     * <p>
     * Note: Implementation uses a cached value.
     * </p>
     */
    public final boolean isModifierKey() {
        return 0 != ( F_MODIFIER_MASK & flags ) ;
    }

    /** 
     * Returns <code>true</code> if the given <code>virtualKey</code> represents a non-printable and
     * non-{@link #isModifierKey(short) modifier} action key, otherwise <code>false</code>. 
     * <p>
     * An action key is one of {@link #VK_HOME}, {@link #VK_END}, {@link #VK_PAGE_UP}, {@link #VK_PAGE_DOWN}, {@link #VK_UP}, {@link #VK_PAGE_DOWN}, 
     * {@link #VK_LEFT}, {@link #VK_RIGHT}, {@link #VK_F1}-{@link #VK_F24}, {@link #VK_PRINTSCREEN}, {@link #VK_CAPS_LOCK}, {@link #VK_PAUSE}, 
     * {@link #VK_INSERT}, {@link #VK_HELP}, {@link #VK_WINDOWS}, etc ...
     * </p>
     */
    public static boolean isActionKey(short vKey) {
        if( ( VK_F1             <= vKey && vKey <= VK_F24                 ) ||
            ( VK_ALL_CANDIDATES <= vKey && vKey <= VK_INPUT_METHOD_ON_OFF ) ||
            ( VK_CUT            <= vKey && vKey <= VK_STOP                ) ) {
            return true;
        }
                
        switch (vKey) {
            case VK_CANCEL:
            case VK_CLEAR:
            case VK_PAUSE:
            case VK_CAPS_LOCK:
            case VK_ESCAPE:
            case VK_PAGE_UP:
            case VK_PAGE_DOWN:
            case VK_END:
            case VK_HOME:
            case VK_LEFT:
            case VK_UP:
            case VK_RIGHT:
            case VK_DOWN:
            case VK_DELETE:
            case VK_NUM_LOCK:
            case VK_SCROLL_LOCK:
                    
            case VK_PRINTSCREEN:
            case VK_INSERT:
            case VK_HELP:
            case VK_META:
            case VK_KP_UP:
            case VK_KP_DOWN:
            case VK_KP_LEFT:
            case VK_KP_RIGHT:
                
            case VK_DEAD_VOICED_SOUND:
            case VK_DEAD_SEMIVOICED_SOUND:
                
            case VK_WINDOWS:
            case VK_CONTEXT_MENU:
            case VK_FINAL:
                
            case VK_CONVERT: 
            case VK_NONCONVERT:
            case VK_ACCEPT:
            case VK_MODECHANGE:

            case VK_KANA:
            case VK_KANJI:
                
            case VK_ALPHANUMERIC:
            case VK_KATAKANA:
            case VK_HIRAGANA:
            case VK_FULL_WIDTH:
            case VK_HALF_WIDTH:
            case VK_ROMAN_CHARACTERS:
                
            case VK_COMPOSE:
            case VK_BEGIN:
                    
                return true;
        }
        return false;
    }
    
    /** 
     * Returns <code>true</code> if {@link #getKeySymbol() key symbol} represents a non-printable and 
     * non-{@link #isModifierKey(short) modifier} action key, otherwise <code>false</code>. 
     * <p>
     * See {@link #isActionKey(short)} for details.
     * </p>
     */
    public final boolean isActionKey() {
        return 0 != ( F_ACTION_MASK & flags ) ;
    }
    
    /** 
     * Returns <code>true</code> if given <code>virtualKey</code> represents a printable character, 
     * i.e. neither a {@link #isModifierKey(short) modifier key}
     * nor an {@link #isActionKey(short) action key}.
     * Otherwise returns <code>false</code>.
     */
    public static boolean isPrintableKey(short vKey) {
        return !isModifierKey(vKey) && !isActionKey(vKey);
    }

    /** 
     * Returns <code>true</code> if {@link #getKeySymbol() key symbol} represents a printable character, 
     * i.e. neither a {@link #isModifierKey(short) modifier key}
     * nor an {@link #isActionKey(short) action key}.
     * Otherwise returns <code>false</code>.
     */
    public final boolean isPrintableKey() {
        return 0 == ( F_NON_PRINT_MASK & flags ) ;
    }
    
    private final short keyCode;
    private final short keySym;
    private final char keyChar;
    private final byte flags;
    private static final byte F_MODIFIER_MASK   = 1 << 0;
    private static final byte F_ACTION_MASK     = 1 << 1;
    private static final byte F_NON_PRINT_MASK  = F_MODIFIER_MASK | F_ACTION_MASK ;

    /** A key has been pressed, excluding {@link #isAutoRepeat() auto-repeat} {@link #isModifierKey() modifier} keys. */
    public static final short EVENT_KEY_PRESSED = 300;
    /** A key has been released, excluding {@link #isAutoRepeat() auto-repeat} {@link #isModifierKey() modifier} keys. */
    public static final short EVENT_KEY_RELEASED= 301;
    /** 
     * A {@link #isPrintableKey() printable} key has been typed (pressed and released), excluding {@link #isAutoRepeat() auto-repeat}.
     * @deprecated Redundant, will be removed soon. Use {@link #EVENT_KEY_RELEASED} and exclude non {@link #isPrintableKey() printable} keys and {@link #isAutoRepeat() auto-repeat}.
     */ 
    public static final short EVENT_KEY_TYPED   = 302;

    /* Virtual key codes. */

    public static final short VK_CANCEL         = (short) 0x03;
    public static final short VK_BACK_SPACE     = (short) 0x08; // '\b'
    public static final short VK_TAB            = (short) 0x09; // '\t'
    public static final short VK_ENTER          = (short) 0x0A; // '\n'
    public static final short VK_CLEAR          = (short) 0x0C;
    public static final short VK_SHIFT          = (short) 0x10;
    public static final short VK_CONTROL        = (short) 0x11;
    public static final short VK_ALT            = (short) 0x12;
    public static final short VK_PAUSE          = (short) 0x13;
    public static final short VK_CAPS_LOCK      = (short) 0x14;
    public static final short VK_ESCAPE         = (short) 0x1B;
    public static final short VK_SPACE          = (short) 0x20;
    public static final short VK_PAGE_UP        = (short) 0x21;
    public static final short VK_PAGE_DOWN      = (short) 0x22;
    public static final short VK_END            = (short) 0x23;
    public static final short VK_HOME           = (short) 0x24;

    /**
     * Constant for the non-numpad <b>left</b> arrow key.
     * @see #VK_KP_LEFT
     */
    public static final short VK_LEFT           = (short) 0x25;

    /**
     * Constant for the non-numpad <b>up</b> arrow key.
     * @see #VK_KP_UP
     */
    public static final short VK_UP             = (short) 0x26;

    /**
     * Constant for the non-numpad <b>right</b> arrow key.
     * @see #VK_KP_RIGHT
     */
    public static final short VK_RIGHT          = (short) 0x27;

    /**
     * Constant for the non-numpad <b>down</b> arrow key.
     * @see #VK_KP_DOWN
     */
    public static final short VK_DOWN           = (short) 0x28;

    /**
     * Constant for the comma key, ","
     */
    public static final short VK_COMMA          = (short) 0x2C;

    /**
     * Constant for the minus key, "-"
     * @since 1.2
     */
    public static final short VK_MINUS          = (short) 0x2D;

    /**
     * Constant for the period key, "."
     */
    public static final short VK_PERIOD         = (short) 0x2E;

    /**
     * Constant for the forward slash key, "/"
     */
    public static final short VK_SLASH          = (short) 0x2F;

    /** VK_0 thru VK_9 are the same as ASCII '0' thru '9' (0x30 - 0x39) */
    public static final short VK_0              = (short) 0x30;
    public static final short VK_1              = (short) 0x31;
    public static final short VK_2              = (short) 0x32;
    public static final short VK_3              = (short) 0x33;
    public static final short VK_4              = (short) 0x34;
    public static final short VK_5              = (short) 0x35;
    public static final short VK_6              = (short) 0x36;
    public static final short VK_7              = (short) 0x37;
    public static final short VK_8              = (short) 0x38;
    public static final short VK_9              = (short) 0x39;

    /**
     * Constant for the semicolon key, ";"
     */
    public static final short VK_SEMICOLON      = (short) 0x3B;

    /**
     * Constant for the equals key, "="
     */
    public static final short VK_EQUALS         = (short) 0x3D;

    /** VK_A thru VK_Z are the same as ASCII 'A' thru 'Z' (0x41 - 0x5A) */
    public static final short VK_A              = (short) 0x41;
    public static final short VK_B              = (short) 0x42;
    public static final short VK_C              = (short) 0x43;
    public static final short VK_D              = (short) 0x44;
    public static final short VK_E              = (short) 0x45;
    public static final short VK_F              = (short) 0x46;
    public static final short VK_G              = (short) 0x47;
    public static final short VK_H              = (short) 0x48;
    public static final short VK_I              = (short) 0x49;
    public static final short VK_J              = (short) 0x4A;
    public static final short VK_K              = (short) 0x4B;
    public static final short VK_L              = (short) 0x4C;
    public static final short VK_M              = (short) 0x4D;
    public static final short VK_N              = (short) 0x4E;
    public static final short VK_O              = (short) 0x4F;
    public static final short VK_P              = (short) 0x50;
    public static final short VK_Q              = (short) 0x51;
    public static final short VK_R              = (short) 0x52;
    public static final short VK_S              = (short) 0x53;
    public static final short VK_T              = (short) 0x54;
    public static final short VK_U              = (short) 0x55;
    public static final short VK_V              = (short) 0x56;
    public static final short VK_W              = (short) 0x57;
    public static final short VK_X              = (short) 0x58;
    public static final short VK_Y              = (short) 0x59;
    public static final short VK_Z              = (short) 0x5A;

    /**
     * Constant for the open bracket key, "["
     */
    public static final short VK_OPEN_BRACKET   = (short) 0x5B;

    /**
     * Constant for the back slash key, "\"
     */
    public static final short VK_BACK_SLASH     = (short) 0x5C;

    /**
     * Constant for the close bracket key, "]"
     */
    public static final short VK_CLOSE_BRACKET  = (short) 0x5D;

    public static final short VK_NUMPAD0        = (short) 0x60;
    public static final short VK_NUMPAD1        = (short) 0x61;
    public static final short VK_NUMPAD2        = (short) 0x62;
    public static final short VK_NUMPAD3        = (short) 0x63;
    public static final short VK_NUMPAD4        = (short) 0x64;
    public static final short VK_NUMPAD5        = (short) 0x65;
    public static final short VK_NUMPAD6        = (short) 0x66;
    public static final short VK_NUMPAD7        = (short) 0x67;
    public static final short VK_NUMPAD8        = (short) 0x68;
    public static final short VK_NUMPAD9        = (short) 0x69;
    public static final short VK_MULTIPLY       = (short) 0x6A;
    public static final short VK_ADD            = (short) 0x6B;

    /** 
     * Constant for the Numpad Separator key. 
     */
    public static final short VK_SEPARATOR      = (short) 0x6C;

    public static final short VK_SUBTRACT       = (short) 0x6D;
    public static final short VK_DECIMAL        = (short) 0x6E;
    public static final short VK_DIVIDE         = (short) 0x6F;
    public static final short VK_DELETE         = (short) 0x7F; /* ASCII DEL */
    public static final short VK_NUM_LOCK       = (short) 0x90;
    public static final short VK_SCROLL_LOCK    = (short) 0x91;

    /** Constant for the F1 function key. */
    public static final short VK_F1             = (short) 0x70;

    /** Constant for the F2 function key. */
    public static final short VK_F2             = (short) 0x71;

    /** Constant for the F3 function key. */
    public static final short VK_F3             = (short) 0x72;

    /** Constant for the F4 function key. */
    public static final short VK_F4             = (short) 0x73;

    /** Constant for the F5 function key. */
    public static final short VK_F5             = (short) 0x74;

    /** Constant for the F6 function key. */
    public static final short VK_F6             = (short) 0x75;

    /** Constant for the F7 function key. */
    public static final short VK_F7             = (short) 0x76;

    /** Constant for the F8 function key. */
    public static final short VK_F8             = (short) 0x77;

    /** Constant for the F9 function key. */
    public static final short VK_F9             = (short) 0x78;

    /** Constant for the F10 function key. */
    public static final short VK_F10            = (short) 0x79;

    /** Constant for the F11 function key. */
    public static final short VK_F11            = (short) 0x7A;

    /** Constant for the F12 function key. */
    public static final short VK_F12            = (short) 0x7B;

    /**
     * Constant for the F13 function key.
     * <p>F13 - F24 are used on IBM 3270 keyboard; use random range for constants.</p>
     */
    public static final short VK_F13            = (short) 0xF000;

    /**
     * Constant for the F14 function key.
     * <p>F13 - F24 are used on IBM 3270 keyboard; use random range for constants.</p>
     */
    public static final short VK_F14            = (short) 0xF001;

    /**
     * Constant for the F15 function key.
     * <p>F13 - F24 are used on IBM 3270 keyboard; use random range for constants.</p>
     */
    public static final short VK_F15            = (short) 0xF002;

    /**
     * Constant for the F16 function key.
     * <p>F13 - F24 are used on IBM 3270 keyboard; use random range for constants.</p>
     */
    public static final short VK_F16            = (short) 0xF003;

    /**
     * Constant for the F17 function key.
     * <p>F13 - F24 are used on IBM 3270 keyboard; use random range for constants.</p>
     */
    public static final short VK_F17            = (short) 0xF004;

    /**
     * Constant for the F18 function key.
     * <p>F13 - F24 are used on IBM 3270 keyboard; use random range for constants.</p>
     */
    public static final short VK_F18            = (short) 0xF005;

    /**
     * Constant for the F19 function key.
     * <p>F13 - F24 are used on IBM 3270 keyboard; use random range for constants.</p>
     */
    public static final short VK_F19            = (short) 0xF006;

    /**
     * Constant for the F20 function key.
     * <p>F13 - F24 are used on IBM 3270 keyboard; use random range for constants.</p>
     */
    public static final short VK_F20            = (short) 0xF007;

    /**
     * Constant for the F21 function key.
     * <p>F13 - F24 are used on IBM 3270 keyboard; use random range for constants.</p>
     */
    public static final short VK_F21            = (short) 0xF008;

    /**
     * Constant for the F22 function key.
     * <p>F13 - F24 are used on IBM 3270 keyboard; use random range for constants.</p>
     */
    public static final short VK_F22            = (short) 0xF009;

    /**
     * Constant for the F23 function key.
     * <p>F13 - F24 are used on IBM 3270 keyboard; use random range for constants.</p>
     */
    public static final short VK_F23            = (short) 0xF00A;

    /**
     * Constant for the F24 function key.
     * <p>F13 - F24 are used on IBM 3270 keyboard; use random range for constants.</p>
     */
    public static final short VK_F24            = (short) 0xF00B;

    public static final short VK_PRINTSCREEN    = (short) 0x9A;
    public static final short VK_INSERT         = (short) 0x9B;
    public static final short VK_HELP           = (short) 0x9C;
    public static final short VK_META           = (short) 0x9D;

    public static final short VK_BACK_QUOTE     = (short) 0xC0;
    public static final short VK_QUOTE          = (short) 0xDE;

    /**
     * Constant for the numeric keypad <b>up</b> arrow key.
     * @see #VK_UP
     */
    public static final short VK_KP_UP          = (short) 0xE0;

    /**
     * Constant for the numeric keypad <b>down</b> arrow key.
     * @see #VK_DOWN
     */
    public static final short VK_KP_DOWN        = (short) 0xE1;

    /**
     * Constant for the numeric keypad <b>left</b> arrow key.
     * @see #VK_LEFT
     */
    public static final short VK_KP_LEFT        = (short) 0xE2;

    /**
     * Constant for the numeric keypad <b>right</b> arrow key.
     * @see #VK_RIGHT
     */
    public static final short VK_KP_RIGHT       = (short) 0xE3;

    /** For European keyboards */
    public static final short VK_DEAD_GRAVE               = (short) 0x80;
    /** For European keyboards */
    public static final short VK_DEAD_ACUTE               = (short) 0x81;
    /** For European keyboards */
    public static final short VK_DEAD_CIRCUMFLEX          = (short) 0x82;
    /** For European keyboards */
    public static final short VK_DEAD_TILDE               = (short) 0x83;
    /** For European keyboards */
    public static final short VK_DEAD_MACRON              = (short) 0x84;
    /** For European keyboards */
    public static final short VK_DEAD_BREVE               = (short) 0x85;
    /** For European keyboards */
    public static final short VK_DEAD_ABOVEDOT            = (short) 0x86;
    /** For European keyboards */
    public static final short VK_DEAD_DIAERESIS           = (short) 0x87;
    /** For European keyboards */
    public static final short VK_DEAD_ABOVERING           = (short) 0x88;
    /** For European keyboards */
    public static final short VK_DEAD_DOUBLEACUTE         = (short) 0x89;
    /** For European keyboards */
    public static final short VK_DEAD_CARON               = (short) 0x8a;
    /** For European keyboards */
    public static final short VK_DEAD_CEDILLA             = (short) 0x8b;
    /** For European keyboards */
    public static final short VK_DEAD_OGONEK              = (short) 0x8c;
    /** For European keyboards */
    public static final short VK_DEAD_IOTA                = (short) 0x8d;
    /** For European keyboards */
    public static final short VK_DEAD_VOICED_SOUND        = (short) 0x8e;
    /** For European keyboards */
    public static final short VK_DEAD_SEMIVOICED_SOUND    = (short) 0x8f;

    /** For European keyboards */
    public static final short VK_AMPERSAND                = (short) 0x96;
    /** For European keyboards */
    public static final short VK_ASTERISK                 = (short) 0x97;
    /** For European keyboards */
    public static final short VK_QUOTEDBL                 = (short) 0x98;
    /** For European keyboards */
    public static final short VK_LESS                     = (short) 0x99;

    /** For European keyboards */
    public static final short VK_GREATER                  = (short) 0xa0;
    /** For European keyboards */
    public static final short VK_BRACELEFT                = (short) 0xa1;
    /** For European keyboards */
    public static final short VK_BRACERIGHT               = (short) 0xa2;

    /**
     * Constant for the "@" key.
     */
    public static final short VK_AT                       = (short) 0x0200;

    /**
     * Constant for the ":" key.
     */
    public static final short VK_COLON                    = (short) 0x0201;

    /**
     * Constant for the "^" key.
     */
    public static final short VK_CIRCUMFLEX               = (short) 0x0202;

    /**
     * Constant for the "$" key.
     */
    public static final short VK_DOLLAR                   = (short) 0x0203;

    /**
     * Constant for the Euro currency sign key.
     */
    public static final short VK_EURO_SIGN                = (short) 0x0204;

    /**
     * Constant for the "!" key.
     */
    public static final short VK_EXCLAMATION_MARK         = (short) 0x0205;

    /**
     * Constant for the inverted exclamation mark key.
     */
    public static final short VK_INVERTED_EXCLAMATION_MARK = (short) 0x0206;

    /**
     * Constant for the "(" key.
     */
    public static final short VK_LEFT_PARENTHESIS         = (short) 0x0207;

    /**
     * Constant for the "#" key.
     */
    public static final short VK_NUMBER_SIGN              = (short) 0x0208;

    /**
     * Constant for the "+" key.
     */
    public static final short VK_PLUS                     = (short) 0x0209;

    /**
     * Constant for the ")" key.
     */
    public static final short VK_RIGHT_PARENTHESIS        = (short) 0x020A;

    /**
     * Constant for the "_" key.
     */
    public static final short VK_UNDERSCORE               = (short) 0x020B;

    /**
     * Constant for the Microsoft Windows "Windows" key.
     * It is used for both the left and right version of the key.  
     */
    public static final short VK_WINDOWS                  = (short) 0x020C;

    /**
     * Constant for the Microsoft Windows Context Menu key.
     */
    public static final short VK_CONTEXT_MENU             = (short) 0x020D;

    /* for input method support on Asian Keyboards */

    /* not clear what this means - listed in Microsoft Windows API */
    public static final short VK_FINAL                    = (short) 0x0018;

    /** Constant for the Convert function key. */
    /* Japanese PC 106 keyboard, Japanese Solaris keyboard: henkan */
    public static final short VK_CONVERT                  = (short) 0x001C;

    /** Constant for the Don't Convert function key. */
    /* Japanese PC 106 keyboard: muhenkan */
    public static final short VK_NONCONVERT               = (short) 0x001D;

    /** Constant for the Accept or Commit function key. */
    /* Japanese Solaris keyboard: kakutei */
    public static final short VK_ACCEPT                   = (short) 0x001E;

    /* not clear what this means - listed in Microsoft Windows API */
    public static final short VK_MODECHANGE               = (short) 0x001F;

    /* replaced by VK_KANA_LOCK for Microsoft Windows and Solaris; 
       might still be used on other platforms */
    public static final short VK_KANA                     = (short) 0x0015;

    /* replaced by VK_INPUT_METHOD_ON_OFF for Microsoft Windows and Solaris; 
       might still be used for other platforms */
    public static final short VK_KANJI                    = (short) 0x0019;

    /**
     * Constant for the Alphanumeric function key.
     */
    /* Japanese PC 106 keyboard: eisuu */
    public static final short VK_ALPHANUMERIC             = (short) 0x00F0;

    /**
     * Constant for the Katakana function key.
     */
    /* Japanese PC 106 keyboard: katakana */
    public static final short VK_KATAKANA                 = (short) 0x00F1;

    /**
     * Constant for the Hiragana function key.
     */
    /* Japanese PC 106 keyboard: hiragana */
    public static final short VK_HIRAGANA                 = (short) 0x00F2;

    /**
     * Constant for the Full-Width Characters function key.
     */
    /* Japanese PC 106 keyboard: zenkaku */
    public static final short VK_FULL_WIDTH               = (short) 0x00F3;

    /**
     * Constant for the Half-Width Characters function key.
     */
    /* Japanese PC 106 keyboard: hankaku */
    public static final short VK_HALF_WIDTH               = (short) 0x00F4;

    /**
     * Constant for the Roman Characters function key.
     */
    /* Japanese PC 106 keyboard: roumaji */
    public static final short VK_ROMAN_CHARACTERS         = (short) 0x00F5;

    /**
     * Constant for the All Candidates function key.
     */
    /* Japanese PC 106 keyboard - VK_CONVERT + ALT: zenkouho */
    public static final short VK_ALL_CANDIDATES           = (short) 0x0100;

    /**
     * Constant for the Previous Candidate function key.
     */
    /* Japanese PC 106 keyboard - VK_CONVERT + SHIFT: maekouho */
    public static final short VK_PREVIOUS_CANDIDATE       = (short) 0x0101;

    /**
     * Constant for the Code Input function key.
     */
    /* Japanese PC 106 keyboard - VK_ALPHANUMERIC + ALT: kanji bangou */
    public static final short VK_CODE_INPUT               = (short) 0x0102;

    /**
     * Constant for the Japanese-Katakana function key.
     * This key switches to a Japanese input method and selects its Katakana input mode.
     */
    /* Japanese Macintosh keyboard - VK_JAPANESE_HIRAGANA + SHIFT */
    public static final short VK_JAPANESE_KATAKANA        = (short) 0x0103;

    /**
     * Constant for the Japanese-Hiragana function key.
     * This key switches to a Japanese input method and selects its Hiragana input mode.
     */
    /* Japanese Macintosh keyboard */
    public static final short VK_JAPANESE_HIRAGANA        = (short) 0x0104;

    /**
     * Constant for the Japanese-Roman function key.
     * This key switches to a Japanese input method and selects its Roman-Direct input mode.
     */
    /* Japanese Macintosh keyboard */
    public static final short VK_JAPANESE_ROMAN           = (short) 0x0105;

    /**
     * Constant for the locking Kana function key.
     * This key locks the keyboard into a Kana layout.
     */
    /* Japanese PC 106 keyboard with special Windows driver - eisuu + Control; Japanese Solaris keyboard: kana */
    public static final short VK_KANA_LOCK                = (short) 0x0106;

    /**
     * Constant for the input method on/off key.
     */
    /* Japanese PC 106 keyboard: kanji. Japanese Solaris keyboard: nihongo */
    public static final short VK_INPUT_METHOD_ON_OFF      = (short) 0x0107;

    /* for Sun keyboards */
    public static final short VK_CUT                      = (short) 0xFFD1;
    public static final short VK_COPY                     = (short) 0xFFCD;
    public static final short VK_PASTE                    = (short) 0xFFCF;
    public static final short VK_UNDO                     = (short) 0xFFCB;
    public static final short VK_AGAIN                    = (short) 0xFFC9;
    public static final short VK_FIND                     = (short) 0xFFD0;
    public static final short VK_PROPS                    = (short) 0xFFCA;
    public static final short VK_STOP                     = (short) 0xFFC8;

    /**
     * Constant for the Compose function key.
     */
    public static final short VK_COMPOSE                  = (short) 0xFF20;

    /**
     * Constant for the AltGraph function key.
     */
    public static final short VK_ALT_GRAPH                = (short) 0xFF7E;

    /**
     * Constant for the Begin key.
     */
    public static final short VK_BEGIN                    = (short) 0xFF58;

    /**
     * This value is used to indicate that the keyCode is unknown.
     * KEY_TYPED events do not have a keyCode value; this value 
     * is used instead.  
     */
    public static final short VK_UNDEFINED      = (short) 0x0;
}

