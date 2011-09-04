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

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import javax.media.opengl.*;
import org.junit.Test;

public class TestMultisampleNEWT {
  static long durationPerTest = 500; // ms
  private GLWindow window;

  public static void main(String[] args) {
     for(int i=0; i<args.length; i++) {
        if(args[i].equals("-time")) {
            durationPerTest = MiscUtils.atoi(args[++i], 500);
        }
     }
     System.out.println("durationPerTest: "+durationPerTest);
     String tstname = TestMultisampleNEWT.class.getName();
     org.junit.runner.JUnitCore.main(tstname);
  }

  @Test
  public void testMultiSampleAA4() throws InterruptedException {
    testMultiSampleAAImpl(4);
  }

  // @Test
  public void testMultiSampleNone() throws InterruptedException {
    testMultiSampleAAImpl(0);
  }

  private void testMultiSampleAAImpl(int samples) throws InterruptedException {
    GLCapabilities caps = new GLCapabilities(null);
    GLCapabilitiesChooser chooser = new MultisampleChooser01();

    if(samples>0) {
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);
    }
    // turns out we need to have alpha, 
    // otherwise no AA will be visible.
    // This is done implicit now ..
    // caps.setAlphaBits(1); 

    window = GLWindow.create(caps);
    window.setCapabilitiesChooser(chooser);
    window.addGLEventListener(new MultisampleDemo01(samples>0?true:false));
    window.setSize(512, 512);
    window.setVisible(true);
    window.setPosition(0, 0);
    window.requestFocus();

    GLCapabilitiesImmutable capsChosen0 = window.getChosenGLCapabilities();

    Thread.sleep(durationPerTest);

    window.destroy();
  }

}
