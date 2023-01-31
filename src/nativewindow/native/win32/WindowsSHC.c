
#include <windows.h>
#include "WindowsSHC.h"

#include <stdlib.h>
#include <stdio.h>

// #define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(args...) fprintf(stderr, args);
#else
    #define DBG_PRINT(args...)
#endif

/* GetProcAddress doesn't exist in A/W variants under desktop Windows */
#ifndef UNDER_CE
#define GetProcAddressA GetProcAddress
#endif

typedef HRESULT(WINAPI *GetDpiForMonitorPROCADDR)(HMONITOR, int, UINT*, UINT*);
#ifndef MDT_EFFECTIVE_DPI
    #define MDT_EFFECTIVE_DPI 0
#endif
// See also SetProcessDpiAwareness(..) and SetThreadDpiAwarenessContext(..)

#define INIT_CALLED_MASK          1 << 0
#define INIT_HAS_SHC_EXT_MASK     1 << 1

#define HAS_INIT(a)        ( 0 != ( INIT_CALLED_MASK & (a)  ) )
#define HAS_SHC_EXT(a)     ( 0 != ( INIT_HAS_SHC_EXT_MASK & (a)  ) )

static int _init = 0; // INIT_ bits, see above
static GetDpiForMonitorPROCADDR _GetDpiForMonitor = NULL;

static int initWindowsSHC() {
    if( !HAS_INIT(_init) ) {
        _init |= INIT_CALLED_MASK;
        HANDLE hShcAPI = LoadLibrary(TEXT("shcore.dll"));
        if (hShcAPI) {
            _GetDpiForMonitor = (GetDpiForMonitorPROCADDR) GetProcAddressA (hShcAPI, "GetDpiForMonitor");
            if(NULL != _GetDpiForMonitor ) {
                _init |= INIT_HAS_SHC_EXT_MASK;
            }
        }
        // FreeLibrary (hShcAPI);
        DBG_PRINT("DWM - initWindowsSHC: hasSHC %d\n", HAS_SHC_EXT(_init));
    }
    return _init;
}

BOOL ShcIsExtensionAvailable() {
    return HAS_SHC_EXT( initWindowsSHC() ) ? TRUE : FALSE;
}

BOOL ShcGetMonitorPixelScale1(HMONITOR hmon, float *psXY) {
    psXY[0] = 0;
    psXY[1] = 0;
    if( !ShcIsExtensionAvailable() ) {
        return FALSE;
    }
    if( NULL == hmon ) {
        return FALSE;
    }
    UINT dpiX=0, dpiY=0;
    if( S_OK != _GetDpiForMonitor(hmon, MDT_EFFECTIVE_DPI, &dpiX, &dpiY) ) {
        return FALSE;
    }
    psXY[0] = (float)(dpiX) / 96.0f;
    psXY[1] = (float)(dpiY) / 96.0f;
    return TRUE;
}

