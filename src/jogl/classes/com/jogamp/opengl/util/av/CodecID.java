/**
 * Copyright 2024 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.util.av;

/**
 * FFmpeg/libAV analog {@code AVCodecID}.
 * <p>
 * Use {@link CodecID#fromFFmpeg(int)} to convert from FFmpeg's {@code AVCodecID}
 * and {@link CodecID#toFFmpeg(CodecID) vice versa.
 * </p>
 */
public enum CodecID {
    NONE,

    //
    // Video Codecs
    //

    MPEG1VIDEO,
    MPEG2VIDEO, ///< preferred ID for MPEG-1/2 video decoding
    H261,
    H263,
    RV10,
    RV20,
    MJPEG,
    MJPEGB,
    LJPEG,
    SP5X,
    JPEGLS,
    MPEG4,
    RAWVIDEO,
    MSMPEG4V1,
    MSMPEG4V2,
    MSMPEG4V3,
    WMV1,
    WMV2,
    H263P,
    H263I,
    FLV1,
    SVQ1,
    SVQ3,
    DVVIDEO,
    HUFFYUV,
    CYUV,
    H264,
    INDEO3,
    VP3,
    THEORA,
    ASV1,
    ASV2,
    FFV1,
    ID_4XM,
    VCR1,
    CLJR,
    MDEC,
    ROQ,
    INTERPLAY_VIDEO,
    XAN_WC3,
    XAN_WC4,
    RPZA,
    CINEPAK,
    WS_VQA,
    MSRLE,
    MSVIDEO1,
    IDCIN,
    ID_8BPS,
    SMC,
    FLIC,
    TRUEMOTION1,
    VMDVIDEO,
    MSZH,
    ZLIB,
    QTRLE,
    TSCC,
    ULTI,
    QDRAW,
    VIXL,
    QPEG,
    PNG,
    PPM,
    PBM,
    PGM,
    PGMYUV,
    PAM,
    FFVHUFF,
    RV30,
    RV40,
    VC1,
    WMV3,
    LOCO,
    WNV1,
    AASC,
    INDEO2,
    FRAPS,
    TRUEMOTION2,
    BMP,
    CSCD,
    MMVIDEO,
    ZMBV,
    AVS,
    SMACKVIDEO,
    NUV,
    KMVC,
    FLASHSV,
    CAVS,
    JPEG2000,
    VMNC,
    VP5,
    VP6,
    VP6F,
    TARGA,
    DSICINVIDEO,
    TIERTEXSEQVIDEO,
    TIFF,
    GIF,
    DXA,
    DNXHD,
    THP,
    SGI,
    C93,
    BETHSOFTVID,
    PTX,
    TXD,
    VP6A,
    AMV,
    VB,
    PCX,
    SUNRAST,
    INDEO4,
    INDEO5,
    MIMIC,
    RL2,
    ESCAPE124,
    DIRAC,
    BFI,
    CMV,
    MOTIONPIXELS,
    TGV,
    TGQ,
    TQI,
    AURA,
    AURA2,
    V210X,
    TMV,
    V210,
    DPX,
    MAD,
    FRWU,
    FLASHSV2,
    CDGRAPHICS,
    R210,
    ANM,
    BINKVIDEO,
    /** Also IFF_BYTERUN1 */
    IFF_ILBM,
    KGV1,
    YOP,
    VP8,
    PICTOR,
    ANSI,
    A64_MULTI,
    A64_MULTI5,
    R10K,
    MXPEG,
    LAGARITH,
    PRORES,
    JV,
    DFA,
    WMV3IMAGE,
    VC1IMAGE,
    UTVIDEO,
    BMV_VIDEO,
    VBLE,
    DXTORY,
    V410,
    XWD,
    CDXL,
    XBM,
    ZEROCODEC,
    MSS1,
    MSA1,
    TSCC2,
    MTS2,
    CLLC,
    MSS2,
    VP9,
    AIC,
    ESCAPE130,
    G2M,
    WEBP,
    HNM4_VIDEO,
    /** Also H265 */
    HEVC,
    FIC,
    ALIAS_PIX,
    BRENDER_PIX,
    PAF_VIDEO,
    EXR,
    VP7,
    SANM,
    SGIRLE,
    MVC1,
    MVC2,
    HQX,
    TDSC,
    HQ_HQA,
    HAP,
    DDS,
    DXV,
    SCREENPRESSO,
    RSCC,
    AVS2,
    PGX,
    AVS3,
    MSP2,
    /** Also H266 {@value} */
    VVC,

    Y41P, // = 0x8000,
    AVRP,
    ID_012V,
    AVUI,
    AYUV,
    TARGA_Y216,
    V308,
    V408,
    YUV4,
    AVRN,
    CPIA,
    XFACE,
    SNOW,
    SMVJPEG,
    APNG,
    DAALA,
    CFHD,
    TRUEMOTION2RT,
    M101,
    MAGICYUV,
    SHEERVIDEO,
    YLC,
    PSD,
    PIXLET,
    SPEEDHQ,
    FMVC,
    SCPR,
    CLEARVIDEO,
    XPM,
    AV1,
    BITPACKED,
    MSCC,
    SRGC,
    SVG,
    GDV,
    FITS,
    IMM4,
    PROSUMER,
    MWSC,
    WCMV,
    RASC,
    HYMT,
    ARBC,
    AGM,
    LSCR,
    VP4,
    IMM5,
    MVDV,
    MVHA,
    CDTOONS,
    MV30,
    NOTCHLC,
    PFM,
    MOBICLIP,
    PHOTOCD,
    IPU,
    ARGO,
    CRI,
    SIMBIOSIS_IMX,
    SGA_VIDEO,
    GEM,
    VBN,
    JPEGXL,
    QOI,
    PHM,
    RADIANCE_HDR,
    WBMP,
    MEDIA100,
    VQC,

    //
    // Various PCM "codecs"
    //

    PCM_S16LE, // = 0x10000,
    PCM_S16BE,
    PCM_U16LE,
    PCM_U16BE,
    PCM_S8,
    PCM_U8,
    PCM_MULAW,
    PCM_ALAW,
    PCM_S32LE,
    PCM_S32BE,
    PCM_U32LE,
    PCM_U32BE,
    PCM_S24LE,
    PCM_S24BE,
    PCM_U24LE,
    PCM_U24BE,
    PCM_S24DAUD,
    PCM_ZORK,
    PCM_S16LE_PLANAR,
    PCM_DVD,
    PCM_F32BE,
    PCM_F32LE,
    PCM_F64BE,
    PCM_F64LE,
    PCM_BLURAY,
    PCM_LXF,
    S302M,
    PCM_S8_PLANAR,
    PCM_S24LE_PLANAR,
    PCM_S32LE_PLANAR,
    PCM_S16BE_PLANAR,
    PCM_S64LE,
    PCM_S64BE,
    PCM_F16LE,
    PCM_F24LE,
    PCM_VIDC,
    PCM_SGA,

    //
    // Various ADPCM codecs
    //

    ADPCM_IMA_QT, // = 0x11000,
    ADPCM_IMA_WAV,
    ADPCM_IMA_DK3,
    ADPCM_IMA_DK4,
    ADPCM_IMA_WS,
    ADPCM_IMA_SMJPEG,
    ADPCM_MS,
    ADPCM_4XM,
    ADPCM_XA,
    ADPCM_ADX,
    ADPCM_EA,
    ADPCM_G726,
    ADPCM_CT,
    ADPCM_SWF,
    ADPCM_YAMAHA,
    ADPCM_SBPRO_4,
    ADPCM_SBPRO_3,
    ADPCM_SBPRO_2,
    ADPCM_THP,
    ADPCM_IMA_AMV,
    ADPCM_EA_R1,
    ADPCM_EA_R3,
    ADPCM_EA_R2,
    ADPCM_IMA_EA_SEAD,
    ADPCM_IMA_EA_EACS,
    ADPCM_EA_XAS,
    ADPCM_EA_MAXIS_XA,
    ADPCM_IMA_ISS,
    ADPCM_G722,
    ADPCM_IMA_APC,
    ADPCM_VIMA,
    ADPCM_AFC,
    ADPCM_IMA_OKI,
    ADPCM_DTK,
    ADPCM_IMA_RAD,
    ADPCM_G726LE,
    ADPCM_THP_LE,
    ADPCM_PSX,
    ADPCM_AICA,
    ADPCM_IMA_DAT4,
    ADPCM_MTAF,
    ADPCM_AGM,
    ADPCM_ARGO,
    ADPCM_IMA_SSI,
    ADPCM_ZORK,
    ADPCM_IMA_APM,
    ADPCM_IMA_ALP,
    ADPCM_IMA_MTF,
    ADPCM_IMA_CUNNING,
    ADPCM_IMA_MOFLEX,
    ADPCM_IMA_ACORN,
    ADPCM_XMD,

    /* AMR */
    AMR_NB, // = 0x12000,
    AMR_WB,

    /* RealAudio codecs*/
    RA_144, // = 0x13000,
    RA_288,

    //
    // Various DPCM codecs
    //

    ROQ_DPCM, // = 0x14000,
    INTERPLAY_DPCM,
    XAN_DPCM,
    SOL_DPCM,
    SDX2_DPCM,
    GREMLIN_DPCM,
    DERF_DPCM,
    WADY_DPCM,
    CBD2_DPCM,

    //
    // Audio Codecs
    //

    MP2, // = 0x15000,
    MP3, ///< preferred ID for decoding MPEG audio layer 1, 2 or 3
    AAC,
    AC3,
    DTS,
    VORBIS,
    DVAUDIO,
    WMAV1,
    WMAV2,
    MACE3,
    MACE6,
    VMDAUDIO,
    FLAC,
    MP3ADU,
    MP3ON4,
    SHORTEN,
    ALAC,
    WESTWOOD_SND1,
    GSM, ///< as in Berlin toast format
    QDM2,
    COOK,
    TRUESPEECH,
    TTA,
    SMACKAUDIO,
    QCELP,
    WAVPACK,
    DSICINAUDIO,
    IMC,
    MUSEPACK7,
    MLP,
    GSM_MS, /* as found in WAV */
    ATRAC3,
    APE,
    NELLYMOSER,
    MUSEPACK8,
    SPEEX,
    WMAVOICE,
    WMAPRO,
    WMALOSSLESS,
    ATRAC3P,
    EAC3,
    SIPR,
    MP1,
    TWINVQ,
    TRUEHD,
    MP4ALS,
    ATRAC1,
    BINKAUDIO_RDFT,
    BINKAUDIO_DCT,
    AAC_LATM,
    QDMC,
    CELT,
    G723_1,
    G729,
    ID_8SVX_EXP,
    ID_8SVX_FIB,
    BMV_AUDIO,
    RALF,
    IAC,
    ILBC,
    OPUS,
    COMFORT_NOISE,
    TAK,
    METASOUND,
    PAF_AUDIO,
    ON2AVC,
    DSS_SP,
    CODEC2,
    FFWAVESYNTH,
    SONIC,
    SONIC_LS,
    EVRC,
    SMV,
    DSD_LSBF,
    DSD_MSBF,
    DSD_LSBF_PLANAR,
    DSD_MSBF_PLANAR,
    ID_4GV,
    INTERPLAY_ACM,
    XMA1,
    XMA2,
    DST,
    ATRAC3AL,
    ATRAC3PAL,
    DOLBY_E,
    APTX,
    APTX_HD,
    SBC,
    ATRAC9,
    HCOM,
    ACELP_KELVIN,
    MPEGH_3D_AUDIO,
    SIREN,
    HCA,
    FASTAUDIO,
    MSNSIREN,
    DFPWM,
    BONK,
    MISC4,
    APAC,
    FTR,
    WAVARC,
    RKA,

    //
    // Subtitle Codecs
    //
    DVD_SUB, //  = 0x17000,
    DVB_SUB,
    TEXT,  ///< raw UTF-8 text
    XSUB,
    SSA,
    MOV_TEXT,
    HDMV_PGS,
    DVB_TELETEXT,
    SRT,
    MICRODVD,
    EIA_608,
    JACOSUB,
    SAMI,
    REALTEXT,
    STL,
    SUBVIEWER1,
    SUBVIEWER,
    SUBRIP,
    WEBVTT,
    MPL2,
    VPLAYER,
    PJS,
    ASS,
    HDMV_TEXT,
    TTML,
    ARIB_CAPTION,

    COUNT      ///< number of codec IDs in this list
    ;

    /**
     * Converts given FFmpeg {@code AVCodecID} to {@link CodecID} or {@code CodecID#NONE} if not matched.
     */
    public static CodecID fromFFmpeg(final int ffmpegID) {
        final int ordinal;
        if( ffmpegID >= 0x17000 ) {
            ordinal = ffmpegID - 0x17000 + DVD_SUB.ordinal();
        } else if( ffmpegID >= 0x15000 ) {
            ordinal = ffmpegID - 0x15000 + MP2.ordinal();
        } else if( ffmpegID >= 0x14000 ) {
            ordinal = ffmpegID - 0x14000 + ROQ_DPCM.ordinal();
        } else if( ffmpegID >= 0x13000 ) {
            ordinal = ffmpegID - 0x13000 + RA_144.ordinal();
        } else if( ffmpegID >= 0x12000 ) {
            ordinal = ffmpegID - 0x12000 + AMR_NB.ordinal();
        } else if( ffmpegID >= 0x11000 ) {
            ordinal = ffmpegID - 0x11000 + ADPCM_IMA_QT.ordinal();
        } else if( ffmpegID >= 0x10000 ) {
            ordinal = ffmpegID - 0x10000 + PCM_S16LE.ordinal();
        } else if( ffmpegID >= 0x8000 ) {
            ordinal = ffmpegID - 0x8000 + Y41P.ordinal();
        } else {
            ordinal = ffmpegID;
        }

        /**
        Y41P, // = 0x8000,
        PCM_S16LE = 0x10000,
        ADPCM_IMA_QT = 0x11000,
        AMR_NB = 0x12000,
        RA_144 = 0x13000,
        ROQ_DPCM = 0x14000,
        MP2 = 0x15000,
        DVD_SUBTITLE = 0x17000,
        */

        final CodecID[] all = CodecID.values();
        if( 0 <= ordinal && ordinal < all.length ) {
            return all[ordinal];
        }
        return CodecID.NONE;
    }

    /** Converts given {@link CodecID} value into FFmpeg {@code AVCodecID} */
    public static int toFFmpeg(final CodecID id) {
        final int ordinal = id.ordinal();
        final int ffmpegID;
        if( ordinal >= DVD_SUB.ordinal() ) {
            ffmpegID = ordinal - DVD_SUB.ordinal() + 0x17000;
        } else if( ordinal >= MP2.ordinal() ) {
            ffmpegID = ordinal - MP2.ordinal() + 0x15000;
        } else if( ordinal >= ROQ_DPCM.ordinal() ) {
            ffmpegID = ordinal - ROQ_DPCM.ordinal() + 0x14000;
        } else if( ordinal >= RA_144.ordinal() ) {
            ffmpegID = ordinal - RA_144.ordinal() + 0x13000;
        } else if( ordinal >= AMR_NB.ordinal() ) {
            ffmpegID = ordinal - AMR_NB.ordinal() + 0x12000;
        } else if( ordinal >= ADPCM_IMA_QT.ordinal() ) {
            ffmpegID = ordinal - ADPCM_IMA_QT.ordinal() + 0x11000;
        } else if( ordinal >= PCM_S16LE.ordinal() ) {
            ffmpegID = ordinal - PCM_S16LE.ordinal() + 0x10000;
        } else if( ordinal >= Y41P.ordinal() ) {
            ffmpegID = ordinal - Y41P.ordinal() + 0x8000;
        } else {
            ffmpegID = ordinal;
        }
        return ffmpegID;
    }

    /** Returns {@code true} if given {@link CodecID} refers to a video codec */
    public static boolean isVideoCodec(final CodecID id) {
        return MPEG1VIDEO.ordinal() <= id.ordinal() && id.ordinal() < PCM_S16LE.ordinal();
    }
    /** Returns {@code true} if given {@link CodecID} refers to an audio PCM codec */
    public static boolean isAudioPCMCodec(final CodecID id) {
        return PCM_S16LE.ordinal() <= id.ordinal() && id.ordinal() < MP2.ordinal();
    }
    /**
     * Returns {@code true} if given {@link CodecID} refers to an audio codec.
     * @param id the {@link CodecID}
     * @param includePCM pass {@code true} to include {@link #isAudioPCMCodec(CodecID)}
     */
    public static boolean isAudioCodec(final CodecID id, final boolean includePCM) {
        return includePCM && PCM_S16LE.ordinal() <= id.ordinal() && id.ordinal() < DVD_SUB.ordinal() ||
                             MP2.ordinal() <= id.ordinal() && id.ordinal() < DVD_SUB.ordinal();
    }
    /** Returns {@code true} if given {@link CodecID} refers to a subtitle codec */
    public static boolean isSubtitleCodec(final CodecID id) {
        return DVD_SUB.ordinal() <= id.ordinal() && id.ordinal() < COUNT.ordinal();
    }
}

