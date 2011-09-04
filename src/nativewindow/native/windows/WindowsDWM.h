#ifndef _WINDOWS_DWM_H_
#define _WINDOWS_DWM_H_

    #include <windows.h> 

    #define DWM_BB_ENABLE                 0x00000001  // fEnable has been specified  
    #define DWM_EC_DISABLECOMPOSITION     0
    #define DWM_EC_ENABLECOMPOSITION      1
      
    typedef struct _DWM_BLURBEHIND  
    {  
        DWORD dwFlags;  
        BOOL fEnable;  
        HRGN hRgnBlur;  
        BOOL fTransitionOnMaximized;  
    } DWM_BLURBEHIND, *PDWM_BLURBEHIND;  
      
    typedef struct _MARGINS  
    {  
        int cxLeftWidth;      // width of left border that retains its size  
        int cxRightWidth;     // width of right border that retains its size  
        int cyTopHeight;      // height of top border that retains its size  
        int cyBottomHeight;   // height of bottom border that retains its size  
    } MARGINS, *PMARGINS;  
      
    BOOL DwmIsExtensionAvailable();
    BOOL DwmIsCompositionEnabled();
    BOOL DwmEnableComposition( UINT uCompositionAction );
    BOOL DwmEnableBlurBehindWindow(HWND hwnd, const DWM_BLURBEHIND* pBlurBehind);
    BOOL DwmExtendFrameIntoClientArea(HWND hwnd, const MARGINS *pMarInset);
      
    /*
        DWM_BLURBEHIND bb = {0};
        bb.dwFlags = DWM_BB_ENABLE;
        bb.fEnable = true;
        bb.hRgnBlur = NULL;
        DwmEnableBlurBehindWindow(hWnd, &bb);
    */

#endif /* _WINDOWS_DWM_H_ */
