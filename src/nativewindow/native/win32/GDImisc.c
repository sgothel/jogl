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
    #define DBG_PRINT_FLUSH(args...) fprintf(stderr, args); fflush(stderr);
#else
    #define DBG_PRINT(args...)
    #define DBG_PRINT_FLUSH(args...)
#endif

static const char * const ClazzNamePoint = "com/jogamp/nativewindow/util/Point";
static const char * const ClazzAnyCstrName = "<init>";
static const char * const ClazzNamePointCstrSignature = "(II)V";

static jclass pointClz = NULL;
static jmethodID pointCstr = NULL;
static jmethodID dumpStackID = NULL;

typedef struct {
  HANDLE threadHandle;
  DWORD threadId;
  volatile BOOL threadReady;
  volatile BOOL threadDead;
} DummyThreadContext;

typedef struct {
  HINSTANCE hInstance;
  const TCHAR* wndClassName;
  const TCHAR* wndName;
  jint x;
  jint y;
  jint width;
  jint height;
  volatile HWND hWnd;
  volatile BOOL threadReady;
} DummyThreadCommand;

#define TM_OPENWIN WM_APP+1
#define TM_CLOSEWIN WM_APP+2
#define TM_STOP WM_APP+3

static const char * sTM_OPENWIN = "TM_OPENWIN";
static const char * sTM_CLOSEWIN = "TM_CLOSEWIN";
static const char * sTM_STOP = "TM_STOP";

/**  3s timeout */
static const int64_t TIME_OUT = 3000000;

static jboolean DDT_CheckAlive(JNIEnv *env, const char *msg, DummyThreadContext *ctx) {
    if( ctx->threadDead ) {
        NativewindowCommon_throwNewRuntimeException(env, "DDT is dead at %s", msg);
        return JNI_FALSE;
    } else {
        DBG_PRINT_FLUSH("*** DDT-Check ALIVE @ %s\n", msg);
        return JNI_TRUE;
    }
}

static jboolean DDT_WaitUntilCreated(JNIEnv *env, DummyThreadContext *ctx, BOOL created) {
    const int64_t t0 = NativewindowCommon_CurrentTimeMillis();
    int64_t t1 = t0;
    if( created ) {
        while( !ctx->threadReady && t1-t0 < TIME_OUT ) {
            t1 = NativewindowCommon_CurrentTimeMillis();
        }
        if( !ctx->threadReady ) {
            NativewindowCommon_throwNewRuntimeException(env, "TIMEOUT (%d ms) while waiting for DDT CREATED", (int)(t1-t0));
            return JNI_FALSE;
        }
        DBG_PRINT_FLUSH("*** DDT-Check CREATED\n");
    } else {
        while( !ctx->threadDead && t1-t0 < TIME_OUT ) {
            t1 = NativewindowCommon_CurrentTimeMillis();
        }
        if( !ctx->threadDead ) {
            NativewindowCommon_throwNewRuntimeException(env, "TIMEOUT (%d ms) while waiting for DDT DESTROYED", (int)(t1-t0));
            return JNI_FALSE;
        }
        DBG_PRINT_FLUSH("*** DDT-Check DEAD\n");
    }
    return JNI_TRUE;
}

static jboolean DDT_WaitUntilReady(JNIEnv *env, const char *msg, DummyThreadCommand *cmd) {
    const int64_t t0 = NativewindowCommon_CurrentTimeMillis();
    int64_t t1 = t0;
    while( !cmd->threadReady && t1-t0 < TIME_OUT ) {
        t1 = NativewindowCommon_CurrentTimeMillis();
    }
    if( !cmd->threadReady ) {
        NativewindowCommon_throwNewRuntimeException(env, "TIMEOUT (%d ms) while waiting for DDT %s", (int)(t1-t0), msg);
        return JNI_FALSE;
    }
    DBG_PRINT_FLUSH("*** DDT-Check READY @ %s\n", msg);
    return JNI_TRUE;
}

static HWND DummyWindowCreate
    (HINSTANCE hInstance, const TCHAR* wndClassName, const TCHAR* wndName, jint x, jint y, jint width, jint height) 
{
    DWORD dwExStyle = WS_EX_APPWINDOW | WS_EX_WINDOWEDGE;
    DWORD dwStyle = WS_OVERLAPPEDWINDOW;
    HWND hWnd = CreateWindowEx( dwExStyle,
                                wndClassName,
                                wndName,
                                dwStyle | WS_CLIPSIBLINGS | WS_CLIPCHILDREN,
                                x, y, width, height,
                                NULL, NULL, hInstance, NULL );
    return hWnd;
}

static DWORD WINAPI DummyDispatchThreadFunc(LPVOID param)
{
    MSG msg;
    BOOL bRet;
    BOOL bEOL=FALSE;
    DummyThreadContext *threadContext = (DummyThreadContext*)param;

    /* there can not be any messages for us now, as the creator waits for
       threadReady before continuing, but we must use this PeekMessage() to
       create the thread message queue */
    PeekMessage(&msg, NULL, 0, 0, PM_NOREMOVE);

    /* now we can safely say: we have a qeue and are ready to receive messages */
    // threadContext->threadId = GetCurrentThreadId();
    threadContext->threadDead = FALSE;
    threadContext->threadReady = TRUE;

    while( !bEOL && (bRet = GetMessage( &msg, NULL, 0, 0 )) != 0 ) {
        if ( -1 == bRet ) {
            fprintf(stderr, "DummyDispatchThread (id %d): GetMessage Error %d, werr %d\n", 
                (int)threadContext->threadId, (int)bRet, (int)GetLastError());
            fflush(stderr);
            bEOL = TRUE;
            break; // EOL
        } else {
            switch(msg.message) {
                case TM_OPENWIN: {
                    DummyThreadCommand *tParam = (DummyThreadCommand*)msg.wParam;
                    DBG_PRINT_FLUSH("*** DDT-Dispatch OPENWIN\n");
                    tParam->hWnd = DummyWindowCreate(tParam->hInstance, tParam->wndClassName, tParam->wndName, tParam->x, tParam->y, tParam->width, tParam->height);
                    tParam->threadReady = TRUE;
                  }
                  break;
                case TM_CLOSEWIN: {
                    DummyThreadCommand *tParam = (DummyThreadCommand*)msg.wParam;
                    DBG_PRINT_FLUSH("*** DDT-Dispatch CLOSEWIN\n");
                    DestroyWindow(tParam->hWnd);
                    tParam->threadReady = TRUE;
                  }
                  break;
                case TM_STOP: {
                    DummyThreadCommand *tParam = (DummyThreadCommand*)msg.wParam;
                    DBG_PRINT_FLUSH("*** DDT-Dispatch STOP -> DEAD\n");
                    tParam->threadReady = TRUE;
                    bEOL = TRUE;
                  }
                  break; // EOL
                default:
                  TranslateMessage(&msg); 
                  DispatchMessage(&msg); 
                  break;
            }
        }
    }
    /* dead */
    DBG_PRINT_FLUSH("*** DDT-Dispatch DEAD\n");
    threadContext->threadDead = TRUE;
    ExitThread(0);
    return 0;
}


HINSTANCE GetApplicationHandle() {
    return GetModuleHandle(NULL);
}

/*   Java->C glue code:
 *   Java package: jogamp.nativewindow.windows.GDIUtil
 *   Java method: long CreateDummyDispatchThread0()
 */
JNIEXPORT jlong JNICALL
Java_jogamp_nativewindow_windows_GDIUtil_CreateDummyDispatchThread0
    (JNIEnv *env, jclass _unused)
{
    DWORD threadId = 0;
    DummyThreadContext * dispThreadCtx = calloc(1, sizeof(DummyThreadContext));

    dispThreadCtx->threadHandle = CreateThread(NULL, 0, DummyDispatchThreadFunc, (LPVOID)dispThreadCtx, 0, &threadId);
    if( NULL == dispThreadCtx->threadHandle || 0 == threadId ) {
        const HANDLE threadHandle = dispThreadCtx->threadHandle;
        if( NULL != threadHandle ) {
            TerminateThread(threadHandle, 0);
        }
        free(dispThreadCtx);
        NativewindowCommon_throwNewRuntimeException(env, "DDT CREATE failed handle %p, id %d, werr %d", 
            (void*)threadHandle, (int)threadId, (int)GetLastError());
        return (jlong)0;
    }
    if( JNI_FALSE == DDT_WaitUntilCreated(env, dispThreadCtx, TRUE) ) {
        const HANDLE threadHandle = dispThreadCtx->threadHandle;
        if( NULL != threadHandle ) {
            TerminateThread(threadHandle, 0);
        }
        free(dispThreadCtx);
        NativewindowCommon_throwNewRuntimeException(env, "DDT CREATE (ack) failed handle %p, id %d, werr %d", 
            (void*)threadHandle, (int)threadId, (int)GetLastError());
        return (jlong)0;
    }
    DBG_PRINT_FLUSH("*** DDT Created %d\n", (int)threadId);
    dispThreadCtx->threadId = threadId;
    return (jlong) (intptr_t) dispThreadCtx;
}

/*   Java->C glue code:
 *   Java package: jogamp.nativewindow.windows.GDIUtil
 *    Java method: boolean CreateWindowClass0(long hInstance, java.lang.String clazzName, long wndProc)
 *     C function: BOOL CreateWindowClass0(HANDLE hInstance, LPCSTR clazzName, HANDLE wndProc);
 */
JNIEXPORT jboolean JNICALL
Java_jogamp_nativewindow_windows_GDIUtil_CreateWindowClass0
    (JNIEnv *env, jclass _unused, jlong jHInstance, jstring jClazzName, jlong wndProc,
     jlong iconSmallHandle, jlong iconBigHandle)
{
    HINSTANCE hInstance = (HINSTANCE) (intptr_t) jHInstance;
    const TCHAR* clazzName = NULL;
    WNDCLASSEX wc;
    jboolean res;

#ifdef UNICODE
    clazzName = NewtCommon_GetNullTerminatedStringChars(env, jClazzName);
#else
    clazzName = (*env)->GetStringUTFChars(env, jClazzName, NULL);
#endif

    ZeroMemory( &wc, sizeof( wc ) );
    if( GetClassInfoEx( hInstance,  clazzName, &wc ) ) {
        // registered already
        res = JNI_TRUE;
    } else {
        // register now
        ZeroMemory( &wc, sizeof( wc ) );
        wc.cbSize = sizeof(WNDCLASSEX);
        wc.style = CS_HREDRAW | CS_VREDRAW ;
        wc.lpfnWndProc = (WNDPROC) (intptr_t) wndProc;
        wc.cbClsExtra = 0;
        wc.cbWndExtra = 0;
        wc.hInstance = hInstance;
        wc.hIcon = (HICON) (intptr_t) iconBigHandle;
        wc.hCursor = NULL;
        wc.hbrBackground = NULL; // no background paint - GetStockObject(BLACK_BRUSH);
        wc.lpszMenuName = NULL;
        wc.lpszClassName = clazzName;
        wc.hIconSm = (HICON) (intptr_t) iconSmallHandle;
        res = ( 0 != RegisterClassEx( &wc ) ) ? JNI_TRUE : JNI_FALSE ;
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
 *   Java method: boolean DestroyWindowClass0(long hInstance, java.lang.String className, long dispThreadCtx)
 */
JNIEXPORT jboolean JNICALL
Java_jogamp_nativewindow_windows_GDIUtil_DestroyWindowClass0
    (JNIEnv *env, jclass gdiClazz, jlong jHInstance, jstring jClazzName, jlong jDispThreadCtx) 
{
    HINSTANCE hInstance = (HINSTANCE) (intptr_t) jHInstance;
    const TCHAR* clazzName = NULL;
    DummyThreadContext * dispThreadCtx = (DummyThreadContext *) (intptr_t) jDispThreadCtx;
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

    if( NULL != dispThreadCtx) {
        const HANDLE threadHandle = dispThreadCtx->threadHandle;
        const DWORD threadId = dispThreadCtx->threadId;
        DummyThreadCommand tParam = {0};
        tParam.threadReady = FALSE;
        DBG_PRINT_FLUSH("*** DDT Destroy %d\n", (int)threadId);
#ifdef VERBOSE_ON
        (*env)->CallStaticVoidMethod(env, gdiClazz, dumpStackID);
#endif

        if( JNI_FALSE == DDT_CheckAlive(env, sTM_STOP, dispThreadCtx) ) {
            free(dispThreadCtx);
            NativewindowCommon_throwNewRuntimeException(env, "DDT %s (alive) failed handle %p, id %d, werr %d", 
                sTM_STOP, (void*)threadHandle, (int)threadId, (int)GetLastError());
            return JNI_FALSE;
        }
        if ( 0 != PostThreadMessage(dispThreadCtx->threadId, TM_STOP, (WPARAM)&tParam, 0) ) {
            if( JNI_FALSE == DDT_WaitUntilReady(env, sTM_STOP, &tParam) ) {
                if( NULL != threadHandle ) {
                    TerminateThread(threadHandle, 0);
                }
                free(dispThreadCtx);
                NativewindowCommon_throwNewRuntimeException(env, "DDT Post %s (ack) failed handle %p, id %d, werr %d",
                    sTM_STOP, (void*)threadHandle, (int)threadId, (int)GetLastError());
                return JNI_FALSE;
            }
            if( JNI_FALSE == DDT_WaitUntilCreated(env, dispThreadCtx, FALSE) ) {
                if( NULL != threadHandle ) {
                    TerminateThread(threadHandle, 0);
                }
                free(dispThreadCtx);
                NativewindowCommon_throwNewRuntimeException(env, "DDT KILL %s (ack) failed handle %p, id %d, werr %d",
                    sTM_STOP, (void*)threadHandle, (int)threadId, (int)GetLastError());
                return JNI_FALSE;
            }
            free(dispThreadCtx); // free after proper DDT shutdown!
        } else {
            if( NULL != threadHandle ) {
                TerminateThread(threadHandle, 0);
            }
            free(dispThreadCtx);
            NativewindowCommon_throwNewRuntimeException(env, "DDT Post %s failed handle %p, id %d, werr %d",
                sTM_STOP, (void*)threadHandle, (int)threadId, (int)GetLastError());
            return JNI_FALSE;
        }
    }

    return res;
}

/*   Java->C glue code:
 *   Java package: jogamp.nativewindow.windows.GDIUtil
 *   Java method: long CreateDummyWindow0(long hInstance, java.lang.String className, jlong dispThreadCtx, java.lang.String windowName, int x, int y, int width, int height)
 */
JNIEXPORT jlong JNICALL
Java_jogamp_nativewindow_windows_GDIUtil_CreateDummyWindow0
    (JNIEnv *env, jclass _unused, jlong jHInstance, jstring jWndClassName, jlong jDispThreadCtx, jstring jWndName, jint x, jint y, jint width, jint height) 
{
    DummyThreadContext * dispThreadCtx = (DummyThreadContext *) (intptr_t) jDispThreadCtx;
    DummyThreadCommand tParam = {0};

    tParam.hInstance = (HINSTANCE) (intptr_t) jHInstance;
    tParam.x = x;
    tParam.y = y;
    tParam.width = width;
    tParam.height = height;
    tParam.hWnd = 0;
    tParam.threadReady = FALSE;

#ifdef UNICODE
    tParam.wndClassName = NewtCommon_GetNullTerminatedStringChars(env, jWndClassName);
    tParam.wndName = NewtCommon_GetNullTerminatedStringChars(env, jWndName);
#else
    tParam.wndClassName = (*env)->GetStringUTFChars(env, jWndClassName, NULL);
    tParam.wndName = (*env)->GetStringUTFChars(env, jWndName, NULL);
#endif

    if( NULL == dispThreadCtx ) {
        tParam.hWnd = DummyWindowCreate(tParam.hInstance, tParam.wndClassName, tParam.wndName, tParam.x, tParam.y, tParam.width, tParam.height);
    } else {
        const HANDLE threadHandle = dispThreadCtx->threadHandle;
        const DWORD threadId = dispThreadCtx->threadId;
        if( JNI_FALSE == DDT_CheckAlive(env, sTM_OPENWIN, dispThreadCtx) ) {
            free(dispThreadCtx);
            NativewindowCommon_throwNewRuntimeException(env, "DDT %s (alive) failed handle %p, id %d, werr %d", 
                sTM_OPENWIN, (void*)threadHandle, (int)threadId, (int)GetLastError());
        } else {
            if( 0 != PostThreadMessage(dispThreadCtx->threadId, TM_OPENWIN, (WPARAM)&tParam, 0) ) {
                if( JNI_FALSE == DDT_WaitUntilReady(env, sTM_OPENWIN, &tParam) ) {
                    if( NULL != threadHandle ) {
                        TerminateThread(threadHandle, 0);
                    }
                    free(dispThreadCtx);
                    tParam.hWnd = 0;
                    NativewindowCommon_throwNewRuntimeException(env, "DDT Post %s (ack) failed handle %p, id %d, werr %d", 
                        sTM_OPENWIN, (void*)threadHandle, (int)threadId, (int)GetLastError());
                }
            } else {
                if( NULL != threadHandle ) {
                    TerminateThread(threadHandle, 0);
                }
                free(dispThreadCtx);
                tParam.hWnd = 0;
                NativewindowCommon_throwNewRuntimeException(env, "DDT Post %s to handle %p, id %d failed, werr %d", 
                    sTM_OPENWIN, (void*)threadHandle, (int)threadId, (int)GetLastError());
            }
        }
    }

#ifdef UNICODE
    free((void*) tParam.wndClassName);
    free((void*) tParam.wndName);
#else
    (*env)->ReleaseStringUTFChars(env, jWndClassName, tParam.wndClassName);
    (*env)->ReleaseStringUTFChars(env, jWndName, tParam.wndName);
#endif

    return (jlong) (intptr_t) tParam.hWnd;
}

/*
 * Class:     jogamp_nativewindow_windows_GDIUtil
 * Method:    DestroyWindow0
 */
JNIEXPORT jboolean JNICALL Java_jogamp_nativewindow_windows_GDIUtil_DestroyWindow0
(JNIEnv *env, jclass unused, jlong jDispThreadCtx, jlong jwin)
{
    jboolean res = JNI_TRUE;
    DummyThreadContext * dispThreadCtx = (DummyThreadContext *) (intptr_t) jDispThreadCtx;
    HWND hWnd = (HWND) (intptr_t) jwin;
    if( NULL == dispThreadCtx ) {
        DestroyWindow(hWnd);
    } else {
        const HANDLE threadHandle = dispThreadCtx->threadHandle;
        const DWORD threadId = dispThreadCtx->threadId;
        DummyThreadCommand tParam = {0};

        tParam.hWnd = hWnd;
        tParam.threadReady = FALSE;

        if( JNI_FALSE == DDT_CheckAlive(env, sTM_CLOSEWIN, dispThreadCtx) ) {
            free(dispThreadCtx);
            res = JNI_FALSE;
            NativewindowCommon_throwNewRuntimeException(env, "DDT %s (alive) failed handle %p, id %d, werr %d", 
                sTM_CLOSEWIN, (void*)threadHandle, (int)threadId, (int)GetLastError());
        } else {
            if ( 0 != PostThreadMessage(dispThreadCtx->threadId, TM_CLOSEWIN, (WPARAM)&tParam, 0) ) {
                if( JNI_FALSE == DDT_WaitUntilReady(env, sTM_CLOSEWIN, &tParam) ) {
                    if( NULL != threadHandle ) {
                        TerminateThread(threadHandle, 0);
                    }
                    free(dispThreadCtx);
                    res = JNI_FALSE;
                    NativewindowCommon_throwNewRuntimeException(env, "DDT Post %s (ack) failed handle %p, id %d, werr %d", 
                        sTM_CLOSEWIN, (void*)threadHandle, (int)threadId, (int)GetLastError());
                }
            } else {
                if( NULL != threadHandle ) {
                    TerminateThread(threadHandle, 0);
                }
                free(dispThreadCtx);
                res = JNI_FALSE;
                NativewindowCommon_throwNewRuntimeException(env, "DDT Post %s to handle %p, id %d failed, werr %d", 
                    sTM_CLOSEWIN, (void*)threadHandle, (int)threadId, (int)GetLastError());
            }
        }
    }
    return res;
}

/*
 * Class:     jogamp_nativewindow_windows_GDIUtil
 * Method:    initIDs0
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_nativewindow_windows_GDIUtil_initIDs0
  (JNIEnv *env, jclass gdiClazz)
{
    if( NativewindowCommon_init(env) ) {
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
        dumpStackID = (*env)->GetStaticMethodID(env, gdiClazz, "dumpStack", "()V");
        if(NULL==dumpStackID) {
            NativewindowCommon_FatalError(env, "FatalError jogamp_nativewindow_windows_GDIUtil: can't get method dumpStack");
        }
    }
    return JNI_TRUE;
}

static LRESULT CALLBACK DummyWndProc( HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam) {
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
 * Signature: (JJII)Lcom/jogamp/nativewindow/util/Point;
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
    HWND hWnd = (HWND) (intptr_t) jwin;
    LONG style = GetWindowLong(hWnd, GWL_STYLE);
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
    HWND hWnd = (HWND) (intptr_t) jwin;
    LONG style = GetWindowLong(hWnd, GWL_STYLE);
    BOOL bIsUndecorated = 0 != (style & (WS_CHILD|WS_POPUP)) ;
    return bIsUndecorated ? JNI_TRUE : JNI_FALSE;
}

#if 0

#include <tlhelp32.h>
#include <tchar.h>

static void printError( TCHAR* msg )
{
  DWORD eNum;
  TCHAR sysMsg[256];
  TCHAR* p;

  eNum = GetLastError( );
  FormatMessage( FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
         NULL, eNum,
         MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), // Default language
         sysMsg, 256, NULL );

  // Trim the end of the line and terminate it with a null
  p = sysMsg;
  while( ( *p > 31 ) || ( *p == 9 ) )
    ++p;
  do { *p-- = 0; } while( ( p >= sysMsg ) &&
                          ( ( *p == '.' ) || ( *p < 33 ) ) );

  // Display the message
  _ftprintf(stderr, TEXT("\n  WARNING: %s failed with error %d (%s)"), msg, eNum, sysMsg );
}

static BOOL SetProcessThreadsAffinityMask( DWORD dwOwnerPID, DWORD_PTR newTAffinity, BOOL verbose ) 
{ 
  HANDLE hThreadSnap = INVALID_HANDLE_VALUE; 
  THREADENTRY32 te32; 
  DWORD_PTR preTAffinity;
  HANDLE threadHandle;
 
  // Take a snapshot of all running threads  
  hThreadSnap = CreateToolhelp32Snapshot( TH32CS_SNAPTHREAD, 0 ); 
  if( hThreadSnap == INVALID_HANDLE_VALUE ) 
    return( FALSE ); 
 
  // Fill in the size of the structure before using it. 
  te32.dwSize = sizeof(THREADENTRY32 ); 
 
  // Retrieve information about the first thread,
  // and exit if unsuccessful
  if( !Thread32First( hThreadSnap, &te32 ) ) 
  {
    if( verbose ) {
        printError( TEXT("Thread32First") );  // Show cause of failure
    }
    CloseHandle( hThreadSnap );     // Must clean up the snapshot object!
    return( FALSE );
  }

  // Now walk the thread list of the system,
  // and display information about each thread
  // associated with the specified process
  do 
  { 
    if( te32.th32OwnerProcessID == dwOwnerPID )
    {
      if( verbose ) {
          _ftprintf(stderr, TEXT("\n     THREAD ID      = 0x%08X, %d"), te32.th32ThreadID, te32.th32ThreadID); 
          _ftprintf(stderr, TEXT("\n     base priority  = %d"), te32.tpBasePri ); 
          _ftprintf(stderr, TEXT("\n     delta priority = %d"), te32.tpDeltaPri ); 
      }
      threadHandle = OpenThread(THREAD_ALL_ACCESS, FALSE, te32.th32ThreadID);
      if( NULL != threadHandle ) {
          preTAffinity = SetThreadAffinityMask(threadHandle, newTAffinity);
          CloseHandle(threadHandle);
          if( verbose ) {
              _ftprintf(stderr, TEXT("\n     affinity %p -> %p"), preTAffinity, newTAffinity);
          }
      } else {
          if( verbose ) {
              _ftprintf(stderr, TEXT("\n     OpenThread failed %d"), (int)GetLastError());
          }
      }
    }
  } while( Thread32Next(hThreadSnap, &te32 ) );

  if( verbose ) {
      _ftprintf(stderr, TEXT("\n"));
  }

//  Don't forget to clean up the snapshot object.
  CloseHandle( hThreadSnap );
  return( TRUE );
}

JNIEXPORT void JNICALL Java_jogamp_nativewindow_windows_GDIUtil_SetProcessThreadsAffinityMask0
  (JNIEnv *env, jclass unused, jlong affinityMask, jboolean verbose)
{
    SetProcessThreadsAffinityMask( GetCurrentProcessId(), (DWORD_PTR)(intptr_t)affinityMask, (BOOL)verbose );
}

#else

JNIEXPORT void JNICALL Java_jogamp_nativewindow_windows_GDIUtil_SetProcessThreadsAffinityMask0
  (JNIEnv *env, jclass unused, jlong affinityMask, jboolean verbose)
{
}

#endif
