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

import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.TileRendererBase;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLUniformData;

public class RedSquareES2 implements GLEventListener, TileRendererBase.TileRendererListener {
    private ShaderState st;
    private PMVMatrix pmvMatrix;
    private GLUniformData pmvMatrixUniform;
    private GLArrayDataServer vertices ;
    private GLArrayDataServer colors ;
    private long t0;
    private int swapInterval = 0;
    private float aspect = 1.0f;
    private boolean doRotate = true;
    private boolean clearBuffers = true;
    private TileRendererBase tileRendererInUse = null;
    private boolean doRotateBeforePrinting;

    public RedSquareES2(int swapInterval) {
        this.swapInterval = swapInterval;
    }

    public RedSquareES2() {
        this.swapInterval = 1;
    }
        
    @Override
    public void addTileRendererNotify(TileRendererBase tr) {
        tileRendererInUse = tr;
        doRotateBeforePrinting = doRotate;
        setDoRotation(false);      
    }
    @Override
    public void removeTileRendererNotify(TileRendererBase tr) {
        tileRendererInUse = null;
        setDoRotation(doRotateBeforePrinting);      
    }
    @Override
    public void startTileRendering(TileRendererBase tr) {
        System.err.println("RedSquareES2.startTileRendering: "+tr);
    }
    @Override
    public void endTileRendering(TileRendererBase tr) {
        System.err.println("RedSquareES2.endTileRendering: "+tr);
    }
    
    public void setAspect(float aspect) { this.aspect = aspect; }
    public void setDoRotation(boolean rotate) { this.doRotate = rotate; }
    public void setClearBuffers(boolean v) { clearBuffers = v; }
    
    @Override
    public void init(GLAutoDrawable glad) {
        System.err.println(Thread.currentThread()+" RedSquareES2.init: tileRendererInUse "+tileRendererInUse);
        final GL2ES2 gl = glad.getGL().getGL2ES2();
        
        System.err.println("RedSquareES2 init on "+Thread.currentThread());
        System.err.println("Chosen GLCapabilities: " + glad.getChosenGLCapabilities());
        System.err.println("INIT GL IS: " + gl.getClass().getName());
        System.err.println(JoglVersion.getGLStrings(gl, null, false).toString());
        if( !gl.hasGLSL() ) {
            System.err.println("No GLSL available, no rendering.");
            return;
        }
        st = new ShaderState();
        st.setVerbose(true);
        final ShaderCode vp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, this.getClass(), "shader",
                "shader/bin", "RedSquareShader", true);
        final ShaderCode fp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, this.getClass(), "shader",
                "shader/bin", "RedSquareShader", true);
        vp0.defaultShaderCustomization(gl, true, true);
        fp0.defaultShaderCustomization(gl, true, true);
        final ShaderProgram sp0 = new ShaderProgram();
        sp0.add(gl, vp0, System.err);
        sp0.add(gl, fp0, System.err);
        st.attachShaderProgram(gl, sp0, true);
        
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
        gl.glEnable(GL2ES2.GL_DEPTH_TEST);
        st.useProgram(gl, false);        

        t0 = System.currentTimeMillis();
        System.err.println(Thread.currentThread()+" RedSquareES2.init FIN");
    }

    @Override
    public void display(GLAutoDrawable glad) {
        long t1 = System.currentTimeMillis();

        final GL2ES2 gl = glad.getGL().getGL2ES2();
        if( clearBuffers ) {
            if( null != tileRendererInUse ) {
              gl.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
            } else {
                gl.glClearColor(0, 0, 0, 0);
            }
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        }
        if( !gl.hasGLSL() ) {
            return;
        }
        st.useProgram(gl, true);
        // One rotation every four seconds
        pmvMatrix.glMatrixMode(PMVMatrix.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, -10);
        if(doRotate) {
            float ang = ((float) (t1 - t0) * 360.0F) / 4000.0F;
            pmvMatrix.glRotatef(ang, 0, 0, 1);
            pmvMatrix.glRotatef(ang, 0, 1, 0);
        }
        st.uniform(gl, pmvMatrixUniform);        

        // Draw a square
        vertices.enableBuffer(gl, true);
        colors.enableBuffer(gl, true);
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
        vertices.enableBuffer(gl, false);
        colors.enableBuffer(gl, false);
        st.useProgram(gl, false);
    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int width, int height) {
        final GL2ES2 gl = glad.getGL().getGL2ES2();
        if(-1 != swapInterval) {        
            gl.setSwapInterval(swapInterval);
        }
        reshapeImpl(gl, x, y, width, height, width, height);
    }
    
    @Override
    public void reshapeTile(TileRendererBase tr,
                            int tileX, int tileY, int tileWidth, int tileHeight, 
                            int imageWidth, int imageHeight) {
        final GL2ES2 gl = tr.getAttachedDrawable().getGL().getGL2ES2();
        gl.setSwapInterval(0);
        reshapeImpl(gl, tileX, tileY, tileWidth, tileHeight, imageWidth, imageHeight);
    }
    
    void reshapeImpl(GL2ES2 gl, int tileX, int tileY, int tileWidth, int tileHeight, int imageWidth, int imageHeight) {
        System.err.println(Thread.currentThread()+" RedSquareES2.reshape "+tileX+"/"+tileY+" "+tileWidth+"x"+tileHeight+" of "+imageWidth+"x"+imageHeight+", swapInterval "+swapInterval+", drawable 0x"+Long.toHexString(gl.getContext().getGLDrawable().getHandle())+", tileRendererInUse "+tileRendererInUse);
        // Thread.dumpStack();
        if( !gl.hasGLSL() ) {
            return;
        }
        
        st.useProgram(gl, true);
        // Set location in front of camera
        pmvMatrix.glMatrixMode(PMVMatrix.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        
        // compute projection parameters 'normal' perspective
        final float fovy=45f;
        final float aspect2 = ( (float) imageWidth / (float) imageHeight ) / aspect;
        final float zNear=1f;
        final float zFar=100f;
        
        // compute projection parameters 'normal' frustum
        final float top=(float)Math.tan(fovy*((float)Math.PI)/360.0f)*zNear;
        final float bottom=-1.0f*top;
        final float left=aspect2*bottom;
        final float right=aspect2*top;
        final float w = right - left;
        final float h = top - bottom;
        
        // compute projection parameters 'tiled'
        final float l = left + tileX * w / imageWidth;
        final float r = l + tileWidth * w / imageWidth;
        final float b = bottom + tileY * h / imageHeight;
        final float t = b + tileHeight * h / imageHeight;
        
        pmvMatrix.glFrustumf(l, r, b, t, zNear, zFar);
        //pmvMatrix.glOrthof(-4.0f, 4.0f, -4.0f, 4.0f, 1.0f, 100.0f);
        st.uniform(gl, pmvMatrixUniform);
        st.useProgram(gl, false);
        
        System.err.println(Thread.currentThread()+" RedSquareES2.reshape FIN");
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
        System.err.println(Thread.currentThread()+" RedSquareES2.dispose: tileRendererInUse "+tileRendererInUse);
        final GL2ES2 gl = glad.getGL().getGL2ES2();
        if( !gl.hasGLSL() ) {
            return;
        }
        st.destroy(gl);
        st = null;
        pmvMatrix.destroy();
        pmvMatrix = null;
        System.err.println(Thread.currentThread()+" RedSquareES2.dispose FIN");
    }    
}
