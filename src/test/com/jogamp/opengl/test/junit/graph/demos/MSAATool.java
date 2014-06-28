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
package com.jogamp.opengl.test.junit.graph.demos;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilitiesImmutable;

public class MSAATool {
    public static boolean glIsEnabled(GL gl, int name) {
        boolean isEnabled = false;
        try {
            isEnabled = gl.glIsEnabled(name);
            int glerr = gl.glGetError();
            if(GL.GL_NO_ERROR != glerr) {
                System.err.println("glIsEnabled(0x"+Integer.toHexString(name)+") -> error 0x"+Integer.toHexString(glerr));
            }
        } catch (Exception e) {
            System.err.println("Caught exception: "+e.getMessage());
            // e.printStackTrace();
        }
        return isEnabled;
    }
    public static void dump(GLAutoDrawable drawable) {
        float[] vf = new float[] { 0f };
        byte[] vb = new byte[] { 0 };
        int[] vi = new int[] { 0, 0 };

        System.out.println("GL MSAA SETUP:");
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        GLCapabilitiesImmutable caps = drawable.getChosenGLCapabilities();
        System.out.println("  Caps realised "+caps);
        System.out.println("  Caps sample buffers "+caps.getSampleBuffers()+", samples "+caps.getNumSamples());

        System.out.println("  GL MULTISAMPLE "+glIsEnabled(gl, GL2ES2.GL_MULTISAMPLE));
        // sample buffers min 0, same as GLX_SAMPLE_BUFFERS_ARB or WGL_SAMPLE_BUFFERS_ARB
        gl.glGetIntegerv(GL2GL3.GL_SAMPLE_BUFFERS, vi, 0);
        // samples min 0
        gl.glGetIntegerv(GL2GL3.GL_SAMPLES, vi, 1);
        System.out.println("  GL SAMPLE_BUFFERS "+vi[0]+", SAMPLES "+vi[1]);

        System.out.println("GL CSAA SETUP:");
        // default FALSE
        System.out.println("  GL SAMPLE COVERAGE "+glIsEnabled(gl, GL2GL3.GL_SAMPLE_COVERAGE));
        // default FALSE
        System.out.println("  GL SAMPLE_ALPHA_TO_COVERAGE "+glIsEnabled(gl, GL2GL3.GL_SAMPLE_ALPHA_TO_COVERAGE));
        // default FALSE
        System.out.println("  GL SAMPLE_ALPHA_TO_ONE "+glIsEnabled(gl, GL2GL3.GL_SAMPLE_ALPHA_TO_ONE));
        // default FALSE, value 1, invert false
        gl.glGetFloatv(GL2GL3.GL_SAMPLE_COVERAGE_VALUE, vf, 0);
        gl.glGetBooleanv(GL2GL3.GL_SAMPLE_COVERAGE_INVERT, vb, 0);
        System.out.println("  GL SAMPLE_COVERAGE "+glIsEnabled(gl, GL2GL3.GL_SAMPLE_COVERAGE) +
                              ": SAMPLE_COVERAGE_VALUE "+vf[0]+
                              ", SAMPLE_COVERAGE_INVERT "+vb[0]);
        dumpBlend(gl);
    }
    public static void dumpBlend(GL gl) {
        int[] vi = new int[] { 0, 0, 0, 0 };
        gl.glGetIntegerv(GL.GL_BLEND, vi, 0);
        gl.glGetIntegerv(GL.GL_BLEND_SRC_ALPHA, vi, 1);
        gl.glGetIntegerv(GL.GL_BLEND_SRC_RGB, vi, 2);
        gl.glGetIntegerv(GL.GL_BLEND_DST_RGB, vi, 3);
        final boolean blendEnabled = vi[0] == GL.GL_TRUE;
        System.out.println("GL_BLEND "+blendEnabled+"/"+glIsEnabled(gl, GL.GL_BLEND) +
                           "  GL_SRC_ALPHA 0x"+Integer.toHexString(vi[1])+
                           "  GL_SRC_RGB 0x"+Integer.toHexString(vi[2])+
                           "  GL_DST_RGB 0x"+Integer.toHexString(vi[3]));
    }
}
