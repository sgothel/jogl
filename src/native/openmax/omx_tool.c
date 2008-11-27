
#include "omx_tool.h"

#include <NVOMX_IndexExtensions.h>
#if !defined(SELF_TEST)
    #include <jni.h>
#endif

#define NOTSET_U8 ((OMX_U8)0xDE)
#define NOTSET_U16 ((OMX_U16)0xDEDE)
#define NOTSET_U32 ((OMX_U32)0xDEDEDEDE)
#define INIT_PARAM(_X_)  (memset(&(_X_), NOTSET_U8, sizeof(_X_)), ((_X_).nSize = sizeof (_X_)), (_X_).nVersion = vOMX)

#if !defined(SELF_TEST)
void java_throwNewRuntimeException(JNIEnv *env, const char* format, ...);
#else
#include <stdarg.h>
void java_throwNewRuntimeException(void *env, const char* format, ...) {
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
    exit(1);
}
#endif
static void DestroyInstanceUnlock(OMXToolBasicAV_t * pOMXAV);

#define OMXSAFE(x) \
do { \
    OMX_ERRORTYPE err = (x); \
    if (err != OMX_ErrorNone) { \
        java_throwNewRuntimeException(NULL, "FAILED at %s:%d, Error: 0x%x\n", __FILE__, __LINE__, err); \
        if(NULL!=pOMXAV) { \
            DestroyInstanceUnlock(pOMXAV); \
        } \
        return -1; \
    } \
} while (0);

#define OMXSAFEERR(x) \
do { \
    OMX_ERRORTYPE err = (x); \
    if (err != OMX_ErrorNone) { \
        java_throwNewRuntimeException(NULL, "FAILED at %s:%d, Error: 0x%x\n", __FILE__, __LINE__, err); \
        if(NULL!=pOMXAV) { \
            DestroyInstanceUnlock(pOMXAV); \
        } \
        return err; \
    } \
} while (0);

static PFNEGLCREATEIMAGEKHRPROC eglCreateImageKHR;
static PFNEGLCREATEFENCESYNCKHRPROC eglCreateFenceSyncKHR;
static PFNEGLFENCEKHRPROC eglFenceKHR;
static PFNEGLGETSYNCATTRIBKHRPROC eglGetSyncAttribKHR;
static PFNEGLSIGNALSYNCKHRPROC eglSignalSyncKHR;
static int _hasEGLSync = 0;

#define GETEXTENSION(type, ext) \
do \
{ \
    ext = (type) eglGetProcAddress(#ext); \
    if (!ext) \
    { \
        fprintf(stderr, "ERROR getting proc addr of " #ext "\n"); \
    } \
} while (0);

int USE_OPENGL  = 1;
int USE_HWAUDIOOUT = 1;
int USE_AUDIOBUFFERING = 0;
const int PORT_VRENDERER = 6;

static OMX_VERSIONTYPE vOMX;

static int _initialized = 0;
static void InitStatic()
{
    if(_initialized) return;
    _initialized = 1;

    vOMX.s.nVersionMajor = 1;
    vOMX.s.nVersionMinor = 1;
    vOMX.s.nRevision = 0;
    vOMX.s.nStep = 0;

    GETEXTENSION(PFNEGLCREATEIMAGEKHRPROC,      eglCreateImageKHR);
    GETEXTENSION(PFNEGLCREATEFENCESYNCKHRPROC,  eglCreateFenceSyncKHR);
    GETEXTENSION(PFNEGLFENCEKHRPROC,            eglFenceKHR);
    GETEXTENSION(PFNEGLGETSYNCATTRIBKHRPROC,    eglGetSyncAttribKHR);
    GETEXTENSION(PFNEGLSIGNALSYNCKHRPROC,       eglSignalSyncKHR);
    if(NULL==eglFenceKHR||NULL==eglGetSyncAttribKHR||NULL==eglSignalSyncKHR) {
        _hasEGLSync = 0;
    } else {
        _hasEGLSync = 1;
    }
    _hasEGLSync = 0; // JAU

    OMX_Init();
}

static void Invalidate(OMXToolBasicAV_t * pOMXAV)
{
    printf("INVALIDATE\n"); fflush(stdout);
    pOMXAV->status=OMXAV_INVALID;
}

static void GetComponentName(OMX_HANDLETYPE hComponent, KDchar *pName, int nameMaxLen)
{
    OMX_VERSIONTYPE v1, v2;
    OMX_UUIDTYPE uuid;

    OMX_GetComponentVersion(hComponent, pName, &v1, &v2, &uuid);
}

static OMX_ERRORTYPE EventHandler(
        OMX_IN OMX_HANDLETYPE hComponent,
        OMX_IN OMX_PTR pAppData,
        OMX_IN OMX_EVENTTYPE eEvent,
        OMX_IN OMX_U32 nData1,
        OMX_IN OMX_U32 nData2,
        OMX_IN OMX_PTR pEventData)
{
    OMXToolBasicAV_t * pOMXAV = (OMXToolBasicAV_t *) pAppData;
    KDchar name[128];

    GetComponentName(hComponent, name, 128);

    switch (eEvent)
    {
        case OMX_EventCmdComplete:
        {
            printf("event complete: cmd 0x%X, s:0x%X, component: %p - %s\n", (unsigned)nData1, (unsigned)nData2, hComponent, name);
            if (nData1 == OMX_CommandStateSet && pOMXAV->status == OMXAV_INVALID)
            {
                if (nData2 > OMX_StateLoaded) {
                    printf("\t state -> StateLoaded\n");
                    // Transition the component down to StateLoaded
                    OMX_SendCommand(hComponent, OMX_CommandStateSet, OMX_StateLoaded, 0);
                }
            }
            else if (nData1 == OMX_CommandFlush && nData2 == OMX_ALL)
            {
                printf("\t flush\n");
                kdThreadSemPost(pOMXAV->flushSem);
            }
            break;
        }
        case OMX_EventBufferFlag:
            if (nData2 & OMX_BUFFERFLAG_EOS)
            {
                printf("event buffer EOS: component: %p - %s\n", hComponent, name);
                if (pOMXAV->endComponent == hComponent)
                {
                    printf("\t end component - FIN\n");
				    pOMXAV->status = OMXAV_FIN;
                }
		    }
		    break;
        case OMX_EventError: 
            {
                if (nData1 == OMX_ErrorIncorrectStateTransition)
                {
                    printf("event error: 0x%X IncorrectTransition, component: %p - %s\n", (unsigned int) nData1, hComponent, name);
                    // We are shutting down, just continue with that process
                    OMX_SendCommand(hComponent, OMX_CommandStateSet, OMX_StateIdle, 0);
                }
                else if(nData1 == OMX_ErrorSameState) 
                {
                    printf("event error: Same State 0x%X, component: %p - %s\n", (unsigned int) nData2, hComponent, name);
                } 
                else
                {
                    printf("event error: 0x%X, component: %p - %s\n", (unsigned int) nData1, hComponent, name);
                    Invalidate(pOMXAV);
                }
            } 
            break;
        default:
            break;
    }

    return OMX_ErrorNone;
}


static OMX_ERRORTYPE EmptyBufferDone(
        OMX_OUT OMX_HANDLETYPE hComponent,
        OMX_OUT OMX_PTR pAppData,
        OMX_OUT OMX_BUFFERHEADERTYPE* pBuffer)
{
    return OMX_ErrorNone;
}

static OMX_ERRORTYPE FillBufferDone(
        OMX_OUT OMX_HANDLETYPE hComponent,
        OMX_OUT OMX_PTR pAppData,
        OMX_OUT OMX_BUFFERHEADERTYPE* pBuffer)
{
    OMXToolBasicAV_t *pOMXAV = (OMXToolBasicAV_t *) pAppData;

    if (pBuffer->nFlags & OMX_BUFFERFLAG_EOS)
    {
        pOMXAV->status = OMXAV_FIN;
    }
    pOMXAV->available++;
    fprintf(stdout, "FillBufferDone avail %d\n", pOMXAV->available); fflush(stdout);

    return OMX_ErrorNone;
}

#define STATE_SLEEP     10  // ms
#define STATE_TIMEOUT 1000  // ms
#define STATE_TIMEOUT_LOOP (STATE_TIMEOUT/STATE_SLEEP)

static OMX_ERRORTYPE WaitForState(OMX_HANDLETYPE hComponent,
                           OMX_STATETYPE eTestState,
                           OMX_STATETYPE eTestState2,
                           OMX_STATETYPE *currentState)
{
    OMX_ERRORTYPE eError = OMX_ErrorNone;
    OMX_STATETYPE eState;
    int loop=STATE_TIMEOUT_LOOP;

    fprintf(stdout, "WaitForState p1 c:%p s1:0x%X s2:0x%X\n", hComponent, eTestState, eTestState2); fflush(stdout); // JAU
    eError = OMX_GetState(hComponent, &eState);
    fprintf(stdout, "WaitForState p2 s:0x%X e:0x%X\n", eState, eError); fflush(stdout); // JAU

    while (loop>0 &&
           OMX_ErrorNone == eError &&
           eState != eTestState && 
           eState != eTestState2) 
    {
        usleep(STATE_SLEEP*1000);
        loop--;

        eError = OMX_GetState(hComponent, &eState);
        fprintf(stdout, "WaitForState p3 s:0x%X e:0x%X\n", eState, eError); fflush(stdout); // JAU
    }

    if(NULL!=currentState) *currentState=eState;

    return eError;
}

static KDint SyncOnState(OMX_HANDLETYPE hComponent, OMX_STATETYPE state)
{
    OMX_STATETYPE currentState;
    OMX_ERRORTYPE eError = WaitForState(hComponent, state, OMX_StateInvalid, &currentState);
    return ( OMX_ErrorNone != eError ) ? -1 : ( currentState!=state ) ? -2 : 0 ;
}

static KDint CheckState(OMX_HANDLETYPE hComponent, OMX_STATETYPE state)
{
    OMX_ERRORTYPE eError = OMX_ErrorNone;
    OMX_STATETYPE eState;

    eError = OMX_GetState(hComponent, &eState);

    return ( OMX_ErrorNone != eError ) ? -1 : ( eState!=state ) ? -2 : 0 ;
}

KDint OMXToolBasicAV_IsFileValid(const KDchar * file)
{ 
    #ifdef _WIN32
        KDchar cvtdPath[_MAX_PATH];

        if(NULL==file) return -1;

        kdStrcpy_s(cvtdPath, _MAX_PATH, file);
        while(kdStrchr(cvtdPath,'/'))
            *kdStrchr(cvtdPath,'/')='\\';

        {
        #ifdef UNICODE
            wchar_t properfilename[_MAX_PATH];
            mbstowcs( properfilename, cvtdPath, _MAX_PATH );
        #else
            char *properfilename = cvtdPath;
        #endif

            if (INVALID_FILE_ATTRIBUTES==GetFileAttributes(properfilename))
            {
                fprintf(stderr, "!>Input file (%s) does not exist!  EXITING.", file);
                return -2;
            }
        }
    #else
        if(NULL==file) return -1;
    #endif

    return 0;
}

KDint OMXToolBasicAV_CheckState(OMXToolBasicAV_t * pOMXAV, OMX_STATETYPE state)
{
    KDint i, res;
    if(NULL==pOMXAV) return -1;

    for(i=0; i<OMXAV_H_NUMBER; i++) {
        if(0!=pOMXAV->comp[i]) {
            if( 0!=(res=CheckState(pOMXAV->comp[i], state)) ) {
                return res-(i*10); 
            }
        }
    }
    return 0;
}

KDint OMXToolBasicAV_WaitForState(OMXToolBasicAV_t * pOMXAV, OMX_STATETYPE state)
{
    KDint res, i;
    fprintf(stdout, "OMXToolBasicAV_WaitForState %p s:%d\n", pOMXAV, state); fflush(stdout); // JAU
    if(NULL==pOMXAV) {
        fprintf(stdout, "OMXToolBasicAV_WaitForState p1\n"); fflush(stdout); // JAU
        return -1;
    }

    for(i=0; i<OMXAV_H_NUMBER; i++) {
        if(0!=pOMXAV->comp[i]) {
            fprintf(stdout, "OMXToolBasicAV_WaitForState p4 %d c:%p\n", i, pOMXAV->comp[i]); fflush(stdout); // JAU
            if( 0!=(res=SyncOnState(pOMXAV->comp[i], state)) ) {
                KDchar name[128];
                GetComponentName(pOMXAV->comp[i], name, 128);
                fprintf(stdout, "OMXToolBasicAV_WaitForState Failed (Wait) %d c:%p - %s, s:0x%X\n", 
                    i, pOMXAV->comp[i], name, state); 
                fflush(stdout); // JAU
                return res-(i*10); 
            }
        }
    }

    return 0;
}

static OMX_ERRORTYPE RequestState(OMX_HANDLETYPE hComponent, OMX_STATETYPE state, KDboolean wait) 
{
    OMX_ERRORTYPE eError = OMX_ErrorNone;
    OMX_STATETYPE eState;
    eError = OMX_GetState(hComponent, &eState);
    fprintf(stdout, "RequestState p2 c:%p, e:0x%X, s:0x%X\n", 
        hComponent, eError, eState); fflush(stdout); // JAU
    // Skip StateSet in case the state is already reached ..
    if(OMX_ErrorNone != eError || eState!=state) {
        eError = OMX_SendCommand(hComponent, OMX_CommandStateSet, state, 0);
        fprintf(stdout, "RequestState p3 c:%p e:0x%X s: 0x%X -> 0x%X\n", 
            hComponent, eError, eState, state); fflush(stdout); // JAU
        if(wait) {
            OMX_STATETYPE currentState;
            eError = WaitForState(hComponent, state, OMX_StateInvalid, &currentState);
            if ( OMX_ErrorNone==eError && currentState!=state ) eError=OMX_StateInvalid;
        }
    }
    return eError;
}

KDint OMXToolBasicAV_RequestState(OMXToolBasicAV_t * pOMXAV, OMX_STATETYPE state, KDboolean wait)
{
    KDint i;
    fprintf(stdout, "OMXToolBasicAV_RequestState %p s:%d, w:%d\n", pOMXAV, state, wait); fflush(stdout); // JAU
    if(NULL==pOMXAV) {
        fprintf(stdout, "OMXToolBasicAV_RequestState p1\n"); fflush(stdout); // JAU
        return -1;
    }

    for(i=0; i<OMXAV_H_NUMBER; i++) {
        if(0!=pOMXAV->comp[i]) {
            OMXSAFE(RequestState(pOMXAV->comp[i], state, KD_FALSE));
        }
    }

    if (wait)
    {
        return OMXToolBasicAV_WaitForState(pOMXAV, state);
    }

    return 0;
}

static KDint SendCommand(OMXToolBasicAV_t * pOMXAV, OMX_COMMANDTYPE cmd, OMX_U32 nParam1, OMX_PTR pCmdData)
{
    KDint i;
    if(NULL==pOMXAV) return -1;

    for(i=0; i<OMXAV_H_NUMBER; i++) {
        if(0!=pOMXAV->comp[i]) {
            if(OMX_ErrorNone!=OMX_SendCommand(pOMXAV->comp[i], cmd, nParam1, pCmdData)) {
                return -1;
            }
            if(OMX_CommandFlush==cmd) {
                kdThreadSemWait(pOMXAV->flushSem);
            }
        }
    }
    return 0;
}

static KDint PlayStop(OMXToolBasicAV_t * pOMXAV);
static int DetachVideoRenderer(OMXToolBasicAV_t * pOMXAV);

static void DestroyInstanceUnlock(OMXToolBasicAV_t * pOMXAV)
{
    // 0: Stop
    // 1: X -> idle
    // 2: Disable all ports
    // 3: DetachVideoRenderer
    // 3: X -> loaded
    // 4: Free Handle
    // 5: Free mutex/semaphores/struct
    int i, res1=0, res2=0;
    if(NULL==pOMXAV) return;

    fprintf(stderr, "Destroy p1\n"); fflush(stderr);
    PlayStop(pOMXAV);

    fprintf(stderr, "Destroy p2\n"); fflush(stderr);
    if(0!=(res1=OMXToolBasicAV_RequestState(pOMXAV, OMX_StateIdle, KD_TRUE)))
    {
        java_throwNewRuntimeException(NULL, "Destroy - Wait for Idle Failed (%d)", res1);
    }

    fprintf(stderr, "Destroy p3\n"); fflush(stderr);
    SendCommand(pOMXAV, OMX_CommandPortDisable, OMX_ALL, 0); // Ignore error ..

    fprintf(stderr, "Destroy p3\n"); fflush(stderr);
    DetachVideoRenderer(pOMXAV);

    fprintf(stderr, "Destroy p4\n"); fflush(stderr);
    if(0!=(res2=OMXToolBasicAV_RequestState(pOMXAV, OMX_StateLoaded, KD_TRUE)))
    {
        if(!res1) {
            java_throwNewRuntimeException(NULL, "Destroy - Wait for Loaded Failed (%d)", res2);
        }
    }

    fprintf(stderr, "Destroy p5\n"); fflush(stderr);
    for(i=0; i<OMXAV_H_NUMBER; i++) {
        if(0!=pOMXAV->comp[i]) {
            OMX_FreeHandle(pOMXAV->comp[i]);
            pOMXAV->comp[i]=0;
        }
    }

    if(0!=pOMXAV->flushSem) {
        fprintf(stderr, "Destroy p6\n"); fflush(stderr);
        kdThreadSemFree(pOMXAV->flushSem);
        pOMXAV->flushSem=0;
    }
    if(0!=pOMXAV->mutex) {
        fprintf(stderr, "Destroy p7\n"); fflush(stderr);
        kdThreadMutexUnlock(pOMXAV->mutex);
        fprintf(stderr, "Destroy p8\n"); fflush(stderr);
        kdThreadMutexFree(pOMXAV->mutex);
        pOMXAV->mutex=0;
    }

    fprintf(stderr, "Destroy DONE\n"); fflush(stderr);

    free(pOMXAV);
}

static OMX_ERRORTYPE AddFile(OMXToolBasicAV_t * pOMXAV, const KDchar* filename)
{
    // FIXME: Non NV case ..
    OMX_ERRORTYPE eError;
    NVX_PARAM_FILENAME oFilenameParam;
    OMX_INDEXTYPE eIndexParamFilename;

    eError = OMX_GetExtensionIndex(pOMXAV->comp[OMXAV_H_READER], NVX_INDEX_PARAM_FILENAME,
                               &eIndexParamFilename);
    if (eError != OMX_ErrorNone)
        return eError;

    INIT_PARAM(oFilenameParam);
    oFilenameParam.pFilename = (char*) filename;

    eError = OMX_SetParameter(pOMXAV->comp[OMXAV_H_READER], eIndexParamFilename, &oFilenameParam);
    if (eError != OMX_ErrorNone)
        return eError;

    return OMX_ErrorNone;
}

static OMX_ERRORTYPE ProbePort(OMXToolBasicAV_t * pOMXAV, int port, KDchar* component)
{
    // FIXME: Non NV case ..
    KDchar codec[256];
    OMX_U32 roles = 1;
    OMX_ERRORTYPE err = OMX_ErrorNone;
    OMX_INDEXTYPE eParam;
    NVX_PARAM_STREAMTYPE oStreamType;
    OMX_PARAM_PORTDEFINITIONTYPE oPortDef;

    INIT_PARAM(oStreamType);
    INIT_PARAM(oPortDef);
    OMXSAFEERR(OMX_GetExtensionIndex(pOMXAV->comp[OMXAV_H_READER], NVX_INDEX_PARAM_STREAMTYPE, &eParam));

    oPortDef.nPortIndex = port;
    OMXSAFEERR(OMX_GetParameter(pOMXAV->comp[OMXAV_H_READER], OMX_IndexParamPortDefinition, &oPortDef));

    oStreamType.nPort = port;
    OMXSAFEERR(OMX_GetParameter(pOMXAV->comp[OMXAV_H_READER], eParam, &oStreamType));

    if (oPortDef.eDomain == OMX_PortDomainVideo)
        kdStrcpy_s(codec, 128, "video_decoder.");
    else if (oPortDef.eDomain == OMX_PortDomainAudio)
        kdStrcpy_s(codec, 128, "audio_decoder.");
    else
        return OMX_ErrorNotImplemented;

    switch (oStreamType.eStreamType)
    {
#define CODEC(a, b) case a: kdStrncat_s(codec, 128, b, kdStrlen(b)); break
        CODEC(NvxStreamType_MPEG4,  "mpeg4");
        CODEC(NvxStreamType_H264,   "avc");
        CODEC(NvxStreamType_H263,   "mpeg4");
        CODEC(NvxStreamType_WMV,    "vc1");
        CODEC(NvxStreamType_MP3,    "mp3");
        CODEC(NvxStreamType_AAC,    "aac");
        CODEC(NvxStreamType_AACSBR, "eaacplus");
        CODEC(NvxStreamType_BSAC,   "bsac");
        CODEC(NvxStreamType_WMA,    "wma");
        CODEC(NvxStreamType_WMAPro, "wmapro");
        CODEC(NvxStreamType_WMALossless, "wmalossless");
        CODEC(NvxStreamType_AMRWB,  "amrwb");
        CODEC(NvxStreamType_AMRNB,  "amrnb");
        CODEC(NvxStreamType_VORBIS, "vorbis");
#undef CODEC
        default:
            return OMX_ErrorNotImplemented;
    }

    {
        OMX_U8 *tmp = (OMX_U8*) kdMalloc(OMX_MAX_STRINGNAME_SIZE + 1);
        kdMemset(tmp, 0, sizeof(OMX_U8) * (OMX_MAX_STRINGNAME_SIZE + 1));

        err = OMX_GetComponentsOfRole(codec, &roles, &tmp);
        kdStrcpy_s(component, 256, (KDchar*) tmp);
        kdFree(tmp);
    }

    printf("%s -> %s\n", codec, component);
    return err != OMX_ErrorNone ? err : roles ? OMX_ErrorNone : OMX_ErrorComponentNotFound;
}

static OMX_ERRORTYPE UpdateStreamInfo(OMXToolBasicAV_t * pOMXAV)
{
    OMX_PARAM_PORTDEFINITIONTYPE oPortDef;
    kdMemset(&oPortDef, 0, sizeof(oPortDef));
    oPortDef.nSize = sizeof(oPortDef);
    oPortDef.nVersion.s.nVersionMajor = 1;
    oPortDef.nVersion.s.nVersionMinor = 1;
    oPortDef.nPortIndex = 0;
    OMXSAFEERR(OMX_GetParameter(pOMXAV->comp[OMXAV_H_READER], OMX_IndexParamPortDefinition, &oPortDef));

    if (oPortDef.eDomain != OMX_PortDomainVideo)
    {
        kdMemset(&oPortDef, 0, sizeof(oPortDef));
        oPortDef.nSize = sizeof(oPortDef);
        oPortDef.nVersion.s.nVersionMajor = 1;
        oPortDef.nVersion.s.nVersionMinor = 1;

        oPortDef.nPortIndex = 1;
        OMXSAFEERR(OMX_GetParameter(pOMXAV->comp[OMXAV_H_READER], OMX_IndexParamPortDefinition, &oPortDef));
    }
    pOMXAV->width = oPortDef.format.video.nFrameWidth;
    pOMXAV->height = oPortDef.format.video.nFrameHeight;

    return OMX_ErrorNone;
}

static int StartClock(OMXToolBasicAV_t * pOMXAV, KDboolean start, KDfloat32 time) {
    OMX_TIME_CONFIG_CLOCKSTATETYPE oClockState;
    OMX_ERRORTYPE eError = OMX_ErrorNone;
    int loop=STATE_TIMEOUT_LOOP;
    INIT_PARAM(oClockState);
    oClockState.nOffset = 0;
    oClockState.nStartTime = (KD_TRUE==start)? (OMX_TICKS) (time * 1000.0 * 1000.0) : 0;
    oClockState.nWaitMask = 0;
    oClockState.eState = (KD_TRUE==start)?OMX_TIME_ClockStateRunning:OMX_TIME_ClockStateStopped;

    eError = OMX_SetConfig(pOMXAV->comp[OMXAV_H_CLOCK], OMX_IndexConfigTimeClockState, &oClockState);
    while (loop>0 && OMX_ErrorNotReady == eError)
    {
        fprintf(stdout, "Play 3.2\n"); fflush(stdout); // JAU
        usleep(STATE_SLEEP*1000);
        loop--;
        eError = OMX_SetConfig(pOMXAV->comp[OMXAV_H_CLOCK], OMX_IndexConfigTimeClockState,
                               &oClockState);
    }
    return (OMX_ErrorNotReady == eError)?-1:0;
}

static KDint SetClockScale(OMXToolBasicAV_t * pOMXAV, KDfloat32 scale)
{
    OMX_TIME_CONFIG_SCALETYPE pScale;
    INIT_PARAM(pScale);
    pScale.xScale = (int) (scale * (1<<16));

    OMX_SetConfig(pOMXAV->comp[OMXAV_H_CLOCK], OMX_IndexConfigTimeScale, &pScale);
    return 0;
}

static int AttachAudioRenderer(OMXToolBasicAV_t * pOMXAV)
{
    int res=0;
    // Configure audio port

    if (USE_AUDIOBUFFERING) 
    {
        // FIXME: proper audio buffering .. 
        OMXSAFE(OMX_GetHandle(&pOMXAV->comp[OMXAV_H_ABUFFERING], "OMX.Nvidia.audio.visualization", pOMXAV, &pOMXAV->callbacks));
        if(0!=(res=SyncOnState(pOMXAV->comp[OMXAV_H_ABUFFERING], OMX_StateLoaded))) {
            java_throwNewRuntimeException(NULL, "Loading AudioBuffering Failed (%d)", res);
            return res;
        }
        /**
        if (m_settings.m_avsync)
        {
            // Tweak the avsync parameter
            NVX_CONFIG_VISU conf;
            INIT_PARAM(conf);
            conf.nAVSyncOffset = m_settings.m_avsync;

            OMX_INDEXTYPE idx;
            OMX_GetExtensionIndex(pOMXAV->comp[OMXAV_H_ABUFFERING], NVX_INDEX_PARAM_VISUDATA, &idx);
            OMX_SetConfig(pOMXAV->comp[OMXAV_H_ABUFFERING], idx, &conf);
        }*/
    }

    OMXSAFE(OMX_GetHandle(&pOMXAV->comp[OMXAV_H_ARENDERER], "OMX.Nvidia.audio.render",pOMXAV, &pOMXAV->callbacks));
    pOMXAV->endComponent = pOMXAV->comp[OMXAV_H_ARENDERER];

    // mandatory before SetupTunnel
    if(0!=(res=SyncOnState(pOMXAV->comp[OMXAV_H_ARENDERER], OMX_StateLoaded))) {
        java_throwNewRuntimeException(NULL, "Loading AudioRenderer Failed (%d)", res);
        return res;
    }

    {
        OMX_INDEXTYPE eIndexConfigOutputType;
        NVX_CONFIG_AUDIOOUTPUT ao;
        OMX_ERRORTYPE eError;

        INIT_PARAM(ao);

        eError = OMX_GetExtensionIndex(pOMXAV->comp[OMXAV_H_ARENDERER], NVX_INDEX_CONFIG_AUDIO_OUTPUT,
                                       &eIndexConfigOutputType);
        if (eError != OMX_ErrorNoMore)
        {   
            /** FIXME: HDMI configuration ..
             // for now, only put audio out hdmi if the settings say to, regardless of the hdmi-video flag.
             // if (// m_settings.m_hdmiVideo || // m_settings.m_hdmiAudio)
             //    ao.eOutputType = NVX_AUDIO_OutputHdmi;
            else */
                ao.eOutputType = NVX_AUDIO_OutputI2S;

            OMX_SetConfig(pOMXAV->comp[OMXAV_H_ARENDERER], eIndexConfigOutputType, &ao);
        }
    }

    OMXSAFE(OMX_SendCommand(pOMXAV->comp[OMXAV_H_CLOCK],     OMX_CommandPortEnable,   pOMXAV->audioPort, 0));
    OMXSAFE(OMX_SendCommand(pOMXAV->comp[OMXAV_H_ARENDERER], OMX_CommandPortEnable, 1, 0));

    if (USE_AUDIOBUFFERING) 
    {
        OMXSAFE(OMX_SetupTunnel(pOMXAV->comp[OMXAV_H_ADECODER], 1, pOMXAV->comp[OMXAV_H_ABUFFERING], 0));
        OMXSAFE(OMX_SetupTunnel(pOMXAV->comp[OMXAV_H_ABUFFERING], 1, pOMXAV->comp[OMXAV_H_ARENDERER], 0));
    }
    else
    {
        OMXSAFE(OMX_SetupTunnel(pOMXAV->comp[OMXAV_H_ADECODER], 1, pOMXAV->comp[OMXAV_H_ARENDERER], 0));
    }

    OMXSAFE(OMX_SetupTunnel(pOMXAV->comp[OMXAV_H_CLOCK],  pOMXAV->audioPort, pOMXAV->comp[OMXAV_H_ARENDERER], 1));

    return OMX_ErrorNone;
}

static int AttachVideoRenderer(OMXToolBasicAV_t * pOMXAV)
{
    int i, res=0;
    if(KD_NULL!=pOMXAV->comp[OMXAV_H_VSCHEDULER]) {
        java_throwNewRuntimeException(NULL, "Detach Video first");
        return -1;
    }
    OMXSAFE(OMX_GetHandle(&pOMXAV->comp[OMXAV_H_VSCHEDULER], "OMX.Nvidia.video.scheduler", pOMXAV, &pOMXAV->callbacks));
    pOMXAV->endComponent = pOMXAV->comp[OMXAV_H_VSCHEDULER];

    // mandatory before SetupTunnel
    if(0!=(res=SyncOnState(pOMXAV->comp[OMXAV_H_VSCHEDULER], OMX_StateLoaded))) {
        java_throwNewRuntimeException(NULL, "Loading VideoScheduler Failed (%d)", res);
        return res;
    }
    // mandatory before EGLUseImage
    OMXSAFE(RequestState(pOMXAV->comp[OMXAV_H_VSCHEDULER], OMX_StateIdle, KD_FALSE));

    fprintf(stdout, "Attach VR %p c:%p\n", pOMXAV, pOMXAV->comp[OMXAV_H_VSCHEDULER]); fflush(stdout); // JAU
    OMXSAFE(UpdateStreamInfo(pOMXAV));

    fprintf(stdout, "UseEGLImg port enable/tunneling %p\n", pOMXAV);
    OMXSAFE(OMX_SendCommand(pOMXAV->comp[OMXAV_H_CLOCK],      OMX_CommandPortEnable, PORT_VRENDERER, 0));
    OMXSAFE(OMX_SendCommand(pOMXAV->comp[OMXAV_H_VDECODER],   OMX_CommandPortEnable,              1, 0));
    OMXSAFE(OMX_SendCommand(pOMXAV->comp[OMXAV_H_VSCHEDULER], OMX_CommandPortEnable,              0, 0));
    OMXSAFE(OMX_SendCommand(pOMXAV->comp[OMXAV_H_VSCHEDULER], OMX_CommandPortEnable,              2, 0));
    OMXSAFE(OMX_SetupTunnel(pOMXAV->comp[OMXAV_H_VDECODER],            1, pOMXAV->comp[OMXAV_H_VSCHEDULER], 0));
    OMXSAFE(OMX_SetupTunnel(pOMXAV->comp[OMXAV_H_CLOCK],  PORT_VRENDERER, pOMXAV->comp[OMXAV_H_VSCHEDULER], 2));

    for (i = 0; i < pOMXAV->vBufferNum; i++) {
        OMXToolImageBuffer_t *pBuf = &pOMXAV->buffers[i];
        // The Texture, EGLImage and EGLSync was created by the Java client,
        // and registered using the OMXToolBasicAV_SetEGLImageTexture2D command.

        fprintf(stdout, "UseEGLImg %p #%d t:%d i:%p s:%p p1\n", pOMXAV, i, pBuf->tex, pBuf->image, pBuf->sync); fflush(stdout); // JAU

        if(NULL==pBuf->image) {
            java_throwNewRuntimeException(NULL, "AttachVideoRenderer: User didn't set buffer %d/%d\n", i, pOMXAV->vBufferNum);
            return -1;
        } else  {
            // tell decoder output port that it will be using EGLImage
            OMXSAFE(OMX_UseEGLImage(
                    pOMXAV->comp[OMXAV_H_VSCHEDULER],
                    &pBuf->omxBufferHeader,
                    1,      // The port to use the EGLImage for
                    pOMXAV, // app private data
                    pBuf->image));
        }
        fprintf(stdout, "UseEGLImg %p #%d t:%d i:%p s:%p b:%p - p2\n", 
            pOMXAV, i, pBuf->tex, pBuf->image, pBuf->sync, pBuf->omxBufferHeader); fflush(stdout); // JAU
    }

    fprintf(stdout, "UseEGLImg %p #%d DONE\n", pOMXAV, i); fflush(stdout); // JAU
    return 0;
}

static int DetachVideoRenderer(OMXToolBasicAV_t * pOMXAV)
{
    int i;
    if(NULL==pOMXAV) return -1;

    if(KD_NULL==pOMXAV->comp[OMXAV_H_VSCHEDULER]) {
        java_throwNewRuntimeException(NULL, "Attach Video first");
        return -1;
    }
    fprintf(stderr, "DetachVideoRenderer p0\n"); fflush(stderr);
    if(0==CheckState(pOMXAV->comp[OMXAV_H_VSCHEDULER], OMX_StateLoaded)) {
        fprintf(stderr, "DetachVideoRenderer DONE (already state loaded)\n"); fflush(stderr);
        return 0;
    }
    OMXSAFE(RequestState(pOMXAV->comp[OMXAV_H_VSCHEDULER], OMX_StateIdle, KD_TRUE));

    fprintf(stderr, "DetachVideoRenderer p1\n"); fflush(stderr);
    OMXSAFE(OMX_SendCommand(pOMXAV->comp[OMXAV_H_CLOCK],      OMX_CommandPortDisable, PORT_VRENDERER, 0));
    OMXSAFE(OMX_SendCommand(pOMXAV->comp[OMXAV_H_VDECODER],   OMX_CommandPortDisable,              1, 0));
    OMXSAFE(OMX_SendCommand(pOMXAV->comp[OMXAV_H_VSCHEDULER], OMX_CommandPortDisable,              0, 0));
    OMXSAFE(OMX_SendCommand(pOMXAV->comp[OMXAV_H_VSCHEDULER], OMX_CommandPortDisable,              2, 0));
    fprintf(stderr, "DetachVideoRenderer p2\n"); fflush(stderr);

    for (i = 0; i < pOMXAV->vBufferNum; i++) {
        OMXToolImageBuffer_t *pBuf = &pOMXAV->buffers[i];

        // tell decoder output port to stop using EGLImage
        if (NULL!=pBuf->omxBufferHeader) {
            OMX_FreeBuffer(
                    pOMXAV->comp[OMXAV_H_VSCHEDULER],
                    1,
                    pBuf->omxBufferHeader);
            pBuf->omxBufferHeader=NULL;
        }
    }

    OMXSAFE(RequestState(pOMXAV->comp[OMXAV_H_VSCHEDULER], OMX_StateLoaded, KD_TRUE));
    fprintf(stderr, "DetachVideoRenderer p3\n"); fflush(stderr);

    OMX_FreeHandle(pOMXAV->comp[OMXAV_H_VSCHEDULER]);
    pOMXAV->comp[OMXAV_H_VSCHEDULER]=NULL;
    fprintf(stderr, "DetachVideoRenderer DONE\n"); fflush(stderr);
    return 0;
}

OMXToolBasicAV_t * OMXToolBasicAV_CreateInstance(int vBufferNum)
{
    int i;
    OMXToolBasicAV_t * pOMXAV = NULL;
    InitStatic();

    if(vBufferNum>EGLIMAGE_MAX_BUFFERS) {
        fprintf(stderr, "buffer number %d > MAX(%d)\n", vBufferNum, EGLIMAGE_MAX_BUFFERS);
        return NULL;
    }
    pOMXAV = malloc(sizeof(OMXToolBasicAV_t));
    if(NULL==pOMXAV) {
        fprintf(stderr, "Init struct failed!\n");
        return NULL;
    }
    memset(pOMXAV, 0, sizeof(OMXToolBasicAV_t));

    pOMXAV->audioPort=-1;
    pOMXAV->videoPort=-1;

    for(i=0; i<OMXAV_H_NUMBER; i++) {
        pOMXAV->comp[i] = KD_NULL;
    }

    pOMXAV->callbacks.EventHandler    = EventHandler;
    pOMXAV->callbacks.EmptyBufferDone = EmptyBufferDone;
    pOMXAV->callbacks.FillBufferDone  = FillBufferDone;

    pOMXAV->mutex = kdThreadMutexCreate(KD_NULL);
    pOMXAV->flushSem = kdThreadSemCreate(0);

    pOMXAV->vBufferNum = vBufferNum;

    pOMXAV->status=OMXAV_INIT;

    return pOMXAV;
}

int OMXToolBasicAV_SetStream(OMXToolBasicAV_t * pOMXAV, const KDchar * stream)
{
    OMX_ERRORTYPE eError = OMX_ErrorNone;

    fprintf(stdout, "SetStream 1 %s  ..\n", stream); fflush(stdout); // JAU

    // FIXME: verify player state .. ie stop !
    if(pOMXAV->status!=OMXAV_INIT) {
        java_throwNewRuntimeException(NULL, "Player instance in use\n");
        return -1;
    }

    kdThreadMutexLock(pOMXAV->mutex);

    fprintf(stdout, "SetStream 3\n"); fflush(stdout); // JAU

    // Use the "super parser" :) FIXME: Non NV case ..
    eError = OMX_GetHandle(&pOMXAV->comp[OMXAV_H_READER], "OMX.Nvidia.reader", pOMXAV, &pOMXAV->callbacks);

    eError = AddFile(pOMXAV, stream);
    if(eError!=OMX_ErrorNone) {
        java_throwNewRuntimeException(NULL, "Couldn't open or handle stream: %s\n", stream);
        kdThreadMutexUnlock(pOMXAV->mutex);
        return -1;
    }

    fprintf(stdout, "SetStream 4\n"); fflush(stdout); // JAU

    // Auto detect codecs
    {
        OMX_PARAM_PORTDEFINITIONTYPE oPortDef;
        INIT_PARAM(oPortDef);
        oPortDef.nPortIndex = 0;
        pOMXAV->videoPort = -1;
        pOMXAV->audioPort = -1;
        OMXSAFE(OMX_GetParameter(pOMXAV->comp[OMXAV_H_READER], OMX_IndexParamPortDefinition, &oPortDef));

        if (oPortDef.eDomain == OMX_PortDomainAudio)
            pOMXAV->audioPort = oPortDef.nPortIndex;
        else if (oPortDef.eDomain == OMX_PortDomainVideo)
            pOMXAV->videoPort = oPortDef.nPortIndex;
        else
            OMXSAFE(OMX_ErrorNotImplemented);

        INIT_PARAM(oPortDef);
        oPortDef.nPortIndex = 1;
        if (OMX_GetParameter(pOMXAV->comp[OMXAV_H_READER], OMX_IndexParamPortDefinition, &oPortDef) == OMX_ErrorNone)
        {
            if (oPortDef.eDomain == OMX_PortDomainAudio)
                pOMXAV->audioPort = oPortDef.nPortIndex;
            else if (oPortDef.eDomain == OMX_PortDomainVideo)
                pOMXAV->videoPort = oPortDef.nPortIndex;
            else
                OMXSAFE(OMX_ErrorNotImplemented);
        }
        if (pOMXAV->audioPort != -1)
        {
            if (ProbePort(pOMXAV, pOMXAV->audioPort, pOMXAV->audioCodec) != OMX_ErrorNone)
            {
                printf("disabling audio port\n");
                OMXSAFE(OMX_SendCommand(pOMXAV->comp[OMXAV_H_READER], OMX_CommandPortDisable, pOMXAV->audioPort, 0));
                pOMXAV->audioPort = -1;
            }
        }
        if (pOMXAV->videoPort != -1)
            if (ProbePort(pOMXAV, pOMXAV->videoPort, pOMXAV->videoCodec) != OMX_ErrorNone)
            {
                printf("disabling video port\n");
                OMXSAFE(OMX_SendCommand(pOMXAV->comp[OMXAV_H_READER], OMX_CommandPortDisable, pOMXAV->videoPort, 0));
                pOMXAV->videoPort = -1;
            }

        if (pOMXAV->audioPort == -1 && pOMXAV->videoPort == -1)
        {
            java_throwNewRuntimeException(NULL, "Neither audioport or videoport could be played back!\n");
            kdThreadMutexUnlock(pOMXAV->mutex);
            return -1;
        }
    }
    fprintf(stdout, "SetStream 5 ; audioPort %d, videoPort %d\n", pOMXAV->audioPort, pOMXAV->videoPort); fflush(stdout); // JAU

    OMXSAFE(OMX_GetHandle(&pOMXAV->comp[OMXAV_H_CLOCK],     "OMX.Nvidia.clock.component", pOMXAV, &pOMXAV->callbacks));

    fprintf(stdout, "Configuring comp[OMXAV_H_CLOCK]\n");
    {

        OMX_TIME_CONFIG_ACTIVEREFCLOCKTYPE oActiveClockType;
		INIT_PARAM(oActiveClockType);
		oActiveClockType.eClock = (pOMXAV->audioPort != -1) ?
                    OMX_TIME_RefClockAudio : OMX_TIME_RefClockVideo;
        OMXSAFE(OMX_SetConfig(pOMXAV->comp[OMXAV_H_CLOCK], OMX_IndexConfigTimeActiveRefClock,
							   &oActiveClockType));
    }
    OMXSAFE(OMX_SendCommand(pOMXAV->comp[OMXAV_H_CLOCK], OMX_CommandPortDisable, (OMX_U32) -1, 0));

    OMXSAFE(UpdateStreamInfo(pOMXAV));

    kdThreadMutexUnlock(pOMXAV->mutex);

    fprintf(stdout, "SetStream X\n"); fflush(stdout); // JAU

    return 0;
}

int OMXToolBasicAV_UpdateStreamInfo(OMXToolBasicAV_t * pOMXAV) {
    if(NULL==pOMXAV) return -1;
    kdThreadMutexLock(pOMXAV->mutex);
    OMXSAFE(UpdateStreamInfo(pOMXAV));
    kdThreadMutexUnlock(pOMXAV->mutex);
    return 0;
}

int OMXToolBasicAV_SetEGLImageTexture2D(OMXToolBasicAV_t * pOMXAV, KDint i, GLuint tex, EGLImageKHR image, EGLSyncKHR sync)
{
    if(NULL==pOMXAV) return -1;
    fprintf(stdout, "SetEGLImg %p #%d/%d t:%d i:%p s:%p..\n", pOMXAV, i, pOMXAV->vBufferNum, tex, image, sync); fflush(stdout); // JAU
    if(i<0||i>=pOMXAV->vBufferNum) return -1;


    kdThreadMutexLock(pOMXAV->mutex);
    {
        OMXToolImageBuffer_t *pBuf = &pOMXAV->buffers[i];
        pBuf->tex=tex;
        pBuf->image=image;
        pBuf->sync=sync;

    }
    kdThreadMutexUnlock(pOMXAV->mutex);

    return 0;
}

int OMXToolBasicAV_ActivateInstance(OMXToolBasicAV_t * pOMXAV) {
    int res;
    if(NULL==pOMXAV) return -1;
    fprintf(stdout, "ActivateInstance 1\n"); fflush(stdout); // JAU

    kdThreadMutexLock(pOMXAV->mutex);

    if (pOMXAV->audioPort != -1)
    {
        OMXSAFE(OMX_GetHandle(&pOMXAV->comp[OMXAV_H_ADECODER],  pOMXAV->audioCodec, pOMXAV, &pOMXAV->callbacks));
    }

    if (pOMXAV->videoPort != -1)
    {
        OMXSAFE(OMX_GetHandle(&pOMXAV->comp[OMXAV_H_VDECODER],  pOMXAV->videoCodec, pOMXAV, &pOMXAV->callbacks));
    }

    //
    // mandatory: before SetupTunnel (->Activate), wait until all devices are ready ..
    //            arender/vrender must wait as well ..
    if(0!=(res=OMXToolBasicAV_WaitForState(pOMXAV, OMX_StateLoaded))) {
        java_throwNewRuntimeException(NULL, "Loaded Failed (%d)", res);
        kdThreadMutexUnlock(pOMXAV->mutex);
        return res;
    }

    if (pOMXAV->audioPort != -1)
    {
        if(0!=(res=AttachAudioRenderer(pOMXAV))) {
            kdThreadMutexUnlock(pOMXAV->mutex);
            return res;
        }
    }

    if (pOMXAV->videoPort != -1)
    {
        if(0!=(res=AttachVideoRenderer(pOMXAV))) {
            kdThreadMutexUnlock(pOMXAV->mutex);
            return res;
        }
    }

    fprintf(stdout, "Setup tunneling\n"); fflush(stdout); // JAU
    {
        // do tunneling
        if (pOMXAV->audioPort != -1)
        {
            fprintf(stdout, "Setup tunneling audio\n"); fflush(stdout); // JAU
            OMXSAFE(OMX_SetupTunnel(pOMXAV->comp[OMXAV_H_READER], pOMXAV->audioPort, pOMXAV->comp[OMXAV_H_ADECODER],  0));
            // The rest of the audio port is configured in AttachAudioRenderer
        }
        
        if (pOMXAV->videoPort != -1)
        {
            fprintf(stdout, "Setup tunneling video\n"); fflush(stdout); // JAU
            OMXSAFE(OMX_SetupTunnel(pOMXAV->comp[OMXAV_H_READER], pOMXAV->videoPort, pOMXAV->comp[OMXAV_H_VDECODER],  0));
            // The rest of the video port is configured in AttachVideoRenderer
        }
    }
    fprintf(stdout, "ActivateInstance .. %p\n", pOMXAV); fflush(stdout);

    //
    // mandatory: wait until all devices are idle
    //            failure means not all necessary ports/buffer are set.
    //
    if(0!=(res=OMXToolBasicAV_RequestState(pOMXAV, OMX_StateIdle, KD_TRUE)))
    {
        java_throwNewRuntimeException(NULL, "Wait for Idle Failed (%d)", res);
        kdThreadMutexUnlock(pOMXAV->mutex);
        return res;
    }
    pOMXAV->status=OMXAV_STOPPED;
    kdThreadMutexUnlock(pOMXAV->mutex);
    fprintf(stdout, "ActivateInstance done %p\n", pOMXAV); fflush(stdout);
    return 0;
}

int OMXToolBasicAV_DetachVideoRenderer(OMXToolBasicAV_t * pOMXAV) {
    int res;
    if(NULL==pOMXAV) return -1;
    if(pOMXAV->status<=OMXAV_INIT) {
        fprintf(stderr, "Err: DetachVideoRenderer invalid");
        return -1;
    }

    kdThreadMutexLock(pOMXAV->mutex);

    res = DetachVideoRenderer(pOMXAV);

    kdThreadMutexUnlock(pOMXAV->mutex);
    return res;
}

int OMXToolBasicAV_AttachVideoRenderer(OMXToolBasicAV_t * pOMXAV) {
    int res;
    if(NULL==pOMXAV) return -1;
    if(pOMXAV->status<=OMXAV_INIT) {
        fprintf(stderr, "Err: AttachVideoRenderer invalid");
        return -1;
    }

    kdThreadMutexLock(pOMXAV->mutex);

    res = AttachVideoRenderer(pOMXAV);

    kdThreadMutexUnlock(pOMXAV->mutex);
    return res;
}

int OMXToolBasicAV_SetClockScale(OMXToolBasicAV_t * pOMXAV, KDfloat32 scale)
{
    int res;
    if(NULL==pOMXAV) return -1;
    if(pOMXAV->status<=OMXAV_INIT) {
        fprintf(stderr, "Err: SetClockScale invalid");
        return -1;
    }

    kdThreadMutexLock(pOMXAV->mutex);

    res = SetClockScale(pOMXAV, scale);

    kdThreadMutexUnlock(pOMXAV->mutex);
    return res;
}


int OMXToolBasicAV_PlayStart(OMXToolBasicAV_t * pOMXAV)
{
    int res;
    if(NULL==pOMXAV) return -1;
    if(pOMXAV->status<=OMXAV_INIT) {
        fprintf(stderr, "Err: Play invalid"); fflush(stderr);
        return -1;
    }
    if(pOMXAV->status==OMXAV_PLAYING) {
        return 0;
    }

    fprintf(stdout, "Play 1\n"); fflush(stdout); // JAU
    kdThreadMutexLock(pOMXAV->mutex);
    fprintf(stdout, "Play 2\n"); fflush(stdout); // JAU

    if(OMXToolBasicAV_CheckState(pOMXAV, OMX_StateIdle)) {
        if(0!=(res=OMXToolBasicAV_RequestState(pOMXAV, OMX_StateIdle, KD_TRUE))) {
            java_throwNewRuntimeException(NULL, "Idle Failed (%d)", res);
            kdThreadMutexUnlock(pOMXAV->mutex);
            return res;
        }
    }
    if(pOMXAV->status==OMXAV_PAUSED)  {
        fprintf(stdout, "Play 3.0\n"); fflush(stdout); // JAU
        SetClockScale(pOMXAV, 1);
    }
    fprintf(stdout, "Play 3.1\n"); fflush(stdout); // JAU
    if(0!=(res=OMXToolBasicAV_RequestState(pOMXAV, OMX_StateExecuting, KD_TRUE))) {
        java_throwNewRuntimeException(NULL, "Play Execute Failed (%d)", res);
        kdThreadMutexUnlock(pOMXAV->mutex);
        return res;
    }
    if(pOMXAV->status==OMXAV_STOPPED || pOMXAV->status==OMXAV_FIN) {
        fprintf(stdout, "Play 3.2\n"); fflush(stdout); // JAU
        if(StartClock(pOMXAV, KD_TRUE, 0.0)) {
            java_throwNewRuntimeException(NULL, "Play StartClock Failed");
            kdThreadMutexUnlock(pOMXAV->mutex);
            return -1;
        }
        fprintf(stdout, "Play 3.3\n"); fflush(stdout); // JAU
    }
    fprintf(stdout, "Play 4.0\n"); fflush(stdout); // JAU

    kdThreadMutexUnlock(pOMXAV->mutex);
    pOMXAV->status=OMXAV_PLAYING;
    fprintf(stdout, "Play DONE\n"); fflush(stdout); // JAU
    return 0;
}

static int PlayStop(OMXToolBasicAV_t * pOMXAV)
{
    int res;
    if(NULL==pOMXAV) return -1;

    if(pOMXAV->status<=OMXAV_INIT) {
        fprintf(stderr, "Err: Stop invalid");
        return -1;
    }
    if( pOMXAV->status!=OMXAV_PLAYING && pOMXAV->status!=OMXAV_PAUSED ) {
        fprintf(stderr, "Err: Stop not playing nor paused");
        return -1;
    }

    if(OMXToolBasicAV_CheckState(pOMXAV, OMX_StateLoaded)) {
        if(StartClock(pOMXAV, KD_FALSE, 0.0)) {
            java_throwNewRuntimeException(NULL, "Stop StopClock Failed");
            kdThreadMutexUnlock(pOMXAV->mutex);
            return -1;
        }
        if(OMXToolBasicAV_CheckState(pOMXAV, OMX_StateIdle)) {
            if(0!=(res=OMXToolBasicAV_RequestState(pOMXAV, OMX_StateIdle, KD_TRUE))) {
                java_throwNewRuntimeException(NULL, "Stop Idle Failed (%d)", res);
                kdThreadMutexUnlock(pOMXAV->mutex);
                return res;
            }
        }
    }
    pOMXAV->status=OMXAV_STOPPED;
    return 0;
}

int OMXToolBasicAV_PlayStop(OMXToolBasicAV_t * pOMXAV)
{
    int res;
    if(NULL==pOMXAV) return -1;

    kdThreadMutexLock(pOMXAV->mutex);
    res=PlayStop(pOMXAV);
    kdThreadMutexUnlock(pOMXAV->mutex);
    return res;
}

int OMXToolBasicAV_PlayPause(OMXToolBasicAV_t * pOMXAV)
{
    int res;
    if(NULL==pOMXAV) return -1;
    if(pOMXAV->status<=OMXAV_INIT) {
        fprintf(stderr, "Err: Pause invalid");
        return -1;
    }
    if(pOMXAV->status==OMXAV_PAUSED) {
        return 0;
    }
    if(pOMXAV->status!=OMXAV_PLAYING) {
        fprintf(stderr, "Err: Pause not playing");
        return -1;
    }

    kdThreadMutexLock(pOMXAV->mutex);
    SetClockScale(pOMXAV, 0);
    if(0!=(res=OMXToolBasicAV_RequestState(pOMXAV, OMX_StatePause, KD_TRUE))) {
        fprintf(stderr, "Err: Pause Pause Failed (%d)", res);
        kdThreadMutexUnlock(pOMXAV->mutex);
        return res;
    }
    pOMXAV->status=OMXAV_PAUSED;
    kdThreadMutexUnlock(pOMXAV->mutex);
    return 0;
}

int OMXToolBasicAV_PlaySeek(OMXToolBasicAV_t * pOMXAV, KDfloat32 time)
{
    int res;
    OMX_ERRORTYPE eError = OMX_ErrorNotReady;

    if(NULL==pOMXAV) return -1;
    if(pOMXAV->status<=OMXAV_INIT) {
        fprintf(stderr, "Err: Seek invalid");
        return -1;
    }

    kdThreadMutexLock(pOMXAV->mutex);

    // 1. Pause the component through the use of OMX_SendCommand requesting a
    //    state transition to OMX_StatePause.
    if(pOMXAV->status!=OMXAV_PAUSED) {
        if(0!=(res=OMXToolBasicAV_RequestState(pOMXAV, OMX_StatePause, KD_TRUE))) {
            fprintf(stderr, "Err: Seek Pause Failed (%d)", res);
            kdThreadMutexUnlock(pOMXAV->mutex);
            return res;
        }
    }

    // 2. Stop the comp[OMXAV_H_CLOCK] components media comp[OMXAV_H_CLOCK] through the use of OMX_SetConfig
    //    on OMX_TIME_CONFIG_CLOCKSTATETYPE requesting a transition to
    //    OMX_TIME_ClockStateStopped.
    if(StartClock(pOMXAV, KD_FALSE, 0.0)) {
        java_throwNewRuntimeException(NULL, "Seek StopClock Failed");
        kdThreadMutexUnlock(pOMXAV->mutex);
        return -1;
    }

    // 3. Seek to the desired location through the use of OMX_SetConfig on
    //    OMX_IndexConfigTimePosition requesting the desired timestamp.
    {
        OMX_TIME_CONFIG_TIMESTAMPTYPE timestamp;
        int loop=STATE_TIMEOUT_LOOP;
        INIT_PARAM(timestamp);
        timestamp.nPortIndex = 0;
        timestamp.nTimestamp = (OMX_TICKS) (time * 1000.0 * 1000.0);

        eError = OMX_SetConfig(pOMXAV->comp[OMXAV_H_READER], OMX_IndexConfigTimePosition, &timestamp);
        while (loop>0 && OMX_ErrorNotReady == eError)
        {
            usleep(STATE_SLEEP*1000);
            loop--;
            eError = OMX_SetConfig(pOMXAV->comp[OMXAV_H_READER], OMX_IndexConfigTimePosition, &timestamp);
        }
        if (OMX_ErrorNotReady == eError) {
            java_throwNewRuntimeException(NULL, "Seek position Failed");
            kdThreadMutexUnlock(pOMXAV->mutex);
            return -1;
        }
    }

    // 4. Flush all components.
    if(SendCommand(pOMXAV, OMX_CommandFlush, OMX_ALL, 0)) {
        fprintf(stderr, "Err: Seek Flush Failed");
        kdThreadMutexUnlock(pOMXAV->mutex);
        return -1;
    }

    // 5. Start the comp[OMXAV_H_CLOCK] components media comp[OMXAV_H_CLOCK] through the use of OMX_SetConfig
    //    on OMX_TIME_CONFIG_CLOCKSTATETYPE requesting a transition to either
    //    OMX_TIME_ClockStateRunning or
    //    OMX_TIME_ClockStateWaitingForStartTime.
    if(StartClock(pOMXAV, KD_TRUE, time)) {
        java_throwNewRuntimeException(NULL, "Seek StartClock Failed");
        kdThreadMutexUnlock(pOMXAV->mutex);
        return -1;
    }

    // 6. Un-pause the component through the use of OMX_SendCommand requesting a
    //    state transition to OMX_StateExecuting.
    if(pOMXAV->status!=OMXAV_PLAYING) {
        if(0!=(res=OMXToolBasicAV_RequestState(pOMXAV, OMX_StateExecuting, KD_TRUE))) {
            fprintf(stderr, "Err: Seek Execute Failed (%d)", res);
            kdThreadMutexUnlock(pOMXAV->mutex);
            return res;
        }
    }
    kdThreadMutexUnlock(pOMXAV->mutex);
    return 0;
}

GLuint OMXToolBasicAV_GetTexture(OMXToolBasicAV_t * pOMXAV) {
    GLuint texID = 0;
    int ret = pOMXAV->glPos;
    kdThreadMutexLock(pOMXAV->mutex);

    if(pOMXAV->status==OMXAV_PLAYING) {
        int next = (pOMXAV->omxPos + 1) % pOMXAV->vBufferNum;
        
        // fprintf(stdout, "GetTexture A avail %d, filled %d, pos o:%d g:%d\n", 
        //     pOMXAV->available, pOMXAV->filled, pOMXAV->omxPos, pOMXAV->glPos); fflush(stdout);

        while (pOMXAV->filled < pOMXAV->vBufferNum)
        {
            int attr;
            if ( !_hasEGLSync || (
                 eglGetSyncAttribKHR(pOMXAV->buffers[pOMXAV->omxPos].sync, EGL_SYNC_STATUS_KHR, &attr) &&
                 attr == EGL_SIGNALED_KHR )
               )
            {
                // fprintf(stdout, "GetTexture p2.1 attr 0x%X\n", attr); fflush(stdout);
                // OpenGL has finished rendering with this texture, so we are free
                // to make OpenMAX IL fill it with new data.
                OMX_FillThisBuffer(pOMXAV->comp[OMXAV_H_VSCHEDULER], pOMXAV->buffers[pOMXAV->omxPos].omxBufferHeader);
                // fprintf(stdout, "GetTexture p2.2\n", attr); fflush(stdout);
                pOMXAV->omxPos = next;
                next = (pOMXAV->omxPos + 1) % pOMXAV->vBufferNum;
                pOMXAV->filled++;
            }
            else
            {
                // fprintf(stdout, "GetTexture p2.3\n", attr); fflush(stdout);
                break;
            }
        }
    }
    if (pOMXAV->available > 1)
    {
        // fprintf(stdout, "GetTexture p3.1\n"); fflush(stdout);
        // We want to make sure that the previous eglImage
        // has finished, so insert a fence command into the
        // command stream to make sure that any rendering using
        // the previous eglImage has finished.
        //
        // Only move on to rendering the next image if the insertion
        // was successfull.
        if (!_hasEGLSync || eglFenceKHR(pOMXAV->buffers[pOMXAV->glPos].sync))
        {
            // fprintf(stdout, "GetTexture p3.2\n"); fflush(stdout);
            pOMXAV->available--;
            pOMXAV->filled--;
            pOMXAV->glPos = (pOMXAV->glPos + 1) % pOMXAV->vBufferNum;
            ret = pOMXAV->glPos;
        }
    }

    texID = pOMXAV->available ? pOMXAV->buffers[ret].tex : 0;
    // fprintf(stdout, "GetTexture B avail %d, filled %d, pos o:%d g:%d t:%d\n", 
    //     pOMXAV->available, pOMXAV->filled, pOMXAV->omxPos, pOMXAV->glPos, texID); fflush(stdout);

    kdThreadMutexUnlock(pOMXAV->mutex);
    return texID;
}

void OMXToolBasicAV_DestroyInstance(OMXToolBasicAV_t * pOMXAV)
{
    if(NULL==pOMXAV) return;

    kdThreadMutexLock(pOMXAV->mutex);
    DestroyInstanceUnlock(pOMXAV);
}

#if defined(SELF_TEST)

#include <KD/NV_extwindowprops.h>

static PFNEGLDESTROYIMAGEKHRPROC eglDestroyImageKHR;

#ifdef _WIN32_WCE
    #define STDOUT_FILE "\\Storage Card\\stdout.txt"
    #define STDERR_FILE "\\Storage Card\\stderr.txt"
#endif

int ModuleTest()
{
    int i;
    OMXToolBasicAV_t * pOMXAV = NULL;
    GLuint tex; EGLImageKHR image; EGLSyncKHR sync;
    KDchar file[512];
    EGLint attrib = EGL_NONE;

#if 0    
    const EGLint s_configAttribs[] = {
        EGL_RED_SIZE,           1,
        EGL_GREEN_SIZE,         1,
        EGL_BLUE_SIZE,          1,
        EGL_ALPHA_SIZE,         EGL_DONT_CARE,
        EGL_DEPTH_SIZE,         1,
        EGL_STENCIL_SIZE,       EGL_DONT_CARE,
        EGL_SURFACE_TYPE,       EGL_WINDOW_BIT,
        EGL_RENDERABLE_TYPE,    EGL_OPENGL_ES2_BIT,
        EGL_NONE
    };
#else
    const EGLint s_configAttribs[] = {
        EGL_RED_SIZE,           5,
        EGL_GREEN_SIZE,         6,
        EGL_BLUE_SIZE,          5,
        EGL_ALPHA_SIZE,         EGL_DONT_CARE,
        EGL_DEPTH_SIZE,         16,
        EGL_STENCIL_SIZE,       EGL_DONT_CARE,
        EGL_SURFACE_TYPE,       EGL_WINDOW_BIT,
        EGL_RENDERABLE_TYPE,    EGL_OPENGL_ES2_BIT,
        EGL_NONE
    };
#endif

    const EGLint contextAttrs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 2,
        EGL_NONE
    };

    EGLint numConfigs;
    EGLint majorVersion;
    EGLint minorVersion;

    EGLint sWidth, sHeight;

    EGLDisplay       eglDisplay;
    EGLConfig        eglConfig;
    EGLContext       eglContext;
    EGLSurface       eglWindowSurface;
    KDWindow        *kdWindow;
    NativeWindowType ntWindow;

//    KDint wSize[2];

#ifdef _WIN32_WCE
    _wfreopen(TEXT(STDOUT_FILE),L"w",stdout);
    _wfreopen(TEXT(STDERR_FILE),L"w",stderr);
#endif

    /*
     * EGL initialisation.
     */

    eglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    eglInitialize(eglDisplay, &majorVersion, &minorVersion);
    eglChooseConfig(eglDisplay, s_configAttribs, &eglConfig, 1, &numConfigs);
    kdWindow = kdCreateWindow(eglDisplay, eglConfig, KD_NULL);
    
    {
        /* Set fullscreen mode */
        KDboolean fullscreen = KD_TRUE;
        kdSetWindowPropertybv(kdWindow,
            KD_WINDOWPROPERTY_FULLSCREEN_NV, &fullscreen);
    }

    kdRealizeWindow(kdWindow, &ntWindow);

    eglContext = eglCreateContext(eglDisplay, eglConfig, EGL_NO_CONTEXT, contextAttrs);
    
    eglWindowSurface = eglCreateWindowSurface(eglDisplay, eglConfig, ntWindow, KD_NULL);
    eglMakeCurrent(eglDisplay, eglWindowSurface, eglWindowSurface, eglContext);

    printf("EGL Extensions : %s\n",eglQueryString(eglDisplay, EGL_EXTENSIONS));
    printf("EGL CLIENT APIs: %s\n",eglQueryString(eglDisplay, EGL_CLIENT_APIS));

    eglQuerySurface(eglDisplay, eglWindowSurface, EGL_WIDTH   , &sWidth);
    eglQuerySurface(eglDisplay, eglWindowSurface, EGL_HEIGHT  , &sHeight);

    /* Set up the viewport and perspective. */
    printf("screen dim %dx%d\n", sWidth, sHeight);
    glViewport(0, 0, sWidth, sHeight);

    /*
    if (argc == 2)
        kdStrcpy_s(file, 512, argv[1]);
    else */
        kdStrcpy_s(file, 512, "/Storage Card/resources/videoplayer/Luna_800x480_1_5M_H264.mp4");

    if( OMXToolBasicAV_IsFileValid(file) ) {
        fprintf(stderr, "File is invalid");
        return -1;
    }

    GETEXTENSION(PFNEGLDESTROYIMAGEKHRPROC,     eglDestroyImageKHR);

    pOMXAV = OMXToolBasicAV_CreateInstance(3);
    if(OMXToolBasicAV_SetStream(pOMXAV, file)) {
        return -1;
    }
    printf("movie dim %dx%d\n", pOMXAV->width, pOMXAV->height);

    glActiveTexture(GL_TEXTURE0);
    printf("i1: 0x%X\n", glGetError());
    glEnable(GL_TEXTURE_2D);
    printf("i2: 0x%X\n", glGetError());

    for (i = 0; i < 3; i++)
    {
        printf("0: 0x%X\n", glGetError());
        glGenTextures(1, &tex);
        printf("1: tex: %d, e 0x%X\n", tex, glGetError());
        glBindTexture(GL_TEXTURE_2D, tex);
        printf("2: 0x%X\n", glGetError());

        // create space for buffer with a texture
        glTexImage2D(
                GL_TEXTURE_2D,    // target
                0,                // level
                GL_RGBA,          // internal format
                pOMXAV->width,    // width
                pOMXAV->height,   // height
                0,                // border
                GL_RGBA,          // format
                GL_UNSIGNED_BYTE, // type
                NULL);            // pixels -- will be provided later
        printf("3: 0x%X\n", glGetError());
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        printf("4: 0x%X\n", glGetError());

        // create EGLImage from texture
        image = eglCreateImageKHR(
                eglDisplay,
                eglContext,
                EGL_GL_TEXTURE_2D_KHR,
                (EGLClientBuffer)(tex),
                &attrib);
        if (!image)
        {
            printf("eglGetError: 0x%x\n", eglGetError());
            printf("ERROR creating EglImage\n");
            return -1;
        }
        printf("5 eglGetError: 0x%x\n", eglGetError());

        sync = eglCreateFenceSyncKHR(
            eglDisplay, EGL_SYNC_PRIOR_COMMANDS_COMPLETE_KHR, &attrib);

        printf("6 eglGetError: 0x%x\n", eglGetError());

        if(OMXToolBasicAV_SetEGLImageTexture2D(pOMXAV, i, tex, image, sync)) {
            return -1;
        }
    }
    
    printf("7\n"); fflush(stdout);
    if( OMXToolBasicAV_ActivateInstance(pOMXAV) ) {
        return -1;
    }

    printf("8\n"); fflush(stdout);
    if( OMXToolBasicAV_PlayStart(pOMXAV) ) {
        return -1;
    }

    printf("8.2\n"); fflush(stdout);

    i = 0;
    while (i++ < 10) {
        glClear(GL_COLOR_BUFFER_BIT);
        // set uniforms
        // set attributes
        // draw arrays ..
        eglSwapBuffers(eglDisplay, eglWindowSurface);
        printf("Sleep %d\n", i); fflush(stdout);
        usleep(1000);
    }
    
    printf("9\n");
    if( OMXToolBasicAV_PlayStop(pOMXAV) ) {
        fprintf(stderr, "Err: Stop");
        return -1;
    }
    printf("A1\n");
    OMXToolBasicAV_DetachVideoRenderer(pOMXAV); // Stop before ..

    printf("A2\n");
    OMXToolBasicAV_AttachVideoRenderer(pOMXAV); // DetachVideoRenderer before ..

    printf("B\n");
    OMXToolBasicAV_DestroyInstance(pOMXAV);

    printf("C\n");
    eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    eglDestroySurface(eglDisplay, eglWindowSurface);
    eglDestroyContext(eglDisplay, eglContext);

    printf("D\n");
    kdDestroyWindow(kdWindow);

    printf("E\n");
    eglTerminate(eglDisplay);
    printf("F\n");
    eglReleaseThread();

    return 0;
}

KDint kdMain(KDint argc, const KDchar *const *argv)
// int main(int argc, const char *const *argv)
{
    return ModuleTest();
}
#endif

