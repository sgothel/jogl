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

#ifndef JOGL_COMMON_H
#define JOGL_COMMON_H 1

#include <jni.h>
#include <stdlib.h>

void JoglCommon_init(JNIEnv *env);

jchar* JoglCommon_GetNullTerminatedStringChars(JNIEnv* env, jstring str);

void JoglCommon_FatalError(JNIEnv *env, const char* msg, ...);
void JoglCommon_throwNewRuntimeException(JNIEnv *env, const char* msg, ...);
jboolean JoglCommon_ExceptionCheck0(JNIEnv *env);
jboolean JoglCommon_ExceptionCheck1_throwNewRuntimeException(JNIEnv *env, const char* msg, ...);

/**
 *
 * 1) Init static jvmHandle, jvmVersion and clazz references
 *    from an early initialization call w/ valid 'JNIEnv * env'

    JoglCommon_init(env);

 *
 * 2) Use current thread JNIEnv or attach current thread to JVM, generating new JNIEnv
 *

    int asDaemon = 0;
    int shallBeDetached = 0;
    JNIEnv* env = JoglCommon_GetJNIEnv(asDaemon, &shallBeDetached);
    if(NULL==env) {
        DBG_PRINT("drawRect: null JNIEnv\n");
        return;
    }
    
 *
 * 3) Use JNIEnv ..
 *
    .. your JNIEnv code here ..

 *
 * 4) Detach thread from JVM if required, i.e. not attached as daemon!
 *    Not recommended for recurring _daemon_ threads (performance)
 *
    JoglCommon_ReleaseJNIEnv(shallBeDetached);
 */
JNIEnv* JoglCommon_GetJNIEnv (int asDaemon, int * shallBeDetached);

void JoglCommon_ReleaseJNIEnv (int shallBeDetached);

#endif
