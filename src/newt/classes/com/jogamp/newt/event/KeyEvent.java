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

/**
 * <a name="eventDelivery"><h5>KeyEvent Delivery</h5></a>
 *
 * Key events are delivered in the following order:
 * <p>
 * <table border="0">
 *   <tr><th>#</th><th>Event Type</th>      <th>Constraints</th>  <th>Notes</th></tr>
 *   <tr><td>1</td><td>{@link #EVENT_KEY_PRESSED}  </td><td> <i> excluding {@link #isAutoRepeat() auto-repeat}-{@link #isModifierKey() modifier} keys</i></td><td></td></tr>
 *   <tr><td>2</td><td>{@link #EVENT_KEY_RELEASED} </td><td> <i> excluding {@link #isAutoRepeat() auto-repeat}-{@link #isModifierKey() modifier} keys</i></td><td></td></tr>
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
    P = pressed, R = released
    0 = normal, 1 = auto-repeat

    P(0), [ R(1), P(1), R(1), ..], R(0)
 * </pre>
 * The idea is if you mask out auto-repeat in your event listener
 * you just get one long pressed P/R tuple for {@link #isPrintableKey() printable} and {@link #isActionKey() Action} keys.
 * </p>
 * <p>
 * {@link #isActionKey() Action} keys will produce {@link #EVENT_KEY_PRESSED pressed}
 * and {@link #EVENT_KEY_RELEASED released} events including {@link #isAutoRepeat() auto-repeat}.
 * </p>
 * <p>
 * {@link #isPrintableKey() Printable} keys will produce {@link #EVENT_KEY_PRESSED pressed} and {@link #EVENT_KEY_RELEASED released} events.
 * </p>
 * <p>
 * {@link #isModifierKey() Modifier} keys will produce {@link #EVENT_KEY_PRESSED pressed} and {@link #EVENT_KEY_RELEASED released} events
 * excluding {@link #isAutoRepeat() auto-repeat}.
 * They will also influence subsequent event's {@link #getModifiers() modifier} bits while pressed.
 * </p>
 *
 * <a name="unicodeMapping"><h5>Unicode Mapping</h5></a>
 * <p>
 * {@link #getKeyChar() Key-chars}, as well as
 * {@link #isPrintableKey() printable} {@link #getKeyCode() key-codes} and {@link #getKeySymbol() key-symbols}
 * use the UTF-16 unicode space w/o collision.
 *
 * </p>
 * <p>
 * Non-{@link #isPrintableKey() printable} {@link #getKeyCode() key-codes} and {@link #getKeySymbol() key-symbols},
 * i.e. {@link #isModifierKey() modifier-} and {@link #isActionKey() action-}keys,
 * are mapped to unicode's control and private range and do not collide w/ {@link #isPrintableKey() printable} unicode values
 * with the following exception.
 * </p>
 *
 * <a name="unicodeCollision"><h5>Unicode Collision</h5></a>
 * <p>
 * The following {@link #getKeyCode() Key-code}s and {@link #getKeySymbol() key-symbol}s collide w/ unicode space:<br/>
 * <table border="1">
 *   <tr><th>unicode range</th>    <th>virtual key code</th>                            <th>unicode character</th></tr>
 *   <tr><td>[0x61 .. 0x78]</td>   <td>[{@link #VK_F1}..{@link #VK_F24}]</td>           <td>['a'..'x']</td></tr>
 * </table>
 * </p>
 * <p>
 * Collision was chosen for {@link #getKeyCode() Key-code} and {@link #getKeySymbol() key-symbol} mapping
 * to allow a minimal code range, i.e. <code>[0..255]</code>.
 * The reduced code range in turn allows the implementation to utilize fast and small lookup tables,
 * e.g. to implement a key-press state tracker.
 * </p>
 * <pre>
 * http://www.utf8-chartable.de/unicode-utf8-table.pl
 * http://www.unicode.org/Public/5.1.0/ucd/PropList.txt
 * https://en.wikipedia.org/wiki/Mapping_of_Unicode_characters
 * https://en.wikipedia.org/wiki/Unicode_control_characters
 * https://en.wikipedia.org/wiki/Private_Use_%28Unicode%29#Private_Use_Areas
 * </pre>
 * </p>
 */
@SuppressWarnings("serial")
public class KeyEvent extends InputEvent
{
    private KeyEvent(final short eventType, final Object source, final long when, final int modifiers, final short keyCode, final short keySym, final int keySymModMask, final char keyChar) {
        super(eventType, source, when, modifiers | keySymModMask);
        this.keyCode=keyCode;
        this.keySym=keySym;
        this.keyChar=keyChar;
        { // cache modifier and action flags
            byte _flags = 0;
            if( isPrintableKey(keySym, false) && isPrintableKey((short)keyChar, true) ) {
                _flags |= F_PRINTABLE_MASK;
            } else {
                if( 0 != keySymModMask ) {
                    _flags |= F_MODIFIER_MASK;
                } else {
                    // A = U - ( P + M )
                    _flags |= F_ACTION_MASK;
                }
            }
            flags = _flags;

            //
            // Validate flags
            //
            final int pma_bits = flags & ( F_PRINTABLE_MASK | F_MODIFIER_MASK | F_ACTION_MASK ) ;
            final int pma_count = Bitfield.Util.bitCount(pma_bits);
            if ( 1 != pma_count ) {
                throw new InternalError("Key must be either of type printable, modifier or action - but it is of "+pma_count+" types: "+this);
            }
        }
    }

    public static KeyEvent create(final short eventType, final Object source, final long when, final int modifiers, final short keyCode, final short keySym, final char keyChar) {
        return new KeyEvent(eventType, source, when, modifiers, keyCode, keySym, getModifierMask(keySym), keyChar);
    }

    /**
     * Returns the <i>UTF-16</i> character reflecting the {@link #getKeySymbol() key symbol}
     * incl. active {@link #isModifierKey() modifiers}.
     * @see #getKeySymbol()
     * @see #getKeyCode()
     */
    public final char getKeyChar() {
        return keyChar;
    }

    /**
     * Returns the virtual <i>key symbol</i> reflecting the current <i>keyboard layout</i>.
     * <p>
     * For {@link #isPrintableKey() printable keys}, the <i>key symbol</i> is the {@link #isModifierKey() unmodified}
     * representation of the UTF-16 {@link #getKeyChar() key char}.<br/>
     * E.g. symbol [{@link #VK_A}, 'A'] for char 'a'.
     * </p>
     * @see #isPrintableKey()
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
     * @see #getKeyChar()
     * @see #getKeySymbol()
     */
    public final short getKeyCode() {
        return keyCode;
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
        sb.append("KeyEvent[").append(getEventTypeString(getEventType())).append(", code ").append(toHexString(keyCode)).append(", sym ").append(toHexString(keySym)).append(", char '").append(keyChar).append("' (").append(toHexString((short)keyChar))
        .append("), printable ").append(isPrintableKey()).append(", modifier ").append(isModifierKey()).append(", action ").append(isActionKey()).append(", ");
        return super.toString(sb).append("]");
    }

    public static String getEventTypeString(final short type) {
        switch(type) {
        case EVENT_KEY_PRESSED: return "EVENT_KEY_PRESSED";
        case EVENT_KEY_RELEASED: return "EVENT_KEY_RELEASED";
        default: return "unknown (" + type + ")";
        }
    }

    /**
     * @param keyChar UTF16 value to map. It is expected that the incoming keyChar value is unshifted and unmodified,
     *        however, lower case a-z is mapped to {@link KeyEvent#VK_A} - {@link KeyEvent#VK_Z}.
     * @return {@link KeyEvent} virtual key (VK) value.
     */
    public static short utf16ToVKey(final char keyChar) {
        if( 'a' <= keyChar && keyChar <= 'z' ) {
            return (short) ( ( keyChar - 'a' ) + KeyEvent.VK_A );
        }
        return (short) keyChar;
    }

    /**
     * Returns <code>true</code> if the given <code>virtualKey</code> represents a modifier key, otherwise <code>false</code>.
     * <p>
     * A modifier key is one of {@link #VK_SHIFT}, {@link #VK_CONTROL}, {@link #VK_ALT}, {@link #VK_ALT_GRAPH}, {@link #VK_META}.
     * </p>
     */
    public static boolean isModifierKey(final short vKey) {
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
     * If <code>vKey</code> is a {@link #isModifierKey() modifier key}, method returns the corresponding modifier mask,
     * otherwise 0.
     */
    public static int getModifierMask(final short vKey) {
        switch (vKey) {
            case VK_SHIFT:
                return InputEvent.SHIFT_MASK;
            case VK_CONTROL:
                return InputEvent.CTRL_MASK;
            case VK_ALT:
            case VK_ALT_GRAPH:
                return InputEvent.ALT_MASK;
            case VK_META:
                return InputEvent.META_MASK;
        }
        return 0;
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
     * Returns <code>true</code> if {@link #getKeySymbol() key symbol} represents a non-printable and
     * non-{@link #isModifierKey(short) modifier} action key, otherwise <code>false</code>.
     * <p>
     * Hence it is the set A of all keys U w/o printable P and w/o modifiers M:
     * <code> A = U - ( P + M ) </code>
     * </p>
     * @see #isPrintableKey()
     * @see #isModifierKey()
     */
    public final boolean isActionKey() {
        return 0 != ( F_ACTION_MASK & flags ) ;
    }

    /**
     * Returns <code>true</code> if given <code>uniChar</code> represents a printable character,
     * i.e. a value other than {@link #VK_UNDEFINED} and not a control or non-printable private code.
     * <p>
     * A printable character is neither a {@link #isModifierKey(short) modifier key}, nor an {@link #isActionKey(short) action key}.
     * </p>
     * <p>
     * Otherwise returns <code>false</code>.
     * </p>
     * <p>
     * Distinction of key character and virtual key code is made due to <a href="#unicodeCollision">unicode collision</a>.
     * </p>
     *
     * @param uniChar the UTF-16 unicode value, which maybe a virtual key code or key character.
     * @param isKeyChar true if <code>uniChar</code> is a key character, otherwise a virtual key code
     */
    public static boolean isPrintableKey(final short uniChar, final boolean isKeyChar) {
        if ( VK_BACK_SPACE == uniChar || VK_TAB == uniChar || VK_ENTER == uniChar ) {
            return true;
        }
        if( !isKeyChar ) {
            if( ( nonPrintableKeys[0].min <= uniChar && uniChar <= nonPrintableKeys[0].max ) ||
                ( nonPrintableKeys[1].min <= uniChar && uniChar <= nonPrintableKeys[1].max ) ||
                ( nonPrintableKeys[2].min <= uniChar && uniChar <= nonPrintableKeys[2].max ) ||
                ( nonPrintableKeys[3].min <= uniChar && uniChar <= nonPrintableKeys[3].max ) ) {
                return false;
            }
        } else {
            if( ( nonPrintableKeys[0].inclKeyChar && nonPrintableKeys[0].min <= uniChar && uniChar <= nonPrintableKeys[0].max ) ||
                ( nonPrintableKeys[1].inclKeyChar && nonPrintableKeys[1].min <= uniChar && uniChar <= nonPrintableKeys[1].max ) ||
                ( nonPrintableKeys[2].inclKeyChar && nonPrintableKeys[2].min <= uniChar && uniChar <= nonPrintableKeys[2].max ) ||
                ( nonPrintableKeys[3].inclKeyChar && nonPrintableKeys[3].min <= uniChar && uniChar <= nonPrintableKeys[3].max ) ) {
                return false;
            }
        }
        return VK_UNDEFINED != uniChar;
    }

    /**
     * Returns <code>true</code> if {@link #getKeySymbol() key symbol} and {@link #getKeyChar() key char}
     * represents a printable character, i.e. a value other than {@link #VK_UNDEFINED}
     * and not a control or non-printable private code.
     * <p>
     * A printable character is neither a {@link #isModifierKey(short) modifier key}, nor an {@link #isActionKey(short) action key}.
     * </p>
     * <p>
     * Otherwise returns <code>false</code>.
     * </p>
     */
    public final boolean isPrintableKey() {
        return 0 != ( F_PRINTABLE_MASK & flags ) ;
    }

    private final short keyCode;
    private final short keySym;
    private final char keyChar;
    private final byte flags;
    private static final byte F_MODIFIER_MASK   = 1 << 0;
    private static final byte F_ACTION_MASK     = 1 << 1;
    private static final byte F_PRINTABLE_MASK  = 1 << 2;

    /** A key has been pressed, excluding {@link #isAutoRepeat() auto-repeat}-{@link #isModifierKey() modifier} keys. */
    public static final short EVENT_KEY_PRESSED = 300;
    /** A key has been released, excluding {@link #isAutoRepeat() auto-repeat}-{@link #isModifierKey() modifier} keys. */
    public static final short EVENT_KEY_RELEASED= 301;

    /**
     * This value, {@code '\0'}, is used to indicate that the keyChar is unknown or not printable.
     */
    public static final char  NULL_CHAR                   = '\0';

    /* Virtual key codes. */

    public static class NonPrintableRange {
        /** min. unicode value, inclusive */
        public short min;
        /** max. unicode value, inclusive */
        public short max;
        /** true if valid for keyChar values as well, otherwise only valid for keyCode and keySym due to collision. */
        public final boolean inclKeyChar;
        private NonPrintableRange(final short min, final short max, final boolean inclKeyChar) {
            this.min = min;
            this.max = max;
            this.inclKeyChar = inclKeyChar;
        }
    };
    /**
     * Non printable key ranges, currently fixed to an array of size 4.
     * <p>
     * Not included, queried upfront:
     * <ul>
     *  <li>{@link #VK_BACK_SPACE}</li>
     *  <li>{@link #VK_TAB}</li>
     *  <li>{@link #VK_ENTER}</li>
     * </ul>
     * </p>
     */
    public final static NonPrintableRange[] nonPrintableKeys = {
        new NonPrintableRange( (short)0x0000, (short)0x001F, true ),  // Unicode: Non printable controls: [0x00 - 0x1F], see exclusion above
        new NonPrintableRange( (short)0x0061, (short)0x0078, false),  // Small 'a' thru 'z' (0x61 - 0x7a) - Not used for keyCode / keySym - Re-used for Fn (collision)
        new NonPrintableRange( (short)0x008F, (short)0x009F, true ),  // Unicode: Non printable controls: [0x7F - 0x9F], Numpad keys [0x7F - 0x8E] are printable!
        new NonPrintableRange( (short)0xE000, (short)0xF8FF, true )   // Unicode: Private 0xE000 - 0xF8FF (Marked Non-Printable)
    };

    //
    // Unicode: Non printable controls: [0x00 - 0x1F]
    //

    /**
     * This value, {@value}, is used to indicate that the keyCode is unknown.
     */
    public static final short VK_UNDEFINED      = (short) 0x0;

           static final short VK_FREE01         = (short) 0x01;

    /** Constant for the HOME function key. ASCII: Start Of Text. */
    public static final short VK_HOME           = (short) 0x02;

    /** Constant for the END function key. ASCII: End Of Text. */
    public static final short VK_END            = (short) 0x03;

    /** Constant for the END function key. ASCII: End Of Transmission. */
    public static final short VK_FINAL          = (short) 0x04;

    /** Constant for the PRINT function key. ASCII: Enquiry. */
    public static final short VK_PRINTSCREEN    = (short) 0x05;

           static final short VK_FREE06         = (short) 0x06;
           static final short VK_FREE07         = (short) 0x07;

    /** Constant for the BACK SPACE key "\b", matching ASCII. Printable! */
    public static final short VK_BACK_SPACE     = (short) 0x08;

    /** Constant for the HORIZ TAB key "\t", matching ASCII. Printable! */
    public static final short VK_TAB            = (short) 0x09;

           /** LINE_FEED "\n", matching ASCII, n/a on keyboard. */
           static final short VK_FREE0A         = (short) 0x0A;

    /** Constant for the PAGE DOWN function key. ASCII: Vertical Tabulation. */
    public static final short VK_PAGE_DOWN      = (short) 0x0B;

    /** Constant for the CLEAR key, i.e. FORM FEED, matching ASCII. */
    public static final short VK_CLEAR          = (short) 0x0C;

    /** Constant for the ENTER key, i.e. CARRIAGE RETURN, matching ASCII. Printable! */
    public static final short VK_ENTER          = (short) 0x0D;

           static final short VK_FREE0E         = (short) 0x0E;

    /** Constant for the CTRL function key. ASCII: shift-in. */
    public static final short VK_SHIFT          = (short) 0x0F;

    /** Constant for the PAGE UP function key. ASCII: Data Link Escape. */
    public static final short VK_PAGE_UP        = (short) 0x10;

    /** Constant for the CTRL function key. ASCII: device-ctrl-one. */
    public static final short VK_CONTROL        = (short) 0x11;

    /** Constant for the left ALT function key. ASCII: device-ctrl-two. */
    public static final short VK_ALT            = (short) 0x12;

    /** Constant for the ALT_GRAPH function key, i.e. right ALT key. ASCII: device-ctrl-three. */
    public static final short VK_ALT_GRAPH      = (short) 0x13;

    /** Constant for the CAPS LOCK function key. ASCII: device-ctrl-four. */
    public static final short VK_CAPS_LOCK      = (short) 0x14;

           static final short VK_FREE15         = (short) 0x15;

    /** Constant for the PAUSE function key. ASCII: sync-idle. */
    public static final short VK_PAUSE          = (short) 0x16;

    /** <b>scroll lock</b> key. ASCII: End Of Transmission Block. */
    public static final short VK_SCROLL_LOCK    = (short) 0x17;

    /** Constant for the CANCEL function key. ASCII: Cancel. */
    public static final short VK_CANCEL         = (short) 0x18;

           static final short VK_FREE19         = (short) 0x19;

    /** Constant for the INSERT function key. ASCII: Substitute. */
    public static final short VK_INSERT         = (short) 0x1A;

    /** Constant for the ESCAPE function key. ASCII: Escape. */
    public static final short VK_ESCAPE         = (short) 0x1B;

    /** Constant for the Convert function key, Japanese "henkan". ASCII: File Separator. */
    public static final short VK_CONVERT        = (short) 0x1C;

    /** Constant for the Don't Convert function key, Japanese "muhenkan". ASCII: Group Separator.*/
    public static final short VK_NONCONVERT     = (short) 0x1D;

    /** Constant for the Accept or Commit function key, Japanese "kakutei". ASCII: Record Separator.*/
    public static final short VK_ACCEPT         = (short) 0x1E;

    /** Constant for the Mode Change (?). ASCII: Unit Separator.*/
    public static final short VK_MODECHANGE     = (short) 0x1F;

    //
    // Unicode: Printable [0x20 - 0x7E]
    // NOTE: Collision of 'a' - 'x' [0x61 .. 0x78], used for keyCode/keySym Fn function keys
    //

    /** Constant for the SPACE function key. ASCII: SPACE. */
    public static final short VK_SPACE          = (short) 0x20;

    /** Constant for the "!" key. */
    public static final short VK_EXCLAMATION_MARK = (short) 0x21;

    /** Constant for the """ key. */
    public static final short VK_QUOTEDBL       = (short) 0x22;

    /** Constant for the "#" key. */
    public static final short VK_NUMBER_SIGN    = (short) 0x23;

    /** Constant for the "$" key. */
    public static final short VK_DOLLAR         = (short) 0x24;

    /** Constant for the "%" key. */
    public static final short VK_PERCENT        = (short) 0x25;

    /** Constant for the "&" key. */
    public static final short VK_AMPERSAND      = (short) 0x26;

    /** Constant for the "'" key. */
    public static final short VK_QUOTE          = (short) 0x27;

    /** Constant for the "(" key. */
    public static final short VK_LEFT_PARENTHESIS  = (short) 0x28;

    /** Constant for the ")" key. */
    public static final short VK_RIGHT_PARENTHESIS = (short) 0x29;

    /** Constant for the "*" key */
    public static final short VK_ASTERISK       = (short) 0x2A;

    /** Constant for the "+" key. */
    public static final short VK_PLUS           = (short) 0x2B;

    /** Constant for the comma key, "," */
    public static final short VK_COMMA          = (short) 0x2C;

    /** Constant for the minus key, "-" */
    public static final short VK_MINUS          = (short) 0x2D;

    /** Constant for the period key, "." */
    public static final short VK_PERIOD         = (short) 0x2E;

    /** Constant for the forward slash key, "/" */
    public static final short VK_SLASH          = (short) 0x2F;

    /** VK_0 thru VK_9 are the same as UTF16/ASCII '0' thru '9' [0x30 - 0x39] */
    public static final short VK_0              = (short) 0x30;
    /** See {@link #VK_0}. */
    public static final short VK_1              = (short) 0x31;
    /** See {@link #VK_0}. */
    public static final short VK_2              = (short) 0x32;
    /** See {@link #VK_0}. */
    public static final short VK_3              = (short) 0x33;
    /** See {@link #VK_0}. */
    public static final short VK_4              = (short) 0x34;
    /** See {@link #VK_0}. */
    public static final short VK_5              = (short) 0x35;
    /** See {@link #VK_0}. */
    public static final short VK_6              = (short) 0x36;
    /** See {@link #VK_0}. */
    public static final short VK_7              = (short) 0x37;
    /** See {@link #VK_0}. */
    public static final short VK_8              = (short) 0x38;
    /** See {@link #VK_0}. */
    public static final short VK_9              = (short) 0x39;

    /** Constant for the ":" key. */
    public static final short VK_COLON          = (short) 0x3A;

    /** Constant for the semicolon key, ";" */
    public static final short VK_SEMICOLON      = (short) 0x3B;

    /** Constant for the equals key, "<" */
    public static final short VK_LESS           = (short) 0x3C;

    /** Constant for the equals key, "=" */
    public static final short VK_EQUALS         = (short) 0x3D;

    /** Constant for the equals key, ">" */
    public static final short VK_GREATER        = (short) 0x3E;

    /** Constant for the equals key, "?" */
    public static final short VK_QUESTIONMARK   = (short) 0x3F;

    /** Constant for the equals key, "@" */
    public static final short VK_AT             = (short) 0x40;

    /** VK_A thru VK_Z are the same as Capital UTF16/ASCII 'A' thru 'Z' (0x41 - 0x5A) */
    public static final short VK_A              = (short) 0x41;
    /** See {@link #VK_A}. */
    public static final short VK_B              = (short) 0x42;
    /** See {@link #VK_A}. */
    public static final short VK_C              = (short) 0x43;
    /** See {@link #VK_A}. */
    public static final short VK_D              = (short) 0x44;
    /** See {@link #VK_A}. */
    public static final short VK_E              = (short) 0x45;
    /** See {@link #VK_A}. */
    public static final short VK_F              = (short) 0x46;
    /** See {@link #VK_A}. */
    public static final short VK_G              = (short) 0x47;
    /** See {@link #VK_A}. */
    public static final short VK_H              = (short) 0x48;
    /** See {@link #VK_A}. */
    public static final short VK_I              = (short) 0x49;
    /** See {@link #VK_A}. */
    public static final short VK_J              = (short) 0x4A;
    /** See {@link #VK_A}. */
    public static final short VK_K              = (short) 0x4B;
    /** See {@link #VK_A}. */
    public static final short VK_L              = (short) 0x4C;
    /** See {@link #VK_A}. */
    public static final short VK_M              = (short) 0x4D;
    /** See {@link #VK_A}. */
    public static final short VK_N              = (short) 0x4E;
    /** See {@link #VK_A}. */
    public static final short VK_O              = (short) 0x4F;
    /** See {@link #VK_A}. */
    public static final short VK_P              = (short) 0x50;
    /** See {@link #VK_A}. */
    public static final short VK_Q              = (short) 0x51;
    /** See {@link #VK_A}. */
    public static final short VK_R              = (short) 0x52;
    /** See {@link #VK_A}. */
    public static final short VK_S              = (short) 0x53;
    /** See {@link #VK_A}. */
    public static final short VK_T              = (short) 0x54;
    /** See {@link #VK_A}. */
    public static final short VK_U              = (short) 0x55;
    /** See {@link #VK_A}. */
    public static final short VK_V              = (short) 0x56;
    /** See {@link #VK_A}. */
    public static final short VK_W              = (short) 0x57;
    /** See {@link #VK_A}. */
    public static final short VK_X              = (short) 0x58;
    /** See {@link #VK_A}. */
    public static final short VK_Y              = (short) 0x59;
    /** See {@link #VK_A}. */
    public static final short VK_Z              = (short) 0x5A;

    /** Constant for the open bracket key, "[" */
    public static final short VK_OPEN_BRACKET   = (short) 0x5B;

    /**Constant for the back slash key, "\" */
    public static final short VK_BACK_SLASH     = (short) 0x5C;

    /** Constant for the close bracket key, "]" */
    public static final short VK_CLOSE_BRACKET  = (short) 0x5D;

    /** Constant for the "^" key. */
    public static final short VK_CIRCUMFLEX     = (short) 0x5E;

    /** Constant for the "_" key */
    public static final short VK_UNDERSCORE     = (short) 0x5F;

    /** Constant for the "`" key */
    public static final short VK_BACK_QUOTE     = (short) 0x60;

    /** Small UTF/ASCII 'a' thru 'z' (0x61 - 0x7a) - Not used for keyCode / keySym. */

    /**
     * Constant for the F<i>n</i> function keys.
     * <p>
     * F1..F24, i.e. F<i>n</i>, are mapped from on <code>0x60+n</code> -> <code>[0x61 .. 0x78]</code>.
     * </p>
     * <p>
     * <b>Warning:</b> The F<i>n</i> function keys <b>do collide</b> with unicode characters small 'a' thru 'x'!<br/>
     * See <a href="#unicodeCollision">Unicode Collision</a> for details.
     * </p>
     */
    public static final short VK_F1             = (short) ( 0x60+ 1 );

    /** Constant for the F2 function key. See {@link #VK_F1}. */
    public static final short VK_F2             = (short) ( 0x60+ 2 );

    /** Constant for the F3 function key. See {@link #VK_F1}. */
    public static final short VK_F3             = (short) ( 0x60+ 3 );

    /** Constant for the F4 function key. See {@link #VK_F1}. */
    public static final short VK_F4             = (short) ( 0x60+ 4 );

    /** Constant for the F5 function key. See {@link #VK_F1}. */
    public static final short VK_F5             = (short) ( 0x60+ 5 );

    /** Constant for the F6 function key. See {@link #VK_F1}. */
    public static final short VK_F6             = (short) ( 0x60+ 6 );

    /** Constant for the F7 function key. See {@link #VK_F1}. */
    public static final short VK_F7             = (short) ( 0x60+ 7 );

    /** Constant for the F8 function key. See {@link #VK_F1}. */
    public static final short VK_F8             = (short) ( 0x60+ 8 );

    /** Constant for the F9 function key. See {@link #VK_F1}. */
    public static final short VK_F9             = (short) ( 0x60+ 9 );

    /** Constant for the F11 function key. See {@link #VK_F1}. */
    public static final short VK_F10            = (short) ( 0x60+10 );

    /** Constant for the F11 function key. See {@link #VK_F1}. */
    public static final short VK_F11            = (short) ( 0x60+11 );

    /** Constant for the F12 function key. See {@link #VK_F1}.*/
    public static final short VK_F12            = (short) ( 0x60+12 );

    /** Constant for the F13 function key. See {@link #VK_F1}. */
    public static final short VK_F13            = (short) ( 0x60+13 );

    /** Constant for the F14 function key. See {@link #VK_F1}. */
    public static final short VK_F14            = (short) ( 0x60+14 );

    /** Constant for the F15 function key. See {@link #VK_F1}. */
    public static final short VK_F15            = (short) ( 0x60+15 );

    /** Constant for the F16 function key. See {@link #VK_F1}. */
    public static final short VK_F16            = (short) ( 0x60+16 );

    /** Constant for the F17 function key. See {@link #VK_F1}. */
    public static final short VK_F17            = (short) ( 0x60+17 );

    /** Constant for the F18 function key. See {@link #VK_F1}. */
    public static final short VK_F18            = (short) ( 0x60+18 );

    /** Constant for the F19 function key. See {@link #VK_F1}. */
    public static final short VK_F19            = (short) ( 0x60+19 );

    /** Constant for the F20 function key. See {@link #VK_F1}. */
    public static final short VK_F20            = (short) ( 0x60+20 );

    /** Constant for the F21 function key. See {@link #VK_F1}. */
    public static final short VK_F21            = (short) ( 0x60+21 );

    /** Constant for the F22 function key. See {@link #VK_F1}. */
    public static final short VK_F22            = (short) ( 0x60+22 );

    /** Constant for the F23 function key. See {@link #VK_F1}. */
    public static final short VK_F23            = (short) ( 0x60+23 );

    /** Constant for the F24 function key. See {@link #VK_F1}. */
    public static final short VK_F24            = (short) ( 0x60+24 );


    /** Constant for the "{" key */
    public static final short VK_LEFT_BRACE     = (short) 0x7B;
    /** Constant for the "|" key */
    public static final short VK_PIPE           = (short) 0x7C;
    /** Constant for the "}" key */
    public static final short VK_RIGHT_BRACE    = (short) 0x7D;

    /** Constant for the "~" key, matching ASCII */
    public static final short VK_TILDE          = (short) 0x7E;

    //
    // Unicode: Non printable controls: [0x7F - 0x9F]
    //
    // Numpad keys [0x7F - 0x8E] are printable
    //

    /** Numeric keypad <b>decimal separator</b> key. Non printable UTF control. */
    public static final short VK_SEPARATOR      = (short) 0x7F;

    /** Numeric keypad VK_NUMPAD0 thru VK_NUMPAD9 are mapped to UTF control (0x80 - 0x89). Non printable UTF control. */
    public static final short VK_NUMPAD0        = (short) 0x80;
    /** See {@link #VK_NUMPAD0}. */
    public static final short VK_NUMPAD1        = (short) 0x81;
    /** See {@link #VK_NUMPAD0}. */
    public static final short VK_NUMPAD2        = (short) 0x82;
    /** See {@link #VK_NUMPAD0}. */
    public static final short VK_NUMPAD3        = (short) 0x83;
    /** See {@link #VK_NUMPAD0}. */
    public static final short VK_NUMPAD4        = (short) 0x84;
    /** See {@link #VK_NUMPAD0}. */
    public static final short VK_NUMPAD5        = (short) 0x85;
    /** See {@link #VK_NUMPAD0}. */
    public static final short VK_NUMPAD6        = (short) 0x86;
    /** See {@link #VK_NUMPAD0}. */
    public static final short VK_NUMPAD7        = (short) 0x87;
    /** See {@link #VK_NUMPAD0}. */
    public static final short VK_NUMPAD8        = (short) 0x88;
    /** See {@link #VK_NUMPAD0}. */
    public static final short VK_NUMPAD9        = (short) 0x89;

    /** Numeric keypad <b>decimal separator</b> key. Non printable UTF control. */
    public static final short VK_DECIMAL        = (short) 0x8A;

    /** Numeric keypad <b>add</b> key. Non printable UTF control. */
    public static final short VK_ADD            = (short) 0x8B;

    /** Numeric keypad <b>subtract</b> key. Non printable UTF control. */
    public static final short VK_SUBTRACT       = (short) 0x8C;

    /** Numeric keypad <b>multiply</b> key. Non printable UTF control. */
    public static final short VK_MULTIPLY       = (short) 0x8D;

    /** Numeric keypad <b>divide</b> key. Non printable UTF control. */
    public static final short VK_DIVIDE         = (short) 0x8E;

    /** Constant for the DEL key, matching ASCII. Non printable UTF control. */
    public static final short VK_DELETE         = (short) 0x93;

    /** Numeric keypad <b>num lock</b> key. Non printable UTF control. */
    public static final short VK_NUM_LOCK       = (short) 0x94;

    /** Constant for the cursor- or numerical-pad <b>left</b> arrow key. Non printable UTF control. */
    public static final short VK_LEFT           = (short) 0x95;

    /** Constant for the cursor- or numerical-pad <b>up</b> arrow key. Non printable UTF control. */
    public static final short VK_UP             = (short) 0x96;

    /** Constant for the cursor- or numerical-pad <b>right</b> arrow key. Non printable UTF control. */
    public static final short VK_RIGHT          = (short) 0x97;

    /** Constant for the cursor- or numerical pad <b>down</b> arrow key. Non printable UTF control. */
    public static final short VK_DOWN           = (short) 0x98;

    /** Constant for the Context Menu key. Non printable UTF control. */
    public static final short VK_CONTEXT_MENU   = (short) 0x99;

    /**
     * Constant for the MS "Windows" function key.
     * It is used for both the left and right version of the key.
     */
    public static final short VK_WINDOWS        = (short) 0x9A;

    /** Constant for the Meta function key. */
    public static final short VK_META           = (short) 0x9B;

    /** Constant for the Help function key. */
    public static final short VK_HELP           = (short) 0x9C;

    /** Constant for the Compose function key. */
    public static final short VK_COMPOSE        = (short) 0x9D;

    /** Constant for the Begin function key. */
    public static final short VK_BEGIN          = (short) 0x9E;

    /** Constant for the Stop function key. */
    public static final short VK_STOP           = (short) 0x9F;

    //
    // Unicode: Printable [0x00A0 - 0xDFFF]
    //

    /** Constant for the inverted exclamation mark key. */
    public static final short VK_INVERTED_EXCLAMATION_MARK = (short) 0xA1;

    /** Constant for the Euro currency sign key. */
    public static final short VK_EURO_SIGN                = (short) 0x20AC;

    //
    // Unicode: Private 0xE000 - 0xF8FF (Marked Non-Printable)
    //

    /* for Sun keyboards */
    public static final short VK_CUT            = (short) 0xF879;
    public static final short VK_COPY           = (short) 0xF87A;
    public static final short VK_PASTE          = (short) 0xF87B;
    public static final short VK_UNDO           = (short) 0xF87C;
    public static final short VK_AGAIN          = (short) 0xF87D;
    public static final short VK_FIND           = (short) 0xF87E;
    public static final short VK_PROPS          = (short) 0xF87F;

    /* for input method support on Asian Keyboards */

    /**
     * Constant for the input method on/off key.
     */
    /* Japanese PC 106 keyboard: kanji. Japanese Solaris keyboard: nihongo */
    public static final short VK_INPUT_METHOD_ON_OFF = (short) 0xF890;

    /**
     * Constant for the Code Input function key.
     */
    /* Japanese PC 106 keyboard - VK_ALPHANUMERIC + ALT: kanji bangou */
    public static final short VK_CODE_INPUT = (short) 0xF891;

    /**
     * Constant for the Roman Characters function key.
     */
    /* Japanese PC 106 keyboard: roumaji */
    public static final short VK_ROMAN_CHARACTERS = (short) 0xF892;

    /**
     * Constant for the All Candidates function key.
     */
    /* Japanese PC 106 keyboard - VK_CONVERT + ALT: zenkouho */
    public static final short VK_ALL_CANDIDATES = (short) 0xF893;

    /**
     * Constant for the Previous Candidate function key.
     */
    /* Japanese PC 106 keyboard - VK_CONVERT + SHIFT: maekouho */
    public static final short VK_PREVIOUS_CANDIDATE = (short) 0xF894;

    /**
     * Constant for the Alphanumeric function key.
     */
    /* Japanese PC 106 keyboard: eisuu */
    public static final short VK_ALPHANUMERIC   = (short) 0xF895;

    /**
     * Constant for the Katakana function key.
     */
    /* Japanese PC 106 keyboard: katakana */
    public static final short VK_KATAKANA       = (short) 0xF896;

    /**
     * Constant for the Hiragana function key.
     */
    /* Japanese PC 106 keyboard: hiragana */
    public static final short VK_HIRAGANA       = (short) 0xF897;

    /**
     * Constant for the Full-Width Characters function key.
     */
    /* Japanese PC 106 keyboard: zenkaku */
    public static final short VK_FULL_WIDTH     = (short) 0xF898;

    /**
     * Constant for the Half-Width Characters function key.
     */
    /* Japanese PC 106 keyboard: hankaku */
    public static final short VK_HALF_WIDTH     = (short) 0xF89A;

    /**
     * Constant for the Japanese-Katakana function key.
     * This key switches to a Japanese input method and selects its Katakana input mode.
     */
    /* Japanese Macintosh keyboard - VK_JAPANESE_HIRAGANA + SHIFT */
    public static final short VK_JAPANESE_KATAKANA = (short) 0xF89B;

    /**
     * Constant for the Japanese-Hiragana function key.
     * This key switches to a Japanese input method and selects its Hiragana input mode.
     */
    /* Japanese Macintosh keyboard */
    public static final short VK_JAPANESE_HIRAGANA = (short) 0xF89C;

    /**
     * Constant for the Japanese-Roman function key.
     * This key switches to a Japanese input method and selects its Roman-Direct input mode.
     */
    /* Japanese Macintosh keyboard */
    public static final short VK_JAPANESE_ROMAN = (short) 0xF89D;

    /**
     * Constant for the locking Kana function key.
     * This key locks the keyboard into a Kana layout.
     */
    /* Japanese PC 106 keyboard with special Windows driver - eisuu + Control; Japanese Solaris keyboard: kana */
    public static final short VK_KANA_LOCK = (short) 0xF89F;

    /**
     * Constant for Keyboard became invisible, e.g. Android's soft keyboard Back button hit while keyboard is visible.
     */
    public static final short VK_KEYBOARD_INVISIBLE = (short) 0xF8FF;

}

