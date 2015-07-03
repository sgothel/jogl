/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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
package jogamp.graph.font;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.jogamp.opengl.GLException;

import com.jogamp.common.util.IntObjectHashMap;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontSet;
import com.jogamp.graph.font.FontFactory;

public class JavaFontLoader implements FontSet {

    // FIXME: Add cache size to limit memory usage
    private static final IntObjectHashMap fontMap = new IntObjectHashMap();

    private static final FontSet fontLoader = new JavaFontLoader();

    public static FontSet get() {
        return fontLoader;
    }

    final static String availableFontFileNames[] =
    {
        /* 00 */ "LucidaBrightRegular.ttf",
        /* 01 */ "LucidaBrightItalic.ttf",
        /* 02 */ "LucidaBrightDemiBold.ttf",
        /* 03 */ "LucidaBrightDemiItalic.ttf",
        /* 04 */ "LucidaSansRegular.ttf",
        /* 05 */ "LucidaSansDemiBold.ttf",
        /* 06 */ "LucidaTypewriterRegular.ttf",
        /* 07 */ "LucidaTypewriterBold.ttf",
    };

    final String javaFontPath;

    private JavaFontLoader() {
        final String javaHome = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty("java.home");
            }
        });
        if(null != javaHome) {
            javaFontPath = javaHome + "/lib/fonts/";
        } else {
            javaFontPath = null;
        }
    }

    static boolean is(final int bits, final int bit) {
        return 0 != ( bits & bit ) ;
    }

    @Override
    public Font getDefault() throws IOException {
        return get(FAMILY_REGULAR, 0) ; // Sans Serif Regular
    }

    @Override
    public Font get(final int family, final int style) throws IOException {
        if(null == javaFontPath) {
            throw new GLException("java font path undefined");
        }
        Font font = (Font)fontMap.get( ( family << 8 ) | style );
        if (font != null) {
            return font;
        }

        // 1st process Sans Serif (2 fonts)
        if( is(style, STYLE_SERIF) ) {
            if( is(style, STYLE_BOLD) ) {
                font = abspath(availableFontFileNames[5], family, style);
            } else {
                font = abspath(availableFontFileNames[4], family, style);
            }
            if(null != font) {
                fontMap.put( ( family << 8 ) | style, font );
            }
            return font;
        }

        // Serif Fonts ..
        switch (family) {
            case FAMILY_LIGHT:
            case FAMILY_MEDIUM:
            case FAMILY_CONDENSED:
            case FAMILY_REGULAR:
                if( is(style, STYLE_BOLD) ) {
                    if( is(style, STYLE_ITALIC) ) {
                        font = abspath(availableFontFileNames[3], family, style);
                    } else {
                        font = abspath(availableFontFileNames[2], family, style);
                    }
                } else if( is(style, STYLE_ITALIC) ) {
                    font = abspath(availableFontFileNames[1], family, style);
                } else {
                    font = abspath(availableFontFileNames[0], family, style);
                }
                break;

            case FAMILY_MONOSPACED:
                if( is(style, STYLE_BOLD) ) {
                    font = abspath(availableFontFileNames[7], family, style);
                } else {
                    font = abspath(availableFontFileNames[6], family, style);
                }
                break;
        }

        return font;
    }

    Font abspath(final String fname, final int family, final int style) throws IOException {
        try {
            final Font f = abspathImpl(javaFontPath+fname, family, style);
            if(null != f) {
                return f;
            }
            throw new IOException (String.format("Problem loading font %s, file %s%s", fname, javaFontPath, fname));
        } catch (final IOException ioe) {
            throw new IOException(String.format("Problem loading font %s, file %s%s", fname, javaFontPath, fname), ioe);
        }
    }
    private Font abspathImpl(final String fname, final int family, final int style) throws IOException {
        final Exception[] privErr = { null };
        final int[] streamLen = { 0 };
        final InputStream stream = AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
            @Override
            public InputStream run() {
                try {
                    final File file = new File(fname);
                    streamLen[0] = (int) file.length();
                    return new BufferedInputStream(new FileInputStream(file), streamLen[0]);
                } catch (final Exception e) {
                    privErr[0] = e;
                    return null;
                }
            } } );
        if( null != privErr[0] ) {
            throw new IOException(privErr[0]);
        }
        if(null != stream) {
            final Font f= FontFactory.get ( stream, streamLen[0], true ) ;
            if(null != f) {
                fontMap.put( ( family << 8 ) | style, f );
                return f;
            }
        }
        return null;
    }
}
