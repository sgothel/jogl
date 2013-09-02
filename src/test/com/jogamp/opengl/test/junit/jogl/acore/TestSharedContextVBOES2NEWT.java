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
 
package com.jogamp.opengl.test.junit.jogl.acore;

import com.jogamp.common.os.Platform;
import com.jogamp.common.util.VersionNumber;
import com.jogamp.newt.opengl.GLWindow;

import javax.media.nativewindow.util.InsetsImmutable;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLProfile;
import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSharedContextVBOES2NEWT extends UITestCase {
    static GLProfile glp;
    static GLCapabilities caps;
    static int width, height;
    GLAutoDrawable sharedDrawable;
    GearsES2 sharedGears;

    @BeforeClass
    public static void initClass() {
        if(GLProfile.isAvailable(GLProfile.GL2ES2)) {
            glp = GLProfile.get(GLProfile.GL2ES2);
            Assert.assertNotNull(glp);
            caps = new GLCapabilities(glp);
            Assert.assertNotNull(caps);
            width  = 256;
            height = 256;
        } else {
            setTestSupported(false);
        }
    }

    private void initShared(boolean onscreen) {
        if(onscreen) {
            GLWindow glWindow = GLWindow.create(caps);
            Assert.assertNotNull(glWindow);
            glWindow.setSize(width, height);
            glWindow.setVisible(true);
            sharedDrawable = glWindow;
        } else {        
            sharedDrawable = GLDrawableFactory.getFactory(glp).createOffscreenAutoDrawable(null, caps, null, width, height, null);
        }
        Assert.assertNotNull(sharedDrawable);
        sharedGears = new GearsES2();
        Assert.assertNotNull(sharedGears);
        sharedDrawable.addGLEventListener(sharedGears);
        // init and render one frame, which will setup the Gears display lists
        sharedDrawable.display();
    }

    private void releaseShared() {
        Assert.assertNotNull(sharedDrawable);
        sharedDrawable.destroy();
        sharedDrawable = null;
    }

    protected GLWindow runTestGL(Animator animator, int x, int y, boolean useShared, boolean vsync) throws InterruptedException {
        GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("Shared Gears NEWT Test: "+x+"/"+y+" shared "+useShared);
        if(useShared) {
            glWindow.setSharedContext(sharedDrawable.getContext());
        }

        glWindow.setSize(width, height);

        GearsES2 gears = new GearsES2(vsync ? 1 : 0);
        if(useShared) {
            gears.setGears(sharedGears.getGear1(), sharedGears.getGear2(), sharedGears.getGear3());
        }
        glWindow.addGLEventListener(gears);

        animator.add(glWindow);
        animator.start();        
        glWindow.setVisible(true);
        Assert.assertTrue(AWTRobotUtil.waitForRealized(glWindow, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(glWindow, true));

        glWindow.setPosition(x, y);

        return glWindow;
    }

    @Test
    public void testCommonAnimatorSharedOnscreen() throws InterruptedException {
        initShared(true);
        Animator animator = new Animator();
        GLWindow f1 = runTestGL(animator, 0, 0, true, false);
        InsetsImmutable insets = f1.getInsets();
        GLWindow f2 = runTestGL(animator, f1.getX()+width+insets.getTotalWidth(), 
                                          f1.getY()+0, true, false);
        GLWindow f3 = runTestGL(animator, f1.getX()+0, 
                                          f1.getY()+height+insets.getTotalHeight(), true, false);
        try {
            Thread.sleep(duration);
        } catch(Exception e) {
            e.printStackTrace();
        }
        animator.stop();

        f1.destroy();
        f2.destroy();
        f3.destroy();
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f1, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f1, false));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f2, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f2, false));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f3, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f3, false));

        releaseShared();
    }

    @Test
    public void testCommonAnimatorSharedOffscreen() throws InterruptedException {
        initShared(false);
        Animator animator = new Animator();
        GLWindow f1 = runTestGL(animator, 0, 0, true, false);
        InsetsImmutable insets = f1.getInsets();
        GLWindow f2 = runTestGL(animator, f1.getX()+width+insets.getTotalWidth(), 
                                          f1.getY()+0, true, false);
        GLWindow f3 = runTestGL(animator, f1.getX()+0, 
                                          f1.getY()+height+insets.getTotalHeight(), true, false);
        try {
            Thread.sleep(duration);
        } catch(Exception e) {
            e.printStackTrace();
        }
        animator.stop();

        f1.destroy();
        f2.destroy();
        f3.destroy();
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f1, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f1, false));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f2, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f2, false));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f3, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f3, false));

        releaseShared();
    }
    
    @Test
    public void testEachWithAnimatorSharedOffscreen() throws InterruptedException {
        VersionNumber osx1070 = new VersionNumber(10,7,0);
        if( Platform.getOSType() == Platform.OSType.MACOS && Platform.getOSVersionNumber().compareTo(osx1070) > 0 ) {
            // instable on OSX .. driver/OS bug when multi threading (3 animator)
            System.err.println("Shared context w/ 3 context each running in there own thread is instable here on OSX 10.7.4/NVidia,");
            System.err.println("SIGSEGV @ glDrawArrays / glBindBuffer .. any shared VBO.");
            System.err.println("Seems to run fine on 10.6.8/NVidia.");
            return;
        }
        initShared(false);
        Animator animator1 = new Animator();
        Animator animator2 = new Animator();
        Animator animator3 = new Animator();
        GLWindow f1 = runTestGL(animator1, 0, 0, true, false);
        InsetsImmutable insets = f1.getInsets();
        GLWindow f2 = runTestGL(animator2, f1.getX()+width+insets.getTotalWidth(), 
                                f1.getY()+0, true, false);
        GLWindow f3 = runTestGL(animator3, f1.getX()+0, 
                                f1.getY()+height+insets.getTotalHeight(), true, false);

        try {
            Thread.sleep(duration);
        } catch(Exception e) {
            e.printStackTrace();
        }
        animator1.stop();
        animator2.stop();
        animator3.stop();

        f1.destroy();
        f2.destroy();
        f3.destroy();
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f1, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f1, false));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f2, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f2, false));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f3, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f3, false));

        releaseShared();
    }
    
    static long duration = 2000; // ms

    public static void main(String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
        /**
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        System.err.println("Press enter to continue");
        System.err.println(stdin.readLine()); */         
        org.junit.runner.JUnitCore.main(TestSharedContextVBOES2NEWT.class.getName());
    }
}
