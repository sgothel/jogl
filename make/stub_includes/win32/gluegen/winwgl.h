/**
 *
 * This file is derived from w64 mingw-runtime package's mingw64/x86_64-w64-mingw32/include/wingdi.h file
 * and states: 
 *
 *     This file has no copyright assigned and is placed in the Public Domain.
 *     No warranty is given; refer to the file DISCLAIMER.PD within this package.
 *
 * Editions / Removals and a split (wingdi.h -> wingdi.h + wingdi_types.h + winwgl.h) were made by the JogAmp Community, 2010, 2012
 */

/**
 * These are standard include replacement files
 * for gluegen processing only!
 */
#ifndef __GLUEGEN__
    #error "This file is intended to be used for GlueGen code generation, not native compilation.
#endif

#ifndef WGL_GDI_VERSION_1_X

#include "wingdi_types.h"

/* layer types */
#define WGL_SWAP_MAIN_PLANE 1
#define WGL_SWAP_OVERLAY1 2
#define WGL_SWAP_OVERLAY2 4
#define WGL_SWAP_OVERLAY3 8
#define WGL_SWAP_OVERLAY4 16
#define WGL_SWAP_OVERLAY5 32
#define WGL_SWAP_OVERLAY6 64
#define WGL_SWAP_OVERLAY7 128
#define WGL_SWAP_OVERLAY8 256
#define WGL_SWAP_OVERLAY9 512
#define WGL_SWAP_OVERLAY10 1024
#define WGL_SWAP_OVERLAY11 2048
#define WGL_SWAP_OVERLAY12 4096
#define WGL_SWAP_OVERLAY13 8192
#define WGL_SWAP_OVERLAY14 16384
#define WGL_SWAP_OVERLAY15 32768
#define WGL_SWAP_UNDERLAY1 65536
#define WGL_SWAP_UNDERLAY2 0x20000
#define WGL_SWAP_UNDERLAY3 0x40000
#define WGL_SWAP_UNDERLAY4 0x80000
#define WGL_SWAP_UNDERLAY5 0x100000
#define WGL_SWAP_UNDERLAY6 0x200000
#define WGL_SWAP_UNDERLAY7 0x400000
#define WGL_SWAP_UNDERLAY8 0x800000
#define WGL_SWAP_UNDERLAY9 0x1000000
#define WGL_SWAP_UNDERLAY10 0x2000000
#define WGL_SWAP_UNDERLAY11 0x4000000
#define WGL_SWAP_UNDERLAY12 0x8000000
#define WGL_SWAP_UNDERLAY13 0x10000000
#define WGL_SWAP_UNDERLAY14 0x20000000
#define WGL_SWAP_UNDERLAY15 0x40000000

#endif /*  WGL_GDI_VERSION_1_X */

#ifndef WGL_GDI_VERSION_1_X
#define WGL_GDI_VERSION_1_X

WINGDIAPI BOOL  WINAPI wglCopyContext(HGLRC,HGLRC,UINT);
WINGDIAPI HGLRC WINAPI wglCreateContext(HDC);
WINGDIAPI BOOL  WINAPI wglDeleteContext(HGLRC);
WINGDIAPI HGLRC WINAPI wglGetCurrentContext(VOID);
WINGDIAPI HDC   WINAPI wglGetCurrentDC(VOID);
WINGDIAPI BOOL  WINAPI wglMakeCurrent(HDC, HGLRC);
WINGDIAPI BOOL  WINAPI wglShareLists(HGLRC, HGLRC);
WINGDIAPI PROC  WINAPI wglGetProcAddress(LPCSTR);
WINGDIAPI BOOL  WINAPI wglSwapLayerBuffers(HDC,UINT);

// Runtime Link GDI/OpenGL-related routines
WINGDIAPI int   WINAPI wglChoosePixelFormat(HDC, CONST PIXELFORMATDESCRIPTOR *);
WINGDIAPI int   WINAPI wglDescribePixelFormat(HDC, int, UINT, LPPIXELFORMATDESCRIPTOR);
WINGDIAPI int   WINAPI wglGetPixelFormat(HDC);
WINGDIAPI BOOL  WINAPI wglSetPixelFormat(HDC, int, CONST PIXELFORMATDESCRIPTOR *);
WINGDIAPI BOOL  WINAPI wglSwapBuffers(HDC);

/* --- FIXME: need to handle these entry points! 
WINGDIAPI HGLRC WINAPI wglCreateLayerContext(HDC, int);
WINGDIAPI BOOL  WINAPI wglUseFontBitmapsA(HDC, DWORD, DWORD, DWORD);
WINGDIAPI BOOL  WINAPI wglUseFontBitmapsW(HDC, DWORD, DWORD, DWORD);
#ifdef UNICODE
#define wglUseFontBitmaps  wglUseFontBitmapsW
#else
#define wglUseFontBitmaps  wglUseFontBitmapsA
#endif // !UNICODE
*/

#endif /*  WGL_GDI_VERSION_1_X */

