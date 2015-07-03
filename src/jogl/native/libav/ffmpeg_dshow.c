/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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
 */
 
#include "ffmpeg_dshow.h"

#ifdef _WIN32

#include <stdio.h>
#include <string.h>

// dshow includes strsafe.h, hence tchar.h cannot be used
// include strsafe.h here for documentation
// #include <tchar.h>
#include <strsafe.h>
#include <dshow.h>

static HRESULT EnumerateDevices(REFGUID category, IEnumMoniker **ppEnum)
{
    // Create the System Device Enumerator.
    ICreateDevEnum *pDevEnum;
    void *pv = NULL;

    HRESULT hr = CoCreateInstance(&CLSID_SystemDeviceEnum, NULL, CLSCTX_INPROC_SERVER, &IID_ICreateDevEnum, (void**)&pDevEnum);

    if (SUCCEEDED(hr)) {
        // Create an enumerator for the category.
        hr = pDevEnum->lpVtbl->CreateClassEnumerator(pDevEnum, category, ppEnum,0);
        if (hr == S_FALSE)
        {
            hr = VFW_E_NOT_FOUND;  // The category is empty. Treat as an error.
        }
        pDevEnum->lpVtbl->Release(pDevEnum);
    }
    return hr;
}

static void getBSTRChars(BSTR bstr, char *pDest, int destLen) {

 #ifdef UNICODE
    // _sntprintf(pDest, destLen, _T("%s"), bstr);
    StringCbPrintfW(pDest, destLen, L"%s", bstr);
 #else
    // _sntprintf(pDest, destLen, _T("%S"), bstr);
    StringCchPrintfA(pDest, destLen, "%S", bstr);
 #endif 
}


static int GetDeviceInformation(IEnumMoniker *pEnum, int verbose, int devIdx,
                                char *pDescr, int descrSize,  
                                char *pName, int nameSize, 
                                char *pPath, int pathSize, int *pWaveID) {
    IMoniker *pMoniker = NULL;
    int i=0;
    int res = devIdx >= 0 ? -1 : 0;

    if( NULL != pDescr ) {
        *pDescr=0;
    }
    if( NULL != pName ) {
        *pName=0;
    }
    if( NULL != pPath ) {
        *pPath=0;
    }
    if( NULL != pWaveID ) {
        *pWaveID=0;
    }

    while (pEnum->lpVtbl->Next(pEnum, 1, &pMoniker, NULL) == S_OK) {
        IPropertyBag *pPropBag;
        HRESULT hr;
        
        hr = pMoniker->lpVtbl->BindToStorage(pMoniker, 0, 0, &IID_IPropertyBag, (void**)&pPropBag);
        if (FAILED(hr)) {
            if( verbose ) {
                fprintf(stderr, "DShowParser: Dev[%d]: bind failed ...\n", i);
            }
            pMoniker->lpVtbl->Release(pMoniker);
            continue;  
        } 
        VARIANT var;
        VariantInit(&var);

        // Get description or friendly name.
        hr = pPropBag->lpVtbl->Read(pPropBag, L"Description", &var, 0);
        if (SUCCEEDED(hr)) {
            if( i == devIdx && NULL != pDescr ) {
                res = 0;
                getBSTRChars(var.bstrVal, pDescr, descrSize);
            }
            if( verbose ) {
                fprintf(stderr, "DShowParser: Dev[%d]: Descr %S\n", i, var.bstrVal);
            }
            VariantClear(&var); 
        } else if( verbose ) {
            fprintf(stderr, "DShowParser: Dev[%d]: cannot read Descr..\n", i);
        }

        hr = pPropBag->lpVtbl->Read(pPropBag, L"FriendlyName", &var, 0);
        if (SUCCEEDED(hr)) {
            if( i == devIdx && NULL != pName ) {
                res = 0;
                getBSTRChars(var.bstrVal, pName, nameSize);
            }
            if( verbose ) {
                fprintf(stderr, "DShowParser: Dev[%d]: Name %S\n", i, var.bstrVal);
            }
            VariantClear(&var); 
        } else if( verbose ) {
            fprintf(stderr, "DShowParser: Dev[%d]: cannot read Name..\n", i);
        }

        hr = pPropBag->lpVtbl->Write(pPropBag, L"FriendlyName", &var);

        // WaveInID applies only to audio capture devices.
        hr = pPropBag->lpVtbl->Read(pPropBag, L"WaveInID", &var, 0);
        if (SUCCEEDED(hr)) {
            if( i == devIdx && NULL != pWaveID ) {
                res = 0;
                *pWaveID=(int)var.lVal;
            }
            if( verbose ) {
                fprintf(stderr, "DShowParser: Dev[%d]: WaveInID %d\n", i, var.lVal);
            }
            VariantClear(&var); 
        }

        hr = pPropBag->lpVtbl->Read(pPropBag, L"DevicePath", &var, 0);
        if (SUCCEEDED(hr)) {
            if( i == devIdx && NULL != pPath ) {
                res = 0;
                getBSTRChars(var.bstrVal, pPath, pathSize);
            }
            if( verbose ) {
                fprintf(stderr, "DShowParser: Dev[%d]: Path %S\n", i, var.bstrVal);
            }
            VariantClear(&var); 
        }

        pPropBag->lpVtbl->Release(pPropBag);
        pMoniker->lpVtbl->Release(pMoniker);

        if( devIdx >= 0 && i == devIdx ) {
            break; // done!
        }
        i++;
    }
    return res;
}

int findDShowVideoDevice(char * dest, int destSize, int devIdx, int verbose) {
    int res = -1;

    HRESULT hr = CoInitializeEx(NULL, COINIT_MULTITHREADED);
    if (SUCCEEDED(hr)) {
        IEnumMoniker *pEnum;

        hr = EnumerateDevices(&CLSID_VideoInputDeviceCategory, &pEnum);
        if (SUCCEEDED(hr)) {
            res = GetDeviceInformation(pEnum, verbose, devIdx, NULL /* pDescr */, 0, dest, destSize, NULL /* pPath */, 0, NULL /* pWaveID */);
            pEnum->lpVtbl->Release(pEnum);
            if( verbose ) {
                fprintf(stderr, "DShowParser: Get VideoInputDevice: res %d, '%s'\n", res, dest);
            }
        } else if( verbose ) {
            fprintf(stderr, "DShowParser: Get VideoInputDevice failed\n");
        }
        /**
        hr = EnumerateDevices(&CLSID_AudioInputDeviceCategory, &pEnum);
        if (SUCCEEDED(hr)) {
            res = GetDeviceInformation(pEnum, verbose, devIdx, NULL, 0, NULL, 0, NULL, 0, NULL);
            pEnum->lpVtbl->Release(pEnum);
        } else if( verbose ) {
            fprintf(stderr, "DShowParser: Get AudioInputDevice failed\n");
        } */
        CoUninitialize();
    } else if( verbose ) {
        fprintf(stderr, "DShowParser: CoInit failed\n");
    }
    return res;
}

#else

int findDShowVideoDevice(char * dest, int destSize, int devIdx, int verbose) {
    return -1;
}

#endif
