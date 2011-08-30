/**
 * Copyright (C) 2011 JogAmp Community. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * BRIAN PAUL BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.jogamp.opengl.test.junit.jogl.demos.es2;

import com.jogamp.common.nio.Buffers;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.opengl.test.junit.jogl.demos.GearsObject;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import java.nio.FloatBuffer;
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLUniformData;

/**
 * GearsES2.java <BR>
 * author: Brian Paul (converted to Java by Ron Cemer and Sven Gothel) <P>
 */
public class GearsES2 implements GLEventListener {
    private final FloatBuffer lightPos = Buffers.newDirectFloatBuffer( new float[] { 5.0f, 5.0f, 10.0f } );
    
    private ShaderState st = null;
    private PMVMatrix pmvMatrix = null;
    private GLUniformData pmvMatrixUniform = null;
    private GLUniformData colorU = null;
    private float view_rotx = 20.0f, view_roty = 30.0f, view_rotz = 0.0f;
    private GearsObjectES2 gear1=null, gear2=null, gear3=null;
    private float angle = 0.0f;
    private int swapInterval = 0;

    private int prevMouseX, prevMouseY;

    public GearsES2(int swapInterval) {
        this.swapInterval = swapInterval;
    }

    public GearsES2() {
        this.swapInterval = 1;
    }

    public void setGears(GearsObjectES2 g1, GearsObjectES2 g2, GearsObjectES2 g3) {
        gear1 = g1;
        gear2 = g2;
        gear3 = g3;
    }

    /**
     * @return gear1
     */
    public GearsObjectES2 getGear1() { return gear1; }

    /**
     * @return gear2
     */
    public GearsObjectES2 getGear2() { return gear2; }

    /**
     * @return gear3
     */
    public GearsObjectES2 getGear3() { return gear3; }


    public void init(GLAutoDrawable drawable) {
        System.err.println("Gears: Init: "+drawable);
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        System.err.println("Chosen GLCapabilities: " + drawable.getChosenGLCapabilities());
        System.err.println("INIT GL IS: " + gl.getClass().getName());
        System.err.println("GL_VENDOR: " + gl.glGetString(GL.GL_VENDOR));
        System.err.println("GL_RENDERER: " + gl.glGetString(GL.GL_RENDERER));
        System.err.println("GL_VERSION: " + gl.glGetString(GL.GL_VERSION));

        gl.glEnable(GL.GL_CULL_FACE);
        gl.glEnable(GL.GL_DEPTH_TEST);
        
        st = new ShaderState();
        st.setVerbose(true);
        final ShaderCode vp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, 1, this.getClass(),
                "shader", "shader/bin", "gears");
        final ShaderCode fp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, this.getClass(),
                "shader", "shader/bin", "gears");
        final ShaderProgram sp0 = new ShaderProgram();
        sp0.add(gl, vp0, System.err);
        sp0.add(gl, fp0, System.err);
        st.attachShaderProgram(gl, sp0);
        st.useProgram(gl, true);
        // Use debug pipeline
        // drawable.setGL(new DebugGL(drawable.getGL()));

        pmvMatrix = new PMVMatrix();
        st.attachObject("pmvMatrix", pmvMatrix);
        pmvMatrixUniform = new GLUniformData("pmvMatrix", 4, 4, pmvMatrix.glGetPMvMvitMatrixf()); // P, Mv, Mvi and Mvit
        st.ownUniform(pmvMatrixUniform);
        st.uniform(gl, pmvMatrixUniform);

        GLUniformData lightU = new GLUniformData("lightPos", 3, lightPos);
        st.ownUniform(lightU);
        st.uniform(gl, lightU);

        colorU = new GLUniformData("color", 4, GearsObject.red);
        st.ownUniform(colorU);
        st.uniform(gl, colorU);

        if(null == gear1) {
            gear1 = new GearsObjectES2(1.0f, 4.0f, 1.0f, 20, 0.7f, pmvMatrix, pmvMatrixUniform, colorU);
            System.err.println("gear1 created: "+gear1);
        } else {
            gear1 = new GearsObjectES2(gear1, pmvMatrix, pmvMatrixUniform, colorU);
            System.err.println("gear1 reused: "+gear1);
        }
                    
        if(null == gear2) {
            gear2 = new GearsObjectES2(0.5f, 2.0f, 2.0f, 10, 0.7f, pmvMatrix, pmvMatrixUniform, colorU);
            System.err.println("gear2 created: "+gear2);
        } else {
            gear2 = new GearsObjectES2(gear2, pmvMatrix, pmvMatrixUniform, colorU);
            System.err.println("gear2 reused: "+gear2);
        }
                
        if(null == gear3) {
            gear3 = new GearsObjectES2(1.3f, 2.0f, 0.5f, 10, 0.7f, pmvMatrix, pmvMatrixUniform, colorU);
            System.err.println("gear3 created: "+gear3);
        } else {
            gear3 = new GearsObjectES2(gear3, pmvMatrix, pmvMatrixUniform, colorU);
            System.err.println("gear3 reused: "+gear3);
        }                
        
        // MouseListener gearsMouse = new TraceMouseAdapter(new GearsMouseAdapter());
        MouseListener gearsMouse = new GearsMouseAdapter();    
        KeyListener gearsKeys = new GearsKeyAdapter();

        if (drawable instanceof Window) {
            Window window = (Window) drawable;
            window.addMouseListener(gearsMouse);
            window.addKeyListener(gearsKeys);
        } else if (GLProfile.isAWTAvailable() && drawable instanceof java.awt.Component) {
            java.awt.Component comp = (java.awt.Component) drawable;
            new com.jogamp.newt.event.awt.AWTMouseAdapter(gearsMouse).addTo(comp);
            new com.jogamp.newt.event.awt.AWTKeyAdapter(gearsKeys).addTo(comp);
        }
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        System.err.println("Gears: Reshape "+x+"/"+y+" "+width+"x"+height+", swapInterval "+swapInterval);
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.setSwapInterval(swapInterval);

        float h = (float)height / (float)width;

        pmvMatrix.glMatrixMode(PMVMatrix.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glFrustumf(-1.0f, 1.0f, -h, h, 5.0f, 60.0f);
        pmvMatrix.glMatrixMode(PMVMatrix.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0.0f, 0.0f, -40.0f);
        st.uniform(gl, pmvMatrixUniform);
    }

    public void dispose(GLAutoDrawable drawable) {
        System.err.println("Gears: Dispose");
        // GL2ES2 gl = drawable.getGL().getGL2ES2();
        // st.useProgram(gl, false);
        // st.destroy(gl);
    }

    public void display(GLAutoDrawable drawable) {
        // Turn the gears' teeth
        angle += 2.0f;

        // Get the GL corresponding to the drawable we are animating
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        // Special handling for the case where the GLJPanel is translucent
        // and wants to be composited with other Java 2D content
        if (GLProfile.isAWTAvailable() && 
            (drawable instanceof javax.media.opengl.awt.GLJPanel) &&
            !((javax.media.opengl.awt.GLJPanel) drawable).isOpaque() &&
            ((javax.media.opengl.awt.GLJPanel) drawable).shouldPreserveColorBufferIfTranslucent()) {
          gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
        } else {
          gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        }

        pmvMatrix.glPushMatrix();
        pmvMatrix.glRotatef(view_rotx, 1.0f, 0.0f, 0.0f);
        pmvMatrix.glRotatef(view_roty, 0.0f, 1.0f, 0.0f);
        pmvMatrix.glRotatef(view_rotz, 0.0f, 0.0f, 1.0f);

        gear1.draw(gl, -3.0f, -2.0f,  1f * angle -    0f, GearsObject.red);
        gear2.draw(gl,  3.1f, -2.0f, -2f * angle -  9.0f, GearsObject.green);
        gear3.draw(gl, -3.1f,  4.2f, -2f * angle - 25.0f, GearsObject.blue);    
        pmvMatrix.glPopMatrix();
    }

    class GearsKeyAdapter extends KeyAdapter {      
        public void keyPressed(KeyEvent e) {
            int kc = e.getKeyCode();
            if(KeyEvent.VK_LEFT == kc) {
                view_roty -= 1;
            } else if(KeyEvent.VK_RIGHT == kc) {
                view_roty += 1;
            } else if(KeyEvent.VK_UP == kc) {
                view_rotx -= 1;
            } else if(KeyEvent.VK_DOWN == kc) {
                view_rotx += 1;
            }
        }
    }

    class GearsMouseAdapter extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            prevMouseX = e.getX();
            prevMouseY = e.getY();
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseDragged(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            int width=0, height=0;
            Object source = e.getSource();
            if(source instanceof Window) {
                Window window = (Window) source;
                width=window.getWidth();
                height=window.getHeight();
            } else if (GLProfile.isAWTAvailable() && source instanceof java.awt.Component) {
                java.awt.Component comp = (java.awt.Component) source;
                width=comp.getWidth();
                height=comp.getHeight();
            } else {
                throw new RuntimeException("Event source neither Window nor Component: "+source);
            }
            float thetaY = 360.0f * ( (float)(x-prevMouseX)/(float)width);
            float thetaX = 360.0f * ( (float)(prevMouseY-y)/(float)height);

            prevMouseX = x;
            prevMouseY = y;

            view_rotx += thetaX;
            view_roty += thetaY;
        }
    }
}
