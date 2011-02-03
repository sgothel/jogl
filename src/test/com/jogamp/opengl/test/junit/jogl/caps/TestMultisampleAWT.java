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

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.awt.GLCanvas;

import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;

import org.junit.Test;


public class TestMultisampleAWT extends UITestCase {
  static long durationPerTest = 250; // ms
  private GLCanvas canvas;

  public static void main(String[] args) {
     for(int i=0; i<args.length; i++) {
        if(args[i].equals("-time")) {
            durationPerTest = MiscUtils.atoi(args[++i], 500);
        }
     }
     System.out.println("durationPerTest: "+durationPerTest);
     String tstname = TestMultisampleAWT.class.getName();
     org.junit.runner.JUnitCore.main(tstname);
  }

  @Test
  public void testMultiSampleAA4() throws InterruptedException, InvocationTargetException {
    testMultiSampleAAImpl(4);
  }

  @Test
  public void testMultiSampleNone() throws InterruptedException, InvocationTargetException {
    testMultiSampleAAImpl(0);
  }

  private void testMultiSampleAAImpl(int samples) throws InterruptedException, InvocationTargetException {
    GLCapabilities caps = new GLCapabilities(null);
    GLCapabilitiesChooser chooser = new MultisampleChooser01();

    if(samples>0) {
        caps.setSampleBuffers(true);
        caps.setNumSamples(samples);
        // turns out we need to have alpha, 
        // otherwise no AA will be visible.
        caps.setAlphaBits(1); 
    }

    canvas = new GLCanvas(caps, chooser, null, null);
    canvas.addGLEventListener(new MultisampleDemo01(samples>0?true:false));
    
    final Frame frame = new Frame("Multi Samples "+samples);
    frame.setLayout(new BorderLayout());
    canvas.setSize(512, 512);
    frame.add(canvas, BorderLayout.CENTER);
    frame.pack();

    javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
        public void run() {
            frame.setVisible(true);
            frame.setLocation(0, 0);
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
