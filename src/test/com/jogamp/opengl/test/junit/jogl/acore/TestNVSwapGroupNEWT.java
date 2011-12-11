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

import com.jogamp.newt.opengl.GLWindow;

import javax.media.nativewindow.util.InsetsImmutable;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestNVSwapGroupNEWT extends UITestCase {
    static GLProfile glp;
    static GLCapabilities caps;
    static int width, height;

    @BeforeClass
    public static void initClass() {
        glp = GLProfile.getDefault();
        Assert.assertNotNull(glp);
        caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        width  = 256;
        height = 256;
    }

    protected GLWindow runTestGL(Animator animator, int x, int y, final int group, final int barrier) {
        GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("Shared Gears NEWT Test: "+x+"/"+y);

        glWindow.setSize(width, height);

        GearsES2 gears = new GearsES2(1);
        glWindow.addGLEventListener(new GLEventListener() {
            public void init(GLAutoDrawable drawable) {
                int[] maxVals = new int[] { -1, -1 } ;
                GLContext glc = drawable.getContext();
                boolean r = glc.queryMaxSwapGroups(maxVals, 0, maxVals, 1);
                System.err.println("swap group max groups "+maxVals[0]+", barriers "+maxVals[0]+", "+r);
                if(maxVals[0]>=group) {
                    System.err.println("swap group joing 1: "+glc.joinSwapGroup(group));
                    if(maxVals[1]>=barrier) {
                        System.err.println("swap group bind 1-1: "+glc.bindSwapBarrier(group, barrier));
                    }
                }                
            }
            public void dispose(GLAutoDrawable drawable) {}
            public void display(GLAutoDrawable drawable) {}
            public void reshape(GLAutoDrawable drawable, int x, int y,
                    int width, int height) {}
        });
        glWindow.addGLEventListener(gears);

        animator.add(glWindow);

        glWindow.setVisible(true);

        /** insets (if supported) are available only if window is set visible and hence is created */
        glWindow.setTopLevelPosition(x, y);

        return glWindow;
    }

    /** NV swap group is currently disabled .. needs more testing */
    @Test
    public void test01() throws InterruptedException {
        // make sure it won't be active for now !
        int swap_group = 9999; 
        int swap_barrier = 9999; 
        
        Animator animator = new Animator();
        GLWindow f1 = runTestGL(animator, 0, 0, swap_group, swap_barrier);
        InsetsImmutable insets = f1.getInsets();
        GLWindow f2 = runTestGL(animator, width+insets.getTotalWidth(), 0, swap_group, swap_barrier);
        animator.setUpdateFPSFrames(1, null);        
        animator.start();
        while(animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }
        animator.stop();

        f1.destroy();
        f2.destroy();

        // see above ..
        // releaseShared();
    }

    static long duration = 500; // ms

    public static void main(String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
        org.junit.runner.JUnitCore.main(TestNVSwapGroupNEWT.class.getName());
    }
}
