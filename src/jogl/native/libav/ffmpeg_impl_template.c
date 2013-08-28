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
 
// #define FF_FUNC(METHOD) Java_jogamp_opengl_util_av_impl_FFMPEGv08 ## METHOD

#include "JoglCommon.h"
#include "ffmpeg_tool.h"

#include "libavutil/pixdesc.h"
#include "libavutil/samplefmt.h"
#if LIBAVUTIL_VERSION_MAJOR < 53
    #include "libavutil/audioconvert.h"
    // 52: #include "libavutil/channel_layout.h"
#endif

#include <GL/gl.h>

static const char * const ClazzNameFFMPEGMediaPlayer = "jogamp/opengl/util/av/impl/FFMPEGMediaPlayer";

static jclass ffmpegMediaPlayerClazz = NULL;
static jmethodID jni_mid_pushSound = NULL;
static jmethodID jni_mid_updateAttributes1 = NULL;
static jmethodID jni_mid_updateAttributes2 = NULL;
static jmethodID jni_mid_isAudioFormatSupported = NULL;

#define HAS_FUNC(f) (NULL!=(f))

typedef unsigned (APIENTRYP AVCODEC_VERSION)(void);
typedef unsigned (APIENTRYP AVUTIL_VERSION)(void);
typedef unsigned (APIENTRYP AVFORMAT_VERSION)(void);
typedef unsigned (APIENTRYP AVRESAMPLE_VERSION)(void);

static AVCODEC_VERSION sp_avcodec_version;
static AVFORMAT_VERSION sp_avformat_version; 
static AVUTIL_VERSION sp_avutil_version;
static AVRESAMPLE_VERSION sp_avresample_version;
// count: 4

// libavcodec
typedef int (APIENTRYP AVCODEC_REGISTER_ALL)(void);
typedef int (APIENTRYP AVCODEC_CLOSE)(AVCodecContext *avctx);
typedef void (APIENTRYP AVCODEC_STRING)(char *buf, int buf_size, AVCodecContext *enc, int encode);
typedef AVCodec *(APIENTRYP AVCODEC_FIND_DECODER)(enum CodecID id);
typedef int (APIENTRYP AVCODEC_OPEN2)(AVCodecContext *avctx, AVCodec *codec, AVDictionary **options);                          // 53.6.0
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
typedef int (APIENTRYP AVCODEC_DECODE_VIDEO2)(AVCodecContext *avctx, AVFrame *picture, int *got_picture_ptr, AVPacket *avpkt); // 52.23.0

static AVCODEC_REGISTER_ALL sp_avcodec_register_all;
static AVCODEC_CLOSE sp_avcodec_close;
static AVCODEC_STRING sp_avcodec_string;
static AVCODEC_FIND_DECODER sp_avcodec_find_decoder;
static AVCODEC_OPEN2 sp_avcodec_open2;                    // 53.6.0
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
static AVCODEC_DECODE_VIDEO2 sp_avcodec_decode_video2;    // 52.23.0
// count: 21

// libavutil
typedef void (APIENTRYP AV_FRAME_UNREF)(AVFrame *frame);
typedef void* (APIENTRYP AV_REALLOC)(void *ptr, size_t size);
typedef void (APIENTRYP AV_FREE)(void *ptr);
typedef int (APIENTRYP AV_GET_BITS_PER_PIXEL)(const AVPixFmtDescriptor *pixdesc);
typedef int (APIENTRYP AV_SAMPLES_GET_BUFFER_SIZE)(int *linesize, int nb_channels, int nb_samples, enum AVSampleFormat sample_fmt, int align);
typedef int (APIENTRYP AV_GET_BYTES_PER_SAMPLE)(enum AVSampleFormat sample_fmt);
typedef int (APIENTRYP AV_OPT_SET_INT)(void *obj, const char *name, int64_t val, int search_flags);
typedef AVDictionaryEntry* (APIENTRYP AV_DICT_GET)(AVDictionary *m, const char *key, const AVDictionaryEntry *prev, int flags);
typedef int (APIENTRYP AV_DICT_COUNT)(AVDictionary **m);
typedef int (APIENTRYP AV_DICT_SET)(AVDictionary **pm, const char *key, const char *value, int flags);
typedef void (APIENTRYP AV_DICT_FREE)(AVDictionary **m);

static const AVPixFmtDescriptor* sp_av_pix_fmt_descriptors;
static AV_FRAME_UNREF sp_av_frame_unref;
static AV_REALLOC sp_av_realloc;
static AV_FREE sp_av_free;
static AV_GET_BITS_PER_PIXEL sp_av_get_bits_per_pixel;
static AV_SAMPLES_GET_BUFFER_SIZE sp_av_samples_get_buffer_size;
static AV_GET_BYTES_PER_SAMPLE sp_av_get_bytes_per_sample;
static AV_OPT_SET_INT sp_av_opt_set_int;
static AV_DICT_GET sp_av_dict_get;
static AV_DICT_COUNT sp_av_dict_count;
static AV_DICT_SET sp_av_dict_set;
static AV_DICT_FREE sp_av_dict_free;
// count: 33

// libavformat
typedef AVFormatContext *(APIENTRYP AVFORMAT_ALLOC_CONTEXT)(void);
typedef void (APIENTRYP AVFORMAT_FREE_CONTEXT)(AVFormatContext *s);  // 52.96.0
typedef void (APIENTRYP AVFORMAT_CLOSE_INPUT)(AVFormatContext **s);  // 53.17.0
typedef void (APIENTRYP AV_REGISTER_ALL)(void);
typedef AVInputFormat *(APIENTRYP AV_FIND_INPUT_FORMAT)(const char *short_name);
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

static AVFORMAT_ALLOC_CONTEXT sp_avformat_alloc_context;
static AVFORMAT_FREE_CONTEXT sp_avformat_free_context;            // 52.96.0 (not used, only for outfile cts)
static AVFORMAT_CLOSE_INPUT sp_avformat_close_input;              // 53.17.0
static AV_REGISTER_ALL sp_av_register_all;
static AV_FIND_INPUT_FORMAT sp_av_find_input_format;
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
// count: 48

// libavdevice [53.0.0]
typedef int (APIENTRYP AVDEVICE_REGISTER_ALL)(void);
static AVDEVICE_REGISTER_ALL sp_avdevice_register_all;
// count: 49

// libavresample [1.0.1]
typedef AVAudioResampleContext* (APIENTRYP AVRESAMPLE_ALLOC_CONTEXT)(void);  // 1.0.1
typedef int (APIENTRYP AVRESAMPLE_OPEN)(AVAudioResampleContext *avr);  // 1.0.1
typedef void (APIENTRYP AVRESAMPLE_CLOSE)(AVAudioResampleContext *avr);  // 1.0.1
typedef void (APIENTRYP AVRESAMPLE_FREE)(AVAudioResampleContext **avr);  // 1.0.1
typedef int (APIENTRYP AVRESAMPLE_CONVERT)(AVAudioResampleContext *avr, uint8_t **output,
                      int out_plane_size, int out_samples, uint8_t **input,
                      int in_plane_size, int in_samples);  // 1.0.1
static AVRESAMPLE_ALLOC_CONTEXT sp_avresample_alloc_context;
static AVRESAMPLE_OPEN sp_avresample_open;
static AVRESAMPLE_CLOSE sp_avresample_close;
static AVRESAMPLE_FREE sp_avresample_free;
static AVRESAMPLE_CONVERT sp_avresample_convert;
// count: 54

#define SYMBOL_COUNT 54

JNIEXPORT jboolean JNICALL FF_FUNC(initSymbols0)
  (JNIEnv *env, jobject instance, jobject jSymbols, jint count)
{
    int64_t* symbols; // jlong -> int64_t -> intptr_t -> FUNC_PTR
    int i;

    if(SYMBOL_COUNT != count) {
        fprintf(stderr, "FFMPEGNatives.initSymbols0: Wrong symbol count: Expected %d, Is %d\n", 
                SYMBOL_COUNT, count);
        return JNI_FALSE;
    }
    JoglCommon_init(env);

    i = 0;
    symbols = (int64_t *) (*env)->GetPrimitiveArrayCritical(env, jSymbols, NULL);

    sp_avcodec_version = (AVCODEC_VERSION) (intptr_t) symbols[i++];
    sp_avformat_version = (AVFORMAT_VERSION) (intptr_t) symbols[i++];
    sp_avutil_version = (AVUTIL_VERSION) (intptr_t) symbols[i++];
    sp_avresample_version = (AVRESAMPLE_VERSION) (intptr_t) symbols[i++];

    sp_avcodec_register_all = (AVCODEC_REGISTER_ALL)  (intptr_t) symbols[i++];
    sp_avcodec_close = (AVCODEC_CLOSE)  (intptr_t) symbols[i++];
    sp_avcodec_string = (AVCODEC_STRING) (intptr_t) symbols[i++];
    sp_avcodec_find_decoder = (AVCODEC_FIND_DECODER) (intptr_t) symbols[i++];
    sp_avcodec_open2 = (AVCODEC_OPEN2) (intptr_t) symbols[i++];
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
    sp_avcodec_decode_video2 = (AVCODEC_DECODE_VIDEO2) (intptr_t) symbols[i++];

    sp_av_pix_fmt_descriptors = (const AVPixFmtDescriptor*)  (intptr_t) symbols[i++];
    sp_av_frame_unref = (AV_FRAME_UNREF) (intptr_t) symbols[i++];
    sp_av_realloc = (AV_REALLOC) (intptr_t) symbols[i++];
    sp_av_free = (AV_FREE) (intptr_t) symbols[i++];
    sp_av_get_bits_per_pixel = (AV_GET_BITS_PER_PIXEL) (intptr_t) symbols[i++];
    sp_av_samples_get_buffer_size = (AV_SAMPLES_GET_BUFFER_SIZE) (intptr_t) symbols[i++];
    sp_av_get_bytes_per_sample = (AV_GET_BYTES_PER_SAMPLE) (intptr_t) symbols[i++];
    sp_av_opt_set_int = (AV_OPT_SET_INT) (intptr_t) symbols[i++];
    sp_av_dict_get = (AV_DICT_GET) (intptr_t) symbols[i++];
    sp_av_dict_count = (AV_DICT_COUNT) (intptr_t) symbols[i++];
    sp_av_dict_set = (AV_DICT_SET) (intptr_t) symbols[i++];
    sp_av_dict_free = (AV_DICT_FREE) (intptr_t) symbols[i++];

    sp_avformat_alloc_context = (AVFORMAT_ALLOC_CONTEXT) (intptr_t) symbols[i++];;
    sp_avformat_free_context = (AVFORMAT_FREE_CONTEXT) (intptr_t) symbols[i++];
    sp_avformat_close_input = (AVFORMAT_CLOSE_INPUT) (intptr_t) symbols[i++];
    sp_av_register_all = (AV_REGISTER_ALL) (intptr_t) symbols[i++];
    sp_av_find_input_format = (AV_FIND_INPUT_FORMAT) (intptr_t) symbols[i++];
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

    sp_avdevice_register_all = (AVDEVICE_REGISTER_ALL) (intptr_t) symbols[i++];

    sp_avresample_alloc_context = (AVRESAMPLE_ALLOC_CONTEXT) (intptr_t) symbols[i++];
    sp_avresample_open = (AVRESAMPLE_OPEN) (intptr_t) symbols[i++];
    sp_avresample_close = (AVRESAMPLE_CLOSE) (intptr_t) symbols[i++];
    sp_avresample_free = (AVRESAMPLE_FREE) (intptr_t) symbols[i++];
    sp_avresample_convert = (AVRESAMPLE_CONVERT) (intptr_t) symbols[i++];

    (*env)->ReleasePrimitiveArrayCritical(env, jSymbols, symbols, 0);

    if(SYMBOL_COUNT != i) {
        // boom
        fprintf(stderr, "FFMPEGNatives.initSymbols0: Wrong symbol assignment count: Expected %d, Is %d\n", 
                SYMBOL_COUNT, i);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

static int _isAudioFormatSupported(JNIEnv *env, jobject ffmpegMediaPlayer, enum AVSampleFormat aSampleFmt, int32_t aSampleRate, int32_t aChannels)
{
    return JNI_TRUE == (*env)->CallBooleanMethod(env, ffmpegMediaPlayer, jni_mid_isAudioFormatSupported, aSampleFmt, aSampleRate, aChannels);
}
static void _updateJavaAttributes(JNIEnv *env, jobject instance, FFMPEGToolBasicAV_t* pAV)
{
    // int shallBeDetached = 0;
    // JNIEnv  * env = JoglCommon_GetJNIEnv (&shallBeDetached); 
    if(NULL!=env) {
        (*env)->CallVoidMethod(env, pAV->ffmpegMediaPlayer, jni_mid_updateAttributes2,
                               pAV->vid, pAV->vPixFmt, pAV->vBufferPlanes, 
                               pAV->vBitsPerPixel, pAV->vBytesPerPixelPerPlane,
                               pAV->vLinesize[0], pAV->vLinesize[1], pAV->vLinesize[2],
                               pAV->vTexWidth[0], pAV->vTexWidth[1], pAV->vTexWidth[2],
                               pAV->vWidth, pAV->vHeight,
                               pAV->aid, pAV->aSampleFmtOut, pAV->aSampleRateOut, pAV->aChannelsOut, pAV->aFrameSize);
        (*env)->CallVoidMethod(env, pAV->ffmpegMediaPlayer, jni_mid_updateAttributes1,
                               pAV->vid, pAV->aid,
                               pAV->vWidth, pAV->vHeight,
                               pAV->bps_stream, pAV->bps_video, pAV->bps_audio,
                               pAV->fps, pAV->frames_video, pAV->frames_audio, pAV->duration,
                               (*env)->NewStringUTF(env, pAV->vcodec),
                               (*env)->NewStringUTF(env, pAV->acodec) );
    }
}

static void freeInstance(JNIEnv *env, FFMPEGToolBasicAV_t* pAV) {
    int i;
    if(NULL != pAV) {
        // Close the A resampler
        if( NULL != pAV->aResampleCtx ) {
            sp_avresample_free(&pAV->aResampleCtx);
            pAV->aResampleCtx = NULL;
        }
        if( NULL != pAV->aResampleBuffer ) {
            sp_av_free(pAV->aResampleBuffer);
            pAV->aResampleBuffer = NULL;
        }

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
            sp_avformat_close_input(&pAV->pFormatCtx);
            // Only for output files!
            // sp_avformat_free_context(pAV->pFormatCtx);
            pAV->pFormatCtx = NULL;
        }
        if( NULL != pAV->ffmpegMediaPlayer ) {
            (*env)->DeleteGlobalRef(env, pAV->ffmpegMediaPlayer);
            pAV->ffmpegMediaPlayer = NULL;
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

JNIEXPORT jint JNICALL FF_FUNC(getAvUtilMajorVersionCC0)
  (JNIEnv *env, jobject instance) {
    return (jint) LIBAVUTIL_VERSION_MAJOR;
}

JNIEXPORT jint JNICALL FF_FUNC(getAvFormatMajorVersionCC0)
  (JNIEnv *env, jobject instance) {
    return (jint) LIBAVFORMAT_VERSION_MAJOR;
}

JNIEXPORT jint JNICALL FF_FUNC(getAvCodecMajorVersionCC0)
  (JNIEnv *env, jobject instance) {
    return (jint) LIBAVCODEC_VERSION_MAJOR;
}

JNIEXPORT jint JNICALL FF_FUNC(getAvResampleMajorVersionCC0)
  (JNIEnv *env, jobject instance) {
    return (jint) LIBAVRESAMPLE_VERSION_MAJOR;
}

JNIEXPORT jboolean JNICALL FF_FUNC(initIDs0)
  (JNIEnv *env, jobject instance)
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
    jni_mid_updateAttributes2 = (*env)->GetMethodID(env, ffmpegMediaPlayerClazz, "updateAttributes2", "(IIIIIIIIIIIIIIIIII)V");
    jni_mid_isAudioFormatSupported = (*env)->GetMethodID(env, ffmpegMediaPlayerClazz, "isAudioFormatSupported", "(III)Z");

    if(jni_mid_pushSound == NULL ||
       jni_mid_updateAttributes1 == NULL ||
       jni_mid_updateAttributes2 == NULL ||
       jni_mid_isAudioFormatSupported == NULL) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jlong JNICALL FF_FUNC(createInstance0)
  (JNIEnv *env, jobject instance, jobject ffmpegMediaPlayer, jboolean verbose)
{
    FFMPEGToolBasicAV_t * pAV = calloc(1, sizeof(FFMPEGToolBasicAV_t));
    if(NULL==pAV) {
        JoglCommon_throwNewRuntimeException(env, "Couldn't alloc instance");
        return 0;
    }
    pAV->avcodecVersion = sp_avcodec_version();
    pAV->avformatVersion = sp_avformat_version(); 
    pAV->avutilVersion = sp_avutil_version();
    if(HAS_FUNC(sp_avresample_version)) {
        pAV->avresampleVersion = sp_avresample_version();
    } else {
        pAV->avresampleVersion = 0;
    }

    #if LIBAVCODEC_VERSION_MAJOR >= 55
        // TODO: We keep code on using 1 a/v frame per decoding cycle now.
        //       This is compatible w/ OpenAL's alBufferData(..)
        //       and w/ OpenGL's texture update command, both copy data immediatly.
        // pAV->useRefCountedFrames = AV_HAS_API_REFCOUNTED_FRAMES(pAV);
        pAV->useRefCountedFrames = 0;
    #else
        pAV->useRefCountedFrames = 0;
    #endif

    pAV->ffmpegMediaPlayer = (*env)->NewGlobalRef(env, ffmpegMediaPlayer);
    pAV->verbose = verbose;
    pAV->vid=AV_STREAM_ID_AUTO;
    pAV->aid=AV_STREAM_ID_AUTO;

    return (jlong) (intptr_t) pAV;
}

JNIEXPORT void JNICALL FF_FUNC(destroyInstance0)
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

static void initPTSStats(PTSStats *ptsStats);
static int64_t evalPTS(PTSStats *ptsStats, int64_t inPTS, int64_t inDTS);

static AVInputFormat* tryAVInputFormat(const char * name, int verbose) {
    AVInputFormat* inFmt = sp_av_find_input_format(name);
    if( verbose) {
        if ( inFmt == NULL ) {
            fprintf(stderr, "Warning: Could not find input format '%s'\n", name);
        } else {
            fprintf(stderr, "Info: Found input format '%s'\n", name);
        }
    }
    return inFmt;
}
static const char * inFmtNames[] = {
    "video4linux2", // linux
    "video4linux",  // linux (old)
    "dshow",        // windows
    "vfwcap",       // windows (old)
    "mpg",
    "yuv2",
    "mjpeg",
    "avi",
    "wmv",
    "libx264",
    "h264",
    "mpegts"
};
static AVInputFormat* findAVInputFormat(int verbose) {
    AVInputFormat* inFmt = NULL;
    const char *inFmtName;
    int i=0;
    do {
        inFmtName = inFmtNames[i++];
        if( NULL == inFmtName ) {
            break;
        }
        inFmt = tryAVInputFormat(inFmtName, verbose);
    } while ( NULL == inFmt );
    return inFmt;
}

JNIEXPORT void JNICALL FF_FUNC(setStream0)
  (JNIEnv *env, jobject instance, jlong ptr, jstring jURL, jboolean jIsCameraInput, jint vid, jint aid,
   jint aMaxChannelCount, jint aPrefSampleRate)
{
    int res, i;
    jboolean iscopy;
    FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)(intptr_t)ptr;

    if (pAV == NULL) {
        JoglCommon_throwNewRuntimeException(env, "NULL AV ptr");
        return;
    }

    // Register all formats and codecs
    sp_avcodec_register_all();
    if( jIsCameraInput && HAS_FUNC(sp_avdevice_register_all) ) {
        sp_avdevice_register_all();
    }
    sp_av_register_all();
    // Network too ..
    if(HAS_FUNC(sp_avformat_network_init)) {
        sp_avformat_network_init();
    }

    pAV->pFormatCtx = sp_avformat_alloc_context();

    const char *urlPath = (*env)->GetStringUTFChars(env, jURL, &iscopy);

    // Open video file
    AVDictionary *inOpts = NULL;
    AVInputFormat* inFmt = NULL;
    if( jIsCameraInput ) {
        inFmt = findAVInputFormat(pAV->verbose);
        if( NULL == inFmt ) {
            JoglCommon_throwNewRuntimeException(env, "Couldn't find input format for camera: %s", urlPath);
            (*env)->ReleaseStringChars(env, jURL, (const jchar *)urlPath);
            return;
        }
        // set maximum values, driver shall 'degrade' ..
        // sp_av_dict_set(&inOpts, "video_size", "640x480", 0);
        // sp_av_dict_set(&inOpts, "video_size", "1280x720", 0);
        sp_av_dict_set(&inOpts, "video_size", "hd720", 0); // video4linux, vfwcap, ..
        // sp_av_dict_set(&inOpts, "video_size", "1280x1024", 0);
        // sp_av_dict_set(&inOpts, "video_size", "320x240", 0);
        sp_av_dict_set(&inOpts, "framerate", "60", 0); // not setting a framerate causes some drivers to crash!
    }
    res = sp_avformat_open_input(&pAV->pFormatCtx, urlPath, inFmt, NULL != inOpts ? &inOpts : NULL);
    if( NULL != inOpts ) {
        sp_av_dict_free(&inOpts);
    }
    if(res != 0) {
        JoglCommon_throwNewRuntimeException(env, "Couldn't open URI: %s, err %d", urlPath, res);
        (*env)->ReleaseStringChars(env, jURL, (const jchar *)urlPath);
        return;
    }

    // Retrieve detailed stream information
    if(sp_avformat_find_stream_info(pAV->pFormatCtx, NULL)<0) {
        (*env)->ReleaseStringChars(env, jURL, (const jchar *)urlPath);
        JoglCommon_throwNewRuntimeException(env, "Couldn't find stream information");
        return;
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

        // Note: OpenAL well supports n-channel by now (SOFT),
        //       however - AFAIK AV_SAMPLE_FMT_S16 would allow no conversion!
        pAV->pACodecCtx->request_sample_fmt=AV_SAMPLE_FMT_S16;
        if( 1 <= aMaxChannelCount && aMaxChannelCount <= 2 ) {
            pAV->pACodecCtx->request_channel_layout=getDefaultAudioChannelLayout(aMaxChannelCount);
            if( AV_HAS_API_REQUEST_CHANNELS(pAV) ) {
                pAV->pACodecCtx->request_channels=aMaxChannelCount;
            }
        }
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
        res = sp_avcodec_open2(pAV->pACodecCtx, pAV->pACodec, NULL);
        if(res<0) {
            JoglCommon_throwNewRuntimeException(env, "Couldn't open audio codec %d, %s", pAV->pACodecCtx->codec_id, pAV->acodec);
            return;
        }
        if (!pAV->pACodecCtx->channel_layout) {
            pAV->pACodecCtx->channel_layout = getDefaultAudioChannelLayout(pAV->pACodecCtx->channels);
        }
        if (!pAV->pACodecCtx->channel_layout) {
            JoglCommon_throwNewRuntimeException(env, "Couldn't determine channel layout of %d channels\n", pAV->pACodecCtx->channels);
            return;
        }
        pAV->aSampleRate = pAV->pACodecCtx->sample_rate;
        pAV->aChannels = pAV->pACodecCtx->channels;
        pAV->aFrameSize = pAV->pACodecCtx->frame_size; // in samples per channel!
        pAV->aSampleFmt = pAV->pACodecCtx->sample_fmt;
        pAV->frames_audio = pAV->pAStream->nb_frames;
        pAV->aSinkSupport = _isAudioFormatSupported(env, pAV->ffmpegMediaPlayer, pAV->aSampleFmt, pAV->aSampleRate, pAV->aChannels);
        if( pAV->verbose ) {
            fprintf(stderr, "A channels %d [l %d], sample_rate %d, frame_size %d, frame_number %d, r_frame_rate %f, avg_frame_rate %f, nb_frames %d, [maxChan %d, prefRate %d, req_chan_layout %d, req_chan %d], sink-support %d \n", 
                pAV->aChannels, pAV->pACodecCtx->channel_layout, pAV->aSampleRate, pAV->aFrameSize, pAV->pACodecCtx->frame_number,
                my_av_q2f(pAV->pAStream->r_frame_rate),
                my_av_q2f(pAV->pAStream->avg_frame_rate),
                pAV->pAStream->nb_frames,
                aMaxChannelCount, aPrefSampleRate, pAV->pACodecCtx->request_channel_layout, pAV->pACodecCtx->request_channels,
                pAV->aSinkSupport);
        }

        // default
        pAV->aSampleFmtOut = pAV->aSampleFmt;
        pAV->aChannelsOut = pAV->aChannels;
        pAV->aSampleRateOut = pAV->aSampleRate;

        if( AV_HAS_API_AVRESAMPLE(pAV) && 
            ( pAV->aSampleFmt != AV_SAMPLE_FMT_S16 || 
              ( 0 != aPrefSampleRate && pAV->aSampleRate != aPrefSampleRate ) || 
              !pAV->aSinkSupport ) 
          ) {
            if( 0 == aPrefSampleRate ) {
                aPrefSampleRate = pAV->aSampleRate;
            }
            int32_t aSinkSupport = 0;
            enum AVSampleFormat aSampleFmtOut = AV_SAMPLE_FMT_S16;
            int32_t aChannelsOut;
            int32_t aSampleRateOut;
            int32_t minChannelCount = MIN_INT(aMaxChannelCount,pAV->pACodecCtx->channels);
            
            if( _isAudioFormatSupported(env, pAV->ffmpegMediaPlayer, aSampleFmtOut, aPrefSampleRate, pAV->pACodecCtx->channels) ) {
                aChannelsOut = pAV->pACodecCtx->channels;
                aSampleRateOut = aPrefSampleRate;
                aSinkSupport = 1;
            } else if( _isAudioFormatSupported(env, pAV->ffmpegMediaPlayer, aSampleFmtOut, aPrefSampleRate, minChannelCount) ) {
                aChannelsOut = minChannelCount;
                aSampleRateOut = aPrefSampleRate;
                aSinkSupport = 1;
            }
            if( aSinkSupport ) {
                pAV->aResampleCtx = sp_avresample_alloc_context();
                sp_av_opt_set_int(pAV->aResampleCtx, "in_channel_layout",  pAV->pACodecCtx->channel_layout,            0);
                sp_av_opt_set_int(pAV->aResampleCtx, "out_channel_layout", getDefaultAudioChannelLayout(aChannelsOut), 0);
                sp_av_opt_set_int(pAV->aResampleCtx, "in_sample_rate",     pAV->aSampleRate,                           0);
                sp_av_opt_set_int(pAV->aResampleCtx, "out_sample_rate",    aSampleRateOut,                             0);
                sp_av_opt_set_int(pAV->aResampleCtx, "in_sample_fmt",      pAV->aSampleFmt,                            0);
                sp_av_opt_set_int(pAV->aResampleCtx, "out_sample_fmt",     aSampleFmtOut,                              0);

                if ( sp_avresample_open(pAV->aResampleCtx) < 0 ) {
                    sp_avresample_free(&pAV->aResampleCtx);
                    pAV->aResampleCtx = NULL;
                    fprintf(stderr, "error initializing libavresample\n");
                } else {
                    // OK
                    pAV->aSampleFmtOut = aSampleFmtOut;
                    pAV->aChannelsOut = aChannelsOut;
                    pAV->aSampleRateOut = aSampleRateOut;
                    pAV->aSinkSupport = 1;
                }
            }
        }

        // Allocate audio frames
        // FIXME: Libav Binary compatibility! JAU01
        pAV->aFrameCount = 1;
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
        res = sp_avcodec_open2(pAV->pVCodecCtx, pAV->pVCodec, NULL);
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
            
        // Allocate video frame
        // FIXME: Libav Binary compatibility! JAU01
        pAV->vWidth = pAV->pVCodecCtx->width;
        pAV->vHeight = pAV->pVCodecCtx->height;
        pAV->vPixFmt = pAV->pVCodecCtx->pix_fmt;
        {   
            AVPixFmtDescriptor pixDesc = sp_av_pix_fmt_descriptors[pAV->vPixFmt];
            pAV->vBitsPerPixel = sp_av_get_bits_per_pixel(&pixDesc);
            pAV->vBufferPlanes = my_getPlaneCount(&pixDesc);
        }

        if( pAV->verbose ) {
            fprintf(stderr, "V frame_size %d, frame_number %d, r_frame_rate %f %d/%d, avg_frame_rate %f %d/%d, nb_frames %d, size %dx%d, fmt 0x%X, bpp %d, planes %d\n", 
                pAV->pVCodecCtx->frame_size, pAV->pVCodecCtx->frame_number, 
                my_av_q2f(pAV->pVStream->r_frame_rate), pAV->pVStream->r_frame_rate.num, pAV->pVStream->r_frame_rate.den, 
                my_av_q2f(pAV->pVStream->avg_frame_rate), pAV->pVStream->avg_frame_rate.num, pAV->pVStream->avg_frame_rate.den,
                pAV->pVStream->nb_frames,
                pAV->vWidth, pAV->vHeight, pAV->vPixFmt, pAV->vBitsPerPixel, pAV->vBufferPlanes);
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
            if( pAV->vBufferPlanes > 1 ) {
                for(i=0; i<3; i++) {
                    // FIXME: Libav Binary compatibility! JAU01
                    pAV->vLinesize[i] = pAV->pVFrame->linesize[i];
                    pAV->vTexWidth[i] = pAV->vLinesize[i] / pAV->vBytesPerPixelPerPlane ;
                }
            } else {
                pAV->vLinesize[0] = pAV->pVCodecCtx->width * pAV->vBytesPerPixelPerPlane;
                if( pAV->vPixFmt == PIX_FMT_YUYV422 ) {
                    // Stuff 2x 16bpp (YUYV) into one RGBA pixel!
                    pAV->vTexWidth[0] = pAV->pVCodecCtx->width / 2;
                } else {
                    pAV->vTexWidth[0] = pAV->pVCodecCtx->width;
                }
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

JNIEXPORT void JNICALL FF_FUNC(setGLFuncs0)
  (JNIEnv *env, jobject instance, jlong ptr, jlong jProcAddrGLTexSubImage2D, jlong jProcAddrGLGetError, jlong jProcAddrGLFlush, jlong jProcAddrGLFinish)
{
    FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)((void *)((intptr_t)ptr));
    pAV->procAddrGLTexSubImage2D = (PFNGLTEXSUBIMAGE2DPROC) (intptr_t)jProcAddrGLTexSubImage2D;
    pAV->procAddrGLGetError = (PFNGLGETERRORPROC) (intptr_t)jProcAddrGLGetError;
    pAV->procAddrGLFlush = (PFNGLFLUSH) (intptr_t)jProcAddrGLFlush;
    pAV->procAddrGLFinish = (PFNGLFINISH) (intptr_t)jProcAddrGLFinish;
}

#if 0
#define DBG_TEXSUBIMG2D_a(c,p,w1,w2,h,i) fprintf(stderr, "TexSubImage2D.%c offset %d / %d, size %d x %d, ", c, (w1*p->pVCodecCtx->width)/w2, p->pVCodecCtx->height/h, p->vTexWidth[i], p->pVCodecCtx->height/h)
#define DBG_TEXSUBIMG2D_b(p) fprintf(stderr, "err 0x%X\n", pAV->procAddrGLGetError())
#else
#define DBG_TEXSUBIMG2D_a(c,p,w1,w2,h,i)
#define DBG_TEXSUBIMG2D_b(p)
#endif

JNIEXPORT jint JNICALL FF_FUNC(readNextPacket0)
  (JNIEnv *env, jobject instance, jlong ptr, jint texTarget, jint texFmt, jint texType)
{
    FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)((void *)((intptr_t)ptr));

    AVPacket packet;
    int frameDecoded;
    jint resPTS = INVALID_PTS;
    uint8_t * pkt_odata;
    int pkt_osize;

    packet.data = NULL; // minimum
    packet.size = 0;    // requirement
    sp_av_init_packet(&packet);

    const int avRes = sp_av_read_frame(pAV->pFormatCtx, &packet);
    pkt_odata = packet.data;
    pkt_osize = packet.size;
    if( AVERROR_EOF == avRes || ( pAV->pFormatCtx->pb && pAV->pFormatCtx->pb->eof_reached ) ) {
        resPTS = END_OF_STREAM_PTS;
    } else if( 0 <= avRes ) {
        if( pAV->verbose ) {
            fprintf(stderr, "P: ptr %p, size %d\n", packet.data, packet.size);
        }
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
                NIOBuffer_t * pNIOBufferCurrent = &pAV->pANIOBuffers[pAV->aFrameCurrent];
                AVFrame* pAFrameCurrent = pAV->pAFrames[pAV->aFrameCurrent];
                if( pAV->useRefCountedFrames ) {
                    sp_av_frame_unref(pAFrameCurrent);
                    pAV->aFrameCurrent = ( pAV->aFrameCurrent + 1 ) % pAV->aFrameCount ;
                }
                sp_avcodec_get_frame_defaults(pAFrameCurrent);

                if (flush_complete) {
                    break;
                }
                len1 = sp_avcodec_decode_audio4(pAV->pACodecCtx, pAFrameCurrent, &frameDecoded, &packet);
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
                    const int32_t bytesPerSample = sp_av_get_bytes_per_sample( pAV->pACodecCtx->sample_fmt );
                    pAV->aPTS += data_size / ( pAV->aChannels * bytesPerSample * ( pAV->aSampleRate / 1000 ) );
                }
                if( pAV->verbose ) {
                    int32_t aDTS = my_av_q2i32( pAFrameCurrent->pkt_dts * 1000, time_base);

                    fprintf(stderr, "A pts %d [pkt_pts %ld], dts %d [pkt_dts %ld], f# %d, aFrame %d/%d %p, dataPtr %p, dataSize %d\n", 
                        pAV->aPTS, pkt_pts, aDTS, pAFrameCurrent->pkt_dts, frameCount,
                        pAV->aFrameCurrent, pAV->aFrameCount, pAFrameCurrent, pAFrameCurrent->data[0], data_size);
                }
                if( NULL != env ) {
                    void* data_ptr = pAFrameCurrent->data[0]; // default

                    if( NULL != pAV->aResampleCtx ) {
                        enum AVSampleFormat aSampleFmtOut; // out fmt
                        int32_t          aChannelsOut;
                        int32_t          aSampleRateOut;

                        uint8_t *tmp_out;
                        int out_samples, out_size, out_linesize;
                        int osize      = sp_av_get_bytes_per_sample( pAV->aSampleFmtOut );
                        int nb_samples = pAFrameCurrent->nb_samples;

                        out_size = sp_av_samples_get_buffer_size(&out_linesize,
                                                                 pAV->aChannelsOut,
                                                                 nb_samples,
                                                                 pAV->aSampleFmtOut, 0 /* align */);

                        tmp_out = sp_av_realloc(pAV->aResampleBuffer, out_size);
                        if (!tmp_out) {
                            JoglCommon_throwNewRuntimeException(env, "Couldn't alloc resample buffer of size %d", out_size);
                            return;
                        }
                        pAV->aResampleBuffer = tmp_out;

                        out_samples = sp_avresample_convert(pAV->aResampleCtx,
                                                            &pAV->aResampleBuffer,
                                                            out_linesize, nb_samples,
                                                            pAFrameCurrent->data,
                                                            pAFrameCurrent->linesize[0],
                                                            pAFrameCurrent->nb_samples);
                        if (out_samples < 0) {
                            JoglCommon_throwNewRuntimeException(env, "avresample_convert() failed");
                            return;
                        }
                        data_size = out_samples * osize * pAV->aChannelsOut;
                        data_ptr = tmp_out;
                    }
                    NIOBuffer_t * pNIOBufferCurrent = &pAV->pANIOBuffers[pAV->aFrameCurrent];
                    int newNIO = NULL == pNIOBufferCurrent->nioRef;
                    if( !newNIO && ( data_ptr != pNIOBufferCurrent->origPtr || data_size > pNIOBufferCurrent->size ) ) {
                        if(pAV->verbose) {
                            fprintf(stderr, "A NIO: Free.0 ptr %p / ref %p, %d bytes\n", 
                                pNIOBufferCurrent->origPtr, pNIOBufferCurrent->nioRef, pNIOBufferCurrent->size);
                        }
                        (*env)->DeleteGlobalRef(env, pNIOBufferCurrent->nioRef);
                        newNIO = 1;
                    }
                    if( newNIO ) {
                        jobject jSampleData = (*env)->NewDirectByteBuffer(env, data_ptr, data_size);
                        pNIOBufferCurrent->nioRef = (*env)->NewGlobalRef(env, jSampleData);
                        pNIOBufferCurrent->origPtr = data_ptr;
                        pNIOBufferCurrent->size = data_size;
                        if(pAV->verbose) {
                            fprintf(stderr, "A NIO: Alloc ptr %p / ref %p, %d bytes\n", 
                                pNIOBufferCurrent->origPtr, pNIOBufferCurrent->nioRef, pNIOBufferCurrent->size);
                        }
                    }
                    (*env)->CallVoidMethod(env, pAV->ffmpegMediaPlayer, jni_mid_pushSound, pNIOBufferCurrent->nioRef, data_size, pAV->aPTS);
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
                sp_avcodec_get_frame_defaults(pAV->pVFrame);
                if (flush_complete) {
                    break;
                }
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
                DBG_TEXSUBIMG2D_a('Y',pAV,1,1,1,0);
                pAV->procAddrGLTexSubImage2D(texTarget, 0, 
                                        0,                 0, 
                                        pAV->vTexWidth[0], pAV->pVCodecCtx->height, 
                                        texFmt, texType, pAV->pVFrame->data[0]);
                DBG_TEXSUBIMG2D_b(pAV);

                if( pAV->vPixFmt == PIX_FMT_YUV420P || pAV->vPixFmt == PIX_FMT_YUVJ420P ) {
                    // U plane
                    // FIXME: Libav Binary compatibility! JAU01
                    DBG_TEXSUBIMG2D_a('U',pAV,1,1,2,1);
                    pAV->procAddrGLTexSubImage2D(texTarget, 0, 
                                            pAV->pVCodecCtx->width, 0,
                                            pAV->vTexWidth[1],      pAV->pVCodecCtx->height/2, 
                                            texFmt, texType, pAV->pVFrame->data[1]);
                    DBG_TEXSUBIMG2D_b(pAV);
                    // V plane
                    // FIXME: Libav Binary compatibility! JAU01
                    DBG_TEXSUBIMG2D_a('V',pAV,1,1,2,2);
                    pAV->procAddrGLTexSubImage2D(texTarget, 0, 
                                            pAV->pVCodecCtx->width, pAV->pVCodecCtx->height/2,
                                            pAV->vTexWidth[2],      pAV->pVCodecCtx->height/2, 
                                            texFmt, texType, pAV->pVFrame->data[2]);
                    DBG_TEXSUBIMG2D_b(pAV);
                } else if( pAV->vPixFmt == PIX_FMT_YUV422P || pAV->vPixFmt == PIX_FMT_YUVJ422P ) {
                    // U plane
                    // FIXME: Libav Binary compatibility! JAU01
                    DBG_TEXSUBIMG2D_a('U',pAV,1,1,1,1);
                    pAV->procAddrGLTexSubImage2D(texTarget, 0, 
                                            pAV->pVCodecCtx->width, 0,
                                            pAV->vTexWidth[1],      pAV->pVCodecCtx->height, 
                                            texFmt, texType, pAV->pVFrame->data[1]);
                    DBG_TEXSUBIMG2D_b(pAV);
                    // V plane
                    // FIXME: Libav Binary compatibility! JAU01
                    DBG_TEXSUBIMG2D_a('V',pAV,3,2,1,1);
                    pAV->procAddrGLTexSubImage2D(texTarget, 0, 
                                            pAV->pVCodecCtx->width+pAV->pVCodecCtx->width/2, 0,
                                            pAV->vTexWidth[2],      pAV->pVCodecCtx->height, 
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
        // restore orig pointer and size values, we may have moved along within packet
        packet.data = pkt_odata;
        packet.size = pkt_osize;
        sp_av_free_packet(&packet);
    }
    return resPTS;
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


JNIEXPORT jint JNICALL FF_FUNC(play0)
  (JNIEnv *env, jobject instance, jlong ptr)
{
    FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)((void *)((intptr_t)ptr));
    return sp_av_read_play(pAV->pFormatCtx);
}
JNIEXPORT jint JNICALL FF_FUNC(pause0)
  (JNIEnv *env, jobject instance, jlong ptr)
{
    FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)((void *)((intptr_t)ptr));
    return sp_av_read_pause(pAV->pFormatCtx);
}

JNIEXPORT jint JNICALL FF_FUNC(seek0)
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

JNIEXPORT jint JNICALL FF_FUNC(getVideoPTS0)
  (JNIEnv *env, jobject instance, jlong ptr)
{
    FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)((void *)((intptr_t)ptr));
    return pAV->vPTS;
}

JNIEXPORT jint JNICALL FF_FUNC(getAudioPTS0)
  (JNIEnv *env, jobject instance, jlong ptr)
{
    FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)((void *)((intptr_t)ptr));
    return pAV->aPTS;
}

