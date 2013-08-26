package jogamp.opengl.util.av.impl;

import com.jogamp.opengl.util.av.AudioSink;
import com.jogamp.opengl.util.texture.TextureSequence.TextureFrame;

interface FFMPEGNatives {
        
    boolean initSymbols0(long[] symbols, int count);
    int getAvUtilMajorVersionCC0();
    int getAvFormatMajorVersionCC0();
    int getAvCodecMajorVersionCC0();
    int getAvResampleMajorVersionCC0();
    boolean initIDs0();
    
    long createInstance0(FFMPEGMediaPlayer upstream, boolean verbose);
    void destroyInstance0(long moviePtr);
    
    /**
     * Issues {@link #updateAttributes(int, int, int, int, int, int, int, float, int, int, String, String)}
     * and {@link #updateAttributes2(int, int, int, int, int, int, int, int, int, int)}.
     * <p>
     * Always uses {@link AudioSink.AudioFormat}:
     * <pre>
     *   [type PCM, sampleRate [10000(?)..44100..48000], sampleSize 16, channelCount 1-2, signed, littleEndian]
     * </pre>
     * </p>
     * 
     * @param moviePtr
     * @param url
     * @param vid
     * @param aid
     * @param aPrefChannelCount
     * @param aPrefSampleRate
     */
    void setStream0(long moviePtr, String url, String inFormat, int vid, int aid, int aMaxChannelCount, int aPrefSampleRate);
    void setGLFuncs0(long moviePtr, long procAddrGLTexSubImage2D, long procAddrGLGetError, long procAddrGLFlush, long procAddrGLFinish);

    int getVideoPTS0(long moviePtr);    
    
    int getAudioPTS0(long moviePtr);
    
    /**
     * @return resulting current video PTS, or {@link TextureFrame#INVALID_PTS}
     */
    int readNextPacket0(long moviePtr, int texTarget, int texFmt, int texType);
    
    int play0(long moviePtr);
    int pause0(long moviePtr);
    int seek0(long moviePtr, int position);
        
    /** FFMPEG/libAV Audio Sample Format */
    public static enum SampleFormat {
        // NONE = -1,
        U8,          ///< unsigned 8 bits
        S16,         ///< signed 16 bits
        S32,         ///< signed 32 bits
        FLT,         ///< float
        DBL,         ///< double

        U8P,         ///< unsigned 8 bits, planar
        S16P,        ///< signed 16 bits, planar
        S32P,        ///< signed 32 bits, planar
        FLTP,        ///< float, planar
        DBLP,        ///< double, planar
        
        COUNT;       ///< Number of sample formats.
        
        public static SampleFormat valueOf(int i) {
            for (SampleFormat fmt : SampleFormat.values()) {
                if(fmt.ordinal() == i) {
                    return fmt;
                }
            }
            return null;            
        }
    };

    /** FFMPEG/libAV Pixel Format */
    public static enum PixelFormat {
        // NONE= -1,
        YUV420P,   ///< planar YUV 4:2:0, 12bpp, (1 Cr & Cb sample per 2x2 Y samples)
        YUYV422,   ///< packed YUV 4:2:2, 16bpp, Y0 Cb Y1 Cr
        RGB24,     ///< packed RGB 8:8:8, 24bpp, RGBRGB...
        BGR24,     ///< packed RGB 8:8:8, 24bpp, BGRBGR...
        YUV422P,   ///< planar YUV 4:2:2, 16bpp, (1 Cr & Cb sample per 2x1 Y samples)
        YUV444P,   ///< planar YUV 4:4:4, 24bpp, (1 Cr & Cb sample per 1x1 Y samples)
        YUV410P,   ///< planar YUV 4:1:0,  9bpp, (1 Cr & Cb sample per 4x4 Y samples)
        YUV411P,   ///< planar YUV 4:1:1, 12bpp, (1 Cr & Cb sample per 4x1 Y samples)
        GRAY8,     ///<        Y        ,  8bpp
        MONOWHITE, ///<        Y        ,  1bpp, 0 is white, 1 is black, in each byte pixels are ordered from the msb to the lsb
        MONOBLACK, ///<        Y        ,  1bpp, 0 is black, 1 is white, in each byte pixels are ordered from the msb to the lsb
        PAL8,      ///< 8 bit with RGB32 palette
        YUVJ420P,  ///< planar YUV 4:2:0, 12bpp, full scale (JPEG), deprecated in favor of YUV420P and setting color_range
        YUVJ422P,  ///< planar YUV 4:2:2, 16bpp, full scale (JPEG), deprecated in favor of YUV422P and setting color_range
        YUVJ444P,  ///< planar YUV 4:4:4, 24bpp, full scale (JPEG), deprecated in favor of YUV444P and setting color_range
        XVMC_MPEG2_MC,///< XVideo Motion Acceleration via common packet passing
        XVMC_MPEG2_IDCT,
        UYVY422,   ///< packed YUV 4:2:2, 16bpp, Cb Y0 Cr Y1
        UYYVYY411, ///< packed YUV 4:1:1, 12bpp, Cb Y0 Y1 Cr Y2 Y3
        BGR8,      ///< packed RGB 3:3:2,  8bpp, (msb)2B 3G 3R(lsb)
        BGR4,      ///< packed RGB 1:2:1 bitstream,  4bpp, (msb)1B 2G 1R(lsb), a byte contains two pixels, the first pixel in the byte is the one composed by the 4 msb bits
        BGR4_BYTE, ///< packed RGB 1:2:1,  8bpp, (msb)1B 2G 1R(lsb)
        RGB8,      ///< packed RGB 3:3:2,  8bpp, (msb)2R 3G 3B(lsb)
        RGB4,      ///< packed RGB 1:2:1 bitstream,  4bpp, (msb)1R 2G 1B(lsb), a byte contains two pixels, the first pixel in the byte is the one composed by the 4 msb bits
        RGB4_BYTE, ///< packed RGB 1:2:1,  8bpp, (msb)1R 2G 1B(lsb)
        NV12,      ///< planar YUV 4:2:0, 12bpp, 1 plane for Y and 1 plane for the UV components, which are interleaved (first byte U and the following byte V)
        NV21,      ///< as above, but U and V bytes are swapped

        ARGB,      ///< packed ARGB 8:8:8:8, 32bpp, ARGBARGB...
        RGBA,      ///< packed RGBA 8:8:8:8, 32bpp, RGBARGBA...
        ABGR,      ///< packed ABGR 8:8:8:8, 32bpp, ABGRABGR...
        BGRA,      ///< packed BGRA 8:8:8:8, 32bpp, BGRABGRA...

        GRAY16BE,  ///<        Y        , 16bpp, big-endian
        GRAY16LE,  ///<        Y        , 16bpp, little-endian
        YUV440P,   ///< planar YUV 4:4:0 (1 Cr & Cb sample per 1x2 Y samples)
        YUVJ440P,  ///< planar YUV 4:4:0 full scale (JPEG), deprecated in favor of YUV440P and setting color_range
        YUVA420P,  ///< planar YUV 4:2:0, 20bpp, (1 Cr & Cb sample per 2x2 Y & A samples)
        VDPAU_H264,///< H.264 HW decoding with VDPAU, data[0] contains a vdpau_render_state struct which contains the bitstream of the slices as well as various fields extracted from headers
        VDPAU_MPEG1,///< MPEG-1 HW decoding with VDPAU, data[0] contains a vdpau_render_state struct which contains the bitstream of the slices as well as various fields extracted from headers
        VDPAU_MPEG2,///< MPEG-2 HW decoding with VDPAU, data[0] contains a vdpau_render_state struct which contains the bitstream of the slices as well as various fields extracted from headers
        VDPAU_WMV3,///< WMV3 HW decoding with VDPAU, data[0] contains a vdpau_render_state struct which contains the bitstream of the slices as well as various fields extracted from headers
        VDPAU_VC1, ///< VC-1 HW decoding with VDPAU, data[0] contains a vdpau_render_state struct which contains the bitstream of the slices as well as various fields extracted from headers
        RGB48BE,   ///< packed RGB 16:16:16, 48bpp, 16R, 16G, 16B, the 2-byte value for each R/G/B component is stored as big-endian
        RGB48LE,   ///< packed RGB 16:16:16, 48bpp, 16R, 16G, 16B, the 2-byte value for each R/G/B component is stored as little-endian

        RGB565BE,  ///< packed RGB 5:6:5, 16bpp, (msb)   5R 6G 5B(lsb), big-endian
        RGB565LE,  ///< packed RGB 5:6:5, 16bpp, (msb)   5R 6G 5B(lsb), little-endian
        RGB555BE,  ///< packed RGB 5:5:5, 16bpp, (msb)1A 5R 5G 5B(lsb), big-endian, most significant bit to 0
        RGB555LE,  ///< packed RGB 5:5:5, 16bpp, (msb)1A 5R 5G 5B(lsb), little-endian, most significant bit to 0

        BGR565BE,  ///< packed BGR 5:6:5, 16bpp, (msb)   5B 6G 5R(lsb), big-endian
        BGR565LE,  ///< packed BGR 5:6:5, 16bpp, (msb)   5B 6G 5R(lsb), little-endian
        BGR555BE,  ///< packed BGR 5:5:5, 16bpp, (msb)1A 5B 5G 5R(lsb), big-endian, most significant bit to 1
        BGR555LE,  ///< packed BGR 5:5:5, 16bpp, (msb)1A 5B 5G 5R(lsb), little-endian, most significant bit to 1

        VAAPI_MOCO, ///< HW acceleration through VA API at motion compensation entry-point, Picture.data[3] contains a vaapi_render_state struct which contains macroblocks as well as various fields extracted from headers
        VAAPI_IDCT, ///< HW acceleration through VA API at IDCT entry-point, Picture.data[3] contains a vaapi_render_state struct which contains fields extracted from headers
        VAAPI_VLD,  ///< HW decoding through VA API, Picture.data[3] contains a vaapi_render_state struct which contains the bitstream of the slices as well as various fields extracted from headers

        YUV420P16LE,  ///< planar YUV 4:2:0, 24bpp, (1 Cr & Cb sample per 2x2 Y samples), little-endian
        YUV420P16BE,  ///< planar YUV 4:2:0, 24bpp, (1 Cr & Cb sample per 2x2 Y samples), big-endian
        YUV422P16LE,  ///< planar YUV 4:2:2, 32bpp, (1 Cr & Cb sample per 2x1 Y samples), little-endian
        YUV422P16BE,  ///< planar YUV 4:2:2, 32bpp, (1 Cr & Cb sample per 2x1 Y samples), big-endian
        YUV444P16LE,  ///< planar YUV 4:4:4, 48bpp, (1 Cr & Cb sample per 1x1 Y samples), little-endian
        YUV444P16BE,  ///< planar YUV 4:4:4, 48bpp, (1 Cr & Cb sample per 1x1 Y samples), big-endian
        VDPAU_MPEG4,  ///< MPEG4 HW decoding with VDPAU, data[0] contains a vdpau_render_state struct which contains the bitstream of the slices as well as various fields extracted from headers
        DXVA2_VLD,    ///< HW decoding through DXVA2, Picture.data[3] contains a LPDIRECT3DSURFACE9 pointer

        RGB444LE,  ///< packed RGB 4:4:4, 16bpp, (msb)4A 4R 4G 4B(lsb), little-endian, most significant bits to 0
        RGB444BE,  ///< packed RGB 4:4:4, 16bpp, (msb)4A 4R 4G 4B(lsb), big-endian, most significant bits to 0
        BGR444LE,  ///< packed BGR 4:4:4, 16bpp, (msb)4A 4B 4G 4R(lsb), little-endian, most significant bits to 1
        BGR444BE,  ///< packed BGR 4:4:4, 16bpp, (msb)4A 4B 4G 4R(lsb), big-endian, most significant bits to 1
        Y400A,     ///< 8bit gray, 8bit alpha
        BGR48BE,   ///< packed RGB 16:16:16, 48bpp, 16B, 16G, 16R, the 2-byte value for each R/G/B component is stored as big-endian
        BGR48LE,   ///< packed RGB 16:16:16, 48bpp, 16B, 16G, 16R, the 2-byte value for each R/G/B component is stored as little-endian
        YUV420P9BE, ///< planar YUV 4:2:0, 13.5bpp, (1 Cr & Cb sample per 2x2 Y samples), big-endian
        YUV420P9LE, ///< planar YUV 4:2:0, 13.5bpp, (1 Cr & Cb sample per 2x2 Y samples), little-endian
        YUV420P10BE,///< planar YUV 4:2:0, 15bpp, (1 Cr & Cb sample per 2x2 Y samples), big-endian
        YUV420P10LE,///< planar YUV 4:2:0, 15bpp, (1 Cr & Cb sample per 2x2 Y samples), little-endian
        YUV422P10BE,///< planar YUV 4:2:2, 20bpp, (1 Cr & Cb sample per 2x1 Y samples), big-endian
        YUV422P10LE,///< planar YUV 4:2:2, 20bpp, (1 Cr & Cb sample per 2x1 Y samples), little-endian
        YUV444P9BE, ///< planar YUV 4:4:4, 27bpp, (1 Cr & Cb sample per 1x1 Y samples), big-endian
        YUV444P9LE, ///< planar YUV 4:4:4, 27bpp, (1 Cr & Cb sample per 1x1 Y samples), little-endian
        YUV444P10BE,///< planar YUV 4:4:4, 30bpp, (1 Cr & Cb sample per 1x1 Y samples), big-endian
        YUV444P10LE,///< planar YUV 4:4:4, 30bpp, (1 Cr & Cb sample per 1x1 Y samples), little-endian
        YUV422P9BE, ///< planar YUV 4:2:2, 18bpp, (1 Cr & Cb sample per 2x1 Y samples), big-endian
        YUV422P9LE, ///< planar YUV 4:2:2, 18bpp, (1 Cr & Cb sample per 2x1 Y samples), little-endian
        VDA_VLD,    ///< hardware decoding through VDA
        GBRP,      ///< planar GBR 4:4:4 24bpp
        GBRP9BE,   ///< planar GBR 4:4:4 27bpp, big endian
        GBRP9LE,   ///< planar GBR 4:4:4 27bpp, little endian
        GBRP10BE,  ///< planar GBR 4:4:4 30bpp, big endian
        GBRP10LE,  ///< planar GBR 4:4:4 30bpp, little endian
        GBRP16BE,  ///< planar GBR 4:4:4 48bpp, big endian
        GBRP16LE,  ///< planar GBR 4:4:4 48bpp, little endian
        COUNT      ///< number of pixel formats in this list
        ;
        public static PixelFormat valueOf(int i) {
            for (PixelFormat fmt : PixelFormat.values()) {
                if(fmt.ordinal() == i) {
                    return fmt;
                }
            }
            return null;            
        }
    }
}
