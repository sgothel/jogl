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

package com.jogamp.opengl.test.junit.jogl.acore;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GLExtensions;
import com.jogamp.opengl.test.junit.util.UITestCase;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Some GL state values are broken w/ Mesa 9.0 w/ multiple different context.
 * <p>
 * This bug lies within Mesa3D (any renderer) and is fixed in
 * commit 8dc79ae7d73cf6711c2182ff9a5d37ef6c989d23.
 * </p>
 * <p>
 * Mesa3D Version 9.0 still exposes this bug,
 * where 9.0.1 has it fixed w/ above commit.
 * </p>
 * <https://jogamp.org/bugzilla/show_bug.cgi?id=651>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLMesaBug651NEWT extends UITestCase {
    static int width, height;

    @BeforeClass
    public static void initClass() {
        width  = 512;
        height = 512;
    }

    @AfterClass
    public static void releaseClass() {
    }

    static class UnitTester implements GLEventListener {
        @Override
        public void init(final GLAutoDrawable drawable) {
            final GL gl = drawable.getGL();
            System.err.println("GL UnitTester");
            System.err.println("  GL_VENDOR: " + gl.glGetString(GL.GL_VENDOR));
            System.err.println("  GL_RENDERER: " + gl.glGetString(GL.GL_RENDERER));
            System.err.println("  GL_VERSION: " + gl.glGetString(GL.GL_VERSION));
            System.err.println("  GL GLSL: "+gl.hasGLSL()+", has-compiler-func: "+gl.isFunctionAvailable("glCompileShader")+", version "+(gl.hasGLSL() ? gl.glGetString(GL2ES2.GL_SHADING_LANGUAGE_VERSION) : "none")+", "+gl.getContext().getGLSLVersionNumber());
            System.err.println("  GL FBO: basic "+ gl.hasBasicFBOSupport()+", full "+gl.hasFullFBOSupport());
            System.err.println("  GL Profile: "+gl.getGLProfile());
            System.err.println("  GL Renderer Quirks:" + gl.getContext().getRendererQuirks().toString());
            System.err.println("  GL:" + gl + ", " + gl.getContext().getGLVersion());

            final int _glerr = gl.glGetError(); // clear pre-error
            System.err.println("  - pre GL-Error 0x"+Integer.toHexString(_glerr));

            final int[] val = new int[1];
            final int[] glerr = new int[] { GL.GL_NO_ERROR, GL.GL_NO_ERROR, GL.GL_NO_ERROR, GL.GL_NO_ERROR, GL.GL_NO_ERROR };
            int i=0;

            val[0]=0;
            gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, val, 0);
            System.out.println("  - GL_MAX_TEXTURE_SIZE: " + val[0]);
            glerr[i] = gl.glGetError(); // clear pre-error
            System.err.println("  - GL-Error 0x"+Integer.toHexString(glerr[i]));
            i++;

            val[0]=0;
            gl.glGetIntegerv(GL.GL_ACTIVE_TEXTURE, val, 0);
            System.out.println("  - GL_ACTIVE_TEXTURE: " + val[0]);
            glerr[i] = gl.glGetError(); // clear pre-error
            System.err.println("  - GL-Error 0x"+Integer.toHexString(glerr[i]));
            i++;

            if(gl.isGL2ES2()) {
                val[0]=0;
                gl.glGetIntegerv(GL2ES2.GL_MAX_TEXTURE_IMAGE_UNITS, val, 0);
                System.out.println("  - GL_MAX_TEXTURE_IMAGE_UNITS: " + val[0]);
                glerr[i] = gl.glGetError(); // clear pre-error
                System.err.println("  - GL-Error 0x"+Integer.toHexString(glerr[i]));
            }
            i++;

            if( gl.hasFullFBOSupport() || gl.isExtensionAvailable(GLExtensions.NV_fbo_color_attachments) ) {
                val[0]=0;
                gl.glGetIntegerv(GL2ES2.GL_MAX_COLOR_ATTACHMENTS, val, 0);
                System.out.println("  - GL_MAX_COLOR_ATTACHMENTS: " + val[0]);
                glerr[i] = gl.glGetError(); // clear pre-error
                System.err.println("  - GL-Error 0x"+Integer.toHexString(glerr[i]));
            }
            i++;

            if( gl.hasFullFBOSupport() ) {
                val[0]=0;
                gl.glGetIntegerv(GL2ES3.GL_MAX_SAMPLES, val, 0);
                System.out.println("  - GL_MAX_SAMPLES: " + val[0]);
                glerr[i] = gl.glGetError(); // clear pre-error
                System.err.println("  - GL-Error 0x"+Integer.toHexString(glerr[i]));
            }
            i++;

            boolean ok = true;
            String res="";
            for(int j=0; j<i; j++) {
                switch(j) {
                    case 0: res += "GL_MAX_TEXTURE_SIZE"; break;
                    case 1: res += "GL_ACTIVE_TEXTURE"; break;
                    case 2: res += "GL_MAX_TEXTURE_IMAGE_UNITS"; break;
                    case 3: res += "GL_MAX_COLOR_ATTACHMENTS"; break;
                    case 4: res += "GL_MAX_SAMPLES"; break;
                }
                if(GL.GL_NO_ERROR == glerr[j]) {
                    res += " OK, ";
                } else {
                    res += " ERROR, ";
                    ok = false;
                }
            }
            Assert.assertTrue(res, ok);
        }

        @Override
        public void dispose(final GLAutoDrawable drawable) {
        }

        @Override
        public void display(final GLAutoDrawable drawable) {
        }

        @Override
        public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        }
    }

    protected void runTestGL(final GLCapabilities caps) throws InterruptedException {
        final GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle(getSimpleTestName("."));

        final UnitTester demo = new UnitTester();

        glWindow.addGLEventListener(demo);

        glWindow.setSize(width, height);
        glWindow.setVisible(true);
        glWindow.display();

        glWindow.destroy();
    }

    @Test
    public void test01_ES1() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GLES1)) { System.err.println("GLES1 n/a"); return; }
        final GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GLES1));
        runTestGL(caps);
    }

    @Test
    public void test02__ES2() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GLES2)) { System.err.println("GLES2 n/a"); return; }
        final GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GLES2));
        runTestGL(caps);
    }

    @Test
    public void test03_GL2() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GL2)) { System.err.println("GL2 n/a"); return; }
        final GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL2));
        runTestGL(caps);
    }

    @Test
    public void test04_GL3() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GL3)) { System.err.println("GL3 n/a"); return; }
        final GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL3));
        runTestGL(caps);
    }

    @Test
    public void test05_GL4() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GL4)) { System.err.println("GL4 n/a"); return; }
        final GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL4));
        runTestGL(caps);
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestGLMesaBug651NEWT.class.getName());
    }
}
