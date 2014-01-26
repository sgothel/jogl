/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 *
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.jogamp.opengl.test.junit.jogl.caps;

import java.lang.reflect.InvocationTargetException;
import java.awt.BorderLayout;
import java.awt.Frame;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;

import com.jogamp.opengl.test.junit.jogl.demos.es1.MultisampleDemoES1;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.texture.TextureIO;

import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestMultisampleES1AWT extends UITestCase {
  static long durationPerTest = 60; // ms
  private GLCanvas canvas;

  public static void main(String[] args) {
     for(int i=0; i<args.length; i++) {
        if(args[i].equals("-time")) {
            durationPerTest = MiscUtils.atoi(args[++i], 500);
        }
     }
     System.out.println("durationPerTest: "+durationPerTest);
     String tstname = TestMultisampleES1AWT.class.getName();
     org.junit.runner.JUnitCore.main(tstname);
  }

  @Test
  public void testOnscreenMultiSampleAA0() throws InterruptedException, InvocationTargetException {
    testMultiSampleAAImpl(0);
  }

  @Test
  public void testOnscreenMultiSampleAA4() throws InterruptedException, InvocationTargetException {
    testMultiSampleAAImpl(4);
  }

  @Test
  public void testOnscreenMultiSampleAA8() throws InterruptedException, InvocationTargetException {
    testMultiSampleAAImpl(8);
  }

  private void testMultiSampleAAImpl(int reqSamples) throws InterruptedException, InvocationTargetException {
    final GLReadBufferUtil screenshot = new GLReadBufferUtil(true, false);
    GLProfile glp = GLProfile.getMaxFixedFunc(true);
    GLCapabilities caps = new GLCapabilities(glp);
    GLCapabilitiesChooser chooser = new MultisampleChooser01();

    if(reqSamples>0) {
        caps.setSampleBuffers(true);
        caps.setNumSamples(reqSamples);
    }

    canvas = new GLCanvas(caps, chooser, null);
    canvas.addGLEventListener(new MultisampleDemoES1(reqSamples>0?true:false));
    canvas.addGLEventListener(new GLEventListener() {
        int displayCount = 0;
        public void init(GLAutoDrawable drawable) {}
        public void dispose(GLAutoDrawable drawable) {}
        public void display(GLAutoDrawable drawable) {
            snapshot(displayCount++, null, drawable.getGL(), screenshot, TextureIO.PNG, null);
        }
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) { }
    });

    final Frame frame = new Frame("Multi Samples "+reqSamples);
    frame.setLayout(new BorderLayout());
    canvas.setSize(512, 512);

    javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
        public void run() {
            frame.add(canvas, BorderLayout.CENTER);
            frame.pack();
            frame.setVisible(true);
            canvas.requestFocus();
            canvas.display();
        }});

    Thread.sleep(durationPerTest);

    javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
        public void run() {
            frame.setVisible(false);
            frame.remove(canvas);
            frame.dispose();
        }});

  }
}
