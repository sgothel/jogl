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
 
#include "jogamp_opengl_util_av_impl_FFMPEGMediaPlayer.h"

#include "JoglCommon.h"
#include "ffmpeg_tool.h"
#include <libavutil/pixdesc.h>
#include <libavutil/samplefmt.h>
#include <GL/gl.h>

static const char * const ClazzNameFFMPEGMediaPlayer = "jogamp/opengl/util/av/impl/FFMPEGMediaPlayer";

static jclass ffmpegMediaPlayerClazz = NULL;
static jmethodID jni_mid_pushSound = NULL;
static jmethodID jni_mid_updateAttributes1 = NULL;
static jmethodID jni_mid_updateAttributes2 = NULL;

#define HAS_FUNC(f) (NULL!=(f))

typedef unsigned (APIENTRYP AVCODEC_VERSION)(void);
typedef unsigned (APIENTRYP AVUTIL_VERSION)(void);
typedef unsigned (APIENTRYP AVFORMAT_VERSION)(void);

static AVCODEC_VERSION sp_avcodec_version;
static AVFORMAT_VERSION sp_avformat_version; 
static AVUTIL_VERSION sp_avutil_version;
// count: 3


// libavcodec
typedef int (APIENTRYP AVCODEC_CLOSE)(AVCodecContext *avctx);
typedef void (APIENTRYP AVCODEC_STRING)(char *buf, int buf_size, AVCodecContext *enc, int encode);
typedef AVCodec *(APIENTRYP AVCODEC_FIND_DECODER)(enum CodecID id);
typedef int (APIENTRYP AVCODEC_OPEN2)(AVCodecContext *avctx, AVCodec *codec, AVDictionary **options);                          // 53.6.0
typedef int (APIENTRYP AVCODEC_OPEN)(AVCodecContext *avctx, AVCodec *codec);
typedef AVFrame *(APIENTRYP AVCODEC_ALLOC_FRAME)(void);
typedef void (APIENTRYP AVCODEC_GET_FRAME_DEFAULTS)(AVFrame *frame);
typedef void (APIENTRYP AVCODEC_FREE_FRAME)(AVFrame **frame);
typedef int (APIENTRYP AVCODEC_DEFAULT_GET_BUFFER)(AVCodecContext *s, AVFrame *pic);
typedef void (APIENTRYP AVCODEC_DEFAULT_RELEASE_BUFFER)(AVCodecContext *s, AVFrame *pic);
typedef void (APIENTRYP AVCODEC_FLUSH_BUFFERS)(AVCodecContext *avctx);
typedef void (APIENTRYP AV_INIT_PACKET)(AVPacket *pkt);
typedef int (APIENTRYP AV_NEW_PACKET)(AVPacket *pkt, int size);
typedef void (APIENTRYP AV_DESTRUCT_PACKET)(AVPacket *pkt);
typedef void (APIENTRYP AV_FREE_PACKET)(AVPacket *pkt);
typedef int (APIENTRYP AVCODEC_DECODE_AUDIO4)(AVCodecContext *avctx, AVFrame *frame, int *got_frame_ptr, AVPacket *avpkt);     // 53.25.0
typedef int (APIENTRYP AVCODEC_DECODE_AUDIO3)(AVCodecContext *avctx, int16_t *samples, int *frame_size_ptr, AVPacket *avpkt);  // 52.23.0
typedef int (APIENTRYP AVCODEC_DECODE_VIDEO2)(AVCodecContext *avctx, AVFrame *picture, int *got_picture_ptr, AVPacket *avpkt); // 52.23.0

static AVCODEC_CLOSE sp_avcodec_close;
static AVCODEC_STRING sp_avcodec_string;
static AVCODEC_FIND_DECODER sp_avcodec_find_decoder;
static AVCODEC_OPEN2 sp_avcodec_open2;                    // 53.6.0
static AVCODEC_OPEN sp_avcodec_open;
static AVCODEC_ALLOC_FRAME sp_avcodec_alloc_frame;
static AVCODEC_GET_FRAME_DEFAULTS sp_avcodec_get_frame_defaults;
static AVCODEC_FREE_FRAME sp_avcodec_free_frame;
static AVCODEC_DEFAULT_GET_BUFFER sp_avcodec_default_get_buffer;
static AVCODEC_DEFAULT_RELEASE_BUFFER sp_avcodec_default_release_buffer;
static AVCODEC_FLUSH_BUFFERS sp_avcodec_flush_buffers;
static AV_INIT_PACKET sp_av_init_packet;
static AV_NEW_PACKET sp_av_new_packet;
static AV_DESTRUCT_PACKET sp_av_destruct_packet;
static AV_FREE_PACKET sp_av_free_packet;
static AVCODEC_DECODE_AUDIO4 sp_avcodec_decode_audio4;    // 53.25.0
static AVCODEC_DECODE_AUDIO3 sp_avcodec_decode_audio3;    // 52.23.0
static AVCODEC_DECODE_VIDEO2 sp_avcodec_decode_video2;    // 52.23.0
// count: 21

// libavutil
typedef void (APIENTRYP AV_FRAME_UNREF)(AVFrame *frame);
typedef void (APIENTRYP AV_FREE)(void *ptr);
typedef int (APIENTRYP AV_GET_BITS_PER_PIXEL)(const AVPixFmtDescriptor *pixdesc);
typedef int (APIENTRYP AV_SAMPLES_GET_BUFFER_SIZE)(int *linesize, int nb_channels, int nb_samples, enum AVSampleFormat sample_fmt, int align);
static const AVPixFmtDescriptor* sp_av_pix_fmt_descriptors;
static AV_FRAME_UNREF sp_av_frame_unref;
static AV_FREE sp_av_free;
static AV_GET_BITS_PER_PIXEL sp_av_get_bits_per_pixel;
static AV_SAMPLES_GET_BUFFER_SIZE sp_av_samples_get_buffer_size;
// count: 26

// libavformat
typedef AVFormatContext *(APIENTRYP AVFORMAT_ALLOC_CONTEXT)(void);
typedef void (APIENTRYP AVFORMAT_FREE_CONTEXT)(AVFormatContext *s);  // 52.96.0
typedef void (APIENTRYP AVFORMAT_CLOSE_INPUT)(AVFormatContext **s);  // 53.17.0
typedef void (APIENTRYP AV_CLOSE_INPUT_FILE)(AVFormatContext *s);
typedef void (APIENTRYP AV_REGISTER_ALL)(void);
typedef int (APIENTRYP AVFORMAT_OPEN_INPUT)(AVFormatContext **ps, const char *filename, AVInputFormat *fmt, AVDictionary **options);
typedef void (APIENTRYP AV_DUMP_FORMAT)(AVFormatContext *ic, int index, const char *url, int is_output);
typedef int (APIENTRYP AV_READ_FRAME)(AVFormatContext *s, AVPacket *pkt);
typedef int (APIENTRYP AV_SEEK_FRAME)(AVFormatContext *s, int stream_index, int64_t timestamp, int flags);
typedef int (APIENTRYP AVFORMAT_SEEK_FILE)(AVFormatContext *s, int stream_index, int64_t min_ts, int64_t ts, int64_t max_ts, int flags);
typedef int (APIENTRYP AV_READ_PLAY)(AVFormatContext *s);
typedef int (APIENTRYP AV_READ_PAUSE)(AVFormatContext *s);
typedef int (APIENTRYP AVFORMAT_NETWORK_INIT)(void);                                                 // 53.13.0
typedef int (APIENTRYP AVFORMAT_NETWORK_DEINIT)(void);                                               // 53.13.0
typedef int (APIENTRYP AVFORMAT_FIND_STREAM_INFO)(AVFormatContext *ic, AVDictionary **options);      // 53.3.0
typedef int (APIENTRYP AV_FIND_STREAM_INFO)(AVFormatContext *ic);

static AVFORMAT_ALLOC_CONTEXT sp_avformat_alloc_context;
static AVFORMAT_FREE_CONTEXT sp_avformat_free_context;            // 52.96.0
static AVFORMAT_CLOSE_INPUT sp_avformat_close_input;              // 53.17.0
static AV_CLOSE_INPUT_FILE sp_av_close_input_file;
static AV_REGISTER_ALL sp_av_register_all;
static AVFORMAT_OPEN_INPUT sp_avformat_open_input;
static AV_DUMP_FORMAT sp_av_dump_format;
static AV_READ_FRAME sp_av_read_frame;
static AV_SEEK_FRAME sp_av_seek_frame;
static AVFORMAT_SEEK_FILE sp_avformat_seek_file;
static AV_READ_PLAY sp_av_read_play;
static AV_READ_PAUSE sp_av_read_pause;
static AVFORMAT_NETWORK_INIT sp_avformat_network_init;            // 53.13.0
static AVFORMAT_NETWORK_DEINIT sp_avformat_network_deinit;        // 53.13.0
static AVFORMAT_FIND_STREAM_INFO sp_avformat_find_stream_info;    // 53.3.0
static AV_FIND_STREAM_INFO sp_av_find_stream_info;
// count: 42

#define SYMBOL_COUNT 42

JNIEXPORT jboolean JNICALL Java_jogamp_opengl_util_av_impl_FFMPEGDynamicLibraryBundleInfo_initSymbols0
  (JNIEnv *env, jclass clazz, jobject jSymbols, jint count)
{
    int64_t* symbols; // jlong -> int64_t -> intptr_t -> FUNC_PTR
    int i;

    if(SYMBOL_COUNT != count) {
        fprintf(stderr, "FFMPEGDynamicLibraryBundleInfo.initSymbols0: Wrong symbol count: Expected %d, Is %d\n", 
                SYMBOL_COUNT, count);
        return JNI_FALSE;
    }
    JoglCommon_init(env);

    i = 0;
    symbols = (int64_t *) (*env)->GetPrimitiveArrayCritical(env, jSymbols, NULL);

    sp_avcodec_version = (AVCODEC_VERSION) (intptr_t) symbols[i++];
    sp_avformat_version = (AVFORMAT_VERSION) (intptr_t) symbols[i++];
    sp_avutil_version = (AVUTIL_VERSION) (intptr_t) symbols[i++];
    // count:  3

    sp_avcodec_close = (AVCODEC_CLOSE)  (intptr_t) symbols[i++];
    sp_avcodec_string = (AVCODEC_STRING) (intptr_t) symbols[i++];
    sp_avcodec_find_decoder = (AVCODEC_FIND_DECODER) (intptr_t) symbols[i++];
    sp_avcodec_open2 = (AVCODEC_OPEN2) (intptr_t) symbols[i++];
    sp_avcodec_open  = (AVCODEC_OPEN) (intptr_t) symbols[i++];
    sp_avcodec_alloc_frame = (AVCODEC_ALLOC_FRAME) (intptr_t) symbols[i++];
    sp_avcodec_get_frame_defaults = (AVCODEC_GET_FRAME_DEFAULTS) (intptr_t) symbols[i++];
    sp_avcodec_free_frame = (AVCODEC_FREE_FRAME) (intptr_t) symbols[i++];
    sp_avcodec_default_get_buffer = (AVCODEC_DEFAULT_GET_BUFFER) (intptr_t) symbols[i++];
    sp_avcodec_default_release_buffer = (AVCODEC_DEFAULT_RELEASE_BUFFER) (intptr_t) symbols[i++];
    sp_avcodec_flush_buffers = (AVCODEC_FLUSH_BUFFERS) (intptr_t) symbols[i++];
    sp_av_init_packet = (AV_INIT_PACKET) (intptr_t) symbols[i++];
    sp_av_new_packet = (AV_NEW_PACKET) (intptr_t) symbols[i++];
    sp_av_destruct_packet = (AV_DESTRUCT_PACKET) (intptr_t) symbols[i++];
    sp_av_free_packet = (AV_FREE_PACKET) (intptr_t) symbols[i++];
    sp_avcodec_decode_audio4 = (AVCODEC_DECODE_AUDIO4) (intptr_t) symbols[i++];
    sp_avcodec_decode_audio3 = (AVCODEC_DECODE_AUDIO3) (intptr_t) symbols[i++];
    sp_avcodec_decode_video2 = (AVCODEC_DECODE_VIDEO2) (intptr_t) symbols[i++];
    // count: 18

    sp_av_pix_fmt_descriptors = (const AVPixFmtDescriptor*)  (intptr_t) symbols[i++];
    sp_av_frame_unref = (AV_FRAME_UNREF) (intptr_t) symbols[i++];
    sp_av_free = (AV_FREE) (intptr_t) symbols[i++];
    sp_av_get_bits_per_pixel = (AV_GET_BITS_PER_PIXEL) (intptr_t) symbols[i++];
    sp_av_samples_get_buffer_size = (AV_SAMPLES_GET_BUFFER_SIZE) (intptr_t) symbols[i++];
    // count: 22

    sp_avformat_alloc_context = (AVFORMAT_ALLOC_CONTEXT) (intptr_t) symbols[i++];;
    sp_avformat_free_context = (AVFORMAT_FREE_CONTEXT) (intptr_t) symbols[i++];
    sp_avformat_close_input = (AVFORMAT_CLOSE_INPUT) (intptr_t) symbols[i++];
    sp_av_close_input_file = (AV_CLOSE_INPUT_FILE) (intptr_t) symbols[i++];
    sp_av_register_all = (AV_REGISTER_ALL) (intptr_t) symbols[i++];
    sp_avformat_open_input = (AVFORMAT_OPEN_INPUT) (intptr_t) symbols[i++];
    sp_av_dump_format = (AV_DUMP_FORMAT) (intptr_t) symbols[i++];
    sp_av_read_frame = (AV_READ_FRAME) (intptr_t) symbols[i++];
    sp_av_seek_frame = (AV_SEEK_FRAME) (intptr_t) symbols[i++];
    sp_avformat_seek_file = (AVFORMAT_SEEK_FILE) (intptr_t) symbols[i++];
    sp_av_read_play = (AV_READ_PLAY) (intptr_t) symbols[i++];
    sp_av_read_pause = (AV_READ_PAUSE) (intptr_t) symbols[i++];
    sp_avformat_network_init = (AVFORMAT_NETWORK_INIT) (intptr_t) symbols[i++];
    sp_avformat_network_deinit = (AVFORMAT_NETWORK_DEINIT) (intptr_t) symbols[i++];
    sp_avformat_find_stream_info = (AVFORMAT_FIND_STREAM_INFO) (intptr_t) symbols[i++];
    sp_av_find_stream_info = (AV_FIND_STREAM_INFO) (intptr_t) symbols[i++];
    // count: 38

    (*env)->ReleasePrimitiveArrayCritical(env, jSymbols, symbols, 0);

    if(SYMBOL_COUNT != i) {
        // boom
        fprintf(stderr, "FFMPEGDynamicLibraryBundleInfo.initSymbols0: Wrong symbol assignment count: Expected %d, Is %d\n", 
                SYMBOL_COUNT, i);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

static void _updateJavaAttributes(JNIEnv *env, jobject instance, FFMPEGToolBasicAV_t* pAV)
{
    // int shallBeDetached = 0;
    // JNIEnv  * env = JoglCommon_GetJNIEnv (&shallBeDetached); 
    if(NULL!=env) {
        int32_t w, h;
        if( NULL != pAV->pVCodecCtx ) {
            // FIXME: Libav Binary compatibility! JAU01
            w = pAV->pVCodecCtx->width; h = pAV->pVCodecCtx->height;
        } else {
            w = 0;                      h = 0;
        }

        (*env)->CallVoidMethod(env, instance, jni_mid_updateAttributes2,
                               pAV->vPixFmt, pAV->vBufferPlanes, 
                               pAV->vBitsPerPixel, pAV->vBytesPerPixelPerPlane,
                               pAV->vLinesize[0], pAV->vLinesize[1], pAV->vLinesize[2],
                               pAV->vTexWidth[0], pAV->vTexWidth[1], pAV->vTexWidth[2], h,
                               pAV->aFramesPerVideoFrame, pAV->aSampleFmt, pAV->aSampleRate, pAV->aChannels);
        (*env)->CallVoidMethod(env, instance, jni_mid_updateAttributes1,
                               pAV->vid, pAV->aid,
                               w, h, 
                               pAV->bps_stream, pAV->bps_video, pAV->bps_audio,
                               pAV->fps, pAV->frames_video, pAV->frames_audio, pAV->duration,
                               (*env)->NewStringUTF(env, pAV->vcodec),
                               (*env)->NewStringUTF(env, pAV->acodec) );
    }
}

static void freeInstance(JNIEnv *env, FFMPEGToolBasicAV_t* pAV) {
    int i;
    if(NULL != pAV) {
        // Close the V codec
        if(NULL != pAV->pVCodecCtx) {
            sp_avcodec_close(pAV->pVCodecCtx);
            pAV->pVCodecCtx = NULL;
        }
        pAV->pVCodec=NULL;

        // Close the A codec
        if(NULL != pAV->pACodecCtx) {
            sp_avcodec_close(pAV->pACodecCtx);
            pAV->pACodecCtx = NULL;
        }
        pAV->pACodec=NULL;

        // Close the frames
        if(NULL != pAV->pVFrame) {
            if(HAS_FUNC(sp_avcodec_free_frame)) {
                sp_avcodec_free_frame(&pAV->pVFrame);
            } else {
                sp_av_free(pAV->pVFrame);
            }
            pAV->pVFrame = NULL;
        }
        if(NULL != pAV->pANIOBuffers) {
            for(i=0; i<pAV->aFrameCount; i++) {
                NIOBuffer_t * pNIOBuffer = &pAV->pANIOBuffers[i];
                if( NULL != pNIOBuffer->nioRef ) {
                    if(pAV->verbose) {
                        fprintf(stderr, "A NIO: Free.X ptr %p / ref %p, %d bytes\n", 
                            pNIOBuffer->origPtr, pNIOBuffer->nioRef, pNIOBuffer->size);
                    }
                    (*env)->DeleteGlobalRef(env, pNIOBuffer->nioRef);
                }
            }
            free(pAV->pANIOBuffers);
            pAV->pANIOBuffers = NULL;
        }
        if(NULL != pAV->pAFrames) {
            for(i=0; i<pAV->aFrameCount; i++) {
                if(HAS_FUNC(sp_avcodec_free_frame)) {
                    sp_avcodec_free_frame(&pAV->pAFrames[i]);
                } else {
                    sp_av_free(pAV->pAFrames[i]);
                }
            }
            free(pAV->pAFrames);
            pAV->pAFrames = NULL;
        }

        // Close the video file
        if(NULL != pAV->pFormatCtx) {
            if(HAS_FUNC(sp_avformat_close_input)) {
                sp_avformat_close_input(&pAV->pFormatCtx);
            } else {
                sp_av_close_input_file(pAV->pFormatCtx);
                if(HAS_FUNC(sp_avformat_free_context)) {
                    sp_avformat_free_context(pAV->pFormatCtx);
                }
            }
            pAV->pFormatCtx = NULL;
        }
        free(pAV);
    }
}

static int my_getPlaneCount(AVPixFmtDescriptor *pDesc) {
    int i, p=-1;
    for(i=pDesc->nb_components-1; i>=0; i--) {
        int p0 = pDesc->comp[i].plane;
        if( p < p0 ) {
            p = p0;
        }
    }
    return p+1;
}

static int my_is_hwaccel_pix_fmt(enum PixelFormat pix_fmt) {
    return sp_av_pix_fmt_descriptors[pix_fmt].flags & PIX_FMT_HWACCEL;
}

#if 0
static enum PixelFormat my_get_format(struct AVCodecContext *s, const enum PixelFormat * fmt) {
    int i=0;
    enum PixelFormat f0, fR = PIX_FMT_NONE;
    char buf[256];

    fprintf(stderr, "get_format ****\n");
    while (fmt[i] != PIX_FMT_NONE /* && ff_is_hwaccel_pix_fmt(fmt[i]) */) {
        f0 = fmt[i];
        if(fR==PIX_FMT_NONE && !my_is_hwaccel_pix_fmt(f0)) {
            fR = f0;
        }
        sp_av_get_pix_fmt_string(buf, sizeof(buf), f0);
        fprintf(stderr, "get_format %d: %d - %s - %s\n", i, f0, sp_av_get_pix_fmt_name(f0), buf);
        ++i;
    }
    fprintf(stderr, "get_format %d - %s *** \n", fR, sp_av_get_pix_fmt_name(fR));
    fflush(NULL);
    return fR;
}
#endif

JNIEXPORT jint JNICALL Java_jogamp_opengl_util_av_impl_FFMPEGMediaPlayer_getAvUtilVersion0
  (JNIEnv *env, jclass clazz) {
    return (jint) sp_avutil_version();
}

JNIEXPORT jint JNICALL Java_jogamp_opengl_util_av_impl_FFMPEGMediaPlayer_getAvFormatVersion0
  (JNIEnv *env, jclass clazz) {
    return (jint) sp_avformat_version();
}

JNIEXPORT jint JNICALL Java_jogamp_opengl_util_av_impl_FFMPEGMediaPlayer_getAvCodecVersion0
  (JNIEnv *env, jclass clazz) {
    return (jint) sp_avcodec_version();
}

JNIEXPORT jboolean JNICALL Java_jogamp_opengl_util_av_impl_FFMPEGMediaPlayer_initIDs0
  (JNIEnv *env, jclass clazz)
{
    JoglCommon_init(env);

    jclass c;
    if (ffmpegMediaPlayerClazz != NULL) {
        return;
    }

    c = (*env)->FindClass(env, ClazzNameFFMPEGMediaPlayer);
    if(NULL==c) {
        JoglCommon_FatalError(env, "JOGL FFMPEG: can't find %s", ClazzNameFFMPEGMediaPlayer);
    }
    ffmpegMediaPlayerClazz = (jclass)(*env)->NewGlobalRef(env, c);
    (*env)->DeleteLocalRef(env, c);
    if(NULL==ffmpegMediaPlayerClazz) {
        JoglCommon_FatalError(env, "JOGL FFMPEG: can't use %s", ClazzNameFFMPEGMediaPlayer);
    }

    jni_mid_pushSound = (*env)->GetMethodID(env, ffmpegMediaPlayerClazz, "pushSound", "(Ljava/nio/ByteBuffer;II)V");
    jni_mid_updateAttributes1 = (*env)->GetMethodID(env, ffmpegMediaPlayerClazz, "updateAttributes", "(IIIIIIIFIIILjava/lang/String;Ljava/lang/String;)V");
    jni_mid_updateAttributes2 = (*env)->GetMethodID(env, ffmpegMediaPlayerClazz, "updateAttributes2", "(IIIIIIIIIIIIIII)V");

    if(jni_mid_pushSound == NULL ||
       jni_mid_updateAttributes1 == NULL ||
       jni_mid_updateAttributes2 == NULL) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jlong JNICALL Java_jogamp_opengl_util_av_impl_FFMPEGMediaPlayer_createInstance0
  (JNIEnv *env, jobject instance, jboolean verbose)
{
    FFMPEGToolBasicAV_t * pAV = calloc(1, sizeof(FFMPEGToolBasicAV_t));
    if(NULL==pAV) {
        JoglCommon_throwNewRuntimeException(env, "Couldn't alloc instance");
        return 0;
    }
    pAV->avcodecVersion = sp_avcodec_version();
    pAV->avformatVersion = sp_avformat_version(); 
    pAV->avutilVersion = sp_avutil_version();

    #if LIBAVCODEC_VERSION_MAJOR >= 55
        // TODO: We keep code on using 1 a/v frame per decoding cycle now.
        //       This is compatible w/ OpenAL's alBufferData(..)
        //       and w/ OpenGL's texture update command, both copy data immediatly.
        // pAV->useRefCountedFrames = AV_HAS_API_REFCOUNTED_FRAMES(pAV);
        pAV->useRefCountedFrames = 0;
    #else
        pAV->useRefCountedFrames = 0;
    #endif

    // Register all formats and codecs
    sp_av_register_all();
    // Network too ..
    if(HAS_FUNC(sp_avformat_network_init)) {
        sp_avformat_network_init();
    }

    pAV->verbose = verbose;
    pAV->vid=AV_STREAM_ID_AUTO;
    pAV->aid=AV_STREAM_ID_AUTO;

    return (jlong) (intptr_t) pAV;
}

JNIEXPORT void JNICALL Java_jogamp_opengl_util_av_impl_FFMPEGMediaPlayer_destroyInstance0
  (JNIEnv *env, jobject instance, jlong ptr)
{
  FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)((void *)((intptr_t)ptr));
  if (pAV != NULL) {
      // stop assumed ..
      freeInstance(env, pAV);
  }
}

static uint64_t getDefaultAudioChannelLayout(int channelCount) {
    switch(channelCount) {
        case 1: return AV_CH_LAYOUT_MONO;
        case 2: return AV_CH_LAYOUT_STEREO;
        case 3: return AV_CH_LAYOUT_SURROUND;
        case 4: return AV_CH_LAYOUT_QUAD;
        case 5: return AV_CH_LAYOUT_5POINT0;
        case 6: return AV_CH_LAYOUT_5POINT1;
        case 7: return AV_CH_LAYOUT_6POINT1;
        case 8: return AV_CH_LAYOUT_7POINT1;
        default: return AV_CH_LAYOUT_NATIVE;
    }
}

static int countAudioPacketsTillVideo(const int maxPackets, FFMPEGToolBasicAV_t *pAV, AVPacket* pPacket, int packetFull, AVFrame* pAFrame, int * pAudioFrames, int *pMaxDataSize);
static int countVideoPacketsTillAudio(const int maxPackets, FFMPEGToolBasicAV_t *pAV, AVPacket* pPacket, int packetFull, int * pVideoFrames);
static void initPTSStats(PTSStats *ptsStats);
static int64_t evalPTS(PTSStats *ptsStats, int64_t inPTS, int64_t inDTS);

JNIEXPORT void JNICALL Java_jogamp_opengl_util_av_impl_FFMPEGMediaPlayer_setStream0
  (JNIEnv *env, jobject instance, jlong ptr, jstring jURL, jint vid, jint aid,
   jint snoopVideoFrameCount, jint aChannelCount, jint aSampleRate)
{
    int res, i;
    jboolean iscopy;
    FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)(intptr_t)ptr;

    if (pAV == NULL) {
        JoglCommon_throwNewRuntimeException(env, "NULL AV ptr");
        return;
    }

    pAV->pFormatCtx = sp_avformat_alloc_context();

    // Open video file
    const char *urlPath = (*env)->GetStringUTFChars(env, jURL, &iscopy);
    res = sp_avformat_open_input(&pAV->pFormatCtx, urlPath, NULL, NULL);
    if(res != 0) {
        JoglCommon_throwNewRuntimeException(env, "Couldn't open URI: %s", urlPath);
        (*env)->ReleaseStringChars(env, jURL, (const jchar *)urlPath);
        return;
    }

    // Retrieve detailed stream information
    if(HAS_FUNC(sp_avformat_find_stream_info)) {
        if(sp_avformat_find_stream_info(pAV->pFormatCtx, NULL)<0) {
            (*env)->ReleaseStringChars(env, jURL, (const jchar *)urlPath);
            JoglCommon_throwNewRuntimeException(env, "Couldn't find stream information");
            return;
        }
    } else {
        if(sp_av_find_stream_info(pAV->pFormatCtx)<0) {
            (*env)->ReleaseStringChars(env, jURL, (const jchar *)urlPath);
            JoglCommon_throwNewRuntimeException(env, "Couldn't find stream information");
            return;
        }
    }

    if(pAV->verbose) {
        // Dump information about file onto standard error
        sp_av_dump_format(pAV->pFormatCtx, 0, urlPath, JNI_FALSE);
    }
    (*env)->ReleaseStringChars(env, jURL, (const jchar *)urlPath);
    // FIXME: Libav Binary compatibility! JAU01
    if (pAV->pFormatCtx->duration != AV_NOPTS_VALUE) {
        pAV->duration = pAV->pFormatCtx->duration / AV_TIME_BASE_MSEC;
    }
    if (pAV->pFormatCtx->start_time != AV_NOPTS_VALUE) {
        pAV->start_time = pAV->pFormatCtx->start_time / AV_TIME_BASE_MSEC;
    }
    if (pAV->pFormatCtx->bit_rate) {
        pAV->bps_stream = pAV->pFormatCtx->bit_rate;
    }

    if(pAV->verbose) {
        fprintf(stderr, "Streams: %d, req vid %d aid %d\n", pAV->pFormatCtx->nb_streams, vid, aid);
    }

    // Find the first audio and video stream, or the one matching vid
    // FIXME: Libav Binary compatibility! JAU01
    for(i=0; ( AV_STREAM_ID_AUTO==pAV->aid || AV_STREAM_ID_AUTO==pAV->vid ) && i<pAV->pFormatCtx->nb_streams; i++) {
        AVStream *st = pAV->pFormatCtx->streams[i];
        if(pAV->verbose) {
            fprintf(stderr, "Stream: %d: is-video %d, is-audio %d\n", i, (AVMEDIA_TYPE_VIDEO == st->codec->codec_type), AVMEDIA_TYPE_AUDIO == st->codec->codec_type);
        }
        if(AVMEDIA_TYPE_VIDEO == st->codec->codec_type) {
            if(AV_STREAM_ID_AUTO==pAV->vid && (AV_STREAM_ID_AUTO==vid || vid == i) ) {
                pAV->pVStream = st;
                pAV->vid=i;
            }
        } else if(AVMEDIA_TYPE_AUDIO == st->codec->codec_type) {
            if(AV_STREAM_ID_AUTO==pAV->aid && (AV_STREAM_ID_AUTO==aid || aid == i) ) {
                pAV->pAStream = st;
                pAV->aid=i;
            }
        }
    }
    if( AV_STREAM_ID_AUTO == pAV->aid ) {
        pAV->aid = AV_STREAM_ID_NONE;
    }
    if( AV_STREAM_ID_AUTO == pAV->vid ) {
        pAV->vid = AV_STREAM_ID_NONE;
    }

    if( pAV->verbose ) {
        fprintf(stderr, "Found vid %d, aid %d\n", pAV->vid, pAV->aid);
    }

    if(0<=pAV->aid) {
        AVFrame * pAFrame0 = sp_avcodec_alloc_frame();
        if( NULL == pAFrame0 ) {
            JoglCommon_throwNewRuntimeException(env, "Couldn't alloc 1st audio frame\n");
            return;
        }

        // Get a pointer to the codec context for the audio stream
        // FIXME: Libav Binary compatibility! JAU01
        pAV->pACodecCtx=pAV->pAStream->codec;

        // FIXME: Libav Binary compatibility! JAU01
        if (pAV->pACodecCtx->bit_rate) {
            pAV->bps_audio = pAV->pACodecCtx->bit_rate;
        }

        // Customize ..
        // pAV->pACodecCtx->thread_count=2;
        // pAV->pACodecCtx->thread_type=FF_THREAD_FRAME|FF_THREAD_SLICE; // Decode more than one frame at once
        pAV->pACodecCtx->thread_count=0;
        pAV->pACodecCtx->thread_type=0;
        pAV->pACodecCtx->workaround_bugs=FF_BUG_AUTODETECT;
        pAV->pACodecCtx->skip_frame=AVDISCARD_DEFAULT;

        pAV->pACodecCtx->request_channel_layout=getDefaultAudioChannelLayout(aChannelCount);
        if( AV_HAS_API_REQUEST_CHANNELS(pAV) && 1 <= aChannelCount && aChannelCount <= 2 ) {
            pAV->pACodecCtx->request_channels=aChannelCount;
        }
        pAV->pACodecCtx->request_sample_fmt=AV_SAMPLE_FMT_S16;
        // ignored: aSampleRate !
        pAV->pACodecCtx->skip_frame=AVDISCARD_DEFAULT;

        sp_avcodec_string(pAV->acodec, sizeof(pAV->acodec), pAV->pACodecCtx, 0);

        // Find the decoder for the audio stream
        pAV->pACodec=sp_avcodec_find_decoder(pAV->pACodecCtx->codec_id);
        if(pAV->pACodec==NULL) {
            JoglCommon_throwNewRuntimeException(env, "Couldn't find audio codec %d, %s", pAV->pACodecCtx->codec_id, pAV->acodec);
            return;
        }

        // Open codec
        #if LIBAVCODEC_VERSION_MAJOR >= 55
            pAV->pACodecCtx->refcounted_frames = pAV->useRefCountedFrames;
        #endif
        if(HAS_FUNC(sp_avcodec_open2)) {
            res = sp_avcodec_open2(pAV->pACodecCtx, pAV->pACodec, NULL);
        } else {
            res = sp_avcodec_open(pAV->pACodecCtx, pAV->pACodec);
        }
        if(res<0) {
            JoglCommon_throwNewRuntimeException(env, "Couldn't open audio codec %d, %s", pAV->pACodecCtx->codec_id, pAV->acodec);
            return;
        }

        pAV->aSampleRate = pAV->pACodecCtx->sample_rate;
        pAV->aChannels = pAV->pACodecCtx->channels;
        pAV->aFrameSize = pAV->pACodecCtx->frame_size; // in samples!
        pAV->aSampleFmt = pAV->pACodecCtx->sample_fmt;
        pAV->frames_audio = pAV->pAStream->nb_frames;
        if( pAV->verbose ) {
            fprintf(stderr, "A channels %d, sample_rate %d, frame_size %d, frame_number %d, r_frame_rate %f, avg_frame_rate %f, nb_frames %d, \n", 
                pAV->aChannels, pAV->aSampleRate, pAV->aFrameSize, pAV->pACodecCtx->frame_number,
                my_av_q2f(pAV->pAStream->r_frame_rate),
                my_av_q2f(pAV->pAStream->avg_frame_rate),
                pAV->pAStream->nb_frames);
        }

        if( 0 >= snoopVideoFrameCount ) {
            pAV->aFramesPerVideoFrame = 0;
        } else {
            if( 0<=pAV->vid ) {
                int aFramesPerVideoFrame;
                int aFramesSequential = 0;
                int aMaxDataSize = 0;
                AVPacket packet;
                int packetFull = 0;
                int _aFramesBeforeVideo;
                int _audioFramesOverlap=0;
                int _aMaxDataSize;
                int _vFrames;
                int _vFramesOverlap=0;
                int _packetCount;
                int totalVFrames = 0;
                int totalAFrames = 0;
                int totalPackets = 0;

                while( totalVFrames < snoopVideoFrameCount ) {
                    int _packetCount = countAudioPacketsTillVideo(40, pAV, &packet, packetFull, pAFrame0, &_aFramesBeforeVideo, &_aMaxDataSize);
                    if( _packetCount >= 0 ) {
                        totalPackets += _packetCount;
                        if( _aFramesBeforeVideo > 0 ) {
                            // one video frame!
                            _vFramesOverlap=1;
                            packetFull = 1;
                        }
                        _aFramesBeforeVideo += _audioFramesOverlap;
                        totalAFrames += _aFramesBeforeVideo;
                        if( _aFramesBeforeVideo > aFramesSequential ) {
                            aFramesSequential = _aFramesBeforeVideo;
                        }
                        if( _aMaxDataSize > aMaxDataSize ) {
                            aMaxDataSize = _aMaxDataSize;
                        }
                        _packetCount = countVideoPacketsTillAudio(40, pAV, &packet, packetFull, &_vFrames);
                        if( _packetCount >= 0 ) {
                            totalPackets += _packetCount;
                            if( _vFrames > 0 ) {
                                // one audio frame!
                                _audioFramesOverlap=1;
                                packetFull = 1;
                            }
                            _vFrames += _vFramesOverlap;
                            totalVFrames += _vFrames;
                        }
                        if( pAV->verbose ) {
                            fprintf(stderr, "Snoop Packet #%d, V-Frames: %d, A-frames %d Seq(now %d, max %d), max-size (now %d, max %d)\n", 
                                totalPackets, totalVFrames, totalAFrames, _aFramesBeforeVideo, aFramesSequential, _aMaxDataSize, aMaxDataSize);
                        }
                    }
                }
                const int audioFramesReadAhead = totalAFrames - totalVFrames;
                if( audioFramesReadAhead > aFramesSequential ) {
                    aFramesPerVideoFrame = audioFramesReadAhead;
                } else {
                    aFramesPerVideoFrame = aFramesSequential;
                }
                if( AV_DEFAULT_AFRAMES > aFramesPerVideoFrame || aFramesPerVideoFrame > 10*AV_DEFAULT_AFRAMES ) {
                    aFramesPerVideoFrame = AV_DEFAULT_AFRAMES;
                }
                pAV->aFramesPerVideoFrame = aFramesPerVideoFrame;
                sp_av_seek_frame(pAV->pFormatCtx, -1, 0, AVSEEK_FLAG_BACKWARD);
                sp_avcodec_flush_buffers( pAV->pACodecCtx );
                if( pAV->verbose ) {
                    fprintf(stderr, "Snooped Packets %d, V-Frames: %d, A-frames %d Seq %d, readAhead %d -> Cached %d/%d, max-size %d\n", 
                        totalPackets, totalVFrames, totalAFrames, aFramesSequential, audioFramesReadAhead, aFramesPerVideoFrame, pAV->aFramesPerVideoFrame, aMaxDataSize);
                }
            } else {
                pAV->aFramesPerVideoFrame = AV_DEFAULT_AFRAMES;
                if( pAV->verbose ) {
                    fprintf(stderr, "A-frame Count %d\n", pAV->aFramesPerVideoFrame);
                }
            }
        }

        // Allocate audio frames
        // FIXME: Libav Binary compatibility! JAU01
        if( pAV->useRefCountedFrames && pAV->aFramesPerVideoFrame > 0 ) {
            pAV->aFrameCount = pAV->aFramesPerVideoFrame;
        } else {
            pAV->aFrameCount = 1;
        }
        pAV->pANIOBuffers = calloc(pAV->aFrameCount, sizeof(NIOBuffer_t));
        pAV->pAFrames = calloc(pAV->aFrameCount, sizeof(AVFrame*));
        pAV->pAFrames[0] = pAFrame0;
        for(i=1; i<pAV->aFrameCount; i++) {
            pAV->pAFrames[i] = sp_avcodec_alloc_frame();
            if( NULL == pAV->pAFrames[i] ) {
                JoglCommon_throwNewRuntimeException(env, "Couldn't alloc audio frame %d / %d", i, pAV->aFrameCount);
                return;
            }
        }
        pAV->aFrameCurrent = 0;
    }

    if(0<=pAV->vid) {
        // Get a pointer to the codec context for the video stream
        // FIXME: Libav Binary compatibility! JAU01
        pAV->pVCodecCtx=pAV->pVStream->codec;
        #if 0
        pAV->pVCodecCtx->get_format = my_get_format;
        #endif

        if (pAV->pVCodecCtx->bit_rate) {
            // FIXME: Libav Binary compatibility! JAU01
            pAV->bps_video = pAV->pVCodecCtx->bit_rate;
        }

        // Customize ..
        // pAV->pVCodecCtx->thread_count=2;
        // pAV->pVCodecCtx->thread_type=FF_THREAD_FRAME|FF_THREAD_SLICE; // Decode more than one frame at once
        pAV->pVCodecCtx->thread_count=0;
        pAV->pVCodecCtx->thread_type=0;
        pAV->pVCodecCtx->workaround_bugs=FF_BUG_AUTODETECT;
        pAV->pVCodecCtx->skip_frame=AVDISCARD_DEFAULT;

        sp_avcodec_string(pAV->vcodec, sizeof(pAV->vcodec), pAV->pVCodecCtx, 0);

        // Find the decoder for the video stream
        pAV->pVCodec=sp_avcodec_find_decoder(pAV->pVCodecCtx->codec_id);
        if(pAV->pVCodec==NULL) {
            JoglCommon_throwNewRuntimeException(env, "Couldn't find video codec %d, %s", pAV->pVCodecCtx->codec_id, pAV->vcodec);
            return;
        }

        // Open codec
        #if LIBAVCODEC_VERSION_MAJOR >= 55
            pAV->pVCodecCtx->refcounted_frames = pAV->useRefCountedFrames;
        #endif
        if(HAS_FUNC(sp_avcodec_open2)) {
            res = sp_avcodec_open2(pAV->pVCodecCtx, pAV->pVCodec, NULL);
        } else {
            res = sp_avcodec_open(pAV->pVCodecCtx, pAV->pVCodec);
        }
        if(res<0) {
            JoglCommon_throwNewRuntimeException(env, "Couldn't open video codec %d, %s", pAV->pVCodecCtx->codec_id, pAV->vcodec);
            return;
        }

        // Hack to correct wrong frame rates that seem to be generated by some codecs
        // FIXME: Libav Binary compatibility! JAU01
        if(pAV->pVCodecCtx->time_base.num>1000 && pAV->pVCodecCtx->time_base.den==1) {
            pAV->pVCodecCtx->time_base.den=1000;
        }
        // FIXME: Libav Binary compatibility! JAU01
        if( 0 < pAV->pVStream->avg_frame_rate.den ) {
            pAV->fps = my_av_q2f(pAV->pVStream->avg_frame_rate);
        } else {
            pAV->fps = my_av_q2f(pAV->pVStream->r_frame_rate);
        }
        pAV->frames_video = pAV->pVStream->nb_frames;
            
        if( pAV->verbose ) {
            fprintf(stderr, "V frame_size %d, frame_number %d, r_frame_rate %f %d/%d, avg_frame_rate %f %d/%d, nb_frames %d, \n", 
                pAV->pVCodecCtx->frame_size, pAV->pVCodecCtx->frame_number, 
                my_av_q2f(pAV->pVStream->r_frame_rate), pAV->pVStream->r_frame_rate.num, pAV->pVStream->r_frame_rate.den, 
                my_av_q2f(pAV->pVStream->avg_frame_rate), pAV->pVStream->avg_frame_rate.num, pAV->pVStream->avg_frame_rate.den,
                pAV->pVStream->nb_frames);
        }

        // Allocate video frame
        // FIXME: Libav Binary compatibility! JAU01
        pAV->vPixFmt = pAV->pVCodecCtx->pix_fmt;
        {   
            AVPixFmtDescriptor pixDesc = sp_av_pix_fmt_descriptors[pAV->vPixFmt];
            pAV->vBitsPerPixel = sp_av_get_bits_per_pixel(&pixDesc);
            pAV->vBufferPlanes = my_getPlaneCount(&pixDesc);
        }
        pAV->pVFrame=sp_avcodec_alloc_frame();
        if( pAV->pVFrame == NULL ) {
            JoglCommon_throwNewRuntimeException(env, "Couldn't alloc video frame");
            return;
        }
        res = sp_avcodec_default_get_buffer(pAV->pVCodecCtx, pAV->pVFrame);
        if(0==res) {
            const int32_t bytesPerPixel = ( pAV->vBitsPerPixel + 7 ) / 8 ;
            if(1 == pAV->vBufferPlanes) {
                pAV->vBytesPerPixelPerPlane = bytesPerPixel;
            } else {
                pAV->vBytesPerPixelPerPlane = 1;
            }
            for(i=0; i<3; i++) {
                // FIXME: Libav Binary compatibility! JAU01
                pAV->vLinesize[i] = pAV->pVFrame->linesize[i];
                pAV->vTexWidth[i] = pAV->vLinesize[i] / pAV->vBytesPerPixelPerPlane ;
            }
            sp_avcodec_default_release_buffer(pAV->pVCodecCtx, pAV->pVFrame);
        } else {
            JoglCommon_throwNewRuntimeException(env, "Couldn't peek video buffer dimension");
            return;
        }
    }
    pAV->vPTS=0;
    pAV->aPTS=0;
    initPTSStats(&pAV->vPTSStats);
    initPTSStats(&pAV->aPTSStats);
    _updateJavaAttributes(env, instance, pAV);
}

JNIEXPORT void JNICALL Java_jogamp_opengl_util_av_impl_FFMPEGMediaPlayer_setGLFuncs0
  (JNIEnv *env, jobject instance, jlong ptr, jlong jProcAddrGLTexSubImage2D, jlong jProcAddrGLGetError, jlong jProcAddrGLFlush, jlong jProcAddrGLFinish)
{
    FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)((void *)((intptr_t)ptr));
    pAV->procAddrGLTexSubImage2D = (PFNGLTEXSUBIMAGE2DPROC) (intptr_t)jProcAddrGLTexSubImage2D;
    pAV->procAddrGLGetError = (PFNGLGETERRORPROC) (intptr_t)jProcAddrGLGetError;
    pAV->procAddrGLFlush = (PFNGLFLUSH) (intptr_t)jProcAddrGLFlush;
    pAV->procAddrGLFinish = (PFNGLFINISH) (intptr_t)jProcAddrGLFinish;
}

#if 0
#define DBG_TEXSUBIMG2D_a(c,p,i) fprintf(stderr, "TexSubImage2D.%c offset %d / %d, size %d x %d, ", c, p->pVCodecCtx->width, p->pVCodecCtx->height/2, p->vTexWidth[i], p->pVCodecCtx->height/2)
#define DBG_TEXSUBIMG2D_b(p) fprintf(stderr, "err 0x%X\n", pAV->procAddrGLGetError())
#else
#define DBG_TEXSUBIMG2D_a(c,p,i)
#define DBG_TEXSUBIMG2D_b(p)
#endif

JNIEXPORT jint JNICALL Java_jogamp_opengl_util_av_impl_FFMPEGMediaPlayer_readNextPacket0
  (JNIEnv *env, jobject instance, jlong ptr, jint texTarget, jint texFmt, jint texType)
{
    FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)((void *)((intptr_t)ptr));

    AVPacket packet;
    int frameDecoded;
    jint resPTS = INVALID_PTS;

    sp_av_init_packet(&packet);

    const int avRes = sp_av_read_frame(pAV->pFormatCtx, &packet);
    if( AVERROR_EOF == avRes || ( pAV->pFormatCtx->pb && pAV->pFormatCtx->pb->eof_reached ) ) {
        resPTS = END_OF_STREAM_PTS;
    } else if( 0 <= avRes ) {
        /**
        if( pAV->verbose ) {
            fprintf(stderr, "P: ptr %p, size %d\n", packet.data, packet.size);
        } */
        if(packet.stream_index==pAV->aid) {
            // Decode audio frame
            if(NULL == pAV->pAFrames) { // no audio registered
                sp_av_free_packet(&packet);
                return 0;
            }
            int frameCount;
            int flush_complete = 0;
            for ( frameCount=0; 0 < packet.size || 0 == frameCount; frameCount++ ) {
                int len1;
                if (flush_complete) {
                    break;
                }
                NIOBuffer_t * pNIOBufferCurrent = &pAV->pANIOBuffers[pAV->aFrameCurrent];
                AVFrame* pAFrameCurrent = pAV->pAFrames[pAV->aFrameCurrent];
                if( pAV->useRefCountedFrames ) {
                    sp_av_frame_unref(pAFrameCurrent);
                    pAV->aFrameCurrent = ( pAV->aFrameCurrent + 1 ) % pAV->aFrameCount ;
                }
                sp_avcodec_get_frame_defaults(pAFrameCurrent);
                if(HAS_FUNC(sp_avcodec_decode_audio4)) {
                    len1 = sp_avcodec_decode_audio4(pAV->pACodecCtx, pAFrameCurrent, &frameDecoded, &packet);
                } else {
                    #if 0
                    len1 = sp_avcodec_decode_audio3(pAV->pACodecCtx, int16_t *samples, int *frame_size_ptr, &frameDecoded, &packet);
                    #endif
                    JoglCommon_throwNewRuntimeException(env, "Unimplemented: FFMPEGMediaPlayer sp_avcodec_decode_audio3 fallback");
                    return 0;
                }
                if (len1 < 0) {
                    // if error, we skip the frame 
                    packet.size = 0;
                    break;
                }
                packet.data += len1;
                packet.size -= len1;

                if (!frameDecoded) {
                    // stop sending empty packets if the decoder is finished 
                    if (!packet.data && pAV->pACodecCtx->codec->capabilities & CODEC_CAP_DELAY) {
                        flush_complete = 1;
                    }
                    continue;
                }

                int32_t data_size = 0;
                if(HAS_FUNC(sp_av_samples_get_buffer_size)) {
                    data_size = sp_av_samples_get_buffer_size(NULL /* linesize, may be NULL */,
                                                              pAV->aChannels,
                                                              pAFrameCurrent->nb_samples,
                                                              pAFrameCurrent->format,
                                                              1 /* align */);
                }
                #if 0
                fprintf(stderr, "channels %d sample_rate %d \n", pAV->aChannels , pAV->aSampleRate);
                fprintf(stderr, "data %d \n", pAV->aFrameSize); 
                #endif

                const AVRational time_base = pAV->pAStream->time_base;
                const int64_t pkt_pts = pAFrameCurrent->pkt_pts;
                if( 0 == frameCount && AV_NOPTS_VALUE != pkt_pts ) { // 1st frame only, discard invalid PTS ..
                    pAV->aPTS = my_av_q2i32( pkt_pts * 1000, time_base);
                } else { // subsequent frames or invalid PTS ..
                    const int32_t bytesPerSample = 2; // av_get_bytes_per_sample( pAV->pACodecCtx->sample_fmt );
                    pAV->aPTS += data_size / ( pAV->aChannels * bytesPerSample * ( pAV->aSampleRate / 1000 ) );
                }
                if( pAV->verbose ) {
                    int32_t aDTS = my_av_q2i32( pAFrameCurrent->pkt_dts * 1000, time_base);

                    fprintf(stderr, "A pts %d [pkt_pts %ld], dts %d [pkt_dts %ld], f# %d, aFrame %d/%d %p, dataPtr %p, dataSize %d\n", 
                        pAV->aPTS, pkt_pts, aDTS, pAFrameCurrent->pkt_dts, frameCount,
                        pAV->aFrameCurrent, pAV->aFrameCount, pAFrameCurrent, pAFrameCurrent->data[0], data_size);
                }
                if( NULL != env ) {
                    NIOBuffer_t * pNIOBufferCurrent = &pAV->pANIOBuffers[pAV->aFrameCurrent];
                    int newNIO = NULL == pNIOBufferCurrent->nioRef;
                    if( !newNIO && ( pAFrameCurrent->data[0] != pNIOBufferCurrent->origPtr || data_size > pNIOBufferCurrent->size ) ) {
                        if(pAV->verbose) {
                            fprintf(stderr, "A NIO: Free.0 ptr %p / ref %p, %d bytes\n", 
                                pNIOBufferCurrent->origPtr, pNIOBufferCurrent->nioRef, pNIOBufferCurrent->size);
                        }
                        (*env)->DeleteGlobalRef(env, pNIOBufferCurrent->nioRef);
                        newNIO = 1;
                    }
                    if( newNIO ) {
                        jobject jSampleData = (*env)->NewDirectByteBuffer(env, pAFrameCurrent->data[0], data_size);
                        pNIOBufferCurrent->nioRef = (*env)->NewGlobalRef(env, jSampleData);
                        pNIOBufferCurrent->origPtr = pAFrameCurrent->data[0];
                        pNIOBufferCurrent->size = data_size;
                        if(pAV->verbose) {
                            fprintf(stderr, "A NIO: Alloc ptr %p / ref %p, %d bytes\n", 
                                pNIOBufferCurrent->origPtr, pNIOBufferCurrent->nioRef, pNIOBufferCurrent->size);
                        }
                    }
                    (*env)->CallVoidMethod(env, instance, jni_mid_pushSound, pNIOBufferCurrent->nioRef, data_size, pAV->aPTS);
                }
            }
        } else if(packet.stream_index==pAV->vid) {
            // Decode video frame
            if(NULL == pAV->pVFrame) {
                sp_av_free_packet(&packet);
                return 0;
            }
            int frameCount;
            int flush_complete = 0;
            for ( frameCount=0; 0 < packet.size || 0 == frameCount; frameCount++ ) {
                int len1;
                if (flush_complete) {
                    break;
                }
                sp_avcodec_get_frame_defaults(pAV->pVFrame);
                len1 = sp_avcodec_decode_video2(pAV->pVCodecCtx, pAV->pVFrame, &frameDecoded, &packet);
                if (len1 < 0) {
                    // if error, we skip the frame
                    packet.size = 0;
                    break;
                }
                packet.data += len1;
                packet.size -= len1;

                if (!frameDecoded) {
                    // stop sending empty packets if the decoder is finished
                    if (!packet.data && pAV->pVCodecCtx->codec->capabilities & CODEC_CAP_DELAY) {
                        flush_complete = 1;
                    }
                    continue;
                }

                // FIXME: Libav Binary compatibility! JAU01
                const AVRational time_base = pAV->pVStream->time_base;
                const int64_t pkt_pts = pAV->pVFrame->pkt_pts;
                const int64_t pkt_dts = pAV->pVFrame->pkt_dts;
                const int64_t fix_pts = evalPTS(&pAV->vPTSStats, pkt_pts, pkt_dts);
                if( AV_NOPTS_VALUE != fix_pts ) { // discard invalid PTS ..
                    pAV->vPTS =  my_av_q2i32( fix_pts * 1000, time_base);
                }
                if( pAV->verbose ) {
                    const int32_t vPTS = AV_NOPTS_VALUE != pkt_pts ? my_av_q2i32( pkt_pts * 1000, time_base) : 0;
                    const int32_t vDTS = AV_NOPTS_VALUE != pkt_dts ? my_av_q2i32( pkt_dts * 1000, time_base) : 0;

                    const double frame_delay_d = av_q2d(pAV->pVCodecCtx->time_base);
                    const double frame_repeat_d = pAV->pVFrame->repeat_pict * (frame_delay_d * 0.5);

                    const int32_t frame_delay_i = my_av_q2i32(1000, pAV->pVCodecCtx->time_base);
                    const int32_t frame_repeat_i = pAV->pVFrame->repeat_pict * (frame_delay_i / 2);

                    const char * warn = frame_repeat_i > 0 ? "REPEAT" : "NORMAL" ;

                    fprintf(stderr, "V fix_pts %d, pts %d [pkt_pts %ld], dts %d [pkt_dts %ld], time d(%lf s + r %lf = %lf s), i(%d ms + r %d = %d ms) - %s - f# %d\n", 
                            pAV->vPTS, vPTS, pkt_pts, vDTS, pkt_dts, 
                            frame_delay_d, frame_repeat_d, (frame_delay_d + frame_repeat_d),
                            frame_delay_i, frame_repeat_i, (frame_delay_i + frame_repeat_i), warn, frameCount);
                }
                resPTS = pAV->vPTS; // Video Frame!

                // 1st plane or complete packed frame
                // FIXME: Libav Binary compatibility! JAU01
                DBG_TEXSUBIMG2D_a('Y',pAV,0);
                pAV->procAddrGLTexSubImage2D(texTarget, 0, 
                                        0,                 0, 
                                        pAV->vTexWidth[0], pAV->pVCodecCtx->height, 
                                        texFmt, texType, pAV->pVFrame->data[0]);
                DBG_TEXSUBIMG2D_b(pAV);

                if(pAV->vPixFmt == PIX_FMT_YUV420P) {
                    // U plane
                    // FIXME: Libav Binary compatibility! JAU01
                    DBG_TEXSUBIMG2D_a('U',pAV,1);
                    pAV->procAddrGLTexSubImage2D(texTarget, 0, 
                                            pAV->pVCodecCtx->width, 0,
                                            pAV->vTexWidth[1],      pAV->pVCodecCtx->height/2, 
                                            texFmt, texType, pAV->pVFrame->data[1]);
                    DBG_TEXSUBIMG2D_b(pAV);
                    // V plane
                    // FIXME: Libav Binary compatibility! JAU01
                    DBG_TEXSUBIMG2D_a('V',pAV,2);
                    pAV->procAddrGLTexSubImage2D(texTarget, 0, 
                                            pAV->pVCodecCtx->width, pAV->pVCodecCtx->height/2,
                                            pAV->vTexWidth[2],      pAV->pVCodecCtx->height/2, 
                                            texFmt, texType, pAV->pVFrame->data[2]);
                    DBG_TEXSUBIMG2D_b(pAV);
                } // FIXME: Add more planar formats !
                pAV->procAddrGLFinish();
                //pAV->procAddrGLFlush();
                if( pAV->useRefCountedFrames ) {
                    sp_av_frame_unref(pAV->pVFrame);
                }
            }
        }

        // Free the packet that was allocated by av_read_frame
        // This code cause a double free and have been commented out.
        // TODO: check what release the packets memory. 
        // sp_av_free_packet(&packet);
    }
    return resPTS;
}

static int countAudioPacketsTillVideo(const int maxPackets, FFMPEGToolBasicAV_t *pAV, AVPacket* pPacket, int packetFull, AVFrame* pAFrame, int * pAudioFrames, int *pMaxDataSize) {
    int frameDecoded;
    int audioFrames = 0;
    int maxDataSize = 0;
    int packetCount = 0;

    for( packetCount = 0; packetCount < maxPackets; packetCount++ ) {
        int readRes;
        if( !packetFull ) {
            sp_av_init_packet(pPacket);
            readRes = sp_av_read_frame(pAV->pFormatCtx, pPacket);
        } else {
            readRes = 1;
            packetFull = 0;
        }
        if( readRes >= 0 ) {
            if(pPacket->stream_index==pAV->aid) {
                // Decode audio frame
                int frameCount;
                int flush_complete = 0;
                for ( frameCount=0; 0 < pPacket->size || 0 == frameCount; frameCount++ ) {
                    int len1;
                    if (flush_complete) {
                        break;
                    }
                    if(HAS_FUNC(sp_avcodec_decode_audio4)) {
                        len1 = sp_avcodec_decode_audio4(pAV->pACodecCtx, pAFrame, &frameDecoded, pPacket);
                    } else {
                        #if 0
                        len1 = sp_avcodec_decode_audio3(pAV->pACodecCtx, int16_t *samples, int *frame_size_ptr, &frameDecoded, pPacket);
                        #endif
                        return -1;
                    }
                    if (len1 < 0) {
                        // if error, we skip the frame 
                        pPacket->size = 0;
                        break;
                    }
                    pPacket->data += len1;
                    pPacket->size -= len1;

                    if (!frameDecoded) {
                        // stop sending empty packets if the decoder is finished 
                        if (!pPacket->data && pAV->pACodecCtx->codec->capabilities & CODEC_CAP_DELAY) {
                            flush_complete = 1;
                        }
                        continue;
                    }

                    int32_t data_size = 0;
                    if(HAS_FUNC(sp_av_samples_get_buffer_size)) {
                        data_size = sp_av_samples_get_buffer_size(NULL /* linesize, may be NULL */,
                                                                  pAV->aChannels,
                                                                  pAFrame->nb_samples,
                                                                  pAFrame->format,
                                                                  1 /* align */);
                        if( data_size > maxDataSize ) {
                            maxDataSize = data_size;
                        }
                    }
                    if( pAV->useRefCountedFrames ) {
                        sp_av_frame_unref(pAFrame);
                    }
                    audioFrames++;
                }
            } else if(pPacket->stream_index==pAV->vid) {
                if( 0 < audioFrames ) {
                    break; // done!
                }
            }
        }
    }
    *pAudioFrames = audioFrames;
    *pMaxDataSize = maxDataSize;
    return packetCount;
}
static int countVideoPacketsTillAudio(const int maxPackets, FFMPEGToolBasicAV_t *pAV, AVPacket* pPacket, int packetFull, int * pVideoFrames) {
    int videoFrames = 0;
    int packetCount = 0;

    for( packetCount = 0; packetCount < maxPackets; packetCount++ ) {
        int readRes;
        if( !packetFull ) {
            sp_av_init_packet(pPacket);
            readRes = sp_av_read_frame(pAV->pFormatCtx, pPacket);
        } else {
            readRes = 1;
            packetFull = 0;
        }
        if( readRes >= 0 ) {
            if(pPacket->stream_index==pAV->aid) {
                if( 0 < videoFrames ) {
                    break; // done!
                }
            } else if(pPacket->stream_index==pAV->vid) {
                videoFrames++;
            }
        }
    }
    *pVideoFrames = videoFrames;
    return packetCount;
}
static void initPTSStats(PTSStats *ptsStats) {
    ptsStats->ptsError = 0;
    ptsStats->dtsError = 0;
    ptsStats->ptsLast = INT64_MIN;
    ptsStats->dtsLast = INT64_MIN;
}
static int64_t evalPTS(PTSStats *ptsStats, int64_t inPTS, int64_t inDTS) {
    int64_t resPTS = AV_NOPTS_VALUE;
            
    if ( inDTS != AV_NOPTS_VALUE ) {
        ptsStats->dtsError += inDTS <= ptsStats->dtsLast;
        ptsStats->dtsLast = inDTS;
    }       
    if ( inPTS != AV_NOPTS_VALUE ) {
        ptsStats->ptsError += inPTS <= ptsStats->ptsLast;
        ptsStats->ptsLast = inPTS;
    }       
    if ( inPTS != AV_NOPTS_VALUE &&
         ( ptsStats->ptsError<=ptsStats->dtsError || inDTS == AV_NOPTS_VALUE ) ) {
        resPTS = inPTS;
    } else {
        resPTS = inDTS;
    }
    return resPTS;
}           


JNIEXPORT jint JNICALL Java_jogamp_opengl_util_av_impl_FFMPEGMediaPlayer_play0
  (JNIEnv *env, jobject instance, jlong ptr)
{
    FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)((void *)((intptr_t)ptr));
    return sp_av_read_play(pAV->pFormatCtx);
}
JNIEXPORT jint JNICALL Java_jogamp_opengl_util_av_impl_FFMPEGMediaPlayer_pause0
  (JNIEnv *env, jobject instance, jlong ptr)
{
    FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)((void *)((intptr_t)ptr));
    return sp_av_read_pause(pAV->pFormatCtx);
}

JNIEXPORT jint JNICALL Java_jogamp_opengl_util_av_impl_FFMPEGMediaPlayer_seek0
  (JNIEnv *env, jobject instance, jlong ptr, jint pos1)
{
    const FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)((void *)((intptr_t)ptr));
    const int64_t pos0 = pAV->vPTS;
    int64_t pts0;
    int streamID;
    AVRational time_base;
    if( pAV->vid >= 0 ) {
        streamID = pAV->vid;
        time_base = pAV->pVStream->time_base;
        pts0 = pAV->pVFrame->pkt_pts;
    } else if( pAV->aid >= 0 ) {
        streamID = pAV->aid;
        time_base = pAV->pAStream->time_base;
        pts0 = pAV->pAFrames[pAV->aFrameCurrent]->pkt_pts;
    } else {
        return pAV->vPTS;
    }
    int64_t pts1 = (int64_t) (pos1 * (int64_t) time_base.den)
                           / (1000 * (int64_t) time_base.num);
    if(pAV->verbose) {
        fprintf(stderr, "SEEK: vid %d, aid %d, pos1 %d, pts: %ld -> %ld\n", pAV->vid, pAV->aid, pos1, pts0, pts1);
    }
    int flags = 0;
    if(pos1 < pos0) {
        flags |= AVSEEK_FLAG_BACKWARD;
    }
    int res;
    if(HAS_FUNC(sp_av_seek_frame)) {
        if(pAV->verbose) {
            fprintf(stderr, "SEEK.0: pre  : s %ld / %ld -> t %d / %ld\n", pos0, pts0, pos1, pts1);
        }
        sp_av_seek_frame(pAV->pFormatCtx, streamID, pts1, flags);
    } else if(HAS_FUNC(sp_avformat_seek_file)) {
        int64_t ptsD = pts1 - pts0;
        int64_t seek_min    = ptsD > 0 ? pts1 - ptsD : INT64_MIN;
        int64_t seek_max    = ptsD < 0 ? pts1 - ptsD : INT64_MAX;
        if(pAV->verbose) {
            fprintf(stderr, "SEEK.1: pre  : s %ld / %ld -> t %d / %ld [%ld .. %ld]\n", 
                pos0, pts0, pos1, pts1, seek_min, seek_max);
        }
        res = sp_avformat_seek_file(pAV->pFormatCtx, -1, seek_min, pts1, seek_max, flags);
    }
    if(NULL != pAV->pVCodecCtx) {
        sp_avcodec_flush_buffers( pAV->pVCodecCtx );
    }
    if(NULL != pAV->pACodecCtx) {
        sp_avcodec_flush_buffers( pAV->pACodecCtx );
    }
    const jint rPTS =  my_av_q2i32( ( pAV->vid >= 0 ? pAV->pVFrame->pkt_pts : pAV->pAFrames[pAV->aFrameCurrent]->pkt_pts ) * 1000, time_base);
    if(pAV->verbose) {
        fprintf(stderr, "SEEK: post : res %d, u %ld\n", res, rPTS);
    }
    return rPTS;
}

JNIEXPORT jint JNICALL Java_jogamp_opengl_util_av_impl_FFMPEGMediaPlayer_getVideoPTS0
  (JNIEnv *env, jobject instance, jlong ptr)
{
    FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)((void *)((intptr_t)ptr));
    return pAV->vPTS;
}

JNIEXPORT jint JNICALL Java_jogamp_opengl_util_av_impl_FFMPEGMediaPlayer_getAudioPTS0
  (JNIEnv *env, jobject instance, jlong ptr)
{
    FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)((void *)((intptr_t)ptr));
    return pAV->aPTS;
}

