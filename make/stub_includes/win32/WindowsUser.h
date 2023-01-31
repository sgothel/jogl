#include <windows.h>
#include <stdint.h>

#ifndef WIN_USER_VERSION_X_X
#define WIN_USER_VERSION_X_X

HMONITOR GetMonitorFromWindow(HWND hwnd);
HMONITOR GetMonitorFromPoint(int x, int y);
HMONITOR GetMonitorFromRect(int left, int top, int right, int bottom);
  
#endif /*  WIN_USER_VERSION_X_X */

