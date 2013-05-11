package com.jogamp.opengl.test.junit.jogl.util.texture;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URLConnection;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.common.util.IOUtil;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.TextureDraw01Accessor;
import com.jogamp.opengl.test.junit.jogl.demos.es2.TextureDraw01ES2Listener;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLPixelBuffer.GLPixelAttributes;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.spi.JPEGImage;
import javax.media.opengl.GL;

public class TestJPEGImage01NEWT extends UITestCase {
    
    static boolean showFPS = false;
    static long duration = 100; // ms
    
    public void testImpl(final boolean withAlpha, final InputStream istream) throws InterruptedException, IOException {
        final GLReadBufferUtil screenshot = new GLReadBufferUtil(true, false);
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities(glp);
        if( withAlpha ) {
            caps.setAlphaBits(1);
        }
        
        final JPEGImage image = JPEGImage.read(istream);
        Assert.assertNotNull(image);
        System.err.println("JPEGImage: "+image);
        
        final int internalFormat = (image.getBytesPerPixel()==4)?GL.GL_RGBA:GL.GL_RGB;
        final TextureData texData = new TextureData(glp, internalFormat,
                                       image.getWidth(),
                                       image.getHeight(),
                                       0,
                                       new GLPixelAttributes(image.getGLFormat(), image.getGLType()),
                                       false /* mipmap */,
                                       false /* compressed */,
                                       false /* must flip-vert */,
                                       image.getData(),
                                       null);
        // final TextureData texData = TextureIO.newTextureData(glp, istream, false /* mipmap */, TextureIO.JPG);
        System.err.println("TextureData: "+texData);        
        
        final GLWindow glad = GLWindow.create(caps);
        glad.setTitle("TestJPEGImage01NEWT");
        // Size OpenGL to Video Surface
        glad.setSize(texData.getWidth(), texData.getHeight());
        
        // load texture from file inside current GL context to match the way
        // the bug submitter was doing it
        final GLEventListener gle = new TextureDraw01ES2Listener( texData ) ;
        glad.addGLEventListener(gle);
        glad.addGLEventListener(new GLEventListener() {                    
            boolean shot = false;
            
            @Override public void init(GLAutoDrawable drawable) {}
            
            public void display(GLAutoDrawable drawable) {
                // 1 snapshot
                if(null!=((TextureDraw01Accessor)gle).getTexture() && !shot) {
                    shot = true;
                    snapshot(0, null, drawable.getGL(), screenshot, TextureIO.PNG, null);
                }
            }
            
            @Override public void dispose(GLAutoDrawable drawable) { }
            @Override public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) { }
        });

        Animator animator = new Animator(glad);
        animator.setUpdateFPSFrames(60, showFPS ? System.err : null);
        QuitAdapter quitAdapter = new QuitAdapter();
        glad.addKeyListener(quitAdapter);
        glad.addWindowListener(quitAdapter);
        glad.setVisible(true);
        animator.start();

        while(!quitAdapter.shouldQuit() && animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }
        
        animator.stop();
        glad.destroy();
    }
    
    @Test
    public void testReadES2_RGB() throws InterruptedException, IOException, MalformedURLException {
        final String fname = null == _fname ? "test-ntscN_3-01-160x90-90pct-yuv444-base.jpg" : _fname;
        final URLConnection urlConn = IOUtil.getResource(this.getClass(), fname);
        testImpl(false, urlConn.getInputStream());
    }

    static String _fname = null;
    public static void main(String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-file")) {
                i++;
                _fname = args[i];
            }
        }
        org.junit.runner.JUnitCore.main(TestJPEGImage01NEWT.class.getName());
    }
}
