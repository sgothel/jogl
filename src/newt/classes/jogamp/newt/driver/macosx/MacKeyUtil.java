package jogamp.newt.driver.macosx;

import com.jogamp.newt.event.KeyEvent;

public class MacKeyUtil {
      
    // KeyCodes (independent)
    private static final int kVK_Return                    = 0x24;
    private static final int kVK_Tab                       = 0x30;
    private static final int kVK_Space                     = 0x31;
    private static final int kVK_Delete                    = 0x33;
    private static final int kVK_Escape                    = 0x35;
    private static final int kVK_Command                   = 0x37;
    private static final int kVK_Shift                     = 0x38;
    private static final int kVK_CapsLock                  = 0x39;
    private static final int kVK_Option                    = 0x3A;
    private static final int kVK_Control                   = 0x3B;
    private static final int kVK_RightShift                = 0x3C;
    private static final int kVK_RightOption               = 0x3D;
    private static final int kVK_RightControl              = 0x3E;
    private static final int kVK_Function                  = 0x3F;
    private static final int kVK_F17                       = 0x40;
    private static final int kVK_VolumeUp                  = 0x48;
    private static final int kVK_VolumeDown                = 0x49;
    private static final int kVK_Mute                      = 0x4A;
    private static final int kVK_F18                       = 0x4F;
    private static final int kVK_F19                       = 0x50;
    private static final int kVK_F20                       = 0x5A;
    private static final int kVK_F5                        = 0x60;
    private static final int kVK_F6                        = 0x61;
    private static final int kVK_F7                        = 0x62;
    private static final int kVK_F3                        = 0x63;
    private static final int kVK_F8                        = 0x64;
    private static final int kVK_F9                        = 0x65;
    private static final int kVK_F11                       = 0x67;
    private static final int kVK_F13                       = 0x69;
    private static final int kVK_F16                       = 0x6A;
    private static final int kVK_F14                       = 0x6B;
    private static final int kVK_F10                       = 0x6D;
    private static final int kVK_F12                       = 0x6F;
    private static final int kVK_F15                       = 0x71;
    private static final int kVK_Help                      = 0x72;
    private static final int kVK_Home                      = 0x73;
    private static final int kVK_PageUp                    = 0x74;
    private static final int kVK_ForwardDelete             = 0x75;
    private static final int kVK_F4                        = 0x76;
    private static final int kVK_End                       = 0x77;
    private static final int kVK_F2                        = 0x78;
    private static final int kVK_PageDown                  = 0x79;
    private static final int kVK_F1                        = 0x7A;
    private static final int kVK_LeftArrow                 = 0x7B;
    private static final int kVK_RightArrow                = 0x7C;
    private static final int kVK_DownArrow                 = 0x7D;
    private static final int kVK_UpArrow                   = 0x7E;
  
    // Key constants handled differently on Mac OS X than other platforms
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
    private static final char NSSysReqFunctionKey         = 0xF731;
    private static final char NSBreakFunctionKey          = 0xF732;
    private static final char NSResetFunctionKey          = 0xF733;
    private static final char NSStopFunctionKey           = 0xF734;
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
    
    static int validateKeyCode(int keyCode, char keyChar) {        
        // OS X Virtual Keycodes
        switch(keyCode) {
            case kVK_Return:               return KeyEvent.VK_ENTER;
            case kVK_Tab:                  return KeyEvent.VK_TAB;
            case kVK_Space:                return KeyEvent.VK_SPACE;
            case kVK_Delete:               return KeyEvent.VK_BACK_SPACE;
            case kVK_Escape:               return KeyEvent.VK_ESCAPE;
            case kVK_Command:              return KeyEvent.VK_ALT;
            case kVK_Shift:                return KeyEvent.VK_SHIFT;
            case kVK_CapsLock:             return KeyEvent.VK_CAPS_LOCK;
            case kVK_Option:               return KeyEvent.VK_WINDOWS;
            case kVK_Control:              return KeyEvent.VK_CONTROL;
            case kVK_RightShift:           return KeyEvent.VK_SHIFT;
            case kVK_RightOption:          return KeyEvent.VK_WINDOWS;
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
        
        if (keyChar == '\r') {
            // Turn these into \n
            return KeyEvent.VK_ENTER;
        }

        if (keyChar >= NSUpArrowFunctionKey && keyChar <= NSModeSwitchFunctionKey) {
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
                default: break;
            }
        }

        if ('a' <= keyChar && keyChar <= 'z') {
            return KeyEvent.VK_A + ( keyChar - 'a' ) ;
        }

        return (int) keyChar; // let's hope for the best (compatibility of keyChar/keyCode's)
    }   
}
