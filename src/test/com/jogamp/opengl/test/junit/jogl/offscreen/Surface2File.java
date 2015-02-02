/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.test.junit.jogl.offscreen;

import com.jogamp.opengl.*;

import com.jogamp.opengl.util.GLReadBufferUtil;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.jogamp.nativewindow.*;

public class Surface2File implements SurfaceUpdatedListener {

    final String filename;
    final boolean alpha;
    final GLReadBufferUtil readBufferUtil;
    int shotNum = 0;

    public Surface2File(final String filename, final boolean alpha) {
        this.filename = filename;
        this.alpha = alpha;
        this.readBufferUtil = new GLReadBufferUtil(alpha, false);
    }

    public void dispose(final GL gl) {
        readBufferUtil.dispose(gl);
    }

    public void surfaceUpdated(final Object updater, final NativeSurface ns, final long when) {
        if (updater instanceof GLDrawable) {
            final GLDrawable drawable = (GLDrawable) updater;
            final GLContext ctx = GLContext.getCurrent();
            if (null != ctx && ctx.getGLDrawable() == drawable) {
                final GL gl = ctx.getGL();
                // FIXME glFinish() is an expensive paranoia sync, should not be necessary due to spec
                gl.glFinish();
                if(readBufferUtil.readPixels(gl, false)) {
                    gl.glFinish();
                    try {
                        surface2File();
                    } catch (final IOException ex) {
                        throw new RuntimeException("can not write survace to file", ex);
                    }
                }
            }
        }
    }

    public void surface2File() throws IOException {
        if (!readBufferUtil.isValid()) {
            return;
        }
        final StringWriter sw = new StringWriter();
        {
            final String pfmt = alpha ? "rgba" : "rgb_" ;
            new PrintWriter(sw).printf("%s-I_%s-%04d.png", filename, pfmt, shotNum);
        }
        final File file = new File(sw.toString());
        readBufferUtil.write(file);
        shotNum++;
    }
}
