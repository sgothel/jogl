/**
 *
 * This file is derived from w64 mingw-runtime package's mingw64/x86_64-w64-mingw32/include/wingdi.h file
 * and states: 
 *
 *     This file has no copyright assigned and is placed in the Public Domain.
 *     No warranty is given; refer to the file DISCLAIMER.PD within this package.
 *
 * Editions / Removals and a split (wingdi.h -> wingdi.h + winwgl.h) were made by the JogAmp Community, 2010
 */

#ifndef WGL_GDI_VERSION_1_X

typedef struct tagRGBQUAD {
    BYTE rgbBlue;
    BYTE rgbGreen;
    BYTE rgbRed;
    BYTE rgbReserved;
} RGBQUAD;

typedef RGBQUAD *LPRGBQUAD;

typedef struct tagBITMAPINFOHEADER {
    DWORD biSize;
    LONG biWidth;
    LONG biHeight;
    WORD biPlanes;
    WORD biBitCount;
    DWORD biCompression;
    DWORD biSizeImage;
    LONG biXPelsPerMeter;
    LONG biYPelsPerMeter;
    DWORD biClrUsed;
    DWORD biClrImportant;
} BITMAPINFOHEADER,*LPBITMAPINFOHEADER,*PBITMAPINFOHEADER;

typedef struct tagBITMAPINFO {
    BITMAPINFOHEADER bmiHeader;
    RGBQUAD bmiColors[1];
} BITMAPINFO,*LPBITMAPINFO,*PBITMAPINFO;

/* constants for the biCompression field */
#define BI_RGB        0
#define BI_RLE8       1
#define BI_RLE4       2
#define BI_BITFIELDS  3

/* DIB color table identifiers */

#define DIB_RGB_COLORS      0 /* color table in RGBs */
#define DIB_PAL_COLORS      1 /* color table in palette indices */

typedef struct tagPIXELFORMATDESCRIPTOR {
    WORD nSize;
    WORD nVersion;
    DWORD dwFlags;
    BYTE iPixelType;
    BYTE cColorBits;
    BYTE cRedBits;
    BYTE cRedShift;
    BYTE cGreenBits;
    BYTE cGreenShift;
    BYTE cBlueBits;
    BYTE cBlueShift;
    BYTE cAlphaBits;
    BYTE cAlphaShift;
    BYTE cAccumBits;
    BYTE cAccumRedBits;
    BYTE cAccumGreenBits;
    BYTE cAccumBlueBits;
    BYTE cAccumAlphaBits;
    BYTE cDepthBits;
    BYTE cStencilBits;
    BYTE cAuxBuffers;
    BYTE iLayerType;
    BYTE bReserved;
    DWORD dwLayerMask;
    DWORD dwVisibleMask;
    DWORD dwDamageMask;
} PIXELFORMATDESCRIPTOR,*PPIXELFORMATDESCRIPTOR,*LPPIXELFORMATDESCRIPTOR;

#define PFD_TYPE_RGBA 0
#define PFD_TYPE_COLORINDEX 1

#define PFD_MAIN_PLANE 0
#define PFD_OVERLAY_PLANE 1
#define PFD_UNDERLAY_PLANE (-1)

#define PFD_DOUBLEBUFFER 0x00000001
#define PFD_STEREO 0x00000002
#define PFD_DRAW_TO_WINDOW 0x00000004
#define PFD_DRAW_TO_BITMAP 0x00000008
#define PFD_SUPPORT_GDI 0x00000010
#define PFD_SUPPORT_OPENGL 0x00000020
#define PFD_GENERIC_FORMAT 0x00000040
#define PFD_NEED_PALETTE 0x00000080
#define PFD_NEED_SYSTEM_PALETTE 0x00000100
#define PFD_SWAP_EXCHANGE 0x00000200
#define PFD_SWAP_COPY 0x00000400
#define PFD_SWAP_LAYER_BUFFERS 0x00000800
#define PFD_GENERIC_ACCELERATED 0x00001000
#define PFD_SUPPORT_DIRECTDRAW 0x00002000

#define PFD_DEPTH_DONTCARE 0x20000000
#define PFD_DOUBLEBUFFER_DONTCARE 0x40000000
#define PFD_STEREO_DONTCARE 0x80000000

/* error codes (from winerror.h) */
/* FIXME: these should have a trailing "L" but apparently PCPP doesn't handle that syntax */
#define ERROR_SUCCESS                    0
#define ERROR_INVALID_PIXEL_FORMAT       2000
#define ERROR_NO_SYSTEM_RESOURCES        1450
#define ERROR_INVALID_DATA               13
#define ERROR_PROC_NOT_FOUND             127
#define ERROR_INVALID_WINDOW_HANDLE      1400

/*
 * ShowWindow() Commands
 */
#define SW_HIDE             0
#define SW_SHOWNORMAL       1
#define SW_NORMAL           1
#define SW_SHOWMINIMIZED    2
#define SW_SHOWMAXIMIZED    3
#define SW_MAXIMIZE         3
#define SW_SHOWNOACTIVATE   4
#define SW_SHOW             5
#define SW_MINIMIZE         6
#define SW_SHOWMINNOACTIVE  7
#define SW_SHOWNA           8
#define SW_RESTORE          9
#define SW_SHOWDEFAULT      10
#define SW_FORCEMINIMIZE    11
#define SW_MAX              11

/**
 * GetObjectType() Command
 */
#define OBJ_PEN 1
#define OBJ_BRUSH 2
#define OBJ_DC 3
#define OBJ_METADC 4
#define OBJ_PAL 5
#define OBJ_FONT 6
#define OBJ_BITMAP 7
#define OBJ_REGION 8
#define OBJ_METAFILE 9
#define OBJ_MEMDC 10
#define OBJ_EXTPEN 11
#define OBJ_ENHMETADC 12
#define OBJ_ENHMETAFILE 13
#define OBJ_COLORSPACE 14

#endif /*  WGL_GDI_VERSION_1_X */

#ifndef WGL_GDI_VERSION_1_X
#define WGL_GDI_VERSION_1_X

// Windows routines
WINBASEAPI DWORD WINAPI GetLastError(VOID);

// OpenGL-related routines
WINGDIAPI int   WINAPI ChoosePixelFormat(HDC, CONST PIXELFORMATDESCRIPTOR *);
WINGDIAPI int   WINAPI DescribePixelFormat(HDC, int, UINT, LPPIXELFORMATDESCRIPTOR);
WINGDIAPI int   WINAPI GetPixelFormat(HDC);
WINGDIAPI BOOL  WINAPI SetPixelFormat(HDC, int, CONST PIXELFORMATDESCRIPTOR *);
WINGDIAPI BOOL  WINAPI SwapBuffers(HDC);

/* --- FIXME: need to handle these entry points! 
WINGDIAPI HGLRC WINAPI wglCreateLayerContext(HDC, int);
WINGDIAPI BOOL  WINAPI wglUseFontBitmapsA(HDC, DWORD, DWORD, DWORD);
WINGDIAPI BOOL  WINAPI wglUseFontBitmapsW(HDC, DWORD, DWORD, DWORD);
#ifdef UNICODE
#define wglUseFontBitmaps  wglUseFontBitmapsW
#else
#define wglUseFontBitmaps wglUseFontBitmapsA
#endif // !UNICODE
*/

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

WINUSERAPI HANDLE WINAPI GetCurrentProcess(void);
WINUSERAPI BOOL WINAPI GetProcessAffinityMask(HANDLE hProcess,PDWORD_PTR lpProcessAffinityMask,PDWORD_PTR lpSystemAffinityMask);
WINUSERAPI BOOL WINAPI SetProcessAffinityMask(HANDLE hProcess,DWORD_PTR dwProcessAffinityMask);


// Routines for changing gamma ramp of display device
WINGDIAPI BOOL        WINAPI GetDeviceGammaRamp(HDC,LPVOID);
WINGDIAPI BOOL        WINAPI SetDeviceGammaRamp(HDC,LPVOID);

#endif /*  WGL_GDI_VERSION_1_X */

