
#include <windows.h>
#include "WindowsDWM.h"

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

typedef HRESULT (WINAPI *DwmEnableCompositionPROCADDR)(UINT uCompositionAction);
typedef HRESULT (WINAPI *DwmIsCompositionEnabledPROCADDR)(BOOL * pfEnabled);
typedef HRESULT (WINAPI *DwmEnableBlurBehindWindowPROCADDR)(HWND hWnd, const DWM_BLURBEHIND* pBlurBehind);  
typedef HRESULT (WINAPI *DwmExtendFrameIntoClientAreaPROCADDR)(HWND hwnd, const MARGINS *pMarInset);  
typedef HRESULT (WINAPI *DwmGetWindowAttributePROCADDR)(HWND hwnd, DWORD dwAttribute, PVOID pvAttribute, DWORD cbAttribute);
typedef HRESULT (WINAPI *DwmSetWindowAttributePROCADDR)(HWND hwnd, DWORD dwAttribute, LPCVOID pvAttribute, DWORD cbAttribute);
typedef BOOL (WINAPI *GetWindowCompositionAttributePROCADDR)(HWND hwnd, WINCOMPATTRDATA* pAttrData);
typedef BOOL (WINAPI *SetWindowCompositionAttributePROCADDR)(HWND hwnd, WINCOMPATTRDATA* pAttrData);

#define INIT_CALLED_MASK          1 << 0
#define INIT_HAS_DWM_EXT_MASK     1 << 1
#define INIT_HAS_WINCOMP_EXT_MASK 1 << 2

#define HAS_INIT(a)        ( 0 != ( INIT_CALLED_MASK & (a)  ) )
#define HAS_DWM_EXT(a)     ( 0 != ( INIT_HAS_DWM_EXT_MASK & (a)  ) )
#define HAS_WINCOMP_EXT(a) ( 0 != ( INIT_HAS_WINCOMP_EXT_MASK & (a)  ) )

static int _init = 0; // INIT_ bits, see above
static DwmEnableCompositionPROCADDR _DwmEnableComposition = NULL;
static DwmIsCompositionEnabledPROCADDR _DwmIsCompositionEnabled = NULL;
static DwmEnableBlurBehindWindowPROCADDR _DwmEnableBlurBehindWindow = NULL;
static DwmExtendFrameIntoClientAreaPROCADDR _DwmExtendFrameIntoClientArea = NULL;
static DwmGetWindowAttributePROCADDR _DwmGetWindowAttribute = NULL;
static DwmSetWindowAttributePROCADDR _DwmSetWindowAttribute = NULL;
static GetWindowCompositionAttributePROCADDR _GetWindowCompositionAttribute = NULL;
static SetWindowCompositionAttributePROCADDR _SetWindowCompositionAttribute = NULL;

static int initWindowsDWM() {
    if( !HAS_INIT(_init) ) {
        _init |= INIT_CALLED_MASK;
        HANDLE hDwmAPI = LoadLibrary(TEXT("dwmapi.dll"));
        if (hDwmAPI) {
            _DwmEnableComposition = (DwmEnableCompositionPROCADDR) GetProcAddressA (hDwmAPI, "DwmEnableComposition");
            _DwmIsCompositionEnabled = (DwmIsCompositionEnabledPROCADDR) GetProcAddressA (hDwmAPI, "DwmIsCompositionEnabled");
            _DwmEnableBlurBehindWindow = (DwmEnableBlurBehindWindowPROCADDR) GetProcAddressA (hDwmAPI, "DwmEnableBlurBehindWindow");
            _DwmExtendFrameIntoClientArea = (DwmExtendFrameIntoClientAreaPROCADDR) GetProcAddressA (hDwmAPI, "DwmExtendFrameIntoClientArea");
            _DwmGetWindowAttribute = (DwmGetWindowAttributePROCADDR) GetProcAddressA (hDwmAPI, "DwmGetWindowAttribute");
            _DwmSetWindowAttribute = (DwmSetWindowAttributePROCADDR) GetProcAddressA (hDwmAPI, "DwmSetWindowAttribute");
            if(NULL != _DwmEnableComposition && NULL != _DwmIsCompositionEnabled && 
               NULL != _DwmEnableBlurBehindWindow && NULL != _DwmExtendFrameIntoClientArea &&
               NULL != _DwmGetWindowAttribute && NULL != _DwmSetWindowAttribute) {
                _init |= INIT_HAS_DWM_EXT_MASK;
            }
        }
        // FreeLibrary (hDwmAPI);  
        HANDLE hUser32 = LoadLibrary(TEXT("user32.dll"));
        if (hUser32) {
            _GetWindowCompositionAttribute = (GetWindowCompositionAttributePROCADDR) GetProcAddressA (hUser32, "GetWindowCompositionAttribute");
            _SetWindowCompositionAttribute = (SetWindowCompositionAttributePROCADDR) GetProcAddressA (hUser32, "SetWindowCompositionAttribute");
            if( NULL != _GetWindowCompositionAttribute &&
                NULL != _SetWindowCompositionAttribute ) {
                _init |= INIT_HAS_WINCOMP_EXT_MASK;
            }
        }
        // FreeLibrary (hUser32);  
        DBG_PRINT("DWM - initWindowsDWM: hasDWM %d, hasWinComp %d\n", HAS_DWM_EXT(_init), HAS_WINCOMP_EXT(_init));
    }
    return _init;
}

BOOL DwmIsExtensionAvailable() {
    return HAS_DWM_EXT( initWindowsDWM() ) ? TRUE : FALSE;
}

BOOL DwmIsCompositionEnabled( ) {
    if( HAS_DWM_EXT( initWindowsDWM() ) ) {
        BOOL fEnabled = FALSE;
        if( 0 == _DwmIsCompositionEnabled(&fEnabled) ) {
            DBG_PRINT("DWM - DwmIsCompositionEnabled: %d\n", fEnabled);
            return fEnabled;
        }
    }
    DBG_PRINT("DWM - DwmIsCompositionEnabled failed\n");
    return FALSE;
}

BOOL DwmEnableComposition( UINT uCompositionAction ) {
    if( HAS_DWM_EXT( initWindowsDWM() ) ) {
        return 0 == _DwmEnableComposition(uCompositionAction) ? TRUE : FALSE;
    }
    return FALSE;
}

BOOL DwmEnableBlurBehindWindow(HWND hwnd, const DWM_BLURBEHIND* pBlurBehind) {  
    if( HAS_DWM_EXT( initWindowsDWM() ) ) {
        _DwmEnableBlurBehindWindow(hwnd, pBlurBehind);
        DBG_PRINT("DWM - DwmEnableBlurBehindWindow: hwnd %p, f %d, on %d, %p\n", 
            (void *)hwnd, 
            (int) pBlurBehind->dwFlags,
            (int) pBlurBehind->fEnable,
            (void *)pBlurBehind->hRgnBlur);
        return TRUE;
    }
    DBG_PRINT("DWM - DwmEnableBlurBehindWindow: n/a\n");
    return FALSE;
}  

BOOL DwmExtendFrameIntoClientArea(HWND hwnd, const MARGINS *pMarInset) {
    if( HAS_DWM_EXT( initWindowsDWM() ) ) {
        _DwmExtendFrameIntoClientArea(hwnd, pMarInset);
        return TRUE;
    }
    return FALSE;
}  
  
HRESULT DwmGetWindowAttribute(HWND hwnd, DWORD dwAttribute, PVOID pvAttribute, DWORD cbAttribute) {
    if( HAS_DWM_EXT( initWindowsDWM() ) ) {
        return _DwmGetWindowAttribute(hwnd, dwAttribute, pvAttribute, cbAttribute);
    }
    return E_NOINTERFACE;
}

HRESULT DwmSetWindowAttribute(HWND hwnd, DWORD dwAttribute, LPCVOID pvAttribute, DWORD cbAttribute) {
    if( HAS_DWM_EXT( initWindowsDWM() ) ) {
        return _DwmSetWindowAttribute(hwnd, dwAttribute, pvAttribute, cbAttribute);
    }
    return E_NOINTERFACE;
}

BOOL IsWindowCompositionExtensionAvailable() {
    return HAS_WINCOMP_EXT( initWindowsDWM() ) ? TRUE : FALSE;
}

BOOL GetWindowCompositionAccentPolicy(HWND hwnd, AccentPolicy* pAccentPolicy) {
    if( HAS_WINCOMP_EXT( initWindowsDWM() ) ) {
        WINCOMPATTRDATA attrData = { WCA_ACCENT_POLICY, pAccentPolicy, sizeof(AccentPolicy) };
        return _GetWindowCompositionAttribute(hwnd, &attrData);
    }
    return FALSE;
}
BOOL SetWindowCompositionAccentPolicy(HWND hwnd, const AccentPolicy* pAccentPolicy) {
    if( HAS_WINCOMP_EXT( initWindowsDWM() ) ) {
        WINCOMPATTRDATA attrData = { WCA_ACCENT_POLICY, (AccentPolicy*)pAccentPolicy, sizeof(AccentPolicy) };
        return _SetWindowCompositionAttribute(hwnd, &attrData);
    }
    return FALSE;
}

#if 0
BOOL GetWindowCompositionAttribute(HWND hwnd, WINCOMPATTRDATA* pAttrData) {
    if( HAS_WINCOMP_EXT( initWindowsDWM() ) ) {
        return _GetWindowCompositionAttribute(hwnd, pAttrData);
    }
    return FALSE;
}

BOOL SetWindowCompositionAttribute(HWND hwnd, WINCOMPATTRDATA* pAttrData) {
    if( HAS_WINCOMP_EXT( initWindowsDWM() ) ) {
        return _SetWindowCompositionAttribute(hwnd, pAttrData);
    }
    return FALSE;
}
#endif

