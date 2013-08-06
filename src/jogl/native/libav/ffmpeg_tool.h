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

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>

#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>

/**
 *  AV_TIME_BASE   1000000
 */
#define AV_TIME_BASE_MSEC    (AV_TIME_BASE/1000)

static inline float my_av_q2f(AVRational a){
    return a.num / (float) a.den;
}
static inline int32_t my_av_q2i32(int32_t snum, AVRational a){
    return (snum * a.num) / a.den;
}

typedef struct {
    int32_t          verbose;

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
    int32_t          vLinesize[3];  // decoded video linesize in bytes for each plane
    int32_t          vTexWidth[3];  // decoded video tex width in bytes for each plane


    int32_t          aid;
    AVStream*        pAStream;
    AVCodecContext*  pACodecCtx;
    AVCodec*         pACodec;
    AVFrame**        pAFrames;
    int32_t          aFrameCount;
    int32_t          aFrameCurrent;
    int32_t          aSampleRate;
    int32_t          aChannels;
    int32_t          aFrameSize;
    enum AVSampleFormat aSampleFmt; // native decoder fmt
    int32_t          aPTS;       // msec - overall last audio PTS

    float            fps;        // frames per seconds
    int32_t          bps_stream; // bits per seconds
    int32_t          bps_video;  // bits per seconds
    int32_t          bps_audio;  // bits per seconds
    int32_t          totalFrames;
    int32_t          duration;   // msec
    int32_t          start_time; // msec

    char             acodec[64];
    char             vcodec[64];

} FFMPEGToolBasicAV_t ;

#endif /* _FFMPEG_TOOL_H */

