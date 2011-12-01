#include <jni.h>
#include <stdlib.h>
#include <assert.h>

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#undef WIN32_LEAN_AND_MEAN

#include <wingdi.h>
#include <stddef.h>

#include <gluegen_stdint.h>

#include <stdio.h>

#include "NativewindowCommon.h"
#include "jogamp_nativewindow_windows_GDIUtil.h"

// #define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(args...) fprintf(stderr, args);
#else
    #define DBG_PRINT(args...)
#endif

static const char * const ClazzNamePoint = "javax/media/nativewindow/util/Point";
static const char * const ClazzAnyCstrName = "<init>";
static const char * const ClazzNamePointCstrSignature = "(II)V";

static jclass pointClz = NULL;
static jmethodID pointCstr = NULL;

HINSTANCE GetApplicationHandle() {
    return GetModuleHandle(NULL);
}

/*   Java->C glue code:
 *   Java package: jogamp.nativewindow.windows.GDIUtil
 *    Java method: boolean CreateWindowClass(long hInstance, java.lang.String clazzName, long wndProc)
 *     C function: BOOL CreateWindowClass(HANDLE hInstance, LPCSTR clazzName, HANDLE wndProc);
 */
JNIEXPORT jboolean JNICALL
Java_jogamp_nativewindow_windows_GDIUtil_CreateWindowClass
    (JNIEnv *env, jclass _unused, jlong jHInstance, jstring jClazzName, jlong wndProc) 
{
    HINSTANCE hInstance = (HINSTANCE) (intptr_t) jHInstance;
    const TCHAR* clazzName = NULL;
    WNDCLASS  wc;
    jboolean res;

#ifdef UNICODE
    clazzName = NewtCommon_GetNullTerminatedStringChars(env, jClazzName);
#else
    clazzName = (*env)->GetStringUTFChars(env, jClazzName, NULL);
#endif

    ZeroMemory( &wc, sizeof( wc ) );
    if( GetClassInfo( hInstance,  clazzName, &wc ) ) {
        // registered already
        res = JNI_TRUE;
    } else {
        // register now
        ZeroMemory( &wc, sizeof( wc ) );
        wc.style = CS_HREDRAW | CS_VREDRAW ;
        wc.lpfnWndProc = (WNDPROC) (intptr_t) wndProc;
        wc.cbClsExtra = 0;
        wc.cbWndExtra = 0;
        wc.hInstance = hInstance;
        wc.hIcon = NULL;
        wc.hCursor = LoadCursor( NULL, IDC_ARROW);
        wc.hbrBackground = NULL; // no background paint - GetStockObject(BLACK_BRUSH);
        wc.lpszMenuName = NULL;
        wc.lpszClassName = clazzName;
        res = ( 0 != RegisterClass( &wc ) ) ? JNI_TRUE : JNI_FALSE ;
    }

#ifdef UNICODE
    free((void*) clazzName);
#else
    (*env)->ReleaseStringUTFChars(env, jClazzName, clazzName);
#endif

    return res;
}

/*   Java->C glue code:
 *   Java package: jogamp.nativewindow.windows.GDIUtil
 *    Java method: boolean DestroyWindowClass(long hInstance, java.lang.String className)
 *     C function: BOOL DestroyWindowClass(HANDLE hInstance, LPCSTR className);
 */
JNIEXPORT jboolean JNICALL
Java_jogamp_nativewindow_windows_GDIUtil_DestroyWindowClass
    (JNIEnv *env, jclass _unused, jlong jHInstance, jstring jClazzName) 
{
    HINSTANCE hInstance = (HINSTANCE) (intptr_t) jHInstance;
    const TCHAR* clazzName = NULL;
    jboolean res;

#ifdef UNICODE
    clazzName = NewtCommon_GetNullTerminatedStringChars(env, jClazzName);
#else
    clazzName = (*env)->GetStringUTFChars(env, jClazzName, NULL);
#endif

    res = ( 0 != UnregisterClass( clazzName, hInstance ) ) ? JNI_TRUE : JNI_FALSE ;

#ifdef UNICODE
    free((void*) clazzName);
#else
    (*env)->ReleaseStringUTFChars(env, jClazzName, clazzName);
#endif

    return res;
}


/*   Java->C glue code:
 *   Java package: jogamp.nativewindow.windows.GDIUtil
 *    Java method: long CreateDummyWindow0(long hInstance, java.lang.String className, java.lang.String windowName, int x, int y, int width, int height)
 *     C function: HANDLE CreateDummyWindow0(HANDLE hInstance, LPCSTR className, LPCSTR windowName, int x, int y, int width, int height);
 */
JNIEXPORT jlong JNICALL
Java_jogamp_nativewindow_windows_GDIUtil_CreateDummyWindow0
    (JNIEnv *env, jclass _unused, jlong jHInstance, jstring jWndClassName, jstring jWndName, jint x, jint y, jint width, jint height) 
{
    HINSTANCE hInstance = (HINSTANCE) (intptr_t) jHInstance;
    const TCHAR* wndClassName = NULL;
    const TCHAR* wndName = NULL;
    DWORD     dwExStyle;
    DWORD     dwStyle;
    HWND      hWnd;

#ifdef UNICODE
    wndClassName = NewtCommon_GetNullTerminatedStringChars(env, jWndClassName);
    wndName = NewtCommon_GetNullTerminatedStringChars(env, jWndName);
#else
    wndClassName = (*env)->GetStringUTFChars(env, jWndClassName, NULL);
    wndName = (*env)->GetStringUTFChars(env, jWndName, NULL);
#endif

    dwExStyle = WS_EX_APPWINDOW | WS_EX_WINDOWEDGE;
    dwStyle = WS_OVERLAPPEDWINDOW;

    hWnd = CreateWindowEx( dwExStyle,
                           wndClassName,
                           wndName,
                           dwStyle | WS_CLIPSIBLINGS | WS_CLIPCHILDREN,
                           x, y, width, height,
                           NULL, NULL, hInstance, NULL );

#ifdef UNICODE
    free((void*) wndClassName);
    free((void*) wndName);
#else
    (*env)->ReleaseStringUTFChars(env, jWndClassName, wndClassName);
    (*env)->ReleaseStringUTFChars(env, jWndName, wndName);
#endif

    return (jlong) (intptr_t) hWnd;
}


/*
 * Class:     jogamp_nativewindow_windows_GDIUtil
 * Method:    initIDs0
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_nativewindow_windows_GDIUtil_initIDs0
  (JNIEnv *env, jclass clazz)
{
    if(NativewindowCommon_init(env)) {
        jclass c = (*env)->FindClass(env, ClazzNamePoint);
        if(NULL==c) {
            NativewindowCommon_FatalError(env, "FatalError jogamp_nativewindow_windows_GDIUtil: can't find %s", ClazzNamePoint);
        }
        pointClz = (jclass)(*env)->NewGlobalRef(env, c);
        (*env)->DeleteLocalRef(env, c);
        if(NULL==pointClz) {
            NativewindowCommon_FatalError(env, "FatalError jogamp_nativewindow_windows_GDIUtil: can't use %s", ClazzNamePoint);
        }
        pointCstr = (*env)->GetMethodID(env, pointClz, ClazzAnyCstrName, ClazzNamePointCstrSignature);
        if(NULL==pointCstr) {
            NativewindowCommon_FatalError(env, "FatalError jogamp_nativewindow_windows_GDIUtil: can't fetch %s.%s %s",
                ClazzNamePoint, ClazzAnyCstrName, ClazzNamePointCstrSignature);
        }
    }
    return JNI_TRUE;
}

LRESULT CALLBACK DummyWndProc( HWND   hWnd, UINT   uMsg, WPARAM wParam, LPARAM lParam) {
  return DefWindowProc(hWnd,uMsg,wParam,lParam);
}

/*
 * Class:     jogamp_nativewindow_windows_GDIUtil
 * Method:    getDummyWndProc0
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_jogamp_nativewindow_windows_GDIUtil_getDummyWndProc0
  (JNIEnv *env, jclass clazz)
{
    return (jlong) (intptr_t) DummyWndProc;
}

/*
 * Class:     jogamp_nativewindow_windows_GDIUtil
 * Method:    GetRelativeLocation0
 * Signature: (JJII)Ljavax/media/nativewindow/util/Point;
 */
JNIEXPORT jobject JNICALL Java_jogamp_nativewindow_windows_GDIUtil_GetRelativeLocation0
  (JNIEnv *env, jclass unused, jlong jsrc_win, jlong jdest_win, jint src_x, jint src_y)
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

