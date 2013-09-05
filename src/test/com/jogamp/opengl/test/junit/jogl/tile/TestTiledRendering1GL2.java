/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.test.junit.jogl.tile;

import com.jogamp.opengl.test.junit.jogl.demos.gl2.Gears;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.GLPixelBuffer;
import com.jogamp.opengl.util.TileRenderer;
import com.jogamp.opengl.util.GLPixelBuffer.GLPixelAttributes;
import com.jogamp.opengl.util.TileRendererBase;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

import java.io.File;
import java.io.IOException;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Demos offscreen {@link GLDrawable} being used for
 * {@link TileRenderer} rendering to produce a PNG file.
 * <p>
 * All {@link TileRenderer} operations are 
 * being performed from the main thread sequentially
 * without {@link GLAutoDrawable} or {@link GLEventListener}. 
 * </p>
*/
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTiledRendering1GL2 extends UITestCase {
    static long duration = 500; // ms

    @Test
    public void test01() throws IOException {
        doTest();
    }
    
    static class DrawableContext {
        DrawableContext(GLDrawable d, GLContext glc) {
            this.d = d;
            this.glc = glc;
        }
        GLDrawable d;
        GLContext glc;
    }
    
    private static DrawableContext createDrawableAndCurrentCtx(GLCapabilities glCaps, int width, int height) {
        GLDrawableFactory factory = GLDrawableFactory.getFactory(glCaps.getGLProfile());
        GLDrawable d = factory.createOffscreenDrawable(null, glCaps, null, width, height);
        d.setRealized(true);
        GLContext glc = null;
        glc = d.createContext(null);
        Assert.assertTrue("Context could not be made current", GLContext.CONTEXT_NOT_CURRENT < glc.makeCurrent());
        return new DrawableContext(d, glc);
    }
    
    private static void destroyDrawableContext(DrawableContext dc) {
        if(null != dc.glc) {
            dc.glc.destroy();
            dc.glc = null;
        }
        if(null != dc.d) {
            dc.d.setRealized(false);
            dc.d = null;
        }
    }
    
    void doTest() throws GLException, IOException {
        GLProfile glp = GLProfile.getMaxFixedFunc(true);
        GLCapabilities caps = new GLCapabilities(glp);
        caps.setOnscreen(false);
        
        DrawableContext dc = createDrawableAndCurrentCtx(caps, 256, 256);
        final GL2 gl = dc.glc.getGL().getGL2();
        
        // Fix the image size for now
        final int imageWidth = dc.d.getWidth() * 6;
        final int imageHeight = dc.d.getHeight() * 4;
        
        final String filename = this.getSnapshotFilename(0, "-tile", dc.d.getChosenGLCapabilities(), imageWidth, imageHeight, false, TextureIO.PNG, null);
        final File file = new File(filename);
    
        // Initialize the tile rendering library
        final TileRenderer renderer = new com.jogamp.opengl.util.TileRenderer();        
        renderer.setTileSize(dc.d.getWidth(), dc.d.getHeight(), 0);
        renderer.setImageSize(imageWidth, imageHeight);
        
        final GLPixelBuffer.GLPixelBufferProvider pixelBufferProvider = GLPixelBuffer.defaultProviderWithRowStride;
        final boolean[] flipVertically = { false };
        
        GLPixelAttributes pixelAttribs = pixelBufferProvider.getAttributes(gl, 3);
        GLPixelBuffer pixelBuffer = pixelBufferProvider.allocate(gl, pixelAttribs, imageWidth, imageHeight, 1, true, 0);
        renderer.setImageBuffer(pixelBuffer);
        flipVertically[0] = false;
        
        final Gears gears = new Gears();
        gears.setDoRotation(false);
        gears.init(gl);

        gears.setTileRenderer(renderer);
        do { 
            renderer.beginTile(dc.glc.getGL().getGL2ES3());
            gears.reshape(gl, 0, 0, renderer.getParam(TileRendererBase.TR_CURRENT_TILE_WIDTH), renderer.getParam(TileRendererBase.TR_CURRENT_TILE_HEIGHT));
            gears.display(gl);
            renderer.endTile(dc.glc.getGL().getGL2ES3());
        } while ( !renderer.eot() );
        gears.setTileRenderer(null);

        destroyDrawableContext(dc);
        
        final GLPixelBuffer imageBuffer = renderer.getImageBuffer();
        final TextureData textureData = new TextureData(
                               caps.getGLProfile(),
                               0 /* internalFormat */,
                               imageWidth, imageHeight,
                               0, 
                               imageBuffer.pixelAttributes,
                               false, false, 
                               flipVertically[0],
                               imageBuffer.buffer,
                               null /* Flusher */);
        
        TextureIO.write(textureData, file);
    }
    
    public static void main(String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
        org.junit.runner.JUnitCore.main(TestTiledRendering1GL2.class.getName());
    }    
}
