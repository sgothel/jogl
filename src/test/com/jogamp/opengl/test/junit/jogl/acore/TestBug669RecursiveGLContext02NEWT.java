/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

/**
 * Tests recursive GLContext behavior.
 *
 * <p>
 * Issues {@link GLAutoDrawable#display()} of another {@link GLAutoDrawable}
 * from within {@link GLEventListener#display(GLAutoDrawable)}.
 * </p>
 *
 * <https://jogamp.org/bugzilla/show_bug.cgi?id=669>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBug669RecursiveGLContext02NEWT extends UITestCase {

  @Test(timeout=10000)
  public void test01_Plain() {
      test01Impl(false);
  }

  @Test(timeout=10000)
  public void test01_Anim() {
      test01Impl(true);
  }

  private void test01Impl(final boolean anim) {
    final String profile = GLProfile.GL2ES2;
    if(!GLProfile.isAvailable(profile)) { System.err.println(profile+" n/a"); return; }

    final GLProfile pro = GLProfile.get(profile);
    final GLCapabilities caps = new GLCapabilities(pro);

    final GLWindow window2 = GLWindow.create(caps); // display() triggered by window's GLEventListener!
    window2.setPosition(0, 0);
    window2.setSize(200, 200);
    window2.addGLEventListener(new RedSquareES2());

    final GLWindow window1 = GLWindow.create(caps);

    final Animator animator1 = new Animator();
    final Animator animator2 = new Animator();
    if(anim) {
        animator1.add(window1);
        animator2.add(window2);
    }
    animator1.start();
    animator2.start();

    window1.setPosition(250, 0);
    window1.setSize(200, 200);
    window1.addGLEventListener(new GLEventListener() {
      public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) { }

      public void init(final GLAutoDrawable drawable) { }

      public void dispose(final GLAutoDrawable drawable) { }

      public void display(final GLAutoDrawable drawable) {
        window2.display();
      }
    });
    window1.addGLEventListener(new GearsES2());

    try {
        window2.setVisible(true);
        window1.setVisible(true);
        window1.display();
        window2.display();
        if(anim) {
            try {
                Thread.sleep(500);
            } catch(final InterruptedException ie) {}
        }
    } finally {
        animator1.stop();

        final int win1Frames = window1.getTotalFPSFrames();
        final int win2Frames = window2.getTotalFPSFrames();
        System.err.println("Window1: frames "+win1Frames);
        System.err.println("Window2: frames "+win2Frames);
        Assert.assertTrue("Win2 frames not double the amount of Win1 frames", 2*win2Frames >= win1Frames);
        window1.destroy();
        window2.destroy();
    }
  }

  public static void main(final String args[]) {
      org.junit.runner.JUnitCore.main(TestBug669RecursiveGLContext02NEWT.class.getName());
  }

}

