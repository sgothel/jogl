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
 
package com.jogamp.opengl.test.junit.jogl.caps;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Label;
import java.awt.Transparency;
import java.awt.image.ColorModel;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

public class TestTranslucencyAWT extends UITestCase {
    static Dimension size;
    static long durationPerTest = 400;
    static GLCapabilities glCaps;

    @BeforeClass
    public static void initClass() {
        size = new Dimension(400,200);
        glCaps = new GLCapabilities(null);
        glCaps.setAlphaBits(8);
        glCaps.setBackgroundOpaque(false);
    }

    static Frame getTranslucentFrame() {
        GraphicsConfiguration gc=null;
        GraphicsDevice[] devices= GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        for (int i = 0; i < devices.length ; i++)
        {
            GraphicsConfiguration[] configs = devices[i].getConfigurations();
            for (int j = 0; j < configs.length ; j++) {
                GraphicsConfiguration config = configs[j];
                ColorModel tcm = config.getColorModel(Transparency.TRANSLUCENT);
                boolean capable1 = ( null != tcm ) ? tcm.getTransparency() == Transparency.TRANSLUCENT : false;
                boolean capable2 = false;
                try {
                    capable2 = ((Boolean)ReflectionUtil.callStaticMethod(
                                                "com.sun.awt.AWTUtilities", "isTranslucencyCapable", 
                                                new Class<?>[] { GraphicsConfiguration.class }, 
                                                new Object[] { config } , 
                                                GraphicsConfiguration.class.getClassLoader())).booleanValue();
                    System.err.println("com.sun.awt.AWTUtilities.isTranslucencyCapable(config) passed: "+capable2);
                } catch (RuntimeException re) {
                    System.err.println("com.sun.awt.AWTUtilities.isTranslucencyCapable(config) failed: "+re.getMessage());
                }
                System.err.println(i+":"+j+" "+config+", "+tcm+", capable "+capable1+"/"+capable2);
                if(capable1&&capable2) {
                    gc=configs[j];
                    System.err.println("Chosen "+i+":"+j+" "+config+", "+tcm+", capable "+capable1+"/"+capable2);
                    break;
                }
            }
        }
        final Frame frame = new Frame(gc);
        if(null!=gc) {
            frame.setUndecorated(true);
            frame.setBackground(new Color(0, 0, 0, 0));
        }
        frame.setTitle("AWT Parent Frame (opaque: "+(null==gc)+")");
        return frame;
    }
    
    @Test
    public void test() throws InterruptedException, InvocationTargetException {
        final Frame frame1 = getTranslucentFrame();
        final GLCanvas glCanvas= new GLCanvas(glCaps);
        
        glCanvas.setPreferredSize(size);

        GLEventListener demo1 = new GearsES2(1);
        // setDemoFields(demo1, glCanvas, false);
        glCanvas.addGLEventListener(demo1);
        GLAnimatorControl animator1 = new Animator(glCanvas);
        animator1.start();

        Container cont1 = new Container();
        cont1.setLayout(new BorderLayout());
        cont1.add(glCanvas, BorderLayout.CENTER);
        cont1.setVisible(true);

        frame1.setLayout(new BorderLayout());
        frame1.add(cont1, BorderLayout.EAST);
        frame1.add(new Label("center"), BorderLayout.CENTER);
        frame1.setLocation(0, 0);
        frame1.setSize((int)size.getWidth(), (int)size.getHeight());
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame1.pack();
                frame1.setVisible(true);
            }});

        Assert.assertEquals(true, animator1.isAnimating());
        Assert.assertEquals(false, animator1.isPaused());
        Assert.assertNotNull(animator1.getThread());

        Thread.sleep(durationPerTest);

        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());
        Assert.assertEquals(false, animator1.isPaused());
        Assert.assertEquals(null, animator1.getThread());

        frame1.dispose();
    }

    static int atoi(String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            }
        }
        String tstname = TestTranslucencyAWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
