/* Windows #defines and typedefs required for processing of extracts
   from WINGDI.H and jawt_md.h */

#define FAR
#define WINBASEAPI
#define WINGDIAPI
#define WINUSERAPI
#define WINAPI
#define APIENTRY
#define CONST const
#define VOID void
typedef int                 BOOL;
typedef unsigned char       BYTE;
typedef char                CHAR;
typedef unsigned int        DWORD;
typedef int                 INT;
typedef int                 INT32;
typedef __int64             INT64;
typedef float               FLOAT;
typedef struct _handle*     HANDLE;
typedef HANDLE              HBITMAP;
typedef HANDLE              HDC;
typedef HANDLE              HGDIOBJ;
typedef HANDLE              HGLRC;
typedef HANDLE              HMODULE;
typedef HANDLE              HPALETTE;
typedef HANDLE              HWND;
typedef long                LONG;
typedef const char*         LPCSTR;
typedef void*               LPVOID;
typedef struct _proc*       PROC;
typedef unsigned int*       PUINT;
typedef unsigned int        UINT;
typedef unsigned short      USHORT;
typedef unsigned short      WORD;

typedef struct tagRECT
    {
    LONG left;
    LONG top;
    LONG right;
    LONG bottom;
    } 	RECT;

/* Necessary handle typedefs for parsing wglext.h */

typedef HANDLE              HPBUFFERARB;
typedef HANDLE              HPBUFFEREXT;
typedef HANDLE              HGPUNV;
