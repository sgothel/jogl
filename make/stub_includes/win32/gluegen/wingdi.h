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

#ifndef GDI_VERSION_1_X
#define GDI_VERSION_1_X

#include "wingdi_types.h"

// Windows routines
WINBASEAPI DWORD WINAPI GetLastError(VOID);

// GDI / ICD OpenGL-related routines
WINGDIAPI int   WINAPI ChoosePixelFormat(HDC, CONST PIXELFORMATDESCRIPTOR *);
WINGDIAPI int   WINAPI DescribePixelFormat(HDC, int, UINT, LPPIXELFORMATDESCRIPTOR);
WINGDIAPI int   WINAPI GetPixelFormat(HDC);
WINGDIAPI BOOL  WINAPI SetPixelFormat(HDC, int, CONST PIXELFORMATDESCRIPTOR *);
WINGDIAPI BOOL  WINAPI SwapBuffers(HDC);

// Routines related to bitmap creation for off-screen rendering
WINGDIAPI HDC     WINAPI CreateCompatibleDC(HDC);
WINGDIAPI HBITMAP WINAPI CreateDIBSection(HDC, CONST BITMAPINFO *, UINT, VOID **, HANDLE, DWORD);
WINGDIAPI BOOL    WINAPI DeleteDC(HDC);
WINGDIAPI BOOL    WINAPI DeleteObject(HGDIOBJ);
WINGDIAPI HGDIOBJ WINAPI SelectObject(HDC, HGDIOBJ);

// Routines for creation of a dummy window, device context and OpenGL
// context for the purposes of getting wglChoosePixelFormatARB and
// associated routines
           HINSTANCE   GetApplicationHandle();
WINUSERAPI BOOL WINAPI ShowWindow(HWND hWnd, int nCmdShow);
WINUSERAPI HDC  WINAPI GetDC(HWND);
WINUSERAPI int  WINAPI ReleaseDC(HWND hWnd, HDC hDC);
WINUSERAPI HWND WINAPI WindowFromDC(HDC hDC); // avail in >= Win2k
WINUSERAPI BOOL WINAPI GetClientRect(HWND hwnd, LPRECT lpRect);
WINUSERAPI BOOL WINAPI DestroyWindow(HWND hWnd);
WINUSERAPI DWORD WINAPI GetObjectType(HGDIOBJ h);
WINUSERAPI BOOL WINAPI IsWindowVisible(HWND hWnd);
WINUSERAPI BOOL WINAPI IsWindow(HWND hWnd);
WINUSERAPI HWND WINAPI GetParent(HWND hWnd);
WINUSERAPI HWND WINAPI SetParent(HWND hWndChild,HWND hWndNewParent);

WINUSERAPI HANDLE WINAPI GetCurrentProcess(void);
WINUSERAPI HANDLE WINAPI GetCurrentThread(void);
WINUSERAPI BOOL WINAPI GetProcessAffinityMask(HANDLE hProcess, PDWORD_PTR lpProcessAffinityMask, PDWORD_PTR lpSystemAffinityMask);
WINUSERAPI BOOL WINAPI SetProcessAffinityMask(HANDLE hProcess, DWORD_PTR dwProcessAffinityMask);
WINUSERAPI DWORD_PTR WINAPI SetThreadAffinityMask(HANDLE hThread, DWORD_PTR dwThreadAffinityMask);


// Routines for changing gamma ramp of display device
WINGDIAPI BOOL        WINAPI GetDeviceGammaRamp(HDC,LPVOID);
WINGDIAPI BOOL        WINAPI SetDeviceGammaRamp(HDC,LPVOID);

#endif /*  GDI_VERSION_1_X */

