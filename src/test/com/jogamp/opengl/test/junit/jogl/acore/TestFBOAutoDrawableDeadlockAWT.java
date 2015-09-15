/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.DefaultGLCapabilitiesChooser;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;

import jogamp.nativewindow.jawt.JAWTUtil;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.util.InterruptSource;
import com.jogamp.common.util.RunnableTask;
import com.jogamp.opengl.test.junit.util.UITestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestFBOAutoDrawableDeadlockAWT extends UITestCase {
  static GLProfile glp;
  static int width, height;

  @BeforeClass
  public static void initClass() {
    glp = GLProfile.getGL2ES2();
    Assert.assertNotNull( glp );
    width = 512;
    height = 512;
  }

  protected void runTestGL( final GLCapabilities caps ) throws InterruptedException, InvocationTargetException {
    final GLOffscreenAutoDrawable fbod = GLDrawableFactory.getFactory(caps.getGLProfile()).createOffscreenAutoDrawable(
        null, caps, new DefaultGLCapabilitiesChooser(), 512, 512);

    final boolean[] done = {false};
    final Runnable pbufferCreationAction = new Runnable() {
      public void run() {
        System.err.println("AA.1");
        fbod.display();
        done[ 0 ] = true;
        System.err.println("AA.X");
      }
    };

    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
            Assert.assertTrue(EventQueue.isDispatchThread());
            JAWTUtil.lockToolkit();
            try {
                final RunnableTask rTask = new RunnableTask(pbufferCreationAction, new Object(), false, null);
                System.err.println("BB.0: "+rTask.getSyncObject());
                synchronized (rTask.getSyncObject()) {
                    System.err.println("BB.1: "+rTask.getSyncObject());
                    new InterruptSource.Thread(null, rTask, Thread.currentThread().getName()+"-Pbuffer_Creation").start();
                    try {
                        System.err.println("BB.2");
                        rTask.getSyncObject().wait();
                        System.err.println("BB.3");
                    } catch (final InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    System.err.println("BB.X");
                }
            } finally {
                JAWTUtil.unlockToolkit();
            }
        }
    });
    Assert.assertTrue(done[0]);
    fbod.destroy();
  }

  @Test(timeout = 10000)
  public void testDeadlock() throws InterruptedException, InvocationTargetException {
    final GLCapabilities caps = new GLCapabilities( glp );
    runTestGL( caps );
  }

  static long duration = 500; // ms

  public static void main( final String args[] ) {
    for ( int i = 0; i < args.length; i++ ) {
      if ( args[ i ].equals( "-time" ) ) {
        i++;
        try {
          duration = Integer.parseInt( args[ i ] );
        }
        catch ( final Exception ex ) {
          ex.printStackTrace();
        }
      }
    }
    org.junit.runner.JUnitCore.main( TestFBOAutoDrawableDeadlockAWT.class.getName() );
  }
}
