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

package com.jogamp.opengl.test.junit.jogl.util.texture;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.GLPixelBuffer.GLPixelAttributes;
import com.jogamp.opengl.util.GLPixelBuffer.GLPixelBufferProvider;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.texture.TextureIO;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBug817GLReadBufferUtilGLCTXDefFormatTypeES2NEWT extends UITestCase {
  static long durationPerTest = 60; // ms

  public static void main(final String[] args) {
     for(int i=0; i<args.length; i++) {
        if(args[i].equals("-time")) {
            durationPerTest = MiscUtils.atoi(args[++i], 500);
        }
     }
     System.out.println("durationPerTest: "+durationPerTest);
     final String tstname = TestBug817GLReadBufferUtilGLCTXDefFormatTypeES2NEWT.class.getName();
     org.junit.runner.JUnitCore.main(tstname);
  }

  @Test
  public void test00_RGBtoRGB() throws InterruptedException {
    testImpl(false, false, false, false);
  }
  @Test
  public void test01_RGBtoRGBA() throws InterruptedException {
    testImpl(false, true, false, false);
  }

  @Test
  public void test10_RGBAtoRGB() throws InterruptedException {
    testImpl(true, false, false, false);
  }
  @Test
  public void test11_RGBAtoRGBA() throws InterruptedException {
    testImpl(true, true, false, false);
  }
  @Test
  public void test21_RGBtoRGBA_pbuffer() throws InterruptedException {
    testImpl(false, true, true, false);
  }
  @Test
  public void test22_RGBtoRGBA_fbo() throws InterruptedException {
    testImpl(false, true, false, true);
  }
  @Test
  public void test31_RGBAtoRGBA_pbuffer() throws InterruptedException {
    testImpl(true, true, true, false);
  }
  @Test
  public void test32_RGBAtoRGBA_fbo() throws InterruptedException {
    testImpl(true, true, false, true);
  }

  private void testImpl(final boolean alphaCaps, final boolean readAlpha, final boolean pbuffer, final boolean fbo) throws InterruptedException {
    final GLReadBufferUtil screenshot = new GLReadBufferUtil(readAlpha ? true : false, false);
    final GLProfile glp = GLProfile.getGL2ES2();
    final GLCapabilities caps = new GLCapabilities(glp);

    caps.setAlphaBits( alphaCaps ? 1 : 0 );
    caps.setPBuffer( pbuffer );
    caps.setFBO( fbo);

    final GLWindow window = GLWindow.create(caps);
    window.addGLEventListener(new GearsES2());
    window.addGLEventListener(new GLEventListener() {
        int displayCount = 0;
        public void init(final GLAutoDrawable drawable) {}
        public void dispose(final GLAutoDrawable drawable) {}
        public void display(final GLAutoDrawable drawable) {
            final GLPixelBufferProvider pixelBufferProvider = screenshot.getPixelBufferProvider();
            final GLPixelAttributes pixelAttribs = pixelBufferProvider.getAttributes(drawable.getGL(), readAlpha ? 4 : 3, true);
            System.err.println("GLPixelAttributes: "+pixelAttribs);
            snapshot(displayCount++, null, drawable.getGL(), screenshot, TextureIO.PNG, null);
        }
        public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) { }
    });
    window.setSize(512, 512);
    window.setVisible(true);
    window.requestFocus();

    Thread.sleep(durationPerTest);

    window.destroy();
  }

}
