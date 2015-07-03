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
package com.jogamp.opengl.test.junit.jogl.glsl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.FloatBuffer;

import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRulerNEWT01 extends UITestCase {
    static long durationPerTest = 500; // ms
    static float[] reqSurfacePixelScale = new float[] { ScalableSurface.AUTOMAX_PIXELSCALE, ScalableSurface.AUTOMAX_PIXELSCALE };
    static boolean manualTest = false;

    private void setTitle(final Window win) {
        final float[] sDPI = win.getPixelsPerMM(new float[2]);
        sDPI[0] *= 25.4f;
        sDPI[1] *= 25.4f;
        win.setTitle("GLWindow: win: "+win.getBounds()+", pix: "+win.getSurfaceWidth()+"x"+win.getSurfaceHeight()+", sDPI "+sDPI[0]+" x "+sDPI[1]);
    }

    private void runTestGL() throws InterruptedException {
        final GLWindow glWindow = GLWindow.create(new GLCapabilities(GLProfile.getGL2ES2()));
        Assert.assertNotNull(glWindow);
        glWindow.setSurfaceScale(reqSurfacePixelScale);
        final float[] valReqSurfacePixelScale = glWindow.getRequestedSurfaceScale(new float[2]);
        glWindow.setSize(640, 480);

        glWindow.addGLEventListener(new GLEventListener() {
            final ShaderState st = new ShaderState();
            final PMVMatrix pmvMatrix = new PMVMatrix();
            final GLUniformData pmvMatrixUniform = new GLUniformData("gcu_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf());
            final GLArrayDataServer vertices0 = GLArrayDataServer.createGLSL("gca_Vertices", 3, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW);
            final GLUniformData rulerPixFreq = new GLUniformData("gcu_RulerPixFreq", 2, Buffers.newDirectFloatBuffer(2));

            @Override
            public void init(final GLAutoDrawable drawable) {
                final GL2ES2 gl = drawable.getGL().getGL2ES2();

                Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

                final ShaderCode vp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, RedSquareES2.class, "shader",
                        "shader/bin", "default", true);
                final ShaderCode fp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, RedSquareES2.class, "shader",
                        "shader/bin", "ruler", true);
                vp0.defaultShaderCustomization(gl, true, true);
                fp0.defaultShaderCustomization(gl, true, true);

                final ShaderProgram sp0 = new ShaderProgram();
                sp0.add(gl, vp0, System.err);
                sp0.add(gl, fp0, System.err);
                Assert.assertTrue(0 != sp0.program());
                Assert.assertTrue(!sp0.inUse());
                Assert.assertTrue(!sp0.linked());
                Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

                st.attachShaderProgram(gl, sp0, true);

                Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
                st.ownUniform(pmvMatrixUniform);
                st.uniform(gl, pmvMatrixUniform);
                Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

                final GLUniformData rulerColor= new GLUniformData("gcu_RulerColor", 3, Buffers.newDirectFloatBuffer(3));
                final FloatBuffer rulerColorV = (FloatBuffer) rulerColor.getBuffer();
                rulerColorV.put(0, 0.5f);
                rulerColorV.put(1, 0.5f);
                rulerColorV.put(2, 0.5f);
                st.ownUniform(rulerColor);
                st.uniform(gl, rulerColor);
                Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

                st.ownUniform(rulerPixFreq);

                vertices0.putf(0); vertices0.putf(1);  vertices0.putf(0);
                vertices0.putf(1);  vertices0.putf(1);  vertices0.putf(0);
                vertices0.putf(0); vertices0.putf(0); vertices0.putf(0);
                vertices0.putf(1);  vertices0.putf(0); vertices0.putf(0);
                vertices0.seal(gl, true);
                st.ownAttribute(vertices0, true);

                // misc GL setup
                gl.glClearColor(1, 1, 1, 1);
                gl.glEnable(GL.GL_DEPTH_TEST);
                Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
            }

            @Override
            public void dispose(final GLAutoDrawable drawable) {
            }

            @Override
            public void display(final GLAutoDrawable drawable) {
                final GL2ES2 gl = drawable.getGL().getGL2ES2();
                vertices0.enableBuffer(gl, true);
                gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
                gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
                vertices0.enableBuffer(gl, false);
            }

            @Override
            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
                final GL2ES2 gl = drawable.getGL().getGL2ES2();
                pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
                pmvMatrix.glLoadIdentity();
                pmvMatrix.glOrthof(0f, 1f, 0f, 1f, -10f, 10f);
                // pmvMatrix.gluPerspective(45.0F, (float) drawable.getWidth() / (float) drawable.getHeight(), 1.0F, 100.0F);
                pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
                pmvMatrix.glLoadIdentity();
                // pmvMatrix.glTranslatef(0, 0, -6);
                // pmvMatrix.glRotatef(45f, 1f, 0f, 0f);
                st.uniform(gl, pmvMatrixUniform);

                final float[] ppmmStore = glWindow.getPixelsPerMM(new float[2]);
                final FloatBuffer rulerPixFreqV = (FloatBuffer) rulerPixFreq.getBuffer();
                rulerPixFreqV.put(0, ppmmStore[0] * 10.0f);
                rulerPixFreqV.put(1, ppmmStore[1] * 10.0f);
                st.uniform(gl, rulerPixFreq);
                System.err.println("Screen pixel/cm "+rulerPixFreqV.get(0)+", "+rulerPixFreqV.get(1));
            }

        });
        final SnapshotGLEventListener snap = new SnapshotGLEventListener();
        glWindow.addGLEventListener(snap);
        glWindow.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if( e.isAutoRepeat() ) {
                    return;
                }
                if(e.getKeyChar()=='x') {
                    final float[] hadSurfacePixelScale = glWindow.getCurrentSurfaceScale(new float[2]);
                    final float[] reqSurfacePixelScale;
                    if( hadSurfacePixelScale[0] == ScalableSurface.IDENTITY_PIXELSCALE ) {
                        reqSurfacePixelScale = new float[] { ScalableSurface.AUTOMAX_PIXELSCALE, ScalableSurface.AUTOMAX_PIXELSCALE };
                    } else {
                        reqSurfacePixelScale = new float[] { ScalableSurface.IDENTITY_PIXELSCALE, ScalableSurface.IDENTITY_PIXELSCALE };
                    }
                    System.err.println("[set PixelScale pre]: had "+hadSurfacePixelScale[0]+"x"+hadSurfacePixelScale[1]+" -> req "+reqSurfacePixelScale[0]+"x"+reqSurfacePixelScale[1]);
                    snap.setMakeSnapshot();
                    glWindow.setSurfaceScale(reqSurfacePixelScale);
                    final float[] valReqSurfacePixelScale = glWindow.getRequestedSurfaceScale(new float[2]);
                    final float[] hasSurfacePixelScale = glWindow.getCurrentSurfaceScale(new float[2]);
                    final float[] nativeSurfacePixelScale = glWindow.getMaximumSurfaceScale(new float[2]);
                    System.err.println("[set PixelScale post]: "+hadSurfacePixelScale[0]+"x"+hadSurfacePixelScale[1]+" (had) -> "+
                                       reqSurfacePixelScale[0]+"x"+reqSurfacePixelScale[1]+" (req) -> "+
                                       valReqSurfacePixelScale[0]+"x"+valReqSurfacePixelScale[1]+" (val) -> "+
                                       hasSurfacePixelScale[0]+"x"+hasSurfacePixelScale[1]+" (has), "+
                                       nativeSurfacePixelScale[0]+"x"+nativeSurfacePixelScale[1]+" (native)");
                    setTitle(glWindow);
                }
            }
        });

        glWindow.setVisible(true);

        final float[] hasSurfacePixelScale1 = glWindow.getCurrentSurfaceScale(new float[2]);
        System.err.println("HiDPI PixelScale: "+reqSurfacePixelScale[0]+"x"+reqSurfacePixelScale[1]+" (req) -> "+
                           valReqSurfacePixelScale[0]+"x"+valReqSurfacePixelScale[1]+" (val) -> "+
                           hasSurfacePixelScale1[0]+"x"+hasSurfacePixelScale1[1]+" (has)");
        setTitle(glWindow);

        snap.setMakeSnapshot();
        glWindow.display();

        Thread.sleep(durationPerTest);

        glWindow.destroy();

    }

    @Test
    public void test01_PSA() throws InterruptedException {
        runTestGL();
    }

    @Test
    public void test99_PS1() throws InterruptedException, InvocationTargetException {
        if(manualTest) return;
        reqSurfacePixelScale[0] = ScalableSurface.IDENTITY_PIXELSCALE;
        reqSurfacePixelScale[1] = ScalableSurface.IDENTITY_PIXELSCALE;
        runTestGL();
    }

    public static void main(final String args[]) throws IOException {
        System.err.println("main - start");
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = MiscUtils.atoi(args[++i], (int)durationPerTest);
            } else if(args[i].equals("-pixelScale")) {
                i++;
                final float pS = MiscUtils.atof(args[i], reqSurfacePixelScale[0]);
                reqSurfacePixelScale[0] = pS;
                reqSurfacePixelScale[1] = pS;
            } else if(args[i].equals("-manual")) {
                manualTest = true;
            }
        }
        final String tstname = TestRulerNEWT01.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
        System.err.println("main - end");
    }
}

