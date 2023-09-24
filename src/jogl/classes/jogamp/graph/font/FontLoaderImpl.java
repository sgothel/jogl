/**
 * Copyright 2011-2023 JogAmp Community. All rights reserved.
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
import com.jogamp.common.util.SecurityUtil;
import com.jogamp.common.util.cache.TempJarCache;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontSet;
import com.jogamp.graph.font.FontFactory;

import java.security.PrivilegedAction;

public abstract class FontLoaderImpl {

    private static boolean attemptedJARLoading = false;
    private static boolean useTempJARCache = false;

    protected static boolean isOneSet(final int bits, final int bit) {
        return 0 != ( bits & bit ) ;
    }

    protected synchronized Font readFont(final String fname, final Uri.Encoded jarName, final String absFontPath) throws IOException {
        if( !attemptedJARLoading ) {
            attemptedJARLoading = true;
            Platform.initSingleton();
            if( TempJarCache.isInitialized(false) ) {
                try {
                    final Uri uri = JarUtil.getRelativeOf(FontLoaderImpl.class, null, jarName);
                    final Exception e0 = SecurityUtil.doPrivileged(new PrivilegedAction<Exception>() {
                        @Override
                        public Exception run() {
                            try {
                                TempJarCache.addResources(FontLoaderImpl.class, uri);
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
            final Font f = readFontImpl(absFontPath+fname);
            if( null != f ) {
                return f;
            }
            throw new IOException(String.format("Problem loading font %s, stream %s%s", fname, absFontPath, fname));
        } catch(final Exception e) {
            throw new IOException(String.format("Problem loading font %s, stream %s%s", fname, absFontPath, fname), e);
        }
    }
    private Font readFontImpl(final String fname) throws IOException {
        final InputStream stream;
        if( useTempJARCache ) {
            final Exception[] privErr = { null };
            stream = SecurityUtil.doPrivileged(new PrivilegedAction<InputStream>() {
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
            return FontFactory.get ( stream, true ) ;
        }
        return null;
    }
}
