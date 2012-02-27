
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

static int _init = 0; // 1: init, 2: has DWM extension
static DwmEnableCompositionPROCADDR _DwmEnableComposition = NULL;
static DwmIsCompositionEnabledPROCADDR _DwmIsCompositionEnabled = NULL;
static DwmEnableBlurBehindWindowPROCADDR _DwmEnableBlurBehindWindow = NULL;
static DwmExtendFrameIntoClientAreaPROCADDR _DwmExtendFrameIntoClientArea = NULL;

static int initWindowsDWM() {
    if(0 == _init) {
        _init = 1;
        HANDLE shell = LoadLibrary(TEXT("dwmapi.dll"));
        if (shell) {
            _DwmEnableComposition = (DwmEnableCompositionPROCADDR) GetProcAddressA (shell, "DwmEnableComposition");
            _DwmIsCompositionEnabled = (DwmIsCompositionEnabledPROCADDR) GetProcAddressA (shell, "DwmIsCompositionEnabled");
            _DwmEnableBlurBehindWindow = (DwmEnableBlurBehindWindowPROCADDR) GetProcAddressA (shell, "DwmEnableBlurBehindWindow");
            _DwmExtendFrameIntoClientArea = (DwmExtendFrameIntoClientAreaPROCADDR) GetProcAddressA (shell, "DwmExtendFrameIntoClientArea");
            if(NULL != _DwmEnableComposition && NULL != _DwmIsCompositionEnabled && 
               NULL != _DwmEnableBlurBehindWindow && NULL != _DwmExtendFrameIntoClientArea) {
                _init = 2;
            }
        }
        // FreeLibrary (shell);  
        DBG_PRINT("DWM - initWindowsDWM: %d - s %p, e %p, c %p\n", _init, shell, _DwmEnableBlurBehindWindow, _DwmExtendFrameIntoClientArea);
    }
    return _init;
}

BOOL DwmIsExtensionAvailable() {
    return (2 == initWindowsDWM()) ? TRUE : FALSE;
}

BOOL DwmIsCompositionEnabled( ) {
    if(2 == initWindowsDWM()) {
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
    if(2 == initWindowsDWM()) {
        return 0 == _DwmEnableComposition(uCompositionAction) ? TRUE : FALSE;
    }
    return FALSE;
}

BOOL DwmEnableBlurBehindWindow(HWND hwnd, const DWM_BLURBEHIND* pBlurBehind) {  
    if(2 == initWindowsDWM()) {
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
    if(2 == initWindowsDWM()) {
        _DwmExtendFrameIntoClientArea(hwnd, pMarInset);
        return TRUE;
    }
    return FALSE;
}  
  
