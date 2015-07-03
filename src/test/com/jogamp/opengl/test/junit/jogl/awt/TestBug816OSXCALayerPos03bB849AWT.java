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

package com.jogamp.opengl.test.junit.jogl.awt;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.event.TraceWindowAdapter;
import com.jogamp.newt.event.awt.AWTWindowAdapter;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

/**
 * AWT Frame BorderLayout w/ Checkbox North, Panel.GLCanvas Center.
 * <p>
 * Checkbox toggles GLCanvas's parent panel's visibility state.
 * </p>
 * <p>
 * Validates bugs:
 * <ul>
 *   <li>Bug 816: OSX CALayer Positioning Bug</li>
 *   <li>Bug 729: OSX CALayer shall honor the Component's visibility state</li>
 *   <li>Bug 849: AWT GLAutoDrawables (JAWTWindow) shall honor it's parent visibility state</li>
 * </ul>
 * </p>
 * <p>
 * Diff. OSX CALayer positioning w/ java6, [7uxx..7u40[, and >= 7u40
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBug816OSXCALayerPos03bB849AWT extends UITestCase {
    static long duration = 1600; // ms
    static int width=640, height=480;

    @Test
    public void test() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(getGLP());

        final Frame frame = new Frame("TestBug816OSXCALayerPos03bAWT");
        Assert.assertNotNull(frame);

        final GLCanvas glCanvas1 = new GLCanvas(caps);
        Assert.assertNotNull(glCanvas1);
        glCanvas1.addGLEventListener(new GearsES2(1));
        // Put it in a panel
        final Panel panel = new Panel(new GridLayout(1, 1));
        panel.add(glCanvas1);

        final Animator animator = new Animator();
        animator.add(glCanvas1);

        final QuitAdapter quitAdapter = new QuitAdapter();
        new AWTWindowAdapter(new TraceWindowAdapter(quitAdapter), glCanvas1).addTo(frame);

        // Create a check box that hides / shows canvas
        final Checkbox checkbox = new Checkbox("Visible canvas", true);
        checkbox.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent ev) {
                final boolean visible = checkbox.getState();
                System.err.println("XXXX Panel setVisible "+visible);
                panel.setVisible(visible);
                System.err.println("XXXX Visible: [panel "+panel.isVisible()+", canvas "+glCanvas1.isVisible()+"]; Displayable: [panel "+panel.isDisplayable()+", canvas "+glCanvas1.isDisplayable()+"]");
                if( panel.isVisible() ) {
                    frame.validate(); // take care of resized frame while hidden
                }
            }
        });

        // Build a GUI that displays canvas and check box
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);
        frame.add(checkbox, BorderLayout.NORTH);

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.setSize(width, height);
                frame.setVisible(true);
            }});
        Assert.assertEquals(true,  AWTRobotUtil.waitForVisible(frame, true));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glCanvas1, true));

        animator.start();
        Assert.assertTrue(animator.isStarted());
        Assert.assertTrue(animator.isAnimating());

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while(!quitAdapter.shouldQuit() && t1 - t0 < duration) {
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
        }

        Assert.assertNotNull(frame);
        Assert.assertNotNull(glCanvas1);

        Assert.assertNotNull(animator);
        animator.stop();
        Assert.assertFalse(animator.isAnimating());
        Assert.assertFalse(animator.isStarted());

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.setVisible(false);
            }});
        Assert.assertEquals(false, frame.isVisible());
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.remove(panel);
                frame.dispose();
            }});
    }

    static GLProfile getGLP() {
        return GLProfile.getMaxProgrammableCore(true);
    }

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            }
        }

        org.junit.runner.JUnitCore.main(TestBug816OSXCALayerPos03bB849AWT.class.getName());
    }
}
