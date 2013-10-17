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

//
// Min. required version Windows 7 (For WM_TOUCH)
//
#if WINVER < 0x0601
#error WINVER must be >= 0x0601
#endif
#if _WIN32_WINNT < 0x0601
#error _WIN32_WINNT must be >= 0x0601
#endif

#include <Windows.h>
#include <Windowsx.h>
#include <tchar.h>
#include <stdlib.h>

// NOTE: it looks like SHFullScreen and/or aygshell.dll is not available on the APX 2500 any more
// #ifdef UNDER_CE
// #include "aygshell.h"
// #endif

#include <gluegen_stdint.h>

#if !defined(__MINGW64__) && _MSC_VER <= 1500
    // FIXME: Determine for which MSVC versions ..
    #define strdup(s) _strdup(s)
#endif

#ifndef WM_MOUSEWHEEL
#define WM_MOUSEWHEEL                   0x020A
#endif //WM_MOUSEWHEEL

#ifndef WM_MOUSEHWHEEL
#define WM_MOUSEHWHEEL                  0x020E
#endif //WM_MOUSEHWHEEL

#ifndef WHEEL_DELTAf
#define WHEEL_DELTAf                    (120.0f)
#endif //WHEEL_DELTAf

#ifndef WHEEL_PAGESCROLL
#define WHEEL_PAGESCROLL                (UINT_MAX)
#endif //WHEEL_PAGESCROLL

#ifndef GET_WHEEL_DELTA_WPARAM  // defined for (_WIN32_WINNT >= 0x0500)
#define GET_WHEEL_DELTA_WPARAM(wParam)  ((short)HIWORD(wParam))
#endif
#ifndef GET_KEYSTATE_WPARAM
#define GET_KEYSTATE_WPARAM(wParam)  ((short)LOWORD(wParam))
#endif

#ifndef WM_HSCROLL
#define WM_HSCROLL             0x0114
#endif
#ifndef WM_VSCROLL
#define WM_VSCROLL             0x0115
#endif

#ifndef WH_MOUSE
#define WH_MOUSE 7
#endif
#ifndef WH_MOUSE_LL
#define WH_MOUSE_LL 14
#endif

#ifndef WM_TOUCH
#define WM_TOUCH 0x0240
#endif
#ifndef TOUCH_COORD_TO_PIXEL
#define TOUCH_COORD_TO_PIXEL(l) (l/100)
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
#ifndef EDS_ROTATEDMODE
#define EDS_ROTATEDMODE 0x00000004
#endif
#ifndef DISPLAY_DEVICE_ACTIVE
#define DISPLAY_DEVICE_ACTIVE 0x00000001
#endif
#ifndef DM_INTERLACED
#define DM_INTERLACED       2
#endif

#include "jogamp_newt_driver_windows_DisplayDriver.h"
#include "jogamp_newt_driver_windows_ScreenDriver.h"
#include "jogamp_newt_driver_windows_WindowDriver.h"

#include "Window.h"
#include "MouseEvent.h"
#include "InputEvent.h"
#include "KeyEvent.h"
#include "ScreenMode.h"

#include "NewtCommon.h"

// #define VERBOSE_ON 1
// #define DEBUG_KEYS 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(...) fprintf(stderr, __VA_ARGS__); fflush(stderr) 
#else
    #define DBG_PRINT(...)
#endif

#define STD_PRINT(...) fprintf(stderr, __VA_ARGS__); fflush(stderr) 

static jmethodID insetsChangedID = NULL;
static jmethodID sizeChangedID = NULL;
static jmethodID positionChangedID = NULL;
static jmethodID focusChangedID = NULL;
static jmethodID visibleChangedID = NULL;
static jmethodID windowDestroyNotifyID = NULL;
static jmethodID windowRepaintID = NULL;
static jmethodID sendMouseEventID = NULL;
static jmethodID sendTouchScreenEventID = NULL;
static jmethodID sendKeyEventID = NULL;
static jmethodID requestFocusID = NULL;

static RECT* UpdateInsets(JNIEnv *env, jobject window, HWND hwnd);

typedef struct {
    JNIEnv* jenv;
    jobject jinstance;
    /* client size width */
    int width;
    /* client size height */
    int height;
    /** Tristate: -1 HIDE, 0 NOP, 1 SHOW */
    int setPointerVisible;
    int mouseInside;
    int touchDownCount;
    int supportsMTouch;
} WindowUserData;
    
typedef struct {
    USHORT javaKey;
    USHORT windowsKey;
    USHORT windowsScanCodeUS;
} KeyMapEntry;

// Static table, arranged more or less spatially.
static KeyMapEntry keyMapTable[] = {
    // Modifier keys
    {J_VK_CAPS_LOCK,        VK_CAPITAL, 0},
    {J_VK_SHIFT,            VK_SHIFT, 0},
    {J_VK_SHIFT,            VK_LSHIFT, 0},
    {J_VK_SHIFT,            VK_RSHIFT, 0},
    {J_VK_CONTROL,          VK_CONTROL, 0},
    {J_VK_CONTROL,          VK_LCONTROL, 0},
    {J_VK_CONTROL,          VK_RCONTROL, 0},
    {J_VK_ALT,              VK_MENU, 0},
    {J_VK_ALT,              VK_LMENU, 0},
    {J_VK_ALT_GRAPH,        VK_RMENU, 0},
    {J_VK_NUM_LOCK,         VK_NUMLOCK, 0},

    // Miscellaneous Windows keys
    {J_VK_WINDOWS,          VK_LWIN, 0},
    {J_VK_WINDOWS,          VK_RWIN, 0},
    {J_VK_CONTEXT_MENU,     VK_APPS, 0},

    // Alphabet
    {J_VK_A,                'A', 0},
    {J_VK_B,                'B', 0},
    {J_VK_C,                'C', 0},
    {J_VK_D,                'D', 0},
    {J_VK_E,                'E', 0},
    {J_VK_F,                'F', 0},
    {J_VK_G,                'G', 0},
    {J_VK_H,                'H', 0},
    {J_VK_I,                'I', 0},
    {J_VK_J,                'J', 0},
    {J_VK_K,                'K', 0},
    {J_VK_L,                'L', 0},
    {J_VK_M,                'M', 0},
    {J_VK_N,                'N', 0},
    {J_VK_O,                'O', 0},
    {J_VK_P,                'P', 0},
    {J_VK_Q,                'Q', 0},
    {J_VK_R,                'R', 0},
    {J_VK_S,                'S', 0},
    {J_VK_T,                'T', 0},
    {J_VK_U,                'U', 0},
    {J_VK_V,                'V', 0},
    {J_VK_W,                'W', 0},
    {J_VK_X,                'X', 0},
    {J_VK_Y,                'Y', 0},
    {J_VK_Z,                'Z', 0},
    {J_VK_0,                '0', 0},
    {J_VK_1,                '1', 0},
    {J_VK_2,                '2', 0},
    {J_VK_3,                '3', 0},
    {J_VK_4,                '4', 0},
    {J_VK_5,                '5', 0},
    {J_VK_6,                '6', 0},
    {J_VK_7,                '7', 0},
    {J_VK_8,                '8', 0},
    {J_VK_9,                '9', 0},
    {J_VK_ENTER,            VK_RETURN, 0},
    {J_VK_SPACE,            VK_SPACE, 0},
    {J_VK_BACK_SPACE,       VK_BACK, 0},
    {J_VK_TAB,              VK_TAB, 0},
    {J_VK_ESCAPE,           VK_ESCAPE, 0},
    {J_VK_INSERT,           VK_INSERT, 0},
    {J_VK_DELETE,           VK_DELETE, 0},
    {J_VK_HOME,             VK_HOME, 0},
    // {J_VK_BEGIN,            VK_BEGIN, 0}, // not mapped
    {J_VK_END,              VK_END, 0},
    {J_VK_PAGE_UP,          VK_PRIOR, 0},
    {J_VK_PAGE_DOWN,        VK_NEXT, 0},
    {J_VK_CLEAR,            VK_CLEAR, 0}, // NumPad 5

    // NumPad with NumLock off & extended arrows block (triangular)
    {J_VK_LEFT,             VK_LEFT, 0},
    {J_VK_RIGHT,            VK_RIGHT, 0},
    {J_VK_UP,               VK_UP, 0},
    {J_VK_DOWN,             VK_DOWN, 0},

    // NumPad with NumLock on: numbers
    {J_VK_NUMPAD0,          VK_NUMPAD0, 0},
    {J_VK_NUMPAD1,          VK_NUMPAD1, 0},
    {J_VK_NUMPAD2,          VK_NUMPAD2, 0},
    {J_VK_NUMPAD3,          VK_NUMPAD3, 0},
    {J_VK_NUMPAD4,          VK_NUMPAD4, 0},
    {J_VK_NUMPAD5,          VK_NUMPAD5, 0},
    {J_VK_NUMPAD6,          VK_NUMPAD6, 0},
    {J_VK_NUMPAD7,          VK_NUMPAD7, 0},
    {J_VK_NUMPAD8,          VK_NUMPAD8, 0},
    {J_VK_NUMPAD9,          VK_NUMPAD9, 0},

    // NumPad with NumLock on
    {J_VK_MULTIPLY,         VK_MULTIPLY, 0},
    {J_VK_ADD,              VK_ADD, 0},
    {J_VK_SEPARATOR,        VK_SEPARATOR, 0},
    {J_VK_SUBTRACT,         VK_SUBTRACT, 0},
    {J_VK_DECIMAL,          VK_DECIMAL, 0},
    {J_VK_DIVIDE,           VK_DIVIDE, 0},

    // Functional keys
    {J_VK_F1,               VK_F1, 0},
    {J_VK_F2,               VK_F2, 0},
    {J_VK_F3,               VK_F3, 0},
    {J_VK_F4,               VK_F4, 0},
    {J_VK_F5,               VK_F5, 0},
    {J_VK_F6,               VK_F6, 0},
    {J_VK_F7,               VK_F7, 0},
    {J_VK_F8,               VK_F8, 0},
    {J_VK_F9,               VK_F9, 0},
    {J_VK_F10,              VK_F10, 0},
    {J_VK_F11,              VK_F11, 0},
    {J_VK_F12,              VK_F12, 0},
    {J_VK_F13,              VK_F13, 0},
    {J_VK_F14,              VK_F14, 0},
    {J_VK_F15,              VK_F15, 0},
    {J_VK_F16,              VK_F16, 0},
    {J_VK_F17,              VK_F17, 0},
    {J_VK_F18,              VK_F18, 0},
    {J_VK_F19,              VK_F19, 0},
    {J_VK_F20,              VK_F20, 0},
    {J_VK_F21,              VK_F21, 0},
    {J_VK_F22,              VK_F22, 0},
    {J_VK_F23,              VK_F23, 0},
    {J_VK_F24,              VK_F24, 0},

    {J_VK_PRINTSCREEN,      VK_SNAPSHOT, 0},
    {J_VK_SCROLL_LOCK,      VK_SCROLL, 0},
    {J_VK_PAUSE,            VK_PAUSE, 0},
    {J_VK_CANCEL,           VK_CANCEL, 0},
    {J_VK_HELP,             VK_HELP, 0},

    // Since we unify mappings via US kbd layout .. this is valid:
    {J_VK_SEMICOLON,        VK_OEM_1, 0}, // US only ';:'
    {J_VK_EQUALS,           VK_OEM_PLUS, 0}, // '=+'
    {J_VK_COMMA,            VK_OEM_COMMA, 0}, // ',<'
    {J_VK_MINUS,            VK_OEM_MINUS, 0}, // '-_'
    {J_VK_PERIOD,           VK_OEM_PERIOD, 0}, // '.>'
    {J_VK_SLASH,            VK_OEM_2, 0}, // US only '/?'
    {J_VK_BACK_QUOTE,       VK_OEM_3, 0}, // US only '`~'
    {J_VK_OPEN_BRACKET,     VK_OEM_4, 0}, // US only '[}'
    {J_VK_BACK_SLASH,       VK_OEM_5, 0}, // US only '\|'
    {J_VK_CLOSE_BRACKET,    VK_OEM_6, 0}, // US only ']}'
    {J_VK_QUOTE,            VK_OEM_7, 0}, // US only ''"'
    // {J_VK_????,       VK_OEM_8, 0}, // varies ..
    // {J_VK_????,       VK_OEM_102, 0}, // angle-bracket or backslash key on RT 102-key kbd

    // Japanese
/*
    {J_VK_CONVERT,          VK_CONVERT, 0},
    {J_VK_NONCONVERT,       VK_NONCONVERT, 0},
    {J_VK_INPUT_METHOD_ON_OFF, VK_KANJI, 0},
    {J_VK_ALPHANUMERIC,     VK_DBE_ALPHANUMERIC, 0},
    {J_VK_KATAKANA,         VK_DBE_KATAKANA, 0},
    {J_VK_HIRAGANA,         VK_DBE_HIRAGANA, 0},
    {J_VK_FULL_WIDTH,       VK_DBE_DBCSCHAR, 0},
    {J_VK_HALF_WIDTH,       VK_DBE_SBCSCHAR, 0},
    {J_VK_ROMAN_CHARACTERS, VK_DBE_ROMAN, 0},
*/

    {J_VK_UNDEFINED,        0, 0}
};

#ifndef KLF_ACTIVATE
    #define KLF_ACTIVATE 0x00000001
#endif
#ifndef MAPVK_VK_TO_VSC
    #define MAPVK_VK_TO_VSC 0
#endif
#ifndef MAPVK_VSC_TO_VK
    #define MAPVK_VSC_TO_VK 1
#endif
#ifndef MAPVK_VK_TO_CHAR
    #define MAPVK_VK_TO_CHAR 2
#endif
#ifndef MAPVK_VSC_TO_VK_EX
    #define MAPVK_VSC_TO_VK_EX 3
#endif
#ifndef MAPVK_VK_TO_VSC_EX
    #define MAPVK_VK_TO_VSC_EX 4
#endif

#define IS_WITHIN(k,a,b) ((a)<=(k)&&(k)<=(b))

static HKL kbdLayoutUS = 0;
static const LPCSTR US_LAYOUT_NAME = "00000409";

static BYTE kbdState[256];
static USHORT spaceScanCode;

static void InitKeyMapTableScanCode(JNIEnv *env) {
    HKL hkl = GetKeyboardLayout(0);
    int i;

    kbdLayoutUS = LoadKeyboardLayout( US_LAYOUT_NAME, 0 /* ? KLF_ACTIVATE ? */ );
    if( 0 == kbdLayoutUS ) {
        int lastError = (int) GetLastError();
        kbdLayoutUS = hkl; // use prev. layout .. well
        STD_PRINT("Warning: NEWT Windows: LoadKeyboardLayout(US, ..) failed: winErr 0x%X %d\n", lastError, lastError);
    }
    ActivateKeyboardLayout(hkl, 0);

    spaceScanCode = MapVirtualKeyEx(VK_SPACE, MAPVK_VK_TO_VSC, hkl);

    // Setup keyMapTable's windowsScanCodeUS
    for (i = 0; keyMapTable[i].windowsKey != 0; i++) {
        USHORT scancode = (USHORT) MapVirtualKeyEx(keyMapTable[i].windowsKey, MAPVK_VK_TO_VSC_EX, kbdLayoutUS);
        #ifdef DEBUG_KEYS
        if( 0 == scancode ) {
            int lastError = (int) GetLastError();
            STD_PRINT("*** WindowsWindow: InitKeyMapTableScanCode: No ScanCode for windows vkey 0x%X (item %d), winErr 0x%X %d\n", 
                keyMapTable[i].windowsKey, i, lastError, lastError);
        }
        STD_PRINT("*** WindowsWindow: InitKeyMapTableScanCode: %3.3d windows vkey 0x%X -> scancode 0x%X\n", 
            i, keyMapTable[i].windowsKey, scancode);
        #endif
        keyMapTable[i].windowsScanCodeUS = scancode;
    }
}

static void ParseWmVKeyAndScanCode(USHORT winVKey, BYTE winScanCode, BYTE flags, USHORT *outJavaVKeyUS, USHORT *outJavaVKeyXX, USHORT *outUTF16Char) {
    wchar_t uniChars[2] = { L'\0', L'\0' }; // uint16_t
    USHORT winVKeyUS = 0;
    int nUniChars, i, j;
    USHORT javaVKeyUS = J_VK_UNDEFINED;
    USHORT javaVKeyXX = J_VK_UNDEFINED;

    HKL hkl = GetKeyboardLayout(0);

    //
    // winVKey, winScanCode -> UTF16 w/ current KeyboardLayout
    //
    GetKeyboardState(kbdState); 
    kbdState[winVKey] |=  0x80;
    nUniChars = ToUnicodeEx(winVKey, winScanCode, kbdState, uniChars, 2, 0, hkl);
    kbdState[winVKey] &= ~0x80;

    *outUTF16Char = (USHORT)(uniChars[0]); // Note: Even dead key are written in uniChar's ..

    if ( 0 > nUniChars ) { // Dead key
        char junkbuf[2] = { '\0', '\0'};

        // We need to reset layout so that next translation
        // is unaffected by the dead status.  We do this by
        // translating <SPACE> key.
        kbdState[VK_SPACE] |= 0x80;
        ToAsciiEx(VK_SPACE, spaceScanCode, kbdState, (WORD*)junkbuf, 0, hkl);
        kbdState[VK_SPACE] &= ~0x80;
    }

    //
    // winVKey -> javaVKeyXX
    //
    for (i = 0; keyMapTable[i].windowsKey != 0; i++) {
        if ( keyMapTable[i].windowsKey == winVKey ) {
            javaVKeyXX = keyMapTable[i].javaKey;
            break;
        }
    }
    if( IS_WITHIN( winVKey, VK_NUMPAD0, VK_DIVIDE ) ) {
        // Use modded keySym for keypad for US and NN
        winVKeyUS = winVKey;
        javaVKeyUS = javaVKeyXX;
    } else {
        // Assume extended scan code 0xE0 if extended flags is set (no 0xE1 from WM_KEYUP/WM_KEYDOWN)
        USHORT winScanCodeExt = winScanCode;
        if( 0 != ( 0x01 & flags ) ) {
            winScanCodeExt |= 0xE000;
        }

        //
        // winVKey, winScanCodeExt -> javaVKeyUS w/ US KeyboardLayout
        //
        for (i = 0; keyMapTable[i].windowsKey != 0; i++) {
            if ( keyMapTable[i].windowsScanCodeUS == winScanCodeExt ) {
                winVKeyUS = keyMapTable[i].windowsKey;
                javaVKeyUS = keyMapTable[i].javaKey;
                break;
            }
        }
        if( J_VK_UNDEFINED == javaVKeyUS ) {
            javaVKeyUS = javaVKeyXX;
        }
    }

    *outJavaVKeyUS = javaVKeyUS;
    *outJavaVKeyXX = javaVKeyXX;

#ifdef DEBUG_KEYS
    STD_PRINT("*** WindowsWindow: ParseWmVKeyAndScanCode winVKey 0x%X, winScanCode 0x%X, winScanCodeExt 0x%X, flags 0x%X -> UTF(0x%X, %c, res %d, sizeof %d), vKeys( US(win 0x%X, java 0x%X), XX(win 0x%X, java 0x%X))\n", 
        (int)winVKey, (int)winScanCode, winScanCodeExt, (int)flags,
        *outUTF16Char, *outUTF16Char, nUniChars, sizeof(uniChars[0]),
        winVKeyUS, javaVKeyUS, winVKey, javaVKeyXX);
#endif
}

static jint GetModifiers(USHORT jkey) {
    jint modifiers = 0;
    // have to do &0xFFFF to avoid runtime assert caused by compiling with
    // /RTCcsu
    if ( HIBYTE((GetKeyState(VK_CONTROL) & 0xFFFF)) != 0 || J_VK_CONTROL == jkey ) {
        modifiers |= EVENT_CTRL_MASK;
    }
    if ( HIBYTE((GetKeyState(VK_SHIFT) & 0xFFFF)) != 0 || J_VK_SHIFT == jkey ) {
        modifiers |= EVENT_SHIFT_MASK;
    }
    if ( HIBYTE((GetKeyState(VK_LMENU) & 0xFFFF)) != 0 || J_VK_ALT == jkey ) {
        modifiers |= EVENT_ALT_MASK;
    }
    if ( HIBYTE((GetKeyState(VK_RMENU) & 0xFFFF)) != 0 || (USHORT)J_VK_ALT_GRAPH == jkey ) {
        modifiers |= EVENT_ALT_GRAPH_MASK;
    }
    if ( HIBYTE((GetKeyState(VK_LBUTTON) & 0xFFFF)) != 0 ) {
        modifiers |= EVENT_BUTTON1_MASK;
    }
    if ( HIBYTE((GetKeyState(VK_MBUTTON) & 0xFFFF)) != 0 ) {
        modifiers |= EVENT_BUTTON2_MASK;
    }
    if ( HIBYTE((GetKeyState(VK_RBUTTON) & 0xFFFF)) != 0 ) {
        modifiers |= EVENT_BUTTON3_MASK;
    }

    return modifiers;
}

/**
static BOOL IsAltKeyDown(BYTE flags, BOOL system) {
    // The Alt modifier is reported in the 29th bit of the lParam,
    // i.e., it is the 5th bit of `flags' (which is HIBYTE(HIWORD(lParam))).
    return system && ( flags & (1<<5) ) != 0;
} */

static int WmKeyDown(JNIEnv *env, jobject window, USHORT wkey, WORD repCnt, BYTE scanCode, BYTE flags, BOOL system) {
    UINT modifiers = 0;
    USHORT javaVKeyUS=0, javaVKeyXX=0, utf16Char=0;
    if (wkey == VK_PROCESSKEY) {
        return 1;
    }

    ParseWmVKeyAndScanCode(wkey, scanCode, flags, &javaVKeyUS, &javaVKeyXX, &utf16Char);

    modifiers = GetModifiers( javaVKeyUS );

    (*env)->CallVoidMethod(env, window, sendKeyEventID,
                           (jshort) EVENT_KEY_PRESSED,
                           (jint) modifiers, (jshort) javaVKeyUS, (jshort) javaVKeyXX, (jchar) utf16Char);

    return 0;
}

static int WmKeyUp(JNIEnv *env, jobject window, USHORT wkey, WORD repCnt, BYTE scanCode, BYTE flags, BOOL system) {
    UINT modifiers = 0;
    USHORT javaVKeyUS=0, javaVKeyXX=0, utf16Char=0;
    if (wkey == VK_PROCESSKEY) {
        return 1;
    }

    ParseWmVKeyAndScanCode(wkey, scanCode, flags, &javaVKeyUS, &javaVKeyXX, &utf16Char);

    modifiers = GetModifiers( javaVKeyUS );

    (*env)->CallVoidMethod(env, window, sendKeyEventID,
                           (jshort) EVENT_KEY_RELEASED,
                           (jint) modifiers, (jshort) javaVKeyUS, (jshort) javaVKeyXX, (jchar) utf16Char);

    return 0;
}

static void NewtWindows_requestFocus (JNIEnv *env, jobject window, HWND hwnd, jboolean force) {
    HWND pHwnd, current;
    BOOL isEnabled = IsWindowEnabled(hwnd);
    pHwnd = GetParent(hwnd);
    current = GetFocus();
    DBG_PRINT("*** WindowsWindow: requestFocus.S force %d, parent %p, window %p, isEnabled %d, isCurrent %d\n", 
        (int)force, (void*)pHwnd, (void*)hwnd, isEnabled, current==hwnd);

    if( JNI_TRUE==force || current!=hwnd || !isEnabled ) {
        UINT flags = SWP_SHOWWINDOW | SWP_NOSIZE | SWP_NOMOVE;
        if(!isEnabled) {
            EnableWindow(hwnd, TRUE);
        }
        SetWindowPos(hwnd, HWND_TOP, 0, 0, 0, 0, flags);
        SetForegroundWindow(hwnd);  // Slightly Higher Priority
        SetFocus(hwnd);// Sets Keyboard Focus To Window (activates parent window if exist, or this window)
        if(NULL!=pHwnd) {
            SetActiveWindow(hwnd);
        }
        current = GetFocus();
        DBG_PRINT("*** WindowsWindow: requestFocus.X1 isCurrent %d\n", current==hwnd);
    }
    DBG_PRINT("*** WindowsWindow: requestFocus.XX\n");
}

static void NewtWindows_trackPointerLeave(HWND hwnd) {
    TRACKMOUSEEVENT tme;
    memset(&tme, 0, sizeof(TRACKMOUSEEVENT));
    tme.cbSize = sizeof(TRACKMOUSEEVENT);
    tme.dwFlags = TME_LEAVE;
    tme.hwndTrack = hwnd;
    tme.dwHoverTime = 0; // we don't use TME_HOVER
    BOOL ok = TrackMouseEvent(&tme);
    DBG_PRINT( "*** WindowsWindow: trackPointerLeave: %d\n", ok);
    #ifdef VERBOSE_ON
    if(!ok) {
        int lastError = (int) GetLastError();
        DBG_PRINT( "*** WindowsWindow: trackPointerLeave: lastError 0x%X %d\n", lastError, lastError);
    }
    #endif
    (void)ok;
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

    (*env)->CallVoidMethod(env, window, insetsChangedID, JNI_FALSE,
                           m_insets.left, m_insets.right,
                           m_insets.top, m_insets.bottom);
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

        BOOL bIsUndecorated = (style & (WS_CHILD|WS_POPUP)) != 0;
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

    DBG_PRINT("*** WindowsWindow: UpdateInsets window %p, [l %d, r %d - t %d, b %d - %dx%d]\n", 
        (void*)hwnd, (int)m_insets.left, (int)m_insets.right, (int)m_insets.top, (int)m_insets.bottom,
        (int) ( m_insets.left + m_insets.right ), (int) (m_insets.top + m_insets.bottom));

    (*env)->CallVoidMethod(env, window, insetsChangedID, JNI_FALSE,
                           (int)m_insets.left, (int)m_insets.right, (int)m_insets.top, (int)m_insets.bottom);
    return &m_insets;
}

#endif

static void WmSize(JNIEnv *env, WindowUserData * wud, HWND wnd, UINT type)
{
    RECT rc;
    BOOL isVisible = IsWindowVisible(wnd);
    jobject window = wud->jinstance;

    if (type == SIZE_MINIMIZED) {
        // TODO: deal with minimized window sizing
        return;
    }

    // make sure insets are up to date
    (void)UpdateInsets(env, window, wnd);

    GetClientRect(wnd, &rc);
    
    // we report back the dimensions of the client area
    wud->width = (int) ( rc.right  - rc.left );
    wud->height = (int) ( rc.bottom - rc.top );

    DBG_PRINT("*** WindowsWindow: WmSize window %p, %dx%d, visible %d\n", (void*)wnd, wud->width, wud->height, isVisible);

    (*env)->CallVoidMethod(env, window, sizeChangedID, JNI_FALSE, wud->width, wud->height, JNI_FALSE);
}

#ifdef TEST_MOUSE_HOOKS

static HHOOK hookLLMP;
static HHOOK hookMP;

static LRESULT CALLBACK HookLowLevelMouseProc (int code, WPARAM wParam, LPARAM lParam)
{
    // if (code == HC_ACTION)
    {
        const char *msg;
        char msg_buff[128];
        switch (wParam)
        {
            case WM_LBUTTONDOWN: msg = "WM_LBUTTONDOWN"; break;
            case WM_LBUTTONUP: msg = "WM_LBUTTONUP"; break;
            case WM_MOUSEMOVE: msg = "WM_MOUSEMOVE"; break;
            case WM_MOUSEWHEEL: msg = "WM_MOUSEWHEEL"; break;
            case WM_MOUSEHWHEEL: msg = "WM_MOUSEHWHEEL"; break;
            case WM_RBUTTONDOWN: msg = "WM_RBUTTONDOWN"; break;
            case WM_RBUTTONUP: msg = "WM_RBUTTONUP"; break;
            default: 
                sprintf(msg_buff, "Unknown msg: %u", wParam); 
                msg = msg_buff;
                break;
        }//switch

        const MSLLHOOKSTRUCT *p = (MSLLHOOKSTRUCT*)lParam;
        DBG_PRINT("**** LLMP: Code: 0x%X: %s - %d/%d\n", code, msg, (int)p->pt.x, (int)p->pt.y);
    //} else {
    //    DBG_PRINT("**** LLMP: CODE: 0x%X\n", code);
    }
    return CallNextHookEx(hookLLMP, code, wParam, lParam); 
}

static LRESULT CALLBACK HookMouseProc (int code, WPARAM wParam, LPARAM lParam)
{
    // if (code == HC_ACTION)
    {
        const char *msg;
        char msg_buff[128];
        switch (wParam)
        {
            case WM_LBUTTONDOWN: msg = "WM_LBUTTONDOWN"; break;
            case WM_LBUTTONUP: msg = "WM_LBUTTONUP"; break;
            case WM_MOUSEMOVE: msg = "WM_MOUSEMOVE"; break;
            case WM_MOUSEWHEEL: msg = "WM_MOUSEWHEEL"; break;
            case WM_MOUSEHWHEEL: msg = "WM_MOUSEHWHEEL"; break;
            case WM_RBUTTONDOWN: msg = "WM_RBUTTONDOWN"; break;
            case WM_RBUTTONUP: msg = "WM_RBUTTONUP"; break;
            default: 
                sprintf(msg_buff, "Unknown msg: %u", wParam); 
                msg = msg_buff;
                break;
        }//switch

        const MOUSEHOOKSTRUCT *p = (MOUSEHOOKSTRUCT*)lParam;
        DBG_PRINT("**** MP: Code: 0x%X: %s - hwnd %p, %d/%d\n", code, msg, p->hwnd, (int)p->pt.x, (int)p->pt.y);
    //} else {
    //    DBG_PRINT("**** MP: CODE: 0x%X\n", code);
    }
    return CallNextHookEx(hookMP, code, wParam, lParam); 
}

#endif

static BOOL SafeShowCursor(BOOL show) {
    int count, countPre;
    BOOL b;

    if( show ) {
        count = ShowCursor(TRUE);
        if(count < 0) {
            do {
                countPre = count;
                count = ShowCursor(TRUE);
            } while( count > countPre && count < 0 );
        }
        b = count>=0 ? TRUE : FALSE;
    } else {
        count = ShowCursor(FALSE);
        if(count >= 0) {
            do {
                countPre = count;
                count = ShowCursor(FALSE);
            } while( count < countPre && count >= 0 );
        }
        b = count<0 ? TRUE : FALSE;
    }
    return b;
}

static void sendTouchScreenEvent(JNIEnv *env, jobject window, 
                                 short eventType, int modifiers, int actionIdx, 
                                 int count, jint* pointerNames, jint* x, jint* y, jfloat* pressure, float maxPressure) {
    jintArray jNames = (*env)->NewIntArray(env, count);
    if (jNames == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array (names) of size %d", count);
    }
    (*env)->SetIntArrayRegion(env, jNames, 0, count, pointerNames);

    jintArray jX = (*env)->NewIntArray(env, count);
    if (jX == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array (x) of size %d", count);
    }
    (*env)->SetIntArrayRegion(env, jX, 0, count, x);

    jintArray jY = (*env)->NewIntArray(env, count);
    if (jY == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array (y) of size %d", count);
    }
    (*env)->SetIntArrayRegion(env, jY, 0, count, y);

    jfloatArray jPressure = (*env)->NewFloatArray(env, count);
    if (jPressure == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate float array (pressure) of size %d", count);
    }
    (*env)->SetFloatArrayRegion(env, jPressure, 0, count, pressure);

    (*env)->CallVoidMethod(env, window, sendTouchScreenEventID,
                           (jshort)eventType, (jint)modifiers, (jint)actionIdx,
                           jNames, jX, jY, jPressure, (jfloat)maxPressure);
}
    

static LRESULT CALLBACK wndProc(HWND wnd, UINT message, WPARAM wParam, LPARAM lParam) {
    LRESULT res = 0;
    int useDefWindowProc = 0;
    JNIEnv *env = NULL;
    jobject window = NULL;
    BOOL isKeyDown = FALSE;
    WindowUserData * wud;
    WORD repCnt; 
    BYTE scanCode, flags;

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

    // DBG_PRINT("*** WindowsWindow: thread 0x%X - window %p -> %p, msg 0x%X, %d/%d\n", (int)GetCurrentThreadId(), wnd, window, message, (int)LOWORD(lParam), (int)HIWORD(lParam));

    if (NULL==window || NULL==env) {
        return DefWindowProc(wnd, message, wParam, lParam);
    }

    switch (message) {

    //
    // The signal pipeline for destruction is:
    //    Java::DestroyWindow(wnd) _or_ window-close-button -> 
    //     WM_CLOSE -> Java::windowDestroyNotify -> W_DESTROY
    case WM_CLOSE:
        (*env)->CallBooleanMethod(env, window, windowDestroyNotifyID, JNI_FALSE);
        break;

    case WM_DESTROY:
        {
#if !defined(__MINGW64__) && ( defined(UNDER_CE) || _MSC_VER <= 1200 )
            SetWindowLong(wnd, GWL_USERDATA, (intptr_t) NULL);
#else
            SetWindowLongPtr(wnd, GWLP_USERDATA, (intptr_t) NULL);
#endif
            free(wud); wud=NULL;
            (*env)->DeleteGlobalRef(env, window);
        }
        break;

    case WM_SYSCHAR:
        useDefWindowProc = 1;
        break;

    case WM_SYSKEYDOWN:
        repCnt = HIWORD(lParam); scanCode = LOBYTE(repCnt); flags = HIBYTE(repCnt);
        repCnt = LOWORD(lParam);
#ifdef DEBUG_KEYS
        DBG_PRINT("*** WindowsWindow: windProc WM_SYSKEYDOWN sending window %p -> %p, code 0x%X, repCnt %d, scanCode 0x%X, flags 0x%X\n", wnd, window, (int)wParam, (int)repCnt, (int)scanCode, (int)flags);
#endif
        useDefWindowProc = WmKeyDown(env, window, (USHORT)wParam, repCnt, scanCode, flags, TRUE);
        break;

    case WM_SYSKEYUP:
        repCnt = HIWORD(lParam); scanCode = LOBYTE(repCnt); flags = HIBYTE(repCnt);
        repCnt = LOWORD(lParam);
#ifdef DEBUG_KEYS
        DBG_PRINT("*** WindowsWindow: windProc WM_SYSKEYUP sending window %p -> %p, code 0x%X, repCnt %d, scanCode 0x%X, flags 0x%X\n", wnd, window, (int)wParam, (int)repCnt, (int)scanCode, (int)flags);
#endif
        useDefWindowProc = WmKeyUp(env, window, (USHORT)wParam, repCnt, scanCode, flags, TRUE);
        break;

    case WM_CHAR:
        useDefWindowProc = 1;
        break;
        
    case WM_KEYDOWN:
        repCnt = HIWORD(lParam); scanCode = LOBYTE(repCnt); flags = HIBYTE(repCnt);
#ifdef DEBUG_KEYS
        DBG_PRINT("*** WindowsWindow: windProc WM_KEYDOWN sending window %p -> %p, code 0x%X, repCnt %d, scanCode 0x%X, flags 0x%X\n", wnd, window, (int)wParam, (int)repCnt, (int)scanCode, (int)flags);
#endif
        useDefWindowProc = WmKeyDown(env, window, wParam, repCnt, scanCode, flags, FALSE);
        break;

    case WM_KEYUP:
        repCnt = HIWORD(lParam); scanCode = LOBYTE(repCnt); flags = HIBYTE(repCnt);
        repCnt = LOWORD(lParam);
#ifdef DEBUG_KEYS
        DBG_PRINT("*** WindowsWindow: windProc WM_KEYUP sending window %p -> %p, code 0x%X, repCnt %d, scanCode 0x%X, flags 0x%X\n", wnd, window, (int)wParam, (int)repCnt, (int)scanCode, (int)flags);
#endif
        useDefWindowProc = WmKeyUp(env, window, wParam, repCnt, scanCode, flags, FALSE);
        break;

    case WM_SIZE:
        WmSize(env, wud, wnd, (UINT)wParam);
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


    case WM_LBUTTONDOWN: {
            BOOL isMouse = 0 == GetMessageExtraInfo();
            DBG_PRINT("*** WindowsWindow: WM_LBUTTONDOWN %d/%d [%dx%d] inside %d, isMouse %d, tDown %d\n", 
                (jint) GET_X_LPARAM(lParam), (jint) GET_Y_LPARAM(lParam),
                wud->width, wud->height, wud->mouseInside, isMouse, wud->touchDownCount);
            if( isMouse ) {
                wud->mouseInside = 1;
                (*env)->CallVoidMethod(env, window, requestFocusID, JNI_FALSE);
                (*env)->CallVoidMethod(env, window, sendMouseEventID,
                                       (jshort) EVENT_MOUSE_PRESSED,
                                       GetModifiers( 0 ),
                                       (jint) GET_X_LPARAM(lParam), (jint) GET_Y_LPARAM(lParam),
                                       (jshort) 1, (jfloat) 0.0f);
                useDefWindowProc = 1;
            }
        }
        break;

    case WM_LBUTTONUP: {
            BOOL isMouse = 0 == GetMessageExtraInfo();
            DBG_PRINT("*** WindowsWindow: WM_LBUTTONUP %d/%d [%dx%d] inside %d, isMouse %d, tDown %d\n", 
                (jint) GET_X_LPARAM(lParam), (jint) GET_Y_LPARAM(lParam),
                wud->width, wud->height, wud->mouseInside, isMouse, wud->touchDownCount);
            if( isMouse ) {
                wud->mouseInside = 1;
                (*env)->CallVoidMethod(env, window, sendMouseEventID,
                                       (jshort) EVENT_MOUSE_RELEASED,
                                       GetModifiers( 0 ),
                                       (jint) GET_X_LPARAM(lParam), (jint) GET_Y_LPARAM(lParam),
                                       (jshort) 1, (jfloat) 0.0f);
                useDefWindowProc = 1;
            }
        }
        break;

    case WM_MBUTTONDOWN: {
            BOOL isMouse = 0 == GetMessageExtraInfo();
            DBG_PRINT("*** WindowsWindow: WM_MBUTTONDOWN %d/%d [%dx%d] inside %d, isMouse %d, tDown %d\n", 
                (jint) GET_X_LPARAM(lParam), (jint) GET_Y_LPARAM(lParam),
                wud->width, wud->height, wud->mouseInside, isMouse, wud->touchDownCount);
            if( isMouse ) {
                wud->mouseInside = 1;
                (*env)->CallVoidMethod(env, window, requestFocusID, JNI_FALSE);
                (*env)->CallVoidMethod(env, window, sendMouseEventID,
                                       (jshort) EVENT_MOUSE_PRESSED,
                                       GetModifiers( 0 ),
                                       (jint) GET_X_LPARAM(lParam), (jint) GET_Y_LPARAM(lParam),
                                       (jshort) 2, (jfloat) 0.0f);
                useDefWindowProc = 1;
            }
        }
        break;

    case WM_MBUTTONUP: {
            BOOL isMouse = 0 == GetMessageExtraInfo();
            DBG_PRINT("*** WindowsWindow: WM_MBUTTONUP %d/%d [%dx%d] inside %d, isMouse %d, tDown %d\n", 
                (jint) GET_X_LPARAM(lParam), (jint) GET_Y_LPARAM(lParam),
                wud->width, wud->height, wud->mouseInside, isMouse, wud->touchDownCount);
            if( isMouse ) {
                wud->mouseInside = 1;
                (*env)->CallVoidMethod(env, window, sendMouseEventID,
                                       (jshort) EVENT_MOUSE_RELEASED,
                                       GetModifiers( 0 ),
                                       (jint) GET_X_LPARAM(lParam), (jint) GET_Y_LPARAM(lParam),
                                       (jshort) 2, (jfloat) 0.0f);
                useDefWindowProc = 1;
            }
        }
        break;

    case WM_RBUTTONDOWN: {
            BOOL isMouse = 0 == GetMessageExtraInfo();
            DBG_PRINT("*** WindowsWindow: WM_RBUTTONDOWN: %d/%d [%dx%d] inside %d, isMouse %d, tDown %d\n", 
                (jint) GET_X_LPARAM(lParam), (jint) GET_Y_LPARAM(lParam),
                wud->width, wud->height, wud->mouseInside, isMouse, wud->touchDownCount);
            if( isMouse ) {
                wud->mouseInside = 1;
                (*env)->CallVoidMethod(env, window, requestFocusID, JNI_FALSE);
                (*env)->CallVoidMethod(env, window, sendMouseEventID,
                                       (jshort) EVENT_MOUSE_PRESSED,
                                       GetModifiers( 0 ),
                                       (jint) GET_X_LPARAM(lParam), (jint) GET_Y_LPARAM(lParam),
                                       (jshort) 3, (jfloat) 0.0f);
                useDefWindowProc = 1;
            }
        }
        break;

    case WM_RBUTTONUP: {
            BOOL isMouse = 0 == GetMessageExtraInfo();
            DBG_PRINT("*** WindowsWindow: WM_RBUTTONUP %d/%d [%dx%d] inside %d, isMouse %d, tDown %d\n", 
                (jint) GET_X_LPARAM(lParam), (jint) GET_Y_LPARAM(lParam),
                wud->width, wud->height, wud->mouseInside, isMouse, wud->touchDownCount);
            if( isMouse ) {
                wud->mouseInside = 1;
                (*env)->CallVoidMethod(env, window, sendMouseEventID,
                                       (jshort) EVENT_MOUSE_RELEASED,
                                       GetModifiers( 0 ),
                                       (jint) GET_X_LPARAM(lParam), (jint) GET_Y_LPARAM(lParam),
                                       (jshort) 3,  (jfloat) 0.0f);
                useDefWindowProc = 1;
            }
        }
        break;

    case WM_MOUSEMOVE: {
            wud->mouseInside = 1;
            DBG_PRINT("*** WindowsWindow: WM_MOUSEMOVE %d/%d [%dx%d] inside %d, tDown %d\n", 
                (jint) GET_X_LPARAM(lParam), (jint) GET_Y_LPARAM(lParam),
                wud->width, wud->height, wud->mouseInside, wud->touchDownCount);
            if( wud->touchDownCount == 0 ) {
                NewtWindows_trackPointerLeave(wnd);
                (*env)->CallVoidMethod(env, window, sendMouseEventID,
                                       (jshort) EVENT_MOUSE_MOVED,
                                       GetModifiers( 0 ),
                                       (jint) GET_X_LPARAM(lParam), (jint) GET_Y_LPARAM(lParam),
                                       (jshort) 0,  (jfloat) 0.0f);
            }
            useDefWindowProc = 1;
        }
        break;
    case WM_MOUSELEAVE: {
            wud->mouseInside = 0;
            DBG_PRINT("*** WindowsWindow: WM_MOUSELEAVE %d/%d [%dx%d] inside %d, tDown %d\n", 
                (jint) GET_X_LPARAM(lParam), (jint) GET_Y_LPARAM(lParam),
                wud->width, wud->height, wud->mouseInside, wud->touchDownCount);
            (*env)->CallVoidMethod(env, window, sendMouseEventID,
                                   (jshort) EVENT_MOUSE_EXITED,
                                   0,
                                   (jint) -1, (jint) -1, // fake
                                   (jshort) 0,  (jfloat) 0.0f);
            useDefWindowProc = 1;
        }
        break;
    // Java synthesizes EVENT_MOUSE_ENTERED

    case WM_HSCROLL: { // Only delivered if windows has WS_HSCROLL, hence dead code!
            int sb = LOWORD(wParam);
            int modifiers = GetModifiers( 0 ) | EVENT_SHIFT_MASK;
            float rotation;
            switch(sb) {
                case SB_LINELEFT:
                    rotation = 1.0f;
                    break;
                case SB_PAGELEFT:
                    rotation = 2.0f;
                    break;
                case SB_LINERIGHT:
                    rotation = -1.0f;
                    break;
                case SB_PAGERIGHT:
                    rotation = -1.0f;
                    break;
            }
            DBG_PRINT("*** WindowsWindow: WM_HSCROLL 0x%X, rotation %f, mods 0x%X\n", sb, rotation, modifiers);
            (*env)->CallVoidMethod(env, window, sendMouseEventID,
                                   (jshort) EVENT_MOUSE_WHEEL_MOVED,
                                   modifiers,
                                   (jint) 0, (jint) 0,
                                   (jshort) 1,  (jfloat) rotation);
            useDefWindowProc = 1;
            break;
        }
    case WM_MOUSEHWHEEL: /* tilt */
    case WM_MOUSEWHEEL: /* rotation */ {
        // need to convert the coordinates to component-relative
        int x = GET_X_LPARAM(lParam);
        int y = GET_Y_LPARAM(lParam);
        int modifiers = GetModifiers( 0 );
        float rotationOrTilt = (float)(GET_WHEEL_DELTA_WPARAM(wParam))/WHEEL_DELTAf;
        int vKeys = GET_KEYSTATE_WPARAM(wParam);
        POINT eventPt;
        eventPt.x = x;
        eventPt.y = y;
        ScreenToClient(wnd, &eventPt);

        if( WM_MOUSEHWHEEL == message ) {
            modifiers |= EVENT_SHIFT_MASK;
            DBG_PRINT("*** WindowsWindow: WM_MOUSEHWHEEL %d/%d, tilt %f, vKeys 0x%X, mods 0x%X\n", 
                (int)eventPt.x, (int)eventPt.y, rotationOrTilt, vKeys, modifiers);
        } else {
            DBG_PRINT("*** WindowsWindow: WM_MOUSEWHEEL %d/%d, rotation %f, vKeys 0x%X, mods 0x%X\n", 
                (int)eventPt.x, (int)eventPt.y, rotationOrTilt, vKeys, modifiers);
        }
        (*env)->CallVoidMethod(env, window, sendMouseEventID,
                               (jshort) EVENT_MOUSE_WHEEL_MOVED,
                               modifiers,
                               (jint) eventPt.x, (jint) eventPt.y,
                               (jshort) 1,  (jfloat) rotationOrTilt);
        useDefWindowProc = 1;
        break;
    }

    case WM_TOUCH: if( wud->supportsMTouch ) {
        UINT cInputs = LOWORD(wParam);
        // DBG_PRINT("*** WindowsWindow: WM_TOUCH window %p, cInputs %d\n", wnd, cInputs);
        HTOUCHINPUT hTouch = (HTOUCHINPUT)lParam;
        PTOUCHINPUT pInputs = (PTOUCHINPUT) calloc(cInputs, sizeof(TOUCHINPUT));
        if (NULL != pInputs) {
            if (GetTouchInputInfo(hTouch, cInputs, pInputs, sizeof(TOUCHINPUT))) {
                UINT i;
                short eventType[cInputs];
                jint modifiers = GetModifiers( 0 );
                jint actionIdx = -1;
                jint pointerNames[cInputs];
                jint x[cInputs], y[cInputs];
                jfloat pressure[cInputs];
                jfloat maxPressure = 1.0F; // FIXME: n/a on windows ?

                for (i=0; i < cInputs; i++) {
                    PTOUCHINPUT pTi = & pInputs[i];
                    int inside;
                    POINT eventPt;
                    int isDown = pTi->dwFlags & TOUCHEVENTF_DOWN;
                    int isUp = pTi->dwFlags & TOUCHEVENTF_UP;
                    int isMove = pTi->dwFlags & TOUCHEVENTF_MOVE;

                    int isPrim = pTi->dwFlags & TOUCHEVENTF_PRIMARY;
                    int isNoCoalesc = pTi->dwFlags & TOUCHEVENTF_NOCOALESCE;

                    #ifdef VERBOSE_ON
                    const char * touchAction;
                    if( isDown ) {
                        touchAction = "down";
                    } else if( isUp ) {
                        touchAction = "_up_";
                    } else if( isMove ) {
                        touchAction = "move";
                    } else {
                        touchAction = "undf";
                    }
                    #endif

                    pointerNames[i] = (jint)pTi->dwID;
                    eventPt.x = TOUCH_COORD_TO_PIXEL(pTi->x);
                    eventPt.y = TOUCH_COORD_TO_PIXEL(pTi->y);
                    ScreenToClient(wnd, &eventPt);
                    x[i] = (jint)eventPt.x;
                    y[i] = (jint)eventPt.y;
                    pressure[i] = 1.0F; // FIXME: n/a on windows ?
                    if(isDown) {
                        eventType[i] = (jshort) EVENT_MOUSE_PRESSED;
                    } else if(isUp) {
                        eventType[i] = (jshort) EVENT_MOUSE_RELEASED;
                    } else if(isMove) {
                        eventType[i] = (jshort) EVENT_MOUSE_MOVED;
                    } else {
                        eventType[i] = (jshort) 0;
                    }
                    if(isPrim) {
                        actionIdx = (jint)i;
                    }
                    inside = 0 <= x[i] && 0 <= y[i] && x[i] < wud->width && y[i] < wud->height;

                    #ifdef VERBOSE_ON
                    DBG_PRINT("*** WindowsWindow: WM_TOUCH[%d/%d].%s name 0x%x, prim %d, nocoalsc %d, %d/%d [%dx%d] inside %d/%d, tDown %d\n", 
                        (i+1), cInputs, touchAction, (int)(pTi->dwID), isPrim, isNoCoalesc, x[i], y[i], wud->width, wud->height, inside, wud->mouseInside, wud->touchDownCount);
                    #endif
                }
                int sentCount = 0, updownCount=0, moveCount=0;
                // Primary first, if available!
                if( 0 <= actionIdx ) {
                    sendTouchScreenEvent(env, window, eventType[actionIdx], modifiers, actionIdx, 
                                         cInputs, pointerNames, x, y, pressure, maxPressure);
                    sentCount++;
                }
                // 1 Move second ..
                for (i=0; i < cInputs; i++) {
                    short et = eventType[i];
                    if( (jshort) EVENT_MOUSE_MOVED == et ) {
                        if( i != actionIdx && 0 == moveCount ) {
                            sendTouchScreenEvent(env, window, et, modifiers, i, 
                                                 cInputs, pointerNames, x, y, pressure, maxPressure);
                            sentCount++;
                        }
                        moveCount++;
                    }
                }
                // Up and downs last
                for (i=0; i < cInputs; i++) {
                    short et = eventType[i];
                    if( (jshort) EVENT_MOUSE_MOVED != et ) {
                        if( i != actionIdx ) {
                            sendTouchScreenEvent(env, window, et, modifiers, i, 
                                                 cInputs, pointerNames, x, y, pressure, maxPressure);
                            sentCount++;
                        }
                        updownCount++;
                    }
                }
                DBG_PRINT("*** WindowsWindow: WM_TOUCH.summary pCount %d, prim %d, updown %d, move %d, sent %d\n", 
                    cInputs, actionIdx, updownCount, moveCount, sentCount);

                // Message processed - close it
                CloseTouchInputHandle(hTouch);
            } else {
                useDefWindowProc = 1;
            }
            free(pInputs);
        }
        break;
    }

    case WM_SETFOCUS:
        DBG_PRINT("*** WindowsWindow: WM_SETFOCUS window %p, lost %p\n", wnd, (HWND)wParam);
        (*env)->CallVoidMethod(env, window, focusChangedID, JNI_FALSE, JNI_TRUE);
        useDefWindowProc = 1;
        break;

    case WM_KILLFOCUS:
        DBG_PRINT("*** WindowsWindow: WM_KILLFOCUS window %p, received %p\n", wnd, (HWND)wParam);
        wud->touchDownCount=0;
        wud->mouseInside=0;
        (*env)->CallVoidMethod(env, window, focusChangedID, JNI_FALSE, JNI_FALSE);
        useDefWindowProc = 1;
        break;

    case WM_SHOWWINDOW:
        (*env)->CallVoidMethod(env, window, visibleChangedID, JNI_FALSE, wParam==TRUE?JNI_TRUE:JNI_FALSE);
        break;

    case WM_MOVE:
        DBG_PRINT("*** WindowsWindow: WM_MOVE window %p, %d/%d\n", wnd, GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam));
        (*env)->CallVoidMethod(env, window, positionChangedID, JNI_FALSE, (jint)GET_X_LPARAM(lParam), (jint)GET_Y_LPARAM(lParam));
        useDefWindowProc = 1;
        break;

    case WM_PAINT: {
        RECT r;
        if (GetUpdateRect(wnd, &r, FALSE /* do not erase background */)) {
            // clear the whole client area and issue repaint for it, w/o looping through erase background
            ValidateRect(wnd, NULL); // clear all!
            (*env)->CallVoidMethod(env, window, windowRepaintID, JNI_FALSE, 0, 0, -1, -1);
        } else {
            // shall not happen ?
            ValidateRect(wnd, NULL); // clear all!
        }
        // return 0 == done
        break;
    }
    case WM_ERASEBKGND:
        // ignore erase background
        (*env)->CallVoidMethod(env, window, windowRepaintID, JNI_FALSE, 0, 0, -1, -1);
        res = 1; // return 1 == done, OpenGL, etc .. erases the background, hence we claim to have just done this
        break;
    case WM_SETCURSOR :
        if (0 != wud->setPointerVisible) { // Tristate, -1, 0, 1
            BOOL visibilityChangeSuccessful;
            if (1 == wud->setPointerVisible) {
                visibilityChangeSuccessful = SafeShowCursor(TRUE);
            } else /* -1 == wud->setPointerVisible */ {
                visibilityChangeSuccessful = SafeShowCursor(FALSE);
            }
            useDefWindowProc = visibilityChangeSuccessful ? 1 : 0;
            DBG_PRINT("*** WindowsWindow: WM_SETCURSOR requested visibility: %d success: %d\n", wud->setPointerVisible, visibilityChangeSuccessful);
            wud->setPointerVisible = 0;
            // own signal, consumed
        } else {
            useDefWindowProc = 1; // NOP for us, allow parent to act
        }
        break;
    default:
        useDefWindowProc = 1;
    }

    if (useDefWindowProc) {
        return DefWindowProc(wnd, message, wParam, lParam);
    }
    return res;
}

/*
 * Class:     jogamp_newt_driver_windows_DisplayDriver
 * Method:    DispatchMessages
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_windows_DisplayDriver_DispatchMessages0
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
            // TranslateMessage(&msg); // No more needed: We translate V_KEY -> UTF Char manually in key up/down
            DispatchMessage(&msg);
        }
    } while (gotOne && i < 100);
}

/*
 * Class:     jogamp_newt_driver_windows_ScreenDriver
 * Method:    getVirtualOriginX0
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_jogamp_newt_driver_windows_ScreenDriver_getVirtualOriginX0
  (JNIEnv *env, jobject obj)
{
    if( GetSystemMetrics( SM_CMONITORS) > 1) {
        return (jint)GetSystemMetrics(SM_XVIRTUALSCREEN);
    } else {
        return 0;
    }
}

/*
 * Class:     jogamp_newt_driver_windows_ScreenDriver
 * Method:    getVirtualOriginY0
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_jogamp_newt_driver_windows_ScreenDriver_getVirtualOriginY0
  (JNIEnv *env, jobject obj)
{
    if( GetSystemMetrics( SM_CMONITORS ) > 1) {
        return (jint)GetSystemMetrics(SM_YVIRTUALSCREEN);
    } else {
        return 0;
    }
}

/*
 * Class:     jogamp_newt_driver_windows_ScreenDriver
 * Method:    getVirtualWidthImpl
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_jogamp_newt_driver_windows_ScreenDriver_getVirtualWidthImpl0
  (JNIEnv *env, jobject obj)
{
    if( GetSystemMetrics( SM_CMONITORS) > 1) {
        return (jint)GetSystemMetrics(SM_CXVIRTUALSCREEN);
    } else {
        return (jint)GetSystemMetrics(SM_CXSCREEN);
    }
}

/*
 * Class:     jogamp_newt_driver_windows_ScreenDriver
 * Method:    getVirtualHeightImpl
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_jogamp_newt_driver_windows_ScreenDriver_getVirtualHeightImpl0
  (JNIEnv *env, jobject obj)
{
    if( GetSystemMetrics( SM_CMONITORS ) > 1) {
        return (jint)GetSystemMetrics(SM_CYVIRTUALSCREEN);
    } else {
        return (jint)GetSystemMetrics(SM_CYSCREEN);
    }
}

static int NewtScreen_RotationNativeCCW2NewtCCW(JNIEnv *env, int native) {
    int newt;
    switch (native) {
        case DMDO_DEFAULT:
            newt = 0;
            break;
        case DMDO_90:
            newt = 90;
            break;
        case DMDO_180:
            newt = 180;
            break;
        case DMDO_270:
            newt = 270;
            break;
        default:
            NewtCommon_throwNewRuntimeException(env, "invalid native rotation: %d", native);
        break;
    }
    return newt;
}

static int NewtScreen_RotationNewtCCW2NativeCCW(JNIEnv *env, jint newt) {
    int native;
    switch (newt) {
        case 0:
            native = DMDO_DEFAULT;
            break;
        case 90:
            native = DMDO_90;
            break;
        case 180:
            native = DMDO_180;
            break;
        case 270:
            native = DMDO_270;
            break;
        default:
            NewtCommon_throwNewRuntimeException(env, "invalid newt rotation: %d", newt);
    }
    return native;
}

static LPCTSTR NewtScreen_getAdapterName(DISPLAY_DEVICE * device, int crt_idx) {
    memset(device, 0, sizeof(DISPLAY_DEVICE)); 
    device->cb = sizeof(DISPLAY_DEVICE);
    if( FALSE == EnumDisplayDevices(NULL, crt_idx, device, 0) ) {
        DBG_PRINT("*** WindowsWindow: getAdapterName.EnumDisplayDevices(crt_idx %d) -> FALSE\n", crt_idx);
        return NULL;
    }

    if( NULL == device->DeviceName || 0 == _tcslen(device->DeviceName) ) {
        return NULL;
    }

    return device->DeviceName;
}

static LPCTSTR NewtScreen_getMonitorName(LPCTSTR adapterName, DISPLAY_DEVICE * device, int monitor_idx, BOOL onlyActive) {
    memset(device, 0, sizeof(DISPLAY_DEVICE)); 
    device->cb = sizeof(DISPLAY_DEVICE);
    if( 0 == monitor_idx ) {
        if( FALSE == EnumDisplayDevices(adapterName, monitor_idx, device, 0) ) {
            DBG_PRINT("*** WindowsWindow: getDisplayName.EnumDisplayDevices(monitor_idx %d).adapter -> FALSE\n", monitor_idx);
            return NULL;
        }
    }

    if( onlyActive && 0 == ( device->StateFlags & DISPLAY_DEVICE_ACTIVE ) ) {
        DBG_PRINT("*** WindowsWindow: !DISPLAY_DEVICE_ACTIVE(monitor_idx %d).display\n", monitor_idx);
        return NULL;
    }
    if( NULL == device->DeviceName || 0 == _tcslen(device->DeviceName) ) {
        return NULL;
    }

    return device->DeviceName;
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_windows_ScreenDriver_dumpMonitorInfo0
  (JNIEnv *env, jclass clazz)
{
    DISPLAY_DEVICE aDevice, dDevice;
    int i = 0, j;
    LPCTSTR aName, dName;
    while(NULL != (aName = NewtScreen_getAdapterName(&aDevice, i))) {
        fprintf(stderr, "*** [%d]: <%s> flags 0x%X active %d\n", i, aName, aDevice.StateFlags, ( 0 != ( aDevice.StateFlags & DISPLAY_DEVICE_ACTIVE ) ) );
        j=0;
        while(NULL != (dName = NewtScreen_getMonitorName(aName, &dDevice, j, FALSE))) {
            fprintf(stderr, "*** [%d][%d]: <%s> flags 0x%X active %d\n", i, j, dName, dDevice.StateFlags, ( 0 != ( dDevice.StateFlags & DISPLAY_DEVICE_ACTIVE ) ) );
            j++;
        }
        i++;
    }
}

static HDC NewtScreen_createDisplayDC(LPCTSTR displayDeviceName) {
    return CreateDC("DISPLAY", displayDeviceName, NULL, NULL);
}

/*
 * Class:     jogamp_newt_driver_windows_ScreenDriver
 * Method:    getAdapterName0
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_jogamp_newt_driver_windows_ScreenDriver_getAdapterName0
  (JNIEnv *env, jobject obj, jint crt_idx)
{
    DISPLAY_DEVICE device;
    LPCTSTR adapterName = NewtScreen_getAdapterName(&device, crt_idx);
    DBG_PRINT("*** WindowsWindow: getAdapterName(crt_idx %d) -> %s, active %d\n", crt_idx, 
              (NULL==adapterName?"nil":adapterName), 0 == ( device.StateFlags & DISPLAY_DEVICE_ACTIVE ));
    if(NULL == adapterName) {
        return NULL;
    }
#ifdef UNICODE
    return (*env)->NewString(env, adapterName, wcslen(adapterName));
#else
    return (*env)->NewStringUTF(env, adapterName);
#endif
}

/*
 * Class:     jogamp_newt_driver_windows_ScreenDriver
 * Method:    getActiveMonitorName0
 * Signature: (Ljava/lang/String;I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_jogamp_newt_driver_windows_ScreenDriver_getActiveMonitorName0
  (JNIEnv *env, jobject obj, jstring jAdapterName, jint monitor_idx)
{
    DISPLAY_DEVICE device;
    LPCTSTR monitorName;
#ifdef UNICODE
    LPCTSTR adapterName = NewtCommon_GetNullTerminatedStringChars(env, jAdapterName);
    monitorName = NewtScreen_getMonitorName(adapterName, &device, monitor_idx, TRUE);
    DBG_PRINT("*** WindowsWindow: getMonitorName(%s, monitor_idx %d) -> %s\n", adapterName, monitor_idx, (NULL==monitorName?"nil":monitorName));
    free((void*) adapterName);
#else
    LPCTSTR adapterName = (*env)->GetStringUTFChars(env, jAdapterName, NULL);
    monitorName = NewtScreen_getMonitorName(adapterName, &device, monitor_idx, TRUE);
    DBG_PRINT("*** WindowsWindow: getMonitorName(%s, monitor_idx %d) -> %s\n", adapterName, monitor_idx, (NULL==monitorName?"nil":monitorName));
    (*env)->ReleaseStringUTFChars(env, jAdapterName, adapterName);
#endif
    if(NULL == monitorName) {
        return NULL;
    }
#ifdef UNICODE
    return (*env)->NewString(env, monitorName, wcslen(monitorName));
#else
    return (*env)->NewStringUTF(env, monitorName);
#endif
}

/*
 * Class:     jogamp_newt_driver_windows_ScreenDriver
 * Method:    getMonitorMode0
 * Signature: (Ljava/lang/String;I)[I
 */
JNIEXPORT jintArray JNICALL Java_jogamp_newt_driver_windows_ScreenDriver_getMonitorMode0
  (JNIEnv *env, jobject obj, jstring jAdapterName, jint mode_idx)
{
    DISPLAY_DEVICE device;
    LPCTSTR adapterName;
    {
#ifdef UNICODE
        adapterName = NewtCommon_GetNullTerminatedStringChars(env, jAdapterName);
#else
        adapterName = (*env)->GetStringUTFChars(env, jAdapterName, NULL);
#endif
    }
    int devModeID;
    if(-1 < mode_idx) {
        devModeID = (int) mode_idx;
    } else {
        devModeID = ENUM_CURRENT_SETTINGS;
    }

    DEVMODE dm;
    ZeroMemory(&dm, sizeof(dm));
    dm.dmSize = sizeof(dm);
    
    int res = EnumDisplaySettingsEx(adapterName, devModeID, &dm, ( ENUM_CURRENT_SETTINGS == devModeID ) ? 0 : EDS_ROTATEDMODE);
    DBG_PRINT("*** WindowsWindow: getMonitorMode.EnumDisplaySettingsEx(%s, mode_idx %d/%d) -> %d\n", adapterName, mode_idx, devModeID, res);
#ifdef UNICODE
    free((void*) adapterName);
#else
    (*env)->ReleaseStringUTFChars(env, jAdapterName, adapterName);
#endif

    if (0 == res) {
        return (*env)->NewIntArray(env, 0);
    }

    // swap width and height, since Windows reflects rotated dimension, we don't
    if (DMDO_90 == dm.dmDisplayOrientation || DMDO_270 == dm.dmDisplayOrientation) {
        int tempWidth = dm.dmPelsWidth;
        dm.dmPelsWidth = dm.dmPelsHeight;
        dm.dmPelsHeight = tempWidth;
    }

    int flags = 0;
    if( 0 != ( dm.dmDisplayFlags & DM_INTERLACED ) ) {
        flags |= FLAG_INTERLACE;
    }

    jint prop[ NUM_MONITOR_MODE_PROPERTIES_ALL ];
    int propIndex = 0;

    prop[propIndex++] = NUM_MONITOR_MODE_PROPERTIES_ALL;
    prop[propIndex++] = dm.dmPelsWidth;
    prop[propIndex++] = dm.dmPelsHeight;
    prop[propIndex++] = dm.dmBitsPerPel;
    prop[propIndex++] = dm.dmDisplayFrequency * 100; // Hz*100
    prop[propIndex++] = flags;
    prop[propIndex++] = 0; // not bound to id
    prop[propIndex++] = NewtScreen_RotationNativeCCW2NewtCCW(env, dm.dmDisplayOrientation);

    jintArray properties = (*env)->NewIntArray(env, NUM_MONITOR_MODE_PROPERTIES_ALL);
    if (properties == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array of size %d", NUM_MONITOR_MODE_PROPERTIES_ALL);
    }
    (*env)->SetIntArrayRegion(env, properties, 0, NUM_MONITOR_MODE_PROPERTIES_ALL, prop);
    
    return properties;
}

/*
 * Class:     jogamp_newt_driver_windows_ScreenDriver
 * Method:    getMonitorDevice0
 * Signature: (Ljava/lang/String;I)[I
 */
JNIEXPORT jintArray JNICALL Java_jogamp_newt_driver_windows_ScreenDriver_getMonitorDevice0
  (JNIEnv *env, jobject obj, jstring jAdapterName, jint monitor_idx)
{
    DISPLAY_DEVICE device;
    LPCTSTR adapterName;
    {
#ifdef UNICODE
        adapterName = NewtCommon_GetNullTerminatedStringChars(env, jAdapterName);
#else
        adapterName = (*env)->GetStringUTFChars(env, jAdapterName, NULL);
#endif
    }

    HDC hdc = NewtScreen_createDisplayDC(adapterName);
    int widthmm = GetDeviceCaps(hdc, HORZSIZE);
    int heightmm = GetDeviceCaps(hdc, VERTSIZE);
    DeleteDC(hdc);
    int devModeID = ENUM_CURRENT_SETTINGS;

    DEVMODE dm;
    ZeroMemory(&dm, sizeof(dm));
    dm.dmSize = sizeof(dm);
    
    int res = EnumDisplaySettingsEx(adapterName, devModeID, &dm, 0);
    DBG_PRINT("*** WindowsWindow: getMonitorDevice.EnumDisplaySettingsEx(%s, devModeID %d) -> %d\n", adapterName, devModeID, res);
#ifdef UNICODE
    free((void*) adapterName);
#else
    (*env)->ReleaseStringUTFChars(env, jAdapterName, adapterName);
#endif
    if (0 == res) {
        return (*env)->NewIntArray(env, 0);
    }
    
    jsize propCount = MIN_MONITOR_DEVICE_PROPERTIES - 1 - NUM_MONITOR_MODE_PROPERTIES;
    jint prop[ propCount ];
    int propIndex = 0;

    prop[propIndex++] = propCount;
    prop[propIndex++] = monitor_idx;
    prop[propIndex++] = widthmm;
    prop[propIndex++] = heightmm;
    prop[propIndex++] = dm.dmPosition.x; // rotated viewport
    prop[propIndex++] = dm.dmPosition.y; // rotated viewport
    prop[propIndex++] = dm.dmPelsWidth;  // rotated viewport
    prop[propIndex++] = dm.dmPelsHeight; // rotated viewport

    jintArray properties = (*env)->NewIntArray(env, propCount);
    if (properties == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array of size %d", propCount);
    }
    (*env)->SetIntArrayRegion(env, properties, 0, propCount, prop);
    
    return properties;
}

/*
 * Class:     jogamp_newt_driver_windows_ScreenDriver
 * Method:    setMonitorMode0
 * Signature: (IIIIIIIII)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_windows_ScreenDriver_setMonitorMode0
  (JNIEnv *env, jobject object, jint monitor_idx, jint x, jint y, jint width, jint height, jint bits, jint rate, jint flags, jint rot)
{
    DISPLAY_DEVICE adapterDevice, monitorDevice;
    LPCTSTR adapterName = NewtScreen_getAdapterName(&adapterDevice, monitor_idx);
    if(NULL == adapterName) {
        DBG_PRINT("*** WindowsWindow: setMonitorMode.getAdapterName(monitor_idx %d) -> NULL\n", monitor_idx);
        return JNI_FALSE;
    }
    LPCTSTR monitorName = NewtScreen_getMonitorName(adapterName, &monitorDevice, 0, TRUE);
    if(NULL == monitorName) {
        DBG_PRINT("*** WindowsWindow: setMonitorMode.getMonitorName(monitor_idx 0) -> NULL\n");
        return JNI_FALSE;
    }

    DEVMODE dm;
    // initialize the DEVMODE structure
    ZeroMemory(&dm, sizeof(dm));
    dm.dmSize = sizeof(dm);
    if( 0 <= x && 0 <= y ) {
        dm.dmPosition.x = (int)x;
        dm.dmPosition.y = (int)y;
    }
    dm.dmPelsWidth = (int)width;
    dm.dmPelsHeight = (int)height;
    dm.dmBitsPerPel = (int)bits;
    dm.dmDisplayFrequency = (int)rate;
    if( 0 != ( flags & FLAG_INTERLACE ) ) {
        dm.dmDisplayFlags |= DM_INTERLACED;
    }
    dm.dmDisplayOrientation = NewtScreen_RotationNewtCCW2NativeCCW(env, rot);

    // swap width and height, since Windows reflects rotated dimension, we don't
    if ( DMDO_90 == dm.dmDisplayOrientation || DMDO_270 == dm.dmDisplayOrientation ) {
        int tempWidth = dm.dmPelsWidth;
        dm.dmPelsWidth = dm.dmPelsHeight;
        dm.dmPelsHeight = tempWidth;
    }

    dm.dmFields = DM_DISPLAYORIENTATION | DM_PELSWIDTH | DM_PELSHEIGHT | DM_BITSPERPEL | DM_DISPLAYFREQUENCY | DM_DISPLAYFLAGS;
    if( 0 <= x && 0 <= y ) {
        dm.dmFields |= DM_POSITION;
    }
    
    return ( DISP_CHANGE_SUCCESSFUL == ChangeDisplaySettingsEx(adapterName, &dm, NULL, 0, NULL) ) ? JNI_TRUE : JNI_FALSE ;
}

/*
 * Class:     jogamp_newt_driver_windows_WindowDriver
 * Method:    initIDs0
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_windows_WindowDriver_initIDs0
  (JNIEnv *env, jclass clazz, jlong hInstance)
{
    NewtCommon_init(env);

    insetsChangedID = (*env)->GetMethodID(env, clazz, "insetsChanged", "(ZIIII)V");
    sizeChangedID = (*env)->GetMethodID(env, clazz, "sizeChanged", "(ZIIZ)V");
    positionChangedID = (*env)->GetMethodID(env, clazz, "positionChanged", "(ZII)V");
    focusChangedID = (*env)->GetMethodID(env, clazz, "focusChanged", "(ZZ)V");
    visibleChangedID = (*env)->GetMethodID(env, clazz, "visibleChanged", "(ZZ)V");
    windowDestroyNotifyID = (*env)->GetMethodID(env, clazz, "windowDestroyNotify", "(Z)Z");
    windowRepaintID = (*env)->GetMethodID(env, clazz, "windowRepaint", "(ZIIII)V");
    sendMouseEventID = (*env)->GetMethodID(env, clazz, "sendMouseEvent", "(SIIISF)V");
    sendTouchScreenEventID = (*env)->GetMethodID(env, clazz, "sendTouchScreenEvent", "(SII[I[I[I[FF)V");
    sendKeyEventID = (*env)->GetMethodID(env, clazz, "sendKeyEvent", "(SISSC)V");
    requestFocusID = (*env)->GetMethodID(env, clazz, "requestFocus", "(Z)V");

    if (insetsChangedID == NULL ||
        sizeChangedID == NULL ||
        positionChangedID == NULL ||
        focusChangedID == NULL ||
        visibleChangedID == NULL ||
        windowDestroyNotifyID == NULL ||
        windowRepaintID == NULL ||
        sendMouseEventID == NULL ||
        sendTouchScreenEventID == NULL ||
        sendKeyEventID == NULL ||
        requestFocusID == NULL) {
        return JNI_FALSE;
    }
    InitKeyMapTableScanCode(env);

    return JNI_TRUE;
}

/*
 * Class:     jogamp_newt_driver_windows_WindowDriver
 * Method:    getNewtWndProc0
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_windows_WindowDriver_getNewtWndProc0
  (JNIEnv *env, jclass clazz)
{
    return (jlong) (intptr_t) wndProc;
}

static void NewtWindow_setVisiblePosSize(HWND hwnd, BOOL atop, BOOL visible, 
                                         int x, int y, int width, int height)
{
    UINT flags;
    BOOL bRes;
    
    DBG_PRINT("*** WindowsWindow: NewtWindow_setVisiblePosSize %d/%d %dx%d, atop %d, visible %d\n", 
        x, y, width, height, atop, visible);

    if(visible) {
        flags = SWP_SHOWWINDOW;
    } else {
        flags = SWP_NOACTIVATE | SWP_NOZORDER;
    }
    if(0>=width || 0>=height ) {
        flags |= SWP_NOSIZE;
    }

    if(atop) {
        SetWindowPos(hwnd, HWND_TOP, x, y, width, height, flags);
        SetWindowPos(hwnd, HWND_TOPMOST, x, y, width, height, flags);
    } else {
        SetWindowPos(hwnd, HWND_NOTOPMOST, x, y, width, height, flags);
        SetWindowPos(hwnd, HWND_TOP, x, y, width, height, flags);
    }
    // SetWindowPos(hwnd, atop ? HWND_TOPMOST : HWND_TOP, x, y, width, height, flags);

    InvalidateRect(hwnd, NULL, TRUE);
    UpdateWindow(hwnd);
}

#define WS_DEFAULT_STYLES (WS_CLIPSIBLINGS | WS_CLIPCHILDREN | WS_TABSTOP)

/*
 * Class:     jogamp_newt_driver_windows_WindowDriver
 * Method:    CreateWindow
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_windows_WindowDriver_CreateWindow0
  (JNIEnv *env, jobject obj, 
   jlong hInstance, jstring jWndClassName, jstring jWndName, jint winMajor, jint winMinor,
   jlong parent, jint jx, jint jy, jint defaultWidth, jint defaultHeight, jboolean autoPosition, jint flags)
{
    HWND parentWindow = (HWND) (intptr_t) parent;
    const TCHAR* wndClassName = NULL;
    const TCHAR* wndName = NULL;
    DWORD windowStyle = WS_DEFAULT_STYLES | WS_VISIBLE;
    int x=(int)jx, y=(int)jy;
    int width=(int)defaultWidth, height=(int)defaultHeight;
    HWND window = NULL;
    int _x = x, _y = y; // pos for CreateWindow, might be tweaked

#ifdef UNICODE
    wndClassName = NewtCommon_GetNullTerminatedStringChars(env, jWndClassName);
    wndName = NewtCommon_GetNullTerminatedStringChars(env, jWndName);
#else
    wndClassName = (*env)->GetStringUTFChars(env, jWndClassName, NULL);
    wndName = (*env)->GetStringUTFChars(env, jWndName, NULL);
#endif

    if( NULL!=parentWindow ) {
        if (!IsWindow(parentWindow)) {
            DBG_PRINT("*** WindowsWindow: CreateWindow failure: Passed parentWindow %p is invalid\n", parentWindow);
            return 0;
        }
        windowStyle |= WS_CHILD ;
    } else if ( TST_FLAG_IS_UNDECORATED(flags) ) {
        windowStyle |= WS_POPUP | WS_SYSMENU | WS_MAXIMIZEBOX | WS_MINIMIZEBOX;
    } else {
        windowStyle |= WS_OVERLAPPEDWINDOW;
        if(JNI_TRUE == autoPosition) {
            // user didn't requested specific position, use WM default
            _x = CW_USEDEFAULT;
            _y = 0;
        }
    }

    window = CreateWindow(wndClassName, wndName, windowStyle,
                          _x, _y, width, height,
                          parentWindow, NULL,
                          (HINSTANCE) (intptr_t) hInstance,
                          NULL);

    DBG_PRINT("*** WindowsWindow: CreateWindow thread 0x%X, win %d.%d parent %p, window %p, %d/%d %dx%d, undeco %d, alwaysOnTop %d, autoPosition %d\n", 
        (int)GetCurrentThreadId(), winMajor, winMinor, parentWindow, window, x, y, width, height,
        TST_FLAG_IS_UNDECORATED(flags), TST_FLAG_IS_ALWAYSONTOP(flags), autoPosition);

    if (NULL == window) {
        int lastError = (int) GetLastError();
        DBG_PRINT("*** WindowsWindow: CreateWindow failure: 0x%X %d\n", lastError, lastError);
        return 0;
    } else {
        WindowUserData * wud = (WindowUserData *) malloc(sizeof(WindowUserData));
        wud->jinstance = (*env)->NewGlobalRef(env, obj);
        wud->jenv = env;
        wud->width = width;
        wud->height = height;
        wud->setPointerVisible = 0;
        wud->mouseInside = 0;
        wud->touchDownCount = 0;
        wud->supportsMTouch = 0;
        if ( winMajor > 6 || ( winMajor == 6 && winMinor >= 1 ) ) {
            int value = GetSystemMetrics(SM_DIGITIZER);
            if (value & NID_READY) { /* ready */
                if (value  & NID_MULTI_INPUT) { /* multitouch */
                    wud->supportsMTouch = 1;
                }
                if (value & NID_INTEGRATED_TOUCH) { /* Integrated touch */
                }
            }
        }
        DBG_PRINT("*** WindowsWindow: CreateWindow supportsMTouch %d\n", wud->supportsMTouch);

#if !defined(__MINGW64__) && ( defined(UNDER_CE) || _MSC_VER <= 1200 )
        SetWindowLong(window, GWL_USERDATA, (intptr_t) wud);
#else
        SetWindowLongPtr(window, GWLP_USERDATA, (intptr_t) wud);
#endif

        // gather and adjust position and size
        {
            RECT rc;
            RECT * insets;

            ShowWindow(window, SW_SHOW);

            // send insets before visibility, allowing java code a proper sync point!
            insets = UpdateInsets(env, wud->jinstance, window);
            (*env)->CallVoidMethod(env, wud->jinstance, visibleChangedID, JNI_FALSE, JNI_TRUE);

            if(JNI_TRUE == autoPosition) {
                GetWindowRect(window, &rc);
                x = rc.left + insets->left; // client coords
                y = rc.top + insets->top;   // client coords
            }
            DBG_PRINT("*** WindowsWindow: CreateWindow client: %d/%d %dx%d (autoPosition %d)\n", x, y, width, height, autoPosition);

            x -= insets->left; // top-level
            y -= insets->top;  // top-level
            width += insets->left + insets->right;   // top-level
            height += insets->top + insets->bottom;  // top-level
            DBG_PRINT("*** WindowsWindow: CreateWindow top-level %d/%d %dx%d\n", x, y, width, height);

            NewtWindow_setVisiblePosSize(window, TST_FLAG_IS_ALWAYSONTOP(flags), TRUE, x, y, width, height);
        }
        if( wud->supportsMTouch ) {
            RegisterTouchWindow(window, 0);
        }
    }

#ifdef UNICODE
    free((void*) wndClassName);
    free((void*) wndName);
#else
    (*env)->ReleaseStringUTFChars(env, jWndClassName, wndClassName);
    (*env)->ReleaseStringUTFChars(env, jWndName, wndName);
#endif

#ifdef TEST_MOUSE_HOOKS
    hookLLMP = SetWindowsHookEx(WH_MOUSE_LL, &HookLowLevelMouseProc, (HINSTANCE) (intptr_t) hInstance, 0);
    hookMP = SetWindowsHookEx(WH_MOUSE_LL, &HookMouseProc, (HINSTANCE) (intptr_t) hInstance, 0);
    DBG_PRINT("**** LLMP Hook %p, MP Hook %p\n", hookLLMP, hookMP);
#endif

    return (jlong) (intptr_t) window;
}

/*
 * Class:     jogamp_newt_driver_windows_WindowDriver
 * Method:    MonitorFromWindow
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_windows_WindowDriver_MonitorFromWindow0
  (JNIEnv *env, jobject obj, jlong window)
{
    #if (_WIN32_WINNT >= 0x0500 || _WIN32_WINDOWS >= 0x0410 || WINVER >= 0x0500) && !defined(_WIN32_WCE)
        return (jlong) (intptr_t) MonitorFromWindow((HWND) (intptr_t) window, MONITOR_DEFAULTTOPRIMARY);
    #else
        return 0;
    #endif
}

static jboolean NewtWindows_setFullScreen(jboolean fullscreen)
{
    int flags = 0;
    DEVMODE dm;
    // initialize the DEVMODE structure
    ZeroMemory(&dm, sizeof(dm));
    dm.dmSize = sizeof(dm);

    if (0 == EnumDisplaySettings(NULL /*current display device*/, ENUM_CURRENT_SETTINGS, &dm))
    {
        return JNI_FALSE;
    }
    
    flags = ( JNI_TRUE == fullscreen ) ? CDS_FULLSCREEN : CDS_RESET ;

    return ( DISP_CHANGE_SUCCESSFUL == ChangeDisplaySettings(&dm, flags) ) ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     jogamp_newt_driver_windows_WindowDriver
 * Method:    reconfigureWindow0
 * Signature: (JJIIIII)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_windows_WindowDriver_reconfigureWindow0
  (JNIEnv *env, jobject obj, jlong parent, jlong window,
   jint x, jint y, jint width, jint height, jint flags)
{
    HWND hwndP = (HWND) (intptr_t) parent;
    HWND hwnd = (HWND) (intptr_t) window;
    DWORD windowStyle = WS_DEFAULT_STYLES;
    BOOL styleChange = TST_FLAG_CHANGE_DECORATION(flags) || TST_FLAG_CHANGE_FULLSCREEN(flags) || TST_FLAG_CHANGE_PARENTING(flags) ;

    DBG_PRINT( "*** WindowsWindow: reconfigureWindow0 parent %p, window %p, %d/%d %dx%d, parentChange %d, hasParent %d, decorationChange %d, undecorated %d, fullscreenChange %d, fullscreen %d, alwaysOnTopChange %d, alwaysOnTop %d, visibleChange %d, visible %d -> styleChange %d\n",
        parent, window, x, y, width, height,
        TST_FLAG_CHANGE_PARENTING(flags),   TST_FLAG_HAS_PARENT(flags),
        TST_FLAG_CHANGE_DECORATION(flags),  TST_FLAG_IS_UNDECORATED(flags),
        TST_FLAG_CHANGE_FULLSCREEN(flags),  TST_FLAG_IS_FULLSCREEN(flags),
        TST_FLAG_CHANGE_ALWAYSONTOP(flags), TST_FLAG_IS_ALWAYSONTOP(flags),
        TST_FLAG_CHANGE_VISIBILITY(flags), TST_FLAG_IS_VISIBLE(flags), styleChange);

    if (!IsWindow(hwnd)) {
        DBG_PRINT("*** WindowsWindow: reconfigureWindow0 failure: Passed window %p is invalid\n", (void*)hwnd);
        return;
    }

    if (NULL!=hwndP && !IsWindow(hwndP)) {
        DBG_PRINT("*** WindowsWindow: reconfigureWindow0 failure: Passed parent window %p is invalid\n", (void*)hwndP);
        return;
    }

    if(TST_FLAG_IS_VISIBLE(flags)) {
        windowStyle |= WS_VISIBLE ;
    }
    
    // order of call sequence: (MS documentation)
    //    TOP:  SetParent(.., NULL); Clear WS_CHILD [, Set WS_POPUP]
    //  CHILD:  Set WS_CHILD [, Clear WS_POPUP]; SetParent(.., PARENT) 
    //
    if( TST_FLAG_CHANGE_PARENTING(flags) && NULL == hwndP ) {
        // TOP: in -> out
        SetParent(hwnd, NULL);
    }
    
    if( TST_FLAG_CHANGE_FULLSCREEN(flags) && TST_FLAG_IS_FULLSCREEN(flags) ) { // FS on
        // TOP: in -> out
        NewtWindows_setFullScreen(JNI_TRUE);
    }

    if ( styleChange ) {
        if(NULL!=hwndP) {
            windowStyle |= WS_CHILD ;
        } else if ( TST_FLAG_IS_UNDECORATED(flags) ) {
            windowStyle |= WS_POPUP | WS_SYSMENU | WS_MAXIMIZEBOX | WS_MINIMIZEBOX;
        } else {
            windowStyle |= WS_OVERLAPPEDWINDOW;
        }
        SetWindowLong(hwnd, GWL_STYLE, windowStyle);
        SetWindowPos(hwnd, 0, 0, 0, 0, 0, SWP_FRAMECHANGED | SWP_NOSIZE | SWP_NOMOVE | SWP_NOACTIVATE | SWP_NOZORDER );
    }

    if( TST_FLAG_CHANGE_FULLSCREEN(flags) && !TST_FLAG_IS_FULLSCREEN(flags) ) { // FS off
        // CHILD: out -> in
        NewtWindows_setFullScreen(JNI_FALSE);
    }

    if ( TST_FLAG_CHANGE_PARENTING(flags) && NULL != hwndP ) {
        // CHILD: out -> in
        SetParent(hwnd, hwndP );
    }

    NewtWindow_setVisiblePosSize(hwnd, TST_FLAG_IS_ALWAYSONTOP(flags), TST_FLAG_IS_VISIBLE(flags), x, y, width, height);

    if( TST_FLAG_CHANGE_VISIBILITY(flags) ) {
        if( TST_FLAG_IS_VISIBLE(flags) ) {
            ShowWindow(hwnd, SW_SHOW);
        } else {
            ShowWindow(hwnd, SW_HIDE);
        }
    }

    DBG_PRINT("*** WindowsWindow: reconfigureWindow0.X\n");
}

/*
 * Class:     jogamp_newt_driver_windows_WindowDriver
 * Method:    setTitle
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_windows_WindowDriver_setTitle0
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
 * Class:     jogamp_newt_driver_windows_WindowDriver
 * Method:    requestFocus
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_windows_WindowDriver_requestFocus0
  (JNIEnv *env, jobject obj, jlong window, jboolean force)
{
    DBG_PRINT("*** WindowsWindow: RequestFocus0\n");
    NewtWindows_requestFocus ( env, obj, (HWND) (intptr_t) window, force) ;
}

/*
 * Class:     Java_jogamp_newt_driver_windows_WindowDriver
 * Method:    setPointerVisible0
 * Signature: (JJZ)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_windows_WindowDriver_setPointerVisible0
  (JNIEnv *env, jclass clazz, jlong window, jboolean mouseVisible)
{
    HWND hwnd = (HWND) (intptr_t) window;
    WindowUserData * wud;
#if !defined(__MINGW64__) && ( defined(UNDER_CE) || _MSC_VER <= 1200 )
    wud = (WindowUserData *) GetWindowLong(hwnd, GWL_USERDATA);
#else
    wud = (WindowUserData *) GetWindowLongPtr(hwnd, GWLP_USERDATA);
#endif
    wud->setPointerVisible = mouseVisible ? 1 : -1;
    SendMessage(hwnd, WM_SETCURSOR, 0, 0);

    return JNI_TRUE;
}

/*
 * Class:     Java_jogamp_newt_driver_windows_WindowDriver
 * Method:    confinePointer0
 * Signature: (JJZIIII)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_windows_WindowDriver_confinePointer0
  (JNIEnv *env, jclass clazz, jlong window, jboolean confine, jint l, jint t, jint r, jint b)
{
    HWND hwnd = (HWND) (intptr_t) window;
    jboolean res;

    if(JNI_TRUE == confine) {
        // SetCapture(hwnd);
        // res = ( GetCapture() == hwnd ) ? JNI_TRUE : JNI_FALSE;
        RECT rect = { l, t, r, b };
        res = ClipCursor(&rect) ? JNI_TRUE : JNI_FALSE;
    } else {
        // res = ReleaseCapture() ? JNI_TRUE : JNI_FALSE;
        res = ClipCursor(NULL) ? JNI_TRUE : JNI_FALSE;
    }
    DBG_PRINT( "*** WindowsWindow: confinePointer0: %d, [ l %d t %d r %d b %d ], res %d\n", 
        confine, l, t, r, b, res);

    return res;
}

/*
 * Class:     Java_jogamp_newt_driver_windows_WindowDriver
 * Method:    warpPointer0
 * Signature: (JJII)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_windows_WindowDriver_warpPointer0
  (JNIEnv *env, jclass clazz, jlong window, jint x, jint y)
{
    DBG_PRINT( "*** WindowsWindow: warpPointer0: %d/%d\n", x, y);
    SetCursorPos(x, y);
}

