package com.jogamp.opengl.test.junit.graph.demos;

import java.io.File;
import java.io.IOException;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;

import com.jogamp.opengl.util.texture.TextureIO;

public class Screenshot {

    ReadBufferUtil readBufferUtil = new ReadBufferUtil();

    public void dispose() {
        readBufferUtil.dispose();
    }

    public void surface2File(GLAutoDrawable drawable, String filename) {
        GL gl = drawable.getGL();
        // FIXME glFinish() is an expensive paranoia sync, should not be necessary due to spec
        gl.glFinish();
        readBufferUtil.fetchOffscreenTexture(drawable, gl);
        gl.glFinish();
        try {
            surface2File(filename);
        } catch (IOException ex) {
            throw new RuntimeException("can not write survace to file", ex);
        }
    }

    void surface2File(String filename) throws IOException {
        File file = new File(filename);
        TextureIO.write(readBufferUtil.getTextureData(), file);
        System.err.println("Wrote: " + file.getAbsolutePath() + ", ...");
        readBufferUtil.rewindPixelBuffer();
    }
    
}
