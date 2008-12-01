/*
 *  javafx_media_video_Movie.c
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

#include "com_sun_openmax_OMXInstance.h"
#include "omx_tool.h"
#include <stdarg.h>

static const char * const ClazzNameRuntimeException =
                            "java/lang/RuntimeException";
static jclass    runtimeExceptionClz=NULL;
#ifdef _WIN32_WCE
    #define STDOUT_FILE "\\Storage Card\\javafx_demos\\stdout.txt"
    #define STDERR_FILE "\\Storage Card\\javafx_demos\\stderr.txt"
#endif

static void _initStatics(JNIEnv *env)
{
    jclass c;
#ifdef _WIN32_WCE
    _wfreopen(TEXT(STDOUT_FILE),L"w",stdout);
    _wfreopen(TEXT(STDERR_FILE),L"w",stderr);
#endif
    fprintf(stdout, "_initstatics ..\n"); fflush(stdout); // JAU
    if (runtimeExceptionClz != NULL) {
        return;
    }

    c = (*env)->FindClass(env, ClazzNameRuntimeException);
    if(NULL==c) {
        fprintf(stdout, "FatalError: can't find %s\n", ClazzNameRuntimeException);
        (*env)->FatalError(env, ClazzNameRuntimeException);
    }
    runtimeExceptionClz = (jclass)(*env)->NewGlobalRef(env, c);
    if(NULL==runtimeExceptionClz) {
        fprintf(stdout, "FatalError: can't use %s\n", ClazzNameRuntimeException);
        (*env)->FatalError(env, ClazzNameRuntimeException);
    }
}

void java_throwNewRuntimeException(intptr_t jni_env, const char* format, ...)
{
    va_list ap;
    char buffer[255];
    va_start(ap, format);
    #ifdef _WIN32
        _vsnprintf(buffer, sizeof(buffer)-1, format, ap);
    #else
        vsnprintf(buffer, sizeof(buffer)-1, format, ap);
    #endif
    va_end(ap);
    buffer[sizeof(buffer)-1]=0;
    fprintf(stderr, "RuntimeException: %s\n", buffer); fflush(stderr);
    if(jni_env!=0) {
        (*((JNIEnv *)jni_env))->ThrowNew((JNIEnv *)jni_env, runtimeExceptionClz, buffer);
    }
}

void OMXInstance_SaveJavaAttributes(OMXToolBasicAV_t *pOMXAV, KDboolean issueJavaCallback)
{
    if(NULL==pOMXAV || 0==pOMXAV->jni_env || 0==pOMXAV->jni_instance) {
        fprintf(stderr, "OMXInstance_SaveJavaAttributes failed");
        return;
    } else if(issueJavaCallback==KD_TRUE) {
        JNIEnv  * env = (JNIEnv *)pOMXAV->jni_env;
        jobject instance = (jobject)pOMXAV->jni_instance;
        (*env)->CallVoidMethod(env, instance, (jmethodID)pOMXAV->jni_mid_saveAttributes);
    }
}

void OMXInstance_UpdateJavaAttributes(OMXToolBasicAV_t *pOMXAV, KDboolean issueJavaCallback)
{
    if(NULL==pOMXAV || 0==pOMXAV->jni_env || 0==pOMXAV->jni_instance) {
        fprintf(stderr, "OMXInstance_UpdateJavaAttributes failed");
        return;
    } else {
        JNIEnv  * env = (JNIEnv *)pOMXAV->jni_env;
        jobject instance = (jobject)pOMXAV->jni_instance;
        (*env)->SetIntField(env, instance, (jfieldID)pOMXAV->jni_fid_width, (jint)pOMXAV->width);
        (*env)->SetIntField(env, instance, (jfieldID)pOMXAV->jni_fid_height, (jint)pOMXAV->height);
        (*env)->SetIntField(env, instance, (jfieldID)pOMXAV->jni_fid_fps, (jint)pOMXAV->framerate);
        (*env)->SetLongField(env, instance, (jfieldID)pOMXAV->jni_fid_bps, (jlong)pOMXAV->bitrate);
        (*env)->SetLongField(env, instance, (jfieldID)pOMXAV->jni_fid_totalFrames, (jlong)(pOMXAV->length*pOMXAV->framerate));
        if(issueJavaCallback==KD_TRUE) {
            (*env)->CallVoidMethod(env, instance, (jmethodID)pOMXAV->jni_mid_attributesUpdated);
        } else {
            if(strlen(pOMXAV->videoCodec)>0) {
                (*env)->SetObjectField(env, instance, (jfieldID)pOMXAV->jni_fid_vcodec, (*env)->NewStringUTF(env, pOMXAV->videoCodec));
            }
            if(strlen(pOMXAV->audioCodec)>0) {
                (*env)->SetObjectField(env, instance, (jfieldID)pOMXAV->jni_fid_acodec, (*env)->NewStringUTF(env, pOMXAV->audioCodec));
            }
        }
    }
}

JNIEXPORT jlong JNICALL Java_com_sun_openmax_OMXInstance__1createInstance
  (JNIEnv *env, jobject instance)
{
    OMXToolBasicAV_t * pOMXAV;

    _initStatics(env);

    pOMXAV->jni_env=(intptr_t)env;
    pOMXAV->jni_instance=(intptr_t)instance;

    pOMXAV = OMXToolBasicAV_CreateInstance((intptr_t)env, (intptr_t)instance);
    if(NULL!=pOMXAV) {
        jclass cls = (*env)->GetObjectClass(env, instance);
        pOMXAV->jni_mid_saveAttributes = (intptr_t) (*env)->GetMethodID(env, cls, "saveAttributes", "()V");
        pOMXAV->jni_mid_attributesUpdated = (intptr_t) (*env)->GetMethodID(env, cls, "attributesUpdated", "()V");
        pOMXAV->jni_fid_width = (intptr_t) (*env)->GetFieldID(env, cls, "width",  "I");
        pOMXAV->jni_fid_height = (intptr_t) (*env)->GetFieldID(env, cls, "height",  "I");
        pOMXAV->jni_fid_fps = (intptr_t) (*env)->GetFieldID(env, cls, "fps",  "I");
        pOMXAV->jni_fid_bps = (intptr_t) (*env)->GetFieldID(env, cls, "bps",  "J");
        pOMXAV->jni_fid_totalFrames = (intptr_t) (*env)->GetFieldID(env, cls, "totalFrames",  "J");
        pOMXAV->jni_fid_acodec = (intptr_t) (*env)->GetFieldID(env, cls, "acodec",  "Ljava/lang/String;");
        pOMXAV->jni_fid_vcodec = (intptr_t) (*env)->GetFieldID(env, cls, "vcodec",  "Ljava/lang/String;");
    }

    return (jlong) (intptr_t) (void *)pOMXAV;
}

JNIEXPORT void JNICALL Java_com_sun_openmax_OMXInstance__1setStream
  (JNIEnv *env, jobject instance, jlong ptr, jint vBufferNum, jstring jpath)
{
    jboolean iscopy;
    OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));

    fprintf(stdout, "setStream 1 ..\n"); fflush(stdout); // JAU
    if (pOMXAV != NULL) {
        const char *filePath = (*env)->GetStringUTFChars(env, jpath, &iscopy);
        fprintf(stdout, "setStream 2 %s..\n", filePath); fflush(stdout); // JAU
        pOMXAV->jni_env=(intptr_t)env;
        pOMXAV->jni_instance=(intptr_t)instance;
        OMXToolBasicAV_SetStream(pOMXAV, vBufferNum, filePath);
        (*env)->ReleaseStringChars(env, jpath, (const jchar *)filePath);
    }
    fprintf(stdout, "setStream 3 ..\n"); fflush(stdout); // JAU
}

JNIEXPORT void JNICALL Java_com_sun_openmax_OMXInstance__1setStreamEGLImageTexture2D
  (JNIEnv *env, jobject instance, jlong ptr, jint i, jint tex, jlong image, jlong sync)
{
  OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
  if (pOMXAV != NULL) {
    OMXToolBasicAV_SetStreamEGLImageTexture2D( pOMXAV, i, (GLuint) tex, 
                                         (EGLImageKHR)(intptr_t)image,
                                         (EGLSyncKHR)(intptr_t)sync);
  }
}

JNIEXPORT void JNICALL Java_com_sun_openmax_OMXInstance__1activateStream
  (JNIEnv *env, jobject instance, jlong ptr)
{
    OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));

    if (pOMXAV != NULL) {
        OMXToolBasicAV_ActivateStream(pOMXAV);
    }
}

JNIEXPORT void JNICALL Java_com_sun_openmax_OMXInstance__1attachVideoRenderer
  (JNIEnv *env, jobject instance, jlong ptr)
{
  OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
  OMXToolBasicAV_AttachVideoRenderer(pOMXAV);
}

JNIEXPORT void JNICALL Java_com_sun_openmax_OMXInstance__1detachVideoRenderer
  (JNIEnv *env, jobject instance, jlong ptr)
{
  OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
  OMXToolBasicAV_DetachVideoRenderer(pOMXAV);
}

JNIEXPORT void JNICALL Java_com_sun_openmax_OMXInstance__1setPlaySpeed
  (JNIEnv *env, jobject instance, jlong ptr, jfloat scale)
{
  OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
  OMXToolBasicAV_SetPlaySpeed(pOMXAV, scale);
}

JNIEXPORT jfloat JNICALL Java_com_sun_openmax_OMXInstance__1play
  (JNIEnv *env, jobject instance, jlong ptr)
{
  OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
  OMXToolBasicAV_PlayStart(pOMXAV);
  return OMXToolBasicAV_GetCurrentPosition(pOMXAV);
}

JNIEXPORT jfloat JNICALL Java_com_sun_openmax_OMXInstance__1pause
  (JNIEnv *env, jobject instance, jlong ptr)
{
  OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
  OMXToolBasicAV_PlayPause(pOMXAV);
  return OMXToolBasicAV_GetCurrentPosition(pOMXAV);
}

JNIEXPORT jfloat JNICALL Java_com_sun_openmax_OMXInstance__1stop
  (JNIEnv *env, jobject instance, jlong ptr)
{
    OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
    OMXToolBasicAV_PlayStop(pOMXAV);
    return OMXToolBasicAV_GetCurrentPosition(pOMXAV);
}

JNIEXPORT jfloat JNICALL Java_com_sun_openmax_OMXInstance__1seek
  (JNIEnv *env, jobject instance, jlong ptr, jfloat pos)
{
    OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
    OMXToolBasicAV_PlaySeek(pOMXAV, pos);
    return OMXToolBasicAV_GetCurrentPosition(pOMXAV);
}

JNIEXPORT jint JNICALL Java_com_sun_openmax_OMXInstance__1getNextTextureID
  (JNIEnv *env, jobject instance, jlong ptr)
{
  jint textureID = 0xffffffff;
  OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
  if (pOMXAV != NULL) {
      textureID = OMXToolBasicAV_GetNextTextureID(pOMXAV);
  }
  return textureID;
}

JNIEXPORT jfloat JNICALL Java_com_sun_openmax_OMXInstance__1getCurrentPosition
  (JNIEnv *env, jobject instance, jlong ptr)
{
    OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
    return OMXToolBasicAV_GetCurrentPosition(pOMXAV);
}


JNIEXPORT void JNICALL Java_com_sun_openmax_OMXInstance__1destroyInstance
  (JNIEnv *env, jobject instance, jlong ptr)
{
  OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
  if (pOMXAV != NULL) {
    OMXToolBasicAV_DestroyInstance(pOMXAV);
  }
}


