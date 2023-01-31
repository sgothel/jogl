
#include <windows.h>
#include "WindowsUser.h"

#include <stdlib.h>
#include <stdio.h>

// #define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(args...) fprintf(stderr, args);
#else
    #define DBG_PRINT(args...)
#endif

// MONITOR_DEFAULTTONULL 0x00000000
// MONITOR_DEFAULTTOPRIMARY 0x00000001
// MONITOR_DEFAULTTONEAREST 0x00000002

HMONITOR GetMonitorFromWindow(HWND hwnd) {
    return MonitorFromWindow(hwnd, MONITOR_DEFAULTTONEAREST);
}

HMONITOR GetMonitorFromPoint(int x, int y) {
    POINT pt = { (LONG)x, (LONG)y };
    return MonitorFromPoint(pt, MONITOR_DEFAULTTONEAREST);
}

HMONITOR GetMonitorFromRect(int left, int top, int right, int bottom) {
    RECT rect = { (LONG)left, (LONG)top, (LONG)right, (LONG)bottom };
    return MonitorFromRect(&rect, MONITOR_DEFAULTTONEAREST);
}

