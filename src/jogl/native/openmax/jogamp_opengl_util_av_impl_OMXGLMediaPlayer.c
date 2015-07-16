/*
 *  media_video_Movie.c
 *  JFXFramework
 *
 *  Created by sun on 17/02/08.
 *  Copyright 2007 __MyCompanyName__. All rights reserved.
 *
 */

// http://developer.apple.com/technotes/tn2005/tn2140.html
// http://developer.apple.com/qa/qa2005/qa1443.html
// http://developer.apple.com/documentation/QuickTime/Conceptual/QT7UpdateGuide/Chapter03/chapter_3_section_1.html#//apple_ref/doc/c_ref/NewMovieFromProperties
// http://developer.apple.com/qa/qa2001/qa1149.html
// http://developer.apple.com/qa/qa2001/qa1262.html

#include "jogamp_opengl_util_av_impl_OMXGLMediaPlayer.h"
#include "JoglCommon.h"
#include "omx_tool.h"
#include <stdarg.h>

static const char * const ClazzNameOMXGLMediaPlayer = "jogamp/opengl/util/av/impl/OMXGLMediaPlayer";

static jclass omxGLMediaPlayerClazz = NULL;
static jmethodID jni_mid_updateAttributes = NULL;

#ifdef _WIN32_WCE
    #define STDOUT_FILE "\\Storage Card\\demos\\stdout.txt"
    #define STDERR_FILE "\\Storage Card\\demos\\stderr.txt"
#endif

JNIEXPORT jboolean JNICALL Java_jogamp_opengl_util_av_impl_OMXGLMediaPlayer_initIDs0
  (JNIEnv *env, jclass clazz)
{
#ifdef _WIN32_WCE
    _wfreopen(TEXT(STDOUT_FILE),L"w",stdout);
    _wfreopen(TEXT(STDERR_FILE),L"w",stderr);
#endif
    JoglCommon_init(env);

    jclass c;
    if (omxGLMediaPlayerClazz != NULL) {
        return JNI_FALSE;
    }

    c = (*env)->FindClass(env, ClazzNameOMXGLMediaPlayer);
    if(NULL==c) {
        JoglCommon_FatalError(env, "JOGL OMX: can't find %s", ClazzNameOMXGLMediaPlayer);
    }
    omxGLMediaPlayerClazz = (jclass)(*env)->NewGlobalRef(env, c);
    (*env)->DeleteLocalRef(env, c);
    if(NULL==omxGLMediaPlayerClazz) {
        JoglCommon_FatalError(env, "JOGL OMX: can't use %s", ClazzNameOMXGLMediaPlayer);
    }

    jni_mid_updateAttributes = (*env)->GetMethodID(env, omxGLMediaPlayerClazz, "updateAttributes", "(IIIIIFIILjava/lang/String;Ljava/lang/String;)V");

    if(jni_mid_updateAttributes == NULL) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

void OMXInstance_UpdateJavaAttributes(OMXToolBasicAV_t *pAV)
{
    if(NULL==pAV || 0==pAV->jni_instance) {
        fprintf(stderr, "OMXInstance_UpdateJavaAttributes failed");
        return;
    }
    int shallBeDetached = 0;
    JNIEnv  * env = JoglCommon_GetJNIEnv (1 /* daemon */, &shallBeDetached); 
    if(NULL!=env) {
        (*env)->CallVoidMethod(env, (jobject)pAV->jni_instance, jni_mid_updateAttributes,
                               pAV->width, pAV->height, 
                               pAV->bitrate, 0, 0, 
                               pAV->framerate, (uint32_t)(pAV->length*pAV->framerate), pAV->length,
                               (*env)->NewStringUTF(env, pAV->videoCodec),
                               (*env)->NewStringUTF(env, pAV->audioCodec) );
        // detaching thread not required - daemon
        // JoglCommon_ReleaseJNIEnv(shallBeDetached);
    }
}

JNIEXPORT jlong JNICALL Java_jogamp_opengl_util_av_impl_OMXGLMediaPlayer__1createInstance
  (JNIEnv *env, jobject instance)
{
    OMXToolBasicAV_t * pOMXAV;

    pOMXAV = OMXToolBasicAV_CreateInstance((EGLDisplay)(intptr_t)env);
    pOMXAV->jni_instance=(intptr_t)instance;
    return (jlong) (intptr_t) pOMXAV;
}

JNIEXPORT void JNICALL Java_jogamp_opengl_util_av_impl_OMXGLMediaPlayer__1setStream
  (JNIEnv *env, jobject instance, jlong ptr, jint vBufferNum, jstring jpath)
{
    jboolean iscopy;
    OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));

    fprintf(stdout, "setStream 1 ..\n"); fflush(stdout); // JAU
    if (pOMXAV != NULL) {
        const char *filePath = (*env)->GetStringUTFChars(env, jpath, &iscopy);
        fprintf(stdout, "setStream 2 %s..\n", filePath); fflush(stdout); // JAU
        pOMXAV->jni_instance=(intptr_t)instance;
        OMXToolBasicAV_SetStream(pOMXAV, vBufferNum, filePath);
        (*env)->ReleaseStringChars(env, jpath, (const jchar *)filePath);
    }
    fprintf(stdout, "setStream 3 ..\n"); fflush(stdout); // JAU
}

JNIEXPORT void JNICALL Java_jogamp_opengl_util_av_impl_OMXGLMediaPlayer__1setStreamEGLImageTexture2D
  (JNIEnv *env, jobject instance, jlong ptr, jint tex, jlong image, jlong sync)
{
  OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
  if (pOMXAV != NULL) {
    OMXToolBasicAV_SetStreamEGLImageTexture2D( pOMXAV, (GLuint) tex, 
                                         (EGLImageKHR)(intptr_t)image,
                                         (EGLSyncKHR)(intptr_t)sync);
  }
}

JNIEXPORT void JNICALL Java_jogamp_opengl_util_av_impl_OMXGLMediaPlayer__1activateStream
  (JNIEnv *env, jobject instance, jlong ptr)
{
    OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));

    if (pOMXAV != NULL) {
        OMXToolBasicAV_ActivateStream(pOMXAV);
    }
}

JNIEXPORT void JNICALL Java_jogamp_opengl_util_av_impl_OMXGLMediaPlayer__1attachVideoRenderer
  (JNIEnv *env, jobject instance, jlong ptr)
{
  OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
  OMXToolBasicAV_AttachVideoRenderer(pOMXAV);
}

JNIEXPORT void JNICALL Java_jogamp_opengl_util_av_impl_OMXGLMediaPlayer__1detachVideoRenderer
  (JNIEnv *env, jobject instance, jlong ptr)
{
  OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
  OMXToolBasicAV_DetachVideoRenderer(pOMXAV);
}

JNIEXPORT void JNICALL Java_jogamp_opengl_util_av_impl_OMXGLMediaPlayer__1setPlaySpeed
  (JNIEnv *env, jobject instance, jlong ptr, jfloat scale)
{
  OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
  OMXToolBasicAV_SetPlaySpeed(pOMXAV, scale);
}

JNIEXPORT void JNICALL Java_jogamp_opengl_util_av_impl_OMXGLMediaPlayer__1play
  (JNIEnv *env, jobject instance, jlong ptr)
{
  OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
  OMXToolBasicAV_PlayStart(pOMXAV);
}

JNIEXPORT void JNICALL Java_jogamp_opengl_util_av_impl_OMXGLMediaPlayer__1pause
  (JNIEnv *env, jobject instance, jlong ptr)
{
  OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
  OMXToolBasicAV_PlayPause(pOMXAV);
}

JNIEXPORT void JNICALL Java_jogamp_opengl_util_av_impl_OMXGLMediaPlayer__1stop
  (JNIEnv *env, jobject instance, jlong ptr)
{
    OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
    OMXToolBasicAV_PlayStop(pOMXAV);
}

JNIEXPORT jint JNICALL Java_jogamp_opengl_util_av_impl_OMXGLMediaPlayer__1seek
  (JNIEnv *env, jobject instance, jlong ptr, jint pos)
{
    OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
    OMXToolBasicAV_PlaySeek(pOMXAV, pos);
    return OMXToolBasicAV_GetCurrentPosition(pOMXAV);
}

JNIEXPORT jint JNICALL Java_jogamp_opengl_util_av_impl_OMXGLMediaPlayer__1getNextTextureID
  (JNIEnv *env, jobject instance, jlong ptr, jboolean blocking)
{
  jint textureID = 0xffffffff;
  OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
  if (pOMXAV != NULL) {
      textureID = OMXToolBasicAV_GetNextTextureID(pOMXAV, blocking ? 1 : 0);
  }
  return textureID;
}

JNIEXPORT jint JNICALL Java_jogamp_opengl_util_av_impl_OMXGLMediaPlayer__1getCurrentPosition
  (JNIEnv *env, jobject instance, jlong ptr)
{
    OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
    return OMXToolBasicAV_GetCurrentPosition(pOMXAV);
}


JNIEXPORT void JNICALL Java_jogamp_opengl_util_av_impl_OMXGLMediaPlayer__1destroyInstance
  (JNIEnv *env, jobject instance, jlong ptr)
{
  OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
  if (pOMXAV != NULL) {
    OMXToolBasicAV_DestroyInstance(pOMXAV);
  }
}


