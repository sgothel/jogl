/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
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
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

#include <jawt_md.h>

#ifdef WIN32
  #define PLATFORM_DSI_SIZE sizeof(JAWT_Win32DrawingSurfaceInfo)
#elif defined(linux) || defined(__sun) || defined(__FreeBSD__) || defined(_HPUX)
  #define PLATFORM_DSI_SIZE sizeof(JAWT_X11DrawingSurfaceInfo)
#elif defined(macosx)
  #define PLATFORM_DSI_SIZE sizeof(JAWT_MacOSXDrawingSurfaceInfo)
#else
  ERROR: port JAWT_DrawingSurfaceInfo.c to your platform
#endif

JNIEXPORT jobject JNICALL
Java_jogamp_nativewindow_jawt_JAWT_1DrawingSurfaceInfo_platformInfo0(JNIEnv* env, jobject unused, jobject jthis0) {
  JAWT_DrawingSurfaceInfo* dsi;
  dsi = (*env)->GetDirectBufferAddress(env, jthis0);
  if (dsi == NULL) {
    (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/RuntimeException"),
                     "Argument \"jthis0\" was not a direct buffer");
    return NULL;
  }
  if (dsi->platformInfo == NULL) {
    (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/RuntimeException"),
                     "platformInfo pointer is NULL");
    return NULL;
  }
  if(0==PLATFORM_DSI_SIZE) {
    (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/RuntimeException"),
                     "platformInfo size is 0");
    return NULL;
  }
  return (*env)->NewDirectByteBuffer(env, dsi->platformInfo, PLATFORM_DSI_SIZE);
}
