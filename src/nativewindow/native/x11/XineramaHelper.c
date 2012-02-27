/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

#include <XineramaHelper.h>

#include <stdio.h>
#include <dlfcn.h>

// #define DEBUG 1

static const char* XinExtName = "XINERAMA";

#ifdef __sun_obsolete

static const char* XineramaLibNames[] = { "libXext.so", NULL } ;
static const char* XineramaGetInfoName = "XineramaGetInfo";

typedef Status (* PFNXineramaGetInfoPROC) (Display* display, int screen_number,
         XRectangle* framebuffer_rects, unsigned char* framebuffer_hints,
         int* num_framebuffers);

#else

static const char* XineramaLibNames[]= { "libXinerama.so.1", "libXinerama.so", NULL };
static const char* XineramaIsActiveName = "XineramaIsActive";

typedef Bool (* PFNXineramaIsActivePROC) (Display *display);

#endif

static Bool XineramaIsEnabledPlatform(void *xineramaQueryFunc, Display* display) {
    Bool res = False;
    #ifdef __sun_obsolete
      #define MAXFRAMEBUFFERS 16
      unsigned char fbhints[MAXFRAMEBUFFERS];
      XRectangle fbrects[MAXFRAMEBUFFERS];
      int locNumScr = 0;

      if(NULL!=xineramaQueryFunc && NULL!=display) {
          PFNXineramaGetInfoPROC XineramaSolarisPROC = (PFNXineramaGetInfoPROC)xineramaQueryFunc;
          res = XineramaSolarisPROC(display, 0, &fbrects[0], &fbhints[0], &locNumScr) != 0;
      }
    #else
      if(NULL!=xineramaQueryFunc && NULL!=display) {
          PFNXineramaIsActivePROC XineramaIsActivePROC = (PFNXineramaIsActivePROC) xineramaQueryFunc;
          res = XineramaIsActivePROC(display);
      }
    #endif
    return res;
}

void* XineramaGetLibHandle() {
  void* xineramaLibHandle = NULL;
  int i;

  for(i=0; NULL==xineramaLibHandle && NULL!=XineramaLibNames[i]; i++) {
    xineramaLibHandle = dlopen(XineramaLibNames[i], RTLD_LAZY | RTLD_GLOBAL);
  }

  #ifdef DEBUG
    if(NULL!=xineramaLibHandle) {
      fprintf(stderr, "XineramaGetLibHandle: using lib %s -> %p\n", XineramaLibNames[i-1], xineramaLibHandle);
    } else {
      fprintf(stderr, "XineramaGetLibHandle: no native lib available\n");
    }
  #endif

  return xineramaLibHandle;
}

Bool XineramaReleaseLibHandle(void* xineramaLibHandle) {
  #ifdef DEBUG
    fprintf(stderr, "XineramaReleaseLibHandle: release lib %p\n", xineramaLibHandle);
  #endif
  if(NULL==xineramaLibHandle) {
    return False;
  }
  return 0 == dlclose(xineramaLibHandle) ? True : False;
}

void* XineramaGetQueryFunc(void *xineramaLibHandle) {
    void * funcptr = NULL;

    if(NULL==xineramaLibHandle) {
      return NULL;
    }

    #ifdef __sun_obsolete
      #ifdef DEBUG
        fprintf(stderr, "XineramaGetQueryFunc: trying func %p -> %s\n", xineramaLibHandle, XineramaGetInfoName);
      #endif
      funcptr = dlsym(xineramaLibHandle, XineramaGetInfoName);
    #else
      #ifdef DEBUG
        fprintf(stderr, "XineramaGetQueryFunc: trying func %p -> %s\n", xineramaLibHandle, XineramaIsActiveName);
      #endif
      funcptr = dlsym(xineramaLibHandle, XineramaIsActiveName);
    #endif
    #ifdef DEBUG
      fprintf(stderr, "XineramaGetQueryFunc: got func %p\n", funcptr);
    #endif
    return funcptr;
}

Bool XineramaIsEnabled(void *xineramaQueryFunc, Display* display) {
  int32_t major_opcode, first_event, first_error;
  Bool gotXinExt = False;
  Bool res = False;

  if(NULL==xineramaQueryFunc || NULL==display) {
    return False;
  }

  gotXinExt = XQueryExtension(display, XinExtName, &major_opcode,
                              &first_event, &first_error);

  #ifdef DEBUG
    fprintf(stderr, "XineramaIsEnabled: has Xinerama Ext: ext %d, query-func %p\n", gotXinExt, xineramaQueryFunc);
  #endif

  if(gotXinExt) {
    res = XineramaIsEnabledPlatform(xineramaQueryFunc, display);
  }

  return res;
}

