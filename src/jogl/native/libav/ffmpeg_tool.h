/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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
 
#ifndef _FFMPEG_TOOL_H
#define _FFMPEG_TOOL_H

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
    #include "libswresample/swresample.h"
#endif

#ifndef LIBAVRESAMPLE_VERSION_MAJOR
#define LIBAVRESAMPLE_VERSION_MAJOR -1
// Opaque
typedef void* AVAudioResampleContext;
#endif
#ifndef LIBSWRESAMPLE_VERSION_MAJOR
#define LIBSWRESAMPLE_VERSION_MAJOR -1
// Opaque
typedef struct SwrContext SwrContext;
#endif

#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>

#include <GL/gl.h>

typedef void (APIENTRYP PFNGLTEXSUBIMAGE2DPROC) (GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, const GLvoid *pixels);
typedef GLenum (APIENTRYP PFNGLGETERRORPROC) (void);
typedef void (APIENTRYP PFNGLFLUSH) (void);
typedef void (APIENTRYP PFNGLFINISH) (void);

/**
 *  AV_TIME_BASE   1000000
 */
#define AV_TIME_BASE_MSEC    (AV_TIME_BASE/1000)

#define AV_VERSION_MAJOR(i) ( ( i >> 16 ) & 0xFF )
#define AV_VERSION_MINOR(i) ( ( i >>  8 ) & 0xFF )
#define AV_VERSION_SUB(i)   ( ( i >>  0 ) & 0xFF )

/** Sync w/ GLMediaPlayer.STREAM_ID_NONE */
#define AV_STREAM_ID_NONE -2

/** Sync w/ GLMediaPlayer.STREAM_ID_AUTO */
#define AV_STREAM_ID_AUTO -1

/** Default number of audio frames per video frame. Sync w/ FFMPEGMediaPlayer.AV_DEFAULT_AFRAMES. */
#define AV_DEFAULT_AFRAMES 8

/** Constant PTS marking an invalid PTS, i.e. Integer.MIN_VALUE == 0x80000000 == {@value}. Sync w/ TimeFrameI.INVALID_PTS */
#define INVALID_PTS 0x80000000

/** Constant PTS marking the end of the stream, i.e. Integer.MIN_VALUE - 1 == 0x7FFFFFFF == {@value}. Sync w/ TimeFrameI.END_OF_STREAM_PTS */
#define END_OF_STREAM_PTS 0x7FFFFFFF

/** Since 54.0.0.1 */
#define AV_HAS_API_AVRESAMPLE(pAV) ( ( LIBAVRESAMPLE_VERSION_MAJOR >= 0 ) && ( pAV->avresampleVersion != 0 ) )

/** Since 55.0.0.1 */
#define AV_HAS_API_SWRESAMPLE(pAV) ( ( LIBSWRESAMPLE_VERSION_MAJOR >= 0 ) && ( pAV->swresampleVersion != 0 ) )

#define MAX_INT(a,b) ( (a >= b) ? a : b )
#define MIN_INT(a,b) ( (a <= b) ? a : b )

static inline float my_av_q2f(AVRational a){
    return (float)a.num / (float)a.den;
}
static inline float my_av_q2f_r(AVRational a){
    return (float)a.den / (float)a.num;
}
static inline int32_t my_av_q2i32(int64_t snum, AVRational a){
    return (int32_t) ( ( snum * (int64_t) a.num ) / (int64_t)a.den );
}
static inline int my_align(int v, int a){
    return ( v + a - 1 ) & ~( a - 1 );
}

typedef struct {
    void *origPtr;
    jobject nioRef;
    int32_t size;
} NIOBuffer_t;

typedef struct {
    int64_t ptsError; // Number of backward PTS values (earlier than last PTS, excluding AV_NOPTS_VALUE)
    int64_t dtsError; // Number of backward DTS values (earlier than last PTS, excluding AV_NOPTS_VALUE)
    int64_t ptsLast;  // PTS of the last frame
    int64_t dtsLast;  // DTS of the last frame
} PTSStats;

typedef struct {
    jobject          ffmpegMediaPlayer;
    int32_t          verbose;

    uint32_t         avcodecVersion;
    uint32_t         avformatVersion;
    uint32_t         avutilVersion;
    uint32_t         avresampleVersion;
    uint32_t         swresampleVersion;

    int32_t          useRefCountedFrames;

    PFNGLTEXSUBIMAGE2DPROC procAddrGLTexSubImage2D;
    PFNGLGETERRORPROC procAddrGLGetError;
    PFNGLFLUSH procAddrGLFlush;
    PFNGLFINISH procAddrGLFinish;

    AVFormatContext* pFormatCtx;
    int32_t          vid;
    AVStream*        pVStream;
    AVCodecContext*  pVCodecCtx;
    AVCodec*         pVCodec;
    AVFrame*         pVFrame; 
    uint32_t         vBufferPlanes; // 1 for RGB*, 3 for YUV, ..
    uint32_t         vBitsPerPixel;
    uint32_t         vBytesPerPixelPerPlane;
    enum PixelFormat vPixFmt;    // native decoder fmt
    int32_t          vPTS;       // msec - overall last video PTS
    PTSStats         vPTSStats;
    int32_t          vTexWidth[4];  // decoded video tex width in bytes for each plane (max 4)
    int32_t          vWidth;
    int32_t          vHeight;
    jboolean         vFlipped;      // false: !GL-Orientation, true: GL-Orientation

    int32_t          aid;
    AVStream*        pAStream;
    AVCodecContext*  pACodecCtx;
    AVCodec*         pACodec;
    AVFrame**        pAFrames;
    NIOBuffer_t*     pANIOBuffers;
    int32_t          aFrameCount;
    int32_t          aFrameCurrent;
    int32_t          aFrameSize; // in samples per channel!
    enum AVSampleFormat aSampleFmt; // native decoder fmt
    int32_t          aSampleRate;
    int32_t          aChannels;
    int32_t          aSinkSupport; // supported by AudioSink
    AVAudioResampleContext* avResampleCtx;
    struct SwrContext*      swResampleCtx;
    uint8_t*         aResampleBuffer;
    enum AVSampleFormat aSampleFmtOut; // out fmt
    int32_t          aChannelsOut;
    int32_t          aSampleRateOut;
    int32_t          aPTS;       // msec - overall last audio PTS
    PTSStats         aPTSStats;

    float            fps;        // frames per seconds
    int32_t          bps_stream; // bits per seconds
    int32_t          bps_video;  // bits per seconds
    int32_t          bps_audio;  // bits per seconds
    int32_t          frames_video;
    int32_t          frames_audio;
    int32_t          duration;   // msec
    int32_t          start_time; // msec

    char             acodec[64];
    char             vcodec[64];

} FFMPEGToolBasicAV_t ;

#endif /* _FFMPEG_TOOL_H */

