package com.jogamp.opengl.test.junit.graph;

import java.io.IOException;

import javax.media.nativewindow.NativeWindowFactory;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.geom.opengl.SVertex;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.graph.demos.GPURegionGLListener01;
import com.jogamp.opengl.test.junit.graph.demos.GPURegionGLListener02;
import com.jogamp.opengl.test.junit.graph.demos.GPURegionRendererListenerBase01;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.glsl.ShaderState;


public class TestRegionRendererNEWT01 extends UITestCase {

    public static void main(String args[]) throws IOException {
        String tstname = TestRegionRendererNEWT01.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }    
    
    @BeforeClass
    public static void initClass() {
        GLProfile.initSingleton(true);
        NativeWindowFactory.initSingleton(true);
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
    public void testRegionRendererR2T01() throws InterruptedException {
        GLProfile glp = GLProfile.getGL2ES2();
        
        GLCapabilities caps = new GLCapabilities(glp);
        //caps.setOnscreen(false);
        caps.setAlphaBits(4);    

        GLWindow window = createWindow("shape-vbaa1-msaa0", caps, 800,400);
        RenderState rs = RenderState.createRenderState(new ShaderState(), SVertex.factory());
        GPURegionGLListener02  demo02Listener = new GPURegionGLListener02 (rs, Region.TWO_PASS_RENDERING_BIT, 1140, false, false); 
        demo02Listener.attachInputListenerTo(window);                
        window.addGLEventListener(demo02Listener);        
        
        RegionGLListener listener = new RegionGLListener(demo02Listener, window.getTitle(), "GPURegionNewtDemo02");
        window.addGLEventListener(listener);
        
        listener.setTech(-20, 00, 0f, -300, 400);
        window.display();
        
        listener.setTech(-20, 00, 0f, -150, 800);
        window.display();
        
        listener.setTech(-20, 00, 0f, -50, 1000);
        window.display();

        destroyWindow(window); 
    }
    
    @Test
    public void testRegionRendererMSAA01() throws InterruptedException {
        GLProfile glp = GLProfile.get(GLProfile.GL2ES2);
        GLCapabilities caps = new GLCapabilities(glp);
    //    caps.setOnscreen(false);
        caps.setAlphaBits(4);    
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);

        GLWindow window = createWindow("shape-vbaa0-msaa1", caps, 800, 400);
        RenderState rs = RenderState.createRenderState(new ShaderState(), SVertex.factory());

        GPURegionGLListener01 demo01Listener = new GPURegionGLListener01 (rs, 0, 0, false, false);
        demo01Listener.attachInputListenerTo(window);        
        window.addGLEventListener(demo01Listener);
                
        RegionGLListener listener = new RegionGLListener(demo01Listener, window.getTitle(), "GPURegion01");
        window.addGLEventListener(listener);
        
        listener.setTech(-20, 00, 0f, -300, 400);
        window.display();
        
        listener.setTech(-20, 00, 0f, -150, 800);
        window.display();
        
        listener.setTech(-20, 00, 0f, -50, 1000);
        window.display();
        
        destroyWindow(window); 
    }
    
    @Test
    public void testRegionRendererMSAA02() throws InterruptedException {
        GLProfile glp = GLProfile.get(GLProfile.GL2ES2);
        GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);    
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);

        GLWindow window = createWindow("shape-vbaa0-msaa1", caps, 800, 400);
        RenderState rs = RenderState.createRenderState(new ShaderState(), SVertex.factory());

        GPURegionGLListener01 demo01Listener = new GPURegionGLListener01 (rs, Region.VARIABLE_CURVE_WEIGHT_BIT, 0, false, false);
        demo01Listener.attachInputListenerTo(window);        
        window.addGLEventListener(demo01Listener);
                
        RegionGLListener listener = new RegionGLListener(demo01Listener, window.getTitle(), "GPURegion02");
        window.addGLEventListener(listener);
        
        listener.setTech(-20, 00, 0f, -300, 400);
        window.display();
        
        listener.setTech(-20, 00, 0f, -150, 800);
        window.display();
        
        listener.setTech(-20, 00, 0f, -50, 1000);
        window.display();
        
        destroyWindow(window); 
    }
    
    private class RegionGLListener implements GLEventListener {
        String winTitle;
        String name;
        GPURegionRendererListenerBase01 impl;
        
        public RegionGLListener(GPURegionRendererListenerBase01 impl, String title, String name) {
            this.impl = impl;
            this.winTitle = title;
            this.name = name;
        }
        
        public void setTech(float xt, float yt, float angle, int zoom, int fboSize){
            impl.setMatrix(xt, yt, angle, zoom, fboSize);       
        }

        public void init(GLAutoDrawable drawable) {
            impl.init(drawable);
        }
        
        public void display(GLAutoDrawable drawable) {
            impl.display(drawable);

            try {
                impl.printScreen(drawable, "./", winTitle, name, false);
            } catch (GLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void dispose(GLAutoDrawable drawable) {
            impl.dispose(drawable);
            
        }

        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            impl.reshape(drawable, x, y, width, height);
            
        }
    }
}
