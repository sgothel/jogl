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

#include <Windows.h>
#include <Windowsx.h>
#include <tchar.h>
#include <stdlib.h>
// NOTE: it looks like SHFullScreen and/or aygshell.dll is not available on the APX 2500 any more
// #ifdef UNDER_CE
// #include "aygshell.h"
// #endif

/* This typedef is apparently needed for Microsoft compilers before VC8,
   and on Windows CE and MingW32  */
#if !defined(__MINGW64__) && ( defined(UNDER_CE) || _MSC_VER <= 1400 )
    #ifdef _WIN64
        typedef long long intptr_t;
    #else
        typedef int intptr_t;
    #endif
#elif !defined(__MINGW64__) && _MSC_VER <= 1500
    #ifdef _WIN64 // [
        typedef __int64           intptr_t;
    #else // _WIN64 ][
        typedef int               intptr_t;
    #endif // _WIN64 ]
#else
    #include <inttypes.h>
#endif

#if !defined(__MINGW64__) && _MSC_VER <= 1500
    // FIXME: Determine for which MSVC versions ..
    #define strdup(s) _strdup(s)
#endif

#ifndef WM_MOUSEWHEEL
#define WM_MOUSEWHEEL                   0x020A
#endif //WM_MOUSEWHEEL

#ifndef WHEEL_DELTA
#define WHEEL_DELTA                     120
#endif //WHEEL_DELTA

#ifndef WHEEL_PAGESCROLL
#define WHEEL_PAGESCROLL                (UINT_MAX)
#endif //WHEEL_PAGESCROLL

#ifndef GET_WHEEL_DELTA_WPARAM  // defined for (_WIN32_WINNT >= 0x0500)
#define GET_WHEEL_DELTA_WPARAM(wParam)  ((short)HIWORD(wParam))
#endif

#ifndef MONITOR_DEFAULTTONULL
#define MONITOR_DEFAULTTONULL 0
#endif
#ifndef MONITOR_DEFAULTTOPRIMARY
#define MONITOR_DEFAULTTOPRIMARY 1
#endif
#ifndef MONITOR_DEFAULTTONEAREST
#define MONITOR_DEFAULTTONEAREST 2
#endif

#include "com_jogamp_newt_impl_windows_WindowsDisplay.h"
#include "com_jogamp_newt_impl_windows_WindowsScreen.h"
#include "com_jogamp_newt_impl_windows_WindowsWindow.h"

#include "MouseEvent.h"
#include "InputEvent.h"
#include "KeyEvent.h"

#include "NewtCommon.h"

// #define VERBOSE_ON 1
// #define DEBUG_KEYS 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(...) fprintf(stderr, __VA_ARGS__); fflush(stderr) 
#else
    #define DBG_PRINT(...)
#endif

#define STD_PRINT(...) fprintf(stderr, __VA_ARGS__); fflush(stderr) 

static void _FatalError(JNIEnv *env, const char* msg, ...)
{
    char buffer[512];
    va_list ap;

    va_start(ap, msg);
    vsnprintf(buffer, sizeof(buffer), msg, ap);
    va_end(ap);

    fprintf(stderr, "%s\n", buffer);
    (*env)->FatalError(env, buffer);
}

static const char * const ClazzNamePoint = "javax/media/nativewindow/util/Point";
static const char * const ClazzAnyCstrName = "<init>";
static const char * const ClazzNamePointCstrSignature = "(II)V";

static jclass pointClz = NULL;
static jmethodID pointCstr = NULL;

static jmethodID insetsChangedID = NULL;
static jmethodID sizeChangedID = NULL;
static jmethodID positionChangedID = NULL;
static jmethodID focusChangedID = NULL;
static jmethodID visibleChangedID = NULL;
static jmethodID windowDestroyNotifyID = NULL;
static jmethodID windowDestroyedID = NULL;
static jmethodID windowRepaintID = NULL;
static jmethodID enqueueMouseEventID = NULL;
static jmethodID sendMouseEventID = NULL;
static jmethodID enqueueKeyEventID = NULL;
static jmethodID sendKeyEventID = NULL;
static jmethodID focusActionID = NULL;
static jmethodID enqueueRequestFocusID = NULL;

static RECT* UpdateInsets(JNIEnv *env, jobject window, HWND hwnd);

typedef struct {
    JNIEnv* jenv;
    jobject jinstance;
} WindowUserData;
    
typedef struct {
    UINT javaKey;
    UINT windowsKey;
} KeyMapEntry;

// Static table, arranged more or less spatially.
static KeyMapEntry keyMapTable[] = {
    // Modifier keys
    {J_VK_CAPS_LOCK,        VK_CAPITAL},
    {J_VK_SHIFT,            VK_SHIFT},
    {J_VK_CONTROL,          VK_CONTROL},
    {J_VK_ALT,              VK_MENU},
    {J_VK_NUM_LOCK,         VK_NUMLOCK},

    // Miscellaneous Windows keys
    {J_VK_WINDOWS,          VK_LWIN},
    {J_VK_WINDOWS,          VK_RWIN},
    {J_VK_CONTEXT_MENU,     VK_APPS},

    // Alphabet
    {J_VK_A,                'A'},
    {J_VK_B,                'B'},
    {J_VK_C,                'C'},
    {J_VK_D,                'D'},
    {J_VK_E,                'E'},
    {J_VK_F,                'F'},
    {J_VK_G,                'G'},
    {J_VK_H,                'H'},
    {J_VK_I,                'I'},
    {J_VK_J,                'J'},
    {J_VK_K,                'K'},
    {J_VK_L,                'L'},
    {J_VK_M,                'M'},
    {J_VK_N,                'N'},
    {J_VK_O,                'O'},
    {J_VK_P,                'P'},
    {J_VK_Q,                'Q'},
    {J_VK_R,                'R'},
    {J_VK_S,                'S'},
    {J_VK_T,                'T'},
    {J_VK_U,                'U'},
    {J_VK_V,                'V'},
    {J_VK_W,                'W'},
    {J_VK_X,                'X'},
    {J_VK_Y,                'Y'},
    {J_VK_Z,                'Z'},
    {J_VK_0,                '0'},
    {J_VK_1,                '1'},
    {J_VK_2,                '2'},
    {J_VK_3,                '3'},
    {J_VK_4,                '4'},
    {J_VK_5,                '5'},
    {J_VK_6,                '6'},
    {J_VK_7,                '7'},
    {J_VK_8,                '8'},
    {J_VK_9,                '9'},
    {J_VK_ENTER,            VK_RETURN},
    {J_VK_SPACE,            VK_SPACE},
    {J_VK_BACK_SPACE,       VK_BACK},
    {J_VK_TAB,              VK_TAB},
    {J_VK_ESCAPE,           VK_ESCAPE},
    {J_VK_INSERT,           VK_INSERT},
    {J_VK_DELETE,           VK_DELETE},
    {J_VK_HOME,             VK_HOME},
    {J_VK_END,              VK_END},
    {J_VK_PAGE_UP,          VK_PRIOR},
    {J_VK_PAGE_DOWN,        VK_NEXT},
    {J_VK_CLEAR,            VK_CLEAR}, // NumPad 5

    // NumPad with NumLock off & extended arrows block (triangular)
    {J_VK_LEFT,             VK_LEFT},
    {J_VK_RIGHT,            VK_RIGHT},
    {J_VK_UP,               VK_UP},
    {J_VK_DOWN,             VK_DOWN},

    // NumPad with NumLock on: numbers
    {J_VK_NUMPAD0,          VK_NUMPAD0},
    {J_VK_NUMPAD1,          VK_NUMPAD1},
    {J_VK_NUMPAD2,          VK_NUMPAD2},
    {J_VK_NUMPAD3,          VK_NUMPAD3},
    {J_VK_NUMPAD4,          VK_NUMPAD4},
    {J_VK_NUMPAD5,          VK_NUMPAD5},
    {J_VK_NUMPAD6,          VK_NUMPAD6},
    {J_VK_NUMPAD7,          VK_NUMPAD7},
    {J_VK_NUMPAD8,          VK_NUMPAD8},
    {J_VK_NUMPAD9,          VK_NUMPAD9},

    // NumPad with NumLock on
    {J_VK_MULTIPLY,         VK_MULTIPLY},
    {J_VK_ADD,              VK_ADD},
    {J_VK_SEPARATOR,        VK_SEPARATOR},
    {J_VK_SUBTRACT,         VK_SUBTRACT},
    {J_VK_DECIMAL,          VK_DECIMAL},
    {J_VK_DIVIDE,           VK_DIVIDE},

    // Functional keys
    {J_VK_F1,               VK_F1},
    {J_VK_F2,               VK_F2},
    {J_VK_F3,               VK_F3},
    {J_VK_F4,               VK_F4},
    {J_VK_F5,               VK_F5},
    {J_VK_F6,               VK_F6},
    {J_VK_F7,               VK_F7},
    {J_VK_F8,               VK_F8},
    {J_VK_F9,               VK_F9},
    {J_VK_F10,              VK_F10},
    {J_VK_F11,              VK_F11},
    {J_VK_F12,              VK_F12},
    {J_VK_F13,              VK_F13},
    {J_VK_F14,              VK_F14},
    {J_VK_F15,              VK_F15},
    {J_VK_F16,              VK_F16},
    {J_VK_F17,              VK_F17},
    {J_VK_F18,              VK_F18},
    {J_VK_F19,              VK_F19},
    {J_VK_F20,              VK_F20},
    {J_VK_F21,              VK_F21},
    {J_VK_F22,              VK_F22},
    {J_VK_F23,              VK_F23},
    {J_VK_F24,              VK_F24},

    {J_VK_PRINTSCREEN,      VK_SNAPSHOT},
    {J_VK_SCROLL_LOCK,      VK_SCROLL},
    {J_VK_PAUSE,            VK_PAUSE},
    {J_VK_CANCEL,           VK_CANCEL},
    {J_VK_HELP,             VK_HELP},

    // Japanese
/*
    {J_VK_CONVERT,          VK_CONVERT},
    {J_VK_NONCONVERT,       VK_NONCONVERT},
    {J_VK_INPUT_METHOD_ON_OFF, VK_KANJI},
    {J_VK_ALPHANUMERIC,     VK_DBE_ALPHANUMERIC},
    {J_VK_KATAKANA,         VK_DBE_KATAKANA},
    {J_VK_HIRAGANA,         VK_DBE_HIRAGANA},
    {J_VK_FULL_WIDTH,       VK_DBE_DBCSCHAR},
    {J_VK_HALF_WIDTH,       VK_DBE_SBCSCHAR},
    {J_VK_ROMAN_CHARACTERS, VK_DBE_ROMAN},
*/

    {J_VK_UNDEFINED,        0}
};

/*
Dynamic mapping table for OEM VK codes.  This table is refilled
by BuildDynamicKeyMapTable when keyboard layout is switched.
(see NT4 DDK src/input/inc/vkoem.h for OEM VK_ values).
*/
typedef struct {
    // OEM VK codes known in advance
    UINT windowsKey;
    // depends on input langauge (kbd layout)
    UINT javaKey;
} DynamicKeyMapEntry;

static DynamicKeyMapEntry dynamicKeyMapTable[] = {
    {0x00BA,  J_VK_UNDEFINED}, // VK_OEM_1
    {0x00BB,  J_VK_UNDEFINED}, // VK_OEM_PLUS
    {0x00BC,  J_VK_UNDEFINED}, // VK_OEM_COMMA
    {0x00BD,  J_VK_UNDEFINED}, // VK_OEM_MINUS
    {0x00BE,  J_VK_UNDEFINED}, // VK_OEM_PERIOD
    {0x00BF,  J_VK_UNDEFINED}, // VK_OEM_2
    {0x00C0,  J_VK_UNDEFINED}, // VK_OEM_3
    {0x00DB,  J_VK_UNDEFINED}, // VK_OEM_4
    {0x00DC,  J_VK_UNDEFINED}, // VK_OEM_5
    {0x00DD,  J_VK_UNDEFINED}, // VK_OEM_6
    {0x00DE,  J_VK_UNDEFINED}, // VK_OEM_7
    {0x00DF,  J_VK_UNDEFINED}, // VK_OEM_8
    {0x00E2,  J_VK_UNDEFINED}, // VK_OEM_102
    {0, 0}
};

// Auxiliary tables used to fill the above dynamic table.  We first
// find the character for the OEM VK code using ::MapVirtualKey and
// then go through these auxiliary tables to map it to Java VK code.

typedef struct {
    WCHAR c;
    UINT  javaKey;
} CharToVKEntry;

static const CharToVKEntry charToVKTable[] = {
    {L'!',   J_VK_EXCLAMATION_MARK},
    {L'"',   J_VK_QUOTEDBL},
    {L'#',   J_VK_NUMBER_SIGN},
    {L'$',   J_VK_DOLLAR},
    {L'&',   J_VK_AMPERSAND},
    {L'\'',  J_VK_QUOTE},
    {L'(',   J_VK_LEFT_PARENTHESIS},
    {L')',   J_VK_RIGHT_PARENTHESIS},
    {L'*',   J_VK_ASTERISK},
    {L'+',   J_VK_PLUS},
    {L',',   J_VK_COMMA},
    {L'-',   J_VK_MINUS},
    {L'.',   J_VK_PERIOD},
    {L'/',   J_VK_SLASH},
    {L':',   J_VK_COLON},
    {L';',   J_VK_SEMICOLON},
    {L'<',   J_VK_LESS},
    {L'=',   J_VK_EQUALS},
    {L'>',   J_VK_GREATER},
    {L'@',   J_VK_AT},
    {L'[',   J_VK_OPEN_BRACKET},
    {L'\\',  J_VK_BACK_SLASH},
    {L']',   J_VK_CLOSE_BRACKET},
    {L'^',   J_VK_CIRCUMFLEX},
    {L'_',   J_VK_UNDERSCORE},
    {L'`',   J_VK_BACK_QUOTE},
    {L'{',   J_VK_BRACELEFT},
    {L'}',   J_VK_BRACERIGHT},
    {0x00A1, J_VK_INVERTED_EXCLAMATION_MARK},
    {0x20A0, J_VK_EURO_SIGN}, // ????
    {0,0}
};

// For dead accents some layouts return ASCII punctuation, while some
// return spacing accent chars, so both should be listed.  NB: MS docs
// say that conversion routings return spacing accent character, not
// combining.
static const CharToVKEntry charToDeadVKTable[] = {
    {L'`',   J_VK_DEAD_GRAVE},
    {L'\'',  J_VK_DEAD_ACUTE},
    {0x00B4, J_VK_DEAD_ACUTE},
    {L'^',   J_VK_DEAD_CIRCUMFLEX},
    {L'~',   J_VK_DEAD_TILDE},
    {0x02DC, J_VK_DEAD_TILDE},
    {0x00AF, J_VK_DEAD_MACRON},
    {0x02D8, J_VK_DEAD_BREVE},
    {0x02D9, J_VK_DEAD_ABOVEDOT},
    {L'"',   J_VK_DEAD_DIAERESIS},
    {0x00A8, J_VK_DEAD_DIAERESIS},
    {0x02DA, J_VK_DEAD_ABOVERING},
    {0x02DD, J_VK_DEAD_DOUBLEACUTE},
    {0x02C7, J_VK_DEAD_CARON},            // aka hacek
    {L',',   J_VK_DEAD_CEDILLA},
    {0x00B8, J_VK_DEAD_CEDILLA},
    {0x02DB, J_VK_DEAD_OGONEK},
    {0x037A, J_VK_DEAD_IOTA},             // ASCII ???
    {0x309B, J_VK_DEAD_VOICED_SOUND},
    {0x309C, J_VK_DEAD_SEMIVOICED_SOUND},
    {0,0}
};

// ANSI CP identifiers are no longer than this
#define MAX_ACP_STR_LEN 7

static void BuildDynamicKeyMapTable()
{
    HKL hkl = GetKeyboardLayout(0);
    // Will need this to reset layout after dead keys.
    UINT spaceScanCode = MapVirtualKeyEx(VK_SPACE, 0, hkl);
    DynamicKeyMapEntry *dynamic;

    LANGID idLang = LOWORD(GetKeyboardLayout(0));
    UINT codePage;
    TCHAR strCodePage[MAX_ACP_STR_LEN];
    // use the LANGID to create a LCID
    LCID idLocale = MAKELCID(idLang, SORT_DEFAULT);
    // get the ANSI code page associated with this locale
    if (GetLocaleInfo(idLocale, LOCALE_IDEFAULTANSICODEPAGE,
    strCodePage, sizeof(strCodePage)/sizeof(TCHAR)) > 0 )
    {
        codePage = _ttoi(strCodePage);
    } else {
        codePage = GetACP();
    }

    // Entries in dynamic table that maps between Java VK and Windows
    // VK are built in three steps:
    //   1. Map windows VK to ANSI character (cannot map to unicode
    //      directly, since ::ToUnicode is not implemented on win9x)
    //   2. Convert ANSI char to Unicode char
    //   3. Map Unicode char to Java VK via two auxilary tables.

    for (dynamic = dynamicKeyMapTable; dynamic->windowsKey != 0; ++dynamic)
    {
        char cbuf[2] = { '\0', '\0'};
        WCHAR ucbuf[2] = { L'\0', L'\0' };
        int nchars;
        UINT scancode;
        const CharToVKEntry *charMap;
        int nconverted;
        WCHAR uc;
        BYTE kbdState[256];

        // Defaults to J_VK_UNDEFINED
        dynamic->javaKey = J_VK_UNDEFINED;

        GetKeyboardState(kbdState);

        kbdState[dynamic->windowsKey] |=  0x80; // Press the key.

        // Unpress modifiers, since they are most likely pressed as
        // part of the keyboard switching shortcut.
        kbdState[VK_CONTROL] &= ~0x80;
        kbdState[VK_SHIFT]   &= ~0x80;
        kbdState[VK_MENU]    &= ~0x80;

        scancode = MapVirtualKeyEx(dynamic->windowsKey, 0, hkl);
        nchars = ToAsciiEx(dynamic->windowsKey, scancode, kbdState,
                                 (WORD*)cbuf, 0, hkl);

        // Auxiliary table used to map Unicode character to Java VK.
        // Will assign a different table for dead keys (below).
        charMap = charToVKTable;

        if (nchars < 0) { // Dead key
            char junkbuf[2] = { '\0', '\0'};
            // Use a different table for dead chars since different layouts
            // return different characters for the same dead key.
            charMap = charToDeadVKTable;

            // We also need to reset layout so that next translation
            // is unaffected by the dead status.  We do this by
            // translating <SPACE> key.
            kbdState[dynamic->windowsKey] &= ~0x80;
            kbdState[VK_SPACE] |= 0x80;

            ToAsciiEx(VK_SPACE, spaceScanCode, kbdState,
                      (WORD*)junkbuf, 0, hkl);
        }

        nconverted = MultiByteToWideChar(codePage, 0,
                                         cbuf, 1, ucbuf, 2);

        uc = ucbuf[0];
        {
            const CharToVKEntry *map;
            for (map = charMap;  map->c != 0;  ++map) {
                if (uc == map->c) {
                    dynamic->javaKey = map->javaKey;
                    break;
                }
            }
        }

    } // for each VK_OEM_*
}

static jint GetModifiers() {
    jint modifiers = 0;
    // have to do &0xFFFF to avoid runtime assert caused by compiling with
    // /RTCcsu
    if (HIBYTE((GetKeyState(VK_CONTROL) & 0xFFFF)) != 0) {
        modifiers |= EVENT_CTRL_MASK;
    }
    if (HIBYTE((GetKeyState(VK_SHIFT) & 0xFFFF)) != 0) {
        modifiers |= EVENT_SHIFT_MASK;
    }
    if (HIBYTE((GetKeyState(VK_MENU) & 0xFFFF)) != 0) {
        modifiers |= EVENT_ALT_MASK;
    }
    if (HIBYTE((GetKeyState(VK_LBUTTON) & 0xFFFF)) != 0) {
        modifiers |= EVENT_BUTTON1_MASK;
    }
    if (HIBYTE((GetKeyState(VK_MBUTTON) & 0xFFFF)) != 0) {
        modifiers |= EVENT_BUTTON2_MASK;
    }
    if (HIBYTE((GetKeyState(VK_RBUTTON) & 0xFFFF)) != 0) {
        modifiers |= EVENT_BUTTON3_MASK;
    }

    return modifiers;
}

static int WmChar(JNIEnv *env, jobject window, UINT character, UINT repCnt,
                  UINT flags, BOOL system)
{
    // The Alt modifier is reported in the 29th bit of the lParam,
    // i.e., it is the 13th bit of `flags' (which is HIWORD(lParam)).
    BOOL alt_is_down = (flags & (1<<13)) != 0;
    if (system && alt_is_down) {
        if (character == VK_SPACE) {
            return 1;
        }
    }

    if (character == VK_RETURN) {
        character = J_VK_ENTER;
    }
    (*env)->CallVoidMethod(env, window, sendKeyEventID,
                           (jint) EVENT_KEY_TYPED,
                           GetModifiers(),
                           (jint) -1,
                           (jchar) character);
    return 1;
}

UINT WindowsKeyToJavaKey(UINT windowsKey, UINT modifiers)
{
    int i, j;
    // for the general case, use a bi-directional table
    for (i = 0; keyMapTable[i].windowsKey != 0; i++) {
        if (keyMapTable[i].windowsKey == windowsKey) {
            return keyMapTable[i].javaKey;
        }
    }
    for (j = 0; dynamicKeyMapTable[j].windowsKey != 0; j++) {
        if (dynamicKeyMapTable[j].windowsKey == windowsKey) {
            if (dynamicKeyMapTable[j].javaKey != J_VK_UNDEFINED) {
                return dynamicKeyMapTable[j].javaKey;
            } else {
                break;
            }
        }
    }

    return J_VK_UNDEFINED;
}

static int WmKeyDown(JNIEnv *env, jobject window, UINT wkey, UINT repCnt,
                     UINT flags, BOOL system)
{
    UINT modifiers = 0, jkey = 0, character = -1;
    if (wkey == VK_PROCESSKEY) {
        return 1;
    }

    modifiers = GetModifiers();
    jkey = WindowsKeyToJavaKey(wkey, modifiers);

/*
    character = WindowsKeyToJavaChar(wkey, modifiers, SAVE);
*/

    (*env)->CallVoidMethod(env, window, sendKeyEventID,
                           (jint) EVENT_KEY_PRESSED,
                           modifiers,
                           (jint) jkey,
                           (jchar) character);

    /* windows does not create a WM_CHAR for the Del key
       for some reason, so we need to create the KEY_TYPED event on the
       WM_KEYDOWN.
     */
    if (jkey == J_VK_DELETE) {
        (*env)->CallVoidMethod(env, window, sendKeyEventID,
                               (jint) EVENT_KEY_TYPED,
                               GetModifiers(),
                               (jint) -1,
                               (jchar) '\177');
    }

    return 0;
}

static int WmKeyUp(JNIEnv *env, jobject window, UINT wkey, UINT repCnt,
                   UINT flags, BOOL system)
{
    UINT modifiers = 0, jkey = 0, character = -1;
    if (wkey == VK_PROCESSKEY) {
        return 1;
    }

    modifiers = GetModifiers();
    jkey = WindowsKeyToJavaKey(wkey, modifiers);
/*
    character = WindowsKeyToJavaChar(wkey, modifiers, SAVE);
*/

    (*env)->CallVoidMethod(env, window, sendKeyEventID,
                           (jint) EVENT_KEY_RELEASED,
                           modifiers,
                           (jint) jkey,
                           (jchar) character);

    return 0;
}

static void NewtWindows_requestFocus (JNIEnv *env, jobject window, HWND hwnd, jboolean force) {
    HWND pHwnd, current;
    pHwnd = GetParent(hwnd);
    current = GetFocus();
    DBG_PRINT("*** WindowsWindow: requestFocus.S parent %p, window %p, isCurrent %d\n", 
        (void*) pHwnd, (void*)hwnd, current==hwnd);
    if( JNI_TRUE==force || current!=hwnd) {
        if( JNI_TRUE==force || JNI_FALSE == (*env)->CallBooleanMethod(env, window, focusActionID) ) {
            UINT flags = SWP_SHOWWINDOW | SWP_NOSIZE | SWP_NOMOVE;
            SetWindowPos(hwnd, HWND_TOP, 0, 0, 0, 0, flags);
            SetForegroundWindow(hwnd);  // Slightly Higher Priority
            SetFocus(hwnd);// Sets Keyboard Focus To Window
            if(NULL!=pHwnd) {
                SetActiveWindow(hwnd);
            }
            DBG_PRINT("*** WindowsWindow: requestFocus.X1\n");
        } else {
            DBG_PRINT("*** WindowsWindow: requestFocus.X0\n");
        }
    }
    DBG_PRINT("*** WindowsWindow: requestFocus.XX\n");
}

#if 0

static RECT* UpdateInsets(JNIEnv *env, jobject window, HWND hwnd)
{
    // being naughty here
    static RECT m_insets = { 0, 0, 0, 0 };
    RECT outside;
    RECT inside;
    POINT *rp_inside = (POINT *) (void *) &inside;
    int dx, dy, dw, dh;

    if (IsIconic(hwnd)) {
        m_insets.left = m_insets.top = m_insets.right = m_insets.bottom = -1;
        return FALSE;
    }

    m_insets.left = m_insets.top = m_insets.right = m_insets.bottom = 0;

    GetClientRect(hwnd, &inside);
    GetWindowRect(hwnd, &outside);

    DBG_PRINT("*** WindowsWindow: UpdateInsets (a1) window %p, Inside CC: %d/%d - %d/%d %dx%d\n", 
        (void*)hwnd,
        (int)inside.left, (int)inside.top, (int)inside.right, (int)inside.bottom, 
        (int)(inside.right - inside.left), (int)(inside.bottom - inside.top));
    DBG_PRINT("*** WindowsWindow: UpdateInsets (a1) window %p, Outside SC: %d/%d - %d/%d %dx%d\n", 
        (void*)hwnd,
        (int)outside.left, (int)outside.top, (int)outside.right, (int)outside.bottom, 
        (int)(outside.right - outside.left), (int)(outside.bottom - outside.top));

    // xform client -> screen coord
    ClientToScreen(hwnd, rp_inside);
    ClientToScreen(hwnd, rp_inside+1);

    DBG_PRINT("*** WindowsWindow: UpdateInsets (a2) window %p, Inside SC: %d/%d - %d/%d %dx%d\n", 
        (void*)hwnd,
        (int)inside.left, (int)inside.top, (int)inside.right, (int)inside.bottom, 
        (int)(inside.right - inside.left), (int)(inside.bottom - inside.top));

    m_insets.top = inside.top - outside.top;
    m_insets.bottom = outside.bottom - inside.bottom;
    m_insets.left = inside.left - outside.left;
    m_insets.right = outside.right - inside.right;

    DBG_PRINT("*** WindowsWindow: UpdateInsets (1.0) window %p, %d/%d - %d/%d %dx%d\n", 
        (void*)hwnd, 
        (int)m_insets.left, (int)m_insets.top, (int)m_insets.right, (int)m_insets.bottom,
        (int)(m_insets.right-m_insets.left), (int)(m_insets.top-m_insets.bottom));

    (*env)->CallVoidMethod(env, window, insetsChangedID,
                           m_insets.left, m_insets.top,
                           m_insets.right, m_insets.bottom);
    return &m_insets;
}

#else

static RECT* UpdateInsets(JNIEnv *env, jobject window, HWND hwnd)
{
    // being naughty here
    static RECT m_insets = { 0, 0, 0, 0 };
    RECT outside;
    RECT inside;

    if (IsIconic(hwnd)) {
        m_insets.left = m_insets.top = m_insets.right = m_insets.bottom = -1;
        return FALSE;
    }

    m_insets.left = m_insets.top = m_insets.right = m_insets.bottom = 0;

    GetClientRect(hwnd, &inside);
    GetWindowRect(hwnd, &outside);

    if (outside.right - outside.left > 0 && outside.bottom - outside.top > 0) {
        MapWindowPoints(hwnd, 0, (LPPOINT)&inside, 2);
        m_insets.top = inside.top - outside.top;
        m_insets.bottom = outside.bottom - inside.bottom;
        m_insets.left = inside.left - outside.left;
        m_insets.right = outside.right - inside.right;
    } else {
        m_insets.top = -1;
    }
    if (m_insets.left < 0 || m_insets.top < 0 ||
        m_insets.right < 0 || m_insets.bottom < 0)
    {
        LONG style = GetWindowLong(hwnd, GWL_STYLE);
        // TODO: TDV: better undecorated checking needed

        BOOL bIsUndecorated = (style & (WS_CHILD|WS_POPUP|WS_SYSMENU)) != 0;
        if (!bIsUndecorated) {
            /* Get outer frame sizes. */
            if (style & WS_THICKFRAME) {
                m_insets.left = m_insets.right =
                    GetSystemMetrics(SM_CXSIZEFRAME);
                m_insets.top = m_insets.bottom =
                    GetSystemMetrics(SM_CYSIZEFRAME);
            } else {
                m_insets.left = m_insets.right =
                    GetSystemMetrics(SM_CXDLGFRAME);
                m_insets.top = m_insets.bottom =
                    GetSystemMetrics(SM_CYDLGFRAME);
            }

            /* Add in title. */
            m_insets.top += GetSystemMetrics(SM_CYCAPTION);
        } else {
            /* undo the -1 set above */
            m_insets.left = m_insets.top = m_insets.right = m_insets.bottom = 0;
        }
    }

    DBG_PRINT("*** WindowsWindow: UpdateInsets window %p, %d/%d %dx%d\n", 
        (void*)hwnd, (int)m_insets.left, (int)m_insets.top, (int)(m_insets.right-m_insets.left), (int)(m_insets.top-m_insets.bottom));

    (*env)->CallVoidMethod(env, window, insetsChangedID,
                           m_insets.left, m_insets.top,
                           m_insets.right, m_insets.bottom);
    return &m_insets;
}

#endif

static void WmSize(JNIEnv *env, jobject window, HWND wnd, UINT type)
{
    RECT rc;
    int w, h;
    BOOL isVisible = IsWindowVisible(wnd);

    // make sure insets are up to date
    (void)UpdateInsets(env, window, wnd);

    if (type == SIZE_MINIMIZED) {
        // TODO: deal with minimized window sizing
        return;
    }

    GetClientRect(wnd, &rc);
    
    // we report back the dimensions of the client area
    w = rc.right  - rc.left;
    h = rc.bottom - rc.top;

    DBG_PRINT("*** WindowsWindow: WmSize window %p, %dx%d, visible %d\n", (void*)wnd, w, h, isVisible);

    if(isVisible) {
        (*env)->CallVoidMethod(env, window, sizeChangedID, w, h, JNI_FALSE);
    }
}

static LRESULT CALLBACK wndProc(HWND wnd, UINT message,
                                WPARAM wParam, LPARAM lParam)
{
    LRESULT res = 0;
    int useDefWindowProc = 0;
    JNIEnv *env = NULL;
    jobject window = NULL;
    BOOL isKeyDown = FALSE;
    WindowUserData * wud;

#ifdef DEBUG_KEYS
    if (  WM_KEYDOWN == message ) {
        STD_PRINT("*** WindowsWindow: wndProc window %p, 0x%X %d/%d\n", wnd, message, (int)LOWORD(lParam), (int)HIWORD(lParam));
    }
#endif

#if !defined(__MINGW64__) && ( defined(UNDER_CE) || _MSC_VER <= 1200 )
    wud = (WindowUserData *) GetWindowLong(wnd, GWL_USERDATA);
#else
    wud = (WindowUserData *) GetWindowLongPtr(wnd, GWLP_USERDATA);
#endif
    if(NULL==wud) {
        return DefWindowProc(wnd, message, wParam, lParam);
    }
    env = wud->jenv;
    window = wud->jinstance;

    // DBG_PRINT("*** WindowsWindow: thread 0x%X - window %p -> %p, 0x%X %d/%d\n", (int)GetCurrentThreadId(), wnd, window, message, (int)LOWORD(lParam), (int)HIWORD(lParam));

    if (NULL==window || NULL==env) {
        return DefWindowProc(wnd, message, wParam, lParam);
    }

    switch (message) {

    //
    // The signal pipeline for destruction is:
    //    Java::DestroyWindow(wnd) _or_ window-close-button -> 
    //     WM_CLOSE -> Java::windowDestroyNotify -> W_DESTROY -> Java::windowDestroyed ->
    //       Java::CleanupWindowResources()
    case WM_CLOSE:
        (*env)->CallVoidMethod(env, window, windowDestroyNotifyID);
        break;

    case WM_DESTROY:
        {
#if !defined(__MINGW64__) && ( defined(UNDER_CE) || _MSC_VER <= 1200 )
            SetWindowLong(wnd, GWL_USERDATA, (intptr_t) NULL);
#else
            SetWindowLongPtr(wnd, GWLP_USERDATA, (intptr_t) NULL);
#endif
            free(wud); wud=NULL;
            (*env)->CallVoidMethod(env, window, windowDestroyedID);
            (*env)->DeleteGlobalRef(env, window);
        }
        break;

    case WM_SYSCHAR:
        useDefWindowProc = WmChar(env, window, wParam,
                                  LOWORD(lParam), HIWORD(lParam), FALSE);
        break;

    case WM_CHAR:
        useDefWindowProc = WmChar(env, window, wParam,
                                  LOWORD(lParam), HIWORD(lParam), TRUE);
        break;
        
    case WM_KEYDOWN:
#ifdef DEBUG_KEYS
        STD_PRINT("*** WindowsWindow: windProc sending window %p -> %p, 0x%X %d/%d\n", wnd, window, message, (int)LOWORD(lParam), (int)HIWORD(lParam));
#endif
        useDefWindowProc = WmKeyDown(env, window, wParam,
                                     LOWORD(lParam), HIWORD(lParam), FALSE);
        break;

    case WM_KEYUP:
        useDefWindowProc = WmKeyUp(env, window, wParam,
                                   LOWORD(lParam), HIWORD(lParam), FALSE);
        break;

    case WM_SIZE:
        WmSize(env, window, wnd, (UINT)wParam);
        break;

    case WM_SETTINGCHANGE:
        if (wParam == SPI_SETNONCLIENTMETRICS) {
            // make sure insets are updated, we don't need to resize the window 
            // because the size of the client area doesn't change
            (void)UpdateInsets(env, window, wnd);
        } else {
            useDefWindowProc = 1;
        }
        break;


    case WM_LBUTTONDOWN:
        DBG_PRINT("*** WindowsWindow: LBUTTONDOWN\n");
        (*env)->CallVoidMethod(env, window, enqueueRequestFocusID, JNI_FALSE);
        (*env)->CallVoidMethod(env, window, sendMouseEventID,
                               (jint) EVENT_MOUSE_PRESSED,
                               GetModifiers(),
                               (jint) LOWORD(lParam), (jint) HIWORD(lParam),
                               (jint) 1, (jint) 0);
        useDefWindowProc = 1;
        break;

    case WM_LBUTTONUP:
        (*env)->CallVoidMethod(env, window, sendMouseEventID,
                               (jint) EVENT_MOUSE_RELEASED,
                               GetModifiers(),
                               (jint) LOWORD(lParam), (jint) HIWORD(lParam),
                               (jint) 1, (jint) 0);
        useDefWindowProc = 1;
        break;

    case WM_MBUTTONDOWN:
        DBG_PRINT("*** WindowsWindow: MBUTTONDOWN\n");
        (*env)->CallVoidMethod(env, window, enqueueRequestFocusID, JNI_FALSE);
        (*env)->CallVoidMethod(env, window, sendMouseEventID,
                               (jint) EVENT_MOUSE_PRESSED,
                               GetModifiers(),
                               (jint) LOWORD(lParam), (jint) HIWORD(lParam),
                               (jint) 2, (jint) 0);
        useDefWindowProc = 1;
        break;

    case WM_MBUTTONUP:
        (*env)->CallVoidMethod(env, window, sendMouseEventID,
                               (jint) EVENT_MOUSE_RELEASED,
                               GetModifiers(),
                               (jint) LOWORD(lParam), (jint) HIWORD(lParam),
                               (jint) 2, (jint) 0);
        useDefWindowProc = 1;
        break;

    case WM_RBUTTONDOWN:
        DBG_PRINT("*** WindowsWindow: RBUTTONDOWN\n");
        (*env)->CallVoidMethod(env, window, enqueueRequestFocusID, JNI_FALSE);
        (*env)->CallVoidMethod(env, window, sendMouseEventID,
                               (jint) EVENT_MOUSE_PRESSED,
                               GetModifiers(),
                               (jint) LOWORD(lParam), (jint) HIWORD(lParam),
                               (jint) 3, (jint) 0);
        useDefWindowProc = 1;
        break;

    case WM_RBUTTONUP:
        (*env)->CallVoidMethod(env, window, sendMouseEventID,
                               (jint) EVENT_MOUSE_RELEASED,
                               GetModifiers(),
                               (jint) LOWORD(lParam), (jint) HIWORD(lParam),
                               (jint) 3,  (jint) 0);
        useDefWindowProc = 1;
        break;

    case WM_MOUSEMOVE:
        (*env)->CallVoidMethod(env, window, sendMouseEventID,
                               (jint) EVENT_MOUSE_MOVED,
                               GetModifiers(),
                               (jint) LOWORD(lParam), (jint) HIWORD(lParam),
                               (jint) 0,  (jint) 0);
        useDefWindowProc = 1;
        break;

    case WM_MOUSEWHEEL: {
        // need to convert the coordinates to component-relative
        int x = GET_X_LPARAM(lParam);
        int y = GET_Y_LPARAM(lParam);
        POINT eventPt;
        eventPt.x = x;
        eventPt.y = y;
        ScreenToClient(wnd, &eventPt);
        (*env)->CallVoidMethod(env, window, sendMouseEventID,
                               (jint) EVENT_MOUSE_WHEEL_MOVED,
                               GetModifiers(),
                               (jint) eventPt.x, (jint) eventPt.y,
                               (jint) 0,  (jint) (GET_WHEEL_DELTA_WPARAM(wParam)/120.0f));
        useDefWindowProc = 1;
        break;
    }

    case WM_SETFOCUS:
        (*env)->CallVoidMethod(env, window, focusChangedID, JNI_TRUE);
        useDefWindowProc = 1;
        break;

    case WM_KILLFOCUS:
        (*env)->CallVoidMethod(env, window, focusChangedID, JNI_FALSE);
        useDefWindowProc = 1;
        break;

    case WM_SHOWWINDOW:
        (*env)->CallVoidMethod(env, window, visibleChangedID, wParam==TRUE?JNI_TRUE:JNI_FALSE);
        break;

    case WM_MOVE:
        DBG_PRINT("*** WindowsWindow: WM_MOVE window %p, %d/%d\n", wnd, (int)LOWORD(lParam), (int)HIWORD(lParam));
        (*env)->CallVoidMethod(env, window, positionChangedID,
                               (jint)LOWORD(lParam), (jint)HIWORD(lParam));
        useDefWindowProc = 1;
        break;

    case WM_PAINT: {
        RECT r;
        useDefWindowProc = 0;
        if (GetUpdateRect(wnd, &r, TRUE /* erase background */)) {
            /*
            jint width = r.right-r.left;
            jint height = r.bottom-r.top;
            if (width > 0 && height > 0) {
                (*env)->CallVoidMethod(env, window, windowRepaintID, r.left, r.top, width, height);
            }
            ValidateRect(wnd, &r);
            */
        }
        break;
    }
    case WM_ERASEBKGND:
        // ignore erase background
        (*env)->CallVoidMethod(env, window, windowRepaintID, 0, 0, -1, -1);
        useDefWindowProc = 0;
        res = 1; // OpenGL, etc .. erases the background, hence we claim to have just done this
        break;


    // FIXME: generate EVENT_MOUSE_ENTERED, EVENT_MOUSE_EXITED
    default:
        useDefWindowProc = 1;
    }

    if (useDefWindowProc)
        return DefWindowProc(wnd, message, wParam, lParam);
    return res;
}

/*
 * Class:     com_jogamp_newt_impl_windows_WindowsDisplay
 * Method:    DispatchMessages
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_windows_WindowsDisplay_DispatchMessages0
  (JNIEnv *env, jclass clazz)
{
    int i = 0;
    MSG msg;
    BOOL gotOne;

    // Periodically take a break
    do {
        gotOne = PeekMessage(&msg, (HWND) NULL, 0, 0, PM_REMOVE);
        // DBG_PRINT("*** WindowsWindow.DispatchMessages0: thread 0x%X - gotOne %d\n", (int)GetCurrentThreadId(), (int)gotOne);
        if (gotOne) {
            ++i;
#ifdef DEBUG_KEYS
            if(WM_KEYDOWN == msg.message) {
                STD_PRINT("*** WindowsWindow: DispatchMessages window %p, 0x%X %d/%d\n", msg.hwnd, msg.message, (int)LOWORD(msg.lParam), (int)HIWORD(msg.lParam));
            }
#endif
            TranslateMessage(&msg);
            DispatchMessage(&msg);
        }
    } while (gotOne && i < 100);
}

/*
 * Class:     com_jogamp_newt_impl_windows_WindowsDisplay
 * Method:    LoadLibraryW
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_com_jogamp_newt_impl_windows_WindowsDisplay_LoadLibraryW0
  (JNIEnv *env, jclass clazz, jstring dllName)
{
    jchar* _dllName = NewtCommon_GetNullTerminatedStringChars(env, dllName);
    HMODULE lib = LoadLibraryW(_dllName);
    free(_dllName);
    return (jlong) (intptr_t) lib;
}

/*
 * Class:     com_jogamp_newt_impl_windows_WindowsDisplay
 * Method:    RegisterWindowClass
 * Signature: (Ljava/lang/String;J)I
 */
JNIEXPORT jint JNICALL Java_com_jogamp_newt_impl_windows_WindowsDisplay_RegisterWindowClass0
  (JNIEnv *env, jclass clazz, jstring wndClassName, jlong hInstance)
{
    ATOM res;
    WNDCLASS wc;
#ifndef UNICODE
    const char* _wndClassName = NULL;
#endif

    /* register class */
    wc.style = CS_HREDRAW | CS_VREDRAW;
    wc.lpfnWndProc = (WNDPROC)wndProc;
    wc.cbClsExtra = 0;
    wc.cbWndExtra = 0;
    /* This cast is legal because the HMODULE for a DLL is the same as
       its HINSTANCE -- see MSDN docs for DllMain */
    wc.hInstance = (HINSTANCE) (intptr_t) hInstance;
    wc.hIcon = NULL;
    wc.hCursor = LoadCursor(NULL, MAKEINTRESOURCE(IDC_ARROW));
    wc.hbrBackground = GetStockObject(BLACK_BRUSH);
    wc.lpszMenuName = NULL;
#ifdef UNICODE
    wc.lpszClassName = NewtCommon_GetNullTerminatedStringChars(env, wndClassName);
#else
    _wndClassName = (*env)->GetStringUTFChars(env, wndClassName, NULL);
    wc.lpszClassName = strdup(_wndClassName);
    (*env)->ReleaseStringUTFChars(env, wndClassName, _wndClassName);
#endif
    res = RegisterClass(&wc);

    free((void *)wc.lpszClassName);

    return (jint)res;
}

/*
 * Class:     com_jogamp_newt_impl_windows_WindowsDisplay
 * Method:    CleanupWindowResources
 * Signature: (java/lang/String;J)V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_windows_WindowsDisplay_UnregisterWindowClass0
  (JNIEnv *env, jclass clazz, jint wndClassAtom, jlong hInstance)
{
    UnregisterClass(MAKEINTATOM(wndClassAtom), (HINSTANCE) (intptr_t) hInstance);
}

/*
 * Class:     com_jogamp_newt_impl_windows_WindowsScreen
 * Method:    getWidthImpl
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_jogamp_newt_impl_windows_WindowsScreen_getWidthImpl0
  (JNIEnv *env, jobject obj, jint scrn_idx)
{
    return (jint)GetSystemMetrics(SM_CXSCREEN);
}

/*
 * Class:     com_jogamp_newt_impl_windows_WindowsScreen
 * Method:    getHeightImpl
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_jogamp_newt_impl_windows_WindowsScreen_getHeightImpl0
  (JNIEnv *env, jobject obj, jint scrn_idx)
{
    return (jint)GetSystemMetrics(SM_CYSCREEN);
}

static int NewtScreen_RotationNative2Newt(int native) {
    int rot;
    switch (native) {
        case DMDO_DEFAULT:
            rot = 0;
            break;
        case DMDO_270:
            rot = 270;
            break;
        case DMDO_180:
            rot = 180;
            break;
        case DMDO_90:
            rot = 90;
            break;
        default:
            NewtCommon_throwNewRuntimeException(env, "invalid native rotation: %d", native);
        break;
    }
    return rot;
}

static LPCTSTR NewtScreen_getDisplayDeviceName(int scrn_idx) {
    DISPLAY_DEVICE device;

    if( FALSE == EnumDisplayDevices(NULL, scrn_idx, &device, 0) ) {
        return NULL;
    }

    if( 0 == ( device.StateFlags & DISPLAY_DEVICE_ACTIVE ) ) {
        return NULL;
    }

    return device.DeviceName;
}

static HDC NewtScreen_createDisplayDC(LPCTSTR displayDeviceName) {
    return CreateDC("DISPLAY", displayDeviceName, NULL, NULL);
}

/*
 * Class:     com_jogamp_newt_impl_windows_WindowsScreen
 * Method:    getScreenMode0
 * Signature: (II)[I
 */
JNIEXPORT jintArray JNICALL Java_com_jogamp_newt_impl_windows_WindowsScreen_getScreenMode0
  (JNIEnv *env, jobject obj, jint scrn_idx, jint mode_idx)
{
    int prop_num = NUM_SCREEN_MODE_PROPERTIES_ALL;
    LPCTSTR deviceName = NewtScreen_getDisplayDeviceName(scrn_idx);
    if(NULL == deviceName) {
        return null;
    }

    int widthmm, heightmm;
    {
        HDC hdc = NewtScreen_createDisplayDC(deviceName);
        widthmm = GetDeviceCaps(hdc, HORZSIZE);
        heightmm = GetDeviceCaps(hdc, VERTSIZE);
        DeleteDC(hdc);
    }

    DEVMODE dm;
    ZeroMemory(&dm, sizeof(dm));
    dm.dmSize = sizeof(dm);
    
    int devModeID = (int)mode_idx;
    
    if(devModeID == -1) {
        devModeID = ENUM_CURRENT_SETTINGS;
        prop_num++; // add 1st extra prop, mode_idx
    }

    if (0 == EnumDisplaySettings(deviceName, devModeID, &dm))
    {
        return NULL;
    }

    
    jint prop[ prop_num ];
    int propIndex = 0;

    if(devModeID == -1) {
        prop[propIndex++] = mode_idx;
    }
    prop[propIndex++] = 0; // set later for verification of iterator
    prop[propIndex++] = dm.dmPelsWidth;
    prop[propIndex++] = dm.dmPelsHeight;
    prop[propIndex++] = dm.dmBitsPerPel;
    prop[propIndex++] = widthmm;
    prop[propIndex++] = heightmm;
    prop[propIndex++] = dm.dmDisplayFrequency;
    prop[propIndex++] = NewtScreen_RotationNative2Newt(dm.dmDisplayOrientation);
    props[propIndex - NUM_SCREEN_MODE_PROPERTIES_ALL] = propIndex ; // count 

    jintArray properties = (*env)->NewIntArray(env, prop_num);
    if (properties == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array of size %d", prop_num);
    }
    (*env)->SetIntArrayRegion(env, properties, 0, prop_num, prop);
    
    return properties;
}

/*
 * Class:     com_jogamp_newt_impl_windows_WindowsScreen
 * Method:    setScreenMode0
 * Signature: (IIIIII)Z
 */
JNIEXPORT jboolean JNICALL Java_com_jogamp_newt_impl_windows_WindowsScreen_setScreenMode0
  (JNIEnv *env, jobject object, jint scrn_idx, jint width, jint height, jint bits, jint rate, jint rot)
{
    LPCTSTR deviceName = NewtScreen_getDisplayDeviceName(scrn_idx);
    if(NULL == deviceName) {
        return null;
    }

    DEVMODE dm;
    // initialize the DEVMODE structure
    ZeroMemory(&dm, sizeof(dm));
    dm.dmSize = sizeof(dm);

    if (0 == EnumDisplaySettings(deviceName, ENUM_CURRENT_SETTINGS, &dm)) {
        return JNI_FALSE;
    }
    
    dm.dmPelsWidth = (int)width;
    dm.dmPelsHeight = (int)height;
    dm.dmBitsPerPel = (int)bits;
    dm.dmDisplayFrequency = (int)rate;
    dm.dmFields = DM_PELSWIDTH | DM_PELSHEIGHT | DM_BITSPERPEL | DM_DISPLAYFREQUENCY;
    
    int requestedRotation = dm.dmDisplayOrientation;
    int currentRotation = dm.dmDisplayOrientation;
    
    int shouldFlipDims = 0;
    
    int rotation = (int)rot;
    switch (rotation)
    {
        case 0:
            requestedRotation = DMDO_DEFAULT;
            if (currentRotation == DMDO_90 || currentRotation == DMDO_270) 
            {
                shouldFlipDims = 1;
            }
        break;
        case 270:
            requestedRotation = DMDO_270;
            if (currentRotation == DMDO_DEFAULT || currentRotation == DMDO_180) 
            {
                shouldFlipDims = 1;
            }
        break;
        case 180:
            requestedRotation = DMDO_180;
            if (currentRotation == DMDO_90 || currentRotation == DMDO_270) 
            {
                shouldFlipDims = 1;
            }
        break;
        case 90:
            requestedRotation = DMDO_90;
            if (currentRotation == DMDO_DEFAULT || currentRotation == DMDO_180) 
            {
                shouldFlipDims = 1;
            }
        break;
        default:
            //requested rotation not available
            return SCREEN_MODE_ERROR; 
        break;
    }
    /** swap width and height if changing from vertical to horizantal 
      *  or horizantal to vertical
      */
    if (shouldFlipDims)
    {
        int tempWidth = dm.dmPelsWidth;
        dm.dmPelsWidth = dm.dmPelsHeight;
        dm.dmPelsHeight = tempWidth;
    }
    dm.dmDisplayOrientation = requestedRotation;
    dm.dmFields = DM_DISPLAYORIENTATION | DM_PELSWIDTH | DM_PELSHEIGHT | DM_BITSPERPEL | DM_DISPLAYFREQUENCY;
    
    return ( DISP_CHANGE_SUCCESSFUL == ChangeDisplaySettings(&dm, 0) ) ? JNI_TRUE : JNI_FALSE ;
}

/*
 * Class:     com_jogamp_newt_impl_windows_WindowsWindow
 * Method:    initIDs0
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_jogamp_newt_impl_windows_WindowsWindow_initIDs0
  (JNIEnv *env, jclass clazz)
{
    NewtCommon_init(env);

    if(NULL==pointClz) {
        jclass c = (*env)->FindClass(env, ClazzNamePoint);
        if(NULL==c) {
            _FatalError(env, "NEWT WindowsWindows: can't find %s", ClazzNamePoint);
        }
        pointClz = (jclass)(*env)->NewGlobalRef(env, c);
        (*env)->DeleteLocalRef(env, c);
        if(NULL==pointClz) {
            _FatalError(env, "NEWT WindowsWindows: can't use %s", ClazzNamePoint);
        }
        pointCstr = (*env)->GetMethodID(env, pointClz, ClazzAnyCstrName, ClazzNamePointCstrSignature);
        if(NULL==pointCstr) {
            _FatalError(env, "NEWT WindowsWindows: can't fetch %s.%s %s",
                ClazzNamePoint, ClazzAnyCstrName, ClazzNamePointCstrSignature);
        }
    }

    insetsChangedID = (*env)->GetMethodID(env, clazz, "insetsChanged", "(IIII)V");
    sizeChangedID = (*env)->GetMethodID(env, clazz, "sizeChanged", "(IIZ)V");
    positionChangedID = (*env)->GetMethodID(env, clazz, "positionChanged", "(II)V");
    focusChangedID = (*env)->GetMethodID(env, clazz, "focusChanged", "(Z)V");
    visibleChangedID = (*env)->GetMethodID(env, clazz, "visibleChanged", "(Z)V");
    windowDestroyNotifyID    = (*env)->GetMethodID(env, clazz, "windowDestroyNotify", "()V");
    windowDestroyedID = (*env)->GetMethodID(env, clazz, "windowDestroyed", "()V");
    windowRepaintID = (*env)->GetMethodID(env, clazz, "windowRepaint", "(IIII)V");
    enqueueMouseEventID = (*env)->GetMethodID(env, clazz, "enqueueMouseEvent", "(ZIIIIII)V");
    sendMouseEventID = (*env)->GetMethodID(env, clazz, "sendMouseEvent", "(IIIIII)V");
    enqueueKeyEventID = (*env)->GetMethodID(env, clazz, "enqueueKeyEvent", "(ZIIIC)V");
    sendKeyEventID = (*env)->GetMethodID(env, clazz, "sendKeyEvent", "(IIIC)V");
    enqueueRequestFocusID = (*env)->GetMethodID(env, clazz, "enqueueRequestFocus", "(Z)V");
    focusActionID = (*env)->GetMethodID(env, clazz, "focusAction", "()Z");

    if (insetsChangedID == NULL ||
        sizeChangedID == NULL ||
        positionChangedID == NULL ||
        focusChangedID == NULL ||
        visibleChangedID == NULL ||
        windowDestroyNotifyID == NULL ||
        windowDestroyedID == NULL ||
        windowRepaintID == NULL ||
        enqueueMouseEventID == NULL ||
        sendMouseEventID == NULL ||
        enqueueKeyEventID == NULL ||
        sendKeyEventID == NULL ||
        focusActionID == NULL ||
        enqueueRequestFocusID == NULL) {
        return JNI_FALSE;
    }
    BuildDynamicKeyMapTable();
    return JNI_TRUE;
}

/*
 * Class:     com_jogamp_newt_impl_windows_WindowsWindow
 * Method:    CreateWindow
 * Signature: (JILjava/lang/String;JJZIIII)J
 */
JNIEXPORT jlong JNICALL Java_com_jogamp_newt_impl_windows_WindowsWindow_CreateWindow0
  (JNIEnv *env, jobject obj, jlong parent, jint wndClassAtom, jstring jWndName, jlong hInstance, jlong visualID,
        jboolean bIsUndecorated,
        jint jx, jint jy, jint defaultWidth, jint defaultHeight)
{
    HWND parentWindow = (HWND) (intptr_t) parent;
    const TCHAR* wndName = NULL;
    DWORD windowStyle = WS_CLIPSIBLINGS | WS_CLIPCHILDREN | WS_VISIBLE | WS_TABSTOP;
    int x=(int)jx, y=(int)jy;
    int width=(int)defaultWidth, height=(int)defaultHeight;
    HWND window = NULL;

#ifdef UNICODE
    wndName = NewtCommon_GetNullTerminatedStringChars(env, jWndName);
#else
    wndName = (*env)->GetStringUTFChars(env, jWndName, NULL);
#endif

    if(NULL!=parentWindow) {
        if (!IsWindow(parentWindow)) {
            DBG_PRINT("*** WindowsWindow: CreateWindow failure: Passed parentWindow %p is invalid\n", parentWindow);
            return 0;
        }
        windowStyle |= WS_CHILD ;
    } else if (bIsUndecorated) {
        windowStyle |= WS_POPUP | WS_SYSMENU | WS_MAXIMIZEBOX | WS_MINIMIZEBOX;
    } else {
        windowStyle |= WS_OVERLAPPEDWINDOW;
        x = CW_USEDEFAULT;
        y = 0;
    }

    (void) visualID; // FIXME: use the visualID ..

    window = CreateWindow(MAKEINTATOM(wndClassAtom), wndName, windowStyle,
                          x, y, width, height,
                          parentWindow, NULL,
                          (HINSTANCE) (intptr_t) hInstance,
                          NULL);

    DBG_PRINT("*** WindowsWindow: CreateWindow thread 0xX, parent %p, window %p, %d/%d %dx%d\n", 
        (int)GetCurrentThreadId(), parentWindow, window, x, y, width, height);

    if (NULL == window) {
        int lastError = (int) GetLastError();
        DBG_PRINT("*** WindowsWindow: CreateWindow failure: 0x%X %d\n", lastError, lastError);
        return 0;
    } else {
        WindowUserData * wud = (WindowUserData *) malloc(sizeof(WindowUserData));
        wud->jinstance = (*env)->NewGlobalRef(env, obj);
        wud->jenv = env;
#if !defined(__MINGW64__) && ( defined(UNDER_CE) || _MSC_VER <= 1200 )
        SetWindowLong(window, GWL_USERDATA, (intptr_t) wud);
#else
        SetWindowLongPtr(window, GWLP_USERDATA, (intptr_t) wud);
#endif

        UpdateInsets(env, obj, window);
    }

#ifdef UNICODE
    free((void*) wndName);
#else
    (*env)->ReleaseStringUTFChars(env, jWndName, wndName);
#endif


    return (jlong) (intptr_t) window;
}

/*
 * Class:     com_jogamp_newt_impl_windows_WindowsWindow
 * Method:    DestroyWindow
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_windows_WindowsWindow_DestroyWindow0
  (JNIEnv *env, jobject obj, jlong window)
{
    DBG_PRINT("*** WindowsWindow: DestroyWindow thread 0x%X, window %p\n", (int)GetCurrentThreadId(), window);
    DestroyWindow((HWND) (intptr_t) window);
}

/*
 * Class:     com_jogamp_newt_impl_windows_WindowsWindow
 * Method:    GetDC
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_jogamp_newt_impl_windows_WindowsWindow_GetDC0
  (JNIEnv *env, jobject obj, jlong window)
{
    return (jlong) (intptr_t) GetDC((HWND) (intptr_t) window);
}

/*
 * Class:     com_jogamp_newt_impl_windows_WindowsWindow
 * Method:    ReleaseDC
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_windows_WindowsWindow_ReleaseDC0
  (JNIEnv *env, jobject obj, jlong window, jlong dc)
{
    ReleaseDC((HWND) (intptr_t) window, (HDC) (intptr_t) dc);
}

/*
 * Class:     com_jogamp_newt_impl_windows_WindowsWindow
 * Method:    MonitorFromWindow
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_jogamp_newt_impl_windows_WindowsWindow_MonitorFromWindow0
  (JNIEnv *env, jobject obj, jlong window)
{
    #if (_WIN32_WINNT >= 0x0500 || _WIN32_WINDOWS >= 0x0410 || WINVER >= 0x0500) && !defined(_WIN32_WCE)
        return (jlong) (intptr_t) MonitorFromWindow((HWND) (intptr_t) window, MONITOR_DEFAULTTOPRIMARY);
    #else
        return 0;
    #endif
}

/***
 * returns bits: 1: size change, 2: pos change
 */
int NewtWindow_setVisiblePosSize(JNIEnv *env, jobject obj, HWND hwnd, jboolean top, jboolean visible, 
                                 int x, int y, int width, int height)
{
    UINT flags;
    HWND hWndInsertAfter;
    BOOL bRes;
    int iRes=0;
    int wwidth = width; // final window width
    int wheight = height; // final window height
    
    DBG_PRINT("*** WindowsWindow: NewtWindow_setVisiblePosSize %d/%d %dx%d, top %d, visible %d\n", 
        x, y, width, height, (int)top, (int)visible);

    if(JNI_TRUE == visible) {
        flags = SWP_SHOWWINDOW;
    } else {
        flags = SWP_NOACTIVATE | SWP_NOZORDER;
    }

    if(0>x || 0>y ) {
        flags |= SWP_NOMOVE;
    } else {
        iRes |= 2;
    }
    if(0>=width || 0>=height ) {
        flags |= SWP_NOSIZE;
    } else {
        iRes |= 1;
    }

    if(JNI_TRUE == top) {
        hWndInsertAfter = HWND_TOPMOST;
        if ( 0 == ( flags & SWP_NOSIZE ) ) {

            // since width, height are the size of the client area, we need to add insets
            RECT *pInsets = UpdateInsets(env, obj, hwnd);

            wwidth += pInsets->left + pInsets->right;
            wheight += pInsets->top + pInsets->bottom;
        }
        DBG_PRINT("*** WindowsWindow: NewtWindow_setVisiblePosSize top size w/ insets: %d/%d %dx%d\n", x, y, wwidth, wheight);
    } else {
        hWndInsertAfter = HWND_TOP;
        DBG_PRINT("*** WindowsWindow: NewtWindow_setVisiblePosSize client size: %d/%d %dx%d\n", x, y, wwidth, wheight);
    }

    SetWindowPos(hwnd, hWndInsertAfter, x, y, wwidth, wheight, flags);

    InvalidateRect(hwnd, NULL, TRUE);
    UpdateWindow(hwnd);

    // we report back the size of client area
    (*env)->CallVoidMethod(env, obj, sizeChangedID, (jint) width, (jint) height, JNI_FALSE);

    return iRes;
}

/*
 * Class:     com_jogamp_newt_impl_windows_WindowsWindow
 * Method:    setVisible0
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_windows_WindowsWindow_setVisible0
  (JNIEnv *env, jobject obj, jlong window, jboolean visible, jboolean top, jint x, jint y, jint width, jint height)
{
    HWND hwnd = (HWND) (intptr_t) window;
    DBG_PRINT("*** WindowsWindow: setVisible window %p, visible: %d, top %d, %d/%d %dx%d\n", 
        hwnd, (int)visible, (int)top, x, y, width, height);
    if (visible) {
        NewtWindow_setVisiblePosSize(env, obj, hwnd, top, visible, x, y, width, height);
        ShowWindow(hwnd, SW_SHOW);
    } else {
        ShowWindow(hwnd, SW_HIDE);
    }
}

#define FULLSCREEN_NOERROR 0
#define FULLSCREEN_ERROR 1

static int NewtWindows_setFullScreen(jboolean fullscreen)
{
    int flags = 0;
    DEVMODE dm;
    // initialize the DEVMODE structure
    ZeroMemory(&dm, sizeof(dm));
    dm.dmSize = sizeof(dm);

    if (0 == EnumDisplaySettings(NULL /*current display device*/, ENUM_CURRENT_SETTINGS, &dm))
    {
        return FULLSCREEN_ERROR;
    }
    
    if(fullscreen == JNI_TRUE)
    {
        flags = CDS_FULLSCREEN; //set fullscreen temporary
    }    
    else
    {
        flags = CDS_RESET; // reset to registery values
    }
    long result = ChangeDisplaySettings(&dm, flags); 
    if(result == DISP_CHANGE_SUCCESSFUL)
    {
        return FULLSCREEN_NOERROR;
    }
    return FULLSCREEN_ERROR;
}

/*
 * Class:     com_jogamp_newt_impl_windows_WindowsWindow
 * Method:    reconfigureWindow0
 * Signature: (JIIIIZZII)V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_windows_WindowsWindow_reconfigureWindow0
  (JNIEnv *env, jobject obj, jlong parent, jlong window, jint x, jint y, jint width, jint height, 
   jboolean visible, jboolean parentChange, jint fullScreenChange, jint decorationChange)
{
    HWND hwndP = (HWND) (intptr_t) parent;
    HWND hwnd = (HWND) (intptr_t) window;
    DWORD windowStyle = WS_CLIPSIBLINGS | WS_CLIPCHILDREN ;
    BOOL styleChange = ( 0 != decorationChange || 0 != fullScreenChange || JNI_TRUE == parentChange ) ? TRUE : FALSE ;
    UINT flags = SWP_SHOWWINDOW;
    HWND hWndInsertAfter;

    DBG_PRINT("*** WindowsWindow: reconfigureWindow0 parent %p, window %p, %d/%d %dx%d, parentChange %d, fullScreenChange %d, visible %d, decorationChange %d -> styleChange %d\n", 
        parent, window, x, y, width, height, parentChange, fullScreenChange, visible, decorationChange, styleChange);

    if (!IsWindow(hwnd)) {
        DBG_PRINT("*** WindowsWindow: reconfigureWindow0 failure: Passed window %p is invalid\n", (void*)hwnd);
        return;
    }

    if (NULL!=hwndP && !IsWindow(hwndP)) {
        DBG_PRINT("*** WindowsWindow: reconfigureWindow0 failure: Passed parent window %p is invalid\n", (void*)hwndP);
        return;
    }

    if(JNI_TRUE == visible) {
        windowStyle |= WS_VISIBLE ;
    }
    
    if(fullScreenChange < 0)
    {
        NewtWindows_setFullScreen(JNI_FALSE);
    }

    // order of call sequence: (MS documentation)
    //    TOP:  SetParent(.., NULL); Clear WS_CHILD [, Set WS_POPUP]
    //  CHILD:  Set WS_CHILD [, Clear WS_POPUP]; SetParent(.., PARENT) 
    //
    if ( JNI_TRUE == parentChange && NULL == hwndP ) {
        SetParent(hwnd, NULL);
    }
    
    if(fullScreenChange > 0)
    {
        NewtWindows_setFullScreen(JNI_TRUE);
    }

    if ( styleChange ) {
        if(NULL!=hwndP) {
            windowStyle |= WS_CHILD ;
        } else if ( decorationChange < 0 || 0 < fullScreenChange ) {
            windowStyle |= WS_POPUP | WS_SYSMENU | WS_MAXIMIZEBOX | WS_MINIMIZEBOX;
        } else {
            windowStyle |= WS_OVERLAPPEDWINDOW;
        }
        SetWindowLong(hwnd, GWL_STYLE, windowStyle);
        SetWindowPos(hwnd, 0, 0, 0, 0, 0, SWP_FRAMECHANGED | SWP_NOSIZE | SWP_NOMOVE | SWP_NOACTIVATE | SWP_NOZORDER );
    }

    if ( JNI_TRUE == parentChange && NULL != hwndP ) {
        SetParent(hwnd, hwndP );
    }

    NewtWindow_setVisiblePosSize(env, obj, hwnd, (NULL == hwndP) ? JNI_TRUE : JNI_FALSE /* top */, visible,
                                 x, y, width, height);

    DBG_PRINT("*** WindowsWindow: reconfigureWindow0.X\n");
}

/*
 * Class:     com_jogamp_newt_impl_windows_WindowsWindow
 * Method:    setTitle
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_windows_WindowsWindow_setTitle0
  (JNIEnv *env, jclass clazz, jlong window, jstring title)
{
    HWND hwnd = (HWND) (intptr_t) window;
    if (title != NULL) {
        jchar *titleString = NewtCommon_GetNullTerminatedStringChars(env, title);
        if (titleString != NULL) {
            SetWindowTextW(hwnd, titleString);
            free(titleString);
        }
    }
}

/*
 * Class:     com_jogamp_newt_impl_windows_WindowsWindow
 * Method:    requestFocus
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_windows_WindowsWindow_requestFocus0
  (JNIEnv *env, jobject obj, jlong window, jboolean force)
{
    DBG_PRINT("*** WindowsWindow: RequestFocus0\n");
    NewtWindows_requestFocus ( env, obj, (HWND) (intptr_t) window, force) ;
}

/*
 * Class:     com_jogamp_newt_impl_windows_WindowsWindows
 * Method:    getRelativeLocation0
 * Signature: (JJII)Ljavax/media/nativewindow/util/Point;
 */
JNIEXPORT jobject JNICALL Java_com_jogamp_newt_impl_windows_WindowsWindow_getRelativeLocation0
  (JNIEnv *env, jobject obj, jlong jsrc_win, jlong jdest_win, jint src_x, jint src_y)
{
    HWND src_win = (HWND) (intptr_t) jsrc_win;
    HWND dest_win = (HWND) (intptr_t) jdest_win;
    POINT dest = { src_x, src_y } ;
    int res;

    res = MapWindowPoints(src_win, dest_win, &dest, 1);

    DBG_PRINT("*** WindowsWindow: getRelativeLocation0: %p %d/%d -> %p %d/%d - ok: %d\n",
        (void*)src_win, src_x, src_y, (void*)dest_win, (int)dest.x, (int)dest.y, res);

    return (*env)->NewObject(env, pointClz, pointCstr, (jint)dest.x, (jint)dest.y);
}

