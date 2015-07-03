#ifndef WGL_DWM_VERSION_1_X

#define DWM_BB_ENABLE                 0x00000001
#define DWM_BB_BLURREGION             0x00000002
#define DWM_BB_TRANSITIONONMAXIMIZED  0x00000004
#define DWM_EC_DISABLECOMPOSITION     0
#define DWM_EC_ENABLECOMPOSITION      1

  
typedef struct tagDWM_BLURBEHIND {  
    DWORD dwFlags;  
    int fEnable; /* BOOL */
    HRGN hRgnBlur;  
    int fTransitionOnMaximized; /* BOOL */
} DWM_BLURBEHIND, *PDWM_BLURBEHIND;
  
typedef struct tagMARGINS {  
    int cxLeftWidth;
    int cxRightWidth;
    int cyTopHeight;
    int cyBottomHeight;
} MARGINS, *PMARGINS;
  
#endif /*  WGL_DWM_VERSION_1_X */

#ifndef WGL_DWM_VERSION_1_X
#define WGL_DWM_VERSION_1_X

BOOL DwmIsExtensionAvailable();
BOOL DwmIsCompositionEnabled();
BOOL DwmEnableComposition( UINT uCompositionAction );
BOOL DwmEnableBlurBehindWindow(HWND, CONST DWM_BLURBEHIND *);
BOOL DwmExtendFrameIntoClientArea(HWND, CONST MARGINS *);
  
#endif /*  WGL_DWM_VERSION_1_X */

