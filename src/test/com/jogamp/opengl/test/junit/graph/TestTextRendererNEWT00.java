/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.test.junit.graph;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLRunnable;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.curve.opengl.TextRenderer;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.geom.opengl.SVertex;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.glsl.ShaderState;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTextRendererNEWT00 extends UITestCase {
    static final boolean DEBUG = false;
    static final boolean TRACE = false;
    static long duration = 100; // ms
    
    static final int[] texSize = new int[] { 0 }; 
    static final int fontSize = 24;
    static Font font;

    @BeforeClass
    public static void setup() throws IOException {
        font = FontFactory.get(FontFactory.UBUNTU).getDefault();
    }
    
    static int atoi(String a) {
        try {
            return Integer.parseInt(a);
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }
    
    public static void main(String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = atoi(args[i]);
            }
        }
        String tstname = TestTextRendererNEWT00.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }    
        
    static void sleep() {
        try {
            System.err.println("** new frame ** (sleep: "+duration+"ms)");
            Thread.sleep(duration);
        } catch (InterruptedException ie) {}
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
    public void testTextRendererMSAA01() throws InterruptedException {
        GLProfile glp = GLProfile.get(GLProfile.GL2ES2);
        GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);    
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);
        System.err.println("Requested: "+caps);

        GLWindow window = createWindow("text-vbaa0-msaa1", caps, 800, 400);
        window.display();
        System.err.println("Chosen: "+window.getChosenGLCapabilities());
        
        final RenderState rs = RenderState.createRenderState(new ShaderState(), SVertex.factory());
        final TextRendererListener textGLListener = new TextRendererListener(rs);
        final TextRenderer renderer = textGLListener.getRenderer();
        window.addGLEventListener(textGLListener);

        window.invoke(true, new GLRunnable() {
            @Override
            public boolean run(GLAutoDrawable drawable) {
                int c=0;
                renderString(drawable, renderer, "GlueGen", c++, -1, -1000);
                renderString(drawable, renderer, "JOAL", c++, -1, -1000);
                renderString(drawable, renderer, "JOGL", c++, -1, -1000);
                renderString(drawable, renderer, "JOCL", c++, -1, -1000);
                try {
                    textGLListener.printScreen(drawable, "./", "TestTextRendererNEWT00-snap"+screenshot_num, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }            
        });
        sleep();            

        destroyWindow(window); 
    }    
    int screenshot_num = 0;
    
    int lastRow = -1;
    
    void renderString(GLAutoDrawable drawable, TextRenderer renderer, String text, int column, int row, int z0) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        final int height = drawable.getHeight();
        
        int dx = 0;
        int dy = height;        
        if(0>row) {
            row = lastRow + 1;
        }
        AABBox textBox = font.getStringBounds(text, fontSize);
        dx += font.getAdvanceWidth('X', fontSize) * column;
        dy -= (int)textBox.getHeight() * ( row + 1 );
        renderer.resetModelview(null);
        renderer.translate(gl, dx, dy, z0);
        renderer.drawString3D(gl, font, text, fontSize, texSize);
        
        lastRow = row;
    }
        
    public class TextRendererListener implements GLEventListener {
        private GLReadBufferUtil screenshot;
        private TextRenderer renderer;
        
        public TextRendererListener(RenderState rs) {
            this.screenshot = new GLReadBufferUtil(false, false);
            this.renderer = TextRenderer.create(rs, 0);
        }
    
        public final TextRenderer getRenderer() { return renderer; }
        
        public void printScreen(GLAutoDrawable drawable, String dir, String objName, boolean exportAlpha) throws GLException, IOException {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.printf("%s-%03dx%03d-T%04d", objName, drawable.getWidth(), drawable.getHeight(), texSize[0]);
            
            final String filename = dir + sw +".png";
            if(screenshot.readPixels(drawable.getGL(), false)) {
                screenshot.write(new File(filename));
            }
        }
        
        public void init(GLAutoDrawable drawable) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();
            gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
            renderer.init(gl);
            renderer.setAlpha(gl, 1.0f);
            renderer.setColorStatic(gl, 0.0f, 0.0f, 0.0f);        
        }
        
        public void reshape(GLAutoDrawable drawable, int xstart, int ystart, int width, int height) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();
            
            gl.glViewport(xstart, ystart, width, height);        
            // renderer.reshapePerspective(gl, 45.0f, width, height, 0.1f, 1000.0f);
            renderer.reshapeOrtho(gl, width, height, 0.1f, 1000.0f);
        }
    
        public void display(GLAutoDrawable drawable) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();            
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
            
            renderString(drawable, renderer, "012345678901234567890123456789", 0,  0, -1000);
            renderString(drawable, renderer, "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX", 0, -1, -1000);
            renderString(drawable, renderer, "Hello World", 0, -1, -1000);
            renderString(drawable, renderer, "4567890123456", 4, -1, -1000);
            renderString(drawable, renderer, "I like JogAmp", 4, -1, -1000);
        }        
    
        public void dispose(GLAutoDrawable drawable) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();            
            screenshot.dispose(gl);
            renderer.destroy(gl);
        }            
    }

}
