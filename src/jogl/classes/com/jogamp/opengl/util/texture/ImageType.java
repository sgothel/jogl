/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.util.texture;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Image type classification.
 * <p>
 * Allows to classify the {@link ImageType} of an {@link InputStream} via {@link #ImageType(InputStream)}
 * or to simply define one {@link ImageType} via {@link #ImageType(String)}.
 * </p>
 * @since 2.3.2
 */
public class ImageType {
    /**
     * Minimum number of bytes to determine the image data type, i.e. {@value} bytes.
     */
    public static final int MAGIC_MAX_SIZE = 25;

    /**
     * Constant which can be used as a file suffix to indicate a JPEG stream, value {@value}.
     * <ul>
     * <li>{@code http://www.faqs.org/faqs/jpeg-faq/part1/}</li>
     * <li>{@code http://www.iso.org/iso/iso_catalogue/catalogue_tc/catalogue_detail.htm?csnumber=54989}</li>
     * </ul>
     */
    public static final String T_JPG     = "jpg";

    /**
     * Constant which can be used as a file suffix to indicate a PNG stream, value {@value}.
     * <ul>
     * <li>{@code http://www.libpng.org/pub/png/spec/1.1/PNG-Rationale.html#R.PNG-file-signature}</li>
     * </ul>
     */
    public static final String T_PNG     = "png";

    /**
     * Constant which can be used as a file suffix to indicate an Apple Icon Image stream, value {@value}.
     * <p>
     * {@code 'i' 'c' 'n' 's' ascii code}
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_ICNS     = "icns";

    /**
     * Constant which can be used as a file suffix to indicate a Microsoft Windows Icon stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code https://msdn.microsoft.com/en-us/library/ms997538.aspx}</li>
     * </ul>
     */
    public static final String T_ICO     = "ico";
    
    /**
     * Constant which can be used as a file suffix to indicate a Microsoft Windows Cursor stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_CUR     = "cur";
    
    /**
     * Constant which can be used as a file suffix to indicate a GIF stream, value {@value}.
     * <p>
     * {@code GIF87A or GIF89A ascii code}
     * </p>
     * <ul>
     * <li>{@code http://www.w3.org/Graphics/GIF/spec-gif87a.txt http://www.w3.org/Graphics/GIF/spec-gif89a.txt}</li>
     * </ul>
     */
    public static final String T_GIF     = "gif";

    /**
     * Constant which can be used as a file suffix to indicate a GIF stream, value {@value}.
     * <p>
     * {@code BM ascii code}
     * </p>
     * <p>
     * FIXME: Collision or supertype of {@link #T_DIB}?
     * </p>
     * <ul>
     * <li>{@code http://www.fileformat.info/format/bmp/spec/e27073c25463436f8a64fa789c886d9c/view.htm}</li>
     * </ul>
     */
    public static final String T_BMP     = "bmp";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * FIXME: Collision or subtype of {@link #T_BMP}?
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_DIB     = "dib";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_DCX     = "dcx";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_PCX     = "pcx";

    /**
     * Constant which can be used as a file suffix to indicate a PAM stream, NetPbm magic 6 - binary RGB.
     * <ul>
     * <li>{@code http://netpbm.sourceforge.net/doc/ppm.html}</li>
     * </ul>
     */
    public static final String T_PPM     = "ppm";

    /**
     * Constant which can be used as a file suffix to indicate a Adobe PhotoShop stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_PSD     = "psd";

    /**
     * Constant which can be used as a file suffix to indicate a TIFF stream, value {@value}.
     * <p>
     * Intentionally detects only the little endian tiff images ("II" in the spec).
     * </p>
     * <p>
     * FIXME: Collision or supertype of {@link #T_LDF}?
     * </p>
     * <ul>
     * <li>{@code http://partners.adobe.com/public/developer/en/tiff/TIFF6.pdf}</li>
     * </ul>
     */
    public static final String T_TIFF    = "tiff";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * FIXME: Collision or subtype of {@link #T_TIFF}?
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_LDF     = "ldf";

    /**
     * Constant which can be used as a file suffix to indicate an SGI RGB stream, value {@value}.
     * <p>
     * "474 saved as a short" 474 = 0x01DA
     * </p>
     * <ul>
     * <li>{@code http://paulbourke.net/dataformats/sgirgb/sgiversion.html}</li>
     * </ul>
     */
    public static final String T_SGI_RGB = "rgb";

    /**
     * Constant which can be used as a file suffix to indicate a DirectDraw Surface stream, value {@value}.
     * <p>
     * 'D' 'D' 'S' ' ' ascii code
     * </p>
     * <ul>
     * <li>{@code https://msdn.microsoft.com/en-us/library/windows/desktop/bb943991%28v=vs.85%29.aspx#File_Layout1}</li>
     * </ul>
     */
    public static final String T_DDS     = "dds";

    /**
     * Constant which can be used as a file suffix to indicate a Portable Arbitrary Map stream, NetPbm magic 7 - binary RGB and RGBA.
     * <ul>
     * <li>{@code http://netpbm.sourceforge.net/doc/pam.html}</li>
     * </ul>
     */
    public static final String T_PAM     = "pam";

    /**
     * Constant which can be used as a file suffix to indicate a PGM stream, NetPbm magic 5 - binary grayscale.
     * <ul>
     * <li>{@code http://netpbm.sourceforge.net/doc/pgm.html}</li>
     * </ul>
     */
    public static final String T_PGM     = "pgm";

    /**
     * Constant which can be used as a file suffix to indicate a PGM stream, NetPbm magic 4 - binary monochrome.
     * <ul>
     * <li>{@code http://netpbm.sourceforge.net/doc/pbm.html}</li>
     * </ul>
     */
    public static final String T_PBM     = "pbm";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_3D2     = "3d2";

    /**
     * Constant which can be used as a file suffix to indicate an Apple QuickDraw 3D 3DMF stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_3DMF     = "3dmf";

    /**
     * Constant which can be used as a file suffix to indicate a Texas Instruments TI-92 Bitmap stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_92I     = "92i";

    /**
     * Constant which can be used as a file suffix to indicate an Amiga metafile stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_AMFF     = "amff";

    /**
     * Constant which can be used as a file suffix to indicate an America Online Art stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_ART     = "art";

    /**
     * Constant which can be used as a file suffix to indicate a United States Department of Defence Continuous Acquisition and Life-cycle Support Raster stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code http://www.fileformat.info/format/cals/egff.htm}</li>
     * </ul>
     */
    public static final String T_CALS     = "cals";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_CAM     = "cam";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_CBD     = "cbd";


    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_CE2     = "ce2";

    /**
     * Constant which can be used as a file suffix to indicate a Kodak Cineon System stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code http://www.cineon.com/ff_draft.php}</li>
     * </ul>
     */
    public static final String T_CIN     = "cin";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_COB     = "cob";

    /**
     * Constant which can be used as a file suffix to indicate a Corel Photo Paint stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_CPT     = "cpt";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_CVG     = "cvg";


    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_DEM     = "dem";

    /**
     * Constant which can be used as a file suffix to indicate a Digital Picture Exchange stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_DPX     = "dpx";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_DRW     = "drw";

    /**
     * Constant which can be used as a file suffix to indicate a Autocad drawing stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_DWG     = "dwg";

    /**
     * Constant which can be used as a file suffix to indicate a Hexagon Geospatial Enhanced Compression Wavelet stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_ECW     = "ecw";

    /**
     * Constant which can be used as a file suffix to indicate a Microsoft Windows Enhanced metafile stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_EMF     = "emf";

    /**
     * Constant which can be used as a file suffix to indicate a FlashPix stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_FPX     = "fpx";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_FTS     = "fts";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_GRO     = "gro";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_HDR     = "hdr";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_HRU     = "hru";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_IMG     = "img";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_INFINI_D     = "infini-d";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_IWC     = "iwc";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_J6I     = "j6i";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_JIF     = "jif";

    /**
     * Constant which can be used as a file suffix to indicate a JPEG-2000 stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_JP2     = "jp2";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_KDC     = "kdc";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_L64     = "l64";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * FIXME: Collision or supertype of {@link #T_RAD}?
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_LBM     = "lbm";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * FIXME: Collision or subtype of {@link #T_LBM}?
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_RAD     = "rad";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_LWF     = "lwf";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_MBM     = "mbm";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_MGL     = "mgl";

    /**
     * Constant which can be used as a file suffix to indicate an Imagemagick stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_MIF     = "mif";

    /**
     * Constant which can be used as a file suffix to indicate a Multiple-image Network Graphics stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_MNG     = "mng";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_MPW     = "mpw";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_MSP     = "msp";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_N64     = "n64";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_NCR     = "ncr";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_NFF     = "nff";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_NGG     = "ngg";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_NLM     = "nlm";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_NOL     = "nol";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_PAL     = "pal";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_PAX     = "pax";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_PCD     = "pcd";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_PCL     = "pcl";

    /**
     * Constant which can be used as a file suffix to indicate a Softimage pic stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code http://paulbourke.net/dataformats/softimagepic/}</li>
     * </ul>
     */
    public static final String T_PIC     = "pic";
    
    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_PIX     = "pix";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_POL     = "pol";

    /**
     * Constant which can be used as a file suffix to indicate a PaintShop Pro stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_PSP     = "psp";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_QFX     = "qfx";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_QTM     = "qtm";

    /**
     * Constant which can be used as a file suffix to indicate a Sun Raster stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_RAS     = "ras";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_RIX     = "rix";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_SID     = "sid";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_SLD     = "sld";


    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_SOD     = "sod";


    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_WIC     = "wic";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_WLM     = "wlm";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_WMF     = "wmf";

    /**
     * Constant which can be used as a file suffix to indicate a Wordperfect Graphics vectors stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_WPG     = "wpg";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_WRL     = "wrl";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_XBM     = "xbm";

    /**
     * Constant which can be used as a file suffix to indicate a X PixMap stream, value {@value}.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_XPM    = "xpm";

    /**
     * Constant which can be used as a file suffix to indicate a Targa stream, value {@value}.
     * <ul>
     * <li>{@code }</li>
     * </ul>
     */
    public static final String T_TGA     = "tga";

    /**
     * The determined unique type, e.g. {@link #T_PNG}, {@link #T_JPG}, etc.
     * <p>
     * Maybe {@code null} if undetermined, i.e. {@link #isDefined()} returns {@code false}.
     * </p>
     */
    public final String type;

    /**
     * The optionally read header of size {@link #MAGIC_MAX_SIZE} bytes as used to determine the {@link #type},
     * i.e. {@link #ImageType(InputStream)}.
     * <p>
     * May be {@code null}, if {@link #type} has been determined otherwise, i.e {@link #ImageType(String)}.
     * </p>
     * <p>
     * The header is <i>not</i> being used for {@link #hashCode()} and {@link #equals(Object)}!
     * </p>
     */
    public final byte[] header;

    private final int hash;

    /**
     * Creates instance based on given stream.
     * @param stream stream to parse, {@link InputStream#available()} must be &ge; {@link #MAGIC_MAX_SIZE}
     * @throws java.io.IOException if an I/O exception occurred
     */
    public ImageType(final InputStream stream) throws IOException {
        final byte[] _header = new byte[MAGIC_MAX_SIZE];
        type = Util.getFileSuffix(stream, _header);
        this.header = _header;
        this.hash = null != this.type ? this.type.hashCode() : 0;
    }
    /**
     * Creates instance based on the given type.
     * @param type must be one of {@link #T_PNG}, {@link #T_JPG}, etc.
     */
    public ImageType(final String type) {
        this.header = null;
        this.type = type;
        this.hash = this.type.hashCode();
    }
    /** Returns {@code true} if {@link #type} is determined, i.e. not {@code null}, otherwise {@code false}. */
    public final boolean isDefined() { return null != type; }

    @Override
    public final int hashCode() {
        return hash;
    }
    @Override
    public boolean equals(final Object o) {
        if( o == this ) {
            return true;
        } else if( o instanceof ImageType ) {
            final ImageType t = (ImageType)o;
            return this.type.equals(t.type);
        } else {
            return false;
        }
    }
    @Override
    public String toString() { return "ImageType["+type+"]"; }

    /**
     * Static utility functions for {@link ImageType}
     * to determine the {@link ImageType#type}.
     * @since 2.3.2
     */
    public static class Util {
    	/**
    	 * Determines the file suffix (i.e the image format) of the given InputStream. The given
    	 * InputStream must return true from markSupported() and support a minimum of {@link #MAGIC_MAX_SIZE} bytes
    	 * of read-ahead.
    	 *
    	 * @param stream stream to parse, {@link InputStream#available()} must be &ge; {@link #MAGIC_MAX_SIZE}
    	 * @return the file suffix if any, otherwise <code>null</code>
    	 * @throws java.io.IOException if an I/O exception occurred
    	 */
    	public static String getFileSuffix(final InputStream stream) throws IOException {
    	    return getFileSuffix(stream, new byte[MAGIC_MAX_SIZE]);
    	}
        /**
         * Determines the file suffix (i.e the image format) of the given InputStream. The given
         * InputStream must return true from markSupported() and support a minimum of {@link #MAGIC_MAX_SIZE} bytes
         * of read-ahead.
         *
         * @param stream stream to parse, {@link InputStream#available()} must be &ge; {@link #MAGIC_MAX_SIZE}
         * @param b byte array sink, size must be &ge; {@link #MAGIC_MAX_SIZE}
         * @return the file suffix if any, otherwise <code>null</code>
         * @throws java.io.IOException if an I/O exception occurred
         */
        public static String getFileSuffix(InputStream stream, final byte[] b) throws IOException {
    		if (stream == null) {
                throw new IOException("Stream was null");
            }
    		if (!(stream instanceof BufferedInputStream)) {
    			stream = new BufferedInputStream(stream);
            }
    		if (!stream.markSupported()) {
                throw new IOException("Mark not supported");
            }
    		if (stream.available() < MAGIC_MAX_SIZE) {
                throw new IOException("Requires "+MAGIC_MAX_SIZE+" bytes, has "+stream.available()+" bytes");
            }
    		try {
    			stream.mark(MAGIC_MAX_SIZE);
    			final int bytesRead = stream.read(b);
    			if( MAGIC_MAX_SIZE > bytesRead ) {
    			    throw new IOException("Could not read "+MAGIC_MAX_SIZE+" bytes, read "+bytesRead+" bytes");
    			}
    			return getFileSuffix(b);
    		} finally {
    			stream.reset();
    		}

    	}

    	/**
    	 * Determines the file suffix (i.e the image format) of the given bytes from the header
    	 * of a file.
    	 *
    	 * @param b byte array to parse, size must be &ge; {@link #MAGIC_MAX_SIZE}
    	 * @return the file suffix if any, otherwise <code>null</code>
    	 * @throws java.io.IOException if an I/O exception occurred
    	 */
        public static String getFileSuffix(final byte[] b) {
            if( b.length < MAGIC_MAX_SIZE ) {
                throw new IllegalArgumentException("byte array must be >= "+MAGIC_MAX_SIZE+", has "+b.length);
            }
            final byte b0 = b[0];
            final byte b1 = b[1];
            final byte b2 = b[2];
            final byte b3 = b[3];
            final byte b4 = b[4];
            final byte b5 = b[5];

            // T_TGA: NO Signature!

            if (b0 == (byte)0x00) {
                if (b1 == (byte)0x00 && b2 == (byte)0x00 && b3 == (byte)0x0C &&
                    b4 == (byte)0x6A && b5 == (byte)0x50 &&
                    b[6] == (byte)0x20 && b[7] == (byte)0x20 && b[8] == (byte)0x0D && b[9] == (byte)0x0A && b[10] == (byte)0x87 &&
                    b[11] == (byte)0x0A) {
                    return T_JP2;
                }
                else if (b1 == (byte)0x01) {
                	return T_ICO;
                }
                else if (b1 == (byte)0x02) {
                	return T_CUR;
                }
            }
            else if (b0 == (byte)0x01) {
                if (b1 == (byte)0xDA /* && b2 == (byte)0x01 && b3 == (byte)0x01 && b4 == (byte)0x00 && b5 == (byte)0x03 */) {
                    return T_SGI_RGB;
                }
                else if (b1 == (byte)0xFF && b2 == (byte)0x02 && b3 == (byte)0x04 &&
                         b4 == (byte)0x03 && b5 == (byte)0x02) {
                    return T_DRW;
                }
                else if (b1 == (byte)0x00 && b2 == (byte)0x00 && b3 == (byte)0x00 &&
                         b4 == (byte)0x58 && b5 == (byte)0x00 &&
                         b[6] == (byte)0x00 && b[7] == (byte)0x00) {
                    return T_EMF;
                }
            }
            else if (b0 == (byte)0x07 && b1 == (byte)0x20 && b2 == (byte)0x4D && b3 == (byte)0x4D) {
                return T_CAM;
            }
            else if (b0 == (byte)0x0A && b1 == (byte)0x05 && b2 == (byte)0x01 && b3 == (byte)0x08) {
                return T_PCX;
            }
            else if (b0 == (byte)0x1B && b1 == (byte)0x45 && b2 == (byte)0x1B && b3 == (byte)0x26 &&
                     b4 == (byte)0x6C && b5 == (byte)0x30 &&
                     b[6] == (byte)0x4F && b[7] == (byte)0x1B && b[8] == (byte)0x26 && b[9] == (byte)0x6C && b[10] == (byte)0x30 &&
                     b[11] == (byte)0x45 && b[12] == (byte)0x1B && b[13] == (byte)0x26) {
                return T_PCL;
            }
            else if (b0 == (byte)0x20 && b1 == (byte)0x77 && b2 == (byte)0x00 && b3 == (byte)0x02) {
                return T_CBD;
            }
            else if (b0 == (byte)0x23) {
                if (b1 == (byte)0x20 && b2 == (byte)0x24 && b3 == (byte)0x49 &&
                    b4 == (byte)0x64 && b5 == (byte)0x3A &&
                    b[6] == (byte)0x20) {
                    return T_SID;
                }
                else if (b1 == (byte)0x56 && b2 == (byte)0x52 && b3 == (byte)0x4D &&
                    b4 == (byte)0x4C && b5 == (byte)0x20 &&
                    b[6] == (byte)0x56 && b[7] == (byte)0x32 && b[8] == (byte)0x2E && b[9] == (byte)0x30) {
                    return T_WRL;
                }
                else if (b1 == (byte)0x64 && b2 == (byte)0x65 && b3 == (byte)0x66 &&
                    b4 == (byte)0x69 && b5 == (byte)0x6E &&
                    b[6] == (byte)0x65) {
                    return T_XBM;
                }
            }
            else if (b0 == (byte)0x2A && b1 == (byte)0x2A && b2 == (byte)0x54 && b3 == (byte)0x49 &&
                    b4 == (byte)0x39 && b5 == (byte)0x32 &&
                    b[6] == (byte)0x2A && b[7] == (byte)0x2A && b[8] == (byte)0x01 && b[9] == (byte)0x00 && b[10] == (byte)0x58 &&
                    b[11] == (byte)0x6E && b[12] == (byte)0x56 && b[13] == (byte)0x69) {
                return T_92I;
            }
            else if (b0 == (byte)0x2F && b1 == (byte)0x2A && b2 == (byte)0x20 && b3 == (byte)0x58 &&
                    b4 == (byte)0x50 && b5 == (byte)0x4D &&
                    b[6] == (byte)0x20 && b[7] == (byte)0x2A && b[8] == (byte)0x2F) {
                return T_XPM;
            }
            else if (b0 == (byte)0x33 && b1 == (byte)0x44 && b2 == (byte)0x4D && b3 == (byte)0x46) {
                return T_3DMF;
            }
            else if (b0 == (byte)0x35 && b1 == (byte)0x4B && b2 == (byte)0x50 && b3 == (byte)0x35 &&
                    b4 == (byte)0x31 && b5 == (byte)0x5D &&
                    b[6] == (byte)0x2A && b[7] == (byte)0x67 && b[8] == (byte)0x72 && b[9] == (byte)0x72 && b[10] == (byte)0x80 &&
                    b[11] == (byte)0x83 && b[12] == (byte)0x85 && b[13] == (byte)0x63) {
                return T_HRU;
            }
            else if (b0 == (byte)0x36 && b1 == (byte)0x34 && b2 == (byte)0x4C && b3 == (byte)0x41 &&
                     b4 == (byte)0x4E && b5 == (byte)0x20 &&
                     b[6] == (byte)0x49 && b[7] == (byte)0x44 && b[8] == (byte)0x42 && b[9] == (byte)0x4C && b[10] == (byte)0x4F &&
                     b[11] == (byte)0x43 && b[12] == (byte)0x4B) {
                return T_L64;
            }
            else if (b0 == (byte)0x37 && b1 == (byte)0x00 && b2 == (byte)0x00 && b3 == (byte)0x10 &&
                     b4 == (byte)0x42 && b5 == (byte)0x00 &&
                     b[6] == (byte)0x00 && b[7] == (byte)0x10 && b[8] == (byte)0x00 && b[9] == (byte)0x00 && b[10] == (byte)0x00 &&
                     b[11] == (byte)0x00 && b[12] == (byte)0x39 && b[13] == (byte)0x64) {
                return T_MBM;
            }
            else if (b0 == (byte)0x38 && b1 == (byte)0x42 && b2 == (byte)0x50 && b3 == (byte)0x53 &&
                     b4 == (byte)0x00 && b5 == (byte)0x01 &&
                     b[6] == (byte)0x00 && b[7] == (byte)0x00 && b[8] == (byte)0x00 && b[9] == (byte)0x00) {
                return T_PSD;
            }
            else if (b0 == (byte)0x3A && b1 == (byte)0xDE && b2 == (byte)0x68 && b3 == (byte)0xB1) {
                return T_DCX;
            }
            else if (b0 == (byte)0x3D && b1 == (byte)0x02) {
                return T_3D2;
            }
            else if (b0 == (byte)0x41) {
                if (b1 == (byte)0x43 && b2 == (byte)0x31 && b3 == (byte)0x30) {
                    return T_DWG;
                }
                else if (b1 == (byte)0x48) {
                    return T_PAL;
                }
                else if (b1 == (byte)0x4D && b2 == (byte)0x46 && b3 == (byte)0x46) {
                    return T_AMFF;
                }
                else if (b1 == (byte)0x75 && b2 == (byte)0x74 && b3 == (byte)0x6F &&
                         b4 == (byte)0x43 && b5 == (byte)0x41 &&
                         b[6] == (byte)0x44 && b[7] == (byte)0x20 && b[8] == (byte)0x53 && b[9] == (byte)0x6C && b[10] == (byte)0x69 &&
                         b[11] == (byte)0x64 && b[12] == (byte)0x65) {
                    return T_SLD;
                }
            }
            else if (b0 == (byte)0x42 && b1 == (byte)0x4D) {
                if (b2 == (byte)0x36) {
                    // FIXME: Collision or subtype of T_BMP?
                    return T_DIB;
                } else {
                    return T_BMP;
                }
            }
            else if (b0 == (byte)0x43) {
                if (b1 == (byte)0x36 && b2 == (byte)0x34) {
                    return T_N64;
                }
                else if (b1 == (byte)0x41 && b2 == (byte)0x4C && b3 == (byte)0x41 &&
                         b4 == (byte)0x4D && b5 == (byte)0x55 &&
                         b[6] == (byte)0x53 && b[7] == (byte)0x43 && b[8] == (byte)0x56 && b[9] == (byte)0x47) {
                    return T_CVG;
                }
                else if (b1 == (byte)0x50 && b2 == (byte)0x54 && b3 == (byte)0x46 &&
                         b4 == (byte)0x49 && b5 == (byte)0x4C &&
                         b[6] == (byte)0x45) {
                    return T_CPT;
                }
                else if (b1 == (byte)0x61 && b2 == (byte)0x6C && b3 == (byte)0x69 &&
                         b4 == (byte)0x67 && b5 == (byte)0x61 &&
                         b[6] == (byte)0x72 && b[7] == (byte)0x69) {
                    return T_COB;
                }
            }
            else if (b0 == (byte)0x44) {
                if (b1 == (byte)0x44 && b2 == (byte)0x53 && b3 == (byte)0x20) {
                    return T_DDS;
                }
                else if (b1 == (byte)0x61 && b2 == (byte)0x6E && b3 == (byte)0x4D) {
                    return T_MSP;
                }
            }
            else if (b0 == (byte)0x45) {
                if (b1 == (byte)0x59 && b2 == (byte)0x45 && b3 == (byte)0x53) {
                    return T_CE2;
                }
                else if (b1 == (byte)0x78 && b2 == (byte)0x69 && b3 == (byte)0x66) { /* EXIF */
                    /**
                     *  (b0 == (byte)0x45 && b1 == (byte)0x78 && b2 == (byte)0x69 && b3 == (byte)0x66) || // EXIF
                     *  (b0 == (byte)0x4A && b1 == (byte)0x46 && b2 == (byte)0x49 && b3 == (byte)0x46) || // JFIF
                     *  (b0 == (byte)0xff && b1 == (byte)0xd8 ) // && b2 == (byte)0xff
                     */
                    return T_JPG;
                }
            }
            else if (b0 == (byte)0x46 && b1 == (byte)0x4F && b2 == (byte)0x52 && b3 == (byte)0x4D) {
                if (b4 == (byte)0x41 && b5 == (byte)0x54 && b[6] == (byte)0x3D) {
                    // FIXME: Collision or subtype of T_LBM?
                    return T_RAD;
                } else {
                    return T_LBM;
                }
            }
            else if (b0 == (byte)0x47 && b1 == (byte)0x49 && b2 == (byte)0x46 && b3 == (byte)0x38 &&
                     (b4 == (byte)0x37 || b4 == (byte)0x39) && b5 == (byte)0x61) {
                return T_GIF;
            }
            else if (b0 == (byte)0x48 && b1 == (byte)0x50 && b2 == (byte)0x48 && b3 == (byte)0x50 &&
                     b4 == (byte)0x34 && b5 == (byte)0x38 &&
                     b[6] == (byte)0x2D && b[7] == (byte)0x45 && b[8] == (byte)0x1E && b[9] == (byte)0x2B) {
                return T_GRO;
            }
            else if (b0 == (byte)0x49) {
                if (b1 == (byte)0x49 && b2 == (byte)0x2A && b3 == (byte)0x00) {
                    if (b4 == (byte)0x08 && b5 == (byte)0x00 &&
                        b[6] == (byte)0x00 && b[7] == (byte)0x00 && b[8] == (byte)0x0E && b[9] == (byte)0x00 && b[10] == (byte)0x00 &&
                        b[11] == (byte)0x01 && b[12] == (byte)0x04 && b[13] == (byte)0x00) {
                        // FIXME: Collision or subtype of T_TIFF?
                        return T_LDF;
                    } else {
                        return T_TIFF;
                    }
                }
                else if (b1 == (byte)0x57 && b2 == (byte)0x43 && b3 == (byte)0x01) {
                    return T_IWC;
                }
            }
            else if (b0 == (byte)0x4A) {
                if (b1 == (byte)0x46 && b2 == (byte)0x49 && b3 == (byte)0x46) { /* JFIF */
                    /**
                     *  (b0 == (byte)0x45 && b1 == (byte)0x78 && b2 == (byte)0x69 && b3 == (byte)0x66) || // EXIF
                     *  (b0 == (byte)0x4A && b1 == (byte)0x46 && b2 == (byte)0x49 && b3 == (byte)0x46) || // JFIF
                     *  (b0 == (byte)0xff && b1 == (byte)0xd8 ) // && b2 == (byte)0xff
                     */
                    return T_JPG;
                }
                else if (b1 == (byte)0x47 && (b2 == (byte)0x03 || b2 == (byte)0x04) && b3 == (byte)0x0E &&
                         b4 == (byte)0x00 && b5 == (byte)0x00 &&
                         b[6] == (byte)0x00) {
                    return T_ART;
                }
                else if (b1 == (byte)0x49 && b2 == (byte)0x46 && b3 == (byte)0x39 &&
                         b4 == (byte)0x39 && b5 == (byte)0x61) {
                    return T_JIF;
                }
            }
            else if (b0 == (byte)0x4D) {
                if (b1 == (byte)0x47 && b2 == (byte)0x4C) {
                    return T_MGL;
                }
                else if (b1 == (byte)0x4D && b2 == (byte)0x00 && b3 == (byte)0x2A) {
                    return T_KDC;
                }
                else if (b1 == (byte)0x50 && b2 == (byte)0x46) {
                    return T_MPW;
                }
            }
            else if (b0 == (byte)0x4E) {
                if (b1 == (byte)0x47 && b2 == (byte)0x47 && b3 == (byte)0x00 &&
                    b4 == (byte)0x01 && b5 == (byte)0x00) {
                    return T_NGG;
                }
                else if (b1 == (byte)0x4C && b2 == (byte)0x4D && b3 == (byte)0x20 &&
                         b4 == (byte)0x01 && b5 == (byte)0x02 &&
                         b[6] == (byte)0x00) {
                    return T_NLM;
                }
                else if (b1 == (byte)0x4F && b2 == (byte)0x4C && b3 == (byte)0x00 &&
                         b4 == (byte)0x01 && b5 == (byte)0x00 &&
                         b[6] == (byte)0x06 && b[7] == (byte)0x01 && b[8] == (byte)0x03 && b[9] == (byte)0x00) {
                    return T_NOL;
                }
            }
            else if (b0 == (byte)0x50) {
                if (b1 == (byte)0x31 /* plain */|| b1 == (byte)0x34) {
                    return T_PBM;
                }
                else if (b1 == (byte)0x32 /* plain */|| b1 == (byte)0x35) {
                    return T_PGM;
                }
                else if (b1 == (byte)0x33 /* plain */|| b1 == (byte)0x36) {
                    return T_PPM;
                }
                else if (b1 == (byte)0x37) {
                    return T_PAM;
                }
                else if (b1 == (byte)0x41 && b2 == (byte)0x58) {
                    return T_PAX;
                }
                else if (b1 == (byte)0x49 && b2 == (byte)0x58 && b3 == (byte)0x20) {
                    return T_PIX;
                }
                else if (b1 == (byte)0x4F && b2 == (byte)0x4C && b3 == (byte)0x20 &&
                         b4 == (byte)0x46 && b5 == (byte)0x6F &&
                         b[6] == (byte)0x72 && b[7] == (byte)0x6D && b[8] == (byte)0x61 && b[9] == (byte)0x74) {
                    return T_POL;
                }
                else if (b1 == (byte)0x61 && b2 == (byte)0x69 && b3 == (byte)0x6E &&
                         b4 == (byte)0x74 && b5 == (byte)0x20 &&
                         b[6] == (byte)0x53 && b[7] == (byte)0x68 && b[8] == (byte)0x6F && b[9] == (byte)0x70 && b[10] == (byte)0x20 &&
                         b[11] == (byte)0x50 && b[12] == (byte)0x72 && b[13] == (byte)0x6F && b[14] == (byte)0x20 && b[15] == (byte)0x49 &&
                         b[16] == (byte)0x6D && b[17] == (byte)0x61 && b[18] == (byte)0x67 && b[19] == (byte)0x65 && b[20] == (byte)0x20 &&
                         b[21] == (byte)0x46 && b[22] == (byte)0x69 && b[23] == (byte)0x6C && b[24] == (byte)0x65) {
                    return T_PSP;
                }
            }
            else if (b0 == (byte)0x51 && b1 == (byte)0x4C && b2 == (byte)0x49 && b3 == (byte)0x49 &&
                     b4 == (byte)0x46 && b5 == (byte)0x41 &&
                     b[6] == (byte)0x58) {
                return T_QFX;
            }
            else if (b0 == (byte)0x52 && b1 == (byte)0x49 && b2 == (byte)0x58 && b3 == (byte)0x33) {
                return T_RIX;
            }
            else if (b0 == (byte)0x53) {
                if (b1 == (byte)0x44 && b2 == (byte)0x50 && b3 == (byte)0x58) {
                    return T_DPX;
                }
                else if (b1 == (byte)0x49 && b2 == (byte)0x4D && b3 == (byte)0x50 &&
                         b4 == (byte)0x4C && b5 == (byte)0x45 &&
                         b[6] == (byte)0x20 && b[7] == (byte)0x20 && b[8] == (byte)0x3D) {
                    return T_FTS;
                }
                else if (b1 == (byte)0x74 && b2 == (byte)0x6F && b3 == (byte)0x72 &&
                         b4 == (byte)0x6D && b5 == (byte)0x33 &&
                         b[6] == (byte)0x44) {
                    return T_SOD;
                }
                else if (b1 == (byte)0x80 && b2 == (byte)0xf6 && b3 == (byte)0x34) {
                    return T_PIC;
                }
            }
            else if (b0 == (byte)0x56 && b1 == (byte)0x69 && b2 == (byte)0x73 && b3 == (byte)0x74 &&
                     b4 == (byte)0x61 && b5 == (byte)0x20 &&
                      b[6] == (byte)0x44 && b[7] == (byte)0x45 && b[8] == (byte)0x4D && b[9] == (byte)0x20 && b[10] == (byte)0x46 &&
                      b[11] == (byte)0x69 && b[12] == (byte)0x6C && b[13] == (byte)0x65) {
                return T_DEM;
            }
            else if (b0 == (byte)0x57 && b1 == (byte)0x56 && b2 == (byte)0x02 && b3 == (byte)0x00 &&
                     b4 == (byte)0x47 && b5 == (byte)0x45 &&
                     b[6] == (byte)0x00 && b[7] == (byte)0x0E) {
                return T_LWF;
            }
            else if (b0 == (byte)0x59 && b1 == (byte)0xA6 && b2 == (byte)0x6A && b3 == (byte)0x95) {
                return T_RAS;
            }
            else if (b0 == (byte)0x63 && b1 == (byte)0x52 && b2 == (byte)0x01 && b3 == (byte)0x01 &&
                     b4 == (byte)0x38 && b5 == (byte)0x09 &&
                     b[6] == (byte)0x3D && b[7] == (byte)0x00) {
                return T_PCD;
            }
            else if (b0 == (byte)0x65) {
                if (b1 == (byte)0x02 && b2 == (byte)0x01 && b3 == (byte)0x02) {
                    return T_ECW;
                }
                else if (b1 == (byte)0x6C && b2 == (byte)0x6D && b3 == (byte)0x6F) {
                    return T_INFINI_D;
                }
            }
            else if (b0 == (byte)0x69 && b1 == (byte)0x63 && b2 == (byte)0x6E && b3 == (byte)0x73) {
                return T_ICNS;
            }
            else if (b0 == (byte)0x6D && b1 == (byte)0x6F && b2 == (byte)0x6F && b3 == (byte)0x76) {
                return T_QTM;
            }
            else if (b0 == (byte)0x6E) {
                if (b1 == (byte)0x63 && b2 == (byte)0x6F && b3 == (byte)0x6C &&
                    b4 == (byte)0x73) {
                    return T_HDR;
                }
                else if (b1 == (byte)0x66 && b2 == (byte)0x66) {
                    return T_NFF;
                }
                else if (b1 == (byte)0x6E && b2 == (byte)0x0A && b3 == (byte)0x00 &&
                         b4 == (byte)0x5E && b5 == (byte)0x00) {
                    return T_NCR;
                }
            }
            else if (b0 == (byte)0x73 && b1 == (byte)0x72 && b2 == (byte)0x63 && b3 == (byte)0x64 &&
                     b4 == (byte)0x6F && b5 == (byte)0x63 &&
                     b[6] == (byte)0x69 && b[7] == (byte)0x64 && b[8] == (byte)0x3A) {
                return T_CALS;
            }
            else if (b0 == (byte)0x7B && b1 == (byte)0x0A && b2 == (byte)0x20 && b3 == (byte)0x20 &&
                     b4 == (byte)0x43 && b5 == (byte)0x72 &&
                     b[6] == (byte)0x65 && b[7] == (byte)0x61 && b[8] == (byte)0x74 && b[9] == (byte)0x65 && b[10] == (byte)0x64) {
                return T_MIF;
            }
            else if (b0 == (byte)0x7E && b1 == (byte)0x42 && b2 == (byte)0x4B && b3 == (byte)0x00) {
                return T_PSP;
            }
            else if (b0 == (byte)0x80) {
                if (b1 == (byte)0x2A && b2 == (byte)0x5F && b3 == (byte)0xD7 &&
                    b4 == (byte)0x00 && b5 == (byte)0x00 &&
                    b[6] == (byte)0x08 && b[7] == (byte)0x00 && b[8] == (byte)0x00 && b[9] == (byte)0x00 && b[10] == (byte)0x04 &&
                    b[11] == (byte)0x00 && b[12] == (byte)0x00 && b[13] == (byte)0x00) {
                    return T_CIN;
                }
                else if (b1 == (byte)0x3E && b2 == (byte)0x44 && b3 == (byte)0x53 &&
                         b4 == (byte)0x43 && b5 == (byte)0x49 &&
                         b[6] == (byte)0x4D) {
                    return T_J6I;
                }
            }
            else if (b0 == (byte)0x89 && b1 == (byte)0x50 && b2 == (byte)0x4E && b3 == (byte)0x47 && /* 'P' 'N' 'G', ascii code */
                     b4 == (byte)0x0D && b5 == (byte)0x0A && b[6] == (byte)0x1A && b[7] == (byte)0x0A) {
                // -119, 80, 78, 71, 13, 10, 26, 10
                return T_PNG;
            }
            else if (b0 == (byte)0x8A && b1 == (byte)0x4D && b2 == (byte)0x4E && b3 == (byte)0x47 &&
                     b4 == (byte)0x0D && b5 == (byte)0x0A &&
                     b[6] == (byte)0x1A && b[7] == (byte)0x0A) {
                return T_MNG;
            }
            else if (b0 == (byte)0xD0 && b1 == (byte)0xCF && b2 == (byte)0x11 && b3 == (byte)0xE0 &&
                     b4 == (byte)0xA1 && b5 == (byte)0xB1 &&
                     b[6] == (byte)0x1A && b[7] == (byte)0xE1 && b[8] == (byte)0x00) {
                return T_FPX;
            }
            else if (b0 == (byte)0xD3 && b1 == (byte)0x23 && b2 == (byte)0x00 && b3 == (byte)0x00 &&
                     b4 == (byte)0x03 && b5 == (byte)0x00 &&
                     b[6] == (byte)0x00 && b[7] == (byte)0x00) {
                return T_WLM;
            }
            else if (b0 == (byte)0xD7 && b1 == (byte)0xCD && b2 == (byte)0xC6 && b3 == (byte)0x9A) {
                return T_WMF;
            }
            else if (b0 == (byte)0xEB && b1 == (byte)0x3C && b2 == (byte)0x90 && b3 == (byte)0x2A) {
                return T_IMG;
            }
            else if (b0 == (byte)0xFA && b1 == (byte)0xDE && b2 == (byte)0xBA && b3 == (byte)0xBE &&
                     b4 == (byte)0x01 && b5 == (byte)0x01) {
                return T_WIC;
            }
            else if (b0 == (byte)0xFF) {
                if (b1 == (byte)0xD8 /* && b2 == (byte)0xff */) {
                    /**
                     *  (b0 == (byte)0x45 && b1 == (byte)0x78 && b2 == (byte)0x69 && b3 == (byte)0x66) || // EXIF
                     *  (b0 == (byte)0x4A && b1 == (byte)0x46 && b2 == (byte)0x49 && b3 == (byte)0x46) || // JFIF
                     *  (b0 == (byte)0xff && b1 == (byte)0xd8 ) // && b2 == (byte)0xff
                     */
                    return T_JPG;
                }
                else if (b1 == (byte)0x57 && b2 == (byte)0x50 && b3 == (byte)0x43 && b4 == (byte)0x10) {
                    return T_WPG;
                }
            }
            return null;
    	}
    }
}
