/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.test.junit.jogl.awt;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
 
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLProfile;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * Tests for bug 461, a failure of GLDrawableFactory.createGLPbuffer() on Windows
 * when the stencil buffer is turned on.
 *
 * @author Wade Walker (from code sample provided by Owen Dimond)
 */
public class TestBug461OffscreenSupersamplingSwingAWT extends UITestCase implements GLEventListener {
    JFrame jframe;
    GLPbuffer offScreenBuffer;
    
    private void render(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        Assert.assertNotNull(gl);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);      

        // draw a triangle filling the window
        gl.glBegin(GL.GL_TRIANGLES);
        gl.glColor3f(1, 0, 0);
        gl.glVertex2d(-1, -1);
        gl.glColor3f(0, 1, 0);
        gl.glVertex2d(0, 1);
        gl.glColor3f(0, 0, 1);
        gl.glVertex2d(1, -1);
        gl.glEnd();
    }
    
    /* @Override */
    public void init(GLAutoDrawable drawable) {                        
    }

    /* @Override */
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    }    
    
    /* @Override */
    public void display(GLAutoDrawable drawable) {             
        render(offScreenBuffer);
        BufferedImage outputImage = com.jogamp.opengl.util.awt.Screenshot.readToBufferedImage(200, 200, false);        
        Assert.assertNotNull(outputImage);
        ImageIcon imageIcon = new ImageIcon(outputImage);
        JLabel imageLabel = new JLabel(imageIcon);        
        jframe.getContentPane().add(imageLabel);
    }

    /* @Override */
    public void dispose(GLAutoDrawable drawable) {  
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    jframe.setVisible(false);
                    jframe.dispose();
                }});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testOffscreenSupersampling() throws InterruptedException, InvocationTargetException {
        jframe = new JFrame("Offscreen Supersampling");
        Assert.assertNotNull(jframe);
        jframe.setSize( 300, 300);
        jframe.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        GLProfile glp = GLProfile.get(GLProfile.GL2);
        Assert.assertNotNull(glp);
        
        GLDrawableFactory fac = GLDrawableFactory.getFactory(glp);
        Assert.assertNotNull(fac);
        
        Assert.assertTrue( fac.canCreateGLPbuffer(GLProfile.getDefaultDesktopDevice()) );
        
        GLCapabilities glCap = new GLCapabilities(glp);
        Assert.assertNotNull(glCap);
        
        // COMMENTING OUT THIS LINE FIXES THE ISSUE. 
        // Setting this in JOGL1 works. Thus this is a JOGL2 issue.
        glCap.setSampleBuffers(true);
      
        // Without line below, there is an error on Windows.
        glCap.setDoubleBuffered(false);
        // Needed for drop shadows
        glCap.setStencilBits(1);

        //makes a new buffer
        offScreenBuffer = fac.createGLPbuffer(GLProfile.getDefaultDesktopDevice(), glCap, null, 200, 200, null);
        Assert.assertNotNull(offScreenBuffer);
        offScreenBuffer.addGLEventListener(this);        
        offScreenBuffer.display();
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                jframe.setVisible(true);
            }});
    }

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(TestBug461OffscreenSupersamplingSwingAWT.class.getName());
    }
}

