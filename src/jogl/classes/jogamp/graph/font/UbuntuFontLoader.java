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

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import com.jogamp.common.net.Uri;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.IOUtil;
import com.jogamp.common.util.IntObjectHashMap;
import com.jogamp.common.util.JarUtil;
import com.jogamp.common.util.cache.TempJarCache;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontSet;
import com.jogamp.graph.font.FontFactory;

import java.security.AccessController;
import java.security.PrivilegedAction;

public class UbuntuFontLoader implements FontSet {

    // FIXME: Add cache size to limit memory usage
    private static final IntObjectHashMap fontMap = new IntObjectHashMap();

    private static final Uri.Encoded jarSubDir = Uri.Encoded.cast("atomic/");
    private static final Uri.Encoded jarName = Uri.Encoded.cast("jogl-fonts-p0.jar");

    private static final String absFontPath = "jogamp/graph/font/fonts/ubuntu/" ;

    private static final FontSet fontLoader = new UbuntuFontLoader();

    public static final FontSet get() {
        return fontLoader;
    }

    final static String availableFontFileNames[] =
    {
        /* 00 */ "Ubuntu-R.ttf",   // regular
        /* 01 */ "Ubuntu-RI.ttf",  // regular italic
        /* 02 */ "Ubuntu-B.ttf",   // bold
        /* 03 */ "Ubuntu-BI.ttf",  // bold italic
        /* 04 */ "Ubuntu-L.ttf",   // light
        /* 05 */ "Ubuntu-LI.ttf",  // light italic
        /* 06 */ "Ubuntu-M.ttf",   // medium
        /* 07 */ "Ubuntu-MI.ttf",  // medium italic

    };

    private UbuntuFontLoader() {
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
        Font font = (Font)fontMap.get( ( family << 8 ) | style );
        if (font != null) {
            return font;
        }

        switch (family) {
            case FAMILY_MONOSPACED:
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

            case FAMILY_LIGHT:
                if( is(style, STYLE_ITALIC) ) {
                    font = abspath(availableFontFileNames[5], family, style);
                } else {
                    font = abspath(availableFontFileNames[4], family, style);
                }
                break;

            case FAMILY_MEDIUM:
                if( is(style, STYLE_ITALIC) ) {
                    font = abspath(availableFontFileNames[6], family, style);
                } else {
                    font = abspath(availableFontFileNames[7], family, style);
                }
                break;
        }

        return font;
    }

    private static boolean attemptedJARLoading = false;
    private static boolean useTempJARCache = false;

    private synchronized Font abspath(final String fname, final int family, final int style) throws IOException {
        if( !attemptedJARLoading ) {
            attemptedJARLoading = true;
            Platform.initSingleton();
            if( TempJarCache.isInitialized() ) {
                try {
                    final Uri uri = JarUtil.getRelativeOf(UbuntuFontLoader.class, jarSubDir, jarName);
                    final Exception e0 = AccessController.doPrivileged(new PrivilegedAction<Exception>() {
                        @Override
                        public Exception run() {
                            try {
                                TempJarCache.addResources(UbuntuFontLoader.class, uri);
                                useTempJARCache = true;
                                return null;
                            } catch (final Exception e) {
                                return e;
                            }
                        } } );
                    if( null != e0 ) {
                        throw e0;
                    }
                } catch(final Exception e1) {
                    System.err.println("Caught "+e1.getMessage());
                    e1.printStackTrace();
                }
            }
        }
        try {
            final Font f = abspathImpl(absFontPath+fname, family, style);
            if( null != f ) {
                return f;
            }
            throw new IOException(String.format("Problem loading font %s, stream %s%s", fname, absFontPath, fname));
        } catch(final Exception e) {
            throw new IOException(String.format("Problem loading font %s, stream %s%s", fname, absFontPath, fname), e);
        }
    }
    private Font abspathImpl(final String fname, final int family, final int style) throws IOException {
        final InputStream stream;
        if( useTempJARCache ) {
            final Exception[] privErr = { null };
            stream = AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
                @Override
                public InputStream run() {
                    try {
                        final Uri uri = TempJarCache.getResourceUri(fname);
                        return null != uri ? uri.toURL().openConnection().getInputStream() : null;
                    } catch (final Exception e) {
                        privErr[0] = e;
                        return null;
                    }
                } } );
            if( null != privErr[0] ) {
                throw new IOException(privErr[0]);
            }
        } else {
            final URLConnection urlConn = IOUtil.getResource(fname, getClass().getClassLoader(), null);
            stream = null != urlConn ? urlConn.getInputStream() : null;
        }
        if(null != stream) {
            final Font f= FontFactory.get ( stream, true ) ;
            if(null != f) {
                fontMap.put( ( family << 8 ) | style, f );
                return f;
            }
        }
        return null;
    }
}
