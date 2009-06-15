/*
 * Essential wgl and supporting routines and data structures extracted
 * from WINGDI.H.
 *
 * Copyright (c) 1985-1997, Microsoft Corp. All rights reserved.
 *
 */

typedef struct tagRGBQUAD {
        BYTE    rgbBlue;
        BYTE    rgbGreen;
        BYTE    rgbRed;
        BYTE    rgbReserved;
} RGBQUAD;
typedef RGBQUAD FAR* LPRGBQUAD;

typedef struct tagBITMAPINFOHEADER{
        DWORD      biSize;
        LONG       biWidth;
        LONG       biHeight;
        WORD       biPlanes;
        WORD       biBitCount;
        DWORD      biCompression;
        DWORD      biSizeImage;
        LONG       biXPelsPerMeter;
        LONG       biYPelsPerMeter;
        DWORD      biClrUsed;
        DWORD      biClrImportant;
} BITMAPINFOHEADER, FAR *LPBITMAPINFOHEADER, *PBITMAPINFOHEADER;

typedef struct tagBITMAPINFO {
    BITMAPINFOHEADER    bmiHeader;
    RGBQUAD             bmiColors[1];
} BITMAPINFO, FAR *LPBITMAPINFO, *PBITMAPINFO;

/* constants for the biCompression field */
#define BI_RGB        0
#define BI_RLE8       1
#define BI_RLE4       2
#define BI_BITFIELDS  3

/* DIB color table identifiers */

#define DIB_RGB_COLORS      0 /* color table in RGBs */
#define DIB_PAL_COLORS      1 /* color table in palette indices */

/* Pixel format descriptor */
typedef struct tagPIXELFORMATDESCRIPTOR
{
    WORD  nSize;
    WORD  nVersion;
    DWORD dwFlags;
    BYTE  iPixelType;
    BYTE  cColorBits;
    BYTE  cRedBits;
    BYTE  cRedShift;
    BYTE  cGreenBits;
    BYTE  cGreenShift;
    BYTE  cBlueBits;
    BYTE  cBlueShift;
    BYTE  cAlphaBits;
    BYTE  cAlphaShift;
    BYTE  cAccumBits;
    BYTE  cAccumRedBits;
    BYTE  cAccumGreenBits;
    BYTE  cAccumBlueBits;
    BYTE  cAccumAlphaBits;
    BYTE  cDepthBits;
    BYTE  cStencilBits;
    BYTE  cAuxBuffers;
    BYTE  iLayerType;
    BYTE  bReserved;
    DWORD dwLayerMask;
    DWORD dwVisibleMask;
    DWORD dwDamageMask;
} PIXELFORMATDESCRIPTOR, *PPIXELFORMATDESCRIPTOR, FAR *LPPIXELFORMATDESCRIPTOR;

/* pixel types */
#define PFD_TYPE_RGBA        0
#define PFD_TYPE_COLORINDEX  1

/* layer types */
#define PFD_MAIN_PLANE       0
#define PFD_OVERLAY_PLANE    1
#define PFD_UNDERLAY_PLANE   (-1)
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

/* PIXELFORMATDESCRIPTOR flags */
#define PFD_DOUBLEBUFFER            0x00000001
#define PFD_STEREO                  0x00000002
#define PFD_DRAW_TO_WINDOW          0x00000004
#define PFD_DRAW_TO_BITMAP          0x00000008
#define PFD_SUPPORT_GDI             0x00000010
#define PFD_SUPPORT_OPENGL          0x00000020
#define PFD_GENERIC_FORMAT          0x00000040
#define PFD_NEED_PALETTE            0x00000080
#define PFD_NEED_SYSTEM_PALETTE     0x00000100
#define PFD_SWAP_EXCHANGE           0x00000200
#define PFD_SWAP_COPY               0x00000400
#define PFD_SWAP_LAYER_BUFFERS      0x00000800
#define PFD_GENERIC_ACCELERATED     0x00001000
#define PFD_SUPPORT_DIRECTDRAW      0x00002000

/* PIXELFORMATDESCRIPTOR flags for use in ChoosePixelFormat only */
#define PFD_DEPTH_DONTCARE          0x20000000
#define PFD_DOUBLEBUFFER_DONTCARE   0x40000000
#define PFD_STEREO_DONTCARE         0x80000000

/* OpenGL error codes (from winerror.h) */
/* FIXME: these should have a trailing "L" but apparently PCPP doesn't handle that syntax */
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

// Windows routines
WINBASEAPI DWORD WINAPI GetLastError(VOID);
WINBASEAPI HMODULE WINAPI LoadLibraryA(LPCSTR lpLibFileName);
WINBASEAPI PROC WINAPI GetProcAddress(HMODULE hModule, LPCSTR lpProcName);

// OpenGL-related routines
WINGDIAPI int   WINAPI ChoosePixelFormat(HDC, CONST PIXELFORMATDESCRIPTOR *);
WINGDIAPI int   WINAPI DescribePixelFormat(HDC, int, UINT, LPPIXELFORMATDESCRIPTOR);
WINGDIAPI int   WINAPI GetPixelFormat(HDC);
WINGDIAPI BOOL  WINAPI SetPixelFormat(HDC, int, CONST PIXELFORMATDESCRIPTOR *);
WINGDIAPI BOOL  WINAPI wglCopyContext(HGLRC, HGLRC, UINT);
WINGDIAPI HGLRC WINAPI wglCreateContext(HDC);
WINGDIAPI BOOL  WINAPI wglDeleteContext(HGLRC);
WINGDIAPI HGLRC WINAPI wglGetCurrentContext(VOID);
WINGDIAPI HDC   WINAPI wglGetCurrentDC(VOID);
WINGDIAPI BOOL  WINAPI wglMakeCurrent(HDC, HGLRC);
WINGDIAPI BOOL  WINAPI wglShareLists(HGLRC, HGLRC);
WINGDIAPI BOOL  WINAPI SwapBuffers(HDC);
WINGDIAPI PROC  WINAPI wglGetProcAddress(LPCSTR);
WINGDIAPI BOOL  WINAPI wglSwapLayerBuffers(HDC,UINT);

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

// Routines related to bitmap creation for off-screen rendering
WINGDIAPI HDC     WINAPI CreateCompatibleDC(HDC);
WINGDIAPI HBITMAP WINAPI CreateDIBSection(HDC, CONST BITMAPINFO *, UINT, VOID **, HANDLE, DWORD);
WINGDIAPI BOOL    WINAPI DeleteDC(HDC);
WINGDIAPI BOOL    WINAPI DeleteObject(HGDIOBJ);
WINGDIAPI HGDIOBJ WINAPI SelectObject(HDC, HGDIOBJ);

// Routines for creation of a dummy window, device context and OpenGL
// context for the purposes of getting wglChoosePixelFormatARB and
// associated routines
HDC                    CreateDummyWindow(int,int,int,int);
WINUSERAPI BOOL WINAPI ShowWindow(HWND hWnd, int nCmdShow);
WINUSERAPI HDC  WINAPI GetDC(HWND);
WINUSERAPI int  WINAPI ReleaseDC(HWND hWnd, HDC hDC);
WINUSERAPI BOOL WINAPI DestroyWindow(HWND hWnd);

// Routines for changing gamma ramp of display device
WINGDIAPI BOOL        WINAPI GetDeviceGammaRamp(HDC,LPVOID);
WINGDIAPI BOOL        WINAPI SetDeviceGammaRamp(HDC,LPVOID);
