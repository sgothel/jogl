#include <windows.h>
#include <stdint.h>

#ifndef WGL_DWM_VERSION_1_X

#define DWM_BB_ENABLE                 0x00000001
#define DWM_BB_BLURREGION             0x00000002
#define DWM_BB_TRANSITIONONMAXIMIZED  0x00000004
#define DWM_EC_DISABLECOMPOSITION     0
#define DWM_EC_ENABLECOMPOSITION      1

typedef enum _DWMWINDOWATTRIBUTE { 
  DWMWA_NCRENDERING_ENABLED          = 1,
  DWMWA_NCRENDERING_POLICY,
  DWMWA_TRANSITIONS_FORCEDISABLED,
  DWMWA_ALLOW_NCPAINT,
  DWMWA_CAPTION_BUTTON_BOUNDS,
  DWMWA_NONCLIENT_RTL_LAYOUT,
  DWMWA_FORCE_ICONIC_REPRESENTATION,
  DWMWA_FLIP3D_POLICY,
  DWMWA_EXTENDED_FRAME_BOUNDS,
  DWMWA_HAS_ICONIC_BITMAP,
  DWMWA_DISALLOW_PEEK,
  DWMWA_EXCLUDED_FROM_PEEK,
  DWMWA_CLOAK,
  DWMWA_CLOAKED,
  DWMWA_FREEZE_REPRESENTATION,
  DWMWA_LAST
} DWMWINDOWATTRIBUTE;

typedef enum _DWMNCRENDERINGPOLICY { 
  DWMNCRP_USEWINDOWSTYLE = 0,
  DWMNCRP_DISABLED,
  DWMNCRP_ENABLED,
  DWMNCRP_LAST
} DWMNCRENDERINGPOLICY;
  
typedef struct tagDWM_BLURBEHIND {  
    DWORD dwFlags;  
    int32_t fEnable; /* BOOL */
    HRGN hRgnBlur;  
    int32_t fTransitionOnMaximized; /* BOOL */
} DWM_BLURBEHIND, *PDWM_BLURBEHIND;
  
typedef struct tagMARGINS {  
    int32_t cxLeftWidth;
    int32_t cxRightWidth;
    int32_t cyTopHeight;
    int32_t cyBottomHeight;
} MARGINS, *PMARGINS;
  
#endif /*  WGL_DWM_VERSION_1_X */

#ifndef WGL_DWM_VERSION_1_X
#define WGL_DWM_VERSION_1_X

BOOL DwmIsExtensionAvailable();
BOOL DwmIsCompositionEnabled();
BOOL DwmEnableComposition( UINT uCompositionAction );
BOOL DwmEnableBlurBehindWindow(HWND, CONST DWM_BLURBEHIND *);
BOOL DwmExtendFrameIntoClientArea(HWND, CONST MARGINS *);
HRESULT DwmGetWindowAttribute(HWND hwnd, DWORD dwAttribute, PVOID pvAttribute, DWORD cbAttribute);
HRESULT DwmSetWindowAttribute(HWND hwnd, DWORD dwAttribute, LPCVOID pvAttribute, DWORD cbAttribute);
  
#endif /*  WGL_DWM_VERSION_1_X */

#ifndef WGL_WINCOMP_VERSION_0_X

typedef enum _AccentState {
    ACCENT_DISABLED = 0,
    ACCENT_ENABLE_GRADIENT = 1,
    ACCENT_ENABLE_TRANSPARENTGRADIENT = 2,
    ACCENT_ENABLE_BLURBEHIND = 3,
    ACCENT_INVALID_STATE = 4
} AccentState;

typedef struct _AccentPolicy {
    AccentState AccentState;
    int32_t AccentFlags;
    int32_t GradientColor;
    int32_t AnimationId;
} AccentPolicy;


#ifndef __GLUEGEN__

typedef enum _WindowCompositionAttribute {
    WCA_ACCENT_POLICY = 19
} WindowCompositionAttribute;

typedef struct _WINCOMPATTRDATA {
    /** The attribute to query */
    WindowCompositionAttribute attribute;
    /** result storage */
    PVOID pData;
    /** size of the result storage */
    ULONG dataSize;
} WINCOMPATTRDATA;

#endif

#endif /*  WGL_WINCOMP_VERSION_0_X */

#ifndef WGL_WINCOMP_VERSION_0_X
#define WGL_WINCOMP_VERSION_0_X

BOOL IsWindowCompositionExtensionAvailable();
BOOL GetWindowCompositionAccentPolicy(HWND hwnd, AccentPolicy* pAccentPolicy);
BOOL SetWindowCompositionAccentPolicy(HWND hwnd, const AccentPolicy* pAccentPolicy);
#if 0
    BOOL GetWindowCompositionAttribute(HWND hwnd, WINCOMPATTRDATA* pAttrData);
    BOOL SetWindowCompositionAttribute(HWND hwnd, WINCOMPATTRDATA* pAttrData);
#endif

#endif /*  WGL_WINCOMP_VERSION_0_X */
