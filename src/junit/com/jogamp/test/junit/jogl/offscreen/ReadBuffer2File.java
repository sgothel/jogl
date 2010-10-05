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
 
package com.jogamp.test.junit.jogl.offscreen;

import java.io.IOException;
import javax.media.opengl.*;

import com.jogamp.opengl.util.texture.TextureIO;
import java.io.File;

public class ReadBuffer2File extends ReadBufferBase {

    public ReadBuffer2File(GLDrawable externalRead) {
        super(externalRead);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        super.dispose(drawable);
    }
    int shotNum = 0;

    void copyTextureData2File() throws IOException {
        if (!readBufferUtil.isValid()) {
            return;
        }

        File file = File.createTempFile("shot" + shotNum + "-", ".ppm");
        TextureIO.write(readBufferUtil.getTextureData(), file);
        System.out.println("Wrote: " + file.getAbsolutePath() + ", ...");
        shotNum++;
        readBufferUtil.rewindPixelBuffer();
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        super.display(drawable);
        try {
            copyTextureData2File();
        } catch (IOException ex) {
            throw new RuntimeException("can not read buffer to file", ex);
        }
    }
}
