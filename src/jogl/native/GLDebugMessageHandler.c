
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


static jmethodID glDebugMessageARB = NULL; // int source, int type, int id, int severity, String msg
static jmethodID glDebugMessageAMD = NULL; // int id, int category, int severity, String msg

typedef void (APIENTRY* _local_PFNGLDEBUGMESSAGECALLBACKARBPROC) (GLDEBUGPROCARB callback, const GLvoid *userParam);
typedef void (APIENTRY* _local_GLDEBUGPROCARB)(GLenum source,GLenum type,GLuint id,GLenum severity,GLsizei length,const GLchar *message,GLvoid *userParam);

typedef void (APIENTRY* _local_PFNGLDEBUGMESSAGECALLBACKAMDPROC) (GLDEBUGPROCAMD callback, const GLvoid *userParam);
typedef void (APIENTRY* _local_GLDEBUGPROCAMD)(GLuint id,GLenum category,GLenum severity,GLsizei length,const GLchar *message,GLvoid *userParam);

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
    JavaVM *vm;
    int version;
    jobject obj;
    int extType;
} DebugHandlerType;
    

// GLDEBUGARB(GLenum source,GLenum type,GLuint id,GLenum severity,GLsizei length,const GLchar *message,GLvoid *userParam);
static void GLDebugMessageARBCallback(GLenum source, GLenum type, GLuint id, GLenum severity, 
                                      GLsizei length, const GLchar *message, GLvoid *userParam) {
    DebugHandlerType * handle = (DebugHandlerType*) (intptr_t) userParam;
    JavaVM *vm = handle->vm;
    int version = handle->version;
    jobject obj = handle->obj;
    JNIEnv *curEnv = NULL;
    JNIEnv *newEnv = NULL;
    int envRes ;
    DBG_PRINT("GLDebugMessageARBCallback: 00 - %s, vm %p, version 0x%X, jobject %p, extType %d\n", 
        message, handle->vm, handle->version, (void*)handle->obj, handle->extType);

    // retrieve this thread's JNIEnv curEnv - or detect it's detached
    envRes = (*vm)->GetEnv(vm, (void **) &curEnv, version) ;
    DBG_PRINT("GLDebugMessageARBCallback: 01 - JVM Env: curEnv %p, res 0x%X\n", curEnv, envRes);
    if( JNI_EDETACHED == envRes ) {
        // detached thread - attach to JVM
        if( JNI_OK != ( envRes = (*vm)->AttachCurrentThread(vm, (void**) &newEnv, NULL) ) ) {
            fprintf(stderr, "GLDebugMessageARBCallback: can't attach thread: %d\n", envRes);
            return;
        }
        curEnv = newEnv;
        DBG_PRINT("GLDebugMessageARBCallback: 02 - attached .. \n");
    } else if( JNI_OK != envRes ) {
        // oops ..
        fprintf(stderr, "GLDebugMessageARBCallback: can't GetEnv: %d\n", envRes);
        return;
    }
    (*curEnv)->CallVoidMethod(curEnv, obj, glDebugMessageARB, 
                              (jint) source, (jint) type, (jint) id, (jint) severity, 
                              (*curEnv)->NewStringUTF(curEnv, message));
    if( NULL != newEnv ) {
        // detached attached thread
        (*vm)->DetachCurrentThread(vm);
        DBG_PRINT("GLDebugMessageARBCallback: 04 - detached .. \n");
    }
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
    JavaVM *vm = handle->vm;
    int version = handle->version;
    jobject obj = handle->obj;
    JNIEnv *curEnv = NULL;
    JNIEnv *newEnv = NULL;
    int envRes ;
    DBG_PRINT("GLDebugMessageAMDCallback: 00 - %s, vm %p, version 0x%X, jobject %p, extType %d\n", 
        message, handle->vm, handle->version, (void*)handle->obj, handle->extType);

    // retrieve this thread's JNIEnv curEnv - or detect it's detached
    envRes = (*vm)->GetEnv(vm, (void **) &curEnv, version) ;
    DBG_PRINT("GLDebugMessageAMDCallback: 01 - JVM Env: curEnv %p, res 0x%X\n", curEnv, envRes);
    if( JNI_EDETACHED == envRes ) {
        // detached thread - attach to JVM
        if( JNI_OK != ( envRes = (*vm)->AttachCurrentThread(vm, (void**) &newEnv, NULL) ) ) {
            fprintf(stderr, "GLDebugMessageAMDCallback: can't attach thread: %d\n", envRes);
            return;
        }
        curEnv = newEnv;
        DBG_PRINT("GLDebugMessageAMDCallback: 02 - attached .. \n");
    } else if( JNI_OK != envRes ) {
        // oops ..
        fprintf(stderr, "GLDebugMessageAMDCallback: can't GetEnv: %d\n", envRes);
        return;
    }
    (*curEnv)->CallVoidMethod(curEnv, obj, glDebugMessageAMD, 
                              (jint) id, (jint) category, (jint) severity, 
                              (*curEnv)->NewStringUTF(curEnv, message));
    if( NULL != newEnv ) {
        // detached attached thread
        (*vm)->DetachCurrentThread(vm);
        DBG_PRINT("GLDebugMessageAMDCallback: 04 - detached .. \n");
    }
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
    JavaVM *vm;
    DebugHandlerType * handle = malloc(sizeof(DebugHandlerType));
    if(0 != (*env)->GetJavaVM(env, &vm)) {
        vm = NULL;
        JoglCommon_throwNewRuntimeException(env, "GetJavaVM failed");
    }
    handle->vm = vm;
    handle->version = (*env)->GetVersion(env);
    handle->obj = (*env)->NewGlobalRef(env, obj);
    handle->extType = extType;
    DBG_PRINT("GLDebugMessageHandler.register0: vm %p, version 0x%X, jobject %p, extType %d\n", 
        handle->vm, handle->version, (void*)handle->obj, handle->extType);

    if(jogamp_opengl_GLDebugMessageHandler_EXT_ARB == extType) {
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

    DBG_PRINT("GLDebugMessageHandler.unregister0: vm %p, version 0x%X, jobject %p, extType %d\n", 
        handle->vm, handle->version, (void*)handle->obj, handle->extType);

    if(JNI_FALSE == (*env)->IsSameObject(env, obj, handle->obj)) {
        JoglCommon_throwNewRuntimeException(env, "wrong handle (obj doesn't match)");
    }

    if(jogamp_opengl_GLDebugMessageHandler_EXT_ARB == handle->extType) {
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

