#include <windows.h>
#include <stdint.h>

#ifndef WIN_SHC_VERSION_X_X
#define WIN_SHC_VERSION_X_X

BOOL ShcIsExtensionAvailable();
BOOL ShcGetMonitorPixelScale1(HMONITOR hmon, float *psXY);
  
#endif /*  WIN_SHC_VERSION_X_X */

