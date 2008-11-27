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

#include "com_sun_javafx_media_video_openmax_OMXMoviePlayerImpl.h"
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

static JNIEnv *_env = NULL;
void java_throwNewRuntimeException(JNIEnv *env, const char* format, ...)
{
    va_list ap;
    char buffer[255];
    va_start(ap, format);
    if(env!=NULL) {
        _env=env;
    }
    #ifdef _WIN32
        _vsnprintf(buffer, sizeof(buffer)-1, format, ap);
    #else
        vsnprintf(buffer, sizeof(buffer)-1, format, ap);
    #endif
    va_end(ap);
    buffer[sizeof(buffer)-1]=0;
    fprintf(stderr, "RuntimeException: %s\n", buffer); fflush(stderr);
    if(_env!=NULL) {
        (*env)->ThrowNew(_env, runtimeExceptionClz, buffer);
    }
}

JNIEXPORT jlong JNICALL Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1createInstance
  (JNIEnv *env, jobject instance, jint vBufferNum)
{
    OMXToolBasicAV_t * pOMXAV;

    _initStatics(env);
    _env = env;

    pOMXAV = OMXToolBasicAV_CreateInstance(vBufferNum);

    return (jlong)((intptr_t)((void *)pOMXAV));
}

JNIEXPORT void JNICALL Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1setStream
  (JNIEnv *env, jobject instance, jlong ptr, jstring jpath)
{
    jboolean iscopy;
    OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));

    _env = env;

    fprintf(stdout, "setStream 1 ..\n"); fflush(stdout); // JAU
    if (pOMXAV != NULL) {
        const char *filePath = (*env)->GetStringUTFChars(env, jpath, &iscopy);
        fprintf(stdout, "setStream 2 %s..\n", filePath); fflush(stdout); // JAU
        OMXToolBasicAV_SetStream(pOMXAV, filePath);
        (*env)->ReleaseStringChars(env, jpath, (const jchar *)filePath);
    }
    fprintf(stdout, "setStream 3 ..\n"); fflush(stdout); // JAU
}

JNIEXPORT void JNICALL Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1updateStreamInfo
  (JNIEnv *env, jobject instance, jlong ptr)
{
    OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));

    _env = env;

    if (pOMXAV != NULL) {
        OMXToolBasicAV_UpdateStreamInfo(pOMXAV);
    }
}

JNIEXPORT void JNICALL Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1setEGLImageTexture2D
  (JNIEnv *env, jobject instance, jlong ptr, jint i, jint tex, jlong image, jlong sync)
{
  OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
  _env = env;
  if (pOMXAV != NULL) {
    OMXToolBasicAV_SetEGLImageTexture2D( pOMXAV, i, (GLuint) tex, 
                                         (EGLImageKHR)(intptr_t)image,
                                         (EGLSyncKHR)(intptr_t)sync);
  }
}

JNIEXPORT void JNICALL Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1activateInstance
  (JNIEnv *env, jobject instance, jlong ptr)
{
    OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));

    _env = env;

    if (pOMXAV != NULL) {
        OMXToolBasicAV_ActivateInstance(pOMXAV);
    }
}

/*
 * Class:     com_sun_javafx_media_video_openmax_OMXMovieImpl
 * Method:    _play
 * Signature: (ILjava/nio/ByteBuffer;JFI)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1play
  (JNIEnv *env, jobject instance, jlong ptr, jlong position, jfloat rate, jint loopCount)
{
//fprintf(stdout, "Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1play\n");
  OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
  _env = env;
  if (pOMXAV != NULL) {
      if(OMXToolBasicAV_PlayStart(pOMXAV)) {
        java_throwNewRuntimeException(env, "Couldn't start play %p p:%ld r:%f l%d",
            pOMXAV, position, rate, loopCount);
      }
  }
}

/*
 * Class:     com_sun_javafx_media_video_openmax_OMXMovieImpl
 * Method:    _stop
 * Signature: (I)I
 */
JNIEXPORT jlong JNICALL Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1stop
  (JNIEnv *env, jobject instance, jlong ptr)
{
    jlong frame = 0;
    _env = env;
    return frame;
}

/*
 * Class:     com_sun_javafx_media_video_openmax_OMXMovieImpl
 * Method:    _setVolume
 * Signature: (IF)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1setVolume
  (JNIEnv *env, jobject instance, jlong ptr, jfloat volume)
{
    _env = env;
}

/*
 * Class:     com_sun_javafx_media_video_openmax_OMXMovieImpl
 * Method:    _getCurrentPosition
 * Signature: (I)I
 */
JNIEXPORT jlong JNICALL Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1getCurrentPosition
  (JNIEnv *env, jobject instance, jlong ptr)
{
    jlong frame = 0;
    _env = env;
    return frame;
}

/*
 * Class:     com_sun_javafx_media_video_openmax_OMXMovieImpl
 * Method:    _getCurrentPosition
 * Signature: (I)I
 */
JNIEXPORT jlong JNICALL Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1getCurrentLoaded
  (JNIEnv *env, jobject instance, jlong ptr)
{
    jlong frame = 0;
    _env = env;
    return frame;
}

JNIEXPORT jlong JNICALL Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1getDuration
  (JNIEnv *env, jobject instance, jlong ptr)
{
    jlong frame = 0;
    _env = env;
    return frame;
}
/*
 * Class:     com_sun_javafx_media_video_openmax_OMXMovieImpl
 * Method:    _step
 * Signature: (IIJ)V
 */
JNIEXPORT jlong JNICALL Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1step
  (JNIEnv *env, jobject instance, jlong ptr, jint direction, jlong position)
{
//fprintf(stdout, "Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1step\n");
    jlong frame = position;
    _env = env;
    return frame;
}

/*
 * Class:     com_sun_javafx_media_video_openmax_OMXMovieImpl
 * Method:    _setRate
 * Signature: (IF)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1setRate
  (JNIEnv *env, jobject instance, jlong ptr, jfloat rate)
{
//fprintf(stdout, "Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1setRate\n");
    _env = env;
}

/*
 * Class:     com_sun_javafx_media_video_openmax_OMXMovieImpl
 * Method:    _setRate
 * Signature: (IF)V
 */
JNIEXPORT jlong JNICALL Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1setCurrentPosition
  (JNIEnv *env, jobject obj, jlong ptr, jlong position)
{
//fprintf(stdout, "Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1setRate\n");
    _env = env;
    return position;
}

/*
 * Class:     com_sun_javafx_media_video_openmax_OMXMovieImpl
 * Method:    _lock
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1lock
  (JNIEnv *env, jobject instance, jlong ptr)
{
//fprintf(stdout, "Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1lock\n");
    _env = env;
}

/*
 * Class:     com_sun_javafx_media_video_openmax_OMXMovieImpl
 * Method:    _unlock
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1unlock
  (JNIEnv *env, jobject instance, jlong ptr)
{
//fprintf(stdout, "Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1unlock\n");
    _env = env;
}

/*
 * Class:     com_sun_javafx_media_video_openmax_OMXMovieImpl
 * Method:    _getTextureID
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1getTextureID
  (JNIEnv *env, jobject instance, jlong ptr)
{
//fprintf(stdout, "Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1getTextureID\n");
  jint textureID = 0xffffffff;
  OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
  _env = env;
  if (pOMXAV != NULL) {
      textureID = OMXToolBasicAV_GetTexture(pOMXAV);
  }
  return textureID;
}

/*
 * Class:     com_sun_javafx_media_video_openmax_OMXMovieImpl
 * Method:    _getWidth
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1getWidth
  (JNIEnv *env, jobject instance, jlong ptr)
{
//fprintf(stdout, "Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1getWidth\n");
  jint width = 0;
    
  OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
  _env = env;
  if (pOMXAV != NULL) {
      width = pOMXAV->width;
  }
  return width;
}

/*
 * Class:     com_sun_javafx_media_video_openmax_OMXMovieImpl
 * Method:    _getHeight
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1getHeight
  (JNIEnv *env, jobject instance, jlong ptr)
{
//fprintf(stdout, "Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1getHeight\n");
  jint height = 0;
    
  OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
  _env = env;
  if (pOMXAV != NULL) {
      height = pOMXAV->height;
  }
  return height;
}

/*
 * Class:     com_sun_javafx_media_video_openmax_OMXMovieImpl
 * Method:    _task
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1task
  (JNIEnv *env, jobject instance, jlong ptr)
{
  _env = env;
}

/*
 * Class:     com_sun_javafx_media_video_openmax_OMXMovieImpl
 * Method:    _destroy
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1destroyInstance
  (JNIEnv *env, jobject instance, jlong ptr)
{
//fprintf(stdout, "Java_com_sun_javafx_media_video_openmax_OMXMovieImpl__1destroy\n");
  OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *)((void *)((intptr_t)ptr));
  _env = env;
  if (pOMXAV != NULL) {
    OMXToolBasicAV_DestroyInstance(pOMXAV);
  }
}


