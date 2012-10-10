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
 
package com.jogamp.opengl.test.junit.jogl.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.fixedfunc.GLPointerFunc;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.gl2es1.GLUgl2es1;

import org.junit.Test;

import com.jogamp.common.nio.Buffers;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.GLArrayDataWrapper;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.ImmModeSink;

/**
 * Testing the ImmModeSink w/ GL2ES1 context
 */
public class TestImmModeSinkES1NEWT extends UITestCase {
    static int duration = 100;
    static final int iWidth = 400;
    static final int iHeight = 400;

    static GLCapabilities getCaps(String profile) {
        if( !GLProfile.isAvailable(profile) )  {
            System.err.println("Profile "+profile+" n/a");
            return null;
        }
        return new GLCapabilities(GLProfile.get(profile));
    }
    
    void doTest(GLCapabilitiesImmutable reqGLCaps, GLEventListener demo) throws InterruptedException {
        System.out.println("Requested  GL Caps: "+reqGLCaps);
        
        //
        // Create native windowing resources .. X11/Win/OSX
        // 
        final GLWindow glad = GLWindow.create(reqGLCaps);
        glad.addGLEventListener(demo);
        
        final SnapshotGLEventListener snapshotGLEventListener = new SnapshotGLEventListener();
        glad.addGLEventListener(snapshotGLEventListener);
        glad.setSize(iWidth, iHeight);
        glad.setVisible(true);
        
        snapshotGLEventListener.setMakeSnapshot();
        glad.display(); // initial resize/display
                
        Thread.sleep(duration);
        
        glad.destroy();
    }
    
    static class DemoGL2ES1Plain implements GLEventListener {
        final boolean useArrayData;
        final boolean useVBO;
        final GLU glu;
        
        final float[] vertices = new float[] { 0,          0,       0,
                                               iWidth,     0,       0,
                                               iWidth / 2, iHeight, 0 };
        
        final float[] colors = new float[] { 1, 0, 0, 
                                             0, 1, 0, 
                                             0, 0, 1 };
        
        final ByteBuffer bufferAll;
        final int bufferVOffset, bufferCOffset;
        final int bufferVSize, bufferCSize;
        final FloatBuffer bufferC, bufferV;
        final int[] vboName = new int[] { 0 };
        final GLArrayDataWrapper arrayC, arrayV;
        
        DemoGL2ES1Plain(boolean useArrayData, boolean useVBO) {
            this.useArrayData = useArrayData;
            this.useVBO = useVBO;            
            this.glu = new GLUgl2es1();
            
            bufferAll = Buffers.newDirectByteBuffer( ( colors.length + vertices.length ) * Buffers.SIZEOF_FLOAT );
            
            bufferVOffset = 0;
            bufferVSize = 3*3*GLBuffers.sizeOfGLType(GL.GL_FLOAT);
            bufferCOffset = bufferVSize;
            bufferCSize = 3*3*GLBuffers.sizeOfGLType(GL.GL_FLOAT);
            
            bufferV = (FloatBuffer) GLBuffers.sliceGLBuffer(bufferAll, bufferVOffset, bufferVSize, GL.GL_FLOAT);
            bufferV.put(vertices, 0, vertices.length).rewind();
            bufferC = (FloatBuffer) GLBuffers.sliceGLBuffer(bufferAll, bufferCOffset, bufferCSize, GL.GL_FLOAT);
            bufferC.put(colors, 0, colors.length).rewind();
            
            System.err.println("bufferAll: "+bufferAll+", byteOffset "+Buffers.getDirectBufferByteOffset(bufferAll));
            System.err.println("bufferV: off "+bufferVOffset+", size "+bufferVSize+": "+bufferV+", byteOffset "+Buffers.getDirectBufferByteOffset(bufferV));
            System.err.println("bufferC: off "+bufferCOffset+", size "+bufferCSize+": "+bufferC+", byteOffset "+Buffers.getDirectBufferByteOffset(bufferC));
        
            if(useArrayData) {            
                arrayV = GLArrayDataWrapper.createFixed(GLPointerFunc.GL_VERTEX_ARRAY, 3, GL.GL_FLOAT, false, 0,
                                                        bufferV, 0, bufferVOffset, GL.GL_STATIC_DRAW, GL.GL_ARRAY_BUFFER);
                
                arrayC = GLArrayDataWrapper.createFixed(GLPointerFunc.GL_COLOR_ARRAY, 3, GL.GL_FLOAT, false, 0,
                                                        bufferC, 0, bufferCOffset, GL.GL_STATIC_DRAW, GL.GL_ARRAY_BUFFER);
            } else {
                arrayV = null;
                arrayC = null;                
            }
        }
        
        @Override
        public void init(GLAutoDrawable drawable) {
            GL gl = drawable.getGL();
            System.err.println("GL_VENDOR   "+gl.glGetString(GL.GL_VENDOR));
            System.err.println("GL_RENDERER "+gl.glGetString(GL.GL_RENDERER));
            System.err.println("GL_VERSION  "+gl.glGetString(GL.GL_VERSION));
            if(useVBO) {
                gl.glGenBuffers(1, vboName, 0);
                if(0 == vboName[0]) {
                    throw new GLException("glGenBuffers didn't return valid VBO name");
                }
            }
        }

        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            GL2ES1 gl = drawable.getGL().getGL2ES1();
            
            gl.glMatrixMode( GL2ES1.GL_PROJECTION );
            gl.glLoadIdentity();
    
            // coordinate system origin at lower left with width and height same as the window
            glu.gluOrtho2D( 0.0f, width, 0.0f, height );
    
            gl.glMatrixMode( GL2ES1.GL_MODELVIEW );
            gl.glLoadIdentity();
    
            gl.glViewport( 0, 0, width, height );        
        }
        
        @Override
        public void display(GLAutoDrawable drawable) {
            GL2ES1 gl = drawable.getGL().getGL2ES1();
            
            gl.glClear( GL.GL_COLOR_BUFFER_BIT );
    
            // draw a triangle filling the window
            gl.glLoadIdentity();

            if(useVBO) {
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboName[0]);
                gl.glBufferData(GL.GL_ARRAY_BUFFER, bufferAll.limit(), bufferAll, GL.GL_STATIC_DRAW);
                if(useArrayData) {
                    arrayV.setVBOName(vboName[0]);
                    arrayC.setVBOName(vboName[0]);
                }
            }
            
            gl.glEnableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
            if(useArrayData) {
                gl.glVertexPointer(arrayV);
            } else {
                if(useVBO) {            
                    gl.glVertexPointer(3, GL.GL_FLOAT, 0, bufferVOffset);
                } else {
                    gl.glVertexPointer(3, GL.GL_FLOAT, 0, bufferV);
                }
            }
                        
            gl.glEnableClientState(GLPointerFunc.GL_COLOR_ARRAY);            
            if(useArrayData) {
                gl.glColorPointer(arrayC);
            } else {
                if(useVBO) {            
                    gl.glColorPointer(3, GL.GL_FLOAT, 0, bufferCOffset);
                } else {
                    gl.glColorPointer(3, GL.GL_FLOAT, 0, bufferC);
                }
            }
                        
            if(useVBO) {
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
            }
            
            gl.glDrawArrays(GL.GL_TRIANGLES, 0, 3);
            gl.glFlush();
            
            gl.glDisableClientState(GLPointerFunc.GL_COLOR_ARRAY);
            gl.glDisableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
        }

        @Override
        public void dispose(GLAutoDrawable drawable) {
            GL gl = drawable.getGL();
            if(0 != vboName[0]) {
                gl.glDeleteBuffers(1, vboName, 0);
                vboName[0] = 0;
            }
        }        
    }
    
    static class DemoGL2ES1ImmModeSink implements GLEventListener {
        final ImmModeSink ims;
        final GLU glu;
        
        DemoGL2ES1ImmModeSink(boolean useVBO) {
            ims = ImmModeSink.createFixed(3*3, 
                                          3, GL.GL_FLOAT, // vertex
                                          3, GL.GL_FLOAT, // color
                                          0, GL.GL_FLOAT, // normal
                                          0, GL.GL_FLOAT, // texCoords 
                                          useVBO ? GL.GL_STATIC_DRAW : 0);
            glu = new GLUgl2es1();
        }
        
        @Override
        public void init(GLAutoDrawable drawable) {
            GL gl = drawable.getGL();
            System.err.println("GL_VENDOR   "+gl.glGetString(GL.GL_VENDOR));
            System.err.println("GL_RENDERER "+gl.glGetString(GL.GL_RENDERER));
            System.err.println("GL_VERSION  "+gl.glGetString(GL.GL_VERSION));
        }

        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            GL2ES1 gl = drawable.getGL().getGL2ES1();
            
            gl.glMatrixMode( GL2ES1.GL_PROJECTION );
            gl.glLoadIdentity();
    
            // coordinate system origin at lower left with width and height same as the window
            glu.gluOrtho2D( 0.0f, width, 0.0f, height );
    
            gl.glMatrixMode( GL2ES1.GL_MODELVIEW );
            gl.glLoadIdentity();
    
            gl.glViewport( 0, 0, width, height );        
        }
        
        @Override
        public void display(GLAutoDrawable drawable) {
            GL2ES1 gl = drawable.getGL().getGL2ES1();
            
            gl.glClear( GL.GL_COLOR_BUFFER_BIT );
    
            // draw a triangle filling the window
            gl.glLoadIdentity();
            
            ims.glBegin(GL.GL_TRIANGLES);
            ims.glColor3f( 1, 0, 0 );
            ims.glVertex2f( 0, 0 );
            ims.glColor3f( 0, 1, 0 );
            ims.glVertex2f( iWidth, 0 );
            ims.glColor3f( 0, 0, 1 );
            ims.glVertex2f( iWidth / 2, iHeight );
            ims.glEnd(gl, true);
        }

        @Override
        public void dispose(GLAutoDrawable drawable) {
        }        
    }
    
    @Test
    public void test01Plain__GL2ES1_VBOOffUsePlain() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES1);
        if(null == reqGLCaps) return;
        doTest(reqGLCaps, new DemoGL2ES1Plain(false, false));
    }
    
    @Test
    public void test02Plain__GL2ES1_VBOOffUseArrayData() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES1);
        if(null == reqGLCaps) return;
        doTest(reqGLCaps, new DemoGL2ES1Plain(true, false));
    }
    
    @Test
    public void test03Plain__GL2ES1_VBOOnUsePlain() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES1);
        if(null == reqGLCaps) return;
        doTest(reqGLCaps, new DemoGL2ES1Plain(false, true));
    }
    
    @Test
    public void test04Plain__GL2ES1_VBOOnUseArrayData() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES1);
        if(null == reqGLCaps) return;
        doTest(reqGLCaps, new DemoGL2ES1Plain(true, true));
    }
    
    @Test
    public void test05ImmSinkGL2ES1_VBOOff() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES1);
        if(null == reqGLCaps) return;
        doTest(reqGLCaps, new DemoGL2ES1ImmModeSink(false));
    }
    
    @Test
    public void test06ImmSinkGL2ES1_VBOOn() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES1);
        if(null == reqGLCaps) return;
        doTest(reqGLCaps, new DemoGL2ES1ImmModeSink(true));
    }
        
    public static void main(String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                duration = MiscUtils.atoi(args[++i], duration);
            }
        }
        org.junit.runner.JUnitCore.main(TestImmModeSinkES1NEWT.class.getName());
    }

}
