/**
 * Copyright 2012-2023 JogAmp Community. All rights reserved.
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
#include "ffmpeg_static.h"
#include "ffmpeg_dshow.h"

#include <GL/gl.h>

#define HAS_FUNC(f) (NULL!=(f))

typedef unsigned (APIENTRYP AVUTIL_VERSION)(void);
typedef unsigned (APIENTRYP AVFORMAT_VERSION)(void);
typedef unsigned (APIENTRYP AVCODEC_VERSION)(void);
typedef unsigned (APIENTRYP AVDEVICE_VERSION)(void);
typedef unsigned (APIENTRYP SWRESAMPLE_VERSION)(void);

static AVUTIL_VERSION sp_avutil_version;
static AVFORMAT_VERSION sp_avformat_version; 
static AVCODEC_VERSION sp_avcodec_version;
static AVDEVICE_VERSION sp_avdevice_version;
static SWRESAMPLE_VERSION sp_swresample_version;
// count: 5

// libavcodec
typedef int (APIENTRYP AVCODEC_CLOSE)(AVCodecContext *avctx);
typedef void (APIENTRYP AVCODEC_STRING)(char *buf, int buf_size, AVCodecContext *enc, int encode);
typedef AVCodec *(APIENTRYP AVCODEC_FIND_DECODER)(enum AVCodecID avCodecID); // lavc 53: 'enum CodecID id', lavc 54: 'enum AVCodecID id'
typedef AVCodecContext* (APIENTRYP AVCODEC_ALLOC_CONTEXT3)(const AVCodec* codec);
typedef void (APIENTRYP AVCODEC_FREE_CONTEXT)(AVCodecContext** avctx);
typedef int (APIENTRYP AVCODEC_PARAMTERS_TO_CONTEXT)(AVCodecContext *codec, const AVCodecParameters *par);
typedef int (APIENTRYP AVCODEC_OPEN2)(AVCodecContext *avctx, AVCodec *codec, AVDictionary **options);                          // 53.6.0
typedef AVFrame *(APIENTRYP AV_FRAME_ALLOC)(void); // 55.28.1
typedef void (APIENTRYP AV_FREE_FRAME)(AVFrame **frame); // 55.28.1
typedef int (APIENTRYP AVCODEC_DEFAULT_GET_BUFFER2)(AVCodecContext *s, AVFrame *frame, int flags); // 55.
typedef int (APIENTRYP AV_IMAGE_FILL_LINESIZES)(int linesizes[4], enum AVPixelFormat pix_fmt, int width); // lavu 51: 'enum PixelFormat pix_fmt', lavu 53: 'enum AVPixelFormat pix_fmt'
typedef void (APIENTRYP AVCODEC_FLUSH_BUFFERS)(AVCodecContext *avctx);
typedef AVPacket* (APIENTRYP AV_PACKET_ALLOC)(void);
typedef void (APIENTRYP AV_PACKET_FREE)(AVPacket **pkt);
typedef int (APIENTRYP AV_NEW_PACKET)(AVPacket *pkt, int size);
typedef void (APIENTRYP AV_PACKET_UNREF)(AVPacket *pkt);
typedef int (APIENTRYP AVCODEC_SEND_PACKET)(AVCodecContext *avctx, AVPacket *avpkt); // 57
typedef int (APIENTRYP AVCODEC_RECEIVE_FRAME)(AVCodecContext *avctx, AVFrame *picture); // 57
typedef int (APIENTRYP AVCODEC_DECODE_SUBTITLE2)(AVCodecContext *avctx, AVSubtitle *sub, int *got_sub_ptr, const AVPacket *avpkt); // 52.23
typedef int (APIENTRYP AV_SUBTITLE_FREE)(AVSubtitle *sub); // 52.82

static AVCODEC_CLOSE sp_avcodec_close;
static AVCODEC_STRING sp_avcodec_string;
static AVCODEC_FIND_DECODER sp_avcodec_find_decoder;
static AVCODEC_ALLOC_CONTEXT3 sp_avcodec_alloc_context3;
static AVCODEC_FREE_CONTEXT sp_avcodec_free_context;
static AVCODEC_PARAMTERS_TO_CONTEXT sp_avcodec_parameters_to_context;
static AVCODEC_OPEN2 sp_avcodec_open2;                    // 53.6.0
static AV_FRAME_ALLOC sp_av_frame_alloc;             // 55.28.1
static AV_FREE_FRAME sp_av_free_frame;                    // 55.28.1
static AVCODEC_DEFAULT_GET_BUFFER2 sp_avcodec_default_get_buffer2; // 55.
static AV_IMAGE_FILL_LINESIZES sp_av_image_fill_linesizes;
static AVCODEC_FLUSH_BUFFERS sp_avcodec_flush_buffers;
static AV_PACKET_ALLOC sp_av_packet_alloc; // sp_av_init_packet
static AV_PACKET_FREE sp_av_packet_free;
static AV_NEW_PACKET sp_av_new_packet;
static AV_PACKET_UNREF sp_av_packet_unref;

static AVCODEC_SEND_PACKET sp_avcodec_send_packet;    // 57
static AVCODEC_RECEIVE_FRAME sp_avcodec_receive_frame;    // 57
static AVCODEC_DECODE_SUBTITLE2 sp_avcodec_decode_subtitle2; // 52.23
static AV_SUBTITLE_FREE sp_avsubtitle_free; // 52.82
// count: +20 = 25

// libavutil
typedef AVPixFmtDescriptor* (APIENTRYP AV_PIX_FMT_DESC_GET)(enum AVPixelFormat pix_fmt); // lavu >= 51.45;  lavu 51: 'enum PixelFormat pix_fmt', lavu 53: 'enum AVPixelFormat pix_fmt'
typedef void (APIENTRYP AV_FRAME_UNREF)(AVFrame *frame);
typedef void* (APIENTRYP AV_REALLOC)(void *ptr, size_t size);
typedef void (APIENTRYP AV_FREE)(void *ptr);
typedef int (APIENTRYP AV_GET_BITS_PER_PIXEL)(const AVPixFmtDescriptor *pixdesc);
typedef int (APIENTRYP AV_SAMPLES_GET_BUFFER_SIZE)(int *linesize, int nb_channels, int nb_samples, enum AVSampleFormat sample_fmt, int align);
typedef int (APIENTRYP AV_GET_BYTES_PER_SAMPLE)(enum AVSampleFormat sample_fmt);
typedef int (APIENTRYP AV_OPT_SET_INT)(void *obj, const char *name, int64_t val, int search_flags);
typedef AVDictionaryEntry* (APIENTRYP AV_DICT_ITERATE)(AVDictionary *m, const AVDictionaryEntry *prev);
typedef AVDictionaryEntry* (APIENTRYP AV_DICT_GET)(AVDictionary *m, const char *key, const AVDictionaryEntry *prev, int flags);
typedef int (APIENTRYP AV_DICT_COUNT)(AVDictionary *m);
typedef int (APIENTRYP AV_DICT_SET)(AVDictionary **pm, const char *key, const char *value, int flags);
typedef void (APIENTRYP AV_DICT_FREE)(AVDictionary **m);
typedef void (APIENTRYP AV_CHANNEL_LAYOUT_DEFAULT)(AVChannelLayoutPtr ch_layout, int nb_channels);
typedef void (APIENTRYP AV_CHANNEL_LAYOUT_UNINIT)(AVChannelLayoutPtr ch_layout);
typedef int (APIENTRYP AV_CHANNEL_LAYOUT_DESCRIBE)(AVChannelLayoutPtr ch_layout, char* buf, size_t buf_size);
typedef int (APIENTRYP AV_OPT_SET_CHLAYOUT)(void *obj, const char *name, const AVChannelLayoutPtr val, int search_flags);

static AV_PIX_FMT_DESC_GET sp_av_pix_fmt_desc_get;
static AV_FRAME_UNREF sp_av_frame_unref;
static AV_REALLOC sp_av_realloc;
static AV_FREE sp_av_free;
static AV_GET_BITS_PER_PIXEL sp_av_get_bits_per_pixel;
static AV_SAMPLES_GET_BUFFER_SIZE sp_av_samples_get_buffer_size;
static AV_GET_BYTES_PER_SAMPLE sp_av_get_bytes_per_sample;
static AV_OPT_SET_INT sp_av_opt_set_int;
static AV_DICT_ITERATE sp_av_dict_iterate;
static AV_DICT_GET sp_av_dict_get;
static AV_DICT_COUNT sp_av_dict_count;
static AV_DICT_SET sp_av_dict_set;
static AV_DICT_FREE sp_av_dict_free;
static AV_CHANNEL_LAYOUT_DEFAULT sp_av_channel_layout_default; // >= 59
static AV_CHANNEL_LAYOUT_UNINIT sp_av_channel_layout_uninit; // >= 59
static AV_CHANNEL_LAYOUT_DESCRIBE sp_av_channel_layout_describe; // >= 59
static AV_OPT_SET_CHLAYOUT sp_av_opt_set_chlayout; // >= 59
// count: +17 = 42

// libavformat
typedef AVFormatContext *(APIENTRYP AVFORMAT_ALLOC_CONTEXT)(void);
typedef void (APIENTRYP AVFORMAT_FREE_CONTEXT)(AVFormatContext *s);  // 52.96.0
typedef void (APIENTRYP AVFORMAT_CLOSE_INPUT)(AVFormatContext **s);  // 53.17.0
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
// count: +14 = 56

// libavdevice [53.0.0]
typedef int (APIENTRYP AVDEVICE_REGISTER_ALL)(void);
static AVDEVICE_REGISTER_ALL sp_avdevice_register_all;
// count: +1 = 57

// libswresample [1...]
typedef int (APIENTRYP AV_OPT_SET_SAMPLE_FMT)(void *obj, const char *name, enum AVSampleFormat fmt, int search_flags); // actually lavu .. but exist only w/ swresample!
typedef struct SwrContext *(APIENTRYP SWR_ALLOC)(void);
typedef int (APIENTRYP SWR_INIT)(struct SwrContext *s);
typedef void (APIENTRYP SWR_FREE)(struct SwrContext **s);
typedef int (APIENTRYP SWR_CONVERT)(struct SwrContext *s, uint8_t **out, int out_count, const uint8_t **in , int in_count);
typedef int (APIENTRYP SWR_GET_OUT_SAMPLES)(struct SwrContext *s, int in_samples);

static AV_OPT_SET_SAMPLE_FMT sp_av_opt_set_sample_fmt;
static SWR_ALLOC sp_swr_alloc;
static SWR_INIT sp_swr_init;
static SWR_FREE sp_swr_free;
static SWR_CONVERT sp_swr_convert;
static SWR_GET_OUT_SAMPLES sp_swr_get_out_samples;
// count: +6 = 66

static const char * const ClazzNameString = "java/lang/String";

// We use JNI Monitor Locking, since this removes the need 
// to statically link-in pthreads on window ..
// #define USE_PTHREAD_LOCKING 1
//
#define USE_JNI_LOCKING 1

#if defined (USE_PTHREAD_LOCKING)
    #include <pthread.h>
    #warning USE LOCKING PTHREAD
    static pthread_mutex_t mutex_avcodec_openclose;
    #define MY_MUTEX_LOCK(e,s) pthread_mutex_lock(&(s))
    #define MY_MUTEX_UNLOCK(e,s) pthread_mutex_unlock(&(s))
#elif defined (USE_JNI_LOCKING)
    static jobject mutex_avcodec_openclose;
    #define MY_MUTEX_LOCK(e,s) (*e)->MonitorEnter(e, s)
    #define MY_MUTEX_UNLOCK(e,s) (*e)->MonitorExit(e, s)
#else
    #warning USE LOCKING NONE
    #define MY_MUTEX_LOCK(e,s)
    #define MY_MUTEX_UNLOCK(e,s)
#endif

#define SYMBOL_COUNT 63

JNIEXPORT jboolean JNICALL FF_FUNC(initSymbols0)
  (JNIEnv *env, jobject instance, jobject jmutex_avcodec_openclose, jobject jSymbols, jint count)
{
    #ifdef USE_PTHREAD_LOCKING
        pthread_mutexattr_t renderLockAttr;
    #endif
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

    sp_avutil_version = (AVUTIL_VERSION) (intptr_t) symbols[i++];
    sp_avformat_version = (AVFORMAT_VERSION) (intptr_t) symbols[i++];
    sp_avcodec_version = (AVCODEC_VERSION) (intptr_t) symbols[i++];
    sp_avdevice_version = (AVDEVICE_VERSION) (intptr_t) symbols[i++];
    sp_swresample_version = (SWRESAMPLE_VERSION) (intptr_t) symbols[i++];

    sp_avcodec_close = (AVCODEC_CLOSE)  (intptr_t) symbols[i++];
    sp_avcodec_string = (AVCODEC_STRING) (intptr_t) symbols[i++];
    sp_avcodec_find_decoder = (AVCODEC_FIND_DECODER) (intptr_t) symbols[i++];
    sp_avcodec_alloc_context3 = (AVCODEC_ALLOC_CONTEXT3) (intptr_t) symbols[i++];
    sp_avcodec_free_context = (AVCODEC_FREE_CONTEXT) (intptr_t) symbols[i++];
    sp_avcodec_parameters_to_context = (AVCODEC_PARAMTERS_TO_CONTEXT) (intptr_t) symbols[i++];
    sp_avcodec_open2 = (AVCODEC_OPEN2) (intptr_t) symbols[i++];
    sp_av_frame_alloc = (AV_FRAME_ALLOC) (intptr_t) symbols[i++];
    sp_av_free_frame = (AV_FREE_FRAME) (intptr_t) symbols[i++];
    sp_avcodec_default_get_buffer2 = (AVCODEC_DEFAULT_GET_BUFFER2) (intptr_t) symbols[i++];
    sp_av_image_fill_linesizes = (AV_IMAGE_FILL_LINESIZES) (intptr_t) symbols[i++];
    sp_avcodec_flush_buffers = (AVCODEC_FLUSH_BUFFERS) (intptr_t) symbols[i++];
    sp_av_packet_alloc = (AV_PACKET_ALLOC) (intptr_t) symbols[i++];
    sp_av_packet_free = (AV_PACKET_FREE) (intptr_t) symbols[i++];
    sp_av_new_packet = (AV_NEW_PACKET) (intptr_t) symbols[i++];
    sp_av_packet_unref = (AV_PACKET_UNREF) (intptr_t) symbols[i++];
    sp_avcodec_send_packet = (AVCODEC_SEND_PACKET) (intptr_t) symbols[i++];
    sp_avcodec_receive_frame = (AVCODEC_RECEIVE_FRAME) (intptr_t) symbols[i++];
    sp_avcodec_decode_subtitle2 = (AVCODEC_DECODE_SUBTITLE2) (intptr_t) symbols[i++];
    sp_avsubtitle_free = (AV_SUBTITLE_FREE) (intptr_t) symbols[i++];

    sp_av_pix_fmt_desc_get = (AV_PIX_FMT_DESC_GET) (intptr_t) symbols[i++];
    sp_av_frame_unref = (AV_FRAME_UNREF) (intptr_t) symbols[i++];
    sp_av_realloc = (AV_REALLOC) (intptr_t) symbols[i++];
    sp_av_free = (AV_FREE) (intptr_t) symbols[i++];
    sp_av_get_bits_per_pixel = (AV_GET_BITS_PER_PIXEL) (intptr_t) symbols[i++];
    sp_av_samples_get_buffer_size = (AV_SAMPLES_GET_BUFFER_SIZE) (intptr_t) symbols[i++];
    sp_av_get_bytes_per_sample = (AV_GET_BYTES_PER_SAMPLE) (intptr_t) symbols[i++];
    sp_av_opt_set_int = (AV_OPT_SET_INT) (intptr_t) symbols[i++];
    sp_av_dict_iterate = (AV_DICT_ITERATE) (intptr_t) symbols[i++];
    sp_av_dict_get = (AV_DICT_GET) (intptr_t) symbols[i++];
    sp_av_dict_count = (AV_DICT_COUNT) (intptr_t) symbols[i++];
    sp_av_dict_set = (AV_DICT_SET) (intptr_t) symbols[i++];
    sp_av_dict_free = (AV_DICT_FREE) (intptr_t) symbols[i++];
    sp_av_channel_layout_default = (AV_CHANNEL_LAYOUT_DEFAULT) (intptr_t) symbols[i++];
    sp_av_channel_layout_uninit = (AV_CHANNEL_LAYOUT_UNINIT) (intptr_t) symbols[i++];
    sp_av_channel_layout_describe = (AV_CHANNEL_LAYOUT_DESCRIBE) (intptr_t) symbols[i++];
    sp_av_opt_set_chlayout = (AV_OPT_SET_CHLAYOUT) (intptr_t) symbols[i++];

    sp_avformat_alloc_context = (AVFORMAT_ALLOC_CONTEXT) (intptr_t) symbols[i++];;
    sp_avformat_free_context = (AVFORMAT_FREE_CONTEXT) (intptr_t) symbols[i++];
    sp_avformat_close_input = (AVFORMAT_CLOSE_INPUT) (intptr_t) symbols[i++];
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

    sp_av_opt_set_sample_fmt = (AV_OPT_SET_SAMPLE_FMT) (intptr_t) symbols[i++];
    sp_swr_alloc = (SWR_ALLOC) (intptr_t) symbols[i++];
    sp_swr_init = (SWR_INIT) (intptr_t) symbols[i++];
    sp_swr_free = (SWR_FREE) (intptr_t) symbols[i++];
    sp_swr_convert = (SWR_CONVERT) (intptr_t) symbols[i++];
    sp_swr_get_out_samples = (SWR_GET_OUT_SAMPLES) (intptr_t) symbols[i++];

    (*env)->ReleasePrimitiveArrayCritical(env, jSymbols, symbols, 0);

    if(SYMBOL_COUNT != i) {
        // boom
        fprintf(stderr, "FFMPEGNatives.initSymbols0: Wrong symbol assignment count: Expected %d, Is %d\n", 
                SYMBOL_COUNT, i);
        return JNI_FALSE;
    }

    if(!HAS_FUNC(sp_avcodec_default_get_buffer2) || 
       !HAS_FUNC(sp_av_frame_unref) ) {
        fprintf(stderr, "FFMPEGNatives.initSymbols0: avcodec >= 55: avcodec_default_get_buffer2 %p, av_frame_unref %p\n",
            sp_avcodec_default_get_buffer2, sp_av_frame_unref);
        return JNI_FALSE;
    }

#if LIBAVCODEC_VERSION_MAJOR >= 59
    if( !HAS_FUNC(sp_av_channel_layout_default) ||
        !HAS_FUNC(sp_av_channel_layout_uninit) ||
        !HAS_FUNC(sp_av_channel_layout_describe) ||
        !HAS_FUNC(sp_av_opt_set_chlayout)
      ) {
        fprintf(stderr, "FFMPEGNatives.initSymbols0: avcodec >= 59: av_channel_layout_* missing\n");
        return JNI_FALSE;
    }
#endif

    #if defined (USE_PTHREAD_LOCKING)
        pthread_mutexattr_init(&renderLockAttr);
        pthread_mutexattr_settype(&renderLockAttr, PTHREAD_MUTEX_RECURSIVE);
        pthread_mutex_init(&mutex_avcodec_openclose, &renderLockAttr); // recursive
    #elif defined (USE_JNI_LOCKING)
        mutex_avcodec_openclose = (*env)->NewGlobalRef(env, jmutex_avcodec_openclose);
    #endif

    /** At static destroy: Never
    #if defined (USE_PTHREAD_LOCKING)
        pthread_mutex_unlock(&mutex_avcodec_openclose);
        pthread_mutex_destroy(&mutex_avcodec_openclose);
    #elif defined (USE_JNI_LOCKING)
        (*env)->DeleteGlobalRef(env, mutex_avcodec_openclose);
    #endif
    */

    return JNI_TRUE;
}

static inline const char* meta_get_value(AVDictionary *tags, const char* key)
{
    // SECTION_ID_CHAPTER_TAGS
    if (!tags) {
        return NULL;
    }
    const AVDictionaryEntry *entry = NULL;
    if ((entry = sp_av_dict_get(tags, key, entry, AV_DICT_IGNORE_SUFFIX))) {
        return entry->value;
    }
    return NULL;
}
static inline const char* meta_get_title(AVDictionary *tags)
{
    return meta_get_value(tags, "title");
}
static inline const char* meta_get_language(AVDictionary *tags)
{
    return meta_get_value(tags, "language");
}

static int _isAudioFormatSupported(JNIEnv *env, jobject ffmpegMediaPlayer, enum AVSampleFormat aSampleFmt, int32_t aSampleRate, int32_t aChannels) {
    int res = JNI_TRUE == (*env)->CallBooleanMethod(env, ffmpegMediaPlayer, ffmpeg_jni_mid_isAudioFormatSupported, aSampleFmt, aSampleRate, aChannels);
    JoglCommon_ExceptionCheck1_throwNewRuntimeException(env, "FFmpeg: Exception occured at isAudioFormatSupported(..)");
    return res;
}
static void _updateJavaAttributes(JNIEnv *env, FFMPEGToolBasicAV_t* pAV) {
    if(NULL!=env) {
        jclass strclazz = (*env)->FindClass(env, ClazzNameString);
        if( strclazz == NULL ) {
            JoglCommon_throwNewRuntimeException(env, "FFmpeg: No Java String Class");
            return;
        }
        jintArray a_streams = (*env)->NewIntArray(env, pAV->a_stream_count);
        if (a_streams == NULL) {
            JoglCommon_throwNewRuntimeException(env, "FFmpeg: Out of memory (a_streams %u)", pAV->a_stream_count);
            return;
        }
        jintArray v_streams = (*env)->NewIntArray(env, pAV->v_stream_count);
        if (v_streams == NULL) {
            JoglCommon_throwNewRuntimeException(env, "FFmpeg: Out of memory (v_streams %u)", pAV->v_stream_count);
            return;
        }
        jintArray s_streams = (*env)->NewIntArray(env, pAV->s_stream_count);
        if (s_streams == NULL) {
            JoglCommon_throwNewRuntimeException(env, "FFmpeg: Out of memory (s_streams %u)", pAV->s_stream_count);
            return;
        }
        jobjectArray a_langs = (*env)->NewObjectArray(env, pAV->a_stream_count, strclazz, NULL);
        if (a_langs == NULL) {
            JoglCommon_throwNewRuntimeException(env, "FFmpeg: Out of memory (a_langs %u)", pAV->a_stream_count);
            return;
        }
        jobjectArray v_langs = (*env)->NewObjectArray(env, pAV->v_stream_count, strclazz, NULL);
        if (v_langs == NULL) {
            JoglCommon_throwNewRuntimeException(env, "FFmpeg: Out of memory (v_langs %u)", pAV->v_stream_count);
            return;
        }
        jobjectArray s_langs = (*env)->NewObjectArray(env, pAV->s_stream_count, strclazz, NULL);
        if (s_langs == NULL) {
            JoglCommon_throwNewRuntimeException(env, "FFmpeg: Out of memory (s_langs %u)", pAV->s_stream_count);
            return;
        }
        if( 0 < pAV->a_stream_count ) {
            (*env)->SetIntArrayRegion(env, a_streams, 0, pAV->a_stream_count, pAV->a_streams);
            for(int i=0; i<pAV->a_stream_count; ++i) {
                AVStream *st = pAV->pFormatCtx->streams[pAV->a_streams[i]];
                const char* lang0 = meta_get_language(st->metadata);
                const char* lang1 = NULL != lang0 ? lang0 : "und";
                (*env)->SetObjectArrayElement(env, a_langs, i, (*env)->NewStringUTF(env, lang1));
            }
        }
        if( 0 < pAV->v_stream_count ) {
            (*env)->SetIntArrayRegion(env, v_streams, 0, pAV->v_stream_count, pAV->v_streams);
            for(int i=0; i<pAV->v_stream_count; ++i) {
                AVStream *st = pAV->pFormatCtx->streams[pAV->v_streams[i]];
                const char* lang0 = meta_get_language(st->metadata);
                const char* lang1 = NULL != lang0 ? lang0 : "und";
                (*env)->SetObjectArrayElement(env, v_langs, i, (*env)->NewStringUTF(env, lang1));
            }
        }
        if( 0 < pAV->s_stream_count ) {
            (*env)->SetIntArrayRegion(env, s_streams, 0, pAV->s_stream_count, pAV->s_streams);
            for(int i=0; i<pAV->s_stream_count; ++i) {
                AVStream *st = pAV->pFormatCtx->streams[pAV->s_streams[i]];
                const char* lang0 = meta_get_language(st->metadata);
                const char* lang1 = NULL != lang0 ? lang0 : "und";
                (*env)->SetObjectArrayElement(env, s_langs, i, (*env)->NewStringUTF(env, lang1));
            }
        }
        jstring jtitle;
        {
            const char* title = meta_get_title(pAV->pFormatCtx->metadata);
            if( NULL != title ) {
               jtitle = (*env)->NewStringUTF(env, title);
            } else {
               jtitle = NULL;
            }
        }

        (*env)->CallVoidMethod(env, pAV->ffmpegMediaPlayer, ffmpeg_jni_mid_setupFFAttributes,
                               pAV->vid, pAV->vPixFmt, pAV->vBufferPlanes, 
                               pAV->vBitsPerPixel, pAV->vBytesPerPixelPerPlane,
                               pAV->vTexWidth[0], pAV->vTexWidth[1], pAV->vTexWidth[2],
                               pAV->vWidth, pAV->vHeight,
                               pAV->aid, pAV->aSampleFmtOut, pAV->aSampleRateOut, pAV->aChannelsOut, pAV->aFrameSize);
        JoglCommon_ExceptionCheck1_throwNewRuntimeException(env, "FFmpeg: Exception occured at setupFFAttributes(..)");

        (*env)->CallVoidMethod(env, pAV->ffmpegMediaPlayer, ffmpeg_jni_mid_updateAttributes,
                               jtitle,
                               v_streams, v_langs, pAV->vid, 
                               a_streams, a_langs, pAV->aid,
                               s_streams, s_langs, pAV->sid,
                               pAV->vWidth, pAV->vHeight,
                               pAV->bps_stream, pAV->bps_video, pAV->bps_audio,
                               pAV->fps, pAV->frames_video, pAV->frames_audio, pAV->duration,
                               (*env)->NewStringUTF(env, pAV->vcodec),
                               (*env)->NewStringUTF(env, pAV->acodec),
                               (*env)->NewStringUTF(env, pAV->scodec));
        JoglCommon_ExceptionCheck1_throwNewRuntimeException(env, "FFmpeg: Exception occured at updateAttributes(..)");
    }
}
static void _setIsGLOriented(JNIEnv *env, FFMPEGToolBasicAV_t* pAV) {
    if(NULL!=env) {
        (*env)->CallVoidMethod(env, pAV->ffmpegMediaPlayer, ffmpeg_jni_mid_setIsGLOriented, pAV->vFlipped);
        JoglCommon_ExceptionCheck1_throwNewRuntimeException(env, "FFmpeg: Exception occured at setIsGLOriented(..)");
    }
}

static void freeInstance(JNIEnv *env, FFMPEGToolBasicAV_t* pAV) {
    int i;
    if(NULL != pAV) {
        MY_MUTEX_LOCK(env, mutex_avcodec_openclose);
        pAV->ready = 0;
        {
            // Close the V codec
            if(NULL != pAV->pVCodecCtx) {
                sp_avcodec_close(pAV->pVCodecCtx);
                sp_avcodec_free_context(&pAV->pVCodecCtx);
                pAV->pVCodecCtx = NULL;
            }
            pAV->pVCodec=NULL;

            // Close the A codec
            if(NULL != pAV->pACodecCtx) {
                sp_avcodec_close(pAV->pACodecCtx);
                sp_avcodec_free_context(&pAV->pACodecCtx);
                pAV->pACodecCtx = NULL;
            }
            pAV->pACodec=NULL;

            // Close the S codec
            if(NULL != pAV->pSCodecCtx) {
                sp_avcodec_close(pAV->pSCodecCtx);
                sp_avcodec_free_context(&pAV->pSCodecCtx);
                pAV->pSCodecCtx = NULL;
            }
            pAV->pSCodec=NULL;

            // Close the video file
            if(NULL != pAV->pFormatCtx) {
                sp_avformat_close_input(&pAV->pFormatCtx);
                sp_avformat_free_context(pAV->pFormatCtx);
                pAV->pFormatCtx = NULL;
            }
        }
        MY_MUTEX_UNLOCK(env, mutex_avcodec_openclose);

        // Close the A resampler
        if( NULL != pAV->swResampleCtx ) {
            sp_swr_free(&pAV->swResampleCtx);
            pAV->swResampleCtx = NULL;
        }
        if( NULL != pAV->aResampleBuffer ) {
            sp_av_free(pAV->aResampleBuffer);
            pAV->aResampleBuffer = NULL;
        }

        // Close the frames
        if(NULL != pAV->pVFrame) {
            sp_av_free_frame(&pAV->pVFrame);
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
                sp_av_free_frame(&pAV->pAFrames[i]);
            }
            free(pAV->pAFrames);
            pAV->pAFrames = NULL;
        }

        if( NULL != pAV->ffmpegMediaPlayer ) {
            (*env)->DeleteGlobalRef(env, pAV->ffmpegMediaPlayer);
            pAV->ffmpegMediaPlayer = NULL;
        }

        if( NULL != pAV->packet ) {
            sp_av_packet_free(&pAV->packet);
            pAV->packet = NULL;
        }

        free(pAV);
    }
}

static int my_getPlaneCount(const AVPixFmtDescriptor *pDesc) {
    int i, p=-1;
    for(i=pDesc->nb_components-1; i>=0; i--) {
        int p0 = pDesc->comp[i].plane;
        if( p < p0 ) {
            p = p0;
        }
    }
    return p+1;
}

#if 0
static int my_is_hwaccel_pix_fmt(enum PixelFormat pix_fmt) {
    return sp_av_pix_fmt_descriptors[pix_fmt].flags & PIX_FMT_HWACCEL;
}

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

JNIEXPORT jint JNICALL FF_FUNC(getAvDeviceMajorVersionCC0)
  (JNIEnv *env, jobject instance) {
    return (jint) LIBAVDEVICE_VERSION_MAJOR;
}

JNIEXPORT jint JNICALL FF_FUNC(getSwResampleMajorVersionCC0)
  (JNIEnv *env, jobject instance) {
    return (jint) LIBSWRESAMPLE_VERSION_MAJOR;
}

JNIEXPORT jlong JNICALL FF_FUNC(createInstance0)
  (JNIEnv *env, jobject instance, jobject ffmpegMediaPlayer, jboolean verbose)
{
    FFMPEGToolBasicAV_t * pAV = calloc(1, sizeof(FFMPEGToolBasicAV_t));
    if(NULL==pAV) {
        JoglCommon_throwNewRuntimeException(env, "Couldn't alloc instance");
        return 0;
    }
    pAV->ready = 0;
    pAV->avcodecVersion = sp_avcodec_version();
    pAV->avformatVersion = sp_avformat_version(); 
    pAV->avutilVersion = sp_avutil_version();
    if( HAS_FUNC(sp_avdevice_version) ) {
        pAV->avdeviceVersion = sp_avdevice_version();
    } else {
        pAV->avdeviceVersion = 0;
    }
    if( HAS_FUNC(sp_swresample_version) ) {
        pAV->swresampleVersion = sp_swresample_version();
    } else {
        pAV->swresampleVersion = 0;
    }

    // NOTE: We keep code on using 1 a/v frame per decoding cycle now.
    //       This is compatible w/ OpenAL's alBufferData(..)
    //       and w/ OpenGL's texture update command, both copy data immediately.
    //
    // NOTE: ffmpeg using `avcodec_receive_frame()` always uses `refcounted_frames`, i.e. always true now!
    // pAV->useRefCountedFrames = 1;

    pAV->ffmpegMediaPlayer = (*env)->NewGlobalRef(env, ffmpegMediaPlayer);
    pAV->verbose = verbose;
    pAV->vid=AV_STREAM_ID_AUTO;
    pAV->aid=AV_STREAM_ID_AUTO;
    pAV->sid=AV_STREAM_ID_AUTO;
    pAV->a_stream_count=0;
    pAV->v_stream_count=0;
    pAV->s_stream_count=0;
    for(int i=0; i<MAX_STREAM_COUNT; ++i) {
        pAV->a_streams[i]=AV_STREAM_ID_NONE;
        pAV->v_streams[i]=AV_STREAM_ID_NONE;
        pAV->s_streams[i]=AV_STREAM_ID_NONE;
    }

    if(pAV->verbose) {
        fprintf(stderr, "Info: Has swresample %d, device %d\n",
                AV_HAS_API_SWRESAMPLE(pAV), HAS_FUNC(sp_avdevice_register_all));
    }
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

#if LIBAVCODEC_VERSION_MAJOR < 59
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
#else
static void getDefaultAVChannelLayout(AVChannelLayout* cl, int channelCount) {
    sp_av_channel_layout_uninit(cl);
    switch(channelCount) {
        case 1: *cl = (AVChannelLayout)AV_CHANNEL_LAYOUT_MONO; break;
        case 2: *cl = (AVChannelLayout)AV_CHANNEL_LAYOUT_STEREO; break;
        case 3: *cl = (AVChannelLayout)AV_CHANNEL_LAYOUT_SURROUND; break;
        case 4: *cl = (AVChannelLayout)AV_CHANNEL_LAYOUT_QUAD; break;
        case 5: *cl = (AVChannelLayout)AV_CHANNEL_LAYOUT_5POINT0; break;
        case 6: *cl = (AVChannelLayout)AV_CHANNEL_LAYOUT_5POINT1; break;
        case 7: *cl = (AVChannelLayout)AV_CHANNEL_LAYOUT_6POINT1; break;
        case 8: *cl = (AVChannelLayout)AV_CHANNEL_LAYOUT_7POINT1; break;
        default: {
            sp_av_channel_layout_default(cl, channelCount);
        }
    }
}
#endif

static void initPTSStats(PTSStats *ptsStats);
static int64_t evalPTS(PTSStats *ptsStats, int64_t inPTS, int64_t inDTS);

static AVInputFormat* tryAVInputFormat(const char * name, int verbose) {
    AVInputFormat* inFmt = sp_av_find_input_format(name);
    if( verbose) {
        if ( NULL == inFmt ) {
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
    "avfoundation", // osx
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

#if 0
static void getAlignedLinesizes(AVCodecContext *avctx, int linesize[/*4*/]) {
    int stride_align[AV_NUM_DATA_POINTERS];
    int w = avctx->width;
    int h = avctx->height;
    int unaligned;
    int i;

    sp_avcodec_align_dimensions2(avctx, &w, &h, stride_align);

    if (!(avctx->flags & CODEC_FLAG_EMU_EDGE)) {
        int edge_width = sp_avcodec_get_edge_width();
        w += edge_width * 2;
        h += edge_width * 2;
    }

    do {
        // Get alignment for all planes (-> YUVP .. etc)
        sp_av_image_fill_linesizes(linesize, avctx->pix_fmt, w);
        // increase alignment of w for next try (rhs gives the lowest bit set in w)
        w += w & ~(w - 1);

        unaligned = 0;
        for (i = 0; i < 4; i++)
            unaligned |= linesize[i] % stride_align[i];
    } while (unaligned);
}
#endif

#if LIBAVCODEC_VERSION_MAJOR < 60
static int64_t getFrameNum(const AVCodecContext *avctx) {
    return (int64_t)avctx->frame_number;
}
#else
static int64_t getFrameNum(const AVCodecContext *avctx) {
    return avctx->frame_num;
}
#endif

JNIEXPORT void JNICALL FF_FUNC(setStream0)
  (JNIEnv *env, jobject instance, jlong ptr, jstring jURL, jboolean jIsCameraInput, 
   jint vid, jstring jSizeS, jint vWidth, jint vHeight, jint vRate,
   jint aid, jint aMaxChannelCount, jint aPrefSampleRate,
   jint sid)
{
    char cameraName[256];
    int res, i;
    jboolean iscopy;
    FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)(intptr_t)ptr;

    if (pAV == NULL) {
        JoglCommon_throwNewRuntimeException(env, "NULL AV ptr");
        return;
    }

    // Register all formats and codecs
    if( jIsCameraInput && HAS_FUNC(sp_avdevice_register_all) ) {
        sp_avdevice_register_all();
    }
    // Network too ..
    if(HAS_FUNC(sp_avformat_network_init)) {
        sp_avformat_network_init();
    }

    pAV->packet = sp_av_packet_alloc();
    if( NULL == pAV->packet ) {
        JoglCommon_throwNewRuntimeException(env, "Couldn't allocate AVPacket");
        return;
    }
    pAV->pFormatCtx = sp_avformat_alloc_context();

    const char *urlPath = (*env)->GetStringUTFChars(env, jURL, &iscopy);
    const char *filename = urlPath; // allow changing path for camera ..

    // Open video file
    AVDictionary *inOpts = NULL;
    AVInputFormat* inFmt = NULL;
    if( jIsCameraInput ) {
        char buffer[256];
        inFmt = findAVInputFormat(pAV->verbose);
        if( NULL == inFmt ) {
            JoglCommon_throwNewRuntimeException(env, "Couldn't find input format for camera: %s", urlPath);
            (*env)->ReleaseStringUTFChars(env, jURL, (const char *)urlPath);
            return;
        }
        if(pAV->verbose) {
            fprintf(stderr, "Camera: Format: %s (%s)\n", inFmt->long_name, inFmt->name);
        }
        if( 0 == strncmp(inFmt->name, "dshow", 255) ) {
            int devIdx = atoi(urlPath);
            strncpy(cameraName, "video=", sizeof(cameraName));
            res = findDShowVideoDevice(cameraName+6, sizeof(cameraName)-6, devIdx, pAV->verbose);
            if( 0 == res ) {
                if(pAV->verbose) {
                    fprintf(stderr, "Camera %d found: %s\n", devIdx, cameraName);
                }
                filename = cameraName;
            } else if(pAV->verbose) {
                fprintf(stderr, "Camera %d not found\n", devIdx);
            }
        }
        if(pAV->verbose) {
            fprintf(stderr, "Camera: Filename: %s\n", filename);
        }

        int hasSize = 0;
        {
            const char *sizeS = NULL != jSizeS ? (*env)->GetStringUTFChars(env, jSizeS, &iscopy) : NULL;
            if( NULL != sizeS ) {
                snprintf(buffer, sizeof(buffer), "%s", sizeS);
                (*env)->ReleaseStringUTFChars(env, jSizeS, (const char *)sizeS);
                hasSize = 1;
            } else if( vWidth > 0 && vHeight > 0 ) {
                snprintf(buffer, sizeof(buffer), "%dx%d", vWidth, vHeight);
                hasSize = 1;
            }
        }
        if( hasSize ) {
            if(pAV->verbose) {
                fprintf(stderr, "Camera: Size: %s\n", buffer);
            }
            sp_av_dict_set(&inOpts, "video_size", buffer, 0);
        }
        if( vRate > 0 ) {
            snprintf(buffer, sizeof(buffer), "%d", vRate);
            if(pAV->verbose) {
                fprintf(stderr, "Camera: FPS: %s\n", buffer);
            }
            sp_av_dict_set(&inOpts, "framerate", buffer, 0);
        }
        // FIXME pre-select: sp_av_dict_set(&inOpts, "pixel_format", "yuyv422", 0); 
    }

    MY_MUTEX_LOCK(env, mutex_avcodec_openclose);
    {
        res = sp_avformat_open_input(&pAV->pFormatCtx, filename, inFmt, NULL != inOpts ? &inOpts : NULL);
        if( NULL != inOpts ) {
            sp_av_dict_free(&inOpts);
        }
        if(res != 0) {
            MY_MUTEX_UNLOCK(env, mutex_avcodec_openclose);
            JoglCommon_throwNewRuntimeException(env, "Couldn't open URI: %s [%dx%d @ %d hz], err %d", filename, vWidth, vHeight, vRate, res);
            (*env)->ReleaseStringUTFChars(env, jURL, (const char *)urlPath);
            return;
        }

        // Retrieve detailed stream information
        if(sp_avformat_find_stream_info(pAV->pFormatCtx, NULL)<0) {
            MY_MUTEX_UNLOCK(env, mutex_avcodec_openclose);
            (*env)->ReleaseStringUTFChars(env, jURL, (const char *)urlPath);
            JoglCommon_throwNewRuntimeException(env, "Couldn't find stream information");
            return;
        }
    }
    MY_MUTEX_UNLOCK(env, mutex_avcodec_openclose);

    if(pAV->verbose) {
        // Dump information about file onto standard error
        sp_av_dump_format(pAV->pFormatCtx, 0, filename, JNI_FALSE);
    }
    (*env)->ReleaseStringUTFChars(env, jURL, (const char *)urlPath);

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
    pAV->a_stream_count=0;
    pAV->v_stream_count=0;
    pAV->s_stream_count=0;
    for(int i=0; i<MAX_STREAM_COUNT; ++i) {
        pAV->a_streams[i]=AV_STREAM_ID_NONE;
        pAV->v_streams[i]=AV_STREAM_ID_NONE;
        pAV->s_streams[i]=AV_STREAM_ID_NONE;
    }
    for(i=0; i<pAV->pFormatCtx->nb_streams; i++) {
        AVStream *st = pAV->pFormatCtx->streams[i];
        if(pAV->verbose) {
            const char* lang0 = meta_get_language(st->metadata);
            const char* lang1 = NULL != lang0 ? lang0 : "n/a";
            fprintf(stderr, "Stream: %d: is-video %d, is-audio %d, is-sub %d, lang %s\n", i, 
                AVMEDIA_TYPE_VIDEO == st->codecpar->codec_type, AVMEDIA_TYPE_AUDIO == st->codecpar->codec_type, 
                AVMEDIA_TYPE_SUBTITLE == st->codecpar->codec_type, lang1);
        }
        if(AVMEDIA_TYPE_VIDEO == st->codecpar->codec_type) {
            if( pAV->v_stream_count < MAX_STREAM_COUNT-1 ) {
                pAV->v_streams[pAV->v_stream_count++] = i;
            }
            if(AV_STREAM_ID_AUTO==pAV->vid && (AV_STREAM_ID_AUTO==vid || vid == i) ) {
                pAV->pVStream = st;
                pAV->vid=i;
            }
        } else if(AVMEDIA_TYPE_AUDIO == st->codecpar->codec_type) {
            if( pAV->a_stream_count < MAX_STREAM_COUNT-1 ) {
                pAV->a_streams[pAV->a_stream_count++] = i;
            }
            if(AV_STREAM_ID_AUTO==pAV->aid && (AV_STREAM_ID_AUTO==aid || aid == i) ) {
                pAV->pAStream = st;
                pAV->aid=i;
            }
        } else if(AVMEDIA_TYPE_SUBTITLE == st->codecpar->codec_type) {
            if( pAV->s_stream_count < MAX_STREAM_COUNT-1 ) {
                pAV->s_streams[pAV->s_stream_count++] = i;
            }
            if(AV_STREAM_ID_AUTO==pAV->sid && (AV_STREAM_ID_AUTO==sid || sid == i) ) {
                pAV->pSStream = st;
                pAV->sid=i;
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
        fprintf(stderr, "Found vid %d, aid %d, sid %d\n", pAV->vid, pAV->aid, pAV->sid);
    }

    if(0<=pAV->aid) {
        AVFrame * pAFrame0 = sp_av_frame_alloc();
        if( NULL == pAFrame0 ) {
            JoglCommon_throwNewRuntimeException(env, "Couldn't alloc 1st audio frame\n");
            return;
        }

        // Get a pointer to the codec context for the audio stream
        // FIXME: Libav Binary compatibility! JAU01
        pAV->pACodecPar=pAV->pAStream->codecpar;

        // FIXME: Libav Binary compatibility! JAU01
        if (pAV->pACodecPar->bit_rate) {
            pAV->bps_audio = pAV->pACodecPar->bit_rate;
        }

        // Find the decoder for the audio stream
        pAV->pACodec=sp_avcodec_find_decoder(pAV->pACodecPar->codec_id);
        if(pAV->pACodec==NULL) {
            JoglCommon_throwNewRuntimeException(env, "Couldn't find audio codec for codec_id %d", pAV->pACodecPar->codec_id);
            return;
        }

        // Allocate the decoder context for the audio stream
        pAV->pACodecCtx = sp_avcodec_alloc_context3(pAV->pACodec);
        if(pAV->pACodecCtx==NULL) {
            JoglCommon_throwNewRuntimeException(env, "Couldn't allocate audio decoder context for codec_id %d", pAV->pACodecPar->codec_id);
            return;
        }
        res = sp_avcodec_parameters_to_context(pAV->pACodecCtx, pAV->pACodecPar);
        if(res<0) {
            JoglCommon_throwNewRuntimeException(env, "Couldn't copy audio codec-par to context");
            return;
        }

        // Customize ..
        pAV->pACodecCtx->pkt_timebase = pAV->pAStream->time_base;
        // pAV->pACodecCtx->thread_count=2;
        // pAV->pACodecCtx->thread_type=FF_THREAD_FRAME|FF_THREAD_SLICE; // Decode more than one frame at once
        pAV->pACodecCtx->thread_count=0;
        pAV->pACodecCtx->thread_type=0;
        pAV->pACodecCtx->workaround_bugs=FF_BUG_AUTODETECT;
        pAV->pACodecCtx->skip_frame=AVDISCARD_DEFAULT;

        // Note: OpenAL well supports n-channel by now (SOFT),
        //       however - AFAIK AV_SAMPLE_FMT_S16 would allow no conversion!
        pAV->pACodecCtx->request_sample_fmt=AV_SAMPLE_FMT_S16;

        sp_avcodec_string(pAV->acodec, sizeof(pAV->acodec), pAV->pACodecCtx, 0);

        // Open codec
        MY_MUTEX_LOCK(env, mutex_avcodec_openclose);
        {
            res = sp_avcodec_open2(pAV->pACodecCtx, pAV->pACodec, NULL);
        }
        MY_MUTEX_UNLOCK(env, mutex_avcodec_openclose);
        if(res<0) {
            JoglCommon_throwNewRuntimeException(env, "Couldn't open audio codec %d, %s", pAV->pACodecCtx->codec_id, pAV->acodec);
            return;
        }
        // try to shape audio channel-layout on fixed audio channel-count
#if LIBAVCODEC_VERSION_MAJOR < 59
        pAV->aChannels = pAV->pACodecCtx->channels;
        if ( !pAV->pACodecCtx->channel_layout ) {
            const uint64_t cl = getDefaultAudioChannelLayout(pAV->aChannels);
            if ( !cl ) {
                JoglCommon_throwNewRuntimeException(env, "Couldn't determine channel layout of %d channels\n", pAV->aChannels);
                return;
            }
            pAV->pACodecCtx->channel_layout = cl;
        }
        if( pAV->verbose ) {
            fprintf(stderr, "A channels %d, layout 0x%"PRIx64"\n",
                    pAV->aChannels, pAV->pACodecCtx->channel_layout);
        }
#else
        pAV->aChannels = pAV->pACodecCtx->ch_layout.nb_channels;
        if ( pAV->pACodecCtx->ch_layout.order == AV_CHANNEL_ORDER_UNSPEC ) {
            getDefaultAVChannelLayout(&pAV->pACodecCtx->ch_layout, pAV->aChannels);
        }
        if( pAV->verbose ) {
            char buf[256];
            sp_av_channel_layout_describe(&pAV->pACodecCtx->ch_layout, buf, sizeof(buf));
            fprintf(stderr, "A channels %d, layout %s\n", pAV->aChannels, buf);
        }
#endif
        pAV->aSampleRate = pAV->pACodecCtx->sample_rate;
        pAV->aFrameSize = pAV->pACodecCtx->frame_size; // in samples per channel!
        pAV->aSampleFmt = pAV->pACodecCtx->sample_fmt;
        pAV->frames_audio = pAV->pAStream->nb_frames;
        pAV->aSinkSupport = _isAudioFormatSupported(env, pAV->ffmpegMediaPlayer, pAV->aSampleFmt, pAV->aSampleRate, pAV->aChannels);
        if( pAV->verbose ) {
            fprintf(stderr, "A sample_rate %d, frame_size %d, frame_number %"PRId64", [afps %f, sfps %f], nb_frames %"PRId64", [maxChan %d, prefRate %d], sink-support %d \n",
                pAV->aSampleRate, pAV->aFrameSize, getFrameNum(pAV->pACodecCtx),
                my_av_q2f(pAV->pAStream->avg_frame_rate),
                my_av_q2f_r(pAV->pAStream->time_base),
                pAV->pAStream->nb_frames,
                aMaxChannelCount, aPrefSampleRate,
                pAV->aSinkSupport);
        }

        // default
        pAV->aSampleFmtOut = pAV->aSampleFmt;
        pAV->aChannelsOut = pAV->aChannels;
        pAV->aSampleRateOut = pAV->aSampleRate;

        if( ( AV_HAS_API_SWRESAMPLE(pAV) ) && 
            ( pAV->aSampleFmt != AV_SAMPLE_FMT_S16 || 
            ( 0 != aPrefSampleRate && pAV->aSampleRate != aPrefSampleRate ) || 
              !pAV->aSinkSupport ) )
        {
            const int32_t maxOutChannelCount = MIN_INT(aMaxChannelCount, MAX_INT(1, pAV->aChannels));
            if( 0 == aPrefSampleRate ) {
                aPrefSampleRate = pAV->aSampleRate;
            }
            int32_t aSinkSupport = 0;
            enum AVSampleFormat aSampleFmtOut = AV_SAMPLE_FMT_S16;
            int32_t aChannelsOut;
            int32_t aSampleRateOut;
            
            if( _isAudioFormatSupported(env, pAV->ffmpegMediaPlayer, aSampleFmtOut, aPrefSampleRate, pAV->aChannels) ) {
                aChannelsOut = pAV->aChannels;
                aSampleRateOut = aPrefSampleRate;
                aSinkSupport = 1;
            } else if( _isAudioFormatSupported(env, pAV->ffmpegMediaPlayer, aSampleFmtOut, aPrefSampleRate, maxOutChannelCount) ) {
                aChannelsOut = maxOutChannelCount;
                aSampleRateOut = aPrefSampleRate;
                aSinkSupport = 1;
            }

            if( aSinkSupport && AV_HAS_API_SWRESAMPLE(pAV) ) {
                pAV->swResampleCtx = sp_swr_alloc();
#if LIBAVCODEC_VERSION_MAJOR < 59
                const int64_t out_channel_layout = getDefaultAudioChannelLayout(aChannelsOut);
                sp_av_opt_set_int(pAV->swResampleCtx,        "in_channel_layout",  pAV->pACodecCtx->channel_layout,            0);
                sp_av_opt_set_int(pAV->swResampleCtx,        "out_channel_layout", out_channel_layout, 0);
                if( pAV->verbose ) {
                    fprintf(stderr, "A Resample: channels %d -> %d, layout 0x%"PRIx64" -> 0x%"PRIx64", rate %d -> %d, fmt 0x%x -> 0x%x\n",
                            pAV->aChannels, aChannelsOut, pAV->pACodecCtx->channel_layout, out_channel_layout,
                            pAV->aSampleRate, aSampleRateOut, (int)pAV->aSampleFmt, (int)aSampleFmtOut);
                }
#else
                AVChannelLayout out_ch_layout = {0};
                getDefaultAVChannelLayout(&out_ch_layout, aChannelsOut);
                sp_av_opt_set_chlayout(pAV->swResampleCtx,        "in_chlayout",  &pAV->pACodecCtx->ch_layout,                 0);
                sp_av_opt_set_chlayout(pAV->swResampleCtx,        "out_chlayout", &out_ch_layout,                              0);
                if( pAV->verbose ) {
                    char buf1[256], buf2[256];
                    sp_av_channel_layout_describe(&pAV->pACodecCtx->ch_layout, buf1, sizeof(buf1));
                    sp_av_channel_layout_describe(&out_ch_layout, buf2, sizeof(buf2));
                    fprintf(stderr, "A Resample: channels %d -> %d, layout %s -> %s, rate %d -> %d, fmt 0x%x -> 0x%x\n",
                            pAV->aChannels, aChannelsOut, buf1, buf2,
                            pAV->aSampleRate, aSampleRateOut, (int)pAV->aSampleFmt, (int)aSampleFmtOut);
                }
                sp_av_channel_layout_uninit(&out_ch_layout);
#endif
                sp_av_opt_set_int(pAV->swResampleCtx,        "in_sample_rate",     pAV->aSampleRate,                           0);
                sp_av_opt_set_int(pAV->swResampleCtx,        "out_sample_rate",    aSampleRateOut,                             0);
                sp_av_opt_set_sample_fmt(pAV->swResampleCtx, "in_sample_fmt",      pAV->aSampleFmt,                            0);
                sp_av_opt_set_sample_fmt(pAV->swResampleCtx, "out_sample_fmt",     aSampleFmtOut,                              0);

                if ( sp_swr_init(pAV->swResampleCtx) < 0 ) {
                    sp_swr_free(&pAV->swResampleCtx);
                    pAV->swResampleCtx = NULL;
                    fprintf(stderr, "error initializing swresample ctx\n");
                } else {
                    // OK
                    pAV->aSampleFmtOut = aSampleFmtOut;
                    pAV->aChannelsOut = aChannelsOut;
                    pAV->aSampleRateOut = aSampleRateOut;
                    pAV->aSinkSupport = 1;
                }
            }
        }
        if(pAV->verbose) {
            fprintf(stderr, "Info: Need resample %d, Use swresample %d\n", 
                pAV->aSinkSupport, NULL!=pAV->swResampleCtx);
        }

        // Allocate audio frames
        // FIXME: Libav Binary compatibility! JAU01
        pAV->aFrameCount = 1;
        pAV->pANIOBuffers = calloc(pAV->aFrameCount, sizeof(NIOBuffer_t));
        pAV->pAFrames = calloc(pAV->aFrameCount, sizeof(AVFrame*));
        pAV->pAFrames[0] = pAFrame0;
        for(i=1; i<pAV->aFrameCount; i++) {
            pAV->pAFrames[i] = sp_av_frame_alloc();
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
        pAV->pVCodecPar = pAV->pVStream->codecpar;
        #if 0
        pAV->pVCodecCtx->get_format = my_get_format;
        #endif

        if (pAV->pVCodecPar->bit_rate) {
            // FIXME: Libav Binary compatibility! JAU01
            pAV->bps_video = pAV->pVCodecPar->bit_rate;
        }

        // Find the decoder for the video stream
        pAV->pVCodec=sp_avcodec_find_decoder(pAV->pVCodecPar->codec_id);
        if(pAV->pVCodec==NULL) {
            JoglCommon_throwNewRuntimeException(env, "Couldn't find video codec for codec_id %d", pAV->pVCodecPar->codec_id);
            return;
        }

        // Allocate the decoder context for the video stream
        pAV->pVCodecCtx = sp_avcodec_alloc_context3(pAV->pVCodec);
        if(pAV->pVCodecCtx==NULL) {
            JoglCommon_throwNewRuntimeException(env, "Couldn't allocate video decoder context for codec_id %d", pAV->pVCodecPar->codec_id);
            return;
        }
        res = sp_avcodec_parameters_to_context(pAV->pVCodecCtx, pAV->pVCodecPar);
        if(res<0) {
            JoglCommon_throwNewRuntimeException(env, "Couldn't copy video codec-par to context");
            return;
        }
        // Customize ..
        pAV->pVCodecCtx->pkt_timebase = pAV->pVStream->time_base;
        // pAV->pVCodecCtx->thread_count=2;
        // pAV->pVCodecCtx->thread_type=FF_THREAD_FRAME|FF_THREAD_SLICE; // Decode more than one frame at once
        pAV->pVCodecCtx->thread_count=0;
        pAV->pVCodecCtx->thread_type=0;
        pAV->pVCodecCtx->workaround_bugs=FF_BUG_AUTODETECT;
        pAV->pVCodecCtx->skip_frame=AVDISCARD_DEFAULT;

        sp_avcodec_string(pAV->vcodec, sizeof(pAV->vcodec), pAV->pVCodecCtx, 0);

        // Open codec
        MY_MUTEX_LOCK(env, mutex_avcodec_openclose);
        {
            res = sp_avcodec_open2(pAV->pVCodecCtx, pAV->pVCodec, NULL);
        }
        MY_MUTEX_UNLOCK(env, mutex_avcodec_openclose);
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
        if( pAV->pVStream->avg_frame_rate.den && pAV->pVStream->avg_frame_rate.num ) {
            pAV->fps = my_av_q2f(pAV->pVStream->avg_frame_rate);
        } else if( pAV->pVStream->time_base.den && pAV->pVStream->time_base.num ) {
            pAV->fps = my_av_q2f_r(pAV->pVStream->time_base);
        } else {
            pAV->fps = 0.0f; // duh!
        }
        pAV->frames_video = pAV->pVStream->nb_frames;
            
        // Allocate video frame
        // FIXME: Libav Binary compatibility! JAU01
        pAV->vWidth = pAV->pVCodecCtx->width;
        pAV->vHeight = pAV->pVCodecCtx->height;
        pAV->vPixFmt = pAV->pVCodecCtx->pix_fmt; // AV_PIX_FMT_NONE
        pAV->vFlipped = JNI_FALSE;
        {   
            const AVPixFmtDescriptor* pixDesc = sp_av_pix_fmt_desc_get(pAV->vPixFmt);
            if( NULL != pixDesc ) {
                pAV->vBitsPerPixel = sp_av_get_bits_per_pixel(pixDesc);
                pAV->vBufferPlanes = my_getPlaneCount(pixDesc);
            } else {
                JoglCommon_throwNewRuntimeException(env, "Couldn't query AVPixFmtDescriptor from v-ctx pix_fmt 0x%x", (int)pAV->vPixFmt);
                return;
            }
        }

        if( pAV->verbose ) {
            fprintf(stderr, "V frame_size %d, frame_number %"PRId64", [afps %f, sfps %f] -> %f fps, nb_frames %"PRId64", size %dx%d, fmt 0x%X, bpp %d, planes %d, codecCaps 0x%X\n",
                pAV->pVCodecCtx->frame_size, getFrameNum(pAV->pVCodecCtx),
                my_av_q2f(pAV->pVStream->avg_frame_rate),
                my_av_q2f_r(pAV->pVStream->time_base),
                pAV->fps,
                pAV->pVStream->nb_frames,
                pAV->vWidth, pAV->vHeight, pAV->vPixFmt, pAV->vBitsPerPixel, pAV->vBufferPlanes, pAV->pVCodecCtx->codec->capabilities);
        }

        pAV->pVFrame=sp_av_frame_alloc();
        if( pAV->pVFrame == NULL ) {
            JoglCommon_throwNewRuntimeException(env, "Couldn't alloc video frame");
            return;
        }
        // Min. requirement for 'get_buffer2' !
        pAV->pVFrame->width = pAV->pVCodecCtx->width;
        pAV->pVFrame->height = pAV->pVCodecCtx->height;
        pAV->pVFrame->format = pAV->pVCodecCtx->pix_fmt;
        res = sp_avcodec_default_get_buffer2(pAV->pVCodecCtx, pAV->pVFrame, 0);
        if(0!=res) {
            JoglCommon_throwNewRuntimeException(env, "Couldn't peek video buffer dimension");
            return;
        }
        {
            int32_t vLinesize[4];
            if( pAV->vBufferPlanes > 1 ) {
                pAV->vBytesPerPixelPerPlane = 1;
                for(i=0; i<pAV->vBufferPlanes; i++) {
                    // FIXME: Libav Binary compatibility! JAU01
                    vLinesize[i] = pAV->pVFrame->linesize[i];
                    pAV->vTexWidth[i] = vLinesize[i] / pAV->vBytesPerPixelPerPlane ;
                }
            } else {
                pAV->vBytesPerPixelPerPlane = ( pAV->vBitsPerPixel + 7 ) / 8 ;
                vLinesize[0] = pAV->pVFrame->linesize[0];
                if( pAV->vPixFmt == AV_PIX_FMT_YUYV422 || 
                    pAV->vPixFmt == AV_PIX_FMT_UYVY422 ) 
                {
                    // Stuff 2x 16bpp (YUYV, UYVY) into one RGBA pixel!
                    pAV->vTexWidth[0] = pAV->pVCodecCtx->width / 2;
                } else {
                    pAV->vTexWidth[0] = pAV->pVCodecCtx->width;
                }
            }
            if( pAV->verbose ) {
                for(i=0; i<pAV->vBufferPlanes; i++) {
                    fprintf(stderr, "Video: P[%d]: %d texw * %d bytesPP -> %d line\n", i, pAV->vTexWidth[i], pAV->vBytesPerPixelPerPlane, vLinesize[i]);
                }
            }
        }
        sp_av_frame_unref(pAV->pVFrame);
    }

    if(0<=pAV->sid) {
        // Get a pointer to the codec context for the video stream
        // FIXME: Libav Binary compatibility! JAU01
        pAV->pSCodecPar = pAV->pSStream->codecpar;
        #if 0
        pAV->pSCodecCtx->get_format = my_get_format;
        #endif

        // Find the decoder for the video stream
        pAV->pSCodec=sp_avcodec_find_decoder(pAV->pSCodecPar->codec_id);
        if(pAV->pSCodec==NULL) {
            JoglCommon_throwNewRuntimeException(env, "Couldn't find subtitle codec for codec_id %d", pAV->pSCodecPar->codec_id);
            return;
        }

        // Allocate the decoder context for the video stream
        pAV->pSCodecCtx = sp_avcodec_alloc_context3(pAV->pSCodec);
        if(pAV->pSCodecCtx==NULL) {
            JoglCommon_throwNewRuntimeException(env, "Couldn't allocate subtitle decoder context for codec_id %d", pAV->pSCodecPar->codec_id);
            return;
        }
        res = sp_avcodec_parameters_to_context(pAV->pSCodecCtx, pAV->pSCodecPar);
        if(res<0) {
            JoglCommon_throwNewRuntimeException(env, "Couldn't copy video codec-par to context");
            return;
        }
        // Customize ..
        pAV->pSCodecCtx->pkt_timebase = pAV->pSStream->time_base;
        pAV->pVCodecCtx->thread_count=0;
        pAV->pVCodecCtx->thread_type=0;
        pAV->pVCodecCtx->workaround_bugs=FF_BUG_AUTODETECT;
        pAV->pVCodecCtx->skip_frame=AVDISCARD_DEFAULT;

        sp_avcodec_string(pAV->scodec, sizeof(pAV->scodec), pAV->pSCodecCtx, 0);

        // Open codec
        MY_MUTEX_LOCK(env, mutex_avcodec_openclose);
        {
            res = sp_avcodec_open2(pAV->pSCodecCtx, pAV->pSCodec, NULL);
        }
        MY_MUTEX_UNLOCK(env, mutex_avcodec_openclose);
        if(res<0) {
            JoglCommon_throwNewRuntimeException(env, "Couldn't open subtitle codec %d, %s", pAV->pSCodecCtx->codec_id, pAV->scodec);
            return;
        }
    }
    pAV->vPTS=0;
    pAV->aPTS=0;
    pAV->sPTS=0;
    initPTSStats(&pAV->vPTSStats);
    initPTSStats(&pAV->aPTSStats);
    pAV->ready = 1;
    _updateJavaAttributes(env, pAV);
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
    if( 0 == pAV->ready ) {
        return 0;
    }

    jint resPTS = INVALID_PTS;
    uint8_t * pkt_odata;
    int pkt_osize;

    const int avRes = sp_av_read_frame(pAV->pFormatCtx, pAV->packet);
    if( AVERROR_EOF == avRes || ( pAV->pFormatCtx->pb && pAV->pFormatCtx->pb->eof_reached ) ) {
        if( pAV->verbose ) {
            fprintf(stderr, "EOS: avRes[res %d, eos %d], pb-EOS %d\n", 
                avRes, AVERROR_EOF == avRes, 
                ( pAV->pFormatCtx->pb && pAV->pFormatCtx->pb->eof_reached ) );
        }
        resPTS = END_OF_STREAM_PTS;
    } else if( 0 <= avRes ) {
        if( pAV->verbose ) {
            fprintf(stderr, "P: ptr %p, size %d\n", pAV->packet->data, pAV->packet->size);
        }
        int send_pkt = 1; // only send pkt once
        int32_t stream_id = pAV->packet->stream_index;
        if(stream_id == pAV->aid) {
            // Decode audio frame
            if(NULL == pAV->pAFrames) { // no audio registered
                sp_av_packet_unref(pAV->packet);
                return INVALID_PTS;
            }
            int res = 0;
            for (int frameCount=0; 0 <= res || 0 == frameCount; ++frameCount) {
                AVFrame* pAFrameCurrent = pAV->pAFrames[pAV->aFrameCurrent];
                sp_av_frame_unref(pAFrameCurrent);
                pAV->aFrameCurrent = ( pAV->aFrameCurrent + 1 ) % pAV->aFrameCount ;

                if( 0 < send_pkt ) { // only send pkt once
                    res = sp_avcodec_send_packet(pAV->pACodecCtx, pAV->packet);
                    if ( 0 == res ) {
                        // OK
                        send_pkt = 0;
                    } else if ( AVERROR(EAGAIN) == res ) {
                        // input is not accepted in the current state, continue draining frames, then resend package
                        res = 0;
                        if( pAV->verbose ) {
                            fprintf(stderr, "A-P: EAGAIN @ %d\n", frameCount);
                        }
                    } else if ( AVERROR_EOF == res ) {
                        // the decoder has been flushed, and no new packets can be sent to it, continue draining frames
                        send_pkt = 0;
                        res = 0;
                        if( pAV->verbose ) {
                            fprintf(stderr, "A-P: EOF @ %d\n", frameCount);
                        }
                    } else if ( 0 > res ) {
                        // error, but continue draining frames
                        send_pkt = 0;
                        res = 0;
                        if( pAV->verbose ) {
                            fprintf(stderr, "A-P: ERROR %d @ %d\n", res, frameCount);
                        }
                    }
                }
                res = sp_avcodec_receive_frame(pAV->pACodecCtx, pAFrameCurrent);
                if( 0 > res ) {
                    if ( AVERROR(EAGAIN) == res ) {
                        // output is not available in this state - user must try to send new input
                        res = 0;
                        if( 0 == frameCount && pAV->verbose ) {
                            fprintf(stderr, "A-F: EAGAIN @ %d\n", frameCount); // drained at start
                        } // else expected to be drained
                    } else if ( AVERROR_EOF == res ) {
                        // the decoder has been fully flushed
                        res = 0;
                        if( pAV->verbose ) {
                            fprintf(stderr, "A-F: EOF @ %d\n", frameCount);
                        }
                    } else {
                        if( pAV->verbose ) {
                            fprintf(stderr, "A-F: ERROR %d @ %d\n", res, frameCount);
                        }
                    }
                    break; // end loop
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

                // FIXME: Libav Binary compatibility! JAU01
                const AVRational pts_time_base = pAV->pAStream->time_base;
                const int64_t pkt_pts = pAFrameCurrent->pts;
                const int64_t pkt_dts = pAFrameCurrent->pkt_dts;
                const int64_t fix_pts = evalPTS(&pAV->aPTSStats, pkt_pts, pkt_dts);
                const int64_t best_pts = pAFrameCurrent->best_effort_timestamp;
                if( AV_NOPTS_VALUE != best_pts ) { 
                    pAV->aPTS =  my_av_q2i32( best_pts * 1000, pts_time_base);
                } else if( AV_NOPTS_VALUE != fix_pts ) {
                    pAV->aPTS =  my_av_q2i32( fix_pts * 1000, pts_time_base);
                } else { // subsequent frames or invalid PTS ..
                    const int32_t bytesPerSample = sp_av_get_bytes_per_sample( pAV->pACodecCtx->sample_fmt );
                    pAV->aPTS += data_size / ( pAV->aChannels * bytesPerSample * ( pAV->aSampleRate / 1000 ) );
                }
                if( pAV->verbose ) {
                    const AVRational dur_time_base = pAV->pACodecCtx->time_base;
                    const int32_t bPTS = AV_NOPTS_VALUE != best_pts ? my_av_q2i32( best_pts * 1000, pts_time_base) : 0;
                    const int32_t aPTS = AV_NOPTS_VALUE != pkt_pts ? my_av_q2i32( pkt_pts * 1000, pts_time_base) : 0;
                    const int32_t aDTS = AV_NOPTS_VALUE != pkt_dts ? my_av_q2i32( pkt_dts * 1000, pts_time_base) : 0;

                    const double frame_delay_d = av_q2d(dur_time_base) * 1000.0;
                    const double frame_repeat_d = pAFrameCurrent->repeat_pict * (frame_delay_d * 0.5);

                    const int32_t frame_delay_i = my_av_q2i32(1000, dur_time_base);
                    const int32_t frame_repeat_i = pAFrameCurrent->repeat_pict * (frame_delay_i / 2);

                    const char * warn = frame_repeat_i > 0 ? "REPEAT" : "NORMAL" ;

                    fprintf(stderr, "A fix_pts %d, best %d [%"PRId64"], pts %d [pkt_pts %"PRId64"], dts %d [pkt_dts %"PRId64"], time d(%lf ms + r %lf = %lf ms), i(%d ms + r %d = %d ms) - %s - f# %d, aFrame %d/%d %p, dataPtr %p, dataSize %d\n",
                            pAV->aPTS, bPTS, best_pts, aPTS, pkt_pts, aDTS, pkt_dts, 
                            frame_delay_d, frame_repeat_d, (frame_delay_d + frame_repeat_d),
                            frame_delay_i, frame_repeat_i, (frame_delay_i + frame_repeat_i), warn, frameCount,
                            pAV->aFrameCurrent, pAV->aFrameCount, pAFrameCurrent, pAFrameCurrent->data[0], data_size);
                    // fflush(NULL);
                }

                if( NULL != env ) {
                    void* data_ptr = pAFrameCurrent->data[0]; // default

                    if( NULL != pAV->swResampleCtx ) {
                        uint8_t *tmp_out;
                        int out_samples=-1, out_size, out_linesize;
                        int osize      = sp_av_get_bytes_per_sample( pAV->aSampleFmtOut );
                        int nb_samples = sp_swr_get_out_samples(pAV->swResampleCtx, pAFrameCurrent->nb_samples);

                        out_size = sp_av_samples_get_buffer_size(&out_linesize,
                                                                 pAV->aChannelsOut,
                                                                 nb_samples,
                                                                 pAV->aSampleFmtOut, 0 /* align */);

                        tmp_out = sp_av_realloc(pAV->aResampleBuffer, out_size);
                        if (!tmp_out) {
                            JoglCommon_throwNewRuntimeException(env, "Couldn't alloc resample buffer of size %d", out_size);
                            return INVALID_PTS;
                        }
                        pAV->aResampleBuffer = tmp_out;

                        if( NULL != pAV->swResampleCtx ) {
                            out_samples =  sp_swr_convert(pAV->swResampleCtx, 
                                                          &pAV->aResampleBuffer, nb_samples,
                                                          (const uint8_t **)pAFrameCurrent->data, pAFrameCurrent->nb_samples);
                        }
                        if (out_samples < 0) {
                            JoglCommon_throwNewRuntimeException(env, "avresample_convert() failed");
                            return INVALID_PTS;
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
                        (*env)->DeleteLocalRef(env, jSampleData);
                        if(pAV->verbose) {
                            fprintf(stderr, "A NIO: Alloc ptr %p / ref %p, %d bytes\n", 
                                pNIOBufferCurrent->origPtr, pNIOBufferCurrent->nioRef, pNIOBufferCurrent->size);
                        }
                    }
                    (*env)->CallVoidMethod(env, pAV->ffmpegMediaPlayer, ffmpeg_jni_mid_pushSound, pNIOBufferCurrent->nioRef, data_size, pAV->aPTS);
                    JoglCommon_ExceptionCheck1_throwNewRuntimeException(env, "FFmpeg: Exception occured at pushSound(..)");
                }
            } // draining frames loop
        } else if(stream_id == pAV->vid) {
            // Decode video frame
            if(NULL == pAV->pVFrame) {
                sp_av_packet_unref(pAV->packet);
                return INVALID_PTS;
            }
            int res = 0;
            for (int frameCount=0; 0 <= res || 0 == frameCount; ++frameCount) {
                sp_av_frame_unref(pAV->pVFrame);

                if( 0 < send_pkt ) { // only send pkt once
                    res = sp_avcodec_send_packet(pAV->pVCodecCtx, pAV->packet);
                    if ( 0 == res ) {
                        // OK
                        send_pkt = 0;
                    } else if ( AVERROR(EAGAIN) == res ) {
                        // input is not accepted in the current state, continue draining frames, then resend package
                        res = 0;
                        if( pAV->verbose ) {
                            fprintf(stderr, "V-P: EAGAIN @ %d\n", frameCount);
                        }
                    } else if ( AVERROR_EOF == res ) {
                        // the decoder has been flushed, and no new packets can be sent to it, continue draining frames
                        send_pkt = 0;
                        res = 0;
                        if( pAV->verbose ) {
                            fprintf(stderr, "V-P: EOF @ %d\n", frameCount);
                        }
                    } else if ( 0 > res ) {
                        // error, but continue draining frames
                        send_pkt = 0;
                        res = 0;
                        if( pAV->verbose ) {
                            fprintf(stderr, "V-P: ERROR %d @ %d\n", res, frameCount);
                        }
                    }
                }
                res = sp_avcodec_receive_frame(pAV->pVCodecCtx, pAV->pVFrame);
                if( 0 > res ) {
                    if ( AVERROR(EAGAIN) == res ) {
                        // output is not available in this state - user must try to send new input
                        res = 0;
                        if( 0 == frameCount && pAV->verbose ) {
                            fprintf(stderr, "V-F: EAGAIN @ %d\n", frameCount); // drained at start
                        } // else expected to be drained
                    } else if ( AVERROR_EOF == res ) {
                        // the decoder has been fully flushed
                        res = 0;
                        if( pAV->verbose ) {
                            fprintf(stderr, "V-F: EOF @ %d\n", frameCount);
                        }
                    } else {
                        if( pAV->verbose ) {
                            fprintf(stderr, "V-F: ERROR %d @ %d\n", res, frameCount);
                        }
                    }
                    break; // end loop
                }

                // FIXME: Libav Binary compatibility! JAU01
                const AVRational pts_time_base = pAV->pVStream->time_base;
                const int64_t pkt_pts = pAV->pVFrame->pts;
                const int64_t pkt_dts = pAV->pVFrame->pkt_dts;
                const int64_t fix_pts = evalPTS(&pAV->vPTSStats, pkt_pts, pkt_dts);
                const int64_t best_pts = pAV->pVFrame->best_effort_timestamp;
                if( AV_NOPTS_VALUE != best_pts ) { 
                    pAV->vPTS =  my_av_q2i32( best_pts * 1000, pts_time_base);
                } else if( AV_NOPTS_VALUE != fix_pts ) { // discard invalid PTS ..
                    pAV->vPTS =  my_av_q2i32( fix_pts * 1000, pts_time_base);
                }
                if( pAV->verbose ) {
                    const AVRational dur_time_base = pAV->pVCodecCtx->time_base;
                    const int32_t bPTS = AV_NOPTS_VALUE != best_pts ? my_av_q2i32( best_pts * 1000, pts_time_base) : 0;
                    const int32_t vPTS = AV_NOPTS_VALUE != pkt_pts ? my_av_q2i32( pkt_pts * 1000, pts_time_base) : 0;
                    const int32_t vDTS = AV_NOPTS_VALUE != pkt_dts ? my_av_q2i32( pkt_dts * 1000, pts_time_base) : 0;

                    const double frame_delay_d = av_q2d(dur_time_base);
                    const double frame_repeat_d = pAV->pVFrame->repeat_pict * (frame_delay_d * 0.5);

                    const int32_t frame_delay_i = my_av_q2i32(1000, dur_time_base);
                    const int32_t frame_repeat_i = pAV->pVFrame->repeat_pict * (frame_delay_i / 2);

                    const char * warn = frame_repeat_i > 0 ? "REPEAT" : "NORMAL" ;

                    fprintf(stderr, "V fix_pts %d, best %d [%"PRId64"], pts %d [pkt_pts %"PRId64"], dts %d [pkt_dts %"PRId64"], time d(%lf s + r %lf = %lf s), i(%d ms + r %d = %d ms) - %s - f# %d, data %p, lsz %d\n",
                            pAV->vPTS, bPTS, best_pts, vPTS, pkt_pts, vDTS, pkt_dts, 
                            frame_delay_d, frame_repeat_d, (frame_delay_d + frame_repeat_d),
                            frame_delay_i, frame_repeat_i, (frame_delay_i + frame_repeat_i), warn, frameCount,
                            pAV->pVFrame->data[0], pAV->pVFrame->linesize[0]);
                    // fflush(NULL);
                }
                if( 0 == pAV->pVFrame->linesize[0] ) {
                    sp_av_frame_unref(pAV->pVFrame);
                    continue;
                }
                resPTS = pAV->vPTS; // Video Frame!

                int p_offset[] = { 0, 0, 0, 0 };
                if( pAV->pVFrame->linesize[0] < 0 ) {
                    if( JNI_FALSE == pAV->vFlipped ) {
                        pAV->vFlipped = JNI_TRUE;
                        _setIsGLOriented(env, pAV);
                    }

                    // image bottom-up
                    int h_1 = pAV->pVCodecCtx->height - 1;
                    p_offset[0] = pAV->pVFrame->linesize[0] * h_1;
                    if( pAV->vBufferPlanes > 1 ) {
                        p_offset[1] = pAV->pVFrame->linesize[1] * h_1;
                    }
                    if( pAV->vBufferPlanes > 2 ) {
                        p_offset[2] = pAV->pVFrame->linesize[2] * h_1;
                    }
                    /**
                    if( pAV->vBufferPlanes > 3 ) {
                        p_offset[3] = pAV->pVFrame->linesize[3] * h_1;
                    } */
                } else if( JNI_TRUE == pAV->vFlipped ) {
                    pAV->vFlipped = JNI_FALSE;
                    _setIsGLOriented(env, pAV);
                }

                // 1st plane or complete packed frame
                // FIXME: Libav Binary compatibility! JAU01
                DBG_TEXSUBIMG2D_a('Y',pAV,1,1,1,0);
                pAV->procAddrGLTexSubImage2D(texTarget, 0, 
                                        0,                 0, 
                                        pAV->vTexWidth[0], pAV->pVCodecCtx->height, 
                                        texFmt, texType, pAV->pVFrame->data[0] + p_offset[0]);
                DBG_TEXSUBIMG2D_b(pAV);

                if( pAV->vPixFmt == AV_PIX_FMT_YUV420P || pAV->vPixFmt == AV_PIX_FMT_YUVJ420P ) {
                    // U plane
                    // FIXME: Libav Binary compatibility! JAU01
                    DBG_TEXSUBIMG2D_a('U',pAV,1,1,2,1);
                    pAV->procAddrGLTexSubImage2D(texTarget, 0, 
                                            pAV->pVCodecCtx->width, 0,
                                            pAV->vTexWidth[1],      pAV->pVCodecCtx->height/2, 
                                            texFmt, texType, pAV->pVFrame->data[1] + p_offset[1]);
                    DBG_TEXSUBIMG2D_b(pAV);
                    // V plane
                    // FIXME: Libav Binary compatibility! JAU01
                    DBG_TEXSUBIMG2D_a('V',pAV,1,1,2,2);
                    pAV->procAddrGLTexSubImage2D(texTarget, 0, 
                                            pAV->pVCodecCtx->width, pAV->pVCodecCtx->height/2,
                                            pAV->vTexWidth[2],      pAV->pVCodecCtx->height/2, 
                                            texFmt, texType, pAV->pVFrame->data[2] + p_offset[2]);
                    DBG_TEXSUBIMG2D_b(pAV);
                } else if( pAV->vPixFmt == AV_PIX_FMT_YUV422P || pAV->vPixFmt == AV_PIX_FMT_YUVJ422P ) {
                    // U plane
                    // FIXME: Libav Binary compatibility! JAU01
                    DBG_TEXSUBIMG2D_a('U',pAV,1,1,1,1);
                    pAV->procAddrGLTexSubImage2D(texTarget, 0, 
                                            pAV->pVCodecCtx->width, 0,
                                            pAV->vTexWidth[1],      pAV->pVCodecCtx->height, 
                                            texFmt, texType, pAV->pVFrame->data[1] + p_offset[1]);
                    DBG_TEXSUBIMG2D_b(pAV);
                    // V plane
                    // FIXME: Libav Binary compatibility! JAU01
                    DBG_TEXSUBIMG2D_a('V',pAV,3,2,1,1);
                    pAV->procAddrGLTexSubImage2D(texTarget, 0, 
                                            pAV->pVCodecCtx->width+pAV->pVCodecCtx->width/2, 0,
                                            pAV->vTexWidth[2],      pAV->pVCodecCtx->height, 
                                            texFmt, texType, pAV->pVFrame->data[2] + p_offset[2]);
                    DBG_TEXSUBIMG2D_b(pAV);
                } // FIXME: Add more planar formats !

                // We might want a sync here, ensuring the texture data is uploaded?
                //
                // No, glTexSubImage2D() shall block until new pixel data are taken, 
                // i.e. shall be a a synchronous client command
                //
                // pAV->procAddrGLFinish(); // No sync required and too expensive for multiple player
                pAV->procAddrGLFlush(); // No sync required, but be nice

                sp_av_frame_unref(pAV->pVFrame);
            } // draining frames loop
        } else if(stream_id == pAV->sid) {
            // Decode Subtitle package
            int res = 0;
            int got_sub = 0, got_sub2 = 0;
            AVSubtitle sub;

            res = sp_avcodec_decode_subtitle2(pAV->pSCodecCtx, &sub, &got_sub, pAV->packet);
            if (0 > res) {
                res = 0;
                if( pAV->verbose ) {
                    fprintf(stderr, "S-P: EOF.0\n");
                }
            } else {
                // OK
                if( !got_sub ) {
                    if( pAV->packet->data ) {
                        // EAGAIN
                    } else {
                        // EOF
                        if( pAV->verbose ) {
                            fprintf(stderr, "S-P: EOF.1\n");
                        }
                    }
                } else {
                    if (!pAV->packet->data) {
                        // .. pending ..
                        if( pAV->verbose ) {
                            fprintf(stderr, "S-P: Pending\n");
                        }
                    } else {
                        got_sub2 = 1;
                    }
                }
            }
            if( got_sub2 ) {
              int32_t sPTS, sStart, sEnd;
              if( AV_NOPTS_VALUE == sub.pts ) {
                  sPTS = -1;
                  sStart = -1;
                  sEnd = -1;
              } else {
                  sPTS = my_av_q2i32( sub.pts * 1000, AV_TIME_BASE_Q);
                  sStart = my_av_q2i32( ( sub.pts + sub.start_display_time ) * 1000, AV_TIME_BASE_Q);
                  sEnd = my_av_q2i32( ( sub.pts + sub.end_display_time ) * 1000, AV_TIME_BASE_Q);
              }
              for(unsigned int i=0; i<sub.num_rects; ++i) {
                AVSubtitleRect* r = sub.rects[i];
                if( SUBTITLE_TEXT == r->type && NULL != r->text ) {
                    if( pAV->verbose ) {
                        fprintf(stderr, "S[f %d, i %d, pts %d[%d..%d]]: %s\n", (int)r->type, i, r->text, sPTS, sStart, sEnd);
                    }
                    (*env)->CallVoidMethod(env, pAV->ffmpegMediaPlayer, ffmpeg_jni_mid_pushSubtitleText, (*env)->NewStringUTF(env, r->text), sPTS, sStart, sEnd);
                    JoglCommon_ExceptionCheck1_throwNewRuntimeException(env, "FFmpeg: Exception occured at pushSubtitleText(..)");
                } else if( SUBTITLE_ASS == r->type && NULL != r->ass ) {
                    if( pAV->verbose ) {
                        fprintf(stderr, "S[f %d, i %d, pts %d[%d..%d]]: %s\n", (int)r->type, i, r->ass, sPTS, sStart, sEnd);
                    }
                    (*env)->CallVoidMethod(env, pAV->ffmpegMediaPlayer, ffmpeg_jni_mid_pushSubtitleASS, (*env)->NewStringUTF(env, r->ass), sPTS, sStart, sEnd);
                    JoglCommon_ExceptionCheck1_throwNewRuntimeException(env, "FFmpeg: Exception occured at pushSubtitleASS(..)");
                } else {
                    if( pAV->verbose ) {
                        fprintf(stderr, "S[f %d, i %d]: null\n", (int)r->type, i);
                    }
                }
              }
              pAV->sPTS = sPTS;
            }
            if( got_sub ) {
                sp_avsubtitle_free(&sub);
            }
        } // stream_id selection
        sp_av_packet_unref(pAV->packet);
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
    if( 0 == pAV->ready ) {
        return 0;
    }
    return sp_av_read_play(pAV->pFormatCtx);
}
JNIEXPORT jint JNICALL FF_FUNC(pause0)
  (JNIEnv *env, jobject instance, jlong ptr)
{
    FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)((void *)((intptr_t)ptr));
    if( 0 == pAV->ready ) {
        return 0;
    }
    return sp_av_read_pause(pAV->pFormatCtx);
}

JNIEXPORT jint JNICALL FF_FUNC(seek0)
  (JNIEnv *env, jobject instance, jlong ptr, jint pos1)
{
    const FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)((void *)((intptr_t)ptr));
    if( 0 == pAV->ready ) {
        return 0;
    }
    int64_t pos0, pts0;
    int streamID;
    AVRational time_base;
    if( pAV->vid >= 0 ) {
        pos0 = pAV->vPTS;
        streamID = pAV->vid;
        time_base = pAV->pVStream->time_base;
        pts0 = pAV->pVFrame->pts;
    } else if( pAV->aid >= 0 ) {
        pos0 = pAV->aPTS;
        streamID = pAV->aid;
        time_base = pAV->pAStream->time_base;
        pts0 = pAV->pAFrames[pAV->aFrameCurrent]->pts;
    } else {
        return pAV->vPTS;
    }
    int64_t pts1 = (int64_t) (pos1 * (int64_t) time_base.den)
                           / (1000 * (int64_t) time_base.num);
    if(pAV->verbose) {
        fprintf(stderr, "SEEK: vid %d, aid %d, pos0 %"PRId64", pos1 %d, pts: %"PRId64" -> %"PRId64"\n", pAV->vid, pAV->aid, pos0, pos1, pts0, pts1);
    }
    int flags = 0;
    if(pos1 < pos0) {
        flags |= AVSEEK_FLAG_BACKWARD;
    }
    int res = -2;
    if(HAS_FUNC(sp_av_seek_frame)) {
        if(pAV->verbose) {
            fprintf(stderr, "SEEK.0: pre  : s %"PRId64" / %"PRId64" -> t %d / %"PRId64"\n", pos0, pts0, pos1, pts1);
        }
        sp_av_seek_frame(pAV->pFormatCtx, streamID, pts1, flags);
    } else if(HAS_FUNC(sp_avformat_seek_file)) {
        int64_t ptsD = pts1 - pts0;
        int64_t seek_min    = ptsD > 0 ? pts1 - ptsD : INT64_MIN;
        int64_t seek_max    = ptsD < 0 ? pts1 - ptsD : INT64_MAX;
        if(pAV->verbose) {
            fprintf(stderr, "SEEK.1: pre  : s %"PRId64" / %"PRId64" -> t %d / %"PRId64" [%"PRId64" .. %"PRId64"]\n", 
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
    if(NULL != pAV->pSCodecCtx) {
        sp_avcodec_flush_buffers( pAV->pSCodecCtx );
    }
    const jint rPTS =  my_av_q2i32( ( pAV->vid >= 0 ? pAV->pVFrame->pts : pAV->pAFrames[pAV->aFrameCurrent]->pts ) * 1000, time_base);
    if(pAV->verbose) {
        fprintf(stderr, "SEEK: post : res %d, u %d\n", res, rPTS);
    }
    return rPTS;
}

JNIEXPORT jint JNICALL FF_FUNC(getVideoPTS0)
  (JNIEnv *env, jobject instance, jlong ptr)
{
    FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)((void *)((intptr_t)ptr));
    if( 0 == pAV->ready ) {
        return 0;
    }
    return pAV->vPTS;
}

JNIEXPORT jint JNICALL FF_FUNC(getAudioPTS0)
  (JNIEnv *env, jobject instance, jlong ptr)
{
    FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)((void *)((intptr_t)ptr));
    if( 0 == pAV->ready ) {
        return 0;
    }
    return pAV->aPTS;
}

JNIEXPORT jint JNICALL FF_FUNC(getChapterCount0)
  (JNIEnv *env, jobject instance, jlong ptr)
{
    FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)((void *)((intptr_t)ptr));
    if( 0 == pAV->ready ) {
        return 0;
    }
    return pAV->pFormatCtx->nb_chapters;
}

JNIEXPORT jint JNICALL FF_FUNC(getChapterID0)
  (JNIEnv *env, jobject instance, jlong ptr, jint idx)
{
    FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)((void *)((intptr_t)ptr));
    if( 0 == pAV->ready || idx >= pAV->pFormatCtx->nb_chapters ) {
        return 0;
    }
    AVChapter *chapter = pAV->pFormatCtx->chapters[idx];
    return chapter->id;
}

JNIEXPORT jint JNICALL FF_FUNC(getChapterStartPTS0)
  (JNIEnv *env, jobject instance, jlong ptr, jint idx)
{
    FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)((void *)((intptr_t)ptr));
    if( 0 == pAV->ready || idx >= pAV->pFormatCtx->nb_chapters ) {
        return 0;
    }
    AVChapter *chapter = pAV->pFormatCtx->chapters[idx];
    return my_av_q2i32( chapter->start * 1000, chapter->time_base);
}

JNIEXPORT jint JNICALL FF_FUNC(getChapterEndPTS0)
  (JNIEnv *env, jobject instance, jlong ptr, jint idx)
{
    FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)((void *)((intptr_t)ptr));
    if( 0 == pAV->ready || idx >= pAV->pFormatCtx->nb_chapters ) {
        return 0;
    }
    AVChapter *chapter = pAV->pFormatCtx->chapters[idx];
    return my_av_q2i32( chapter->end * 1000, chapter->time_base);
}

JNIEXPORT jstring JNICALL FF_FUNC(getChapterTitle0)
  (JNIEnv *env, jobject instance, jlong ptr, jint idx)
{
    FFMPEGToolBasicAV_t *pAV = (FFMPEGToolBasicAV_t *)((void *)((intptr_t)ptr));
    if( 0 == pAV->ready || idx >= pAV->pFormatCtx->nb_chapters ) {
        return NULL;
    }
    AVChapter *chapter = pAV->pFormatCtx->chapters[idx];
    const char* title = meta_get_title(chapter->metadata);
    return NULL != title ? (*env)->NewStringUTF(env, title) : NULL;
}

