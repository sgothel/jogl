/**
 * Copyright 2019 JogAmp Community. All rights reserved.
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
 
#include "NativewindowCommon.h"

#include "jogamp_nativewindow_drm_DRMLib.h"
#include "jogamp_nativewindow_drm_DRMUtil.h"

#include <fcntl.h>

/** Remove memcpy GLIBC > 2.4 dependencies */
#include <glibc-compat-symbols.h>

// #define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(args...) fprintf(stderr, args);
#else
    #define DBG_PRINT(args...)
#endif

static int _initialized = 0;

JNIEXPORT jboolean JNICALL 
Java_jogamp_nativewindow_drm_DRMUtil_initialize0(JNIEnv *env, jclass clazz, jboolean debug) {
    if( 0 == _initialized ) {
        _initialized=1;
        if(JNI_TRUE == debug) {
            fprintf(stderr, "Info: NativeWindow native init passed\n");
        }
    }
    return JNI_TRUE;
}

JNIEXPORT void JNICALL 
Java_jogamp_nativewindow_drm_DRMUtil_shutdown0(JNIEnv *env, jclass _unused) {
    // NOP
}

/*   Java->C glue code:
 *   Java package: jogamp.nativewindow.drm.DRMLib
 *    Java method: int drmOpenFile(java.lang.String filename)
 *     C function: int drmOpenFile(const char *filename)
 */
JNIEXPORT jint JNICALL
Java_jogamp_nativewindow_drm_DRMLib_drmOpenFile(JNIEnv *env, jclass _unused, jstring filename) {
  const char* _strchars_filename = NULL;
  int _res;
  if ( NULL != filename ) {
    _strchars_filename = (*env)->GetStringUTFChars(env, filename, (jboolean*)NULL);
  if ( NULL == _strchars_filename ) {
      (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/OutOfMemoryError"),
                       "Failed to get UTF-8 chars for argument \"filename\" in native dispatcher for \"drmOpenFile\"");
      return 0;
    }
  }
  _res = (int) open((const char * ) _strchars_filename, O_RDWR);
  if ( NULL != filename ) {
    (*env)->ReleaseStringUTFChars(env, filename, _strchars_filename);
  }
  return _res;
}


