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
#define EVENT_KEY_TYPED 302

#define J_CHAR_UNDEFINED 0xFFFF;
#define J_VK_ENTER          '\n'
#define J_VK_BACK_SPACE     '\b'
#define J_VK_TAB            '\t'
#define J_VK_CANCEL         0x03
#define J_VK_CLEAR          0x0C
#define J_VK_SHIFT          0x10
#define J_VK_CONTROL        0x11
#define J_VK_ALT            0x12
#define J_VK_PAUSE          0x13
#define J_VK_CAPS_LOCK      0x14
#define J_VK_ESCAPE         0x1B
#define J_VK_SPACE          0x20
#define J_VK_PAGE_UP        0x21
#define J_VK_PAGE_DOWN      0x22
#define J_VK_END            0x23
#define J_VK_HOME           0x24
#define J_VK_LEFT           0x25
#define J_VK_UP             0x26
#define J_VK_RIGHT          0x27
#define J_VK_DOWN           0x28
#define J_VK_COMMA          0x2C
#define J_VK_MINUS          0x2D
#define J_VK_PERIOD         0x2E
#define J_VK_SLASH          0x2F
#define J_VK_0              0x30
#define J_VK_1              0x31
#define J_VK_2              0x32
#define J_VK_3              0x33
#define J_VK_4              0x34
#define J_VK_5              0x35
#define J_VK_6              0x36
#define J_VK_7              0x37
#define J_VK_8              0x38
#define J_VK_9              0x39
#define J_VK_SEMICOLON      0x3B
#define J_VK_EQUALS         0x3D
#define J_VK_A              0x41
#define J_VK_B              0x42
#define J_VK_C              0x43
#define J_VK_D              0x44
#define J_VK_E              0x45
#define J_VK_F              0x46
#define J_VK_G              0x47
#define J_VK_H              0x48
#define J_VK_I              0x49
#define J_VK_J              0x4A
#define J_VK_K              0x4B
#define J_VK_L              0x4C
#define J_VK_M              0x4D
#define J_VK_N              0x4E
#define J_VK_O              0x4F
#define J_VK_P              0x50
#define J_VK_Q              0x51
#define J_VK_R              0x52
#define J_VK_S              0x53
#define J_VK_T              0x54
#define J_VK_U              0x55
#define J_VK_V              0x56
#define J_VK_W              0x57
#define J_VK_X              0x58
#define J_VK_Y              0x59
#define J_VK_Z              0x5A
#define J_VK_OPEN_BRACKET   0x5B
#define J_VK_BACK_SLASH     0x5C
#define J_VK_CLOSE_BRACKET  0x5D
#define J_VK_NUMPAD0        0x60
#define J_VK_NUMPAD1        0x61
#define J_VK_NUMPAD2        0x62
#define J_VK_NUMPAD3        0x63
#define J_VK_NUMPAD4        0x64
#define J_VK_NUMPAD5        0x65
#define J_VK_NUMPAD6        0x66
#define J_VK_NUMPAD7        0x67
#define J_VK_NUMPAD8        0x68
#define J_VK_NUMPAD9        0x69
#define J_VK_MULTIPLY       0x6A
#define J_VK_ADD            0x6B
#define J_VK_SEPARATOR      0x6C
#define J_VK_SUBTRACT       0x6D
#define J_VK_DECIMAL        0x6E
#define J_VK_DIVIDE         0x6F
#define J_VK_DELETE         0x7F /* ASCII DEL */
#define J_VK_NUM_LOCK       0x90
#define J_VK_SCROLL_LOCK    0x91
#define J_VK_F1             0x70
#define J_VK_F2             0x71
#define J_VK_F3             0x72
#define J_VK_F4             0x73
#define J_VK_F5             0x74
#define J_VK_F6             0x75
#define J_VK_F7             0x76
#define J_VK_F8             0x77
#define J_VK_F9             0x78
#define J_VK_F10            0x79
#define J_VK_F11            0x7A
#define J_VK_F12            0x7B
#define J_VK_F13            0xF000
#define J_VK_F14            0xF001
#define J_VK_F15            0xF002
#define J_VK_F16            0xF003
#define J_VK_F17            0xF004
#define J_VK_F18            0xF005
#define J_VK_F19            0xF006
#define J_VK_F20            0xF007
#define J_VK_F21            0xF008
#define J_VK_F22            0xF009
#define J_VK_F23            0xF00A
#define J_VK_F24            0xF00B
#define J_VK_PRINTSCREEN    0x9A
#define J_VK_INSERT         0x9B
#define J_VK_HELP           0x9C
#define J_VK_META           0x9D
#define J_VK_BACK_QUOTE     0xC0
#define J_VK_QUOTE          0xDE
#define J_VK_KP_UP          0xE0
#define J_VK_KP_DOWN        0xE1
#define J_VK_KP_LEFT        0xE2
#define J_VK_KP_RIGHT       0xE3
#define J_VK_DEAD_GRAVE               0x80
#define J_VK_DEAD_ACUTE               0x81
#define J_VK_DEAD_CIRCUMFLEX          0x82
#define J_VK_DEAD_TILDE               0x83
#define J_VK_DEAD_MACRON              0x84
#define J_VK_DEAD_BREVE               0x85
#define J_VK_DEAD_ABOVEDOT            0x86
#define J_VK_DEAD_DIAERESIS           0x87
#define J_VK_DEAD_ABOVERING           0x88
#define J_VK_DEAD_DOUBLEACUTE         0x89
#define J_VK_DEAD_CARON               0x8a
#define J_VK_DEAD_CEDILLA             0x8b
#define J_VK_DEAD_OGONEK              0x8c
#define J_VK_DEAD_IOTA                0x8d
#define J_VK_DEAD_VOICED_SOUND        0x8e
#define J_VK_DEAD_SEMIVOICED_SOUND    0x8f
#define J_VK_AMPERSAND                0x96
#define J_VK_ASTERISK                 0x97
#define J_VK_QUOTEDBL                 0x98
#define J_VK_LESS                     0x99
#define J_VK_GREATER                  0xa0
#define J_VK_BRACELEFT                0xa1
#define J_VK_BRACERIGHT               0xa2
#define J_VK_AT                       0x0200
#define J_VK_COLON                    0x0201
#define J_VK_CIRCUMFLEX               0x0202
#define J_VK_DOLLAR                   0x0203
#define J_VK_EURO_SIGN                0x0204
#define J_VK_EXCLAMATION_MARK         0x0205
#define J_VK_INVERTED_EXCLAMATION_MARK 0x0206
#define J_VK_LEFT_PARENTHESIS         0x0207
#define J_VK_NUMBER_SIGN              0x0208
#define J_VK_PLUS                     0x0209
#define J_VK_RIGHT_PARENTHESIS        0x020A
#define J_VK_UNDERSCORE               0x020B
#define J_VK_WINDOWS                  0x020C
#define J_VK_CONTEXT_MENU             0x020D
#define J_VK_FINAL                    0x0018
#define J_VK_CONVERT                  0x001C
#define J_VK_NONCONVERT               0x001D
#define J_VK_ACCEPT                   0x001E
#define J_VK_MODECHANGE               0x001F
#define J_VK_KANA                     0x0015
#define J_VK_KANJI                    0x0019
#define J_VK_ALPHANUMERIC             0x00F0
#define J_VK_KATAKANA                 0x00F1
#define J_VK_HIRAGANA                 0x00F2
#define J_VK_FULL_WIDTH               0x00F3
#define J_VK_HALF_WIDTH               0x00F4
#define J_VK_ROMAN_CHARACTERS         0x00F5
#define J_VK_ALL_CANDIDATES           0x0100
#define J_VK_PREVIOUS_CANDIDATE       0x0101
#define J_VK_CODE_INPUT               0x0102
#define J_VK_JAPANESE_KATAKANA        0x0103
#define J_VK_JAPANESE_HIRAGANA        0x0104
#define J_VK_JAPANESE_ROMAN           0x0105
#define J_VK_KANA_LOCK                0x0106
#define J_VK_INPUT_METHOD_ON_OFF      0x0107
#define J_VK_CUT                      0xFFD1
#define J_VK_COPY                     0xFFCD
#define J_VK_PASTE                    0xFFCF
#define J_VK_UNDO                     0xFFCB
#define J_VK_AGAIN                    0xFFC9
#define J_VK_FIND                     0xFFD0
#define J_VK_PROPS                    0xFFCA
#define J_VK_STOP                     0xFFC8
#define J_VK_COMPOSE                  0xFF20
#define J_VK_ALT_GRAPH                0xFF7E
#define J_VK_BEGIN                    0xFF58
#define J_VK_UNDEFINED      0x0

#endif

