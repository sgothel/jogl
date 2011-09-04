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
typedef unsigned __int32    DWORD;
typedef int                 INT;
typedef __int32             INT32;
typedef __int64             INT64;
typedef float               FLOAT;
typedef struct _handle*     HANDLE;
typedef HANDLE              HBITMAP;
typedef HANDLE              HDC;
typedef HANDLE              HGDIOBJ;
typedef HANDLE              HGLRC;
typedef HANDLE              HMODULE;
typedef HANDLE              HINSTANCE;
typedef HANDLE              HPALETTE;
typedef HANDLE              HWND;
typedef HANDLE              HRGN;
typedef __int32             LONG;
typedef const char*         LPCSTR;
typedef void*               LPVOID;
typedef unsigned __int64    ULONG_PTR;
typedef struct _proc*       PROC;
typedef unsigned int*       PUINT;
typedef unsigned int        UINT;
typedef unsigned short      USHORT;
typedef unsigned short      WORD;
typedef unsigned short      ATOM;
typedef intptr_t            DWORD_PTR;
typedef intptr_t*           PDWORD_PTR;
typedef __int32             HRESULT;

/* Necessary handle typedefs for parsing wglext.h */

typedef HANDLE              HPBUFFERARB;
typedef HANDLE              HPBUFFEREXT;
typedef HANDLE              HGPUNV;

typedef struct tagRECT {
    LONG left;
    LONG top;
    LONG right;
    LONG bottom;
} RECT;

typedef RECT*               PRECT;
typedef RECT*               LPRECT;

