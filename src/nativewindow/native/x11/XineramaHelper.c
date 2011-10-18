/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */

/* This file contains a helper routine to be called by Java code to
   determine whether the Xinerama extension is in use and therefore to
   treat the multiple AWT screens as one large screen. */

#include <gluegen_stdint.h>
#include <X11/Xlib.h>
#include <stdio.h>

#ifdef __sun_obsolete

typedef Status XineramaGetInfoFunc(Display* display, int screen_number,
         XRectangle* framebuffer_rects, unsigned char* framebuffer_hints,
         int* num_framebuffers);
typedef Status XineramaGetCenterHintFunc(Display* display, int screen_number,
                                         int* x, int* y);

XineramaGetCenterHintFunc* XineramaSolarisCenterFunc = NULL;
#include <dlfcn.h>

#else

#include <X11/extensions/Xinerama.h>

#endif

Bool XineramaEnabled(Display* display) {
#ifdef __sun_obsolete

#define MAXFRAMEBUFFERS 16
  char* XinExtName = "XINERAMA";
  int32_t major_opcode, first_event, first_error;
  Bool gotXinExt = False;
  void* libHandle = 0;
  unsigned char fbhints[MAXFRAMEBUFFERS];
  XRectangle fbrects[MAXFRAMEBUFFERS];
  int locNumScr = 0;
  Bool usingXinerama = False;

  char* XineramaLibName= "libXext.so";
  char* XineramaGetInfoName = "XineramaGetInfo";
  char* XineramaGetCenterHintName = "XineramaGetCenterHint";
  XineramaGetInfoFunc* XineramaSolarisFunc = NULL;

  gotXinExt = XQueryExtension(display, XinExtName, &major_opcode,
                              &first_event, &first_error);

  if (gotXinExt) {
    /* load library, load and run XineramaGetInfo */
    libHandle = dlopen(XineramaLibName, RTLD_LAZY | RTLD_GLOBAL);
    if (libHandle != 0) {
      XineramaSolarisFunc = (XineramaGetInfoFunc*)dlsym(libHandle, XineramaGetInfoName);
      XineramaSolarisCenterFunc =
        (XineramaGetCenterHintFunc*)dlsym(libHandle,
                                          XineramaGetCenterHintName);
      if (XineramaSolarisFunc != NULL) {
        if ((*XineramaSolarisFunc)(display, 0, &fbrects[0],
                                   &fbhints[0], &locNumScr) != 0) {

          usingXinerama = True;
        }
      }
      dlclose(libHandle);
    }
  }
  return usingXinerama;
  
#else

  static const char* XinExtName = "XINERAMA";
  int32_t major_opcode, first_event, first_error;
  Bool gotXinExt = False;
  Bool isXinActive = False;

  // fprintf(stderr, "XineramaEnabled: p0\n"); fflush(stderr);

  gotXinExt = XQueryExtension(display, XinExtName, &major_opcode,
                              &first_event, &first_error);
  // fprintf(stderr, "XineramaEnabled: p1 gotXinExt %d\n",gotXinExt); fflush(stderr);

  if (gotXinExt) {
    isXinActive = XineramaIsActive(display);
  }
  // fprintf(stderr, "XineramaEnabled: p2 XineramaIsActive %d\n", isXinActive); fflush(stderr);

  return isXinActive;

#endif
}

