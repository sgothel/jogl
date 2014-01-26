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

import com.jogamp.common.os.Platform;
import com.jogamp.common.util.IntObjectHashMap;
import com.jogamp.common.util.IOUtil;
import com.jogamp.common.util.JarUtil;
import com.jogamp.common.util.cache.TempJarCache;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontSet;
import com.jogamp.graph.font.FontFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class UbuntuFontLoader implements FontSet {

    // FIXME: Add cache size to limit memory usage
    private static final IntObjectHashMap fontMap = new IntObjectHashMap();

    private static final String jarSubDir = "atomic/" ;
    private static final String jarName = "jogl-fonts-p0.jar" ;

    private static final String relFontPath = "fonts/ubuntu/" ;
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

    static boolean is(int bits, int bit) {
        return 0 != ( bits & bit ) ;
    }

    @Override
    public Font getDefault() throws IOException {
        return get(FAMILY_REGULAR, 0) ; // Sans Serif Regular
    }

    @Override
    public Font get(int family, int style) throws IOException {
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
    private static boolean useTempJarCache = false;

    private synchronized Font abspath(String fname, int family, int style) throws IOException {
        final String err = "Problem loading font "+fname+", stream "+relFontPath+fname;
        final Exception[] privErr = { null };
        try {
            final Font f0 = abspathImpl(fname, family, style);
            if(null != f0) {
                return f0;
            }
            if( !attemptedJARLoading ) {
                attemptedJARLoading = true;
                Platform.initSingleton();
                if( TempJarCache.isInitialized() ) {
                    final URI uri = JarUtil.getRelativeOf(UbuntuFontLoader.class, jarSubDir, jarName);
                    AccessController.doPrivileged(new PrivilegedAction<Object>() {
                        @Override
                        public Object run() {
                            try {
                                TempJarCache.addResources(UbuntuFontLoader.class, uri);
                            } catch (Exception e) { privErr[0] = e; }
                            return null;
                        } } );
                    if( null == privErr[0] ) {
                        useTempJarCache = true;
                        final Font f1 = abspathImpl(fname, family, style);
                        if(null != f1) {
                            return f1;
                        }
                    }
                }
            }
        } catch(Exception e) {
            throw new IOException(err, e);
        }
        if( null != privErr[0] ) {
            throw new IOException(err, privErr[0]);
        }
        throw new IOException(err);
    }
    private Font abspathImpl(final String fname, final int family, final int style) throws IOException {
        final URLConnection conn;
        if( useTempJarCache ) {
            // this code-path throws .. all exceptions
            final Exception[] privErr = { null };
            final URLConnection[] privConn = { null };
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    try {
                        final URI uri = TempJarCache.getResource(absFontPath+fname);
                        privConn[0] = null != uri ? uri.toURL().openConnection() : null;
                    } catch (Exception e) { privErr[0] = e; }
                    return null;
                } } );
            if( null != privErr[0] ) {
                throw new IOException(privErr[0]);
            }
            conn = privConn[0];
        } else {
            // no exceptions ..
            conn = IOUtil.getResource(UbuntuFontLoader.class, relFontPath+fname);
        }
        if(null != conn) {
            final Font f= FontFactory.get ( conn ) ;
            if(null != f) {
                fontMap.put( ( family << 8 ) | style, f );
                return f;
            }
        }
        return null;
    }
}
