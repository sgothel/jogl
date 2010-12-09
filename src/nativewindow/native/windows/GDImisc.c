#include <jni.h>
#include <stdlib.h>
#include <assert.h>

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#undef WIN32_LEAN_AND_MEAN

#include <wingdi.h>
#include <stddef.h>

#ifdef _WIN32
  #ifdef _MSC_VER
   /* This typedef is apparently needed for Microsoft compilers before VC8,
      and on Windows CE */
   #if (_MSC_VER < 1400) || defined(UNDER_CE)
    #ifdef _WIN64
     typedef long long intptr_t;
    #else
     typedef int intptr_t;
    #endif
   #endif
  #else
   #include <inttypes.h>
  #endif
#else
  #include <inttypes.h>
#endif

#include <stdio.h>

#include "com_jogamp_nativewindow_impl_windows_GDI.h"

// #define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(args...) fprintf(stderr, args);
#else
    #define DBG_PRINT(args...)
#endif

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

#define NATIVEWINDOW_DUMMY_WINDOW_NAME "__nativewindow_dummy_window"
static ATOM nativewindowClass = 0;
static HINSTANCE nativeHInstance = NULL;

LRESULT CALLBACK DummyWndProc( HWND   hWnd, UINT   uMsg, WPARAM wParam, LPARAM lParam) {
  return DefWindowProc(hWnd,uMsg,wParam,lParam);
}

HWND CreateDummyWindow0(int x, int y, int width, int height ) {
    DWORD     dwExStyle;
    DWORD     dwStyle;
    HWND      hWnd;
    HINSTANCE hInstance = GetModuleHandle(NULL);
    if( nativeHInstance != hInstance || 0 == nativewindowClass ) {
        nativeHInstance=hInstance;
        WNDCLASS  wc;
        ZeroMemory( &wc, sizeof( wc ) );
        wc.style = CS_HREDRAW | CS_VREDRAW | CS_OWNDC;
        wc.lpfnWndProc = (WNDPROC) DummyWndProc;
        wc.cbClsExtra = 0;
        wc.cbWndExtra = 0;
        wc.hInstance = hInstance;
        wc.hIcon = NULL;
        wc.hCursor = NULL;
        wc.hbrBackground = NULL;
        wc.lpszMenuName = NULL;
        wc.lpszClassName = NATIVEWINDOW_DUMMY_WINDOW_NAME;
        if( !(nativewindowClass = RegisterClass( &wc )) ) {
          fprintf(stderr, "FatalError com_jogamp_nativewindow_impl_windows_GDI: RegisterClass Failed: %d\n", GetLastError() );
          return( 0 );
        }
    }

    dwExStyle = WS_EX_APPWINDOW | WS_EX_WINDOWEDGE;
    dwStyle = WS_OVERLAPPEDWINDOW;
    if( !(hWnd=CreateWindowEx( dwExStyle,
                             NATIVEWINDOW_DUMMY_WINDOW_NAME,
                             NATIVEWINDOW_DUMMY_WINDOW_NAME,
                             dwStyle | WS_CLIPSIBLINGS | WS_CLIPCHILDREN,
                             x, y, width, height,
                             NULL, NULL, hInstance, NULL ) ) ) {
        return( 0 );
    }
    return( hWnd );
}

/*
 * Class:     com_jogamp_nativewindow_impl_windows_GDI
 * Method:    initIDs0
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_jogamp_nativewindow_impl_windows_GDI_initIDs0
  (JNIEnv *env, jclass clazz)
{
    if(NULL==pointClz) {
        jclass c = (*env)->FindClass(env, ClazzNamePoint);
        if(NULL==c) {
            _FatalError(env, "FatalError com_jogamp_nativewindow_impl_windows_GDI: can't find %s", ClazzNamePoint);
        }
        pointClz = (jclass)(*env)->NewGlobalRef(env, c);
        (*env)->DeleteLocalRef(env, c);
        if(NULL==pointClz) {
            _FatalError(env, "FatalError com_jogamp_nativewindow_impl_windows_GDI: can't use %s", ClazzNamePoint);
        }
        pointCstr = (*env)->GetMethodID(env, pointClz, ClazzAnyCstrName, ClazzNamePointCstrSignature);
        if(NULL==pointCstr) {
            _FatalError(env, "FatalError com_jogamp_nativewindow_impl_windows_GDI: can't fetch %s.%s %s",
                ClazzNamePoint, ClazzAnyCstrName, ClazzNamePointCstrSignature);
        }
    }
    return JNI_TRUE;
}

/*
 * Class:     com_jogamp_nativewindow_impl_windows_GDI
 * Method:    GetRelativeLocation0
 * Signature: (JJII)Ljavax/media/nativewindow/util/Point;
 */
JNIEXPORT jobject JNICALL Java_com_jogamp_nativewindow_impl_windows_GDI_GetRelativeLocation0
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

