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
 * Utilities for image input and output
 *
 */
public class ImageIOUtil {
    /**
     * Constant which can be used as a file suffix to indicate a JPEG stream.
     * <ul>
     * <li>{@code http://www.faqs.org/faqs/jpeg-faq/part1/}</li>
     * <li>{@code http://www.iso.org/iso/iso_catalogue/catalogue_tc/catalogue_detail.htm?csnumber=54989}</li>
     * </ul>
     */
    public static final String T_JPG     = "jpg";

    /**
     * Constant which can be used as a file suffix to indicate a PNG stream.
     * <ul>
     * <li>{@code http://www.libpng.org/pub/png/spec/1.1/PNG-Rationale.html#R.PNG-file-signature}</li>
     * </ul>
     */
    public static final String T_PNG     = "png";

    /**
     * Constant which can be used as a file suffix to indicate an Apple Icon Image stream.
     * <p>
     * {@code 'i' 'c' 'n' 's' ascii code}
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_ICNS     = "icns";

    /**
     * Constant which can be used as a file suffix to indicate a GIF stream.
     * <p>
     * {@code GIF87A or GIF89A ascii code}
     * </p>
     * <ul>
     * <li>{@code http://www.w3.org/Graphics/GIF/spec-gif87a.txt http://www.w3.org/Graphics/GIF/spec-gif89a.txt}</li>
     * </ul>
     */
    public static final String T_GIF     = "gif";

    /**
     * Constant which can be used as a file suffix to indicate a GIF stream.
     * <p>
     * {@code BM ascii code}
     * </p>
     * <ul>
     * <li>{@code http://www.fileformat.info/format/bmp/spec/e27073c25463436f8a64fa789c886d9c/view.htm}</li>
     * </ul>
     */
    public static final String T_BMP     = "bmp";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_DCX     = "dcx";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
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
     * Constant which can be used as a file suffix to indicate a Adobe PhotoShop stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_PSD     = "psd";

    /**
     * Constant which can be used as a file suffix to indicate a TIFF stream.
     * <p>
     * Intentionally detects only the little endian tiff images ("II" in the spec).
     * </p>
     * <ul>
     * <li>{@code http://partners.adobe.com/public/developer/en/tiff/TIFF6.pdf}</li>
     * </ul>
     */
    public static final String T_TIFF    = "tiff";

    /**
     * Constant which can be used as a file suffix to indicate an SGI RGB stream.
     * <p>
     * "474 saved as a short" 474 = 0x01DA
     * </p>
     * <ul>
     * <li>{@code http://paulbourke.net/dataformats/sgirgb/sgiversion.html}</li>
     * </ul>
     */
    public static final String T_SGI_RGB = "rgb";

    /**
     * Constant which can be used as a file suffix to indicate a DirectDraw Surface stream.
     * <p>
     * 'D' 'D' 'S' ' ' ascii code
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_DDS     = "dds";

    /**
     * Constant which can be used as a file suffix to indicate a PAM stream, NetPbm magic 7 - binary RGB and RGBA.
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
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_3D2     = "3d2";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_3DMF     = "3dmf";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_92I     = "92i";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_AMFF     = "amff";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_ART     = "art";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_CALS     = "cals";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_CAM     = "cam";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_CBD     = "cbd";


    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_CE2     = "ce2";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_CIN     = "cin";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_COB     = "cob";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_CPT     = "cpt";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_CVG     = "cvg";


    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_DEM     = "dem";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_DIB     = "dib";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_DPX     = "dpx";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_DRW     = "drw";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_DWG     = "dwg";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_ECW     = "ecw";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_EMF     = "emf";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_FPX     = "fpx";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_FTS     = "fts";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_GRO     = "gro";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_HDR     = "hdr";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_HRU     = "hru";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_IMG     = "img";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_INFINI_D     = "infini-d";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_IWC     = "iwc";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_J6I     = "j6i";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_JIF     = "jif";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_JP2     = "jp2";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_KDC     = "kdc";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_L64     = "l64";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_LBM     = "lbm";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_LDF     = "ldf";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_LWF     = "lwf";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_MBM     = "mbm";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_MGL     = "mgl";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_MIF     = "mif";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_MNG     = "mng";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_MPW     = "mpw";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_MSP     = "msp";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_N64     = "n64";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_NCR     = "ncr";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_NFF     = "nff";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_NGG     = "ngg";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_NLM     = "nlm";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_NOL     = "nol";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_PAL     = "pal";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_PAX     = "pax";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_PCD     = "pcd";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_PCL     = "pcl";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_PIX     = "pix";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_POL     = "pol";

    /**
     * Constant which can be used as a file suffix to indicate a {@code Paint Shop Pro} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_PSP     = "psp";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_QFX     = "qfx";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_QTM     = "qtm";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_RAD     = "rad";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_RAS     = "ras";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_RIX     = "rix";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_SID     = "sid";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_SLD     = "sld";


    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_SOD     = "sod";


    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_WIC     = "wic";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_WLM     = "wlm";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_WMF     = "wmf";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_WPG     = "wpg";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_WRL     = "wrl";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_XBM     = "xbm";

    /**
     * Constant which can be used as a file suffix to indicate a {@code TBD} stream.
     * <p>
     * TODO
     * </p>
     * <ul>
     * <li>{@code TODO}</li>
     * </ul>
     */
    public static final String T_XPM    = "xpm";

    // JAU

    /**
     * Constant which can be used as a file suffix to indicate an SGI RGB stream.
     * <ul>
     * <li>{@code }</li>
     * </ul>
     */
    public static final String T_SGI     = "sgi";

    /**
     * Constant which can be used as a file suffix to indicate a Targa stream.
     * <ul>
     * <li>{@code }</li>
     * </ul>
     */
    public static final String T_TGA     = "tga";

	/**
	 * Determines the file suffix (i.e the image format) of the given InputStream. The given
	 * InputStream must return true from markSupported() and support a minimum of 32 bytes
	 * of read-ahead.
	 *
	 * @param stream stream to check
	 * @return the file suffix if any, otherwise <code>null</code>
	 * @throws java.io.IOException if an I/O exception occurred
	 */
	public static String getFileSuffix(InputStream stream) throws IOException {
		if (stream == null) {
            throw new IOException("Stream was null");
        }
		if (!(stream instanceof BufferedInputStream)) {
			stream = new BufferedInputStream(stream);
        }
		if (!stream.markSupported()) {
            throw new IOException("Can not get non-destructively the image format");
        }
		if (stream.available() < 32) {
            throw new IOException("Not enough bytes to read in order to get the image format");
        }
		try {
			stream.mark(32);
			final byte[] b = new byte[32];
			stream.read(b);
			return getFileSuffix(b);
		} finally {
			stream.reset();
		}

	}

	/**
	 * Determines the file suffix (i.e the image format) of the given bytes from the header
	 * of a file.
	 *
	 * @param stream stream to check
	 * @return the file suffix if any, otherwise <code>null</code>
	 * @throws java.io.IOException if an I/O exception occurred
	 */
    public static String getFileSuffix(final byte[] b) {
        if ( (b[0] == 0xff && b[1] == 0xd8 /* && b[2] == 0xff */) ||
             (b[0] == 0x4A && b[1] == 0x46 && b[2] == 0x49 && b[3] == 0x46)/* JFIF */ ||
             (b[0] == 0x45 && b[1] == 0x78 && b[2] == 0x69 && b[3] == 0x66)/* EXIF */) {
            return T_JPG;
        }
        if (b[0] == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47 && /* 'P' 'N' 'G', ascii code */
            b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return T_PNG;
        }
        if (b[0] == 0x69 && b[1] == 0x63 && b[2] == 0x6E && b[3] == 0x73) {
            return T_ICNS;
        }
        if (b[0] == 0x47 && b[1] == 0x49 && b[2] == 0x46 && b[3] == 0x38 && (b[4] == 0x37 || b[4] == 0x39) &&
            b[5] == 0x61) {
            return T_GIF;
        }
        if (b[0] == 0x42 && b[1] == 0x4d) {
            return T_BMP;
        }
        if (b[0] == 0x3A && b[1] == 0xDE && b[2] == 0x68 && b[3] == 0xB1) {
            return T_DCX;
        }
        if (b[0] == 0x0A && b[1] == 0x05 && b[2] == 0x01 && b[3] == 0x08) {
            return T_PCX;
        }
        if (b[0] == 0x50 && (b[1] == 0x33 /* plain */|| b[1] == 0x36)) {
            return T_PPM;
        }
        if (b[0] == 0x38 && b[1] == 0x42 && b[2] == 0x50 && b[3] == 0x53 && b[4] == 0x00 && b[5] == 0x01 &&
            b[6] == 0x00 && b[7] == 0x00 && b[8] == 0x00 && b[9] == 0x00) {
            // Adobe PhotoShop
            return T_PSD;
        }
        if (b[0] == 0x49 && b[1] == 0x49 && b[2] == 0x2A && b[3] == 0x00) {
            return T_TIFF;
        }
        if (b[0] == 0x01 && b[1] == 0xDA /* && b[2] == 0x01 && b[3] == 0x01 && b[4] == 0x00 && b[5] == 0x03 */) {
            return T_SGI_RGB;
        }
        if (b[0] == 0x44 && b[1] == 0x44 && b[2] == 0x53 && b[3] == 0x20) {
            return T_DDS;
        }
        if (b[0] == 0x50 && b[1] == 0x37) {
            return T_PAM;
        }
        if (b[0] == 0x50 && (b[1] == 0x32 /* plain */|| b[1] == 0x35)) {
            return T_PGM;
        }
        if (b[0] == 0x50 && (b[1] == 0x31 /* plain */|| b[1] == 0x34)) {
            return T_PBM;
        }
        if (b[0] == 0x3D && b[1] == 0x02) {
            return T_3D2;
        }
        if (b[0] == 0x33 && b[1] == 0x44 && b[2] == 0x4D && b[3] == 0x46) {
            return T_3DMF;
        }
        if (b[0] == 0x2A && b[1] == 0x2A && b[2] == 0x54 && b[3] == 0x49 && b[4] == 0x39 && b[5] == 0x32 &&
            b[6] == 0x2A && b[7] == 0x2A && b[8] == 0x01 && b[9] == 0x00 && b[10] == 0x58 &&
            b[11] == 0x6E && b[12] == 0x56 && b[13] == 0x69) {
            return T_92I;
        }
        if (b[0] == 0x41 && b[1] == 0x4D && b[2] == 0x46 && b[3] == 0x46) {
            return T_AMFF;
        }
        if (b[0] == 0x4A && b[1] == 0x47 && (b[2] == 0x03 || b[2] == 0x04) && b[3] == 0x0E && b[4] == 0x00 &&
            b[5] == 0x00 && b[6] == 0x00) {
            return T_ART;
        }
        if (b[0] == 0x73 && b[1] == 0x72 && b[2] == 0x63 && b[3] == 0x64 && b[4] == 0x6F && b[5] == 0x63 &&
            b[6] == 0x69 && b[7] == 0x64 && b[8] == 0x3A) {
            return T_CALS;
        }
        if (b[0] == 0x07 && b[1] == 0x20 && b[2] == 0x4D && b[3] == 0x4D) {
            return T_CAM;
        }
        if (b[0] == 0x20 && b[1] == 0x77 && b[2] == 0x00 && b[3] == 0x02) {
            return T_CBD;
        }
        if (b[0] == 0x45 && b[1] == 0x59 && b[2] == 0x45 && b[3] == 0x53) {
            return T_CE2;
        }
        if (b[0] == 0x80 && b[1] == 0x2A && b[2] == 0x5F && b[3] == 0xD7 && b[4] == 0x00 && b[5] == 0x00 &&
            b[6] == 0x08 && b[7] == 0x00 && b[8] == 0x00 && b[9] == 0x00 && b[10] == 0x04 &&
            b[11] == 0x00 && b[12] == 0x00 && b[13] == 0x00) {
            return T_CIN;
        }
        if (b[0] == 0x43 && b[1] == 0x61 && b[2] == 0x6C && b[3] == 0x69 && b[4] == 0x67 && b[5] == 0x61 &&
            b[6] == 0x72 && b[7] == 0x69) {
            return T_COB;
        }
        if (b[0] == 0x43 && b[1] == 0x50 && b[2] == 0x54 && b[3] == 0x46 && b[4] == 0x49 && b[5] == 0x4C &&
            b[6] == 0x45) {
            return T_CPT;
        }
        if (b[0] == 0x43 && b[1] == 0x41 && b[2] == 0x4C && b[3] == 0x41 && b[4] == 0x4D && b[5] == 0x55 &&
            b[6] == 0x53 && b[7] == 0x43 && b[8] == 0x56 && b[9] == 0x47) {
            return T_CVG;
        }
        if (b[0] == 0x56 && b[1] == 0x69 && b[2] == 0x73 && b[3] == 0x74 && b[4] == 0x61 && b[5] == 0x20 &&
            b[6] == 0x44 && b[7] == 0x45 && b[8] == 0x4D && b[9] == 0x20 && b[10] == 0x46 &&
            b[11] == 0x69 && b[12] == 0x6C && b[13] == 0x65) {
            return T_DEM;
        }
        if (b[0] == 0x42 && b[1] == 0x4D && b[2] == 0x36) {
            return T_DIB;
        }
        if (b[0] == 0x53 && b[1] == 0x44 && b[2] == 0x50 && b[3] == 0x58) {
            return T_DPX;
        }
        if (b[0] == 0x01 && b[1] == 0xFF && b[2] == 0x02 && b[3] == 0x04 && b[4] == 0x03 && b[5] == 0x02) {
            return T_DRW;
        }
        if (b[0] == 0x41 && b[1] == 0x43 && b[2] == 0x31 && b[3] == 0x30) {
            return T_DWG;
        }
        if (b[0] == 0x65 && b[1] == 0x02 && b[2] == 0x01 && b[3] == 0x02) {
            return T_ECW;
        }
        if (b[0] == 0x01 && b[1] == 0x00 && b[2] == 0x00 && b[3] == 0x00 && b[4] == 0x58 && b[5] == 0x00 &&
            b[6] == 0x00 && b[7] == 0x00) {
            return T_EMF;
        }
        if (b[0] == 0xD0 && b[1] == 0xCF && b[2] == 0x11 && b[3] == 0xE0 && b[4] == 0xA1 && b[5] == 0xB1 &&
            b[6] == 0x1A && b[7] == 0xE1 && b[8] == 0x00) {
            return T_FPX;
        }
        if (b[0] == 0x53 && b[1] == 0x49 && b[2] == 0x4D && b[3] == 0x50 && b[4] == 0x4C && b[5] == 0x45 &&
            b[6] == 0x20 && b[7] == 0x20 && b[8] == 0x3D) {
            return T_FTS;
        }
        if (b[0] == 0x48 && b[1] == 0x50 && b[2] == 0x48 && b[3] == 0x50 && b[4] == 0x34 && b[5] == 0x38 &&
            b[6] == 0x2D && b[7] == 0x45 && b[8] == 0x1E && b[9] == 0x2B) {
            return T_GRO;
        }
        if (b[0] == 0x6E && b[1] == 0x63 && b[2] == 0x6F && b[3] == 0x6C && b[4] == 0x73) {
            return T_HDR;
        }
        if (b[0] == 0x35 && b[1] == 0x4B && b[2] == 0x50 && b[3] == 0x35 && b[4] == 0x31 && b[5] == 0x5D &&
            b[6] == 0x2A && b[7] == 0x67 && b[8] == 0x72 && b[9] == 0x72 && b[10] == 0x80 &&
            b[11] == 0x83 && b[12] == 0x85 && b[13] == 0x63) {
            return T_HRU;
        }
        if (b[0] == 0xEB && b[1] == 0x3C && b[2] == 0x90 && b[3] == 0x2A) {
            return T_IMG;
        }
        if (b[0] == 0x65 && b[1] == 0x6C && b[2] == 0x6D && b[3] == 0x6F) {
            return T_INFINI_D;
        }
        if (b[0] == 0x49 && b[1] == 0x57 && b[2] == 0x43 && b[3] == 0x01) {
            return T_IWC;
        }
        if (b[0] == 0x80 && b[1] == 0x3E && b[2] == 0x44 && b[3] == 0x53 && b[4] == 0x43 && b[5] == 0x49 &&
            b[6] == 0x4D) {
            return T_J6I;
        }
        if (b[0] == 0x4A && b[1] == 0x49 && b[2] == 0x46 && b[3] == 0x39 && b[4] == 0x39 && b[5] == 0x61) {
            return T_JIF;
        }
        if (b[0] == 0x00 && b[1] == 0x00 && b[2] == 0x00 && b[3] == 0x0C && b[4] == 0x6A && b[5] == 0x50 &&
            b[6] == 0x20 && b[7] == 0x20 && b[8] == 0x0D && b[9] == 0x0A && b[10] == 0x87 &&
            b[11] == 0x0A) {
            return T_JP2;
        }
        if (b[0] == 0x4D && b[1] == 0x4D && b[2] == 0x00 && b[3] == 0x2A) {
            return T_KDC;
        }
        if (b[0] == 0x36 && b[1] == 0x34 && b[2] == 0x4C && b[3] == 0x41 && b[4] == 0x4E && b[5] == 0x20 &&
            b[6] == 0x49 && b[7] == 0x44 && b[8] == 0x42 && b[9] == 0x4C && b[10] == 0x4F &&
            b[11] == 0x43 && b[12] == 0x4B) {
            return T_L64;
        }
        if (b[0] == 0x46 && b[1] == 0x4F && b[2] == 0x52 && b[3] == 0x4D) {
            return T_LBM;
        }
        if (b[0] == 0x49 && b[1] == 0x49 && b[2] == 0x2A && b[3] == 0x00 && b[4] == 0x08 && b[5] == 0x00 &&
            b[6] == 0x00 && b[7] == 0x00 && b[8] == 0x0E && b[9] == 0x00 && b[10] == 0x00 &&
            b[11] == 0x01 && b[12] == 0x04 && b[13] == 0x00) {
            return T_LDF;
        }
        if (b[0] == 0x57 && b[1] == 0x56 && b[2] == 0x02 && b[3] == 0x00 && b[4] == 0x47 && b[5] == 0x45 &&
            b[6] == 0x00 && b[7] == 0x0E) {
            return T_LWF;
        }
        if (b[0] == 0x37 && b[1] == 0x00 && b[2] == 0x00 && b[3] == 0x10 && b[4] == 0x42 && b[5] == 0x00 &&
            b[6] == 0x00 && b[7] == 0x10 && b[8] == 0x00 && b[9] == 0x00 && b[10] == 0x00 &&
            b[11] == 0x00 && b[12] == 0x39 && b[13] == 0x64) {
            return T_MBM;
        }
        if (b[0] == 0x4D && b[1] == 0x47 && b[2] == 0x4C) {
            return T_MGL;
        }
        if (b[0] == 0x7B && b[1] == 0x0A && b[2] == 0x20 && b[3] == 0x20 && b[4] == 0x43 && b[5] == 0x72 &&
            b[6] == 0x65 && b[7] == 0x61 && b[8] == 0x74 && b[9] == 0x65 && b[10] == 0x64) {
            return T_MIF;
        }
        if (b[0] == 0x8A && b[1] == 0x4D && b[2] == 0x4E && b[3] == 0x47 && b[4] == 0x0D && b[5] == 0x0A &&
            b[6] == 0x1A && b[7] == 0x0A) {
            return T_MNG;
        }
        if (b[0] == 0x4D && b[1] == 0x50 && b[2] == 0x46) {
            return T_MPW;
        }
        if (b[0] == 0x44 && b[1] == 0x61 && b[2] == 0x6E && b[3] == 0x4D) {
            return T_MSP;
        }
        if (b[0] == 0x43 && b[1] == 0x36 && b[2] == 0x34) {
            return T_N64;
        }
        if (b[0] == 0x6E && b[1] == 0x6E && b[2] == 0x0A && b[3] == 0x00 && b[4] == 0x5E && b[5] == 0x00) {
            return T_NCR;
        }
        if (b[0] == 0x6E && b[1] == 0x66 && b[2] == 0x66) {
            return T_NFF;
        }
        if (b[0] == 0x4E && b[1] == 0x47 && b[2] == 0x47 && b[3] == 0x00 && b[4] == 0x01 && b[5] == 0x00) {
            return T_NGG;
        }
        if (b[0] == 0x4E && b[1] == 0x4C && b[2] == 0x4D && b[3] == 0x20 && b[4] == 0x01 && b[5] == 0x02 &&
            b[6] == 0x00) {
            return T_NLM;
        }
        if (b[0] == 0x4E && b[1] == 0x4F && b[2] == 0x4C && b[3] == 0x00 && b[4] == 0x01 && b[5] == 0x00 &&
            b[6] == 0x06 && b[7] == 0x01 && b[8] == 0x03 && b[9] == 0x00) {
            return T_NOL;
        }
        if (b[0] == 0x41 && b[1] == 0x48) {
            return T_PAL;
        }
        if (b[0] == 0x50 && b[1] == 0x41 && b[2] == 0x58) {
            return T_PAX;
        }
        if (b[0] == 0x63 && b[1] == 0x52 && b[2] == 0x01 && b[3] == 0x01 && b[4] == 0x38 && b[5] == 0x09 &&
            b[6] == 0x3D && b[7] == 0x00) {
            return T_PCD;
        }
        if (b[0] == 0x1B && b[1] == 0x45 && b[2] == 0x1B && b[3] == 0x26 && b[4] == 0x6C && b[5] == 0x30 &&
            b[6] == 0x4F && b[7] == 0x1B && b[8] == 0x26 && b[9] == 0x6C && b[10] == 0x30 &&
            b[11] == 0x45 && b[12] == 0x1B && b[13] == 0x26) {
            return T_PCL;
        }
        if (b[0] == 0x50 && b[1] == 0x49 && b[2] == 0x58 && b[3] == 0x20) {
            return T_PIX;
        }
        if (b[0] == 0x50 && b[1] == 0x4F && b[2] == 0x4C && b[3] == 0x20 && b[4] == 0x46 && b[5] == 0x6F &&
            b[6] == 0x72 && b[7] == 0x6D && b[8] == 0x61 && b[9] == 0x74) {
            return T_POL;
        }
        if (b[0] == 0x7E && b[1] == 0x42 && b[2] == 0x4B && b[3] == 0x00) {
            return T_PSP;
        }
        if (b[0] == 0x50 && b[1] == 0x61 && b[2] == 0x69 && b[3] == 0x6E && b[4] == 0x74 && b[5] == 0x20 &&
            b[6] == 0x53 && b[7] == 0x68 && b[8] == 0x6F && b[9] == 0x70 && b[10] == 0x20 &&
            b[11] == 0x50 && b[12] == 0x72 && b[13] == 0x6F && b[14] == 0x20 && b[15] == 0x49 &&
            b[16] == 0x6D && b[17] == 0x61 && b[18] == 0x67 && b[19] == 0x65 && b[20] == 0x20 &&
            b[21] == 0x46 && b[22] == 0x69 && b[23] == 0x6C && b[24] == 0x65) {
            return T_PSP;
        }
        if (b[0] == 0x51 && b[1] == 0x4C && b[2] == 0x49 && b[3] == 0x49 && b[4] == 0x46 && b[5] == 0x41 &&
            b[6] == 0x58) {
            return T_QFX;
        }
        if (b[0] == 0x6D && b[1] == 0x6F && b[2] == 0x6F && b[3] == 0x76) {
            return T_QTM;
        }
        if (b[0] == 0x46 && b[1] == 0x4F && b[2] == 0x52 && b[3] == 0x4D && b[4] == 0x41 && b[5] == 0x54 &&
            b[6] == 0x3D) {
            return T_RAD;
        }
        if (b[0] == 0x59 && b[1] == 0xA6 && b[2] == 0x6A && b[3] == 0x95) {
            return T_RAS;
        }
        if (b[0] == 0x52 && b[1] == 0x49 && b[2] == 0x58 && b[3] == 0x33) {
            return T_RIX;
        }
        if (b[0] == 0x23 && b[1] == 0x20 && b[2] == 0x24 && b[3] == 0x49 && b[4] == 0x64 && b[5] == 0x3A &&
            b[6] == 0x20) {
            return T_SID;
        }
        if (b[0] == 0x41 && b[1] == 0x75 && b[2] == 0x74 && b[3] == 0x6F && b[4] == 0x43 && b[5] == 0x41 &&
            b[6] == 0x44 && b[7] == 0x20 && b[8] == 0x53 && b[9] == 0x6C && b[10] == 0x69 &&
            b[11] == 0x64 && b[12] == 0x65) {
            return T_SLD;
        }
        if (b[0] == 0x53 && b[1] == 0x74 && b[2] == 0x6F && b[3] == 0x72 && b[4] == 0x6D && b[5] == 0x33 &&
            b[6] == 0x44) {
            return T_SOD;
        }
        if (b[0] == 0xFA && b[1] == 0xDE && b[2] == 0xBA && b[3] == 0xBE && b[4] == 0x01 && b[5] == 0x01) {
            return T_WIC;
        }
        if (b[0] == 0xD3 && b[1] == 0x23 && b[2] == 0x00 && b[3] == 0x00 && b[4] == 0x03 && b[5] == 0x00 &&
            b[6] == 0x00 && b[7] == 0x00) {
            return T_WLM;
        }
        if (b[0] == 0xD7 && b[1] == 0xCD && b[2] == 0xC6 && b[3] == 0x9A) {
            return T_WMF;
        }
        if (b[0] == 0xFF && b[1] == 0x57 && b[2] == 0x50 && b[3] == 0x43 && b[4] == 0x10) {
            return T_WPG;
        }
        if (b[0] == 0x23 && b[1] == 0x56 && b[2] == 0x52 && b[3] == 0x4D && b[4] == 0x4C && b[5] == 0x20 &&
            b[6] == 0x56 && b[7] == 0x32 && b[8] == 0x2E && b[9] == 0x30) {
            return T_WRL;
        }
        if (b[0] == 0x23 && b[1] == 0x64 && b[2] == 0x65 && b[3] == 0x66 && b[4] == 0x69 && b[5] == 0x6E &&
            b[6] == 0x65) {
            return T_XBM;
        }
        if (b[0] == 0x2F && b[1] == 0x2A && b[2] == 0x20 && b[3] == 0x58 && b[4] == 0x50 && b[5] == 0x4D &&
            b[6] == 0x20 && b[7] == 0x2A && b[8] == 0x2F) {
            return T_XPM;
        }
        return null;
	}
}
