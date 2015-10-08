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

/* GetProcAddress doesn't exist in A/W variants under desktop Windows */
#ifndef UNDER_CE
#define GetProcAddressA GetProcAddress
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

#ifndef EDD_GET_DEVICE_INTERFACE_NAME
#define EDD_GET_DEVICE_INTERFACE_NAME 0x00000001
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

// #define DISPLAY_DEVICE_ATTACHED_TO_DESKTOP 0x00000001
// #define DISPLAY_DEVICE_MULTI_DRIVER 0x00000002
#ifndef DISPLAY_DEVICE_PRIMARY_DEVICE
#define DISPLAY_DEVICE_PRIMARY_DEVICE 0x00000004
#endif
#ifndef DISPLAY_DEVICE_MIRRORING_DRIVER
#define DISPLAY_DEVICE_MIRRORING_DRIVER 0x00000008
#endif
// #define DISPLAY_DEVICE_VGA_COMPATIBLE 0x00000010
// #define DISPLAY_DEVICE_REMOVABLE 0x00000020
// #define DISPLAY_DEVICE_MODESPRUNED 0x08000000
// #define DISPLAY_DEVICE_REMOTE 0x04000000
// #define DISPLAY_DEVICE_DISCONNECT 0x02000000

#ifndef DISPLAY_DEVICE_ACTIVE
#define DISPLAY_DEVICE_ACTIVE 0x00000001
#endif
#ifndef DISPLAY_DEVICE_ATTACHED
#define DISPLAY_DEVICE_ATTACHED 0x00000002
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

#include "WindowsEDID.h"

// #define VERBOSE_ON 1
// #define DEBUG_KEYS 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(x, ...) _ftprintf(stderr, __T(x), ##__VA_ARGS__); fflush(stderr) 
#else
    #define DBG_PRINT(...)
#endif

#define STD_PRINT(x, ...) _ftprintf(stderr, __T(x), ##__VA_ARGS__); fflush(stderr) 

static jmethodID insetsChangedID = NULL;
static jmethodID sizeChangedID = NULL;
static jmethodID maximizedChangedID = NULL;
static jmethodID positionChangedID = NULL;
static jmethodID focusChangedID = NULL;
static jmethodID visibleChangedID = NULL;
static jmethodID sizePosInsetsFocusVisibleChangedID = NULL;
static jmethodID windowDestroyNotifyID = NULL;
static jmethodID windowRepaintID = NULL;
static jmethodID sendMouseEventID = NULL;
static jmethodID sendTouchScreenEventID = NULL;
static jmethodID sendKeyEventID = NULL;
static jmethodID requestFocusID = NULL;

typedef WINBOOL (WINAPI *CloseTouchInputHandlePROCADDR)(HANDLE hTouchInput);
typedef WINBOOL (WINAPI *GetTouchInputInfoPROCADDR)(HANDLE hTouchInput, UINT cInputs, PTOUCHINPUT pInputs, int cbSize);
typedef WINBOOL (WINAPI *IsTouchWindowPROCADDR)(HWND hWnd,PULONG pulFlags);
typedef WINBOOL (WINAPI *RegisterTouchWindowPROCADDR)(HWND hWnd,ULONG ulFlags);
typedef WINBOOL (WINAPI *UnregisterTouchWindowPROCADDR)(HWND hWnd);

static int WinTouch_func_avail = 0;
static CloseTouchInputHandlePROCADDR WinTouch_CloseTouchInputHandle = NULL;
static GetTouchInputInfoPROCADDR WinTouch_GetTouchInputInfo = NULL;
static IsTouchWindowPROCADDR WinTouch_IsTouchWindow = NULL;
static RegisterTouchWindowPROCADDR WinTouch_RegisterTouchWindow = NULL;
static UnregisterTouchWindowPROCADDR WinTouch_UnregisterTouchWindow = NULL;

static int NewtEDID_avail = 0;

typedef struct {
    JNIEnv* jenv;
    jobject jinstance;
    /* client x-pos */
    int xpos;
    /* client y-pos */
    int ypos;
    /* client size width */
    int width;
    /* client size height */
    int height;
    /* visible state */
    BOOL visible;
    /* focused state */
    BOOL focused;
    /* Insets left, right, top, bottom */
    RECT insets;
    /** Tristate: -1 HIDE, 0 NOP, 1 SHOW */
    int setPointerVisible;
    /** Tristate: -1 RESET, 0 NOP, 1 SET-NEW */
    int setPointerAction;
    HCURSOR setPointerHandle;
    HCURSOR defPointerHandle;
    /** Bool: 0 NOP, 1 FULLSCREEN */
    BOOL isFullscreen;
    /** Bool: 0 TOP, 1 CHILD */
    BOOL isChildWindow;
    /** Bool: 0 NOP, 1 minimized/iconic */
    BOOL isMinimized;
    /** Bool: 0 NOP, 1 maximized */
    BOOL isMaximized;
    BOOL isOnBottom;
    BOOL isOnTop;
    /** Bug 1205: Clear Window Background -> security! */
    BOOL isInCreation;
    int pointerCaptured;
    int pointerInside;
    int touchDownCount;
    int touchDownLastUp; // mitigate LBUTTONUP after last TOUCH lift
    int supportsMTouch;
} WindowUserData;
    
static void UpdateInsets(JNIEnv *env, WindowUserData *wud, HWND hwnd);

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
    STD_PRINT("*** WindowsWindow: ParseWmVKeyAndScanCode winVKey 0x%X, winScanCode 0x%X, flags 0x%X -> UTF(0x%X, %c, res %d, sizeof %d), vKeys( US(win 0x%X, java 0x%X), XX(win 0x%X, java 0x%X))\n", 
        (int)winVKey, (int)winScanCode, (int)flags,
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
    WindowUserData * wud;
    BOOL isEnabled = IsWindowEnabled(hwnd);
    pHwnd = GetParent(hwnd);
    current = GetFocus();
#if !defined(__MINGW64__) && ( defined(UNDER_CE) || _MSC_VER <= 1200 )
    wud = (WindowUserData *) GetWindowLong(hwnd, GWL_USERDATA);
#else
    wud = (WindowUserData *) GetWindowLongPtr(hwnd, GWLP_USERDATA);
#endif

    DBG_PRINT("*** WindowsWindow: requestFocus.S force %d, parent %p, window %p, isEnabled %d, isCurrent %d, isOn[Top %d, Bottom %d]\n", 
        (int)force, (void*)pHwnd, (void*)hwnd, isEnabled, current==hwnd,
        wud->isOnTop, wud->isOnBottom);

    if( JNI_TRUE==force || current!=hwnd || !isEnabled ) {
        UINT flags = SWP_SHOWWINDOW | SWP_NOSIZE | SWP_NOMOVE;
        if(!isEnabled) {
            EnableWindow(hwnd, TRUE);
        }
        BOOL frontWindow;
        if( wud->isOnBottom ) {
            SetWindowPos(hwnd, HWND_BOTTOM, 0, 0, 0, 0, SWP_NOSIZE | SWP_NOMOVE | SWP_NOACTIVATE);
            frontWindow = FALSE;
        } else if( wud->isOnTop ) {
            SetWindowPos(hwnd, HWND_TOPMOST, 0, 0, 0, 0, flags);
            frontWindow = TRUE;
        } else {
            SetWindowPos(hwnd, HWND_TOP, 0, 0, 0, 0, flags);
            frontWindow = TRUE;
        }
        if( frontWindow ) {
            SetForegroundWindow(hwnd);  // Slightly Higher Priority
        }
        SetFocus(hwnd);// Sets Keyboard Focus To Window (activates parent window if exist, or this window)
        if( frontWindow && NULL!=pHwnd ) {
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

static void UpdateInsets(JNIEnv *env, WindowUserData *wud, HWND hwnd) {
    jobject window = wud->jinstance;
    RECT outside;
    RECT inside;
    int strategy = 0;

    if (IsIconic(hwnd)) {
        wud->insets.left = wud->insets.top = wud->insets.right = wud->insets.bottom = -1;
        return;
    }

    wud->insets.left = wud->insets.top = wud->insets.right = wud->insets.bottom = 0;

    GetClientRect(hwnd, &inside);
    GetWindowRect(hwnd, &outside);

    if (outside.right - outside.left > 0 && outside.bottom - outside.top > 0) {
        MapWindowPoints(hwnd, 0, (LPPOINT)&inside, 2);
        wud->insets.top = inside.top - outside.top;
        wud->insets.bottom = outside.bottom - inside.bottom;
        wud->insets.left = inside.left - outside.left;
        wud->insets.right = outside.right - inside.right;
        strategy = 1;
    } else {
        wud->insets.top = -1;
    }
    if (wud->insets.left < 0 || wud->insets.top < 0 ||
        wud->insets.right < 0 || wud->insets.bottom < 0)
    {
        LONG style = GetWindowLong(hwnd, GWL_STYLE);

        BOOL bIsUndecorated = (style & (WS_CHILD|WS_POPUP)) != 0;
        if (!bIsUndecorated) {
            /* Get outer frame sizes. */
            if (style & WS_THICKFRAME) {
                wud->insets.left = wud->insets.right =
                    GetSystemMetrics(SM_CXSIZEFRAME);
                wud->insets.top = wud->insets.bottom =
                    GetSystemMetrics(SM_CYSIZEFRAME);
            } else {
                wud->insets.left = wud->insets.right =
                    GetSystemMetrics(SM_CXDLGFRAME);
                wud->insets.top = wud->insets.bottom =
                    GetSystemMetrics(SM_CYDLGFRAME);
            }

            /* Add in title. */
            wud->insets.top += GetSystemMetrics(SM_CYCAPTION);
            strategy += 10;
        } else {
            /* undo the -1 set above */
            wud->insets.left = wud->insets.top = wud->insets.right = wud->insets.bottom = 0;
            strategy += 20;
        }
    }

    DBG_PRINT("*** WindowsWindow: UpdateInsets window %p, s %d, [l %d, r %d - t %d, b %d - %dx%d], at-init %d\n", 
        (void*)hwnd, strategy, (int)wud->insets.left, (int)wud->insets.right, (int)wud->insets.top, (int)wud->insets.bottom,
        (int) ( wud->insets.left + wud->insets.right ), (int) (wud->insets.top + wud->insets.bottom), wud->isInCreation);
    if( !wud->isInCreation ) {
        (*env)->CallVoidMethod(env, window, insetsChangedID, JNI_FALSE,
                               (int)wud->insets.left, (int)wud->insets.right, (int)wud->insets.top, (int)wud->insets.bottom);
    }
}

static void WmSize(JNIEnv *env, WindowUserData * wud, HWND wnd, UINT type)
{
    RECT rc;
    BOOL isVisible = IsWindowVisible(wnd);
    jobject window = wud->jinstance;
    BOOL maxChanged = FALSE;

    DBG_PRINT("*** WindowsWindow: WmSize.0 window %p, %dx%d, isMinimized %d, isMaximized %d, visible %d\n", 
        (void*)wnd, wud->width, wud->height, wud->isMinimized, wud->isMaximized, isVisible);

    if (type == SIZE_MINIMIZED) {
        wud->isMinimized = TRUE;
        return;
    }
    if (type == SIZE_MAXIMIZED) {
        if( !wud->isMaximized ) {
            wud->isMaximized = 1;
            maxChanged = TRUE;
        }
    } else if (type == SIZE_RESTORED) {
        wud->isMinimized = FALSE;
        if( wud->isMaximized ) {
            wud->isMaximized = FALSE;
            maxChanged = TRUE;
        }
    }

    // make sure insets are up to date
    UpdateInsets(env, wud, wnd);

    GetClientRect(wnd, &rc);
    
    // we report back the dimensions of the client area
    wud->width = (int) ( rc.right  - rc.left );
    wud->height = (int) ( rc.bottom - rc.top );

    DBG_PRINT("*** WindowsWindow: WmSize.X window %p, %dx%d, isMinimized %d, isMaximized %d (changed %d), visible %d, at-init %d\n", 
        (void*)wnd, wud->width, wud->height, wud->isMinimized, wud->isMaximized, maxChanged, isVisible, wud->isInCreation);

    if( !wud->isInCreation ) {
        if( maxChanged ) {
            jboolean v = wud->isMaximized ? JNI_TRUE : JNI_FALSE;
            (*env)->CallVoidMethod(env, window, maximizedChangedID, v, v);
        }
        (*env)->CallVoidMethod(env, window, sizeChangedID, JNI_FALSE, wud->width, wud->height, JNI_FALSE);
    }
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
    
// #define DO_ERASEBKGND 1

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

        case WM_ACTIVATE: {
                HWND wndPrev = (HWND) lParam;
                BOOL fMinimized = (BOOL) HIWORD(wParam);
                int fActive = LOWORD(wParam);
                BOOL inactive = WA_INACTIVE==fActive;
                #ifdef VERBOSE_ON
                    BOOL anyActive = WA_ACTIVE==fActive, clickActive = WA_CLICKACTIVE==fActive;
                    DBG_PRINT("*** WindowsWindow: WM_ACTIVATE window %p, prev %p, minimized %d, active %d (any %d, click %d, inactive %d), FS %d\n", 
                        wnd, wndPrev, fMinimized, fActive, anyActive, clickActive, inactive, wud->isFullscreen);
                #endif
                if( wud->isFullscreen ) {
                    // Bug 916 - NEWT Fullscreen Mode on Windows ALT-TAB doesn't allow Application Switching
                    // Remedy for 'some' display drivers, i.e. Intel HD: 
                    // Explicitly push fullscreen window to BOTTOM when inactive (ALT-TAB)
                    if( inactive || wud->isOnBottom ) {
                        SetWindowPos(wnd, HWND_BOTTOM, 0, 0, 0, 0, SWP_NOSIZE | SWP_NOMOVE | SWP_NOACTIVATE);
                    } else {
                        UINT flags = SWP_SHOWWINDOW | SWP_NOSIZE | SWP_NOMOVE;
                        if( wud->isOnTop ) {
                            SetWindowPos(wnd, HWND_TOPMOST, 0, 0, 0, 0, flags);
                        } else {
                            SetWindowPos(wnd, HWND_TOP, 0, 0, 0, 0, flags);
                        }
                        SetForegroundWindow(wnd);  // Slightly Higher Priority
                    }
                }
                useDefWindowProc = 1;
            }
            break;

        case WM_WINDOWPOSCHANGING: {
                WINDOWPOS *p = (WINDOWPOS*)lParam;
                BOOL isThis = wnd == p->hwnd;
                BOOL isBottom = HWND_BOTTOM == p->hwndInsertAfter;
                BOOL isTopMost = HWND_TOPMOST == p->hwndInsertAfter;
                BOOL forceBottom = isThis && wud->isOnBottom && !isBottom;
                BOOL forceTop = isThis && wud->isOnTop && !isTopMost;
                #ifdef VERBOSE_ON
                    BOOL isNoTopMost = HWND_NOTOPMOST == p->hwndInsertAfter;
                    BOOL isTop = HWND_TOP == p->hwndInsertAfter;
                    BOOL isNoZ = 0 != ( SWP_NOZORDER & p->flags );
                    DBG_PRINT("*** WindowsWindow: WM_WINDOWPOSCHANGING window %p / %p (= %d), %p[bottom %d, notop %d, top %d, topmost %d, noZ %d, force[Top %d, Bottom %d], %d/%d %dx%d 0x%X\n", 
                        wnd, p->hwnd, isThis, 
                        p->hwndInsertAfter, isBottom, isNoTopMost, isTop, isTopMost, isNoZ,
                        forceTop, forceBottom,
                        p->x, p->y, p->cx, p->cy, p->flags);
                #endif
                if( forceTop ) {
                    p->hwndInsertAfter = HWND_TOPMOST;
                    p->flags &= ~SWP_NOZORDER;
                } else if( forceBottom ) {
                    p->hwndInsertAfter = HWND_BOTTOM;
                    p->flags &= ~SWP_NOZORDER;
                }
                useDefWindowProc = 1;
            }
            break;

        case WM_SETTINGCHANGE:
            if (wParam == SPI_SETNONCLIENTMETRICS) {
                // make sure insets are updated, we don't need to resize the window 
                // because the size of the client area doesn't change
                UpdateInsets(env, wud, wnd);
            } else {
                useDefWindowProc = 1;
            }
            break;

        case WM_SIZE:
            WmSize(env, wud, wnd, (UINT)wParam);
            break;

        case WM_SHOWWINDOW:
            DBG_PRINT("*** WindowsWindow: WM_SHOWWINDOW window %p: %d, at-init %d\n", wnd, wParam==TRUE, wud->isInCreation);
            wud->visible = wParam==TRUE;
            if( !wud->isInCreation ) {
                (*env)->CallVoidMethod(env, window, visibleChangedID, JNI_FALSE, wParam==TRUE?JNI_TRUE:JNI_FALSE);
            }
            break;

        case WM_MOVE:
            wud->xpos = (int)GET_X_LPARAM(lParam);
            wud->ypos = (int)GET_Y_LPARAM(lParam);
            DBG_PRINT("*** WindowsWindow: WM_MOVE window %p, %d/%d, at-init %d\n", wnd, wud->xpos, wud->ypos, wud->isInCreation);
            if( !wud->isInCreation ) {
                (*env)->CallVoidMethod(env, window, positionChangedID, JNI_FALSE, (jint)wud->xpos, (jint)wud->ypos);
            }
            useDefWindowProc = 1;
            break;

        case WM_PAINT: {
            if( wud->isInCreation ) {
                #ifdef DO_ERASEBKGND
                if (GetUpdateRect(wnd, NULL, TRUE /* erase background */)) {
                    DBG_PRINT("*** WindowsWindow: WM_PAINT.0 (dirty)\n");
                    // WM_ERASEBKGND sent!
                #else
                if (GetUpdateRect(wnd, NULL, FALSE /* do not erase background */)) {
                    DBG_PRINT("*** WindowsWindow: WM_PAINT.0 (dirty)\n");
                    ValidateRect(wnd, NULL); // clear all!
                #endif
                } else {
                    DBG_PRINT("*** WindowsWindow: WM_PAINT.0 (clean)\n");
                }
            } else {
                if (GetUpdateRect(wnd, NULL, FALSE /* do not erase background */)) {
                    DBG_PRINT("*** WindowsWindow: WM_PAINT.1 (dirty)\n");
                    // Let NEWT render the whole client area by issueing repaint for it, w/o looping through erase background
                    ValidateRect(wnd, NULL); // clear all!
                    (*env)->CallVoidMethod(env, window, windowRepaintID, JNI_FALSE, 0, 0, -1, -1);
                } else {
                    DBG_PRINT("*** WindowsWindow: WM_PAINT.1 (clean)\n");
                    // shall not happen ?
                    ValidateRect(wnd, NULL); // clear all!
                }
                // return 0 == done
            }
            break;
        }
        case WM_ERASEBKGND:
            if( wud->isInCreation ) {
                #ifdef DO_ERASEBKGND
                    // On Windows the initial window is clean?!
                    // This fill destroys translucency on Windows 10
                    // (which only seem to work on undecorated windows)
                    PAINTSTRUCT ps;
                    HDC hdc;
                    hdc = BeginPaint(wnd, &ps);
                    DBG_PRINT("*** WindowsWindow: WM_ERASEBKGND.0 (erasure) l/b %d/%d r/t %d/%d\n", 
                        ps.rcPaint.left, ps.rcPaint.bottom, ps.rcPaint.right, ps.rcPaint.top);
                    // FillRect(hdc, &ps.rcPaint, (HBRUSH)(COLOR_WINDOW+1));
                    // FillRect(hdc, &ps.rcPaint, (HBRUSH)(COLOR_APPWORKSPACE+1));
                    // A black color also sets alpha to zero for translucency!
                    FillRect(hdc, &ps.rcPaint, (HBRUSH)GetStockObject(BLACK_PEN));
                    EndPaint(wnd, &ps); 
                #else
                    ValidateRect(wnd, NULL); // clear all!
                #endif
                res = 1; // return 1 == done
            } else {
                // ignore erase background, but let NEWT render the whole client area
                DBG_PRINT("*** WindowsWindow: WM_ERASEBKGND.1 (repaint)\n");
                ValidateRect(wnd, NULL); // clear all!
                (*env)->CallVoidMethod(env, window, windowRepaintID, JNI_FALSE, 0, 0, -1, -1);
                res = 1; // return 1 == done, OpenGL, etc .. erases the background, hence we claim to have just done this
            }
            break;

        case WM_SETCURSOR :
            if (0 != wud->setPointerVisible) { // Tristate, -1, 0, 1
                BOOL visibilityChangeSuccessful;
                if (1 == wud->setPointerVisible) {
                    visibilityChangeSuccessful = SafeShowCursor(TRUE);
                } else /* -1 == wud->setPointerVisible */ {
                    visibilityChangeSuccessful = SafeShowCursor(FALSE);
                }
                DBG_PRINT("*** WindowsWindow: WM_SETCURSOR requested visibility: %d success: %d\n", wud->setPointerVisible, visibilityChangeSuccessful);
                wud->setPointerVisible = 0;
                // own signal, consumed, no further processing
                res = 1;
            } else if( 0 != wud->setPointerAction ) {
                if( -1 == wud->setPointerAction ) {
                    wud->setPointerHandle = wud->defPointerHandle;
                }
                HCURSOR preHandle = SetCursor(wud->setPointerHandle);
                DBG_PRINT("*** WindowsWindow: WM_SETCURSOR PointerIcon change %d: pre %p -> set %p, def %p\n", 
                    wud->setPointerAction, (void*)preHandle, (void*)wud->setPointerHandle, (void*)wud->defPointerHandle);
                wud->setPointerAction = 0;
                // own signal, consumed, no further processing
                res = 1;
            } else if( HTCLIENT == LOWORD(lParam) ) {
                BOOL setCur = wud->isChildWindow && wud->defPointerHandle != wud->setPointerHandle;
                #ifdef VERBOSE_ON
                    HCURSOR cur = GetCursor();
                    DBG_PRINT("*** WindowsWindow: WM_SETCURSOR PointerIcon NOP [1 custom-override] set %p, def %p, cur %p, isChild %d, setCur %d\n",
                        (void*)wud->setPointerHandle, (void*)wud->defPointerHandle, (void*)cur, wud->isChildWindow, setCur);
                #endif
                if( setCur ) {
                    SetCursor(wud->setPointerHandle);
                    // own signal, consumed, no further processing
                    res = 1;
                } else {
                    DBG_PRINT("*** WindowsWindow: WM_SETCURSOR PointerIcon NOP [2 parent-override] set %p, def %p\n", (void*)wud->setPointerHandle, (void*)wud->defPointerHandle);
                    // NOP for us, allow parent to act
                    res = 0;
                }
            } else {
                DBG_PRINT("*** WindowsWindow: WM_SETCURSOR !HTCLIENT\n");
                // NOP for us, allow parent to act
                res = 0;
            }
            break;

        case WM_SETFOCUS:
            DBG_PRINT("*** WindowsWindow: WM_SETFOCUS window %p, lost %p, at-init %d\n", wnd, (HWND)wParam, wud->isInCreation);
            wud->focused = TRUE;
            if( !wud->isInCreation ) {
                (*env)->CallVoidMethod(env, window, focusChangedID, JNI_FALSE, JNI_TRUE);
            }
            useDefWindowProc = 1;
            break;

        case WM_KILLFOCUS:
            DBG_PRINT("*** WindowsWindow: WM_KILLFOCUS window %p, received %p, inside %d, captured %d, tDown %d\n",
                wnd, (HWND)wParam, wud->pointerInside, wud->pointerCaptured, wud->touchDownCount);
            if( wud->touchDownCount == 0 ) {
                wud->pointerInside = 0;
                if( wud->pointerCaptured ) {
                    wud->pointerCaptured = 0;
                    ReleaseCapture();
                }
                wud->focused = FALSE;
                if( !wud->isInCreation ) {
                    (*env)->CallVoidMethod(env, window, focusChangedID, JNI_FALSE, JNI_FALSE);
                }
                useDefWindowProc = 1;
            } else {
                // quick focus .. we had it already, are enabled ..
                SetFocus(wnd);// Sets Keyboard Focus To Window (activates parent window if exist, or this window)
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

        case WM_LBUTTONDOWN: {
                DBG_PRINT("*** WindowsWindow: WM_LBUTTONDOWN %d/%d [%dx%d] inside %d, captured %d, tDown [c %d, lastUp %d]\n",
                    (jint) GET_X_LPARAM(lParam), (jint) GET_Y_LPARAM(lParam),
                    wud->width, wud->height, wud->pointerInside, wud->pointerCaptured, wud->touchDownCount, wud->touchDownLastUp);
                if( 0 == wud->touchDownLastUp && 0 == wud->touchDownCount ) {
                    if( 0 == wud->pointerInside ) {
                        wud->pointerInside = 1;
                        NewtWindows_trackPointerLeave(wnd);
                    }
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
                DBG_PRINT("*** WindowsWindow: WM_LBUTTONUP %d/%d [%dx%d] inside %d, captured %d, tDown [c %d, lastUp %d]\n",
                    (jint) GET_X_LPARAM(lParam), (jint) GET_Y_LPARAM(lParam),
                    wud->width, wud->height, wud->pointerInside, wud->pointerCaptured, wud->touchDownCount, wud->touchDownLastUp);
                if( 0 < wud->touchDownLastUp ) {
                    // mitigate LBUTTONUP after last TOUCH lift
                    wud->touchDownLastUp = 0;
                } else if( 0 == wud->touchDownCount ) {
                    jint modifiers = GetModifiers(0);
                    if( wud->pointerCaptured && 0 == ( modifiers & EVENT_BUTTONALL_MASK ) ) {
                        wud->pointerCaptured = 0;
                        ReleaseCapture();
                    }
                    if( 0 == wud->pointerInside ) {
                        wud->pointerInside = 1;
                        NewtWindows_trackPointerLeave(wnd);
                    }
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
                DBG_PRINT("*** WindowsWindow: WM_MBUTTONDOWN %d/%d [%dx%d] inside %d, captured %d, tDown [c %d, lastUp %d]\n",
                    (jint) GET_X_LPARAM(lParam), (jint) GET_Y_LPARAM(lParam),
                    wud->width, wud->height, wud->pointerInside, wud->pointerCaptured, wud->touchDownCount, wud->touchDownLastUp);
                if( 0 == wud->touchDownCount ) {
                    if( 0 == wud->pointerInside ) {
                        wud->pointerInside = 1;
                        NewtWindows_trackPointerLeave(wnd);
                    }
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
                DBG_PRINT("*** WindowsWindow: WM_MBUTTONUP %d/%d [%dx%d] inside %d, captured %d, tDown [c %d, lastUp %d]\n",
                    (jint) GET_X_LPARAM(lParam), (jint) GET_Y_LPARAM(lParam),
                    wud->width, wud->height, wud->pointerInside, wud->pointerCaptured, wud->touchDownCount, wud->touchDownLastUp);
                if( 0 == wud->touchDownCount ) {
                    jint modifiers = GetModifiers(0);
                    if( wud->pointerCaptured && 0 == ( modifiers & EVENT_BUTTONALL_MASK ) ) {
                        wud->pointerCaptured = 0;
                        ReleaseCapture();
                    }
                    if( 0 == wud->pointerInside ) {
                        wud->pointerInside = 1;
                        NewtWindows_trackPointerLeave(wnd);
                    }
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
                DBG_PRINT("*** WindowsWindow: WM_RBUTTONDOWN %d/%d [%dx%d] inside %d, captured %d, tDown [c %d, lastUp %d]\n",
                    (jint) GET_X_LPARAM(lParam), (jint) GET_Y_LPARAM(lParam),
                    wud->width, wud->height, wud->pointerInside, wud->pointerCaptured, wud->touchDownCount, wud->touchDownLastUp);
                if( 0 == wud->touchDownCount ) {
                    if( 0 == wud->pointerInside ) {
                        wud->pointerInside = 1;
                        NewtWindows_trackPointerLeave(wnd);
                    }
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
                DBG_PRINT("*** WindowsWindow: WM_RBUTTONUP %d/%d [%dx%d] inside %d, captured %d, tDown [c %d, lastUp %d]\n",
                    (jint) GET_X_LPARAM(lParam), (jint) GET_Y_LPARAM(lParam),
                    wud->width, wud->height, wud->pointerInside, wud->pointerCaptured, wud->touchDownCount, wud->touchDownLastUp);
                if( 0 == wud->touchDownCount ) {
                    jint modifiers = GetModifiers(0);
                    if( wud->pointerCaptured && 0 == ( modifiers & EVENT_BUTTONALL_MASK ) ) {
                        wud->pointerCaptured = 0;
                        ReleaseCapture();
                    }
                    if( 0 == wud->pointerInside ) {
                        wud->pointerInside = 1;
                        NewtWindows_trackPointerLeave(wnd);
                    }
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
                DBG_PRINT("*** WindowsWindow: WM_MOUSEMOVE %d/%d [%dx%d] inside %d, captured %d, tDown [c %d, lastUp %d]\n",
                    (jint) GET_X_LPARAM(lParam), (jint) GET_Y_LPARAM(lParam),
                    wud->width, wud->height, wud->pointerInside, wud->pointerCaptured, wud->touchDownCount, wud->touchDownLastUp);
                if( 0 == wud->touchDownLastUp && 0 == wud->touchDownCount ) {
                    jint modifiers = GetModifiers(0);
                    if( 0 == wud->pointerCaptured && 0 != ( modifiers & EVENT_BUTTONALL_MASK ) ) {
                        wud->pointerCaptured = 1;
                        SetCapture(wnd);
                    }
                    if( 0 == wud->pointerInside ) {
                        wud->pointerInside = 1;
                        NewtWindows_trackPointerLeave(wnd);
                        SetCursor(wud->setPointerHandle);
                    }
                    (*env)->CallVoidMethod(env, window, sendMouseEventID,
                                           (jshort) EVENT_MOUSE_MOVED,
                                           modifiers,
                                           (jint) GET_X_LPARAM(lParam), (jint) GET_Y_LPARAM(lParam),
                                           (jshort) 0,  (jfloat) 0.0f);
                }
                useDefWindowProc = 1;
            }
            break;
        case WM_MOUSELEAVE: {
                DBG_PRINT("*** WindowsWindow: WM_MOUSELEAVE %d/%d [%dx%d] inside %d, captured %d, tDown [c %d, lastUp %d]\n",
                    (jint) GET_X_LPARAM(lParam), (jint) GET_Y_LPARAM(lParam),
                    wud->width, wud->height, wud->pointerInside, wud->pointerCaptured, wud->touchDownCount, wud->touchDownLastUp);
                if( 0 == wud->touchDownCount ) {
                    wud->pointerInside = 0;
                    (*env)->CallVoidMethod(env, window, sendMouseEventID,
                                           (jshort) EVENT_MOUSE_EXITED,
                                           0,
                                           (jint) -1, (jint) -1, // fake
                                           (jshort) 0,  (jfloat) 0.0f);
                    useDefWindowProc = 1;
                }
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
                if ( WinTouch_GetTouchInputInfo(hTouch, cInputs, pInputs, sizeof(TOUCHINPUT)) ) {
                    UINT i;
                    short eventType[cInputs];
                    jint modifiers = GetModifiers( 0 );
                    jint actionIdx = -1;
                    jint pointerNames[cInputs];
                    jint x[cInputs], y[cInputs];
                    jfloat pressure[cInputs];
                    jfloat maxPressure = 1.0F; // FIXME: n/a on windows ?
                    int allPInside = 0 < cInputs;
                    int sendFocus = 0;

                    for (i=0; i < cInputs; i++) {
                        PTOUCHINPUT pTi = & pInputs[i];
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

                        int pInside = 0 <= eventPt.x && 0 <= eventPt.y && eventPt.x < wud->width && eventPt.y < wud->height;
                        allPInside &= pInside;

                        x[i] = (jint)eventPt.x;
                        y[i] = (jint)eventPt.y;
                        pressure[i] = 1.0F; // FIXME: n/a on windows ?
                        if(isDown) {
                            sendFocus = 0 == wud->touchDownCount;
                            eventType[i] = (jshort) EVENT_MOUSE_PRESSED;
                            wud->touchDownCount++;
                            wud->touchDownLastUp = 0;
                        } else if(isUp) {
                            eventType[i] = (jshort) EVENT_MOUSE_RELEASED;
                            wud->touchDownCount--;
                            // mitigate LBUTTONUP after last TOUCH lift
                            wud->touchDownLastUp = 0 == wud->touchDownCount;
                        } else if(isMove) {
                            eventType[i] = (jshort) EVENT_MOUSE_MOVED;
                            wud->touchDownLastUp = 0;
                        } else {
                            eventType[i] = (jshort) 0;
                        }
                        if(isPrim) {
                            actionIdx = (jint)i;
                        }

                        #ifdef VERBOSE_ON
                        DBG_PRINT("*** WindowsWindow: WM_TOUCH[%d/%d].%s name 0x%x, prim %d, nocoalsc %d, %d/%d [%dx%d] inside [%d/%d], tDown [c %d, lastUp %d]\n", 
                            (i+1), cInputs, touchAction, (int)(pTi->dwID), isPrim, isNoCoalesc, x[i], y[i], wud->width, wud->height, 
                            pInside, allPInside, wud->touchDownCount, wud->touchDownLastUp);
                        #endif
                    }
                    wud->pointerInside = allPInside;
                    if( sendFocus ) {
                        (*env)->CallVoidMethod(env, window, requestFocusID, JNI_FALSE);
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
                    DBG_PRINT("*** WindowsWindow: WM_TOUCH.summary pCount %d, prim %d, updown %d, move %d, sent %d, inside %d, captured %d, tDown [c %d, lastUp %d]\n", 
                        cInputs, actionIdx, updownCount, moveCount, sentCount, wud->pointerInside, wud->pointerCaptured, wud->touchDownCount, wud->touchDownLastUp);

                    // Message processed - close it
                    WinTouch_CloseTouchInputHandle(hTouch);
                } else {
                    useDefWindowProc = 1;
                }
                free(pInputs);
            }
            break;
        }

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

static LPCTSTR NewtScreen_getAdapterName(DISPLAY_DEVICE * device, int adapter_idx) {
    if( 0 > adapter_idx ) {
        DBG_PRINT("*** WindowsWindow: getAdapterName(adapter_idx %d < 0)\n", adapter_idx);
        return NULL;
    }
    memset(device, 0, sizeof(DISPLAY_DEVICE)); 
    device->cb = sizeof(DISPLAY_DEVICE);
    if( FALSE == EnumDisplayDevices(NULL, adapter_idx, device, 0) ) {
        DBG_PRINT("*** WindowsWindow: getAdapterName.EnumDisplayDevices(adapter_idx %d) -> FALSE\n", adapter_idx);
        return NULL;
    }

    if( NULL == device->DeviceName || 0 == _tcslen(device->DeviceName) ) {
        return NULL;
    }

    return device->DeviceName;
}

static LPCTSTR NewtScreen_getMonitorName(LPCTSTR adapterName, DISPLAY_DEVICE * device, int monitor_idx, BOOL onlyActive) {
    if( 0 > monitor_idx ) {
        DBG_PRINT("*** WindowsWindow: getMonitorName(monitor_idx %d < 0)\n", monitor_idx);
        return NULL;
    }
    memset(device, 0, sizeof(DISPLAY_DEVICE)); 
    device->cb = sizeof(DISPLAY_DEVICE);
    if( FALSE == EnumDisplayDevices(adapterName, monitor_idx, device, EDD_GET_DEVICE_INTERFACE_NAME) ) {
        DBG_PRINT("*** WindowsWindow: getMonitorName.EnumDisplayDevices(monitor_idx %d).adapter -> FALSE\n", monitor_idx);
        return NULL;
    }
    if( onlyActive ) {
        if( 0 == ( device->StateFlags & DISPLAY_DEVICE_ACTIVE ) ) {
            DBG_PRINT("*** WindowsWindow: !DISPLAY_DEVICE_ACTIVE(monitor_idx %d).display\n", monitor_idx);
            return NULL;
        }
    }
    if( NULL == device->DeviceName || 0 == _tcslen(device->DeviceName) ) {
        return NULL;
    }

    return device->DeviceName;
}

static int NewtScreen_getFirstActiveNonCloneMonitor(LPCTSTR adapterName, DISPLAY_DEVICE * device) {
    memset(device, 0, sizeof(DISPLAY_DEVICE)); 
    device->cb = sizeof(DISPLAY_DEVICE);
    int monitor_idx;
    for(monitor_idx=0; EnumDisplayDevices(adapterName, monitor_idx, device, EDD_GET_DEVICE_INTERFACE_NAME); monitor_idx++) {
        if( NULL != device->DeviceName && 
            0 < _tcslen(device->DeviceName) && 
            0 != ( device->StateFlags & DISPLAY_DEVICE_ACTIVE ) &&
            0 == ( device->StateFlags & DISPLAY_DEVICE_MIRRORING_DRIVER ) ) {
            return monitor_idx; 
        }
        memset(device, 0, sizeof(DISPLAY_DEVICE)); 
        device->cb = sizeof(DISPLAY_DEVICE);
    }
    return -1;
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_windows_ScreenDriver_dumpMonitorInfo0
  (JNIEnv *env, jclass clazz)
{
    DISPLAY_DEVICE aDevice, dDevice;
    int i = 0, j;
    LPCTSTR aName, dName;
    while(NULL != (aName = NewtScreen_getAdapterName(&aDevice, i))) {
        STD_PRINT("*** [%02d:__]: deviceName <%s>, flags 0x%X, active %d, primary %d\n", 
            i, aDevice.DeviceName, aDevice.StateFlags, 
            ( 0 != ( aDevice.StateFlags & DISPLAY_DEVICE_ACTIVE ) ),
            ( 0 != ( aDevice.StateFlags & DISPLAY_DEVICE_PRIMARY_DEVICE ) ));
        STD_PRINT("           deviceString <%s> \n", aDevice.DeviceString);
        STD_PRINT("           deviceID     <%s> \n", aDevice.DeviceID);
        j=0;
        while(NULL != (dName = NewtScreen_getMonitorName(aName, &dDevice, j, FALSE))) {
            STD_PRINT("*** [%02d:%02d]: deviceName <%s> flags 0x%X active %d, mirror %d\n", 
                i, j, dDevice.DeviceName, dDevice.StateFlags, 
                0 != ( dDevice.StateFlags & DISPLAY_DEVICE_ACTIVE ),
                0 != ( dDevice.StateFlags & DISPLAY_DEVICE_MIRRORING_DRIVER ) );
            STD_PRINT("           deviceString <%s> \n", dDevice.DeviceString);
            STD_PRINT("           deviceID     <%s> \n", dDevice.DeviceID);
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
  (JNIEnv *env, jobject obj, jint adapter_idx)
{
    DISPLAY_DEVICE device;
    LPCTSTR adapterName = NewtScreen_getAdapterName(&device, adapter_idx);
    DBG_PRINT("*** WindowsWindow: getAdapterName(adapter_idx %d) -> %s, active %d\n", adapter_idx, 
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
 * Method:    getMonitorName0
 * Signature: (Ljava/lang/String;IZ)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_jogamp_newt_driver_windows_ScreenDriver_getMonitorName0
  (JNIEnv *env, jobject obj, jstring jAdapterName, jint monitor_idx, jboolean onlyActive)
{
    DISPLAY_DEVICE device;
    LPCTSTR monitorName;
#ifdef UNICODE
    LPCTSTR adapterName = NewtCommon_GetNullTerminatedStringChars(env, jAdapterName);
    monitorName = NewtScreen_getMonitorName(adapterName, &device, monitor_idx, onlyActive);
    DBG_PRINT("*** WindowsWindow: getMonitorName(%s, monitor_idx %d) -> %s\n", adapterName, monitor_idx, (NULL==monitorName?"nil":monitorName));
    free((void*) adapterName);
#else
    LPCTSTR adapterName = (*env)->GetStringUTFChars(env, jAdapterName, NULL);
    monitorName = NewtScreen_getMonitorName(adapterName, &device, monitor_idx, onlyActive);
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
 * Signature: (Ljava/lang/String;II)[I
 */
JNIEXPORT jintArray JNICALL Java_jogamp_newt_driver_windows_ScreenDriver_getMonitorDevice0
  (JNIEnv *env, jobject obj, jint adapter_idx, jint monitor_idx, jint monitor_id)
{
    int gotsize = 0;
    int widthmm, heightmm;
    DISPLAY_DEVICE adapterDevice, monitorDevice;
    LPCTSTR monitorName = NULL;
    LPCTSTR adapterName = NULL;

    adapterName = NewtScreen_getAdapterName(&adapterDevice, adapter_idx);
    if( NULL == adapterName ) {
        DBG_PRINT("ERROR WindowsWindow: adapter[idx %d]: NULL\n", adapter_idx);
        return (*env)->NewIntArray(env, 0);
    }
    DBG_PRINT("*** WindowsWindow: adapter[name %s, idx %d], monitor[idx %d, id %d], EDID-avail %d\n", 
        adapterName, adapter_idx, monitor_idx, monitor_id, NewtEDID_avail);

    monitorName = NewtScreen_getMonitorName(adapterName, &monitorDevice, monitor_idx, TRUE);
    if( NULL == monitorName ) {
        DBG_PRINT("ERROR WindowsWindow: monitor[idx %d]: NULL\n", monitor_idx);
        return (*env)->NewIntArray(env, 0);
    }
    if( NewtEDID_avail ) {
        int widthcm, heightcm;
        if( NewtEDID_GetMonitorSizeFromEDIDByDevice(&monitorDevice, &widthmm, &heightmm, &widthcm, &heightcm) ) {
            DBG_PRINT("*** WindowsWindow: EDID %d x %d [mm], %d x %d [cm]\n", widthmm, heightmm, widthcm, heightcm);
            if( 0 <= widthmm && 0 <= heightmm ) {
                gotsize = 1; // got mm values
                DBG_PRINT("*** WindowsWindow: %d x %d [mm] (EDID mm)\n", widthmm, heightmm);
            } else if( 0 <= widthcm && 0 <= heightcm ) {
                // fallback using cm values
                widthmm = widthcm * 10;
                heightmm = heightcm * 10;
                gotsize = 1;
                DBG_PRINT("*** WindowsWindow: %d x %d [mm] (EDID cm)\n", widthmm, heightmm);
            }
        }
    }
    if( !gotsize ) {
        // fallback using buggy API resulting in erroneous values
        HDC hdc = NewtScreen_createDisplayDC(adapterName);
        widthmm = GetDeviceCaps(hdc, HORZSIZE);
        heightmm = GetDeviceCaps(hdc, VERTSIZE);
        DeleteDC(hdc);
        DBG_PRINT("*** WindowsWindow: %d x %d [mm] (Buggy API)\n", widthmm, heightmm);
    }
    int devModeID = ENUM_CURRENT_SETTINGS;

    DEVMODE dm;
    ZeroMemory(&dm, sizeof(dm));
    dm.dmSize = sizeof(dm);
    
    int res = EnumDisplaySettingsEx(adapterName, devModeID, &dm, 0);
    DBG_PRINT("*** WindowsWindow: getMonitorDevice.EnumDisplaySettingsEx(%s, devModeID %d) -> %d\n", adapterName, devModeID, res);
    if (0 == res) {
        return (*env)->NewIntArray(env, 0);
    }
    
    jsize propCount = MIN_MONITOR_DEVICE_PROPERTIES - 1 - NUM_MONITOR_MODE_PROPERTIES;
    jint prop[ propCount ];
    int propIndex = 0;

    prop[propIndex++] = propCount;
    prop[propIndex++] = monitor_id;
    prop[propIndex++] = 0 != ( monitorDevice.StateFlags & DISPLAY_DEVICE_MIRRORING_DRIVER ); // isClone
    prop[propIndex++] = 0 != ( adapterDevice.StateFlags & DISPLAY_DEVICE_PRIMARY_DEVICE );   // isPrimary
    prop[propIndex++] = widthmm;
    prop[propIndex++] = heightmm;
    prop[propIndex++] = dm.dmPosition.x; // rotated viewport pixel units
    prop[propIndex++] = dm.dmPosition.y; // rotated viewport pixel units
    prop[propIndex++] = dm.dmPelsWidth;  // rotated viewport pixel units
    prop[propIndex++] = dm.dmPelsHeight; // rotated viewport pixel units
    prop[propIndex++] = dm.dmPosition.x; // rotated viewport window units (same)
    prop[propIndex++] = dm.dmPosition.y; // rotated viewport window units (same)
    prop[propIndex++] = dm.dmPelsWidth;  // rotated viewport window units (same)
    prop[propIndex++] = dm.dmPelsHeight; // rotated viewport window units (same)

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
  (JNIEnv *env, jobject object, jint adapter_idx, jint x, jint y, jint width, jint height, jint bits, jint rate, jint flags, jint rot)
{
    DISPLAY_DEVICE adapterDevice, monitorDevice;
    LPCTSTR adapterName = NewtScreen_getAdapterName(&adapterDevice, adapter_idx);
    if(NULL == adapterName) {
        DBG_PRINT("*** WindowsWindow: setMonitorMode.getAdapterName(adapter_idx %d) -> NULL\n", adapter_idx);
        return JNI_FALSE;
    }

    {
        // Just test whether there is an active non-mirror monitor attached
        int monitor_idx = NewtScreen_getFirstActiveNonCloneMonitor(adapterName, &monitorDevice);
        if( 0 > monitor_idx ) {
            DBG_PRINT("*** WindowsWindow: setMonitorMode.getFirstActiveNonCloneMonitor(%s) -> n/a\n", adapterName);
            return JNI_FALSE;
        }
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
    maximizedChangedID = (*env)->GetMethodID(env, clazz, "maximizedChanged", "(ZZ)V");
    positionChangedID = (*env)->GetMethodID(env, clazz, "positionChanged", "(ZII)V");
    focusChangedID = (*env)->GetMethodID(env, clazz, "focusChanged", "(ZZ)V");
    visibleChangedID = (*env)->GetMethodID(env, clazz, "visibleChanged", "(ZZ)V");
    sizePosInsetsFocusVisibleChangedID = (*env)->GetMethodID(env, clazz, "sizePosInsetsFocusVisibleChanged", "(ZIIIIIIIIIIZ)V");
    windowDestroyNotifyID = (*env)->GetMethodID(env, clazz, "windowDestroyNotify", "(Z)Z");
    windowRepaintID = (*env)->GetMethodID(env, clazz, "windowRepaint", "(ZIIII)V");
    sendMouseEventID = (*env)->GetMethodID(env, clazz, "sendMouseEvent", "(SIIISF)V");
    sendTouchScreenEventID = (*env)->GetMethodID(env, clazz, "sendTouchScreenEvent", "(SII[I[I[I[FF)V");
    sendKeyEventID = (*env)->GetMethodID(env, clazz, "sendKeyEvent", "(SISSC)V");
    requestFocusID = (*env)->GetMethodID(env, clazz, "requestFocus", "(Z)V");

    if (insetsChangedID == NULL ||
        sizeChangedID == NULL ||
        maximizedChangedID == NULL ||
        positionChangedID == NULL ||
        focusChangedID == NULL ||
        visibleChangedID == NULL ||
        sizePosInsetsFocusVisibleChangedID == NULL ||
        windowDestroyNotifyID == NULL ||
        windowRepaintID == NULL ||
        sendMouseEventID == NULL ||
        sendTouchScreenEventID == NULL ||
        sendKeyEventID == NULL ||
        requestFocusID == NULL) {
        return JNI_FALSE;
    }
    InitKeyMapTableScanCode(env);

    {
        HANDLE shell = LoadLibrary(TEXT("user32.dll"));
        if (shell) {
            WinTouch_CloseTouchInputHandle = (CloseTouchInputHandlePROCADDR) GetProcAddressA(shell, "CloseTouchInputHandle");
            WinTouch_GetTouchInputInfo = (GetTouchInputInfoPROCADDR) GetProcAddressA(shell, "GetTouchInputInfo");
            WinTouch_IsTouchWindow = (IsTouchWindowPROCADDR) GetProcAddressA(shell, "IsTouchWindow");
            WinTouch_RegisterTouchWindow = (RegisterTouchWindowPROCADDR) GetProcAddressA(shell, "RegisterTouchWindow");
            WinTouch_UnregisterTouchWindow = (UnregisterTouchWindowPROCADDR) GetProcAddressA(shell, "UnregisterTouchWindow");
            if(NULL != WinTouch_CloseTouchInputHandle && NULL != WinTouch_GetTouchInputInfo && 
               NULL != WinTouch_IsTouchWindow && NULL != WinTouch_RegisterTouchWindow && NULL != WinTouch_UnregisterTouchWindow) {
                WinTouch_func_avail = 1;
            } else {
                WinTouch_func_avail = 0;
            }
        }
        NewtEDID_avail = NewtEDID_init();
    }
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

static void NewtWindow_setVisiblePosSize(WindowUserData *wud, HWND hwnd, int jflags, BOOL visible, 
                                         int x, int y, int width, int height)
{
    BOOL atop = TST_FLAG_IS_ALWAYSONTOP(jflags);
    BOOL abottom = TST_FLAG_IS_ALWAYSONBOTTOM(jflags);
    UINT wflags;

    DBG_PRINT("*** WindowsWindow: NewtWindow_setVisiblePosSize client %d/%d %dx%d, atop %d, abottom %d, max[change[%d %d], is[%d %d]], visible %d\n", 
        x, y, width, height, atop, abottom, 
        TST_FLAG_CHANGE_MAXIMIZED_VERT(jflags), TST_FLAG_CHANGE_MAXIMIZED_HORZ(jflags),
        TST_FLAG_IS_MAXIMIZED_VERT(jflags), TST_FLAG_IS_MAXIMIZED_HORZ(jflags),
        visible);

    x -= wud->insets.left; // top-level
    y -= wud->insets.top;  // top-level
    width += wud->insets.left + wud->insets.right;   // top-level
    height += wud->insets.top + wud->insets.bottom;  // top-level
    DBG_PRINT("*** WindowsWindow: NewtWindow_setVisiblePosSize top-level %d/%d %dx%d\n", x, y, width, height);

    if(visible) {
        wflags = SWP_SHOWWINDOW;
        if( abottom ) {
            wflags |= SWP_NOACTIVATE;
        }
    } else {
        wflags = SWP_NOACTIVATE | SWP_NOZORDER;
    }
    if(0>=width || 0>=height ) {
        wflags |= SWP_NOSIZE;
    }

    wud->isOnTop = atop;
    wud->isOnBottom = abottom;
    if(atop) {
        SetWindowPos(hwnd, HWND_TOP, x, y, width, height, wflags);
        SetWindowPos(hwnd, HWND_TOPMOST, x, y, width, height, wflags);
    } else if(abottom) {
        SetWindowPos(hwnd, HWND_NOTOPMOST, x, y, width, height, wflags);
        SetWindowPos(hwnd, HWND_BOTTOM, x, y, width, height, wflags);
    } else {
        SetWindowPos(hwnd, HWND_NOTOPMOST, x, y, width, height, wflags);
        SetWindowPos(hwnd, HWND_TOP, x, y, width, height, wflags);
    }

    if( TST_FLAG_CHANGE_MAXIMIZED_ANY(jflags) ) {
        if( TST_FLAG_IS_MAXIMIZED_VERT(jflags) && TST_FLAG_IS_MAXIMIZED_HORZ(jflags) ) {
            wud->isMaximized = 1;
            ShowWindow(hwnd, SW_MAXIMIZE);
        } else if( !TST_FLAG_IS_MAXIMIZED_VERT(jflags) && !TST_FLAG_IS_MAXIMIZED_HORZ(jflags) ) {
            if( wud->isMaximized ) {
                ShowWindow(hwnd, SW_RESTORE);
                wud->isMaximized = 0;
            }
        }
    }

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
   jlong parent, jint jxpos, jint jypos, jint defaultWidth, jint defaultHeight, jint flags)
{
    HWND parentWindow = (HWND) (intptr_t) parent;
    const TCHAR* wndClassName = NULL;
    const TCHAR* wndName = NULL;
    DWORD windowStyle = WS_DEFAULT_STYLES;
    int xpos=(int)jxpos, ypos=(int)jypos;
    int width=(int)defaultWidth, height=(int)defaultHeight;
    HWND hwnd = NULL;

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
    } else {
        if ( TST_FLAG_IS_UNDECORATED(flags) ) {
            windowStyle |= WS_POPUP | WS_SYSMENU | WS_MAXIMIZEBOX | WS_MINIMIZEBOX;
        } else if ( TST_FLAG_IS_RESIZABLE(flags) ) {
            // WS_OVERLAPPEDWINDOW = (WS_OVERLAPPED | WS_CAPTION | WS_SYSMENU | WS_THICKFRAME | WS_MINIMIZEBOX | WS_MAXIMIZEBOX);
            windowStyle |= WS_OVERLAPPEDWINDOW;
        } else {
            windowStyle |= WS_OVERLAPPED | WS_CAPTION | WS_SYSMENU | WS_MINIMIZEBOX;
        }
        if( TST_FLAG_IS_AUTOPOSITION(flags) ) {
            // user didn't requested specific position, use WM default
            xpos = CW_USEDEFAULT;
            ypos = 0;
        }
    }

    hwnd = CreateWindow(wndClassName, wndName, windowStyle,
                        xpos, ypos, width, height,
                        parentWindow, NULL, (HINSTANCE) (intptr_t) hInstance, NULL);

    DBG_PRINT("*** WindowsWindow: CreateWindow thread 0x%X, win %d.%d parent %p, window %p, %d/%d -> %d/%d %dx%d, undeco %d, alwaysOnTop %d, autoPosition %d\n", 
        (int)GetCurrentThreadId(), winMajor, winMinor, parentWindow, hwnd, jxpos, jypos, xpos, ypos, width, height,
        TST_FLAG_IS_UNDECORATED(flags), TST_FLAG_IS_ALWAYSONTOP(flags), TST_FLAG_IS_AUTOPOSITION(flags));

    if (NULL == hwnd) {
        int lastError = (int) GetLastError();
        DBG_PRINT("*** WindowsWindow: CreateWindow failure: 0x%X %d\n", lastError, lastError);
    } else {
        WindowUserData * wud = (WindowUserData *) malloc(sizeof(WindowUserData));
        wud->jinstance = (*env)->NewGlobalRef(env, obj);
        wud->jenv = env;
        wud->xpos = xpos;
        wud->ypos = ypos;
        wud->width = width;
        wud->height = height;
        wud->visible = TRUE;
        wud->focused = TRUE;
        wud->setPointerVisible = 0;
        wud->setPointerAction = 0;
        wud->defPointerHandle = LoadCursor( NULL, IDC_ARROW);
        wud->setPointerHandle = wud->defPointerHandle;
        wud->isFullscreen = FALSE;
        wud->isChildWindow = NULL!=parentWindow;
        wud->isMinimized = FALSE;
        wud->isMaximized = FALSE;
        wud->isOnBottom = FALSE;
        wud->isOnTop = FALSE;
        wud->isInCreation = TRUE;
        wud->pointerCaptured = 0;
        wud->pointerInside = 0;
        wud->touchDownCount = 0;
        wud->touchDownLastUp = 0;
        wud->supportsMTouch = 0;
        if ( WinTouch_func_avail && winMajor > 6 || ( winMajor == 6 && winMinor >= 1 ) ) {
            int value = GetSystemMetrics(SM_DIGITIZER);
            if (value & NID_READY) { /* ready */
                if (value  & NID_MULTI_INPUT) { /* multitouch */
                    wud->supportsMTouch = 1;
                }
                if (value & NID_INTEGRATED_TOUCH) { /* Integrated touch */
                }
            }
        }
        DBG_PRINT("*** WindowsWindow: CreateWindow winTouchFuncAvail %d, supportsMTouch %d\n", WinTouch_func_avail, wud->supportsMTouch);

#if !defined(__MINGW64__) && ( defined(UNDER_CE) || _MSC_VER <= 1200 )
        SetWindowLong(hwnd, GWL_USERDATA, (intptr_t) wud);
#else
        SetWindowLongPtr(hwnd, GWLP_USERDATA, (intptr_t) wud);
#endif

        // send insets before visibility, allowing java code a proper sync point!
        UpdateInsets(env, wud, hwnd);

        if( TST_FLAG_IS_AUTOPOSITION(flags) ) {
            RECT rc;
            GetWindowRect(hwnd, &rc);
            xpos = rc.left + wud->insets.left; // client coords
            ypos = rc.top + wud->insets.top;   // client coords
            wud->xpos = xpos;
            wud->ypos = ypos;
        }
        DBG_PRINT("*** WindowsWindow: CreateWindow client: %d/%d %dx%d -> %d/%d %dx%d (autoPosition %d)\n", 
            xpos, ypos, width, height, 
            wud->xpos, wud->ypos, wud->width, wud->height,
            TST_FLAG_IS_AUTOPOSITION(flags));
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

    DBG_PRINT("*** WindowsWindow: CreateWindow done\n");
    return (jlong) (intptr_t) hwnd;
}

/*
 * Class:     jogamp_newt_driver_windows_WindowDriver
 * Method:    InitWindow
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_windows_WindowDriver_InitWindow0
  (JNIEnv *env, jobject obj, jlong window, jint flags) 
{
    HWND hwnd = (HWND) (intptr_t) window;
    WindowUserData * wud;
#if !defined(__MINGW64__) && ( defined(UNDER_CE) || _MSC_VER <= 1200 )
    wud = (WindowUserData *) GetWindowLong(hwnd, GWL_USERDATA);
#else
    wud = (WindowUserData *) GetWindowLongPtr(hwnd, GWLP_USERDATA);
#endif

    DBG_PRINT("*** WindowsWindow: InitWindow start %d/%d %dx%d, focused %d, visible %d\n", 
        wud->xpos, wud->ypos, wud->width, wud->height, wud->focused, wud->visible);

    NewtWindow_setVisiblePosSize(wud, hwnd, flags, TRUE, wud->xpos, wud->ypos, wud->width, wud->height);
    wud->isInCreation = FALSE;

    DBG_PRINT("*** WindowsWindow: InitWindow pos/size set: %d/%d %dx%d, focused %d, visible %d\n", 
        wud->xpos, wud->ypos, wud->width, wud->height, wud->focused, wud->visible);

    if( wud->isMaximized ) {
        (*env)->CallVoidMethod(env, wud->jinstance, maximizedChangedID, JNI_TRUE, JNI_TRUE);
    }
    (*env)->CallVoidMethod(env, wud->jinstance, sizePosInsetsFocusVisibleChangedID, JNI_FALSE,
                           (jint)wud->xpos, (jint)wud->ypos,
                           (jint)wud->width, (jint)wud->height,
                           (jint)wud->insets.left, (jint)wud->insets.right, (jint)wud->insets.top, (jint)wud->insets.bottom,
                           (jint)(wud->focused ? 1 : 0),
                           (jint)(wud->visible ? 1 : 0),
                           JNI_FALSE);
    DBG_PRINT("*** WindowsWindow: InitWindow JNI callbacks done\n");

    if( wud->supportsMTouch ) {
        WinTouch_RegisterTouchWindow(hwnd, 0);
    }
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
    BOOL styleChange = TST_FLAG_CHANGE_DECORATION(flags) || TST_FLAG_CHANGE_FULLSCREEN(flags) || 
                       TST_FLAG_CHANGE_PARENTING(flags) || TST_FLAG_CHANGE_RESIZABLE(flags);
    BOOL atop = TST_FLAG_IS_ALWAYSONTOP(flags);
    BOOL abottom = TST_FLAG_IS_ALWAYSONBOTTOM(flags);
    WindowUserData * wud;
#if !defined(__MINGW64__) && ( defined(UNDER_CE) || _MSC_VER <= 1200 )
    wud = (WindowUserData *) GetWindowLong(hwnd, GWL_USERDATA);
#else
    wud = (WindowUserData *) GetWindowLongPtr(hwnd, GWLP_USERDATA);
#endif

    DBG_PRINT( "*** WindowsWindow: reconfigureWindow0 parent %p, window %p, %d/%d %dx%d, parentChange %d, isChild %d, undecoration[change %d, val %d], fullscreen[change %d, val %d], alwaysOnTop[change %d, val %d], alwaysOnBottom[change %d, val %d], visible[change %d, val %d], resizable[change %d, val %d] -> styleChange %d, isChild %d, isMinimized %d, isMaximized %d, isFullscreen %d\n",
        parent, window, x, y, width, height,
        TST_FLAG_CHANGE_PARENTING(flags),   TST_FLAG_IS_CHILD(flags),
        TST_FLAG_CHANGE_DECORATION(flags),  TST_FLAG_IS_UNDECORATED(flags),
        TST_FLAG_CHANGE_FULLSCREEN(flags),  TST_FLAG_IS_FULLSCREEN(flags),
        TST_FLAG_CHANGE_ALWAYSONTOP(flags), TST_FLAG_IS_ALWAYSONTOP(flags),
        TST_FLAG_CHANGE_ALWAYSONBOTTOM(flags), TST_FLAG_IS_ALWAYSONBOTTOM(flags),
        TST_FLAG_CHANGE_VISIBILITY(flags), TST_FLAG_IS_VISIBLE(flags), 
        TST_FLAG_CHANGE_RESIZABLE(flags), TST_FLAG_CHANGE_RESIZABLE(flags), styleChange, 
        wud->isChildWindow, wud->isMinimized, wud->isMaximized, wud->isFullscreen);

    if (!IsWindow(hwnd)) {
        DBG_PRINT("*** WindowsWindow: reconfigureWindow0 failure: Passed window %p is invalid\n", (void*)hwnd);
        return;
    }

    wud->isOnTop = atop;
    wud->isOnBottom = abottom;

    if (NULL!=hwndP && !IsWindow(hwndP)) {
        DBG_PRINT("*** WindowsWindow: reconfigureWindow0 failure: Passed parent window %p is invalid\n", (void*)hwndP);
        return;
    }

    wud->isChildWindow = NULL != hwndP;

    if(TST_FLAG_IS_VISIBLE(flags)) {
        windowStyle |= WS_VISIBLE ;
    }
    
    // order of call sequence: (MS documentation)
    //    TOP:  SetParent(.., NULL); Clear WS_CHILD [, Set WS_POPUP]
    //  CHILD:  Set WS_CHILD [, Clear WS_POPUP]; SetParent(.., PARENT) 
    //
    if( TST_FLAG_CHANGE_PARENTING(flags) && NULL == hwndP ) {
        // TOP: in -> out

        // HIDE to allow setting ICONs (Windows bug?) .. WS_VISIBLE (style) will reset visibility
        ShowWindow(hwnd, SW_HIDE);

        SetParent(hwnd, NULL);
    }
    
    if( TST_FLAG_IS_FULLSCREEN(flags) ) {
        if( TST_FLAG_CHANGE_FULLSCREEN(flags) ) { // FS on
            wud->isFullscreen = TRUE;
            if( !abottom ) {
                NewtWindows_setFullScreen(JNI_TRUE);
            }
        } else if( TST_FLAG_CHANGE_ALWAYSONBOTTOM(flags) ) { // FS BOTTOM toggle
            NewtWindows_setFullScreen( abottom ? JNI_FALSE : JNI_TRUE);
        }
    }

    if ( styleChange ) {
        if(NULL!=hwndP) {
            windowStyle |= WS_CHILD ;
        } else if ( TST_FLAG_IS_UNDECORATED(flags) ) {
            windowStyle |= WS_POPUP | WS_SYSMENU | WS_MAXIMIZEBOX | WS_MINIMIZEBOX;
        } else if ( TST_FLAG_IS_RESIZABLE(flags) ) {
            // WS_OVERLAPPEDWINDOW = (WS_OVERLAPPED | WS_CAPTION | WS_SYSMENU | WS_THICKFRAME | WS_MINIMIZEBOX | WS_MAXIMIZEBOX);
            windowStyle |= WS_OVERLAPPEDWINDOW;
        } else {
            windowStyle |= WS_OVERLAPPED | WS_CAPTION | WS_SYSMENU | WS_MINIMIZEBOX;
        }
        SetWindowLong(hwnd, GWL_STYLE, windowStyle);
        SetWindowPos(hwnd, 0, 0, 0, 0, 0, SWP_FRAMECHANGED | SWP_NOSIZE | SWP_NOMOVE | SWP_NOACTIVATE | SWP_NOZORDER );
    }

    if( TST_FLAG_CHANGE_FULLSCREEN(flags) && !TST_FLAG_IS_FULLSCREEN(flags) ) { // FS off
        wud->isFullscreen = FALSE;
        NewtWindows_setFullScreen(JNI_FALSE);
    }

    if ( TST_FLAG_CHANGE_PARENTING(flags) && NULL != hwndP ) {
        // CHILD: out -> in
        SetParent(hwnd, hwndP );
    }

    NewtWindow_setVisiblePosSize(wud, hwnd, flags, TST_FLAG_IS_VISIBLE(flags), x, y, width, height);

    if( TST_FLAG_CHANGE_VISIBILITY(flags) ) {
        if( TST_FLAG_IS_VISIBLE(flags) ) {
            int cmd = wud->isMinimized ? SW_RESTORE : ( abottom ? SW_SHOWNA : SW_SHOW );
            wud->isMinimized = FALSE;
            ShowWindow(hwnd, cmd);
            if( abottom ) {
                SetWindowPos(hwnd, HWND_BOTTOM, 0, 0, 0, 0, SWP_NOSIZE | SWP_NOMOVE | SWP_NOACTIVATE);
            }
        } else if( !TST_FLAG_CHANGE_VISIBILITY_FAST(flags) && !TST_FLAG_IS_CHILD(flags) ) {
            wud->isMinimized = TRUE;
            ShowWindow(hwnd, SW_MINIMIZE);
        } else {
            ShowWindow(hwnd, SW_HIDE);
        }
    }

    DBG_PRINT("*** WindowsWindow: reconfigureWindow0.X isChild %d, isMinimized %d, isFullscreen %d\n", wud->isChildWindow, wud->isMinimized, wud->isFullscreen);
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
        RECT rect = { l, t, r, b };
        res = ClipCursor(&rect) ? JNI_TRUE : JNI_FALSE;
    } else {
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

JNIEXPORT jlong JNICALL
Java_jogamp_newt_driver_windows_DisplayDriver_createBGRA8888Icon0(JNIEnv *env, jobject _unused, 
    jobject pixels, jint pixels_byte_offset, jboolean pixels_is_direct, jint width, jint height, jboolean isCursor, jint hotX, jint hotY) {

    if( 0 == pixels ) {
        return 0;
    }

    const unsigned char * pixelPtr = (const unsigned char *) ( JNI_TRUE == pixels_is_direct ? 
                                            (*env)->GetDirectBufferAddress(env, pixels) : 
                                            (*env)->GetPrimitiveArrayCritical(env, pixels, NULL) );
    const int bytes = 4 * width * height; // BGRA8888

    DWORD dwWidth, dwHeight;
    BITMAPV5HEADER bi;
    HBITMAP hBitmap;
    void *lpBits;
    HICON handle = NULL;

    dwWidth  = width;  // width of cursor
    dwHeight = height;  // height of cursor

    ZeroMemory(&bi,sizeof(BITMAPV5HEADER));
    bi.bV5Size           = sizeof(BITMAPV5HEADER);
    bi.bV5Width           = dwWidth;
    bi.bV5Height          = -1 * dwHeight;
    bi.bV5Planes = 1;
    bi.bV5BitCount = 32;
    bi.bV5Compression = BI_BITFIELDS;
    // The following mask specification specifies a supported 32 BPP
    // alpha format for Windows XP.
    bi.bV5RedMask   =  0x00FF0000;
    bi.bV5GreenMask =  0x0000FF00;
    bi.bV5BlueMask  =  0x000000FF;
    bi.bV5AlphaMask =  0xFF000000; 

    HDC hdc;
    hdc = GetDC(NULL);

    // Create the DIB section with an alpha channel.
    hBitmap = CreateDIBSection(hdc, (BITMAPINFO *)&bi, DIB_RGB_COLORS, 
        (void **)&lpBits, NULL, (DWORD)0);

    memcpy(lpBits, pixelPtr + pixels_byte_offset, bytes);

    ReleaseDC(NULL,hdc);

    if ( JNI_FALSE == pixels_is_direct ) {
        (*env)->ReleasePrimitiveArrayCritical(env, pixels, (void*)pixelPtr, JNI_ABORT);  
    }

    // Create an empty mask bitmap.
    HBITMAP hMonoBitmap = CreateBitmap(dwWidth,dwHeight,1,1,NULL);

    ICONINFO ii;
    ii.fIcon = isCursor ? FALSE : TRUE;
    ii.xHotspot = hotX;
    ii.yHotspot = hotY;
    ii.hbmMask = hMonoBitmap;
    ii.hbmColor = hBitmap;

    // Create the alpha cursor with the alpha DIB section.
    handle = CreateIconIndirect(&ii);

    DeleteObject(hBitmap);          
    DeleteObject(hMonoBitmap); 

    return (jlong) (intptr_t) handle;
}

JNIEXPORT void JNICALL
Java_jogamp_newt_driver_windows_DisplayDriver_destroyIcon0(JNIEnv *env, jobject _unused, jlong jhandle) {
    HICON handle = (HICON) (intptr_t) jhandle;
    DestroyIcon(handle);
}

JNIEXPORT void JNICALL
Java_jogamp_newt_driver_windows_WindowDriver_setPointerIcon0(JNIEnv *env, jobject _unused, jlong window, jlong iconHandle) {
    HWND hwnd = (HWND) (intptr_t) window;
    WindowUserData * wud;
#if !defined(__MINGW64__) && ( defined(UNDER_CE) || _MSC_VER <= 1200 )
    wud = (WindowUserData *) GetWindowLong(hwnd, GWL_USERDATA);
#else
    wud = (WindowUserData *) GetWindowLongPtr(hwnd, GWLP_USERDATA);
#endif
    wud->setPointerAction = 0 != iconHandle ? 1 : -1;
    wud->setPointerHandle = (HCURSOR) (intptr_t) iconHandle;
    SendMessage(hwnd, WM_SETCURSOR, 0, 0);
}

