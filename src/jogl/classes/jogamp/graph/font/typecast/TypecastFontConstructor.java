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
package jogamp.graph.font.typecast;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.media.opengl.GLException;

import jogamp.graph.font.FontConstructor;
import jogamp.graph.font.typecast.ot.OTFontCollection;

import com.jogamp.common.util.IOUtil;
import com.jogamp.graph.font.Font;

public class TypecastFontConstructor implements FontConstructor  {

    @Override
    public Font create(final File ffile) throws IOException {
        final Object o = AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                OTFontCollection fontset;
                try {
                    fontset = OTFontCollection.create(ffile);
                    return new TypecastFont(fontset);
                } catch (final IOException e) {
                    return e;
                }
            }
        });
        if(o instanceof Font) {
            return (Font)o;
        }
        if(o instanceof IOException) {
            throw (IOException)o;
        }
        throw new InternalError("Unexpected Object: "+o);
    }

    @Override
    public Font create(final URLConnection fconn) throws IOException {
        return AccessController.doPrivileged(new PrivilegedAction<Font>() {
            @Override
            public Font run() {
                File tf = null;
                int len=0;
                Font f = null;
                try {
                    tf = IOUtil.createTempFile( "jogl.font", ".ttf", false);
                    len = IOUtil.copyURLConn2File(fconn, tf);
                    if(len==0) {
                        tf.delete();
                        throw new GLException("Font of stream "+fconn.getURL()+" was zero bytes");
                    }
                    f = create(tf);
                    tf.delete();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
                return f;
            }
        });
    }

}
