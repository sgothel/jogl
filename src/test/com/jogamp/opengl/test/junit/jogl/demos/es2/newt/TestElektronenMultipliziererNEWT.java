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
 
package com.jogamp.opengl.test.junit.jogl.demos.es2.newt;

import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.TraceWindowAdapter;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.QuitAdapter;

import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.test.junit.jogl.demos.es2.ElektronenMultiplizierer;

import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLProfile;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

/**
 * @see com.jogamp.opengl.test.junit.jogl.demos.es2.ElektronenMultiplizierer
 * @author Dominik Ströhlein (DemoscenePassivist), et.al.
 */
public class TestElektronenMultipliziererNEWT extends UITestCase {
    static final int width = 640, height = 480;

    static final String tRoutineClassName = null;
    static final boolean tMultiSampling = false;
    static final int tNumberOfSampleBuffers = -1;
    static final boolean tAnisotropicFiltering = false;
    static final float tAnisotropyLevel = -1.0f;
    static final boolean tFrameCapture = false;
    static final boolean tFrameSkip = true;
    static final int desiredFrameRate = 30;
    static int startFrame = 1700;
    static long duration = 5000; // ms
    
    @AfterClass
    public static void releaseClass() {
    }

    protected void run() throws InterruptedException {
        final ElektronenMultiplizierer demo = new ElektronenMultiplizierer(
                tRoutineClassName,
                tMultiSampling,tNumberOfSampleBuffers,
                tAnisotropicFiltering,tAnisotropyLevel,
                tFrameCapture,
                tFrameSkip, desiredFrameRate, startFrame
        );
        GLCapabilitiesImmutable caps = demo.getGLCapabilities();
        
        GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setSize(width, height);
        glWindow.setTitle("ElektronenMultiplizierer (GL2ES2/NEWT)");
        glWindow.addGLEventListener(demo);

        Animator animator = new Animator(glWindow);
        animator.setUpdateFPSFrames(60, System.err);
        QuitAdapter quitAdapter = new QuitAdapter();

        //glWindow.addKeyListener(new TraceKeyAdapter(quitAdapter));
        glWindow.addWindowListener(new TraceWindowAdapter(quitAdapter));
        glWindow.addKeyListener(quitAdapter);
        glWindow.addWindowListener(quitAdapter);

        final GLWindow f_glWindow = glWindow;
        glWindow.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if(e.getKeyChar()=='f') {
                    new Thread() {
                        public void run() {
                            f_glWindow.setFullscreen(!f_glWindow.isFullscreen());
                    } }.start();
                } else if(e.getKeyChar()=='d') {
                    new Thread() {
                        public void run() {
                            f_glWindow.setUndecorated(!f_glWindow.isUndecorated());
                    } }.start();
                }
            }
        });

        glWindow.setVisible(true);
        animator.start();

        while(!quitAdapter.shouldQuit() && animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }
        animator.stop();
        glWindow.destroy();
    }

    @Test
    public void testElektronenMultiplizierer01() throws InterruptedException {
        run();
    }

    public static void main(String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
            if(args[i].equals("-sframe")) {
                i++;
                try {
                    startFrame = Integer.parseInt(args[i]);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
        org.junit.runner.JUnitCore.main(TestElektronenMultipliziererNEWT.class.getName());
    }
}
