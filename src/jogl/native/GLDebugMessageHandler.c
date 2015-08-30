
#include <stdio.h> /* android */
#include <stdlib.h>
#include <stdarg.h>

#include "jogamp_opengl_GLDebugMessageHandler.h"
#include "JoglCommon.h"

#include <GL/gl.h>
#include <GL/glext.h>

// #define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(...) fprintf(stderr, __VA_ARGS__); fflush(stderr) 
#else
    #define DBG_PRINT(...)
#endif

// Note: 'ARB' is also used for 'KHR'!

static jmethodID glDebugMessageARB = NULL; // int source, int type, int id, int severity, String msg
static jmethodID glDebugMessageAMD = NULL; // int id, int category, int severity, String msg

typedef void (APIENTRY* _local_GLDEBUGPROCARB)(GLenum source,GLenum type,GLuint id,GLenum severity,GLsizei length,const GLchar *message,const void *userParam);
typedef void (APIENTRY* _local_PFNGLDEBUGMESSAGECALLBACKARBPROC) (_local_GLDEBUGPROCARB callback, const void *userParam);

typedef void (APIENTRY* _local_GLDEBUGPROCAMD)(GLuint id,GLenum category,GLenum severity,GLsizei length,const GLchar *message,void *userParam);
typedef void (APIENTRY* _local_PFNGLDEBUGMESSAGECALLBACKAMDPROC) (_local_GLDEBUGPROCAMD callback, void *userParam);

/*
 * Class:     jogamp_opengl_GLDebugMessageHandler
 * Method:    initIDs0
 * Signature: (V)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_opengl_GLDebugMessageHandler_initIDs0
  (JNIEnv *env, jclass clazz)
{
    jboolean res;
    JoglCommon_init(env);

    glDebugMessageARB = (*env)->GetMethodID(env, clazz, "glDebugMessageARB", "(IIIILjava/lang/String;)V");
    glDebugMessageAMD = (*env)->GetMethodID(env, clazz, "glDebugMessageAMD", "(IIILjava/lang/String;)V");

    res = ( NULL != glDebugMessageARB && NULL != glDebugMessageAMD ) ? JNI_TRUE : JNI_FALSE ;

    DBG_PRINT("GLDebugMessageHandler.initIDS0: OK: %d, ARB %p, AMD %p\n", res, glDebugMessageARB, glDebugMessageAMD);

    return res;
}

typedef struct {
    jobject obj;
    int extType;
} DebugHandlerType;
    

// GLDEBUGARB(GLenum source,GLenum type,GLuint id,GLenum severity,GLsizei length,const GLchar *message,GLvoid *userParam);
static void GLDebugMessageARBCallback(GLenum source, GLenum type, GLuint id, GLenum severity, 
                                      GLsizei length, const GLchar *message, GLvoid *userParam) {
    DebugHandlerType * handle = (DebugHandlerType*) (intptr_t) userParam;
    jobject obj = handle->obj;
    JNIEnv *env = NULL;
    int shallBeDetached ;
    DBG_PRINT("GLDebugMessageARBCallback: 00 - %s, jobject %p, extType %d\n", message, (void*)handle->obj, handle->extType);

    env = JoglCommon_GetJNIEnv (1 /* asDaemon */, &shallBeDetached);
    if( NULL == env ) {
        DBG_PRINT("GLDebugMessageARBCallback: Null JNIEnv\n");
        return;
    }
    (*env)->CallVoidMethod(env, obj, glDebugMessageARB, 
                              (jint) source, (jint) type, (jint) id, (jint) severity, 
                              (*env)->NewStringUTF(env, message));
    // detaching thread not required - daemon
    // JoglCommon_ReleaseJNIEnv(shallBeDetached);
    DBG_PRINT("GLDebugMessageARBCallback: 0X\n");
    /**
     * On Java 32bit on 64bit Windows and w/ GL_DEBUG_OUTPUT_SYNCHRONOUS_ARB disables,
     * the unit test com.jogamp.opengl.test.junit.jogl.acore.TestGLDebug00NEWT crashes after this point.
     */
}

// GLDEBUGAMD(GLuint id,GLenum category,GLenum severity,GLsizei length,const GLchar *message,GLvoid *userParam);
static void GLDebugMessageAMDCallback(GLuint id, GLenum category, GLenum severity, 
                                      GLsizei length, const GLchar *message, GLvoid *userParam) {
    DebugHandlerType * handle = (DebugHandlerType*) (intptr_t) userParam;
    jobject obj = handle->obj;
    JNIEnv *env = NULL;
    int shallBeDetached ;
    DBG_PRINT("GLDebugMessageAMDCallback: 00 - %s, jobject %p, extType %d\n", message, (void*)handle->obj, handle->extType);

    env = JoglCommon_GetJNIEnv (1 /* asDaemon */, &shallBeDetached);
    if( NULL == env ) {
        DBG_PRINT("GLDebugMessageARBCallback: Null JNIEnv\n");
        return;
    }
    (*env)->CallVoidMethod(env, obj, glDebugMessageAMD, 
                              (jint) id, (jint) category, (jint) severity, 
                              (*env)->NewStringUTF(env, message));
    // detached attached thread not required - daemon
    // JoglCommon_ReleaseJNIEnv(shallBeDetached);
    DBG_PRINT("GLDebugMessageAMDCallback: 0X\n");
    /**
     * On Java 32bit on 64bit Windows,
     * the unit test com.jogamp.opengl.test.junit.jogl.acore.TestGLDebug00NEWT crashes after this point.
     */
}


/*
 * Class:     jogamp_opengl_GLDebugMessageHandler
 * Method:    register0
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_opengl_GLDebugMessageHandler_register0
  (JNIEnv *env, jobject obj, jlong procAddress, jint extType)
{
    DebugHandlerType * handle = malloc(sizeof(DebugHandlerType));
    handle->obj = (*env)->NewGlobalRef(env, obj);
    handle->extType = extType;
    DBG_PRINT("GLDebugMessageHandler.register0: jobject %p, extType %d\n", (void*)handle->obj, handle->extType);

    if(jogamp_opengl_GLDebugMessageHandler_EXT_KHR == extType ||
       jogamp_opengl_GLDebugMessageHandler_EXT_ARB == extType) {
        _local_PFNGLDEBUGMESSAGECALLBACKARBPROC ptr_glDebugMessageCallbackARB;
        ptr_glDebugMessageCallbackARB = (_local_PFNGLDEBUGMESSAGECALLBACKARBPROC) (intptr_t) procAddress;
        ptr_glDebugMessageCallbackARB((_local_GLDEBUGPROCARB)GLDebugMessageARBCallback, handle);
    } else if(jogamp_opengl_GLDebugMessageHandler_EXT_AMD == extType) {
        _local_PFNGLDEBUGMESSAGECALLBACKAMDPROC ptr_glDebugMessageCallbackAMD;
        ptr_glDebugMessageCallbackAMD = (_local_PFNGLDEBUGMESSAGECALLBACKAMDPROC) (intptr_t) procAddress;
        ptr_glDebugMessageCallbackAMD((_local_GLDEBUGPROCAMD)GLDebugMessageAMDCallback, handle);
    } else {
        JoglCommon_throwNewRuntimeException(env, "unsupported extension type %d", extType);
    }

    return (jlong) (intptr_t) handle;
}

/*
 * Class:     jogamp_opengl_GLDebugMessageHandler
 * Method:    unregister0
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_jogamp_opengl_GLDebugMessageHandler_unregister0
  (JNIEnv *env, jobject obj, jlong procAddress, jlong jhandle)
{
    DebugHandlerType * handle = (DebugHandlerType*) (intptr_t) jhandle;

    DBG_PRINT("GLDebugMessageHandler.unregister0: jobject %p, extType %d\n", (void*)handle->obj, handle->extType);

    if(JNI_FALSE == (*env)->IsSameObject(env, obj, handle->obj)) {
        JoglCommon_throwNewRuntimeException(env, "wrong handle (obj doesn't match)");
    }

    if(jogamp_opengl_GLDebugMessageHandler_EXT_KHR == handle->extType ||
       jogamp_opengl_GLDebugMessageHandler_EXT_ARB == handle->extType) {
        _local_PFNGLDEBUGMESSAGECALLBACKARBPROC ptr_glDebugMessageCallbackARB;
        ptr_glDebugMessageCallbackARB = (_local_PFNGLDEBUGMESSAGECALLBACKARBPROC) (intptr_t) procAddress;
        ptr_glDebugMessageCallbackARB((_local_GLDEBUGPROCARB)NULL, NULL);
    } else if(jogamp_opengl_GLDebugMessageHandler_EXT_AMD == handle->extType) {
        _local_PFNGLDEBUGMESSAGECALLBACKAMDPROC ptr_glDebugMessageCallbackAMD;
        ptr_glDebugMessageCallbackAMD = (_local_PFNGLDEBUGMESSAGECALLBACKAMDPROC) (intptr_t) procAddress;
        ptr_glDebugMessageCallbackAMD((_local_GLDEBUGPROCAMD)NULL, NULL);
    } else {
        JoglCommon_throwNewRuntimeException(env, "unsupported extension type %d", handle->extType);
    }

    (*env)->DeleteGlobalRef(env, handle->obj);
    free(handle);
}

