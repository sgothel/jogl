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

import javax.media.opengl.*;

import com.jogamp.opengl.util.texture.TextureIO;

import java.io.File;
import java.io.IOException;

import javax.media.nativewindow.*;

public class Surface2File implements SurfaceUpdatedListener {

    ReadBufferUtil readBufferUtil = new ReadBufferUtil();
    int shotNum = 0;

    public void dispose(GL gl) {
        readBufferUtil.dispose(gl);
    }

    public void surfaceUpdated(Object updater, NativeSurface ns, long when) {
        if (updater instanceof GLDrawable) {
            GLDrawable drawable = (GLDrawable) updater;
            GLContext ctx = GLContext.getCurrent();
            if (null != ctx && ctx.getGLDrawable() == drawable) {
                GL gl = ctx.getGL();
                // FIXME glFinish() is an expensive paranoia sync, should not be necessary due to spec
                gl.glFinish();
                readBufferUtil.fetchOffscreenTexture(drawable, gl);
                gl.glFinish();
                try {
                    surface2File("shot");
                } catch (IOException ex) {
                    throw new RuntimeException("can not write survace to file", ex);
                }
            }
        }
    }

    public void surface2File(String basename) throws IOException {
        if (!readBufferUtil.isValid()) {
            return;
        }

        File file = File.createTempFile(basename + shotNum + "-", ".ppm");
        TextureIO.write(readBufferUtil.getTextureData(), file);
        System.err.println("Wrote: " + file.getAbsolutePath() + ", ...");
        shotNum++;
        readBufferUtil.rewindPixelBuffer();
    }
}
