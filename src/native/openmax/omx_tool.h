
#ifndef _OMX_TOOL_H
#define _OMX_TOOL_H

#ifdef _WIN32
    #include <windows.h>
    // __declspec(dllimport) void __stdcall Sleep(unsigned long dwMilliseconds);

    #define usleep(t)    Sleep((t) / 1000)

    #ifdef _MSC_VER
        /* This typedef is apparently needed for Microsoft compilers before VC8,
           and on Windows CE */
        #if (_MSC_VER < 1400) || defined(UNDER_CE)
            #ifdef _WIN64
                typedef long long intptr_t;
            #else
                typedef int intptr_t;
            #endif
        #endif
    #else
        #include <inttypes.h>
    #endif
#else
    #include <unistd.h>
    #include <inttypes.h>
#endif

#include <OMX_Core.h>
#include <OMX_Index.h>
#include <OMX_Video.h>
#include <OMX_Audio.h>
#include <OMX_Other.h>
#include <OMX_Image.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <EGL/eglext.h>
#include <KD/kd.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define EGLIMAGE_MAX_BUFFERS 4

extern int USE_OPENGL;
extern int USE_HWAUDIOOUT;
extern int USE_AUDIOBUFFERING;
extern const int PORT_VRENDERER;

typedef struct {
    EGLSyncKHR   sync;
    EGLImageKHR  image;
    GLuint tex;
    OMX_BUFFERHEADERTYPE   *omxBufferHeader;
} OMXToolImageBuffer_t;

typedef enum
{
    OMXAV_INVALID=0,
    OMXAV_INIT,
    OMXAV_STOPPED,
    OMXAV_PLAYING,
    OMXAV_PAUSED,
    OMXAV_FIN,
} OMXToolStatus;

typedef enum
{
    OMXAV_H_READER=0,
    OMXAV_H_CLOCK,
    OMXAV_H_ADECODER,
    OMXAV_H_ABUFFERING,
    OMXAV_H_ARENDERER,
    OMXAV_H_VDECODER,
    OMXAV_H_VRENDERER,
    OMXAV_H_VSCHEDULER,
    OMXAV_H_NUMBER,
} OMXToolHandleIdx;


typedef struct {
    OMX_VERSIONTYPE version;
    OMX_HANDLETYPE comp[OMXAV_H_NUMBER];
    OMX_HANDLETYPE endComponent;
    OMX_CALLBACKTYPE callbacks;

    KDchar audioCodec[256];
    KDchar videoCodec[256];
    int audioPort;
    int videoPort;
    int width;
    int height;

    KDThreadMutex * mutex;
    KDThreadSem   * flushSem;

    OMXToolImageBuffer_t buffers[EGLIMAGE_MAX_BUFFERS];
    int vBufferNum;
    int glPos;
    int omxPos;
    int filled;
    int available;

    int status;
} OMXToolBasicAV_t ;

//
// more internal stuff ..
//
KDint OMXToolBasicAV_IsFileValid(const KDchar * file);

//
// OMX state control ..
//
KDint OMXToolBasicAV_CheckState(OMXToolBasicAV_t * pOMXAV, OMX_STATETYPE state);
KDint OMXToolBasicAV_SetState(OMXToolBasicAV_t * pOMXAV, OMX_STATETYPE state, KDboolean wait);

//
// User related functionality, mutex managed
//
OMXToolBasicAV_t * OMXToolBasicAV_CreateInstance(int vBufferNum); // #1
int OMXToolBasicAV_SetStream(OMXToolBasicAV_t * pOMXAV, const KDchar * stream); // #3
int OMXToolBasicAV_UpdateStreamInfo(OMXToolBasicAV_t * pOMXAV);
int OMXToolBasicAV_SetEGLImageTexture2D(OMXToolBasicAV_t * pOMXAV, KDint i, GLuint tex, EGLImageKHR image, EGLSyncKHR sync); // #2..
int OMXToolBasicAV_ActivateInstance(OMXToolBasicAV_t * pOMXAV);

int OMXToolBasicAV_AttachVideoRenderer(OMXToolBasicAV_t * pOMXAV); // DetachVideoRenderer before ..
int OMXToolBasicAV_DetachVideoRenderer(OMXToolBasicAV_t * pOMXAV); // Stop before ..

int OMXToolBasicAV_SetClockScale(OMXToolBasicAV_t * pOMXAV, KDfloat32 scale);
int OMXToolBasicAV_PlayStart(OMXToolBasicAV_t * pOMXAV);
int OMXToolBasicAV_PlayPause(OMXToolBasicAV_t * pOMXAV);
int OMXToolBasicAV_PlayStop(OMXToolBasicAV_t * pOMXAV);
int OMXToolBasicAV_PlaySeek(OMXToolBasicAV_t * pOMXAV, KDfloat32 time);
GLuint OMXToolBasicAV_GetTexture(OMXToolBasicAV_t * pOMXAV);

void OMXToolBasicAV_DestroyInstance(OMXToolBasicAV_t * pOMXAV);
int ModuleTest();




#endif /* _OMX_TOOL_H */

