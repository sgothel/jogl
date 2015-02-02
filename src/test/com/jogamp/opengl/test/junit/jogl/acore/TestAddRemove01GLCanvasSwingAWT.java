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

package com.jogamp.opengl.test.junit.jogl.acore;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import jogamp.nativewindow.jawt.JAWTUtil;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestAddRemove01GLCanvasSwingAWT extends UITestCase {
    static long durationPerTest = 100;
    static int addRemoveCount = 15;
    static int pauseEach = 0;
    static int pauseDuration = 500;
    static boolean noOnscreenTest = false;
    static boolean noOffscreenTest = false;
    static boolean offscreenPBufferOnly = false;
    static boolean offscreenFBOOnly = false;
    static GLProfile glpGL2, glpGL2ES2;
    static int width, height;
    static boolean waitForKey = false;
    static boolean waitForKeyPost = false;

    @BeforeClass
    public static void initClass() {
        width  = 640;
        height = 480;
        if(GLProfile.isAvailable(GLProfile.GL2ES2)) {
            glpGL2ES2 = GLProfile.get(GLProfile.GL2ES2);
            Assert.assertNotNull(glpGL2ES2);
        }
        if(GLProfile.isAvailable(GLProfile.GL2)) {
            glpGL2 = GLProfile.get(GLProfile.GL2);
            Assert.assertNotNull(glpGL2);
        }
    }

    @AfterClass
    public static void releaseClass() {
    }

    protected JPanel create(final JFrame[] top, final int width, final int height, final int num)
            throws InterruptedException, InvocationTargetException
    {
        final JPanel[] jPanel = new JPanel[] { null };
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    jPanel[0] = new JPanel();
                    jPanel[0].setLayout(new BorderLayout());

                    final JFrame jFrame1 = new JFrame("JFrame #"+num);
                    // jFrame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    jFrame1.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE); // equivalent to Frame, use windowClosing event!
                    jFrame1.getContentPane().add(jPanel[0]);
                    jFrame1.setSize(width, height);

                    top[0] = jFrame1;
                } } );
        return jPanel[0];
    }

    protected void add(final Container cont, final Component comp)
            throws InterruptedException, InvocationTargetException
    {
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    cont.add(comp, BorderLayout.CENTER);
                } } );
    }

    protected void dispose(final GLCanvas glc)
            throws InterruptedException, InvocationTargetException
    {
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    glc.destroy();
                } } );
    }

    protected void setVisible(final JFrame jFrame, final boolean visible) throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    if( visible ) {
                        jFrame.pack();
                        jFrame.validate();
                    }
                    jFrame.setVisible(visible);
                } } ) ;
    }

    protected void dispose(final JFrame jFrame) throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    jFrame.dispose();
                } } ) ;
    }

    protected void runTestGL(final boolean onscreen, final GLCapabilities caps, final int addRemoveOpCount)
            throws AWTException, InterruptedException, InvocationTargetException
    {
        if(waitForKey) {
            UITestCase.waitForKey("Start");
        }
        for(int i=0; i<addRemoveOpCount; i++) {
            int ti = 0;
            System.err.println("Loop."+(ti++)+" "+(i+1)+"/"+addRemoveOpCount);
            final GLCanvas glc = new GLCanvas(caps);
            Assert.assertNotNull(glc);
            if( !onscreen ) {
                glc.setShallUseOffscreenLayer(true);
            }
            final Dimension glc_sz = new Dimension(width, height);
            glc.setMinimumSize(glc_sz);
            glc.setPreferredSize(glc_sz);
            glc.setSize(glc_sz);
            final GearsES2 gears = new GearsES2(1);
            gears.setVerbose(false);
            glc.addGLEventListener(gears);

            final JFrame[] top = new JFrame[] { null };
            final Container glcCont = create(top, width, height, i);
            add(glcCont, glc);

            setVisible(top[0], true);

            final long t0 = System.currentTimeMillis();
            do {
                glc.display();
                Thread.sleep(10);
            } while ( ( System.currentTimeMillis() - t0 ) < durationPerTest ) ;

            System.err.println("Loop."+(ti++)+" "+(i+1)+"/"+addRemoveOpCount+": GLCanvas isOffscreenLayerSurfaceEnabled: "+glc.isOffscreenLayerSurfaceEnabled()+": "+glc.getChosenGLCapabilities());

            dispose(top[0]);

            if( 0 < pauseEach && 0 == i % pauseEach ) {
                System.err.println("******* P A U S E - Start ********");
                // OSXUtil.WaitUntilFinish();
                Thread.sleep(pauseDuration);
                System.err.println("******* P A U S E - End ********");
            }
        }
        if(waitForKeyPost) {
            UITestCase.waitForKey("End");
        }
    }

    @Test
    public void test01Onscreen()
            throws AWTException, InterruptedException, InvocationTargetException
    {
        if( noOnscreenTest || JAWTUtil.isOffscreenLayerRequired() ) {
            System.err.println("No onscreen test requested or platform doesn't support onscreen rendering.");
            return;
        }
        final GLCapabilities caps = new GLCapabilities(glpGL2ES2);
        runTestGL(true, caps, addRemoveCount);
    }

    @Test
    public void test02OffscreenFBO()
            throws AWTException, InterruptedException, InvocationTargetException
    {
        if( noOffscreenTest || !JAWTUtil.isOffscreenLayerSupported() ) {
            System.err.println("No offscreen test requested or platform doesn't support offscreen rendering.");
            return;
        }
        if( offscreenPBufferOnly ) {
            System.err.println("Only PBuffer test is requested.");
            return;
        }
        final GLCapabilities caps = new GLCapabilities(glpGL2ES2);
        if(offscreenPBufferOnly) {
            caps.setPBuffer(true);
            caps.setOnscreen(true); // simulate normal behavior ..
        }
        runTestGL(false, caps, addRemoveCount);
    }

    @Test
    public void test03OffscreenPBuffer()
            throws AWTException, InterruptedException, InvocationTargetException
    {
        if( noOffscreenTest || !JAWTUtil.isOffscreenLayerSupported() ) {
            System.err.println("No offscreen test requested or platform doesn't support offscreen rendering.");
            return;
        }
        if( offscreenFBOOnly ) {
            System.err.println("Only FBO test is requested.");
            return;
        }
        final GLCapabilities caps = new GLCapabilities(glpGL2);
        caps.setPBuffer(true);
        caps.setOnscreen(true); // simulate normal behavior ..
        runTestGL(false, caps, addRemoveCount);
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    durationPerTest = Long.parseLong(args[i]);
                } catch (final Exception ex) { ex.printStackTrace(); }
            } else if(args[i].equals("-loops")) {
                i++;
                addRemoveCount = MiscUtils.atoi(args[i], addRemoveCount);
            } else if(args[i].equals("-pauseEach")) {
                i++;
                pauseEach = MiscUtils.atoi(args[i], pauseEach);
            } else if(args[i].equals("-pauseDuration")) {
                i++;
                pauseDuration = MiscUtils.atoi(args[i], pauseDuration);
            } else if(args[i].equals("-noOnscreen")) {
                noOnscreenTest = true;
            } else if(args[i].equals("-noOffscreen")) {
                noOffscreenTest = true;
            } else if(args[i].equals("-layeredFBO")) {
                offscreenFBOOnly = true;
            } else if(args[i].equals("-layeredPBuffer")) {
                offscreenPBufferOnly = true;
            } else if(args[i].equals("-wait")) {
                waitForKey = true;
            } else if(args[i].equals("-waitPost")) {
                waitForKeyPost = true;
            }
        }
        System.err.println("waitForKey                    "+waitForKey);
        System.err.println("waitForKeyPost                "+waitForKeyPost);

        System.err.println("addRemoveCount                "+addRemoveCount);
        System.err.println("pauseEach                     "+pauseEach);
        System.err.println("pauseDuration                 "+pauseDuration);

        System.err.println("noOnscreenTest                "+noOnscreenTest);
        System.err.println("noOffscreenTest               "+noOffscreenTest);
        System.err.println("offscreenPBufferOnly          "+offscreenPBufferOnly);
        System.err.println("offscreenFBOOnly              "+offscreenFBOOnly);

        org.junit.runner.JUnitCore.main(TestAddRemove01GLCanvasSwingAWT.class.getName());
    }
}
