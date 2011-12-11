package com.jogamp.opengl.test.junit.graph;

import java.io.IOException;

import javax.media.nativewindow.NativeWindowFactory;
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.curve.opengl.TextRenderer;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.geom.opengl.SVertex;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.graph.demos.GPUTextRendererListenerBase01;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.glsl.ShaderState;


public class TestTextRendererNEWT01 extends UITestCase {
    static final boolean DEBUG = false;
    static final boolean TRACE = false;

    public static void main(String args[]) throws IOException {
        String tstname = TestTextRendererNEWT01.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }    
        
    static void destroyWindow(GLWindow window) {
        if(null!=window) {
            window.destroy();
        }
    }

    static GLWindow createWindow(String title, GLCapabilitiesImmutable caps, int width, int height) {
        Assert.assertNotNull(caps);

        GLWindow window = GLWindow.create(caps);
        window.setSize(width, height);
        window.setPosition(10, 10);
        window.setTitle(title);
        Assert.assertNotNull(window);
        window.setVisible(true);

        return window;
    }

    @Test
    public void testTextRendererR2T01() throws InterruptedException {
        GLProfile glp = GLProfile.getGL2ES2();
        
        GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);    

        GLWindow window = createWindow("text-vbaa1-msaa0", caps, 800,400);
        RenderState rs = RenderState.createRenderState(new ShaderState(), SVertex.factory());
        TextGLListener textGLListener = new TextGLListener(rs, Region.VBAA_RENDERING_BIT, DEBUG, TRACE);
        textGLListener.attachInputListenerTo(window);
        window.addGLEventListener(textGLListener);
        
        textGLListener.setFontSet(FontFactory.UBUNTU, 0, 0);
        textGLListener.setTech(-400, -30, 0f, -1000, window.getWidth()*2);
        window.display();
        
        textGLListener.setTech(-400, -30, 0, -380, window.getWidth()*3);
        window.display();
        
        textGLListener.setTech(-400, -20, 0, -80, window.getWidth()*4);
        window.display();

        textGLListener.setFontSet(FontFactory.JAVA, 0, 0);
        textGLListener.setTech(-400, -30, 0f, -1000, window.getWidth()*2);
        window.display();
        
        textGLListener.setTech(-400, -30, 0, -380, window.getWidth()*3);
        window.display();
        
        textGLListener.setTech(-400, -20, 0, -80, window.getWidth()*4);
        window.display();
        
        destroyWindow(window); 
    }
    
    @Test
    public void testTextRendererMSAA01() throws InterruptedException {
        GLProfile glp = GLProfile.get(GLProfile.GL2ES2);
        GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);    
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);

        GLWindow window = createWindow("text-vbaa0-msaa1", caps, 800, 400);
        RenderState rs = RenderState.createRenderState(new ShaderState(), SVertex.factory());
        TextGLListener textGLListener = new TextGLListener(rs, 0, DEBUG, TRACE);
        textGLListener.attachInputListenerTo(window);
        window.addGLEventListener(textGLListener);
        
        textGLListener.setFontSet(FontFactory.UBUNTU, 0, 0);
        textGLListener.setTech(-400, -30, 0f, -1000, 0);
        window.display();
        
        textGLListener.setTech(-400, -30, 0, -380, 0);
        window.display();
        
        textGLListener.setTech(-400, -20, 0, -80, 0);
        window.display();
        
        textGLListener.setFontSet(FontFactory.JAVA, 0, 0);
        textGLListener.setTech(-400, -30, 0f, -1000, 0);
        window.display();
        
        textGLListener.setTech(-400, -30, 0, -380, 0);
        window.display();
        
        textGLListener.setTech(-400, -20, 0, -80, 0);
        window.display();
        
        destroyWindow(window); 
    }
    
    private class TextGLListener extends GPUTextRendererListenerBase01 {
        String winTitle;
        
        public TextGLListener(RenderState rs, int type, boolean debug, boolean trace) {
            super(rs, type, debug, trace);
        }
        
        public void attachInputListenerTo(GLWindow window) {
            super.attachInputListenerTo(window);
            winTitle = window.getTitle();
        }
        public void setTech(float xt, float yt, float angle, int zoom, int fboSize){
            setMatrix(xt, yt, angle, zoom, fboSize);       
        }

        public void init(GLAutoDrawable drawable) {
            super.init(drawable);
            
            GL2ES2 gl = drawable.getGL().getGL2ES2();
            gl.setSwapInterval(1);
            gl.glEnable(GL.GL_DEPTH_TEST);
            
            final TextRenderer textRenderer = (TextRenderer) getRenderer();
            
            textRenderer.setAlpha(gl, 1.0f);
            textRenderer.setColorStatic(gl, 0.0f, 0.0f, 0.0f);
        }
        
        public void display(GLAutoDrawable drawable) {
            super.display(drawable);

            try {
                printScreen(drawable, "./", winTitle, false);
            } catch (GLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
