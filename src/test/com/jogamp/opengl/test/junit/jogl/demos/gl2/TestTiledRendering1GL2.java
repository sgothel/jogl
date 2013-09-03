package com.jogamp.opengl.test.junit.jogl.demos.gl2;

import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.util.TGAWriter;
import com.jogamp.opengl.util.TileRenderer;
import com.jogamp.opengl.util.awt.ImageUtil;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLOffscreenAutoDrawable;

/** Demonstrates the TileRenderer class by rendering a large version
    of the Gears demo to the specified file. */

public class TestTiledRendering1GL2 {

  public static void main(String[] args) throws IOException {
      
    if (args.length != 1) {
      System.out.println("Usage: java TiledRendering [output file name]");
      System.out.println("Writes output (a large version of the Gears demo) to");
      System.out.println("the specified file, using either ImageIO or the fast TGA writer");
      System.out.println("depending on the file extension.");
      System.exit(1);
    }

    String filename = args[0];
    File file = new File(filename);

    GLCapabilities caps = new GLCapabilities(null);
    caps.setDoubleBuffered(false);

    if (!GLDrawableFactory.getFactory(caps.getGLProfile()).canCreateGLPbuffer(null, caps.getGLProfile())) {
      System.out.println("Demo requires pbuffer support");
      System.exit(1);
    }

    // Use a pbuffer for rendering
    final GLDrawableFactory factory = GLDrawableFactory.getFactory(caps.getGLProfile());
    final GLOffscreenAutoDrawable glad = factory.createOffscreenAutoDrawable(null, caps, null, 256, 256, null);
            
    // Fix the image size for now
    // final int imageWidth = glad.getWidth() * 16;
    // final int imageHeight = glad.getHeight() * 12;
    final int imageWidth = glad.getWidth() * 2;
    final int imageHeight = glad.getHeight() * 2;
    
    System.err.println("XXX1: "+imageWidth+"x"+imageHeight);
    System.err.println("XXX1: "+glad);
    
    // Figure out the file format
    TGAWriter tga = null;
    BufferedImage img = null;
    ByteBuffer buf = null;
    
    if (filename.endsWith(".tga")) {
      tga = new TGAWriter();
      tga.open(file,
               imageWidth,
               imageHeight,
               false);
      buf = tga.getImageData();
    } else {
      img = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_3BYTE_BGR);
      buf = ByteBuffer.wrap(((DataBufferByte) img.getRaster().getDataBuffer()).getData());
    }

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
    renderer.setImageBuffer(GL2.GL_BGR, GL.GL_UNSIGNED_BYTE, buf);

    glad.addGLEventListener( new GLEventListener() {
        @Override
        public void init(GLAutoDrawable drawable) {
            System.err.println("XXX2: "+drawable);
        }
        @Override
        public void dispose(GLAutoDrawable drawable) {
        }
        @Override
        public void display(GLAutoDrawable drawable) {
            renderer.beginTile(drawable.getGL().getGL2());
        }
        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        }
    });
    final Gears gears = new Gears();
    gears.setDoRotation(false);
    glad.addGLEventListener( gears );
    glad.addGLEventListener( new GLEventListener() {
        @Override
        public void init(GLAutoDrawable drawable) {
        }
        @Override
        public void dispose(GLAutoDrawable drawable) {
        }
        @Override
        public void display(GLAutoDrawable drawable) {
            renderer.endTile(drawable.getGL().getGL2());
        }
        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        }
    });
    
    do {
        glad.display();
    } while ( !renderer.eot() );

    glad.destroy();
    
    // Close things up and/or write image using ImageIO
    if (tga != null) {
      tga.close();
    } else {
      ImageUtil.flipImageVertically(img);
      if (!ImageIO.write(img, IOUtil.getFileSuffix(file), file)) {
        System.err.println("Error writing file using ImageIO (unsupported file format?)");
      }
    }
  }
}
