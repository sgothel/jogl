/**
 * Copyright 2013 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package com.jogamp.newt.event;

import com.jogamp.newt.event.KeyEvent;

public class UTFKeyUtil {
      
    //
    // UTF Key Constants
    //
    private static final char UTF_Equal                = '=';
    private static final char UTF_Minus                = '-';
    private static final char UTF_RightBracket         = ']';
    private static final char UTF_LeftBracket          = '[';
    private static final char UTF_Quote                = '\''; 
    private static final char UTF_Semicolon            = ';';
    private static final char UTF_Backslash            = '\\'; 
    private static final char UTF_Comma                = ',';
    private static final char UTF_Slash                = '/';
    private static final char UTF_Period               = '.';
    private static final char UTF_Grave                = '`'; // back quote
        
    /**
     * @param keyChar UTF16 value to map. Note: Lower case values are preferred.
     * @return {@link KeyEvent} virtual key (VK) value if possible,
     *         otherwise simply the UTF16 value of type short as a last resort.
     */
    public static short utf16ToVKey(char keyChar) {
        if( 'a' <= keyChar && keyChar <= 'z' ) {
            return (short) ( ( keyChar - 'a' ) + KeyEvent.VK_A );
        }
        if( '0' <= keyChar && keyChar <= '9' ) {
            return (short) ( ( keyChar - '0' ) + KeyEvent.VK_0 );
        }
        switch(keyChar) {
            //
            // KeyCodes (Layout Dependent)
            //
            case UTF_Equal:           return KeyEvent.VK_EQUALS;
            case UTF_Minus:           return KeyEvent.VK_MINUS;
            case UTF_RightBracket:    return KeyEvent.VK_CLOSE_BRACKET;
            case UTF_LeftBracket:     return KeyEvent.VK_OPEN_BRACKET;
            case UTF_Quote:           return KeyEvent.VK_QUOTE; 
            case UTF_Semicolon:       return KeyEvent.VK_SEMICOLON;
            case UTF_Backslash:       return KeyEvent.VK_BACK_SLASH; 
            case UTF_Comma:           return KeyEvent.VK_COMMA;
            case UTF_Slash:           return KeyEvent.VK_SLASH;
            case UTF_Period:          return KeyEvent.VK_PERIOD;
            case UTF_Grave:           return KeyEvent.VK_BACK_QUOTE; // KeyEvent.VK_DEAD_GRAVE
        }
        if( 'A' <= keyChar && keyChar <= 'Z' ) {
            return (short) ( ( keyChar - 'A' ) + KeyEvent.VK_A );
        }
        return (short) keyChar;
    }    
}
