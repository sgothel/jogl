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

import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.test.junit.util.GLSLSimpleProgram;
import com.jogamp.opengl.test.junit.util.UITestCase;


import javax.media.opengl.FPSCounter;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLProfile;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.test.junit.jogl.demos.es2.shader.RedSquareShader;
import com.jogamp.opengl.test.junit.util.MiscUtils;

import java.io.IOException;
import javax.media.opengl.GL2ES2;
import org.junit.AfterClass;

public class TestGLSLSimple01NEWT extends UITestCase {
    static long durationPerTest = 100; // ms

    @Test(timeout=60000)
    public void testGLSLCompilation01() {
        GLProfile glp = GLProfile.get(GLProfile.GL2ES2);
        Assert.assertNotNull(glp);
        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);

        GLWindow window = GLWindow.create(caps);
        Assert.assertNotNull(window);
        window.setSize(800, 600);
        window.setVisible(true);
        Assert.assertTrue(window.isNativeValid());

        GLContext context = window.getContext();
        context.setSynchronized(true);
        
        // trigger native creation of drawable/context
        window.display();
        Assert.assertTrue(window.isRealized());
        Assert.assertTrue(window.getContext().isCreated());

        context.makeCurrent();

        // given

        GL2ES2 gl = context.getGL().getGL2ES2();
        GLSLSimpleProgram myShader = GLSLSimpleProgram.create(gl,
                RedSquareShader.VERTEX_SHADER_TEXT,
                RedSquareShader.FRAGMENT_SHADER_TEXT,
                true);

        myShader.release(gl);
        context.release();
        window.destroy();
    }

    @Test(timeout=60000)
    public void testGLSLUse01() throws InterruptedException {
        GLProfile glp = GLProfile.get(GLProfile.GL2ES2);
        Assert.assertNotNull(glp);
        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);

        GLWindow window = GLWindow.create(caps);
        Assert.assertNotNull(window);
        window.setSize(800, 600);
        window.setVisible(true);
        Assert.assertTrue(window.isNativeValid());
        window.addGLEventListener(new RedSquareES2());
        
        Animator animator = new Animator(window);
        animator.setUpdateFPSFrames(1, null);        
        animator.start();
        Assert.assertEquals(true, animator.isAnimating());
        while(animator.isAnimating() && animator.getTotalFPSDuration()<durationPerTest) {
            Thread.sleep(100);
        }
        Assert.assertEquals(true, animator.isAnimating());

        window.destroy();
        animator.stop();
    }

    public static void main(String args[]) throws IOException {
        System.err.println("main - start");
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = MiscUtils.atoi(args[++i], (int)durationPerTest);
            }
        }
        String tstname = TestGLSLSimple01NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
        System.err.println("main - end");
    }

}
