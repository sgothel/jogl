#ifdef _WIN32
    #include <windows.h>
    // __declspec(dllimport) void __stdcall Sleep(unsigned long dwMilliseconds);

    #define usleep(t)    Sleep((t) / 1000)
#endif

#include <gluegen_stdint.h>
#include <gluegen_inttypes.h>
#include <gluegen_stddef.h>
#include <gluegen_stdint.h>

#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libavutil/avutil.h"
#if LIBAVCODEC_VERSION_MAJOR >= 54
#include "libavresample/avresample.h"
#endif

#include "libavutil/pixdesc.h"
#include "libavutil/samplefmt.h"
#if LIBAVUTIL_VERSION_MAJOR < 53
    #include "libavutil/audioconvert.h"
    // 52: #include "libavutil/channel_layout.h"
#endif

#include "jogamp_opengl_util_av_impl_FFMPEGv09Natives.h"

#define FF_FUNC(METHOD) Java_jogamp_opengl_util_av_impl_FFMPEGv09Natives_ ## METHOD

#include "../jogamp_opengl_util_av_impl_FFMPEGvXXNatives.c"
