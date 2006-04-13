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
  static const char* platformDSIClassName = "com/sun/opengl/impl/windows/JAWT_Win32DrawingSurfaceInfo";
#elif defined(linux) || defined(__sun) || defined(__FreeBSD__) || defined(_HPUX)
  #define PLATFORM_DSI_SIZE sizeof(JAWT_X11DrawingSurfaceInfo)
  static const char* platformDSIClassName = "com/sun/opengl/impl/x11/JAWT_X11DrawingSurfaceInfo";
#elif defined(macosx)
  #define PLATFORM_DSI_SIZE sizeof(JAWT_MacOSXDrawingSurfaceInfo)
  static const char* platformDSIClassName = "com/sun/opengl/impl/macosx/JAWT_MacOSXDrawingSurfaceInfo";
#else
  ERROR: port JAWT_DrawingSurfaceInfo.c to your platform
#endif

static jclass    platformDSIClass = NULL;
static jmethodID factoryMethod    = NULL;

JNIEXPORT jobject JNICALL
Java_com_sun_opengl_impl_JAWT_1DrawingSurfaceInfo_platformInfo0(JNIEnv* env, jobject unused, jobject jthis0) {
  JAWT_DrawingSurfaceInfo* dsi;
  jobject dirbuf;
  jclass clazz;
  char sig[512];
  dsi = (*env)->GetDirectBufferAddress(env, jthis0);
  if (dsi == NULL) {
    (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/RuntimeException"),
                     "Argument \"jthis0\" was not a direct buffer");
    return NULL;
  }
  if (dsi->platformInfo == NULL) {
    return NULL;
  }
  dirbuf = (*env)->NewDirectByteBuffer(env, dsi->platformInfo, PLATFORM_DSI_SIZE);
  if (dirbuf == NULL) {
    return NULL;
  }
  if (platformDSIClass == NULL) {
    /* Note: possible race condition here but we will only leak a few
       global JNI handles at worst */
    clazz = (*env)->FindClass(env, platformDSIClassName);
    if (clazz == NULL) {
      return NULL;
    }
    clazz         = (jclass) (*env)->NewGlobalRef(env, clazz);
    sprintf(sig, "(Ljava/nio/ByteBuffer;)L%s;", platformDSIClassName);
    factoryMethod = (*env)->GetStaticMethodID(env, clazz, "create", sig);
    if (factoryMethod == NULL) {
      (*env)->DeleteGlobalRef(env, clazz);
      return NULL;
    }
    platformDSIClass = clazz;
  }
  return (*env)->CallStaticObjectMethod(env, platformDSIClass, factoryMethod, dirbuf);
}
