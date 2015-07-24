package com.jogamp.opengl.test.junit.jogl.acore;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.Timer;

import org.junit.Test;

import com.jogamp.common.os.Platform;
import com.jogamp.opengl.*;
import com.jogamp.opengl.test.junit.util.DumpGLInfo;

/**
 * Bug 1160.
 * Test for context sharing with an external context. Creates an external GL context, then
 * sets up an offscreen drawable which shares with it. The test contains two cases: one
 * which creates and repaints the offscreen drawable on the EDT, and one which does so on
 * a dedicated thread. On Windows+NVidia, the former fails.
 */
public class TestSharedExternalContextAWT {

  private static final int LATCH_COUNT = 5;

  private void doTest(final boolean aUseEDT) throws Exception {
    final CountDownLatch latch = new CountDownLatch(LATCH_COUNT);
    final MyGLEventListener listener = new MyGLEventListener(aUseEDT, latch);

    /**
     * For the purpose of this test, this offscreen drawable will be used to create
     * an external GL context. In the actual application, the external context
     * represents a GL context which lives outside the JVM.
     */
    final Runnable runnable = new Runnable() {
      public void run() {
        final GLProfile glProfile = GLProfile.getDefault();
        final GLCapabilities caps = new GLCapabilities(glProfile);
        final GLAutoDrawable buffer = GLDrawableFactory.getDesktopFactory().createOffscreenAutoDrawable(
            GLProfile.getDefaultDevice(), caps, null, 512, 512
        );
        // The listener will set up the context sharing in its init() method.
        buffer.addGLEventListener(new DumpGLInfo(Platform.getNewline()+Platform.getNewline()+"Root GLContext", false, false, false));
        buffer.addGLEventListener(listener);
        buffer.display();
      }
    };

    // Wait for test to finish.
    final Thread thread = new Thread(runnable);
    thread.start();
    thread.join();
    latch.await(3, TimeUnit.SECONDS);

    // If exceptions occurred, fail.
    final Exception e = listener.fException;
    if (e != null) {
      throw e;
    }
  }

  @Test
  public void test01OnEDT() throws Exception {
    doTest(true);
  }

  // @Test
  public void test02OnExecutorThread() throws Exception {
    doTest(false);
  }

  /**
   * Listener that creates an external drawable and an offscreen drawable, with context
   * sharing between the two.
   */
  private static class MyGLEventListener implements GLEventListener {
    private GLOffscreenAutoDrawable fOffscreenDrawable;
    private final boolean fUseEDT;
    private final CountDownLatch fLatch;

    private Exception fException = null;

    public MyGLEventListener(final boolean aUseEDT, final CountDownLatch aLatch) {
      fUseEDT = aUseEDT;
      fLatch = aLatch;
    }

    @Override
    public void init(final GLAutoDrawable drawable) {
      final GL2 gl = drawable.getGL().getGL2();
      gl.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
      gl.glClear(GL.GL_COLOR_BUFFER_BIT);

      final GLContext master = drawable.getContext();
      System.err.println("Master (orig) Ct: "+master);

      // Create the external context on the caller thread.
      final GLContext ext = GLDrawableFactory.getDesktopFactory().createExternalGLContext();
      System.err.println("External Context: "+ext);

      // This runnable creates an offscreen drawable which shares with the external context.
      final Runnable initializer = new Runnable() {
        public void run() {
          fOffscreenDrawable = GLDrawableFactory.getDesktopFactory().createOffscreenAutoDrawable(
              GLProfile.getDefaultDevice(),
              new GLCapabilities(GLProfile.getDefault()),
              new DefaultGLCapabilitiesChooser(),
              512, 512
          );
          // fOffscreenDrawable.setSharedContext(ext);
          fOffscreenDrawable.setSharedContext(master);
          // Causes GLException on NVidia driver if using EDT (see below)
          try {
            fOffscreenDrawable.display();
          } catch (final GLException e) {
            fException = e;
            throw e;
          }
        }
      };

      /**
       * Depending on the test case, invoke the initialization on the EDT or on an
       * executor thread. The test also displays the offscreen drawable a few times
       * before finishing.
       */
      if (fUseEDT) {
        // Initialize using invokeAndWait().
        try {
          EventQueue.invokeAndWait(initializer);
        } catch (final InterruptedException e) {
          fException = e;
        } catch (final InvocationTargetException e) {
          fException = e;
        }

        // Display using a Swing timer, i.e. also on the EDT.
        final Timer t = new Timer(200, new ActionListener() {
          int i = 0;

          @Override
          public void actionPerformed(final ActionEvent e) {
            if (++i > LATCH_COUNT) {
              return;
            }

            System.err.println("Update on EDT");
            fOffscreenDrawable.display();
            fLatch.countDown();
          }
        });
        t.start();
      } else {
        // Initialize and display using a single-threaded executor.
        final ScheduledExecutorService exe = Executors.newSingleThreadScheduledExecutor();
        exe.submit(initializer);
        exe.scheduleAtFixedRate(new Runnable() {
          int i = 0;

          @Override
          public void run() {
            if (++i > LATCH_COUNT) {
              return;
            }

            System.err.println("Update on Executor thread");
            fOffscreenDrawable.display();
            fLatch.countDown();
          }
        }, 0, 200, TimeUnit.MILLISECONDS);
      }
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
    }
  }

  public static void main(final String[] pArgs)
  {
      org.junit.runner.JUnitCore.main(TestSharedExternalContextAWT.class.getName());
  }
}
