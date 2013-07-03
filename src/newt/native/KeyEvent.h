/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

#ifndef _KEY_EVENT_H_
#define _KEY_EVENT_H_

#define EVENT_KEY_PRESSED  300
#define EVENT_KEY_RELEASED 301

#define J_VK_UNDEFINED      ( 0x0U )
#define J_VK_HOME           ( 0x02U )
#define J_VK_END            ( 0x03U )
#define J_VK_FINAL          ( 0x04U )
#define J_VK_PRINTSCREEN    ( 0x05U )
#define J_VK_BACK_SPACE     ( 0x08U )
#define J_VK_TAB            ( 0x09U )
#define J_VK_PAGE_DOWN      ( 0x0BU )
#define J_VK_CLEAR          ( 0x0CU )
#define J_VK_ENTER          ( 0x0DU )
#define J_VK_SHIFT          ( 0x0FU )
#define J_VK_PAGE_UP        ( 0x10U )
#define J_VK_CONTROL        ( 0x11U )
#define J_VK_ALT            ( 0x12U )
#define J_VK_ALT_GRAPH      ( 0x13U )
#define J_VK_CAPS_LOCK      ( 0x14U )
#define J_VK_PAUSE          ( 0x16U )
#define J_VK_SCROLL_LOCK    ( 0x17U )
#define J_VK_CANCEL         ( 0x18U )
#define J_VK_INSERT         ( 0x1AU )
#define J_VK_ESCAPE         ( 0x1BU )
#define J_VK_CONVERT        ( 0x1CU )
#define J_VK_NONCONVERT     ( 0x1DU )
#define J_VK_ACCEPT         ( 0x1EU )
#define J_VK_MODECHANGE     ( 0x1FU )

//
// Unicode: Printable [0x20 - 0x7E]
//

#define J_VK_SPACE          ( 0x20U )
#define J_VK_EXCLAMATION_MARK ( 0x21U )
#define J_VK_QUOTEDBL       ( 0x22U )
#define J_VK_NUMBER_SIGN    ( 0x23U )
#define J_VK_DOLLAR         ( 0x24U )
#define J_VK_PERCENT        ( 0x25U )
#define J_VK_AMPERSAND      ( 0x26U )
#define J_VK_QUOTE          ( 0x27U )
#define J_VK_LEFT_PARENTHESIS  ( 0x28U )
#define J_VK_RIGHT_PARENTHESIS ( 0x29U )
#define J_VK_ASTERISK       ( 0x2AU )
#define J_VK_PLUS           ( 0x2BU )
#define J_VK_COMMA          ( 0x2CU )
#define J_VK_MINUS          ( 0x2DU )
#define J_VK_PERIOD         ( 0x2EU )
#define J_VK_SLASH          ( 0x2FU )
#define J_VK_0              ( 0x30U )
#define J_VK_1              ( 0x31U )
#define J_VK_2              ( 0x32U )
#define J_VK_3              ( 0x33U )
#define J_VK_4              ( 0x34U )
#define J_VK_5              ( 0x35U )
#define J_VK_6              ( 0x36U )
#define J_VK_7              ( 0x37U )
#define J_VK_8              ( 0x38U )
#define J_VK_9              ( 0x39U )
#define J_VK_COLON          ( 0x3AU )
#define J_VK_SEMICOLON      ( 0x3BU )
#define J_VK_LESS           ( 0x3CU )
#define J_VK_EQUALS         ( 0x3DU )
#define J_VK_GREATER        ( 0x3EU )
#define J_VK_QUESTIONMARK   ( 0x3FU )
#define J_VK_AT             ( 0x40U )
#define J_VK_A              ( 0x41U )
#define J_VK_B              ( 0x42U )
#define J_VK_C              ( 0x43U )
#define J_VK_D              ( 0x44U )
#define J_VK_E              ( 0x45U )
#define J_VK_F              ( 0x46U )
#define J_VK_G              ( 0x47U )
#define J_VK_H              ( 0x48U )
#define J_VK_I              ( 0x49U )
#define J_VK_J              ( 0x4AU )
#define J_VK_K              ( 0x4BU )
#define J_VK_L              ( 0x4CU )
#define J_VK_M              ( 0x4DU )
#define J_VK_N              ( 0x4EU )
#define J_VK_O              ( 0x4FU )
#define J_VK_P              ( 0x50U )
#define J_VK_Q              ( 0x51U )
#define J_VK_R              ( 0x52U )
#define J_VK_S              ( 0x53U )
#define J_VK_T              ( 0x54U )
#define J_VK_U              ( 0x55U )
#define J_VK_V              ( 0x56U )
#define J_VK_W              ( 0x57U )
#define J_VK_X              ( 0x58U )
#define J_VK_Y              ( 0x59U )
#define J_VK_Z              ( 0x5AU )
#define J_VK_OPEN_BRACKET   ( 0x5BU )
#define J_VK_BACK_SLASH     ( 0x5CU )
#define J_VK_CLOSE_BRACKET  ( 0x5DU )
#define J_VK_CIRCUMFLEX     ( 0x5EU )
#define J_VK_UNDERSCORE     ( 0x5FU )
#define J_VK_BACK_QUOTE     ( 0x60U )
#define J_VK_F1             ( 0x60U+ 1U )
#define J_VK_F2             ( 0x60U+ 2U )
#define J_VK_F3             ( 0x60U+ 3U )
#define J_VK_F4             ( 0x60U+ 4U )
#define J_VK_F5             ( 0x60U+ 5U )
#define J_VK_F6             ( 0x60U+ 6U )
#define J_VK_F7             ( 0x60U+ 7U )
#define J_VK_F8             ( 0x60U+ 8U )
#define J_VK_F9             ( 0x60U+ 9U )
#define J_VK_F10            ( 0x60U+10U )
#define J_VK_F11            ( 0x60U+11U )
#define J_VK_F12            ( 0x60U+12U )
#define J_VK_F13            ( 0x60U+13U )
#define J_VK_F14            ( 0x60U+14U )
#define J_VK_F15            ( 0x60U+15U )
#define J_VK_F16            ( 0x60U+16U )
#define J_VK_F17            ( 0x60U+17U )
#define J_VK_F18            ( 0x60U+18U )
#define J_VK_F19            ( 0x60U+19U )
#define J_VK_F20            ( 0x60U+20U )
#define J_VK_F21            ( 0x60U+21U )
#define J_VK_F22            ( 0x60U+22U )
#define J_VK_F23            ( 0x60U+23U )
#define J_VK_F24            ( 0x60U+24U )
#define J_VK_LEFT_BRACE     ( 0x7BU )
#define J_VK_PIPE           ( 0x7CU )
#define J_VK_RIGHT_BRACE    ( 0x7DU )
#define J_VK_TILDE          ( 0x7EU )

//
// Unicode: Non printable controls: [0x7F - 0x9F]
//

#define J_VK_SEPARATOR      ( 0x7FU )
#define J_VK_NUMPAD0        ( 0x80U )
#define J_VK_NUMPAD1        ( 0x81U )
#define J_VK_NUMPAD2        ( 0x82U )
#define J_VK_NUMPAD3        ( 0x83U )
#define J_VK_NUMPAD4        ( 0x84U )
#define J_VK_NUMPAD5        ( 0x85U )
#define J_VK_NUMPAD6        ( 0x86U )
#define J_VK_NUMPAD7        ( 0x87U )
#define J_VK_NUMPAD8        ( 0x88U )
#define J_VK_NUMPAD9        ( 0x89U )
#define J_VK_DECIMAL        ( 0x8AU )
#define J_VK_ADD            ( 0x8BU )
#define J_VK_SUBTRACT       ( 0x8CU )
#define J_VK_MULTIPLY       ( 0x8DU )
#define J_VK_DIVIDE         ( 0x8EU )

#define J_VK_DELETE         ( 0x93U )
#define J_VK_NUM_LOCK       ( 0x94U )
#define J_VK_LEFT           ( 0x95U )
#define J_VK_UP             ( 0x96U )
#define J_VK_RIGHT          ( 0x97U )
#define J_VK_DOWN           ( 0x98U )
#define J_VK_CONTEXT_MENU   ( 0x99U )
#define J_VK_WINDOWS        ( 0x9AU )
#define J_VK_META           ( 0x9BU )
#define J_VK_HELP           ( 0x9CU )
#define J_VK_COMPOSE        ( 0x9DU )
#define J_VK_BEGIN          ( 0x9EU )
#define J_VK_STOP           ( 0x9FU )

//
// Unicode: Printable [0x00A0 - 0xDFFF]
//

#define J_VK_INVERTED_EXCLAMATION_MARK ( 0xA1U )
#define J_VK_EURO_SIGN                ( 0x20ACU )

//
// Unicode: Private 0xE000 - 0xF8FF (Marked Non-Printable)
//

/* for Sun keyboards */
#define J_VK_CUT            ( 0xF879U )
#define J_VK_COPY           ( 0xF87AU )
#define J_VK_PASTE          ( 0xF87BU )
#define J_VK_UNDO           ( 0xF87CU )
#define J_VK_AGAIN          ( 0xF87DU )
#define J_VK_FIND           ( 0xF87EU )
#define J_VK_PROPS          ( 0xF87FU )

/* for input method support on Asian Keyboards */
#define J_VK_INPUT_METHOD_ON_OFF ( 0xF890U )
#define J_VK_CODE_INPUT ( 0xF891U )
#define J_VK_ROMAN_CHARACTERS ( 0xF892U )
#define J_VK_ALL_CANDIDATES ( 0xF893U )
#define J_VK_PREVIOUS_CANDIDATE ( 0xF894U )
#define J_VK_ALPHANUMERIC   ( 0xF895U )
#define J_VK_KATAKANA       ( 0xF896U )
#define J_VK_HIRAGANA       ( 0xF897U )
#define J_VK_FULL_WIDTH     ( 0xF898U )
#define J_VK_HALF_WIDTH     ( 0xF89AU )
#define J_VK_JAPANESE_KATAKANA ( 0xF89BU )
#define J_VK_JAPANESE_HIRAGANA ( 0xF89CU )
#define J_VK_JAPANESE_ROMAN ( 0xF89DU )
#define J_VK_KANA_LOCK ( 0xF89FU )

#define J_VK_KEYBOARD_INVISIBLE ( 0xF8FFU )

#endif

