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

static volatile DWORD threadid = 0;

typedef struct ThreadParam_s
{
  jlong jHInstance;
  const TCHAR* wndClassName;
  const TCHAR* wndName;
  jint x;
  jint y;
  jint width;
  jint height;
  volatile HWND hWnd;
  volatile BOOL threadReady;
} ThreadParam;

#define TM_OPENWIN WM_APP+1
#define TM_CLOSEWIN WM_APP+2
#define TM_STOP WM_APP+3

DWORD WINAPI ThreadFunc(LPVOID param)
{
    MSG msg;
    BOOL bRet;
    ThreadParam *startupThreadParam = (ThreadParam*)param;

    /* there can not be any messages for us now, as the creator waits for
       threadReady before continuing, but we must use this PeekMessage() to
       create the thread message queue */
    PeekMessage(&msg, NULL, 0, 0, PM_NOREMOVE);

    /* now we can safely say: we have a qeue and are ready to receive messages */
    startupThreadParam->threadReady = TRUE;

    while( (bRet = GetMessage( &msg, NULL, 0, 0 )) != 0) {
        if (bRet == -1) {
            return 0;
        } else {
            switch(msg.message) {
                case TM_OPENWIN: {
                    ThreadParam *tParam = (ThreadParam*)msg.wParam;
                    HINSTANCE hInstance = (HINSTANCE) (intptr_t) tParam->jHInstance;
                    DWORD dwExStyle;
                    DWORD dwStyle;
        
                    dwExStyle = WS_EX_APPWINDOW | WS_EX_WINDOWEDGE;
                    dwStyle = WS_OVERLAPPEDWINDOW;
        
                    HWND hwnd = CreateWindowEx( dwExStyle,
                                    tParam->wndClassName,
                                    tParam->wndName,
                                    dwStyle | WS_CLIPSIBLINGS | WS_CLIPCHILDREN,
                                    tParam->x, tParam->y, tParam->width, tParam->height,
                                    NULL, NULL, hInstance, NULL );
        
                    tParam->hWnd = hwnd;
                    tParam->threadReady = TRUE;
                  }
                  break;
                case TM_CLOSEWIN: {
                    ThreadParam *tParam = (ThreadParam*)msg.wParam;
                    HWND hwnd = tParam->hWnd;
                    DestroyWindow(hwnd);
                    tParam->threadReady = TRUE;
                  }
                  break;
                case TM_STOP:
                  return 0;
                default:
                  TranslateMessage(&msg); 
                  DispatchMessage(&msg); 
                  break;
            }
        }
    }
    return 0;
} /* ThreadFunc */


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

    if( 0 != threadid ) {
        NativewindowCommon_throwNewRuntimeException(env, "Native threadid already created 0x%X", (int)threadid);
        return JNI_FALSE;
    }

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

    if( JNI_TRUE == res ) {
        ThreadParam tParam = {0};

        CreateThread(NULL, 0, ThreadFunc, (LPVOID)&tParam, 0, (DWORD *)&threadid);
        if(threadid) {
            while(!tParam.threadReady) { /* nop */ }
        } else {
            res = JNI_FALSE;
        }
    }

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

    if( 0 == threadid ) {
        NativewindowCommon_throwNewRuntimeException(env, "Native threadid zero 0x%X", (int)threadid);
        return JNI_FALSE;
    }

    PostThreadMessage(threadid, TM_STOP, 0, 0);

    return res;
}

/*   Java->C glue code:
 *   Java package: jogamp.nativewindow.windows.GDIUtil
 *    Java method: long CreateDummyWindowAndMessageLoop(long hInstance, java.lang.String className, java.lang.String windowName, int x, int y, int width, int height)
 *     C function: HANDLE CreateDummyWindowAndMessageLoop(HANDLE hInstance, LPCSTR className, LPCSTR windowName, int x, int y, int width, int height);
 */
JNIEXPORT jlong JNICALL
Java_jogamp_nativewindow_windows_GDIUtil_CreateDummyWindowAndMessageLoop
    (JNIEnv *env, jclass _unused, jlong jHInstance, jstring jWndClassName, jstring jWndName, jint x, jint y, jint width, jint height) 
{
    volatile HWND hWnd = 0;
    ThreadParam tParam = {0};

    if( 0 == threadid ) {
        NativewindowCommon_throwNewRuntimeException(env, "Native threadid zero 0x%X", (int)threadid);
        return JNI_FALSE;
    }

    tParam.jHInstance = jHInstance;
    tParam.x = x;
    tParam.y = y;
    tParam.width = width;
    tParam.height = height;
    tParam.hWnd = hWnd;
    tParam.threadReady = FALSE;

#ifdef UNICODE
    tParam.wndClassName = NewtCommon_GetNullTerminatedStringChars(env, jWndClassName);
    tParam.wndName = NewtCommon_GetNullTerminatedStringChars(env, jWndName);
#else
    tParam.wndClassName = (*env)->GetStringUTFChars(env, jWndClassName, NULL);
    tParam.wndName = (*env)->GetStringUTFChars(env, jWndName, NULL);
#endif

    PostThreadMessage(threadid, TM_OPENWIN, (WPARAM)&tParam, 0);

    while(!tParam.threadReady) { /* nop */ }

#ifdef UNICODE
    free((void*) tParam.wndClassName);
    free((void*) tParam.wndName);
#else
    (*env)->ReleaseStringUTFChars(env, jWndClassName, tParam.wndClassName);
    (*env)->ReleaseStringUTFChars(env, jWndName, tParam.wndName);
#endif

    return (jlong) (intptr_t) hWnd;
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

LRESULT CALLBACK DummyWndProc( HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam) {
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

/*
 * Class:     jogamp_nativewindow_windows_GDIUtil
 * Method:    IsChild0
 */
JNIEXPORT jboolean JNICALL Java_jogamp_nativewindow_windows_GDIUtil_IsChild0
  (JNIEnv *env, jclass unused, jlong jwin)
{
    HWND hwnd = (HWND) (intptr_t) jwin;
    LONG style = GetWindowLong(hwnd, GWL_STYLE);
    BOOL bIsChild = 0 != (style & WS_CHILD) ;
    return bIsChild ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     jogamp_nativewindow_windows_GDIUtil
 * Method:    IsUndecorated0
 */
JNIEXPORT jboolean JNICALL Java_jogamp_nativewindow_windows_GDIUtil_IsUndecorated0
  (JNIEnv *env, jclass unused, jlong jwin)
{
    HWND hwnd = (HWND) (intptr_t) jwin;
    LONG style = GetWindowLong(hwnd, GWL_STYLE);
    BOOL bIsUndecorated = 0 != (style & (WS_CHILD|WS_POPUP)) ;
    return bIsUndecorated ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     jogamp_nativewindow_windows_GDIUtil
 * Method:    SendCloseMessage
 */
JNIEXPORT jboolean JNICALL Java_jogamp_nativewindow_windows_GDIUtil_SendCloseMessage
(JNIEnv *env, jclass unused, jlong jwin)
{
    ThreadParam tParam = {0};
    volatile HWND hwnd = (HWND) (intptr_t) jwin;

    if( 0 == threadid ) {
        NativewindowCommon_throwNewRuntimeException(env, "Native threadid zero 0x%X", (int)threadid);
        return JNI_FALSE;
    }

    tParam.hWnd = hwnd;
    tParam.threadReady = FALSE;

    PostThreadMessage(threadid, TM_CLOSEWIN, (WPARAM)&tParam, 0);

    while(!tParam.threadReady) { /* nop */ }

    return JNI_TRUE;
}

