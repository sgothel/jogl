/**
 * Copyright 2010-2023 JogAmp Community. All rights reserved.
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

import java.awt.AWTException;
import java.awt.Robot;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.util.InterruptSource;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.NewtTestUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestDestroyGLAutoDrawableNewtAWT extends UITestCase {

    static long duration = 100; // ms

    static void destroyGLAD(final GLAutoDrawable glad, final int mode) {
        final String tname = Thread.currentThread().toString();
        System.err.println(tname+": Destroy mode "+mode+": Start: Realised "+glad.isRealized());
        glad.destroy();
        System.err.println(tname+": Destroy mode "+mode+": End: Realised "+glad.isRealized());
    }

    /**
     *
     * @param destroyMode 0 on-thread, 1 render-thread, 2 edt-thread, 3 external-thread, 10 key-press, 11 mouse-click
     * @throws InterruptedException
     * @throws AWTException
     * @throws InvocationTargetException
     */
    protected void runTestGL(final int destroyMode) throws InterruptedException, InvocationTargetException, AWTException {
        final Robot robot = new Robot();
        robot.setAutoWaitForIdle(true);

        final GLCapabilities caps = new GLCapabilities(GLProfile.getGL2ES2());

        final GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("TestDestroyGLAutoDrawableNewtAWT Mode "+destroyMode);
        glWindow.addGLEventListener(new RedSquareES2().setVerbose(false));

        final short quitKey = KeyEvent.VK_Q;

        glWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyNotify(final WindowEvent e) {
                System.err.println("Window DestroyNotify: "+e);
            }

            @Override
            public void windowDestroyed(final WindowEvent e) {
                System.err.println("Window Destroyed: "+e);
            }

            @Override
            public void windowGainedFocus(final WindowEvent e) {
                System.err.println("Window Focus Gained: "+e);
            }

            @Override
            public void windowLostFocus(final WindowEvent e) {
                System.err.println("Window Focus Lost: "+e);
            }
        });
        final KeyListener keyAction = new KeyListener() {
            @Override
            public void keyPressed(final KeyEvent e) {
                System.err.println("KEY PRESSED: "+e);
            }
            @Override
            public void keyReleased(final KeyEvent e) {
                System.err.println("KEY RELEASED: "+e);
                if( e.isAutoRepeat() ) {
                    return;
                }
                switch(e.getKeyCode()) {
                    case quitKey:
                        destroyGLAD(glWindow, 10);
                        break;
                }
            }
        };
        final MouseListener mouseAction = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                System.err.println("MOUSE PRESSED: "+e);
            }
            @Override
            public void mouseReleased(final MouseEvent e) {
                System.err.println("MOUSE RELEASED: "+e);
            }
            @Override
            public void mouseClicked(final MouseEvent e) {
                System.err.println("MOUSE CLICKED: "+e);
                destroyGLAD(glWindow, 11);
            }
        };

        glWindow.setSize(256, 256);
        glWindow.setVisible(true);
        Assert.assertEquals(true,  NewtTestUtil.waitForRealized(glWindow, true, null));

        final Animator animator = new Animator(glWindow);
        animator.setUpdateFPSFrames(30, null);
        animator.start();

        while(animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(30);
        }

        System.err.println("AWT Robot Init");
        final java.awt.Point objCenter;
        {
            final int[] oc = AWTRobotUtil.getCenterLocation(glWindow, false /* onTitleBarIfWindow */);
            objCenter = new java.awt.Point(oc[0], oc[1]);
        }
        AWTRobotUtil.awtRobotMouseMove(robot, objCenter.x, objCenter.y); // Bug 919: Reset mouse position
        AWTRobotUtil.assertRequestFocusAndWait(null, glWindow, glWindow, null, null);  // programmatic
        AWTRobotUtil.awtRobotMouseMove(robot, objCenter.x, objCenter.y); // Bug 919: Reset mouse position
        AWTRobotUtil.waitForIdle(robot);
        System.err.println("AWT Robot OK");

        switch( destroyMode ) {
            case 1: {
                    // Since we pull the resources under the GLAutoDrawable
                    // while destroying it within the display call - it causes an exception:
                    // - WindowImpl.getGraphicsConfiguration(WindowImpl.java:1173) (NPE this.config)
                    // - ...
                    // - GLDrawableImpl.unlockSurface(GLDrawableImpl.java:340) ->
                    animator.stop(); // let's have the exception thrown here to catch it, not on animator
                    try {
                        glWindow.invoke(true, (final GLAutoDrawable glad) -> {  destroyGLAD(glad, 1); return true; } );
                    } catch( final GLException gle ) {
                        // OK, since
                        System.err.println("Expected exception: "+gle.getMessage());
                    }
                }
                break;
            case 2: {
                    glWindow.runOnEDTIfAvail(true, () -> { destroyGLAD(glWindow, 2); } );
                }
                break;
            case 3: {
                    new InterruptSource.Thread( () -> { destroyGLAD(glWindow, 3); } ).start();
                }
                break;
            case 10: {
                    glWindow.addKeyListener(keyAction);
                    AWTRobotUtil.waitForIdle(robot);
                    AWTRobotUtil.newtKeyPress(0, robot, true, quitKey, 10);
                    AWTRobotUtil.newtKeyPress(0, robot, false, quitKey, 100);
                }
                break;
            case 11: {
                    glWindow.addMouseListener(mouseAction);
                    AWTRobotUtil.waitForIdle(robot);
                    AWTRobotUtil.mouseClick(robot, objCenter, 1, AWTRobotUtil.ROBOT_DELAY, 0);
                }
                break;
            default: {
                    destroyGLAD(glWindow, 0);
                }
                break;
        }
        Assert.assertEquals(true,  NewtTestUtil.waitForRealized(glWindow, false, null));
        Assert.assertEquals(false,  glWindow.isNativeValid());
        animator.stop(); // Avoiding a ThreadDeath of animator at shutdown
        Assert.assertEquals(false,  animator.isAnimating());
    }

    @Test
    public void test00OnThread() throws InterruptedException, InvocationTargetException, AWTException {
        runTestGL(0);
    }

    @Test
    public void test01RenderThread() throws InterruptedException, InvocationTargetException, AWTException {
        runTestGL(1);
    }

    @Test
    public void test02EDTThread() throws InterruptedException, InvocationTargetException, AWTException {
        runTestGL(2);
    }

    @Test
    public void test03ExtThread() throws InterruptedException, InvocationTargetException, AWTException {
        runTestGL(3);
    }

    @Test
    public void test10EDTKeyEvent() throws InterruptedException, InvocationTargetException, AWTException {
        runTestGL(10);
    }

    @Test
    public void test11EDTMouseEvent() throws InterruptedException, InvocationTargetException, AWTException {
        runTestGL(11);
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestDestroyGLAutoDrawableNewtAWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
