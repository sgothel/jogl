/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.DisplayMode;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.Timer;
import java.util.TimerTask;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;

import org.junit.Assume;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.os.Platform;
import com.jogamp.common.util.VersionNumber;
import com.jogamp.common.util.awt.AWTEDTExecutor;
import com.jogamp.opengl.util.AnimatorBase;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * Sample program that relies on JOGL's mechanism to handle the OpenGL context
 * and rendering loop when using an AWT canvas attached to an Applet.
 * <p>
 * BUG on OSX/CALayer w/ Java6:
 * If frame.setTitle() is issued right after initialization the call hangs in
 * <pre>
 * at apple.awt.CWindow._setTitle(Native Method)
 *  at apple.awt.CWindow.setTitle(CWindow.java:765) [1.6.0_37, build 1.6.0_37-b06-434-11M3909]
 * </pre>
 * </p>
 * <p>
 * OSX/CALayer is forced by using an Applet component in this unit test.
 * </p>
 * <p>
 * Similar deadlock has been experienced w/ other mutable operation on an AWT Container owning a GLCanvas child,
 * e.g. setResizable*().
 * </p>
 * <p>
 * Users shall make sure all mutable AWT calls are performed on the EDT, even before 1st setVisible(true) !
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLCanvasAWTActionDeadlock02AWT extends UITestCase {
  static int framesPerTest = 240; // frames

  static class MiniPApplet extends Applet implements MouseMotionListener, KeyListener {
      private static final long serialVersionUID = 1L;

      /////////////////////////////////////////////////////////////
      //
      // Test parameters

      public int frameRate           = 120;
      public int numSamples          = 4;

      public boolean fullScreen      = false;
      public boolean useAnimator     = true;
      public boolean resizeableFrame = true;

      public boolean restartCanvas   = true;
      public int restartTimeout      = 100; // in number of frames.

      public boolean printThreadInfo = false;
      public boolean printEventInfo  = false;

      /////////////////////////////////////////////////////////////
      //
      // Internal variables

      int width;
      int height;

      String OPENGL_VENDOR;
      String OPENGL_RENDERER;
      String OPENGL_VERSION;
      String OPENGL_EXTENSIONS;

      int currentSamples = -1;

      private Frame frame;
      private GLProfile profile;
      private GLCapabilities capabilities;
      private GLCanvas canvas;

      private SimpleListener listener;
      private CustomAnimator animator;

      private long beforeTime;
      private long overSleepTime;
      private final long frameRatePeriod = 1000000000L / frameRate;

      private boolean initialized = false;
      private boolean osxCALayerAWTModBug = false;
      boolean justInitialized = true;

      private double theta = 0;
      private double s = 0;
      private double c = 0;

      private long millisOffset;
      private int fcount, lastm;
      private float frate;
      private final int fint = 3;

      private boolean setFramerate = false;
      private boolean restarted = false;

      private int frameCount = 0;

      void run() throws InterruptedException, InvocationTargetException {
        // Thread loop = new Thread("Animation Thread") {
          // public void run() {
            frameCount = 0;
            while ( frameCount < framesPerTest ) {
              if (!initialized) {
                setup();
              }

              if (restartCanvas && restartTimeout == frameCount) {
                restart();
              }

              if (useAnimator) {
                animator.requestRender();
              } else {
                canvas.display();
              }

              clock();

              frameCount++;
              if( null == frame ) {
                  break;
              }
            }
            dispose();
          // }
        // };
        // loop.start();
      }

      void setup() throws InterruptedException, InvocationTargetException {
        if (printThreadInfo) System.out.println("Current thread at setup(): " + Thread.currentThread());

        millisOffset = System.currentTimeMillis();

        final VersionNumber version170 = new VersionNumber(1, 7, 0);
        osxCALayerAWTModBug = Platform.OSType.MACOS == Platform.getOSType() &&
                              0 > Platform.getJavaVersionNumber().compareTo(version170);
        System.err.println("OSX CALayer AWT-Mod Bug "+osxCALayerAWTModBug);
        System.err.println("OSType "+Platform.getOSType());
        System.err.println("Java Version "+Platform.getJavaVersionNumber());

        // Frame setup ----------------------------------------------------------

        width = 300;
        height = 300;
        final MiniPApplet applet = this;

        final GraphicsEnvironment environment =
          GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice displayDevice = environment.getDefaultScreenDevice();
        frame = new Frame(displayDevice.getDefaultConfiguration());

        final Rectangle fullScreenRect;
        if (fullScreen) {
          final DisplayMode mode = displayDevice.getDisplayMode();
          fullScreenRect = new Rectangle(0, 0, mode.getWidth(), mode.getHeight());
        } else {
          fullScreenRect = null;
        }
        // All AWT Mods on AWT-EDT, especially due to the follow-up complicated code!
        AWTEDTExecutor.singleton.invoke(true, new Runnable() {
            public void run() {
                frame.setTitle("MiniPApplet");
            } } );
        if (fullScreen) {
            try {
                javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        frame.setUndecorated(true);
                        frame.setBackground(Color.GRAY);
                        frame.setBounds(fullScreenRect);
                        frame.setVisible(true);
                    }});
            } catch (final Throwable t) {
                t.printStackTrace();
                Assume.assumeNoException(t);
            }
        }
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setLayout(null);
                    frame.add(applet);
                    if (fullScreen) {
                      frame.invalidate();
                    } else {
                      frame.pack();
                    }
                    frame.setResizable(resizeableFrame);
                    if (fullScreen) {
                      // After the pack(), the screen bounds are gonna be 0s
                      frame.setBounds(fullScreenRect);
                      applet.setBounds((fullScreenRect.width - applet.width) / 2,
                                       (fullScreenRect.height - applet.height) / 2,
                                       applet.width, applet.height);
                    } else {
                      final Insets insets = frame.getInsets();

                      final int windowW = applet.width + insets.left + insets.right;
                      final int windowH = applet.height + insets.top + insets.bottom;
                      final int locationX = 100;
                      final int locationY = 100;

                      frame.setSize(windowW, windowH);
                      frame.setLocation(locationX, locationY);

                      final int usableWindowH = windowH - insets.top - insets.bottom;
                      applet.setBounds((windowW - width)/2, insets.top + (usableWindowH - height)/2, width, height);
                    }
                }});
        } catch (final Throwable t) {
            t.printStackTrace();
            Assume.assumeNoException(t);
        }


        frame.add(this);
        frame.addWindowListener(new WindowAdapter() {
          public void windowClosing(final WindowEvent e) {
              try {
                  dispose();
              } catch (final Exception ex) {
                  Assume.assumeNoException(ex);
              }
          }
        });

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.setVisible(true);
            } } );

        // Canvas setup ----------------------------------------------------------

        profile = GLProfile.getDefault();
        capabilities = new GLCapabilities(profile);
        capabilities.setSampleBuffers(true);
        capabilities.setNumSamples(numSamples);
        capabilities.setDepthBits(24);
        // capabilities.setStencilBits(8); // No Stencil on OSX w/ hw-accel !
        capabilities.setAlphaBits(8);

        canvas = new GLCanvas(capabilities);
        canvas.setBounds(0, 0, width, height);

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                MiniPApplet.this.setLayout(new BorderLayout());
                MiniPApplet.this.add(canvas, BorderLayout.CENTER);
                MiniPApplet.this.validate();
            } } );
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);

        // Setting up animation
        listener = new SimpleListener();
        canvas.addGLEventListener(listener);
        if (useAnimator) {
          animator = new CustomAnimator(canvas);
          animator.start();
        }
        initialized = true;
      }

      void restart() throws InterruptedException, InvocationTargetException {
        System.out.println("Restarting surface...");

        // Stopping animation, removing current canvas.
        if (useAnimator) {
          animator.stop();
          animator.remove(canvas);
        }
        canvas.disposeGLEventListener(listener, true);
        this.remove(canvas);

        capabilities = new GLCapabilities(profile);
        capabilities.setSampleBuffers(true);
        capabilities.setNumSamples(numSamples);

        canvas = new GLCanvas(capabilities);
        canvas.setBounds(0, 0, width, height);

        // Setting up animation again
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                MiniPApplet.this.setLayout(new BorderLayout());
                MiniPApplet.this.add(canvas, BorderLayout.CENTER);
                MiniPApplet.this.validate();
            } } );
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);

        canvas.addGLEventListener(listener);
        if (useAnimator) {
          animator.add(canvas);
          animator.start();
        }

        setFramerate = false;
        restarted = true;

        System.out.println("Done");
      }

      void dispose() throws InterruptedException, InvocationTargetException {
        if( null == frame ) {
            return;
        }

        // Stopping animation, removing current canvas.
        if (useAnimator) {
          animator.stop();
          animator.remove(canvas);
        }
        canvas.removeGLEventListener(listener);
        if( EventQueue.isDispatchThread() ) {
            MiniPApplet.this.remove(canvas);
            frame.remove(MiniPApplet.this);
            frame.validate();
            frame.dispose();
            frame = null;
        } else {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    MiniPApplet.this.remove(canvas);
                    frame.remove(MiniPApplet.this);
                    frame.validate();
                    frame.dispose();
                    frame = null;
                }});
        }
      }

      void draw(final GL2 gl) {
        if( !osxCALayerAWTModBug || !justInitialized ) {
            AWTEDTExecutor.singleton.invoke(true, new Runnable() {
                public void run() {
                    frame.setTitle("frame " + frameCount);
                } } );
        }

        if (printThreadInfo) System.out.println("Current thread at draw(): " + Thread.currentThread());

        if (OPENGL_VENDOR == null) {
          OPENGL_VENDOR     = gl.glGetString(GL.GL_VENDOR);
          OPENGL_RENDERER   = gl.glGetString(GL.GL_RENDERER);
          OPENGL_VERSION    = gl.glGetString(GL.GL_VERSION);
          OPENGL_EXTENSIONS = gl.glGetString(GL.GL_EXTENSIONS);
          System.out.println(OPENGL_VENDOR);
          System.out.println(OPENGL_RENDERER);
          System.out.println(OPENGL_VERSION);
          System.out.println(OPENGL_EXTENSIONS);

          final int[] temp = { 0 };
          gl.glGetIntegerv(GL2ES3.GL_MAX_SAMPLES, temp, 0);
          System.out.println("Maximum number of samples supported by the hardware: " + temp[0]);
          System.out.println("Frame: "+frame);
          System.out.println("Applet: "+MiniPApplet.this);
          System.out.println("GLCanvas: "+canvas);
          System.out.println("GLDrawable: "+canvas.getDelegatedDrawable());
        }

        if (currentSamples == -1) {
          final int[] temp = { 0 };
          gl.glGetIntegerv(GL.GL_SAMPLES, temp, 0);
          currentSamples = temp[0];
          if (numSamples != currentSamples) {
            System.err.println("Requested sampling level " + numSamples + " not supported. Using " + currentSamples + " samples instead.");
          }
        }

        if (!setFramerate) {
          if (60 < frameRate) {
            // Disables vsync
            gl.setSwapInterval(0);
          } else if (30 < frameRate) {
            gl.setSwapInterval(1);
          } else {
            gl.setSwapInterval(2);
          }
          setFramerate = true;
        }

        if (restarted) {
          final int[] temp = { 0 };
          gl.glGetIntegerv(GL.GL_SAMPLES, temp, 0);
          if (numSamples != temp[0]) {
            System.err.println("Multisampling level requested " + numSamples + " not supported. Using " + temp[0] + "samples instead.");
          }
        }

        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);

        theta += 0.01;
        s = Math.sin(theta);
        c = Math.cos(theta);

        gl.glBegin(GL.GL_TRIANGLES);
        gl.glColor3f(1, 0, 0);
        gl.glVertex2d(-c, -c);
        gl.glColor3f(0, 1, 0);
        gl.glVertex2d(0, c);
        gl.glColor3f(0, 0, 1);
        gl.glVertex2d(s, -s);
        gl.glEnd();

        gl.glFlush();

        fcount += 1;
        final int m = (int) (System.currentTimeMillis() - millisOffset);
        if (m - lastm > 1000 * fint) {
          frate = (float)(fcount) / fint;
          fcount = 0;
          lastm = m;
          System.err.println("fps: " + frate);
        }
      }

      void clock() {
        final long afterTime = System.nanoTime();
        final long timeDiff = afterTime - beforeTime;
        final long sleepTime = (frameRatePeriod - timeDiff) - overSleepTime;

        if (sleepTime > 0) {  // some time left in this cycle
          try {
            Thread.sleep(sleepTime / 1000000L, (int) (sleepTime % 1000000L));
          } catch (final InterruptedException ex) { }

          overSleepTime = (System.nanoTime() - afterTime) - sleepTime;

        } else {    // sleepTime <= 0; the frame took longer than the period
          overSleepTime = 0L;
        }

        beforeTime = System.nanoTime();
      }

      class SimpleListener implements GLEventListener {
        @Override
        public void display(final GLAutoDrawable drawable) {
            draw(drawable.getGL().getGL2());
            justInitialized = false;
        }

        @Override
        public void dispose(final GLAutoDrawable drawable) { }

        @Override
        public void init(final GLAutoDrawable drawable) {
            justInitialized = true;
        }

        @Override
        public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int w, final int h) { }
      }

      public void mouseDragged(final MouseEvent ev) {
        if (printEventInfo) {
          System.err.println("Mouse dragged event: " + ev);
        }
      }

      public void mouseMoved(final MouseEvent ev) {
        if (printEventInfo) {
          System.err.println("Mouse moved event: " + ev);
        }
      }

      public void keyPressed(final KeyEvent ev) {
        if (printEventInfo) {
          System.err.println("Key pressed event: " + ev);
        }
      }

      public void keyReleased(final KeyEvent ev) {
        if (printEventInfo) {
          System.err.println("Key released event: " + ev);
        }
      }

      public void keyTyped(final KeyEvent ev) {
        if (printEventInfo) {
          System.err.println("Key typed event: " + ev);
        }
      }

      /** An Animator subclass which renders one frame at the time
       *  upon calls to the requestRender() method.
       **/
      public static class CustomAnimator extends AnimatorBase {
          private Timer timer = null;
          private TimerTask task = null;
          private volatile boolean shouldRun;

          protected String getBaseName(final String prefix) {
              return "Custom" + prefix + "Animator" ;
          }

          /** Creates an CustomAnimator with an initial drawable to
           * animate. */
          public CustomAnimator(final GLAutoDrawable drawable) {
              if (drawable != null) {
                  add(drawable);
              }
          }

          public synchronized void requestRender() {
              shouldRun = true;
          }

          public final synchronized boolean isStarted() {
              return (timer != null);
          }

          public final synchronized boolean isAnimating() {
              return (timer != null) && (task != null);
          }

          private void startTask() {
              if(null != task) {
                  return;
              }

              task = new TimerTask() {
                  private boolean firstRun = true;
                  public void run() {
                      if (firstRun) {
                        Thread.currentThread().setName("OPENGL");
                        firstRun = false;
                      }
                      if(CustomAnimator.this.shouldRun) {
                         CustomAnimator.this.animThread = Thread.currentThread();
                          // display impl. uses synchronized block on the animator instance
                          display();
                          synchronized (this) {
                            // done with current frame.
                            shouldRun = false;
                          }
                      }
                  }
              };

              fpsCounter.resetFPSCounter();
              shouldRun = false;

              timer.schedule(task, 0, 1);
          }

          public synchronized boolean  start() {
              if (timer != null) {
                  return false;
              }
              timer = new Timer();
              startTask();
              return true;
          }

          /** Stops this CustomAnimator. */
          public synchronized boolean stop() {
              if (timer == null) {
                  return false;
              }
              shouldRun = false;
              if(null != task) {
                  task.cancel();
                  task = null;
              }
              if(null != timer) {
                  timer.cancel();
                  timer = null;
              }
              animThread = null;
              try {
                  Thread.sleep(20); // ~ 1/60 hz wait, since we can't ctrl stopped threads / holding the lock is OK here!
              } catch (final InterruptedException e) { }
              return true;
          }

          public final synchronized boolean isPaused() { return false; }
          public synchronized boolean resume() { return false; }
          public synchronized boolean pause() { return false; }
      }
  }

  @Test
  public void test00() {
    TestGLCanvasAWTActionDeadlock02AWT.MiniPApplet mini;
    try {
      final Class<?> c = Thread.currentThread().getContextClassLoader().loadClass(TestGLCanvasAWTActionDeadlock02AWT.MiniPApplet.class.getName());
      mini = (TestGLCanvasAWTActionDeadlock02AWT.MiniPApplet) c.newInstance();
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
    if (mini != null) {
      try {
          mini.run();
      } catch (final Exception ex) {
          Assume.assumeNoException(ex);
      }
    }
  }

  public static void main(final String args[]) {
    for(int i=0; i<args.length; i++) {
        if(args[i].equals("-frames")) {
            framesPerTest = MiscUtils.atoi(args[++i], framesPerTest);
        }
    }
    org.junit.runner.JUnitCore.main(TestGLCanvasAWTActionDeadlock02AWT.class.getName());
  }

}
