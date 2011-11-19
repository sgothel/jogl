/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.test.junit.jogl.demos.es2;

import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLRunnable;
import javax.media.opengl.GLUniformData;

public class RedSquareES2 implements GLEventListener {
    ShaderState st;
    PMVMatrix pmvMatrix;
    GLUniformData pmvMatrixUniform;
    GLArrayDataServer vertices ;
    GLArrayDataServer colors ;
    long t0;
    private int swapInterval = 0;
    MyMouseAdapter myMouse = new MyMouseAdapter();
    GLWindow glWindow = null;

    public RedSquareES2(int swapInterval) {
        this.swapInterval = swapInterval;
    }

    public RedSquareES2() {
        this.swapInterval = 1;
    }
        
    public void init(GLAutoDrawable glad) {
        System.err.println(Thread.currentThread()+" RedSquareES2.init ...");
        GL2ES2 gl = glad.getGL().getGL2ES2();
        
        System.err.println(Thread.currentThread()+"Chosen GLCapabilities: " + glad.getChosenGLCapabilities());
        System.err.println(Thread.currentThread()+"INIT GL IS: " + gl.getClass().getName());
        System.err.println(Thread.currentThread()+"GL_VENDOR: " + gl.glGetString(GL.GL_VENDOR));
        System.err.println(Thread.currentThread()+"GL_RENDERER: " + gl.glGetString(GL.GL_RENDERER));
        System.err.println(Thread.currentThread()+"GL_VERSION: " + gl.glGetString(GL.GL_VERSION));
        
        System.err.println(Thread.currentThread()+" GL Profile: "+gl.getGLProfile());
        System.err.println(Thread.currentThread()+" GL:" + gl);
        System.err.println(Thread.currentThread()+" GL_VERSION=" + gl.glGetString(GL.GL_VERSION));
        
        st = new ShaderState();
        st.setVerbose(true);
        final ShaderCode vp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, 1, this.getClass(),
                "shader", "shader/bin", "RedSquareShader");
        final ShaderCode fp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, this.getClass(),
                "shader", "shader/bin", "RedSquareShader");
        final ShaderProgram sp0 = new ShaderProgram();
        sp0.add(gl, vp0, System.err);
        sp0.add(gl, fp0, System.err);
        st.attachShaderProgram(gl, sp0);
        st.useProgram(gl, true);        
        
        // setup mgl_PMVMatrix
        pmvMatrix = new PMVMatrix();
        pmvMatrix.glMatrixMode(PMVMatrix.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glMatrixMode(PMVMatrix.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();       
        pmvMatrixUniform = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf()); // P, Mv
        st.ownUniform(pmvMatrixUniform);
        st.uniform(gl, pmvMatrixUniform);        
        
        // Allocate Vertex Array
        vertices = GLArrayDataServer.createGLSL("mgl_Vertex", 3, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW);
        vertices.putf(-2); vertices.putf( 2); vertices.putf( 0);
        vertices.putf( 2); vertices.putf( 2); vertices.putf( 0);
        vertices.putf(-2); vertices.putf(-2); vertices.putf( 0);
        vertices.putf( 2); vertices.putf(-2); vertices.putf( 0);
        vertices.seal(gl, true);
        st.ownAttribute(vertices, true);
        vertices.enableBuffer(gl, false);
        
        // Allocate Color Array
        colors= GLArrayDataServer.createGLSL("mgl_Color", 4, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW);
        colors.putf(1); colors.putf(0); colors.putf(0); colors.putf(1);
        colors.putf(0); colors.putf(0); colors.putf(1); colors.putf(1);
        colors.putf(1); colors.putf(0); colors.putf(0); colors.putf(1);
        colors.putf(1); colors.putf(0); colors.putf(0); colors.putf(1);
        colors.seal(gl, true);          
        st.ownAttribute(colors, true);
        colors.enableBuffer(gl, false);
        
        // OpenGL Render Settings
        gl.glClearColor(0, 0, 0, 1);
        gl.glEnable(GL2ES2.GL_DEPTH_TEST);
        st.useProgram(gl, false);        

        if (glad instanceof GLWindow) {
            glWindow = (GLWindow) glad;
            glWindow.addMouseListener(myMouse);
        }
        t0 = System.currentTimeMillis();
        System.err.println(Thread.currentThread()+" RedSquareES2.init FIN");
    }

    public void display(GLAutoDrawable glad) {
        long t1 = System.currentTimeMillis();

        GL2ES2 gl = glad.getGL().getGL2ES2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        st.useProgram(gl, true);
        // One rotation every four seconds
        pmvMatrix.glMatrixMode(PMVMatrix.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, -10);
        float ang = ((float) (t1 - t0) * 360.0F) / 4000.0F;
        pmvMatrix.glRotatef(ang, 0, 0, 1);
        pmvMatrix.glRotatef(ang, 0, 1, 0);
        st.uniform(gl, pmvMatrixUniform);        

        // Draw a square
        vertices.enableBuffer(gl, true);
        colors.enableBuffer(gl, true);
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
        vertices.enableBuffer(gl, false);
        colors.enableBuffer(gl, false);
        st.useProgram(gl, false);
    }

    public void enableAndroidTrace(boolean v) {
        useAndroidDebug = v;
    }
    
    public void reshape(GLAutoDrawable glad, int x, int y, int width, int height) {
        System.err.println(Thread.currentThread()+" RedSquareES2.reshape "+x+"/"+y+" "+width+"x"+height+", swapInterval "+swapInterval);        
        GL2ES2 gl = glad.getGL().getGL2ES2();
        
        st.useProgram(gl, true);
        // Set location in front of camera
        pmvMatrix.glMatrixMode(PMVMatrix.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.gluPerspective(45.0F, (float) width / (float) height, 1.0F, 100.0F);
        //pmvMatrix.glOrthof(-4.0f, 4.0f, -4.0f, 4.0f, 1.0f, 100.0f);
        st.uniform(gl, pmvMatrixUniform);
        st.useProgram(gl, false);
        
        if(useAndroidDebug) {
            try {
                android.os.Debug.startMethodTracing("RedSquareES2.trace");
                // android.os.Debug.startAllocCounting();
                useAndroidDebug = true;
            } catch (NoClassDefFoundError e) { useAndroidDebug=false; }
        }
        
        System.err.println(Thread.currentThread()+" RedSquareES2.reshape FIN");
    }
    private boolean useAndroidDebug = false;

    public void dispose(GLAutoDrawable glad) {
        if(useAndroidDebug) {
            // android.os.Debug.stopAllocCounting();
            android.os.Debug.stopMethodTracing();
        }
        System.err.println(Thread.currentThread()+" RedSquareES2.dispose ... ");
        if (null != glWindow) {
            glWindow.removeMouseListener(myMouse);
            glWindow = null;            
        }
        GL2ES2 gl = glad.getGL().getGL2ES2();
        st.destroy(gl);
        st = null;
        pmvMatrix.destroy();
        pmvMatrix = null;
        System.err.println(Thread.currentThread()+" RedSquareES2.dispose FIN");
    }
    
    class MyMouseAdapter extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            System.err.println(e);
            if(null != glWindow && e.getSource() == glWindow.getDelegatedWindow()) {
                if(e.getX() < glWindow.getWidth()/2) {
                    glWindow.setFullscreen(!glWindow.isFullscreen());
                    System.err.println("setFullscreen: "+glWindow.isFullscreen());
                } else { 
                    glWindow.invoke(false, new GLRunnable() {
                        public boolean run(GLAutoDrawable drawable) {
                            GL gl = drawable.getGL();
                            gl.setSwapInterval(gl.getSwapInterval()<=0?1:0);
                            System.err.println("setSwapInterval: "+gl.getSwapInterval());
                            final GLAnimatorControl a = drawable.getAnimator();
                            if( null != a ) {
                                a.resetFPSCounter();
                            }
                            return true;
                        }
                    });
                }                
            }
        }
     }
}
