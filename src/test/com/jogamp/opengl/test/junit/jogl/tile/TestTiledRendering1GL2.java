package com.jogamp.opengl.test.junit.jogl.tile;

import com.jogamp.opengl.test.junit.jogl.demos.gl2.Gears;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.GLPixelBuffer;
import com.jogamp.opengl.util.TileRenderer;
import com.jogamp.opengl.util.GLPixelBuffer.GLPixelAttributes;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

import java.io.File;
import java.io.IOException;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLOffscreenAutoDrawable;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/** Demonstrates the TileRenderer class by rendering a large version
    of the Gears demo to the specified file. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTiledRendering1GL2 extends UITestCase {
    static long duration = 500; // ms

    @Test
    public void test01() throws IOException {
        doTest();
    }
    
    void doTest() throws IOException {      
        GLCapabilities caps = new GLCapabilities(null);
        caps.setDoubleBuffered(false);
    
        // Use a pbuffer for rendering
        final GLDrawableFactory factory = GLDrawableFactory.getFactory(caps.getGLProfile());
        final GLOffscreenAutoDrawable glad = factory.createOffscreenAutoDrawable(null, caps, null, 256, 256, null);
                
        // Fix the image size for now
        final int imageWidth = glad.getWidth() * 6;
        final int imageHeight = glad.getHeight() * 4;
        
        final String filename = this.getSnapshotFilename(0, "-tile", glad.getChosenGLCapabilities(), imageWidth, imageHeight, false, TextureIO.PNG, null);
        final File file = new File(filename);
    
        // Initialize the tile rendering library
        final TileRenderer renderer = new com.jogamp.opengl.util.TileRenderer();
        final TileRenderer.PMVMatrixCallback pmvMatrixCallback = new TileRenderer.PMVMatrixCallback() { 
          public void reshapePMVMatrix(GL _gl, int tileNum, int tileColumn, int tileRow, int tileX, int tileY, int tileWidth, int tileHeight, int imageWidth, int imageHeight) {
            final GL2 gl = _gl.getGL2();
            gl.glMatrixMode( GL2.GL_PROJECTION );
            gl.glLoadIdentity();
            
            /* compute projection parameters */
            float left, right, bottom, top; 
            if( imageHeight > imageWidth ) {
                float a = (float)imageHeight / (float)imageWidth;
                left = -1.0f;
                right = 1.0f;
                bottom = -a;
                top = a;
            } else {
                float a = (float)imageWidth / (float)imageHeight;
                left = -a;
                right = a;
                bottom = -1.0f;
                top = 1.0f;
            }
            final float w = right - left;
            final float h = top - bottom;
            final float l = left + w * tileX / imageWidth;
            final float r = l + w * tileWidth / imageWidth;
            final float b = bottom + h * tileY / imageHeight;
            final float t = b + h * tileHeight / imageHeight;
            
            final float _w = r - l;
            final float _h = t - b;
            System.err.println(">> [l "+left+", r "+right+", b "+bottom+", t "+top+"] "+w+"x"+h+" -> [l "+l+", r "+r+", b "+b+", t "+t+"] "+_w+"x"+_h);
            gl.glFrustum(l, r, b, t, 5.0f, 60.0f);
            
            gl.glMatrixMode(GL2.GL_MODELVIEW);        
          }
        };
        
        renderer.setTileSize(glad.getWidth(), glad.getHeight(), 0);
        renderer.setPMVMatrixCallback(pmvMatrixCallback);
        renderer.setImageSize(imageWidth, imageHeight);
        
        final GLPixelBuffer.GLPixelBufferProvider pixelBufferProvider = GLPixelBuffer.defaultProviderWithRowStride;
        final boolean[] flipVertically = { false };
        
        glad.addGLEventListener( new GLEventListener() {
            @Override
            public void init(GLAutoDrawable drawable) {
                final GL gl = drawable.getGL();
                GLPixelAttributes pixelAttribs = pixelBufferProvider.getAttributes(gl, 3);
                GLPixelBuffer pixelBuffer = pixelBufferProvider.allocate(gl, pixelAttribs, imageWidth, imageHeight, 1, true, 0);
                renderer.setImageBuffer(pixelBuffer);
                if( drawable.isGLOriented() ) {
                    flipVertically[0] = false;
                } else {
                    flipVertically[0] = true;
                }
            }
            @Override
            public void dispose(GLAutoDrawable drawable) {}
            @Override
            public void display(GLAutoDrawable drawable) {
                renderer.beginTile(drawable.getGL().getGL2());
            }
            @Override
            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
        });
        final Gears gears = new Gears();
        gears.setDoRotation(false);
        glad.addGLEventListener( gears );
        glad.addGLEventListener( new GLEventListener() {
            @Override
            public void init(GLAutoDrawable drawable) {}
            @Override
            public void dispose(GLAutoDrawable drawable) {}
            @Override
            public void display(GLAutoDrawable drawable) {
                renderer.endTile(drawable.getGL().getGL2());
            }
            @Override
            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
        });
        
        do {
            glad.display();
        } while ( !renderer.eot() );
    
        glad.destroy();
        
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
