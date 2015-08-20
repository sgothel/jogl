/**
 * Copyright 2015 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 *
 ***
 *
 * This code is inspired by Ofek Shilon's code and blog post:
 *    <http://ofekshilon.com/2014/06/19/reading-specific-monitor-dimensions/>
 *    See: function 'NewtEDID_GetMonitorSizeFromEDIDByModelName'
 *
 * In contrast to Ofek's code, function 'NewtEDID_GetMonitorSizeFromEDIDByDevice'
 * uses the proper link from 
 *      DISPLAY_DEVICE.DeviceID -> SP_DEVICE_INTERFACE_DETAIL_DATA.DevicePath,
 * where DISPLAY_DEVICE.DeviceID is the monitor's enumeration via:
 *      EnumDisplayDevices(adapterName, monitor_idx, &ddMon, EDD_GET_DEVICE_INTERFACE_NAME);
 * Hence the path to the registry-entry is well determined instead of just comparing
 * the monitor's model name.
 *
 */

#include <Windows.h>
#include <Windowsx.h>
#include <tchar.h>
#include <stdlib.h>
#include <stdio.h>
#include <stddef.h>

#include <SetupApi.h>
#include <cfgmgr32.h>   // for MAX_DEVICE_ID_LEN

#include "WindowsEDID.h"

// #define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(x, ...) _ftprintf(stderr, __T(x), ##__VA_ARGS__); fflush(stderr) 
#else
    #define DBG_PRINT(...)
#endif

#define NAME_SIZE 128

/* GetProcAddress doesn't exist in A/W variants under desktop Windows */
#ifndef UNDER_CE
#define GetProcAddressA GetProcAddress
#endif

#ifndef EDD_GET_DEVICE_INTERFACE_NAME
#define EDD_GET_DEVICE_INTERFACE_NAME 0x00000001
#endif

static const GUID GUID_CLASS_MONITOR = { 0x4d36e96e, 0xe325, 0x11ce, 0xbf, 0xc1, 0x08, 0x00, 0x2b, 0xe1, 0x03, 0x18 };
static const GUID GUID_DEVINTERFACE_MONITOR = { 0xe6f07b5f, 0xee97, 0x4a90, 0xb0, 0x76, 0x33, 0xf5, 0x7b, 0xf4, 0xea, 0xa7 };

#ifdef _UNICODE
typedef  HDEVINFO (WINAPI *SetupDiGetClassDevsPROCADDR)(CONST GUID *ClassGuid,PCWSTR Enumerator,HWND hwndParent,DWORD Flags);
typedef  WINBOOL (WINAPI *SetupDiGetDeviceInstanceIdPROCADDR)(HDEVINFO DeviceInfoSet,PSP_DEVINFO_DATA DeviceInfoData,PCWSTR DeviceInstanceId,DWORD DeviceInstanceIdSize,PDWORD RequiredSize);
typedef  WINBOOL (WINAPI *SetupDiGetDeviceInterfaceDetailPROCADDR)(HDEVINFO DeviceInfoSet,PSP_DEVICE_INTERFACE_DATA DeviceInterfaceData,PSP_DEVICE_INTERFACE_DETAIL_DATA_W DeviceInterfaceDetailData,DWORD DeviceInterfaceDetailDataSize,PDWORD RequiredSize,PSP_DEVINFO_DATA DeviceInfoData);
#else
typedef  HDEVINFO (WINAPI *SetupDiGetClassDevsPROCADDR)(CONST GUID *ClassGuid,PCSTR Enumerator,HWND hwndParent,DWORD Flags);
typedef  WINBOOL (WINAPI *SetupDiGetDeviceInstanceIdPROCADDR)(HDEVINFO DeviceInfoSet,PSP_DEVINFO_DATA DeviceInfoData,PSTR DeviceInstanceId,DWORD DeviceInstanceIdSize,PDWORD RequiredSize);
typedef  WINBOOL (WINAPI *SetupDiGetDeviceInterfaceDetailPROCADDR)(HDEVINFO DeviceInfoSet,PSP_DEVICE_INTERFACE_DATA DeviceInterfaceData,PSP_DEVICE_INTERFACE_DETAIL_DATA_A DeviceInterfaceDetailData,DWORD DeviceInterfaceDetailDataSize,PDWORD RequiredSize,PSP_DEVINFO_DATA DeviceInfoData);
#endif

typedef  WINBOOL (WINAPI *SetupDiEnumDeviceInfoPROCADDR)(HDEVINFO DeviceInfoSet,DWORD MemberIndex,PSP_DEVINFO_DATA DeviceInfoData);
typedef  WINBOOL (WINAPI *SetupDiEnumDeviceInterfacesPROCADDR)(HDEVINFO DeviceInfoSet,PSP_DEVINFO_DATA DeviceInfoData,CONST GUID *InterfaceClassGuid,DWORD MemberIndex,PSP_DEVICE_INTERFACE_DATA DeviceInterfaceData);
typedef  HKEY (WINAPI *SetupDiOpenDevRegKeyPROCADDR)(HDEVINFO DeviceInfoSet,PSP_DEVINFO_DATA DeviceInfoData,DWORD Scope,DWORD HwProfile,DWORD KeyType,REGSAM samDesired);
typedef  WINBOOL (WINAPI *SetupDiDestroyDeviceInfoListPROCADDR)(HDEVINFO DeviceInfoSet);

static int WinSetupAPI_avail = 0;
static SetupDiGetClassDevsPROCADDR WinSetup_SetupDiGetClassDevs = NULL;
static SetupDiGetDeviceInstanceIdPROCADDR WinSetup_SetupDiGetDeviceInstanceId = NULL;
static SetupDiGetDeviceInterfaceDetailPROCADDR WinSetup_SetupDiGetDeviceInterfaceDetail = NULL;
static SetupDiEnumDeviceInfoPROCADDR WinSetup_SetupDiEnumDeviceInfo = NULL;
static SetupDiEnumDeviceInterfacesPROCADDR WinSetup_SetupDiEnumDeviceInterfaces = NULL;
static SetupDiOpenDevRegKeyPROCADDR WinSetup_SetupDiOpenDevRegKey = NULL;
static SetupDiDestroyDeviceInfoListPROCADDR WinSetup_SetupDiDestroyDeviceInfoList = NULL;

static int _init = 0;

int NewtEDID_init() {
    if( !_init ) {
        WinSetupAPI_avail = 0;
        HANDLE setup = LoadLibrary(TEXT("setupapi.dll"));
        if( setup ) {
        #ifdef _UNICODE
            WinSetup_SetupDiGetClassDevs = (SetupDiGetClassDevsPROCADDR) GetProcAddressA(setup, "SetupDiGetClassDevsW");
            WinSetup_SetupDiGetDeviceInstanceId = (SetupDiGetDeviceInstanceIdPROCADDR) GetProcAddressA(setup, "SetupDiGetDeviceInstanceIdW");
            WinSetup_SetupDiGetDeviceInterfaceDetail = (SetupDiGetDeviceInterfaceDetailPROCADDR) GetProcAddressA(setup, "SetupDiGetDeviceInterfaceDetailW");
        #else
            WinSetup_SetupDiGetClassDevs = (SetupDiGetClassDevsPROCADDR) GetProcAddressA(setup, "SetupDiGetClassDevsA");
            WinSetup_SetupDiGetDeviceInstanceId = (SetupDiGetDeviceInstanceIdPROCADDR) GetProcAddressA(setup, "SetupDiGetDeviceInstanceIdA");
            WinSetup_SetupDiGetDeviceInterfaceDetail = (SetupDiGetDeviceInterfaceDetailPROCADDR) GetProcAddressA(setup, "SetupDiGetDeviceInterfaceDetailA");
        #endif
            WinSetup_SetupDiEnumDeviceInfo = (SetupDiEnumDeviceInfoPROCADDR) GetProcAddressA(setup, "SetupDiEnumDeviceInfo");
            WinSetup_SetupDiEnumDeviceInterfaces = (SetupDiEnumDeviceInterfacesPROCADDR) GetProcAddressA(setup, "SetupDiEnumDeviceInterfaces");
            WinSetup_SetupDiOpenDevRegKey = (SetupDiOpenDevRegKeyPROCADDR) GetProcAddressA(setup, "SetupDiOpenDevRegKey");
            WinSetup_SetupDiDestroyDeviceInfoList = (SetupDiDestroyDeviceInfoListPROCADDR) GetProcAddressA(setup, "SetupDiDestroyDeviceInfoList");
            if( NULL != WinSetup_SetupDiGetClassDevs &&
                NULL != WinSetup_SetupDiGetDeviceInstanceId &&
                NULL != WinSetup_SetupDiGetDeviceInterfaceDetail &&
                NULL != WinSetup_SetupDiEnumDeviceInfo &&
                NULL != WinSetup_SetupDiEnumDeviceInterfaces &&
                NULL != WinSetup_SetupDiOpenDevRegKey &&
                NULL != WinSetup_SetupDiDestroyDeviceInfoList ) {
                WinSetupAPI_avail = 1;
            }
        }
        _init = 1;
    }
    return WinSetupAPI_avail;
}

static _TCHAR* Get2ndSlashBlock(const _TCHAR* sIn, _TCHAR* sOut, size_t sOutLen)
{
    _TCHAR* s = _tcschr(sIn, '\\');
    if( NULL != s ) {
        s += 1; // skip '\\'
        _TCHAR* t = _tcschr(s, '\\');
        if( NULL != t ) {
            size_t len = t - s;
            if( len > 0 ) {
                if( sOutLen >= len ) {
                    // Bug 1196: Unresolved strncpy_s (MSVCRT) on WinXP.
                    // Mapped: _tcsncpy_s -> strncpy_s (!UNICODE).
                    // On WinXP MSVCRT has no strncpy_s.
                    // _tcsncpy_s(sOut, sOutLen, s, len);
                    if( len <= sOutLen-1 ) {
                        _tcsncpy(sOut, s, len);
                        return sOut;
                    }
                }
            }
        }
    }
    return NULL;
}

static int GetMonitorSizeFromEDIDByRegKey(const HKEY hEDIDRegKey, int* widthMm, int* heightMm, int *widthCm, int *heightCm)
{
    DWORD dwType, actualValueNameLength = NAME_SIZE;
    _TCHAR valueName[NAME_SIZE];

    BYTE edidData[1024];
    DWORD edidSize = sizeof(edidData);

    *widthMm = -1;
    *heightMm = -1;
    *widthCm = -1;
    *heightCm = -1;

    LONG retValue;
    DWORD i;
    for (i = 0, retValue = ERROR_SUCCESS; retValue != ERROR_NO_MORE_ITEMS; i++) {
        retValue = RegEnumValue(hEDIDRegKey, i, &valueName[0],
                                &actualValueNameLength, NULL, &dwType,
                                edidData, // buffer
                                &edidSize); // buffer size

        if ( retValue == ERROR_SUCCESS && edidSize >= 23 &&
             0 == _tcscmp(valueName, _T("EDID")) )
        {
            DBG_PRINT("*** EDID Version %d.%d, data-size %d\n", (int)edidData[18], (int)edidData[19], edidSize);
            if( edidSize >= 69 ) {
                // 54 + 12 = 66: Horizontal display size, mm, 8 lsbits (0–4095 mm, 161 in)
                // 54 + 13 = 67: Vertical display size, mm, 8 lsbits (0–4095 mm, 161 in)
                // 54 + 14 = 68:
                //               Bits 7–4   Horizontal display size, mm, 4 msbits
                //               Bits 3–0   Vertical display size, mm, 4 msbits
                *widthMm  = ( (int)(edidData[68] & 0xF0) << 4 ) | (int)edidData[66];
                *heightMm = ( (int)(edidData[68] & 0x0F) << 8 ) | (int)edidData[67];
            }
            *widthCm = (int) edidData[21];
            *heightCm = (int) edidData[22];
            return 1; // valid EDID found
        }
    }
    return 0; // EDID not found
}

int NewtEDID_GetMonitorSizeFromEDIDByModelName(const DISPLAY_DEVICE* ddMon, int* widthMm, int* heightMm, int *widthCm, int *heightCm)
{
    _TCHAR useDevModelNameStore[MAX_DEVICE_ID_LEN];
    _TCHAR *useDevModelName = Get2ndSlashBlock(ddMon->DeviceID, useDevModelNameStore, MAX_DEVICE_ID_LEN);
    if( NULL == useDevModelName ) {
        return 0;
    }

    HDEVINFO devInfo = WinSetup_SetupDiGetClassDevs(
        &GUID_CLASS_MONITOR, //class GUID
        NULL, //enumerator
        NULL, //HWND
        DIGCF_PRESENT | DIGCF_PROFILE); // Flags //DIGCF_ALLCLASSES|

    if (NULL == devInfo) {
        return 0;
    }

    int bRes = 0;
    DWORD i;
    DWORD lastError = ERROR_SUCCESS;
    for (i = 0; !bRes && ERROR_SUCCESS == lastError; i++) {
        SP_DEVINFO_DATA devInfoData;
        memset(&devInfoData, 0, sizeof(devInfoData));
        devInfoData.cbSize = sizeof(devInfoData);

        if (WinSetup_SetupDiEnumDeviceInfo(devInfo, i, &devInfoData)) {
            _TCHAR devModelName[MAX_DEVICE_ID_LEN];
            WinSetup_SetupDiGetDeviceInstanceId(devInfo, &devInfoData, devModelName, MAX_PATH, NULL);

            if( NULL != _tcsstr(devModelName, useDevModelName) ) {
                HKEY hEDIDRegKey = WinSetup_SetupDiOpenDevRegKey(devInfo, &devInfoData,
                                                        DICS_FLAG_GLOBAL, 0, DIREG_DEV, KEY_READ);

                if ( 0 != hEDIDRegKey && hEDIDRegKey != INVALID_HANDLE_VALUE ) {
                    bRes = GetMonitorSizeFromEDIDByRegKey(hEDIDRegKey, widthMm, heightMm, widthCm, heightCm);
                    RegCloseKey(hEDIDRegKey);
                }
            }
        }
        lastError = GetLastError();
    }
    WinSetup_SetupDiDestroyDeviceInfoList(devInfo);
    return bRes;
}

int NewtEDID_GetMonitorSizeFromEDIDByDevice(const DISPLAY_DEVICE* ddMon, int* widthMm, int* heightMm, int *widthCm, int *heightCm)
{
    HDEVINFO devInfo = WinSetup_SetupDiGetClassDevs(
        &GUID_DEVINTERFACE_MONITOR,
        NULL, //enumerator
        NULL, //HWND
        DIGCF_DEVICEINTERFACE | DIGCF_PRESENT); // Flags //DIGCF_ALLCLASSES|

    if (NULL == devInfo) {
        return 0;
    }

    DWORD devIfaceDetailDataSize = offsetof(SP_DEVICE_INTERFACE_DETAIL_DATA, DevicePath) + MAX_PATH * sizeof(TCHAR);
    PSP_DEVICE_INTERFACE_DETAIL_DATA pDevIfaceDetailData = (PSP_DEVICE_INTERFACE_DETAIL_DATA) malloc(devIfaceDetailDataSize);

    int bRes = 0;
    DWORD i;
    DWORD lastError = ERROR_SUCCESS;
    for (i = 0; !bRes && ERROR_SUCCESS == lastError; i++) {
        SP_DEVICE_INTERFACE_DATA devIfaceData;
        memset(&devIfaceData, 0, sizeof(devIfaceData));
        devIfaceData.cbSize = sizeof(devIfaceData);

        if ( WinSetup_SetupDiEnumDeviceInterfaces(devInfo, NULL, &GUID_DEVINTERFACE_MONITOR, i, &devIfaceData) ) {
            memset(pDevIfaceDetailData, 0, devIfaceDetailDataSize);
            pDevIfaceDetailData->cbSize = sizeof(*pDevIfaceDetailData);
            DWORD devIfaceDetailDataReqSize = 0;
            SP_DEVINFO_DATA devInfoData2;
            memset(&devInfoData2, 0, sizeof(devInfoData2));
            devInfoData2.cbSize = sizeof(devInfoData2);
            if( WinSetup_SetupDiGetDeviceInterfaceDetail(devInfo, &devIfaceData, pDevIfaceDetailData, devIfaceDetailDataSize, 
                                                         &devIfaceDetailDataReqSize, &devInfoData2) ) {
                int found = 0 == _tcsicmp(pDevIfaceDetailData->DevicePath, ddMon->DeviceID);
                DBG_PRINT("*** Got[%d].2 found %d, devicePath <%s>\n", i, found, pDevIfaceDetailData->DevicePath);
                if( found ) {
                    HKEY hEDIDRegKey = WinSetup_SetupDiOpenDevRegKey(devInfo, &devInfoData2,
                                                                     DICS_FLAG_GLOBAL, 0, DIREG_DEV, KEY_READ);
                    DBG_PRINT("*** Got[%d] hEDIDRegKey %p\n", i, (void*)hEDIDRegKey);
                    if ( 0 != hEDIDRegKey && hEDIDRegKey != INVALID_HANDLE_VALUE ) {
                        bRes = GetMonitorSizeFromEDIDByRegKey(hEDIDRegKey, widthMm, heightMm, widthCm, heightCm);
                        RegCloseKey(hEDIDRegKey);
                    }
                }
            } else {
                lastError = GetLastError();
                DBG_PRINT("*** fail.2 at %d, werr %d\n", i, lastError);
            }
        } else {
            lastError = GetLastError();
            DBG_PRINT("*** fail.1 at %d, werr %d\n", i, lastError);
        }
    }
    DBG_PRINT("*** Result: found %d, enum-iter %d, werr %d\n", bRes, i, (int)lastError);
    WinSetup_SetupDiDestroyDeviceInfoList(devInfo);
    free(pDevIfaceDetailData);
    return bRes;
}

int NewtEDID_GetIndexedDisplayDevice(int useDevIdx, int useMonIdx, DISPLAY_DEVICE* ddMonOut, int getDeviceInterfaceName, int verbose)
{
    DISPLAY_DEVICE ddAdp;
    DWORD devIdx; // device index
    DWORD monIdx; // monitor index

    memset(&ddAdp, 0, sizeof(ddAdp));
    ddAdp.cb = sizeof(ddAdp);
     
    const DWORD dwFlagsMonitor = 0 != getDeviceInterfaceName ? EDD_GET_DEVICE_INTERFACE_NAME : 0;

    for(devIdx = 0; 
        ( devIdx <= useDevIdx || 0 > useDevIdx ) && EnumDisplayDevices(0, devIdx, &ddAdp, 0); 
        devIdx++) 
    {
        if( NULL != ddAdp.DeviceName && 0 != _tcslen(ddAdp.DeviceName) ) {
            if( verbose ) {
                _ftprintf(stderr, __T("*** [%02d:__]: deviceName <%s> flags 0x%X active %d\n"), 
                    devIdx, ddAdp.DeviceName, ddAdp.StateFlags, ( 0 != ( ddAdp.StateFlags & DISPLAY_DEVICE_ACTIVE ) ) );
                _ftprintf(stderr, __T("           deviceString <%s> \n"), ddAdp.DeviceString);
                _ftprintf(stderr, __T("           deviceID     <%s> \n"), ddAdp.DeviceID);
            }
            if( devIdx == useDevIdx || 0 > useDevIdx ) {
                DISPLAY_DEVICE ddMon;
                memset(&ddMon, 0, sizeof(ddMon));
                ddMon.cb = sizeof(ddMon);

                for(monIdx = 0; 
                    ( monIdx <= useMonIdx || 0 > useMonIdx ) && EnumDisplayDevices(ddAdp.DeviceName, monIdx, &ddMon, dwFlagsMonitor); 
                    monIdx++)
                {
                    if( NULL != ddMon.DeviceName && 0 < _tcslen(ddMon.DeviceName) ) {
                        if( verbose ) {
                            _ftprintf(stderr, __T("*** [%02d:%02d]: deviceName <%s> flags 0x%X active %d\n"), 
                                devIdx, monIdx, ddMon.DeviceName, ddMon.StateFlags, ( 0 != ( ddMon.StateFlags & DISPLAY_DEVICE_ACTIVE ) ) );
                            _ftprintf(stderr, __T("           deviceString <%s> \n"), ddMon.DeviceString);
                            _ftprintf(stderr, __T("           deviceID     <%s> \n"), ddMon.DeviceID);
                        }
                        if( monIdx == useMonIdx ) {
                            *ddMonOut = ddMon;
                            return 1;
                        }
                    }
                    memset(&ddMon, 0, sizeof(ddMon));
                    ddMon.cb = sizeof(ddMon);
                }
            }
        }
        memset(&ddAdp, 0, sizeof(ddAdp));
        ddAdp.cb = sizeof(ddAdp);
    }
    memset(ddMonOut, 0, sizeof(*ddMonOut));
    ddMonOut->cb = sizeof(*ddMonOut);
    return 0;
}

#ifdef WINDOWS_EDID_WITH_MAIN

int _tmain(int argc, _TCHAR* argv [])
{
#ifdef _UNICODE
    _ftprintf(stderr, __T("_UNICODE enabled\n"));
#else
    fprintf(stderr, "_UNICODE disabled\n");
#endif
    if( !NewtEDID_init() ) {
        _ftprintf(stderr, __T("setupapi not available\n"));
        return 1;
    }
    DISPLAY_DEVICE ddMon;

    if( 3 != argc ) {
        NewtEDID_GetIndexedDisplayDevice(-1, -1, &ddMon, 0 /* getDeviceInterfaceName */, 1 /* verbose */);
        _ftprintf(stderr, __T("Usage: %s dev-idx mon-idx\n"), argv[0]);
        return 1;
    }
    int useDevIdx = _tstoi(argv[1]);
    int useMonIdx = _tstoi(argv[2]);
    int widthMm, heightMm;
    int widthCm, heightCm;

    //
    // Proper method
    //
    if( 0 == NewtEDID_GetIndexedDisplayDevice(useDevIdx, useMonIdx, &ddMon, 1 /* getDeviceInterfaceName */, 0 /* verbose */) ) {
        _ftprintf(stderr, __T("No monitor found at dev %d : mon %d\n"), useDevIdx, useMonIdx);
        return 1;
    }
    _ftprintf(stderr, __T("Found monitor at dev %d : mon %d:\n"), useDevIdx, useMonIdx);
    _ftprintf(stderr, __T("   Device Name  : %s\n"), ddMon.DeviceName);
    _ftprintf(stderr, __T("   Device String: %s\n"), ddMon.DeviceString);
    _ftprintf(stderr, __T("   Device ID    : %s\n"), ddMon.DeviceID);
    fflush(NULL);

    if( NewtEDID_GetMonitorSizeFromEDIDByDevice(&ddMon, &widthMm, &heightMm, &widthCm, &heightCm) ) {
        _ftprintf(stderr, __T("Proper: Found EDID size [%d, %d] [mm], [%d, %d] [cm]\n"), widthMm, heightMm, widthCm, heightCm);
    }

    //
    // Monitor model name method
    //
    if( 0 == NewtEDID_GetIndexedDisplayDevice(useDevIdx, useMonIdx, &ddMon, 0 /* getDeviceInterfaceName */, 0 /* verbose */) ) {
        _ftprintf(stderr, __T("No monitor found at dev %d : mon %d\n"), useDevIdx, useMonIdx);
        return 1;
    }
    _ftprintf(stderr, __T("Found monitor at dev %d : mon %d:\n"), useDevIdx, useMonIdx);
    _ftprintf(stderr, __T("   Device Name  : %s\n"), ddMon.DeviceName);
    _ftprintf(stderr, __T("   Device String: %s\n"), ddMon.DeviceString);
    _ftprintf(stderr, __T("   Device ID    : %s\n"), ddMon.DeviceID);
    fflush(NULL);

    if( NewtEDID_GetMonitorSizeFromEDIDByModelName(&ddMon, &widthMm, &heightMm, &widthCm, &heightCm) ) {
        _ftprintf(stderr, __T("ModelN: Found EDID size [%d, %d] [mm], [%d, %d] [cm]\n"), widthMm, heightMm, widthCm, heightCm);
    }
    return 0;
}

#endif /*  WINDOWS_EDID_WITH_MAIN */
