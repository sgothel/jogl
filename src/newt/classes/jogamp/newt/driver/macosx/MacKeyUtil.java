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
package jogamp.newt.driver.macosx;

import com.jogamp.newt.event.KeyEvent;

public class MacKeyUtil {

    //
    // KeyCodes (Layout Dependent)
    //
    private static final short kVK_ANSI_A                    = 0x00;
    private static final short kVK_ANSI_S                    = 0x01;
    private static final short kVK_ANSI_D                    = 0x02;
    private static final short kVK_ANSI_F                    = 0x03;
    private static final short kVK_ANSI_H                    = 0x04;
    private static final short kVK_ANSI_G                    = 0x05;
    private static final short kVK_ANSI_Z                    = 0x06;
    private static final short kVK_ANSI_X                    = 0x07;
    private static final short kVK_ANSI_C                    = 0x08;
    private static final short kVK_ANSI_V                    = 0x09;
    private static final short kVK_ANSI_B                    = 0x0B;
    private static final short kVK_ANSI_Q                    = 0x0C;
    private static final short kVK_ANSI_W                    = 0x0D;
    private static final short kVK_ANSI_E                    = 0x0E;
    private static final short kVK_ANSI_R                    = 0x0F;
    private static final short kVK_ANSI_Y                    = 0x10;
    private static final short kVK_ANSI_T                    = 0x11;
    private static final short kVK_ANSI_1                    = 0x12;
    private static final short kVK_ANSI_2                    = 0x13;
    private static final short kVK_ANSI_3                    = 0x14;
    private static final short kVK_ANSI_4                    = 0x15;
    private static final short kVK_ANSI_6                    = 0x16;
    private static final short kVK_ANSI_5                    = 0x17;
    private static final short kVK_ANSI_Equal                = 0x18;
    private static final short kVK_ANSI_9                    = 0x19;
    private static final short kVK_ANSI_7                    = 0x1A;
    private static final short kVK_ANSI_Minus                = 0x1B;
    private static final short kVK_ANSI_8                    = 0x1C;
    private static final short kVK_ANSI_0                    = 0x1D;
    private static final short kVK_ANSI_RightBracket         = 0x1E;
    private static final short kVK_ANSI_O                    = 0x1F;
    private static final short kVK_ANSI_U                    = 0x20;
    private static final short kVK_ANSI_LeftBracket          = 0x21;
    private static final short kVK_ANSI_I                    = 0x22;
    private static final short kVK_ANSI_P                    = 0x23;
    private static final short kVK_ANSI_L                    = 0x25;
    private static final short kVK_ANSI_J                    = 0x26;
    private static final short kVK_ANSI_Quote                = 0x27;
    private static final short kVK_ANSI_K                    = 0x28;
    private static final short kVK_ANSI_Semicolon            = 0x29;
    private static final short kVK_ANSI_Backslash            = 0x2A;
    private static final short kVK_ANSI_Comma                = 0x2B;
    private static final short kVK_ANSI_Slash                = 0x2C;
    private static final short kVK_ANSI_N                    = 0x2D;
    private static final short kVK_ANSI_M                    = 0x2E;
    private static final short kVK_ANSI_Period               = 0x2F;
    private static final short kVK_ANSI_Grave                = 0x32;
    private static final short kVK_ANSI_KeypadDecimal        = 0x41;
    private static final short kVK_ANSI_KeypadMultiply       = 0x43;
    private static final short kVK_ANSI_KeypadPlus           = 0x45;
    private static final short kVK_ANSI_KeypadClear          = 0x47;
    private static final short kVK_ANSI_KeypadDivide         = 0x4B;
    private static final short kVK_ANSI_KeypadEnter          = 0x4C;
    private static final short kVK_ANSI_KeypadMinus          = 0x4E;
    private static final short kVK_ANSI_KeypadEquals         = 0x51;
    private static final short kVK_ANSI_Keypad0              = 0x52;
    private static final short kVK_ANSI_Keypad1              = 0x53;
    private static final short kVK_ANSI_Keypad2              = 0x54;
    private static final short kVK_ANSI_Keypad3              = 0x55;
    private static final short kVK_ANSI_Keypad4              = 0x56;
    private static final short kVK_ANSI_Keypad5              = 0x57;
    private static final short kVK_ANSI_Keypad6              = 0x58;
    private static final short kVK_ANSI_Keypad7              = 0x59;
    private static final short kVK_ANSI_Keypad8              = 0x5B;
    private static final short kVK_ANSI_Keypad9              = 0x5C;

    //
    // KeyCodes (Layout Independent)
    //
    private static final short kVK_Return                    = 0x24;
    private static final short kVK_Tab                       = 0x30;
    private static final short kVK_Space                     = 0x31;
    private static final short kVK_Delete                    = 0x33;
    private static final short kVK_Escape                    = 0x35;
    private static final short kVK_Command                   = 0x37;
    private static final short kVK_Shift                     = 0x38;
    private static final short kVK_CapsLock                  = 0x39;
    private static final short kVK_Option                    = 0x3A;
    private static final short kVK_Control                   = 0x3B;
    private static final short kVK_RightShift                = 0x3C;
    private static final short kVK_RightOption               = 0x3D;
    private static final short kVK_RightControl              = 0x3E;
    // private static final short kVK_Function                  = 0x3F;
    private static final short kVK_F17                       = 0x40;
    // private static final short kVK_VolumeUp                  = 0x48;
    // private static final short kVK_VolumeDown                = 0x49;
    // private static final short kVK_Mute                      = 0x4A;
    private static final short kVK_F18                       = 0x4F;
    private static final short kVK_F19                       = 0x50;
    private static final short kVK_F20                       = 0x5A;
    private static final short kVK_F5                        = 0x60;
    private static final short kVK_F6                        = 0x61;
    private static final short kVK_F7                        = 0x62;
    private static final short kVK_F3                        = 0x63;
    private static final short kVK_F8                        = 0x64;
    private static final short kVK_F9                        = 0x65;
    private static final short kVK_F11                       = 0x67;
    private static final short kVK_F13                       = 0x69;
    private static final short kVK_F16                       = 0x6A;
    private static final short kVK_F14                       = 0x6B;
    private static final short kVK_F10                       = 0x6D;
    private static final short kVK_F12                       = 0x6F;
    private static final short kVK_F15                       = 0x71;
    private static final short kVK_Help                      = 0x72;
    private static final short kVK_Home                      = 0x73;
    private static final short kVK_PageUp                    = 0x74;
    private static final short kVK_ForwardDelete             = 0x75;
    private static final short kVK_F4                        = 0x76;
    private static final short kVK_End                       = 0x77;
    private static final short kVK_F2                        = 0x78;
    private static final short kVK_PageDown                  = 0x79;
    private static final short kVK_F1                        = 0x7A;
    private static final short kVK_LeftArrow                 = 0x7B;
    private static final short kVK_RightArrow                = 0x7C;
    private static final short kVK_DownArrow                 = 0x7D;
    private static final short kVK_UpArrow                   = 0x7E;

    //
    // Key constants handled differently on Mac OS X than other platforms
    //
    private static final char NSUpArrowFunctionKey        = 0xF700;
    private static final char NSDownArrowFunctionKey      = 0xF701;
    private static final char NSLeftArrowFunctionKey      = 0xF702;
    private static final char NSRightArrowFunctionKey     = 0xF703;
    private static final char NSF1FunctionKey             = 0xF704;
    private static final char NSF2FunctionKey             = 0xF705;
    private static final char NSF3FunctionKey             = 0xF706;
    private static final char NSF4FunctionKey             = 0xF707;
    private static final char NSF5FunctionKey             = 0xF708;
    private static final char NSF6FunctionKey             = 0xF709;
    private static final char NSF7FunctionKey             = 0xF70A;
    private static final char NSF8FunctionKey             = 0xF70B;
    private static final char NSF9FunctionKey             = 0xF70C;
    private static final char NSF10FunctionKey            = 0xF70D;
    private static final char NSF11FunctionKey            = 0xF70E;
    private static final char NSF12FunctionKey            = 0xF70F;
    private static final char NSF13FunctionKey            = 0xF710;
    private static final char NSF14FunctionKey            = 0xF711;
    private static final char NSF15FunctionKey            = 0xF712;
    private static final char NSF16FunctionKey            = 0xF713;
    private static final char NSF17FunctionKey            = 0xF714;
    private static final char NSF18FunctionKey            = 0xF715;
    private static final char NSF19FunctionKey            = 0xF716;
    private static final char NSF20FunctionKey            = 0xF717;
    private static final char NSF21FunctionKey            = 0xF718;
    private static final char NSF22FunctionKey            = 0xF719;
    private static final char NSF23FunctionKey            = 0xF71A;
    private static final char NSF24FunctionKey            = 0xF71B;
    /**
    private static final char NSF25FunctionKey            = 0xF71C;
    private static final char NSF26FunctionKey            = 0xF71D;
    private static final char NSF27FunctionKey            = 0xF71E;
    private static final char NSF28FunctionKey            = 0xF71F;
    private static final char NSF29FunctionKey            = 0xF720;
    private static final char NSF30FunctionKey            = 0xF721;
    private static final char NSF31FunctionKey            = 0xF722;
    private static final char NSF32FunctionKey            = 0xF723;
    private static final char NSF33FunctionKey            = 0xF724;
    private static final char NSF34FunctionKey            = 0xF725;
    private static final char NSF35FunctionKey            = 0xF726;
    */
    private static final char NSInsertFunctionKey         = 0xF727;
    private static final char NSDeleteFunctionKey         = 0xF728;
    private static final char NSHomeFunctionKey           = 0xF729;
    private static final char NSBeginFunctionKey          = 0xF72A;
    private static final char NSEndFunctionKey            = 0xF72B;
    private static final char NSPageUpFunctionKey         = 0xF72C;
    private static final char NSPageDownFunctionKey       = 0xF72D;
    private static final char NSPrintScreenFunctionKey    = 0xF72E;
    private static final char NSScrollLockFunctionKey     = 0xF72F;
    private static final char NSPauseFunctionKey          = 0xF730;
    // private static final char NSSysReqFunctionKey         = 0xF731;
    // private static final char NSBreakFunctionKey          = 0xF732;
    // private static final char NSResetFunctionKey          = 0xF733;
    private static final char NSStopFunctionKey           = 0xF734;
    /**
    private static final char NSMenuFunctionKey           = 0xF735;
    private static final char NSUserFunctionKey           = 0xF736;
    private static final char NSSystemFunctionKey         = 0xF737;
    private static final char NSPrintFunctionKey          = 0xF738;
    private static final char NSClearLineFunctionKey      = 0xF739;
    private static final char NSClearDisplayFunctionKey   = 0xF73A;
    private static final char NSInsertLineFunctionKey     = 0xF73B;
    private static final char NSDeleteLineFunctionKey     = 0xF73C;
    private static final char NSInsertCharFunctionKey     = 0xF73D;
    private static final char NSDeleteCharFunctionKey     = 0xF73E;
    private static final char NSPrevFunctionKey           = 0xF73F;
    private static final char NSNextFunctionKey           = 0xF740;
    private static final char NSSelectFunctionKey         = 0xF741;
    private static final char NSExecuteFunctionKey        = 0xF742;
    private static final char NSUndoFunctionKey           = 0xF743;
    private static final char NSRedoFunctionKey           = 0xF744;
    private static final char NSFindFunctionKey           = 0xF745;
    private static final char NSHelpFunctionKey           = 0xF746;
    private static final char NSModeSwitchFunctionKey     = 0xF747;
    */

    static short validateKeyCode(final short keyCode, final char keyChar) {
        // OS X Virtual Keycodes
        switch(keyCode) {
            //
            // KeyCodes (Layout Dependent)
            //
            case kVK_ANSI_A:               return KeyEvent.VK_A;
            case kVK_ANSI_S:               return KeyEvent.VK_S;
            case kVK_ANSI_D:               return KeyEvent.VK_D;
            case kVK_ANSI_F:               return KeyEvent.VK_F;
            case kVK_ANSI_H:               return KeyEvent.VK_H;
            case kVK_ANSI_G:               return KeyEvent.VK_G;
            case kVK_ANSI_Z:               return KeyEvent.VK_Z;
            case kVK_ANSI_X:               return KeyEvent.VK_X;
            case kVK_ANSI_C:               return KeyEvent.VK_C;
            case kVK_ANSI_V:               return KeyEvent.VK_V;
            case kVK_ANSI_B:               return KeyEvent.VK_B;
            case kVK_ANSI_Q:               return KeyEvent.VK_Q;
            case kVK_ANSI_W:               return KeyEvent.VK_W;
            case kVK_ANSI_E:               return KeyEvent.VK_E;
            case kVK_ANSI_R:               return KeyEvent.VK_R;
            case kVK_ANSI_Y:               return KeyEvent.VK_Y;
            case kVK_ANSI_T:               return KeyEvent.VK_T;
            case kVK_ANSI_1:               return KeyEvent.VK_1;
            case kVK_ANSI_2:               return KeyEvent.VK_2;
            case kVK_ANSI_3:               return KeyEvent.VK_3;
            case kVK_ANSI_4:               return KeyEvent.VK_4;
            case kVK_ANSI_6:               return KeyEvent.VK_6;
            case kVK_ANSI_5:               return KeyEvent.VK_5;
            case kVK_ANSI_Equal:           return KeyEvent.VK_EQUALS;
            case kVK_ANSI_9:               return KeyEvent.VK_9;
            case kVK_ANSI_7:               return KeyEvent.VK_7;
            case kVK_ANSI_Minus:           return KeyEvent.VK_MINUS;
            case kVK_ANSI_8:               return KeyEvent.VK_8;
            case kVK_ANSI_0:               return KeyEvent.VK_0;
            case kVK_ANSI_RightBracket:    return KeyEvent.VK_CLOSE_BRACKET;
            case kVK_ANSI_O:               return KeyEvent.VK_O;
            case kVK_ANSI_U:               return KeyEvent.VK_U;
            case kVK_ANSI_LeftBracket:     return KeyEvent.VK_OPEN_BRACKET;
            case kVK_ANSI_I:               return KeyEvent.VK_I;
            case kVK_ANSI_P:               return KeyEvent.VK_P;
            case kVK_ANSI_L:               return KeyEvent.VK_L;
            case kVK_ANSI_J:               return KeyEvent.VK_J;
            case kVK_ANSI_Quote:           return KeyEvent.VK_QUOTE;
            case kVK_ANSI_K:               return KeyEvent.VK_K;
            case kVK_ANSI_Semicolon:       return KeyEvent.VK_SEMICOLON;
            case kVK_ANSI_Backslash:       return KeyEvent.VK_BACK_SLASH;
            case kVK_ANSI_Comma:           return KeyEvent.VK_COMMA;
            case kVK_ANSI_Slash:           return KeyEvent.VK_SLASH;
            case kVK_ANSI_N:               return KeyEvent.VK_N;
            case kVK_ANSI_M:               return KeyEvent.VK_M;
            case kVK_ANSI_Period:          return KeyEvent.VK_PERIOD;
            case kVK_ANSI_Grave:           return KeyEvent.VK_BACK_QUOTE; // KeyEvent.VK_DEAD_GRAVE
            case kVK_ANSI_KeypadDecimal:   return KeyEvent.VK_DECIMAL;
            case kVK_ANSI_KeypadMultiply:  return KeyEvent.VK_MULTIPLY;
            case kVK_ANSI_KeypadPlus:      return KeyEvent.VK_PLUS;
            case kVK_ANSI_KeypadClear:     return KeyEvent.VK_CLEAR;
            case kVK_ANSI_KeypadDivide:    return KeyEvent.VK_DIVIDE;
            case kVK_ANSI_KeypadEnter:     return KeyEvent.VK_ENTER;
            case kVK_ANSI_KeypadMinus:     return KeyEvent.VK_MINUS;
            case kVK_ANSI_KeypadEquals:    return KeyEvent.VK_EQUALS;
            case kVK_ANSI_Keypad0:         return KeyEvent.VK_0;
            case kVK_ANSI_Keypad1:         return KeyEvent.VK_1;
            case kVK_ANSI_Keypad2:         return KeyEvent.VK_2;
            case kVK_ANSI_Keypad3:         return KeyEvent.VK_3;
            case kVK_ANSI_Keypad4:         return KeyEvent.VK_4;
            case kVK_ANSI_Keypad5:         return KeyEvent.VK_5;
            case kVK_ANSI_Keypad6:         return KeyEvent.VK_6;
            case kVK_ANSI_Keypad7:         return KeyEvent.VK_7;
            case kVK_ANSI_Keypad8:         return KeyEvent.VK_8;
            case kVK_ANSI_Keypad9:         return KeyEvent.VK_9;

            //
            // KeyCodes (Layout Independent)
            //
            case kVK_Return:               return KeyEvent.VK_ENTER;
            case kVK_Tab:                  return KeyEvent.VK_TAB;
            case kVK_Space:                return KeyEvent.VK_SPACE;
            case kVK_Delete:               return KeyEvent.VK_BACK_SPACE;
            case kVK_Escape:               return KeyEvent.VK_ESCAPE;
            case kVK_Command:              return KeyEvent.VK_WINDOWS;
            case kVK_Shift:                return KeyEvent.VK_SHIFT;
            case kVK_CapsLock:             return KeyEvent.VK_CAPS_LOCK;
            case kVK_Option:               return KeyEvent.VK_ALT;
            case kVK_Control:              return KeyEvent.VK_CONTROL;
            case kVK_RightShift:           return KeyEvent.VK_SHIFT;
            case kVK_RightOption:          return KeyEvent.VK_ALT_GRAPH;
            case kVK_RightControl:         return KeyEvent.VK_CONTROL;
            // case kVK_Function:             return KeyEvent.VK_F;
            case kVK_F17:                  return KeyEvent.VK_F17;
            // case kVK_VolumeUp:
            // case kVK_VolumeDown:
            // case kVK_Mute:
            case kVK_F18:                  return KeyEvent.VK_F18;
            case kVK_F19:                  return KeyEvent.VK_F19;
            case kVK_F20:                  return KeyEvent.VK_F20;
            case kVK_F5:                   return KeyEvent.VK_F5;
            case kVK_F6:                   return KeyEvent.VK_F6;
            case kVK_F7:                   return KeyEvent.VK_F7;
            case kVK_F3:                   return KeyEvent.VK_F3;
            case kVK_F8:                   return KeyEvent.VK_F8;
            case kVK_F9:                   return KeyEvent.VK_F9;
            case kVK_F11:                  return KeyEvent.VK_F11;
            case kVK_F13:                  return KeyEvent.VK_F13;
            case kVK_F16:                  return KeyEvent.VK_F16;
            case kVK_F14:                  return KeyEvent.VK_F14;
            case kVK_F10:                  return KeyEvent.VK_F10;
            case kVK_F12:                  return KeyEvent.VK_F12;
            case kVK_F15:                  return KeyEvent.VK_F15;
            case kVK_Help:                 return KeyEvent.VK_HELP;
            case kVK_Home:                 return KeyEvent.VK_HOME;
            case kVK_PageUp:               return KeyEvent.VK_PAGE_UP;
            case kVK_ForwardDelete:        return KeyEvent.VK_DELETE;
            case kVK_F4:                   return KeyEvent.VK_F4;
            case kVK_End:                  return KeyEvent.VK_END;
            case kVK_F2:                   return KeyEvent.VK_F2;
            case kVK_PageDown:             return KeyEvent.VK_PAGE_DOWN;
            case kVK_F1:                   return KeyEvent.VK_F1;
            case kVK_LeftArrow:            return KeyEvent.VK_LEFT;
            case kVK_RightArrow:           return KeyEvent.VK_RIGHT;
            case kVK_DownArrow:            return KeyEvent.VK_DOWN;
            case kVK_UpArrow:              return KeyEvent.VK_UP;
        }

        switch (keyChar) {
            case NSUpArrowFunctionKey:     return KeyEvent.VK_UP;
            case NSDownArrowFunctionKey:   return KeyEvent.VK_DOWN;
            case NSLeftArrowFunctionKey:   return KeyEvent.VK_LEFT;
            case NSRightArrowFunctionKey:  return KeyEvent.VK_RIGHT;
            case NSF1FunctionKey:          return KeyEvent.VK_F1;
            case NSF2FunctionKey:          return KeyEvent.VK_F2;
            case NSF3FunctionKey:          return KeyEvent.VK_F3;
            case NSF4FunctionKey:          return KeyEvent.VK_F4;
            case NSF5FunctionKey:          return KeyEvent.VK_F5;
            case NSF6FunctionKey:          return KeyEvent.VK_F6;
            case NSF7FunctionKey:          return KeyEvent.VK_F7;
            case NSF8FunctionKey:          return KeyEvent.VK_F8;
            case NSF9FunctionKey:          return KeyEvent.VK_F9;
            case NSF10FunctionKey:         return KeyEvent.VK_F10;
            case NSF11FunctionKey:         return KeyEvent.VK_F11;
            case NSF12FunctionKey:         return KeyEvent.VK_F12;
            case NSF13FunctionKey:         return KeyEvent.VK_F13;
            case NSF14FunctionKey:         return KeyEvent.VK_F14;
            case NSF15FunctionKey:         return KeyEvent.VK_F15;
            case NSF16FunctionKey:         return KeyEvent.VK_F16;
            case NSF17FunctionKey:         return KeyEvent.VK_F17;
            case NSF18FunctionKey:         return KeyEvent.VK_F18;
            case NSF19FunctionKey:         return KeyEvent.VK_F19;
            case NSF20FunctionKey:         return KeyEvent.VK_F20;
            case NSF21FunctionKey:         return KeyEvent.VK_F21;
            case NSF22FunctionKey:         return KeyEvent.VK_F22;
            case NSF23FunctionKey:         return KeyEvent.VK_F23;
            case NSF24FunctionKey:         return KeyEvent.VK_F24;
            case NSInsertFunctionKey:      return KeyEvent.VK_INSERT;
            case NSDeleteFunctionKey:      return KeyEvent.VK_DELETE;
            case NSHomeFunctionKey:        return KeyEvent.VK_HOME;
            case NSBeginFunctionKey:       return KeyEvent.VK_BEGIN;
            case NSEndFunctionKey:         return KeyEvent.VK_END;
            case NSPageUpFunctionKey:      return KeyEvent.VK_PAGE_UP;
            case NSPageDownFunctionKey:    return KeyEvent.VK_PAGE_DOWN;
            case NSPrintScreenFunctionKey: return KeyEvent.VK_PRINTSCREEN;
            case NSScrollLockFunctionKey:  return KeyEvent.VK_SCROLL_LOCK;
            case NSPauseFunctionKey:       return KeyEvent.VK_PAUSE;
            // Not handled:
            // NSSysReqFunctionKey
            // NSBreakFunctionKey
            // NSResetFunctionKey
            case NSStopFunctionKey:        return KeyEvent.VK_STOP;
            // Not handled:
            // NSMenuFunctionKey
            // NSUserFunctionKey
            // NSSystemFunctionKey
            // NSPrintFunctionKey
            // NSClearLineFunctionKey
            // NSClearDisplayFunctionKey
            // NSInsertLineFunctionKey
            // NSDeleteLineFunctionKey
            // NSInsertCharFunctionKey
            // NSDeleteCharFunctionKey
            // NSPrevFunctionKey
            // NSNextFunctionKey
            // NSSelectFunctionKey
            // NSExecuteFunctionKey
            // NSUndoFunctionKey
            // NSRedoFunctionKey
            // NSFindFunctionKey
            // NSHelpFunctionKey
            // NSModeSwitchFunctionKey
        }

        return (short) keyChar; // let's hope for the best (compatibility of keyChar/keyCode's)
    }
}
