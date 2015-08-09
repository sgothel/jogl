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
	
    public static String getFileSuffix(final byte[] b) {
    	/**
         * http://www.faqs.org/faqs/jpeg-faq/part1/
         * http://www.iso.org/iso/iso_catalogue/catalogue_tc/catalogue_detail.htm?csnumber=54989
         */
        if ((b[0] == 0xff && b[1] == 0xd8 /* && b[2] == 0xff */)
                || (b[0] == 0x4A && b[1] == 0x46 && b[2] == 0x49 && b[3] == 0x46)/* JFIF */
                || (b[0] == 0x45 && b[1] == 0x78 && b[2] == 0x69 && b[3] == 0x66)/* EXIF */) {
            return TextureIO.JPG;
        }
        /**
         * http://www.libpng.org/pub/png/spec/1.1/PNG-Rationale.html#R.PNG-file-signature
         */
        if (b[0] == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47 /* 'P' 'N' 'G', ascii code */
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return TextureIO.PNG;
        }
        /**
         * Apple Icon Image
         *
         * 'i' 'c' 'n' 's' ascii code
         */
        if (b[0] == 0x69 && b[1] == 0x63 && b[2] == 0x6E && b[3] == 0x73) {
            return "icns";
        }
        /**
         * http://www.w3.org/Graphics/GIF/spec-gif87a.txt http://www.w3.org/Graphics/GIF/spec-gif89a.txt
         *
         * GIF87A or GIF89A ascii code
         */
        if (b[0] == 0x47 && b[1] == 0x49 && b[2] == 0x46 && b[3] == 0x38 && (b[4] == 0x37 || b[4] == 0x39)
                && b[5] == 0x61) {
            return TextureIO.GIF;
        }
        /**
         * http://www.fileformat.info/format/bmp/spec/e27073c25463436f8a64fa789c886d9c/view.htm
         *
         * BM ascii code
         */
        if (b[0] == 0x42 && b[1] == 0x4d) {
            return "bmp";
        }
        if (b[0] == 0x3A && b[1] == 0xDE && b[2] == 0x68 && b[3] == 0xB1) {
            return "dcx";
        }
        if (b[0] == 0x0A && b[1] == 0x05 && b[2] == 0x01 && b[3] == 0x08) {
            return "pcx";
        }
        /**
         * http://netpbm.sourceforge.net/doc/ppm.html
         */
        if (b[0] == 0x50 && (b[1] == 0x33 /* plain */|| b[1] == 0x36)) {
            return TextureIO.PPM;
        }
        if (b[0] == 0x38 && b[1] == 0x42 && b[2] == 0x50 && b[3] == 0x53 && b[4] == 0x00 && b[5] == 0x01
                && b[6] == 0x00 && b[7] == 0x00 && b[8] == 0x00 && b[9] == 0x00) {
            // Adobe PhotoShop
            return "psd";
        }
        /**
         * http://partners.adobe.com/public/developer/en/tiff/TIFF6.pdf
         *
         * intentionally detects only the little endian tiff images ("II" in the spec)
         */
        if (b[0] == 0x49 && b[1] == 0x49 && b[2] == 0x2A && b[3] == 0x00) {
            return TextureIO.TIFF;
        }
        /**
         * http://paulbourke.net/dataformats/sgirgb/sgiversion.html
         *
         * "474 saved as a short" 474 = 0x01DA
         */
        if (b[0] == 0x01 && b[1] == 0xDA /* && b[2] == 0x01 && b[3] == 0x01 && b[4] == 0x00 && b[5] == 0x03 */) {
            return TextureIO.SGI_RGB;
        }
        /**
         * 'D' 'D' 'S' ' ' ascii code
         */
        if (b[0] == 0x44 && b[1] == 0x44 && b[2] == 0x53 && b[3] == 0x20) {
            return TextureIO.DDS;
        }
        /**
         * http://netpbm.sourceforge.net/doc/pam.html
         */
        if (b[0] == 0x50 && b[1] == 0x37) {
            return TextureIO.PAM;
        }
        /**
         * http://netpbm.sourceforge.net/doc/pgm.html
         */
        if (b[0] == 0x50 && (b[1] == 0x32 /* plain */|| b[1] == 0x35)) {
            return "pgm";
        }
        /**
         * http://netpbm.sourceforge.net/doc/pbm.html
         */
        if (b[0] == 0x50 && (b[1] == 0x31 /* plain */|| b[1] == 0x34)) {
            return "pbm";
        }
        if (b[0] == 0x3D && b[1] == 0x02) {
            return "3d2";
        }
        if (b[0] == 0x33 && b[1] == 0x44 && b[2] == 0x4D && b[3] == 0x46) {
            return "3dmf";
        }
        if (b[0] == 0x2A && b[1] == 0x2A && b[2] == 0x54 && b[3] == 0x49 && b[4] == 0x39 && b[5] == 0x32
                && b[6] == 0x2A && b[7] == 0x2A && b[8] == 0x01 && b[9] == 0x00 && b[10] == 0x58
                && b[11] == 0x6E && b[12] == 0x56 && b[13] == 0x69) {
            return "92i";
        }
        if (b[0] == 0x41 && b[1] == 0x4D && b[2] == 0x46 && b[3] == 0x46) {
            return "amff";
        }
        if (b[0] == 0x4A && b[1] == 0x47 && (b[2] == 0x03 || b[2] == 0x04) && b[3] == 0x0E && b[4] == 0x00
                && b[5] == 0x00 && b[6] == 0x00) {
            return "art";
        }
        if (b[0] == 0x73 && b[1] == 0x72 && b[2] == 0x63 && b[3] == 0x64 && b[4] == 0x6F && b[5] == 0x63
                && b[6] == 0x69 && b[7] == 0x64 && b[8] == 0x3A) {
            return "cals";
        }
        if (b[0] == 0x07 && b[1] == 0x20 && b[2] == 0x4D && b[3] == 0x4D) {
            return "cam";
        }
        if (b[0] == 0x20 && b[1] == 0x77 && b[2] == 0x00 && b[3] == 0x02) {
            return "cbd";
        }
        if (b[0] == 0x45 && b[1] == 0x59 && b[2] == 0x45 && b[3] == 0x53) {
            return "ce2";
        }
        if (b[0] == 0x80 && b[1] == 0x2A && b[2] == 0x5F && b[3] == 0xD7 && b[4] == 0x00 && b[5] == 0x00
                && b[6] == 0x08 && b[7] == 0x00 && b[8] == 0x00 && b[9] == 0x00 && b[10] == 0x04
                && b[11] == 0x00 && b[12] == 0x00 && b[13] == 0x00) {
            return "cin";
        }
        if (b[0] == 0x43 && b[1] == 0x61 && b[2] == 0x6C && b[3] == 0x69 && b[4] == 0x67 && b[5] == 0x61
                && b[6] == 0x72 && b[7] == 0x69) {
            return "cob";
        }
        if (b[0] == 0x43 && b[1] == 0x50 && b[2] == 0x54 && b[3] == 0x46 && b[4] == 0x49 && b[5] == 0x4C
                && b[6] == 0x45) {
            return "cpt";
        }
        if (b[0] == 0x43 && b[1] == 0x41 && b[2] == 0x4C && b[3] == 0x41 && b[4] == 0x4D && b[5] == 0x55
                && b[6] == 0x53 && b[7] == 0x43 && b[8] == 0x56 && b[9] == 0x47) {
            return "cvg";
        }
        if (b[0] == 0x56 && b[1] == 0x69 && b[2] == 0x73 && b[3] == 0x74 && b[4] == 0x61 && b[5] == 0x20
                && b[6] == 0x44 && b[7] == 0x45 && b[8] == 0x4D && b[9] == 0x20 && b[10] == 0x46
                && b[11] == 0x69 && b[12] == 0x6C && b[13] == 0x65) {
            return "dem";
        }
        if (b[0] == 0x42 && b[1] == 0x4D && b[2] == 0x36) {
            return "dib";
        }
        if (b[0] == 0x53 && b[1] == 0x44 && b[2] == 0x50 && b[3] == 0x58) {
            return "dpx";
        }
        if (b[0] == 0x01 && b[1] == 0xFF && b[2] == 0x02 && b[3] == 0x04 && b[4] == 0x03 && b[5] == 0x02) {
            return "drw";
        }
        if (b[0] == 0x41 && b[1] == 0x43 && b[2] == 0x31 && b[3] == 0x30) {
            return "dwg";
        }
        if (b[0] == 0x65 && b[1] == 0x02 && b[2] == 0x01 && b[3] == 0x02) {
            return "ecw";
        }
        if (b[0] == 0x01 && b[1] == 0x00 && b[2] == 0x00 && b[3] == 0x00 && b[4] == 0x58 && b[5] == 0x00
                && b[6] == 0x00 && b[7] == 0x00) {
            return "emf";
        }
        if (b[0] == 0xD0 && b[1] == 0xCF && b[2] == 0x11 && b[3] == 0xE0 && b[4] == 0xA1 && b[5] == 0xB1
                && b[6] == 0x1A && b[7] == 0xE1 && b[8] == 0x00) {
            return "fpx";
        }
        if (b[0] == 0x53 && b[1] == 0x49 && b[2] == 0x4D && b[3] == 0x50 && b[4] == 0x4C && b[5] == 0x45
                && b[6] == 0x20 && b[7] == 0x20 && b[8] == 0x3D) {
            return "fts";
        }
        if (b[0] == 0x48 && b[1] == 0x50 && b[2] == 0x48 && b[3] == 0x50 && b[4] == 0x34 && b[5] == 0x38
                && b[6] == 0x2D && b[7] == 0x45 && b[8] == 0x1E && b[9] == 0x2B) {
            return "gro";
        }
        if (b[0] == 0x6E && b[1] == 0x63 && b[2] == 0x6F && b[3] == 0x6C && b[4] == 0x73) {
            return "hdr";
        }
        if (b[0] == 0x35 && b[1] == 0x4B && b[2] == 0x50 && b[3] == 0x35 && b[4] == 0x31 && b[5] == 0x5D
                && b[6] == 0x2A && b[7] == 0x67 && b[8] == 0x72 && b[9] == 0x72 && b[10] == 0x80
                && b[11] == 0x83 && b[12] == 0x85 && b[13] == 0x63) {
            return "hru";
        }
        if (b[0] == 0xEB && b[1] == 0x3C && b[2] == 0x90 && b[3] == 0x2A) {
            return "img";
        }
        if (b[0] == 0x65 && b[1] == 0x6C && b[2] == 0x6D && b[3] == 0x6F) {
            return "infini-d";
        }
        if (b[0] == 0x49 && b[1] == 0x57 && b[2] == 0x43 && b[3] == 0x01) {
            return "iwc";
        }
        if (b[0] == 0x80 && b[1] == 0x3E && b[2] == 0x44 && b[3] == 0x53 && b[4] == 0x43 && b[5] == 0x49
                && b[6] == 0x4D) {
            return "j6i";
        }
        if (b[0] == 0x4A && b[1] == 0x49 && b[2] == 0x46 && b[3] == 0x39 && b[4] == 0x39 && b[5] == 0x61) {
            return "jif";
        }
        if (b[0] == 0x00 && b[1] == 0x00 && b[2] == 0x00 && b[3] == 0x0C && b[4] == 0x6A && b[5] == 0x50
                && b[6] == 0x20 && b[7] == 0x20 && b[8] == 0x0D && b[9] == 0x0A && b[10] == 0x87
                && b[11] == 0x0A) {
            return "jp2";
        }
        if (b[0] == 0x4D && b[1] == 0x4D && b[2] == 0x00 && b[3] == 0x2A) {
            return "kdc";
        }
        if (b[0] == 0x36 && b[1] == 0x34 && b[2] == 0x4C && b[3] == 0x41 && b[4] == 0x4E && b[5] == 0x20
                && b[6] == 0x49 && b[7] == 0x44 && b[8] == 0x42 && b[9] == 0x4C && b[10] == 0x4F
                && b[11] == 0x43 && b[12] == 0x4B) {
            return "l64";
        }
        if (b[0] == 0x46 && b[1] == 0x4F && b[2] == 0x52 && b[3] == 0x4D) {
            return "lbm";
        }
        if (b[0] == 0x49 && b[1] == 0x49 && b[2] == 0x2A && b[3] == 0x00 && b[4] == 0x08 && b[5] == 0x00
                && b[6] == 0x00 && b[7] == 0x00 && b[8] == 0x0E && b[9] == 0x00 && b[10] == 0x00
                && b[11] == 0x01 && b[12] == 0x04 && b[13] == 0x00) {
            return "ldf";
        }
        if (b[0] == 0x57 && b[1] == 0x56 && b[2] == 0x02 && b[3] == 0x00 && b[4] == 0x47 && b[5] == 0x45
                && b[6] == 0x00 && b[7] == 0x0E) {
            return "lwf";
        }
        if (b[0] == 0x37 && b[1] == 0x00 && b[2] == 0x00 && b[3] == 0x10 && b[4] == 0x42 && b[5] == 0x00
                && b[6] == 0x00 && b[7] == 0x10 && b[8] == 0x00 && b[9] == 0x00 && b[10] == 0x00
                && b[11] == 0x00 && b[12] == 0x39 && b[13] == 0x64) {
            return "mbm";
        }
        if (b[0] == 0x4D && b[1] == 0x47 && b[2] == 0x4C) {
            return "mgl";
        }
        if (b[0] == 0x7B && b[1] == 0x0A && b[2] == 0x20 && b[3] == 0x20 && b[4] == 0x43 && b[5] == 0x72
                && b[6] == 0x65 && b[7] == 0x61 && b[8] == 0x74 && b[9] == 0x65 && b[10] == 0x64) {
            return "mif";
        }
        if (b[0] == 0x8A && b[1] == 0x4D && b[2] == 0x4E && b[3] == 0x47 && b[4] == 0x0D && b[5] == 0x0A
                && b[6] == 0x1A && b[7] == 0x0A) {
            return "mng";
        }
        if (b[0] == 0x4D && b[1] == 0x50 && b[2] == 0x46) {
            return "mpw";
        }
        if (b[0] == 0x44 && b[1] == 0x61 && b[2] == 0x6E && b[3] == 0x4D) {
            return "msp";
        }
        if (b[0] == 0x43 && b[1] == 0x36 && b[2] == 0x34) {
            return "n64";
        }
        if (b[0] == 0x6E && b[1] == 0x6E && b[2] == 0x0A && b[3] == 0x00 && b[4] == 0x5E && b[5] == 0x00) {
            return "ncr";
        }
        if (b[0] == 0x6E && b[1] == 0x66 && b[2] == 0x66) {
            return "nff";
        }
        if (b[0] == 0x4E && b[1] == 0x47 && b[2] == 0x47 && b[3] == 0x00 && b[4] == 0x01 && b[5] == 0x00) {
            return "ngg";
        }
        if (b[0] == 0x4E && b[1] == 0x4C && b[2] == 0x4D && b[3] == 0x20 && b[4] == 0x01 && b[5] == 0x02
                && b[6] == 0x00) {
            return "nlm";
        }
        if (b[0] == 0x4E && b[1] == 0x4F && b[2] == 0x4C && b[3] == 0x00 && b[4] == 0x01 && b[5] == 0x00
                && b[6] == 0x06 && b[7] == 0x01 && b[8] == 0x03 && b[9] == 0x00) {
            return "nol";
        }
        if (b[0] == 0x41 && b[1] == 0x48) {
            return "pal";
        }
        if (b[0] == 0x50 && b[1] == 0x41 && b[2] == 0x58) {
            return "pax";
        }
        if (b[0] == 0x63 && b[1] == 0x52 && b[2] == 0x01 && b[3] == 0x01 && b[4] == 0x38 && b[5] == 0x09
                && b[6] == 0x3D && b[7] == 0x00) {
            return "pcd";
        }
        if (b[0] == 0x1B && b[1] == 0x45 && b[2] == 0x1B && b[3] == 0x26 && b[4] == 0x6C && b[5] == 0x30
                && b[6] == 0x4F && b[7] == 0x1B && b[8] == 0x26 && b[9] == 0x6C && b[10] == 0x30
                && b[11] == 0x45 && b[12] == 0x1B && b[13] == 0x26) {
            return "pcl";
        }
        if (b[0] == 0x50 && b[1] == 0x49 && b[2] == 0x58 && b[3] == 0x20) {
            return "pix";
        }
        if (b[0] == 0x50 && b[1] == 0x4F && b[2] == 0x4C && b[3] == 0x20 && b[4] == 0x46 && b[5] == 0x6F
                && b[6] == 0x72 && b[7] == 0x6D && b[8] == 0x61 && b[9] == 0x74) {
            return "pol";
        }
        // Paint Shop Pro
        if (b[0] == 0x7E && b[1] == 0x42 && b[2] == 0x4B && b[3] == 0x00) {
            return "psp";
        }
        if (b[0] == 0x50 && b[1] == 0x61 && b[2] == 0x69 && b[3] == 0x6E && b[4] == 0x74 && b[5] == 0x20
                && b[6] == 0x53 && b[7] == 0x68 && b[8] == 0x6F && b[9] == 0x70 && b[10] == 0x20
                && b[11] == 0x50 && b[12] == 0x72 && b[13] == 0x6F && b[14] == 0x20 && b[15] == 0x49
                && b[16] == 0x6D && b[17] == 0x61 && b[18] == 0x67 && b[19] == 0x65 && b[20] == 0x20
                && b[21] == 0x46 && b[22] == 0x69 && b[23] == 0x6C && b[24] == 0x65) {
            return "psp";
        }
        if (b[0] == 0x51 && b[1] == 0x4C && b[2] == 0x49 && b[3] == 0x49 && b[4] == 0x46 && b[5] == 0x41
                && b[6] == 0x58) {
            return "qfx";
        }
        if (b[0] == 0x6D && b[1] == 0x6F && b[2] == 0x6F && b[3] == 0x76) {
            return "qtm";
        }
        if (b[0] == 0x46 && b[1] == 0x4F && b[2] == 0x52 && b[3] == 0x4D && b[4] == 0x41 && b[5] == 0x54
                && b[6] == 0x3D) {
            return "rad";
        }
        if (b[0] == 0x59 && b[1] == 0xA6 && b[2] == 0x6A && b[3] == 0x95) {
            return "ras";
        }
        if (b[0] == 0x52 && b[1] == 0x49 && b[2] == 0x58 && b[3] == 0x33) {
            return "rix";
        }
        if (b[0] == 0x23 && b[1] == 0x20 && b[2] == 0x24 && b[3] == 0x49 && b[4] == 0x64 && b[5] == 0x3A
                && b[6] == 0x20) {
            return "sid";
        }
        if (b[0] == 0x41 && b[1] == 0x75 && b[2] == 0x74 && b[3] == 0x6F && b[4] == 0x43 && b[5] == 0x41
                && b[6] == 0x44 && b[7] == 0x20 && b[8] == 0x53 && b[9] == 0x6C && b[10] == 0x69
                && b[11] == 0x64 && b[12] == 0x65) {
            return "sld";
        }
        if (b[0] == 0x53 && b[1] == 0x74 && b[2] == 0x6F && b[3] == 0x72 && b[4] == 0x6D && b[5] == 0x33
                && b[6] == 0x44) {
            return "sod";
        }
        if (b[0] == 0xFA && b[1] == 0xDE && b[2] == 0xBA && b[3] == 0xBE && b[4] == 0x01 && b[5] == 0x01) {
            return "wic";
        }
        if (b[0] == 0xD3 && b[1] == 0x23 && b[2] == 0x00 && b[3] == 0x00 && b[4] == 0x03 && b[5] == 0x00
                && b[6] == 0x00 && b[7] == 0x00) {
            return "wlm";
        }
        if (b[0] == 0xD7 && b[1] == 0xCD && b[2] == 0xC6 && b[3] == 0x9A) {
            return "wmf";
        }
        if (b[0] == 0xFF && b[1] == 0x57 && b[2] == 0x50 && b[3] == 0x43 && b[4] == 0x10) {
            return "wpg";
        }
        if (b[0] == 0x23 && b[1] == 0x56 && b[2] == 0x52 && b[3] == 0x4D && b[4] == 0x4C && b[5] == 0x20
                && b[6] == 0x56 && b[7] == 0x32 && b[8] == 0x2E && b[9] == 0x30) {
            return "wrl";
        }
        if (b[0] == 0x23 && b[1] == 0x64 && b[2] == 0x65 && b[3] == 0x66 && b[4] == 0x69 && b[5] == 0x6E
                && b[6] == 0x65) {
            return "xbm";
        }
        if (b[0] == 0x2F && b[1] == 0x2A && b[2] == 0x20 && b[3] == 0x58 && b[4] == 0x50 && b[5] == 0x4D
                && b[6] == 0x20 && b[7] == 0x2A && b[8] == 0x2F) {
            return "xpm";
        }
        return null;
	}
}
