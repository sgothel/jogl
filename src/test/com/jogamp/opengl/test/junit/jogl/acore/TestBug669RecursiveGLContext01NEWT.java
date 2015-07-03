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
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

import jogamp.opengl.GLContextImpl;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

/**
 * Tests simple recursive GLContext behavior.
 *
 * <p>
 * Issues {@link GLContext#makeCurrent()} and {@link GLContext#release()}
 * from within {@link GLEventListener#display(GLAutoDrawable)}.
 * </p>
 *
 * <https://jogamp.org/bugzilla/show_bug.cgi?id=669>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBug669RecursiveGLContext01NEWT extends UITestCase {

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
    final GLWindow window = GLWindow.create(caps);

    final Animator animator = new Animator();
    if(anim) {
        animator.add(window);
    }
    animator.start();

    window.setSize(640, 480);
    window.addGLEventListener(new GLEventListener() {
      private void makeCurrentRecursive(final GLContextImpl context, final int lockCount) {
        Assert.assertEquals(true, context.isOwner(Thread.currentThread()));
        Assert.assertEquals(lockCount, context.getLockCount());
        Assert.assertEquals(true, context.isCurrent());

        Assert.assertEquals(GLContext.CONTEXT_CURRENT, context.makeCurrent()); // recursive: lock +1

        Assert.assertEquals(true, context.isOwner(Thread.currentThread()));
        Assert.assertEquals(lockCount+1, context.getLockCount());
        Assert.assertEquals(true, context.isCurrent());
      }
      private void releaseRecursive(final GLContextImpl context, final int lockCount) {
        Assert.assertEquals(true, context.isOwner(Thread.currentThread()));
        Assert.assertEquals(lockCount, context.getLockCount());
        Assert.assertEquals(true, context.isCurrent()); // still current

        context.release();  // recursive: lock -1

        Assert.assertEquals(true, context.isOwner(Thread.currentThread()));
        Assert.assertEquals(lockCount-1, context.getLockCount());
        Assert.assertEquals(true, context.isCurrent()); // still current
      }

      public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) { }

      public void init(final GLAutoDrawable drawable) { }

      public void dispose(final GLAutoDrawable drawable) { }

      public void display(final GLAutoDrawable drawable) {
        final GLContextImpl context = (GLContextImpl)drawable.getContext();
        makeCurrentRecursive(context, 1);
        releaseRecursive(context, 2);
      }
    });
    window.addGLEventListener(new GearsES2());

    try {
        window.setVisible(true);
        window.display();
    } finally {
        animator.stop();
        window.destroy();
    }
  }

  public static void main(final String args[]) {
      org.junit.runner.JUnitCore.main(TestBug669RecursiveGLContext01NEWT.class.getName());
  }

}

