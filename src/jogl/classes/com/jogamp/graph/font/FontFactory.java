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
package com.jogamp.graph.font;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.jogamp.common.net.Uri;
import com.jogamp.common.util.IOUtil;
import com.jogamp.common.util.PropertyAccess;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.common.util.cache.TempJarCache;

import jogamp.graph.font.FontConstructor;
import jogamp.graph.font.JavaFontLoader;
import jogamp.graph.font.UbuntuFontLoader;

/**
 * The optional property <i>jogamp.graph.font.ctor</i>
 * allows user to specify the {@link FontConstructor} implementation.
 * <p>
 * Default {@link FontConstructor} is {@link jogamp.graph.font.typecast.TypecastFontConstructor},
 * i.e. using our internal <i>typecast</i> branch.
 * </p>
 */
public class FontFactory {
    private static final String FontConstructorPropKey = "jogamp.graph.font.ctor";
    private static final String DefaultFontConstructor = "jogamp.graph.font.typecast.TypecastFontConstructor";

    /** Ubuntu is the default font family, {@value} */
    public static final int UBUNTU = 0;

    /** Java fonts are optional, {@value} */
    public static final int JAVA = 1;

    private static final FontConstructor fontConstr;

    static {
        /**
         * For example:
         *   "jogamp.graph.font.typecast.TypecastFontFactory" (default)
         *   "jogamp.graph.font.ttf.TTFFontImpl"
         */
        String fontImplName = PropertyAccess.getProperty(FontConstructorPropKey, true);
        if(null == fontImplName) {
            fontImplName = DefaultFontConstructor;
        }
        fontConstr = (FontConstructor) ReflectionUtil.createInstance(fontImplName, FontFactory.class.getClassLoader());
    }

    public static final FontSet getDefault() {
        return get(UBUNTU);
    }

    public static final FontSet get(final int font) {
        switch (font) {
            case JAVA:
                return JavaFontLoader.get();
            default:
                return UbuntuFontLoader.get();
        }
    }

    /**
     * Creates a Font instance.
     * @param file font file
     * @return the new Font instance
     * @throws IOException
     */
    public static final Font get(final File file) throws IOException {
        return fontConstr.create(file);
    }

    /**
     * Creates a Font instance based on a determinated font stream with its given length
     * of the font segment.
     * <p>
     * No explicit stream copy is performed as in {@link #get(InputStream, boolean)}
     * due to the known {@code streamLen}.
     * </p>
     * @param stream font stream
     * @param streamLen length of the font segment within this font stream
     * @param closeStream {@code true} to close the {@code stream}
     * @return the new Font instance
     * @throws IOException
     */
    public static final Font get(final InputStream stream, final int streamLen, final boolean closeStream) throws IOException {
        try {
            return fontConstr.create(stream, streamLen);
        } finally {
            if( closeStream ) {
                stream.close();
            }
        }
    }

    /**
     * Creates a Font instance based on an undeterminated font stream length.
     * <p>
     * The font stream is temporarily copied into a temp file
     * to gather it's size and to gain random access.
     * The temporary file will be deleted at exit.
     * </p>
     * @param stream dedicated font stream
     * @param closeStream {@code true} to close the {@code stream}
     * @return the new Font instance
     * @throws IOException
     */
    public static final Font get(final InputStream stream, final boolean closeStream) throws IOException {
        final IOException[] ioe = { null };
        final int[] streamLen = { 0 };
        final File tempFile[] = { null };

        final InputStream bis = AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
            @Override
            public InputStream run() {
                InputStream bis = null;
                try {
                    tempFile[0] = IOUtil.createTempFile( "jogl.font", ".ttf", false);
                    streamLen[0] = IOUtil.copyStream2File(stream, tempFile[0], -1);
                    if( 0 == streamLen[0] ) {
                        throw new IOException("Font stream has zero bytes");
                    }
                    bis = new BufferedInputStream(new FileInputStream(tempFile[0]), streamLen[0]);
                } catch (final IOException e) {
                    ioe[0] = e;
                    if( null != tempFile[0] ) {
                        tempFile[0].delete();
                        tempFile[0] = null;
                    }
                    streamLen[0] = 0;
                } finally {
                    if( closeStream ) {
                        IOUtil.close(stream, ioe, System.err);
                    }
                }
                return bis;
            } });
        if( null != ioe[0] ) {
            throw ioe[0];
        }
        if( null == bis ) {
            throw new IOException("Could not cache font stream"); // should not be reached
        }
        try {
            return fontConstr.create(bis, streamLen[0]);
        } finally {
            if( null != bis ) {
                bis.close();
            }
            if( null != tempFile[0] ) {
                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    @Override
                    public Object run() {
                        tempFile[0].delete();
                        return null;
                    } } );
            }
        }
    }

    public static final Font get(final Class<?> context, final String fname, final boolean useTempJarCache) throws IOException {
        InputStream stream = null;
        if( useTempJarCache ) {
            try {
                final Uri uri = TempJarCache.getResourceUri(fname);
                stream = null != uri ? uri.toURL().openConnection().getInputStream() : null;
            } catch (final Exception e) {
                throw new IOException(e);
            }
        } else {
            stream = IOUtil.getResource(fname, context.getClassLoader(), context).getInputStream();
        }
        if( null != stream ) {
            return FontFactory.get ( stream, true ) ;
        }
        return null;
    }

    public static boolean isPrintableChar( final char c ) {
        if( Character.isWhitespace(c) ) {
            return true;
        }
        if( 0 == c || Character.isISOControl(c) ) {
            return false;
        }
        final Character.UnicodeBlock block = Character.UnicodeBlock.of( c );
        return block != null && block != Character.UnicodeBlock.SPECIALS;
    }
}
