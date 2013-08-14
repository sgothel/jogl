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
typedef int (APIENTRYP AVCODEC_DEFAULT_GET_BUFFER)(AVCodecContext *s, AVFrame *pic);
typedef void (APIENTRYP AVCODEC_DEFAULT_RELEASE_BUFFER)(AVCodecContext *s, AVFrame *pic);
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
static AVCODEC_DEFAULT_GET_BUFFER sp_avcodec_default_get_buffer;
static AVCODEC_DEFAULT_RELEASE_BUFFER sp_avcodec_default_release_buffer;
static AV_FREE_PACKET sp_av_free_packet;
static AVCODEC_DECODE_AUDIO4 sp_avcodec_decode_audio4;    // 53.25.0
static AVCODEC_DECODE_AUDIO3 sp_avcodec_decode_audio3;    // 52.23.0
static AVCODEC_DECODE_VIDEO2 sp_avcodec_decode_video2;    // 52.23.0
// count: 15

// libavutil
typedef void (APIENTRYP AV_FREE)(void *ptr);
typedef int (APIENTRYP AV_GET_BITS_PER_PIXEL)(const AVPixFmtDescriptor *pixdesc);
typedef int (APIENTRYP AV_SAMPLES_GET_BUFFER_SIZE)(int *linesize, int nb_channels, int nb_samples, enum AVSampleFormat sample_fmt, int align);
static const AVPixFmtDescriptor* sp_av_pix_fmt_descriptors;
static AV_FREE sp_av_free;
static AV_GET_BITS_PER_PIXEL sp_av_get_bits_per_pixel;
static AV_SAMPLES_GET_BUFFER_SIZE sp_av_samples_get_buffer_size;
// count: 19

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
static AVFORMAT_NETWORK_INIT sp_avformat_network_init;            // 53.13.0
static AVFORMAT_NETWORK_DEINIT sp_avformat_network_deinit;        // 53.13.0
static AVFORMAT_FIND_STREAM_INFO sp_avformat_find_stream_info;    // 53.3.0
static AV_FIND_STREAM_INFO sp_av_find_stream_info;
// count: 32

#define SYMBOL_COUNT 32

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
    sp_avcodec_default_get_buffer = (AVCODEC_DEFAULT_GET_BUFFER) (intptr_t) symbols[i++];
    sp_avcodec_default_release_buffer = (AVCODEC_DEFAULT_RELEASE_BUFFER) (intptr_t) symbols[i++];
    sp_av_free_packet = (AV_FREE_PACKET) (intptr_t) symbols[i++];
    sp_avcodec_decode_audio4 = (AVCODEC_DECODE_AUDIO4) (intptr_t) symbols[i++];
    sp_avcodec_decode_audio3 = (AVCODEC_DECODE_AUDIO3) (intptr_t) symbols[i++];
    sp_avcodec_decode_video2 = (AVCODEC_DECODE_VIDEO2) (intptr_t) symbols[i++];
    // count: 15

    sp_av_pix_fmt_descriptors = (const AVPixFmtDescriptor*)  (intptr_t) symbols[i++];
    sp_av_free = (AV_FREE) (intptr_t) symbols[i++];
    sp_av_get_bits_per_pixel = (AV_GET_BITS_PER_PIXEL) (intptr_t) symbols[i++];
    sp_av_samples_get_buffer_size = (AV_SAMPLES_GET_BUFFER_SIZE) (intptr_t) symbols[i++];
    // count: 19

    sp_avformat_alloc_context = (AVFORMAT_ALLOC_CONTEXT) (intptr_t) symbols[i++];;
    sp_avformat_free_context = (AVFORMAT_FREE_CONTEXT) (intptr_t) symbols[i++];
    sp_avformat_close_input = (AVFORMAT_CLOSE_INPUT) (intptr_t) symbols[i++];
    sp_av_close_input_file = (AV_CLOSE_INPUT_FILE) (intptr_t) symbols[i++];
    sp_av_register_all = (AV_REGISTER_ALL) (intptr_t) symbols[i++];
    sp_avformat_open_input = (AVFORMAT_OPEN_INPUT) (intptr_t) symbols[i++];
    sp_av_dump_format = (AV_DUMP_FORMAT) (intptr_t) symbols[i++];
    sp_av_read_frame = (AV_READ_FRAME) (intptr_t) symbols[i++];
    sp_av_seek_frame = (AV_SEEK_FRAME) (intptr_t) symbols[i++];
    sp_avformat_network_init = (AVFORMAT_NETWORK_INIT) (intptr_t) symbols[i++];
    sp_avformat_network_deinit = (AVFORMAT_NETWORK_DEINIT) (intptr_t) symbols[i++];
    sp_avformat_find_stream_info = (AVFORMAT_FIND_STREAM_INFO) (intptr_t) symbols[i++];
    sp_av_find_stream_info = (AV_FIND_STREAM_INFO) (intptr_t) symbols[i++];
    // count: 32

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

        (*env)->CallVoidMethod(env, instance, jni_mid_updateAttributes1,
                               pAV->vid, pAV->aid,
                               w, h, 
                               pAV->bps_stream, pAV->bps_video, pAV->bps_audio,
                               pAV->fps, pAV->frames_video, pAV->frames_audio, pAV->duration,
                               (*env)->NewStringUTF(env, pAV->vcodec),
                               (*env)->NewStringUTF(env, pAV->acodec) );
        (*env)->CallVoidMethod(env, instance, jni_mid_updateAttributes2,
                               pAV->vPixFmt, pAV->vBufferPlanes, 
                               pAV->vBitsPerPixel, pAV->vBytesPerPixelPerPlane,
                               pAV->vLinesize[0], pAV->vLinesize[1], pAV->vLinesize[2],
                               pAV->vTexWidth[0], pAV->vTexWidth[1], pAV->vTexWidth[2],
                               pAV->aSampleFmt, pAV->aSampleRate, pAV->aChannels);
    }
}

static void freeInstance(FFMPEGToolBasicAV_t* pAV) {
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
            sp_av_free(pAV->pVFrame);
            pAV->pVFrame = NULL;
        }
        if(NULL != pAV->pAFrames) {
            for(i=0; i<pAV->aFrameCount; i++) {
                sp_av_free(pAV->pAFrames[i]);
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
    jni_mid_updateAttributes2 = (*env)->GetMethodID(env, ffmpegMediaPlayerClazz, "updateAttributes2", "(IIIIIIIIIIIII)V");

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
      freeInstance(pAV);
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

JNIEXPORT void JNICALL Java_jogamp_opengl_util_av_impl_FFMPEGMediaPlayer_setStream0
  (JNIEnv *env, jobject instance, jlong ptr, jstring jURL, jint vid, jint aid, jint audioFrameCount,
   jint aChannelCount, jint aSampleRate)
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
        (*env)->ReleaseStringChars(env, jURL, (const jchar *)urlPath);
        JoglCommon_throwNewRuntimeException(env, "Couldn't open URL");
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
        pAV->pACodecCtx->thread_count=1;
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
        if(HAS_FUNC(sp_avcodec_open2)) {
            res = sp_avcodec_open2(pAV->pACodecCtx, pAV->pACodec, NULL);
        } else {
            res = sp_avcodec_open(pAV->pACodecCtx, pAV->pACodec);
        }
        if(res<0) {
            JoglCommon_throwNewRuntimeException(env, "Couldn't open audio codec %d, %s", pAV->pACodecCtx->codec_id, pAV->acodec);
            return;
        }

        // Allocate audio frames
        // FIXME: Libav Binary compatibility! JAU01
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

        pAV->aFrameCount = audioFrameCount;
        pAV->pAFrames = calloc(audioFrameCount, sizeof(AVFrame*));
        for(i=0; i<pAV->aFrameCount; i++) {
            pAV->pAFrames[i]=sp_avcodec_alloc_frame();
            if(pAV->pAFrames[i]==NULL) {
                JoglCommon_throwNewRuntimeException(env, "Couldn't alloc audio frame %d / %d", i, audioFrameCount);
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
        pAV->pVCodecCtx->thread_count=1;
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

        // Allocate video frames
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
    _updateJavaAttributes(env, instance, pAV);
}

JNIEXPORT void JNICALL Java_jogamp_opengl_util_av_impl_FFMPEGMediaPlayer_setGLFuncs0
  (JNIEnv *env, jobject instance, jlong ptr, jlong jProcAddrGLTexSubImage2D, jlong jProcAddrGLGetError)
{
    FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)((void *)((intptr_t)ptr));
    pAV->procAddrGLTexSubImage2D = (PFNGLTEXSUBIMAGE2DPROC) (intptr_t)jProcAddrGLTexSubImage2D;
    pAV->procAddrGLGetError = (PFNGLGETERRORPROC) (intptr_t)jProcAddrGLGetError;
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
    int frameFinished;
    jint resPTS = 0; // resulting current PTS: audio < 0, video > 0, invalid == 0

    if(sp_av_read_frame(pAV->pFormatCtx, &packet)>=0) {
        if(packet.stream_index==pAV->aid) {
            // Decode audio frame
            if(NULL == pAV->pAFrames) { // no audio registered
                sp_av_free_packet(&packet);
                return 0;
            }
            AVFrame* pAFrameCurrent = pAV->pAFrames[pAV->aFrameCurrent];
            pAV->aFrameCurrent = ( pAV->aFrameCurrent + 1 ) % pAV->aFrameCount ;
            int frameCount;
            int flush_complete = 0;
            for ( frameCount=0; 0 < packet.size || 0 == frameCount; frameCount++ ) {
                int len1;
                if (flush_complete) {
                    break;
                }
                if(HAS_FUNC(sp_avcodec_decode_audio4)) {
                    len1 = sp_avcodec_decode_audio4(pAV->pACodecCtx, pAFrameCurrent, &frameFinished, &packet);
                } else {
                    #if 0
                    len1 = sp_avcodec_decode_audio3(pAV->pACodecCtx, int16_t *samples, int *frame_size_ptr, &frameFinished, &packet);
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

                if (!frameFinished) {
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
                int aptsMode;
                if( 0 == frameCount && AV_NOPTS_VALUE != pkt_pts ) { // 1st frame only, discard invalid PTS ..
                    pAV->aPTS = (pkt_pts * (int64_t) 1000 * (int64_t) time_base.num) / (int64_t) time_base.den ;
                    aptsMode = 0;
                } else { // subsequent frames or invalid PTS ..
                    const int32_t bytesPerSample = 2; // av_get_bytes_per_sample( pAV->pACodecCtx->sample_fmt );
                    pAV->aPTS += data_size / ( pAV->aChannels * bytesPerSample * ( pAV->aSampleRate / 1000 ) );
                    aptsMode = 1;
                }
                if( pAV->verbose ) {
                    fprintf(stderr, "A pts %d [pkt_pts %ld, pkt_dts %ld], dataSize %d, f# %d, pts-mode %d\n", 
                        pAV->aPTS, pkt_pts, pAFrameCurrent->pkt_dts, data_size, frameCount, aptsMode);
                }
                if( NULL != env ) {
                    jobject jSampleData = (*env)->NewDirectByteBuffer(env, pAFrameCurrent->data[0], data_size);
                    (*env)->CallVoidMethod(env, instance, jni_mid_pushSound, jSampleData, data_size, pAV->aPTS);
                }
                resPTS = pAV->aPTS * -1; // Audio Frame!
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
                len1 = sp_avcodec_decode_video2(pAV->pVCodecCtx, pAV->pVFrame, &frameFinished, &packet);
                if (len1 < 0) {
                    // if error, we skip the frame
                    packet.size = 0;
                    break;
                }
                packet.data += len1;
                packet.size -= len1;

                if (!frameFinished) {
                    // stop sending empty packets if the decoder is finished
                    if (!packet.data && pAV->pVCodecCtx->codec->capabilities & CODEC_CAP_DELAY) {
                        flush_complete = 1;
                    }
                    continue;
                }

                // FIXME: Libav Binary compatibility! JAU01
                const AVRational time_base = pAV->pVStream->time_base;
                const int64_t pkt_pts = pAV->pVFrame->pkt_pts;
                if(AV_NOPTS_VALUE != pkt_pts) { // discard invalid PTS ..
                    int32_t vPTS2 = (pAV->pVFrame->pkt_dts * (int64_t) 1000 * (int64_t) time_base.num) / (int64_t) time_base.den ;
                    pAV->vPTS = (pkt_pts * (int64_t) 1000 * (int64_t) time_base.num) / (int64_t) time_base.den ;
                    if( pAV->verbose ) {
                        fprintf(stderr, "V pts %d [pkt_pts %ld], pts2 %d [pkt_dts %ld]\n", pAV->vPTS, pkt_pts, vPTS2, pAV->pVFrame->pkt_dts);
                    }
                } else {
                    if( pAV->verbose ) {
                        fprintf(stderr, "V pts ?? [pkt_pts %ld], pts2 ?? [pkt_dts %ld]\n", pkt_pts, pAV->pVFrame->pkt_dts);
                    }
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
            }
        }

        // Free the packet that was allocated by av_read_frame
        // This code cause a double free and have been commented out.
        // TODO: check what release the packets memory. 
        // sp_av_free_packet(&packet);
    }
    return resPTS;
}

JNIEXPORT jint JNICALL Java_jogamp_opengl_util_av_impl_FFMPEGMediaPlayer_seek0
  (JNIEnv *env, jobject instance, jlong ptr, jint pos1)
{
    FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)((void *)((intptr_t)ptr));
    int64_t pos0 = pAV->vPTS;
    int64_t pts0 = pAV->pVFrame->pkt_pts;
    int64_t pts1 = (int64_t) (pos1 * (int64_t) pAV->pVStream->time_base.den)
                             / (1000 * (int64_t) pAV->pVStream->time_base.num);
    int flags = 0;
    if(pos1 < pos0) {
        flags |= AVSEEK_FLAG_BACKWARD;
    }
    fprintf(stderr, "SEEK: pre  : u %ld, p %ld -> u %ld, p %ld\n", pos0, pts0, pos1, pts1);
    sp_av_seek_frame(pAV->pFormatCtx, pAV->vid, pts1, flags);
    pAV->vPTS = (int64_t) (pAV->pVFrame->pkt_pts * (int64_t) 1000 * (int64_t) pAV->pVStream->time_base.num)
                / (int64_t) pAV->pVStream->time_base.den;
    fprintf(stderr, "SEEK: post : u %ld, p %ld\n", pAV->vPTS, pAV->pVFrame->pkt_pts);
    return pAV->vPTS;
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

