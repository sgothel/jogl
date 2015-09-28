/**
 * Copyright 2015 JogAmp Community. All rights reserved.
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Label;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;

import junit.framework.Assert;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.util.InterruptedRuntimeException;
import com.jogamp.common.util.SourcedInterruptedException;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;

import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Test to check if interrupt on AWT-EventQueue causes a malfunction in JOGL.
 * <p>
 * After tests are displaying an ever color rotating rectangle in an AWT component alone
 * and with an additional GearsES2 within a GLCanvas.
 * </p>
 * <p>
 * The AWT component is issuing an interrupt during paint on the AWT-EDT.
 * </p>
 * <p>
 * The reporter claims that an interrupt on the AWT-EDT shall not disturb neither AWT nor JOGL's GLCanvas
 * and rendering shall continue.
 * This seems to be true.
 * </p>
 * <p>
 * The test passes on GNU/Linux and Windows using JRE 1.8.0_60.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBug1225EventQueueInterruptedAWT extends UITestCase {
    static long durationPerTest = 1000; // ms

    private void setVisible(final JFrame frame, final boolean v) throws InterruptedException, InvocationTargetException {
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.pack();
                // frame.setSize(new Dimension(800, 600));
                frame.setVisible(v);
            }});
    }
    private void dispose(final JFrame jFrame) throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    jFrame.dispose();
                } } ) ;
    }


    @Test
    public void test01_NoGL() throws InterruptedException, InvocationTargetException {
        testImpl(false);
    }

    @Test
    public void test02_WithGL() throws InterruptedException, InvocationTargetException {
        testImpl(true);
    }

    class OurUncaughtExceptionHandler implements UncaughtExceptionHandler {
        public volatile Thread thread = null;
        public volatile Throwable exception = null;

        @Override
        public void uncaughtException(final Thread t, final Throwable e) {
            thread = t;
            exception = e;
            System.err.println("*** UncaughtException (this Thread "+Thread.currentThread().getName()+") : Thread <"+t.getName()+">, "+e.getClass().getName()+": "+e.getMessage());
            ExceptionUtils.dumpThrowable("", e);
        }
    }
    void testImpl(final boolean useGL) throws InterruptedException, InvocationTargetException {
        AWTRobotUtil.validateAWTEDTIsAlive();
        final OurUncaughtExceptionHandler uncaughtHandler = new OurUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler( uncaughtHandler );

        final Dimension csize = new Dimension(800, 400);
        final JPanel panel = new JPanel(new GridLayout(2, 1));
        final GLCanvas glc;
        if( useGL ) {
            glc = new GLCanvas();
            {
                final GearsES2 gears = new GearsES2();
                gears.setVerbose(false);
                glc.addGLEventListener(gears);
            }
            glc.addGLEventListener(new GLEventListener() {
                @Override
                public void init(final GLAutoDrawable drawable) {
                }
                @Override
                public void dispose(final GLAutoDrawable drawable) {
                }
                @Override
                public void display(final GLAutoDrawable drawable) {
                    if( Thread.interrupted() ) {
                        final InterruptedRuntimeException e = new InterruptedRuntimeException(new InterruptedException("Interrupt detected in GLEventListener, thread: "+Thread.currentThread().getName()));
                        ExceptionUtils.dumpThrowable("", e);
                        throw e;
                    }
                }
                @Override
                public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
                }

            });
            glc.setSize(csize);
            glc.setPreferredSize(csize);
            panel.add(glc);
        } else {
            NativeWindowFactory.initSingleton();
            glc = null;
            final Label l = new Label("No GL Object");
            l.setSize(csize);
            l.setPreferredSize(csize);
            panel.add(l);
        }
        final InterruptingComponent icomp = new InterruptingComponent();
        panel.add(icomp);
        icomp.setSize(csize);
        icomp.setPreferredSize(csize);

        final JFrame frame = new JFrame();
        frame.setResizable(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.getContentPane().add(panel, BorderLayout.CENTER);
        setVisible(frame, true);
        if( useGL ) {
            Assert.assertTrue(AWTRobotUtil.waitForRealized(glc, true));
        }
        Assert.assertTrue(AWTRobotUtil.waitForRealized(icomp, true));

        final InterruptableLoop loop = new InterruptableLoop(icomp, glc);
        final Thread thread = new Thread(loop);
        thread.start();

        for(int i=0; thread.isAlive() && null == loop.exception && null == uncaughtHandler.exception && i<100; i++) {
            icomp.interruptAWTEventQueue();
            Thread.sleep(durationPerTest/100);
        }
        if(thread.isAlive()) {
            System.err.println("Manually stopping loop");
            loop.stop();
            thread.join();
        }
        dispose(frame);

        if( null != loop.exception ) {
            ExceptionUtils.dumpThrowable("loop", loop.exception);
        }
        if( null != uncaughtHandler.exception ) {
            ExceptionUtils.dumpThrowable("uncaughtHandler", uncaughtHandler.exception);
        }
        Assert.assertNull("Caught Exception in loop", loop.exception);
        Assert.assertNull("Caught Exception via uncaughtHandler", uncaughtHandler.exception);
    }

    static class InterruptableLoop implements Runnable {
        public volatile Exception exception = null;
        public volatile boolean shallStop = false;
        final InterruptingComponent icomp;
        final GLCanvas glc;
        boolean alt = false;;

        InterruptableLoop(final InterruptingComponent icomp, final GLCanvas glc) {
            this.icomp = icomp;
            this.glc = glc;
        }

        public void stop() {
            shallStop = true;
        }

        @Override
        public void run()
        {
            try
            {
                while( !shallStop ) {
                    if( alt ) {
                        icomp.repaint(); // issues paint of GLCanvas on AWT-EDT
                    } else if( null != glc ) {
                        glc.display(); // issues paint of GLCanvas on AWT-EDT
                    }
                    alt = !alt;
                    Thread.sleep(16);
                    if( Thread.interrupted() ) {
                        final InterruptedRuntimeException e = new InterruptedRuntimeException(new InterruptedException("Interrupt detected in loop, thread: "+Thread.currentThread().getName()));
                        throw e;
                    }
                }
            }
            catch (final InterruptedException e) {
                exception = SourcedInterruptedException.wrap(e);
                ExceptionUtils.dumpThrowable("", exception);
            } catch (final Exception e) {
                exception = e;
                ExceptionUtils.dumpThrowable("", exception);
            }
        }
    }

    static class InterruptingComponent extends Component {
        private static final long serialVersionUID = 1L;

        private volatile boolean doInterrupt = false;

        private final Color[] colors =
                new Color[] { Color.BLACK, Color.BLUE, Color.DARK_GRAY, Color.GRAY, Color.LIGHT_GRAY };
        private int colorIdx = 0;

        public InterruptingComponent() {
        }

        public void interruptAWTEventQueue() {
            doInterrupt = true;
        }

        @Override
        public void paint(final Graphics g)
        {
            if( Thread.interrupted() ) {
                final InterruptedRuntimeException e = new InterruptedRuntimeException(new InterruptedException("Interrupt detected in AWT Component, thread: "+Thread.currentThread().getName()));
                ExceptionUtils.dumpThrowable("", e);
                throw e;
            }

            g.setColor(colors[colorIdx++]);
            if( colorIdx >= colors.length ) {
                colorIdx = 0;
            }
            g.fillRect(0, 0, getWidth(), getHeight());

            if(doInterrupt) {
                final Thread c = Thread.currentThread();
                System.err.println("Thread "+c.getName()+": *Interrupting*");
                doInterrupt = false;
                c.interrupt();
            }
        }
    }

    public static void main(final String[] args) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = MiscUtils.atol(args[++i], durationPerTest);
            }
        }
        org.junit.runner.JUnitCore.main(TestBug1225EventQueueInterruptedAWT.class.getName());
    }
}

