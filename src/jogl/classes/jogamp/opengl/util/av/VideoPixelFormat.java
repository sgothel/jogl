/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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
package jogamp.opengl.util.av;

/** FFMPEG/libAV compatible video pixel format */
public enum VideoPixelFormat {
    // NONE= -1,
    /** planar YUV 4:2:0, 12bpp, (1 Cr & Cb sample per 2x2 Y samples) */
    YUV420P,
    /** packed YUV 4:2:2, 16bpp, Y0 Cb Y1 Cr ( sharing Cb and Cr w/ 2 pixels )*/
    YUYV422,
    /** packed RGB 8:8:8, 24bpp, RGBRGB... */
    RGB24,
    /** packed RGB 8:8:8, 24bpp, BGRBGR... */
    BGR24,
    /** planar YUV 4:2:2, 16bpp, (1 Cr & Cb sample per 2x1 Y samples) */
    YUV422P,
    /** planar YUV 4:4:4, 24bpp, (1 Cr & Cb sample per 1x1 Y samples) */
    YUV444P,
    /** planar YUV 4:1:0,  9bpp, (1 Cr & Cb sample per 4x4 Y samples) */
    YUV410P,
    /** planar YUV 4:1:1, 12bpp, (1 Cr & Cb sample per 4x1 Y samples) */
    YUV411P,
    /** Y, 8bpp */
    GRAY8,
    /** Y,  1bpp, 0 is white, 1 is black, in each byte pixels are ordered from the msb to the lsb */
    MONOWHITE,
    /** Y,  1bpp, 0 is black, 1 is white, in each byte pixels are ordered from the msb to the lsb */
    MONOBLACK,
    /** 8 bit with RGB32 palette */
    PAL8,
    /** planar YUV 4:2:0, 12bpp, full scale (JPEG), deprecated in favor of YUV420P and setting color_range */
    YUVJ420P,
    /** planar YUV 4:2:2, 16bpp, full scale (JPEG), deprecated in favor of YUV422P and setting color_range */
    YUVJ422P,
    /** planar YUV 4:4:4, 24bpp, full scale (JPEG), deprecated in favor of YUV444P and setting color_range */
    YUVJ444P,
    /** XVideo Motion Acceleration via common packet passing */
    XVMC_MPEG2_MC,
    /** */
    XVMC_MPEG2_IDCT,
    /** packed YUV 4:2:2, 16bpp, Cb Y0 Cr Y1 ( sharing Cb and Cr w/ 2 pixels ) */
    UYVY422,
    /** packed YUV 4:1:1, 12bpp, Cb Y0 Y1 Cr Y2 Y3 */
    UYYVYY411,
    /** packed RGB 3:3:2,  8bpp, (msb)2B 3G 3R(lsb) */
    BGR8,
    /** packed RGB 1:2:1 bitstream,  4bpp, (msb)1B 2G 1R(lsb), a byte contains two pixels, the first pixel in the byte is the one composed by the 4 msb bits */
    BGR4,
    /** packed RGB 1:2:1,  8bpp, (msb)1B 2G 1R(lsb) */
    BGR4_BYTE,
    /** packed RGB 3:3:2,  8bpp, (msb)2R 3G 3B(lsb) */
    RGB8,
    /** packed RGB 1:2:1 bitstream,  4bpp, (msb)1R 2G 1B(lsb), a byte contains two pixels, the first pixel in the byte is the one composed by the 4 msb bits */
    RGB4,
    /** packed RGB 1:2:1,  8bpp, (msb)1R 2G 1B(lsb) */
    RGB4_BYTE,
    /** planar YUV 4:2:0, 12bpp, 1 plane for Y and 1 plane for the UV components, which are interleaved (first byte U and the following byte V) */
    NV12,
    /** as above, but U and V bytes are swapped */
    NV21,

    /** packed ARGB 8:8:8:8, 32bpp, ARGBARGB... */
    ARGB,
    /** packed RGBA 8:8:8:8, 32bpp, RGBARGBA... */
    RGBA,
    /** packed ABGR 8:8:8:8, 32bpp, ABGRABGR... */
    ABGR,
    /** packed BGRA 8:8:8:8, 32bpp, BGRABGRA... */
    BGRA,

    /** Y, 16bpp, big-endian */
    GRAY16BE,
    /** Y        , 16bpp, little-endian */
    GRAY16LE,
    /** planar YUV 4:4:0 (1 Cr & Cb sample per 1x2 Y samples) */
    YUV440P,
    /** planar YUV 4:4:0 full scale (JPEG), deprecated in favor of YUV440P and setting color_range */
    YUVJ440P,
    /** planar YUV 4:2:0, 20bpp, (1 Cr & Cb sample per 2x2 Y & A samples) */
    YUVA420P,
    /** H.264 HW decoding with VDPAU, data[0] contains a vdpau_render_state struct which contains the bitstream of the slices as well as various fields extracted from headers */
    VDPAU_H264,
    /** MPEG-1 HW decoding with VDPAU, data[0] contains a vdpau_render_state struct which contains the bitstream of the slices as well as various fields extracted from headers */
    VDPAU_MPEG1,
    /** MPEG-2 HW decoding with VDPAU, data[0] contains a vdpau_render_state struct which contains the bitstream of the slices as well as various fields extracted from headers */
    VDPAU_MPEG2,
    /** WMV3 HW decoding with VDPAU, data[0] contains a vdpau_render_state struct which contains the bitstream of the slices as well as various fields extracted from headers */
    VDPAU_WMV3,
    /** VC-1 HW decoding with VDPAU, data[0] contains a vdpau_render_state struct which contains the bitstream of the slices as well as various fields extracted from headers */
    VDPAU_VC1,
    /** packed RGB 16:16:16, 48bpp, 16R, 16G, 16B, the 2-byte value for each R/G/B component is stored as big-endian */
    RGB48BE,
    /** packed RGB 16:16:16, 48bpp, 16R, 16G, 16B, the 2-byte value for each R/G/B component is stored as little-endian */
    RGB48LE,

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
    /**
     * Returns the matching PixelFormat value corresponding to the given PixelFormat's integer ordinal.
     * <pre>
     *   given:
     *     ordinal = enumValue.ordinal()
     *   reverse:
     *     enumValue = EnumClass.values()[ordinal]
     * </pre>
     * @throws IllegalArgumentException if the given ordinal is out of range, i.e. not within [ 0 .. PixelFormat.values().length-1 ]
     */
    public static VideoPixelFormat valueOf(final int ordinal) throws IllegalArgumentException {
        final VideoPixelFormat[] all = VideoPixelFormat.values();
        if( 0 <= ordinal && ordinal < all.length ) {
            return all[ordinal];
        }
        throw new IllegalArgumentException("Ordinal "+ordinal+" out of range of PixelFormat.values()[0.."+(all.length-1)+"]");
    }
}