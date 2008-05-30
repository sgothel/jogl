/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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

#include <windows.h>
#include <stdlib.h>
#ifdef UNDER_CE
#include "aygshell.h"
#endif

/* This typedef is apparently needed for Microsoft compilers before VC8,
   and on Windows CE */
#if (_MSC_VER < 1400) || defined(UNDER_CE)
#ifdef _WIN64
typedef long long intptr_t;
#else
typedef int intptr_t;
#endif
#endif

#include "com_sun_javafx_newt_windows_WindowsWindow.h"

#include "MouseEvent.h"
#include "KeyEvent.h"

static jmethodID sizeChangedID = NULL;
static jmethodID positionChangedID = NULL;
static jmethodID windowClosedID = NULL;
static jmethodID windowDestroyedID = NULL;
static jmethodID sendMouseEventID = NULL;
static jmethodID sendKeyEventID = NULL;

// This is set by DispatchMessages, below, and cleared when it exits
static JNIEnv* env = NULL;

// Really need to factor this out in to a separate run-time file
static jchar* GetNullTerminatedStringChars(JNIEnv* env, jstring str)
{
    jchar* strChars = NULL;
    strChars = calloc((*env)->GetStringLength(env, str) + 1, sizeof(jchar));
    if (strChars != NULL) {
        (*env)->GetStringRegion(env, str, 0, (*env)->GetStringLength(env, str), strChars);
    }
    return strChars;
}

static LRESULT CALLBACK wndProc(HWND wnd, UINT message,
                                WPARAM wParam, LPARAM lParam)
{
    RECT rc;
    int useDefWindowProc = 0;
    jobject window = NULL;

#ifdef UNDER_CE
    window = (jobject) GetWindowLong(wnd, GWL_USERDATA);
#else
    window = (jobject) GetWindowLongPtr(wnd, GWLP_USERDATA);
#endif
    if (window == NULL || env == NULL) {
        // Shouldn't happen
        return DefWindowProc(wnd, message, wParam, lParam);
    }
    
    switch (message) {
    case WM_CLOSE:
        (*env)->CallVoidMethod(env, window, windowClosedID);
        DestroyWindow(wnd);
        break;

    case WM_DESTROY:
        (*env)->CallVoidMethod(env, window, windowDestroyedID);
        break;

    case WM_KEYDOWN:
        (*env)->CallVoidMethod(env, obj, sendKeyEventID, (jint) EVENT_KEY_PRESSED, 
                               (jint) 0, (jint) 0x28, (jchar) 0);
        useDefWindowProc = 1;
        break;

    case WM_KEYUP:
        (*env)->CallVoidMethod(env, obj, sendKeyEventID, (jint) EVENT_KEY_PRESSED, 
                               (jint) 0, (jint) 0x26, (jchar) 0);
        useDefWindowProc = 1;
        break;

    case WM_SIZE:
        GetClientRect(wnd, &rc);
        (*env)->CallVoidMethod(env, window, sizeChangedID, (jint) rc.right, (jint) rc.bottom);
        break;

    /* FIXME: 
    case WM_MOUSE_LALA:
        (*env)->CallVoidMethod(env, obj, sendMouseEventID, (jint) eventType, (jint) mod, 
                              (jint) x, (jint) y, (jint) button);
      */
    default:
        useDefWindowProc = 1;
    }

    if (useDefWindowProc)
        return DefWindowProc(wnd, message, wParam, lParam);
    return 0;
}

/*
 * Class:     com_sun_javafx_newt_windows_WindowsWindow
 * Method:    initIDs
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_javafx_newt_windows_WindowsWindow_initIDs
  (JNIEnv *env, jclass clazz)
{
    sizeChangedID = (*env)->GetMethodID(env, clazz, "sizeChanged", "(II)V");
    positionChangedID = (*env)->GetMethodID(env, clazz, "positionChanged", "(II)V");
    windowClosedID    = (*env)->GetMethodID(env, clazz, "windowClosed",    "()V");
    windowDestroyedID = (*env)->GetMethodID(env, clazz, "windowDestroyed", "()V");
    sendMouseEventID = (*env)->GetMethodID(env, clazz, "sendMouseEvent", "(IIIII)V");
    sendKeyEventID = (*env)->GetMethodID(env, clazz, "sendKeyEvent", "(IIIC)V");
    if (sizeChangedID == NULL ||
        positionChangedID == NULL ||
        windowClosedID == NULL ||
        windowDestroyedID == NULL ||
        sendMouseEventID == NULL ||
        sendKeyEventID == NULL) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

/*
 * Class:     com_sun_javafx_newt_windows_WindowsWindow
 * Method:    LoadLibraryW
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_javafx_newt_windows_WindowsWindow_LoadLibraryW
  (JNIEnv *env, jclass clazz, jstring dllName)
{
    jchar* _dllName = GetNullTerminatedStringChars(env, dllName);
    HMODULE lib = LoadLibraryW(_dllName);
    free(_dllName);
    return (jlong) (intptr_t) lib;
}

/*
 * Class:     com_sun_javafx_newt_windows_WindowsWindow
 * Method:    RegisterWindowClass
 * Signature: (Ljava/lang/String;J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_javafx_newt_windows_WindowsWindow_RegisterWindowClass
  (JNIEnv *env, jclass clazz, jstring appName, jlong hInstance)
{
    WNDCLASSW* wc;
    const char* _appName = NULL;

    wc = calloc(1, sizeof(WNDCLASSW));
    /* register class */
    wc->style = CS_HREDRAW | CS_VREDRAW;
    wc->lpfnWndProc = (WNDPROC)wndProc;
    wc->cbClsExtra = 0;
    wc->cbWndExtra = 0;
    /* This cast is legal because the HMODULE for a DLL is the same as
       its HINSTANCE -- see MSDN docs for DllMain */
    wc->hInstance = (HINSTANCE) hInstance;
    wc->hIcon = NULL;
    wc->hCursor = 0;
    wc->hbrBackground = GetStockObject(BLACK_BRUSH);
    wc->lpszMenuName = NULL;
    wc->lpszClassName = GetNullTerminatedStringChars(env, appName);
    if (!RegisterClassW(wc)) {
        free(wc);
        return 0;
    }
    return (jlong) (intptr_t) wc;
}

/*
 * Class:     com_sun_javafx_newt_windows_WindowsWindow
 * Method:    CreateWindow
 * Signature: (Ljava/lang/String;JJIIII)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_javafx_newt_windows_WindowsWindow_CreateWindow
  (JNIEnv *env, jobject obj, jstring windowClassName, jlong hInstance, jlong visualID,
                             jint jx, jint jy, jint defaultWidth, jint defaultHeight)
{
    jchar* wndClassName = GetNullTerminatedStringChars(env, windowClassName);
    DWORD windowStyle = WS_CLIPSIBLINGS | WS_CLIPCHILDREN | WS_VISIBLE;
    int x=(int)x, y=(int)y;
    int width=(int)defaultWidth, height=(int)defaultHeight;
    HWND window = NULL;

/** FIXME: why ? use setFullscreen() ..
 *
#ifdef UNDER_CE
    width = GetSystemMetrics(SM_CXSCREEN);
    height = GetSystemMetrics(SM_CYSCREEN);
    x = y = 0;
#else
    windowStyle |= WS_OVERLAPPEDWINDOW;
    x = CW_USEDEFAULT;
    y = 0;
    width = defaultWidth;
    height = defaultHeight;
#endif
 */
    (void) visualID; // FIXME: use the visualID ..

    window = CreateWindowW(wndClassName, wndClassName, windowStyle,
                           x, y, width, height,
                           NULL, NULL,
                           (HINSTANCE) hInstance,
                           NULL);
    if (window != NULL) {
#ifdef UNDER_CE
        SetWindowLong(window, GWL_USERDATA, (intptr_t) (*env)->NewGlobalRef(env, obj));
#else
        SetWindowLongPtr(window, GWLP_USERDATA, (intptr_t) (*env)->NewGlobalRef(env, obj));
#endif
        ShowWindow(window, SW_SHOWNORMAL);
    }
    return (jlong) (intptr_t) window;
}

/*
 * Class:     com_sun_javafx_newt_windows_WindowsWindow
 * Method:    DispatchMessages
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_newt_windows_WindowsWindow_DispatchMessages
  (JNIEnv *_env, jclass clazz, jlong window)
{
    int i = 0;
    MSG msg;
    BOOL gotOne;

    env = _env;

    // Periodically take a break
    do {
        gotOne = PeekMessage(&msg, (HWND) window, 0, 0, PM_REMOVE);
        if (gotOne) {
            ++i;
            TranslateMessage(&msg);
            DispatchMessage(&msg);
        }
    } while (gotOne && i < 100);

    env = NULL;
}

/*
 * Class:     com_sun_javafx_newt_windows_WindowsWindow
 * Method:    setSize0
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_newt_windows_WindowsWindow_setSize0
  (JNIEnv *env, jobject obj, jlong window, jint width, jint height)
{
    RECT r;
    HWND w = (HWND) window;
    GetWindowRect(w, &r);
    MoveWindow(w, r.left, r.top, width, height, TRUE);
    (*env)->CallVoidMethod(env, obj, sizeChangedID, (jint) width, (jint) height);
}

/*
 * Class:     com_sun_javafx_newt_windows_WindowsWindow
 * Method:    setFullScreen0
 * Signature: (JZ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_javafx_newt_windows_WindowsWindow_setFullScreen0
  (JNIEnv *env, jobject obj, jlong window, jboolean fullscreen)
{
#ifdef UNDER_CE
    int screenWidth;
    int screenHeight;
    HWND win = (HWND) window;

    if (fullscreen) {
        screenWidth  = GetSystemMetrics(SM_CXSCREEN);
        screenHeight = GetSystemMetrics(SM_CYSCREEN);
        /* First, hide all of the shell parts */
        SHFullScreen(win,
                     SHFS_HIDETASKBAR | SHFS_HIDESIPBUTTON | SHFS_HIDESTARTICON);
        MoveWindow(win, 0, 0, screenWidth, screenHeight, TRUE);
        (*env)->CallVoidMethod(env, obj, sizeChangedID, (jint) screenWidth, (jint) screenHeight);
    } else {
        RECT rc;
        int width, height;

        /* First, show all of the shell parts */
        SHFullScreen(win,
                     SHFS_SHOWTASKBAR | SHFS_SHOWSIPBUTTON | SHFS_SHOWSTARTICON);
        /* Now resize the window to the size of the work area */
        SystemParametersInfo(SPI_GETWORKAREA, 0, &rc, FALSE);
        width = rc.right - rc.left;
        height = rc.bottom - rc.top;
        MoveWindow(win, rc.left, rc.top, width, height, TRUE);
        (*env)->CallVoidMethod(env, obj, sizeChangedID, (jint) width, (jint) height);
    }
    return JNI_TRUE;
#else
    /* For the time being, full-screen not supported on the desktop */
    return JNI_FALSE;
#endif
}
