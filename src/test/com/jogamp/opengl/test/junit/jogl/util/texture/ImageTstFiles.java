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
package com.jogamp.opengl.test.junit.jogl.util.texture;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;

import com.jogamp.common.util.IOUtil;

public class ImageTstFiles {
    public static final String[] pngFileNames = new String[] {
            "bug724-transparent-grey_gimpexp.png",
            "bug724-transparent-grey_orig.png",
            "cross-grey-alpha-16x16.png",
            "grayscale_texture.png",
            "pointer-grey-alpha-16x24.png",
            "test-ntscI_3-01-160x90.png",
            "test-ntscI_4-01-160x90.png",
            "test-ntscIG3-01-160x90.png",
            "test-ntscIG4-01-160x90.png",
            "test-ntscN_3-01-160x90.png",
            "test-ntscN_4-01-160x90.png",
            "test-ntscNG4-01-160x90.png",
            "test-ntscP_3-01-160x90.png",
            "test-ntscP_4-01-160x90.png"
            };

    public static final String[] jpgFileNames = new String[] {
            "bug745_qttdef_post_frame.jpg",
            "darwin_03_N_4-YCCK-640x452.jpg", // local
            "darwin_03_N_4-YCCK.jpg",         // local
            "j1-baseline.jpg",
            "j2-progressive.jpg",
            "j3-baseline_gray.jpg",
            "test-cmyk-01.jpg",
            "test-ntscN_3-01-160x90-60pct-yuv422h-base.jpg",
            "test-ntscN_3-01-160x90-60pct-yuv422h-prog.jpg",
            "test-ntscN_3-01-160x90-90pct-yuv444-base.jpg",
            "test-ntscN_3-01-160x90-90pct-yuv444-prog.jpg",
            "test-ycck-01.jpg" };

    public static final String[] tgaFileNames = new String[] {
            "bug744-rle32.tga",
            "bug982.rle32.256x256.tga",
            "test-u32.tga"
    };
    public static final String[] ddsFileNames = new String[] {
            "test-64x32_DXT1.dds",
            "test-64x32_DXT5.dds",
            "test-64x32_uncompressed.dds"
    };

    public static class NamedInputStream {
        final String fullPath;
        final String basePath;
        final InputStream stream;
        public NamedInputStream(final String fullPath, final String basePath, final InputStream stream) {
            this.fullPath = fullPath;
            this.basePath = basePath;
            this.stream = stream;
        }
    }
    public ArrayList<NamedInputStream> pngStreams;
    public ArrayList<NamedInputStream> jpgStreams;
    public ArrayList<NamedInputStream> tgaStreams;
    public ArrayList<NamedInputStream> ddsStreams;
    public ArrayList<NamedInputStream> allStreams;

    private final ArrayList<NamedInputStream> init(final String[] source) throws IOException {
        final ArrayList<NamedInputStream> sink = new ArrayList<NamedInputStream>();
        for(int i=0; i<source.length; i++) {
            final URLConnection testTextureUrlConn = IOUtil.getResource(source[i], this.getClass().getClassLoader(), this.getClass());
            if( null != testTextureUrlConn ) {
                final InputStream s = testTextureUrlConn.getInputStream();
                if( null != s ) {
                    sink.add(new NamedInputStream(testTextureUrlConn.getURL().toString(), source[i], s));
                }
            }
        }
        return sink;
    }

    public void init() throws IOException {
        pngStreams = init(pngFileNames);
        jpgStreams = init(jpgFileNames);
        tgaStreams = init(tgaFileNames);
        ddsStreams = init(ddsFileNames);
        allStreams = new ArrayList<NamedInputStream>();
        allStreams.addAll(pngStreams);
        allStreams.addAll(jpgStreams);
        allStreams.addAll(tgaStreams);
        allStreams.addAll(ddsStreams);
    }
    public void clear() {
        pngStreams.clear();
        jpgStreams.clear();
        tgaStreams.clear();
        ddsStreams.clear();
        allStreams.clear();
    }
}
